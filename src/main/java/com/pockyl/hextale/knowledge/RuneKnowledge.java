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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a researcher has found out. Lives on the player (a data attachment copied on death), not on the diary item: a
 * lost diary loses nothing. The server copy is authoritative; clients get a copy through {@code KnowledgeSyncPayload}.
 * <p>
 * A rune is learned with 1-3 stars from a scroll found in the world (the best scroll counts). Studied items show their
 * essences. Researched topics are the devices and tools the researcher may make; the parchment of each topic remembers
 * the sections the ink has passed and the curses found, so walls stay known from sheet to sheet. Done quests of the
 * researcher's guide are kept by id.
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
            BITS_MAP.optionalFieldOf("curses", Map.of()).forGetter(k -> bitsByKey(k.curses))
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
            BITS, k -> bitsByKey(k.curses),
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
    /** Per topic id: cursed dark sections found the hard way (the orb burst there). */
    private final Map<String, BitSet> curses = new HashMap<>();

    public RuneKnowledge() {
    }

    private RuneKnowledge(Map<String, Integer> stars, List<String> researched, Map<String, List<Long>> seen, List<String> studied,
                          List<String> quests, Map<String, List<Long>> curses) {
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
        readBits(curses, this.curses);
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

    public void forgetResearch() {
        researched.clear();
        seen.clear();
        curses.clear();
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

    /** Cursed sections of this topic's parchment found so far (a copy). */
    public BitSet curses(ResearchTopic topic) {
        BitSet bits = curses.get(topic.id());
        return bits == null ? new BitSet() : (BitSet) bits.clone();
    }

    public void curse(ResearchTopic topic, int cell) {
        curses.computeIfAbsent(topic.id(), key -> new BitSet()).set(cell);
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

    public void forgetQuests() {
        quests.clear();
    }

    public RuneKnowledge copy() {
        return new RuneKnowledge(starsByKey(), List.copyOf(researched), bitsByKey(seen), List.copyOf(studied), List.copyOf(quests),
                bitsByKey(curses));
    }
}
