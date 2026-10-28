package com.pockyl.hextale.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What a researcher has found out. Lives on the player (a data attachment copied on death), not on the diary item: a
 * lost diary loses nothing. The server copy is authoritative; clients get a copy through {@code KnowledgeSyncPayload}.
 * <p>
 * A rune is learned with 1-3 stars from a scroll found in the world (the best scroll counts). Studied items show their
 * essences. Researched topics are the devices and tools the researcher may make; the parchment of each topic remembers
 * the sections the ink has passed, so walls stay known from sheet to sheet, until a burst orb burns it: the next attempt
 * is drawn on a new parchment. Done quests of the researcher's guide are kept by id.
 */
public final class RuneKnowledge {
    private static final Codec<Map<String, Integer>> INT_MAP = Codec.unboundedMap(Codec.STRING, Codec.INT);
    private static final Codec<Map<String, List<Long>>> BITS_MAP = Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf());
    public static final MapCodec<RuneKnowledge> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            INT_MAP.optionalFieldOf("stars", Map.of()).forGetter(RuneKnowledge::starsByKey),
            Codec.STRING.listOf().optionalFieldOf("researched", List.of()).forGetter(k -> List.copyOf(k.researched)),
            BITS_MAP.optionalFieldOf("seen", Map.of()).forGetter(k -> bitsByKey(k.seen)),
            Codec.STRING.listOf().optionalFieldOf("studied", List.of()).forGetter(k -> List.copyOf(k.studied)),
            Codec.STRING.listOf().optionalFieldOf("quests", List.of()).forGetter(k -> List.copyOf(k.quests)),
            INT_MAP.optionalFieldOf("attempts", Map.of()).forGetter(k -> Map.copyOf(k.attempts)),
            Codec.STRING.listOf().optionalFieldOf("observed", List.of()).forGetter(k -> List.copyOf(k.observed)),
            Codec.STRING.listOf().optionalFieldOf("noticed").forGetter(RuneKnowledge::noticedList)
    ).apply(instance, RuneKnowledge::new));
    public static final Codec<RuneKnowledge> CODEC = MAP_CODEC.codec();

    private static final StreamCodec<ByteBuf, List<String>> STRINGS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(4096));
    private static final StreamCodec<ByteBuf, Map<String, Integer>> INTS =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);
    private static final StreamCodec<ByteBuf, Map<String, List<Long>>> BITS =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(64)));
    public static final StreamCodec<ByteBuf, RuneKnowledge> STREAM_CODEC = StreamCodec.composite(
            INTS, RuneKnowledge::starsByKey,
            STRINGS, k -> List.copyOf(k.researched),
            BITS, k -> bitsByKey(k.seen),
            STRINGS, k -> List.copyOf(k.studied),
            STRINGS, k -> List.copyOf(k.quests),
            INTS, k -> new LinkedHashMap<>(k.attempts),
            STRINGS, k -> List.copyOf(k.observed),
            ByteBufCodecs.optional(STRINGS), RuneKnowledge::noticedList,
            RuneKnowledge::new);

    /** Learned runes in the order of discovery, with their stars (1-3). */
    private final Map<ComplexRune, Integer> stars = new LinkedHashMap<>();
    /** Ids of researched topics, in the order they were researched. */
    private final Set<String> researched = new LinkedHashSet<>();
    /** Ids of items whose essences the researcher has studied at the table: their tooltips show the essences. */
    private final Set<String> studied = new LinkedHashSet<>();
    /** Ids of the researcher's guide quests that are done. */
    private final Set<String> quests = new LinkedHashSet<>();
    /** Per topic id: parchment sections the ink has passed. */
    private final Map<String, BitSet> seen = new HashMap<>();
    /** Per topic id: how many parchments burned; the next attempt is drawn on a new parchment. */
    private final Map<String, Integer> attempts = new HashMap<>();
    /** Ids of compounds the researcher has seen form in a Lab Bench flask: the journal shows the zone they form in. */
    private final Set<String> observed = new LinkedHashSet<>();
    /**
     * Ids of the guide's steps the researcher has taken in: looked at in the book, or done on purpose. A step that opened,
     * or happened by itself, and is not in here is shown as new.
     */
    private final Set<String> noticed = new LinkedHashSet<>();
    /** False for knowledge saved before steps were noticed: the server takes in what it can already see, once. */
    private boolean noticing = true;

    public RuneKnowledge() {
    }

    private RuneKnowledge(Map<String, Integer> stars, List<String> researched, Map<String, List<Long>> seen, List<String> studied,
                          List<String> quests, Map<String, Integer> attempts, List<String> observed, Optional<List<String>> noticed) {
        stars.forEach((key, value) -> {
            ComplexRune rune = ComplexRunes.byKey(key);
            if (rune != null && value > 0) {
                this.stars.put(rune, Math.min(3, value));
            }
        });
        this.researched.addAll(researched);
        this.studied.addAll(studied);
        this.quests.addAll(quests);
        readBits(seen, this.seen);
        attempts.forEach((key, value) -> {
            if (value > 0) {
                this.attempts.put(key, value);
            }
        });
        this.observed.addAll(observed);
        this.noticing = noticed.isPresent();
        noticed.ifPresent(this.noticed::addAll);
    }

    private Optional<List<String>> noticedList() {
        return noticing ? Optional.of(List.copyOf(noticed)) : Optional.empty();
    }

    private Map<String, Integer> starsByKey() {
        Map<String, Integer> out = new LinkedHashMap<>();
        stars.forEach((rune, value) -> out.put(rune.key(), value));
        return out;
    }

    private static void readBits(Map<String, List<Long>> in, Map<String, BitSet> out) {
        in.forEach((key, value) -> out.put(key, BitSet.valueOf(value.stream().mapToLong(Long::longValue).toArray())));
    }

    private static Map<String, List<Long>> bitsByKey(Map<String, BitSet> source) {
        Map<String, List<Long>> out = new HashMap<>();
        source.forEach((key, bits) -> {
            List<Long> longs = new ArrayList<>();
            for (long word : bits.toLongArray()) {
                longs.add(word);
            }
            out.put(key, longs);
        });
        return out;
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public boolean knows(ComplexRune rune) {
        return stars.containsKey(rune);
    }

    /** 0 for an unknown rune. */
    public int stars(ComplexRune rune) {
        return stars.getOrDefault(rune, 0);
    }

    /**
     * Learns a rune from a scroll, or raises its stars.
     *
     * @return whether the rune was new
     */
    public boolean learn(ComplexRune rune, int stars) {
        boolean fresh = !knows(rune);
        this.stars.merge(rune, Math.max(1, Math.min(3, stars)), Math::max);
        return fresh;
    }

    public boolean forget(ComplexRune rune) {
        return stars.remove(rune) != null;
    }

    public int runeCount() {
        return stars.size();
    }

    /** Learned runes in the order of discovery. */
    public List<ComplexRune> runes() {
        return List.copyOf(stars.keySet());
    }

    // ---- Researched topics --------------------------------------------------------------------------------------------

    public boolean researched(ResearchTopic topic) {
        return researched.contains(topic.id());
    }

    /** @return whether the topic was new */
    public boolean research(ResearchTopic topic) {
        return researched.add(topic.id());
    }

    /** Forgets one topic, with its parchment's progress; whether it was researched. */
    public boolean forgetResearch(ResearchTopic topic) {
        seen.remove(topic.id());
        attempts.remove(topic.id());
        return researched.remove(topic.id());
    }

    /** Ids of the researched topics, in the order they were researched. */
    public List<String> researchedIds() {
        return List.copyOf(researched);
    }

    public void forgetResearch() {
        researched.clear();
        seen.clear();
        attempts.clear();
        observed.clear();
    }

    /** Whether the researcher has seen this compound form in a flask (and so knows its zone). */
    public boolean observed(Essence compound) {
        return observed.contains(compound.id());
    }

    /** @return whether the compound was new */
    public boolean observe(Essence compound) {
        return observed.add(compound.id());
    }

    /** Sections of this topic's parchment the ink has passed (a copy). */
    public BitSet seen(ResearchTopic topic) {
        BitSet bits = seen.get(topic.id());
        return bits == null ? new BitSet() : (BitSet) bits.clone();
    }

    /** @return whether anything was new */
    public boolean see(ResearchTopic topic, int[] cells) {
        BitSet bits = seen.computeIfAbsent(topic.id(), key -> new BitSet());
        boolean changed = false;
        for (int cell : cells) {
            if (!bits.get(cell)) {
                bits.set(cell);
                changed = true;
            }
        }
        return changed;
    }

    /** The researcher's attempt at this topic: 0 until a parchment of it burns. */
    public int attempt(ResearchTopic topic) {
        return attempts.getOrDefault(topic.id(), 0);
    }

    /**
     * The orb burst and the parchment burned: the next attempt is drawn on a new one, so what the ink found on the old
     * one is forgotten.
     *
     * @return the new attempt
     */
    public int burn(ResearchTopic topic) {
        seen.remove(topic.id());
        return attempts.merge(topic.id(), 1, Integer::sum);
    }

    // ---- Studied items and quests -------------------------------------------------------------------------------------

    /** Whether the item's essences are studied: its tooltip shows them. */
    public boolean hasStudied(Identifier item) {
        return studied.contains(item.toString());
    }

    /** @return whether the item was new */
    public boolean study(Identifier item) {
        return studied.add(item.toString());
    }

    public void forgetItems() {
        studied.clear();
    }

    public Set<String> studied() {
        return Collections.unmodifiableSet(studied);
    }

    public boolean questDone(Quest quest) {
        return quests.contains(quest.id());
    }

    public void completeQuest(Quest quest) {
        quests.add(quest.id());
    }

    /** Un-does one step; whether it was done. */
    public boolean forgetQuest(Quest quest) {
        return quests.remove(quest.id());
    }

    public void forgetQuests() {
        quests.clear();
        noticed.clear();
    }

    public boolean anyQuestDone() {
        return !quests.isEmpty();
    }

    // ---- Noticed steps ------------------------------------------------------------------------------------------------

    /** Whether the steps the researcher has taken in are tracked (false only for knowledge saved before they were). */
    public boolean noticing() {
        return noticing;
    }

    public boolean noticed(String step) {
        return noticed.contains(step);
    }

    /** The researcher has taken in this step; true if it was new. */
    public boolean notice(String step) {
        return noticed.add(step);
    }

    /** Starts tracking with these steps already taken in. */
    public void startNoticing(java.util.Collection<String> steps) {
        noticing = true;
        noticed.addAll(steps);
    }

    public RuneKnowledge copy() {
        return new RuneKnowledge(starsByKey(), List.copyOf(researched), bitsByKey(seen), List.copyOf(studied), List.copyOf(quests),
                Map.copyOf(attempts), List.copyOf(observed), noticedList());
    }
}
