package com.pockyl.hextale.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * What a researcher has found out. Lives on the player (a data attachment copied on death), not on the diary item: a
 * lost diary loses nothing. The server copy is authoritative; clients get a copy through {@code KnowledgeSyncPayload}.
 * <p>
 * A rune is learned with 1-3 stars (the best sheet counts). Tried items reveal their elements, and the parchment of
 * each rune remembers the sections the ink has passed, so walls stay known from sheet to sheet.
 */
public final class RuneKnowledge {
    private static final Codec<Map<String, Integer>> INT_MAP = Codec.unboundedMap(Codec.STRING, Codec.INT);
    private static final Codec<Map<String, List<Long>>> BITS_MAP = Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf());
    public static final Codec<RuneKnowledge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            INT_MAP.optionalFieldOf("stars", Map.of()).forGetter(k -> k.starsByKey()),
            INT_MAP.optionalFieldOf("best", Map.of()).forGetter(k -> k.bestByKey()),
            Codec.STRING.listOf().optionalFieldOf("items", List.of()).forGetter(k -> List.copyOf(k.items)),
            BITS_MAP.optionalFieldOf("seen", Map.of()).forGetter(RuneKnowledge::seenByKey),
            Codec.STRING.listOf().optionalFieldOf("notes", List.of()).forGetter(k -> keys(k.notes, DiaryNote::id)),
            Codec.STRING.listOf().optionalFieldOf("read_notes", List.of()).forGetter(k -> keys(k.readNotes, DiaryNote::id))
    ).apply(instance, RuneKnowledge::new));

    private static final StreamCodec<ByteBuf, List<String>> STRINGS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(4096));
    private static final StreamCodec<ByteBuf, Map<String, Integer>> INTS =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);
    private static final StreamCodec<ByteBuf, Map<String, List<Long>>> BITS =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(64)));
    public static final StreamCodec<ByteBuf, RuneKnowledge> STREAM_CODEC = StreamCodec.composite(
            INTS, RuneKnowledge::starsByKey,
            INTS, RuneKnowledge::bestByKey,
            STRINGS, k -> List.copyOf(k.items),
            BITS, RuneKnowledge::seenByKey,
            STRINGS, k -> keys(k.notes, DiaryNote::id),
            STRINGS, k -> keys(k.readNotes, DiaryNote::id),
            RuneKnowledge::new);

    /** Learned runes in the order of discovery, with their stars (1-3). */
    private final Map<ComplexRune, Integer> stars = new LinkedHashMap<>();
    /** Fewest symbols of a finished sheet per rune. */
    private final Map<ComplexRune, Integer> best = new HashMap<>();
    /** Ids of items the researcher has written with: their elements are known. */
    private final Set<String> items = new LinkedHashSet<>();
    /** Per rune: parchment sections the ink has passed. */
    private final Map<ComplexRune, BitSet> seen = new HashMap<>();
    /** Unlocked diary notes in the order they were written. */
    private final Set<DiaryNote> notes = new LinkedHashSet<>();
    private final Set<DiaryNote> readNotes = EnumSet.noneOf(DiaryNote.class);

    public RuneKnowledge() {
    }

    private RuneKnowledge(Map<String, Integer> stars, Map<String, Integer> best, List<String> items,
                          Map<String, List<Long>> seen, List<String> notes, List<String> readNotes) {
        stars.forEach((key, value) -> {
            ComplexRune rune = ComplexRunes.byKey(key);
            if (rune != null && value > 0) {
                this.stars.put(rune, Math.min(3, value));
            }
        });
        best.forEach((key, value) -> {
            ComplexRune rune = ComplexRunes.byKey(key);
            if (rune != null) {
                this.best.put(rune, value);
            }
        });
        this.items.addAll(items);
        seen.forEach((key, value) -> {
            ComplexRune rune = ComplexRunes.byKey(key);
            if (rune != null) {
                this.seen.put(rune, BitSet.valueOf(value.stream().mapToLong(Long::longValue).toArray()));
            }
        });
        notes.stream().map(DiaryNote::byId).filter(Objects::nonNull).forEach(this.notes::add);
        readNotes.stream().map(DiaryNote::byId).filter(Objects::nonNull).forEach(this.readNotes::add);
    }

    private static <T> List<String> keys(Collection<T> values, Function<T, String> key) {
        return values.stream().map(key).toList();
    }

    private Map<String, Integer> starsByKey() {
        Map<String, Integer> out = new LinkedHashMap<>();
        stars.forEach((rune, value) -> out.put(rune.key(), value));
        return out;
    }

    private Map<String, Integer> bestByKey() {
        Map<String, Integer> out = new LinkedHashMap<>();
        best.forEach((rune, value) -> out.put(rune.key(), value));
        return out;
    }

    private Map<String, List<Long>> seenByKey() {
        Map<String, List<Long>> out = new HashMap<>();
        seen.forEach((rune, bits) -> {
            List<Long> longs = new ArrayList<>();
            for (long word : bits.toLongArray()) {
                longs.add(word);
            }
            out.put(rune.key(), longs);
        });
        return out;
    }

    public boolean knows(ComplexRune rune) {
        return stars.containsKey(rune);
    }

    /** 0 for an unknown rune. */
    public int stars(ComplexRune rune) {
        return stars.getOrDefault(rune, 0);
    }

    /** Fewest symbols of a finished sheet, or 0. */
    public int best(ComplexRune rune) {
        return best.getOrDefault(rune, 0);
    }

    /**
     * Records a finished sheet.
     *
     * @return whether the rune was new
     */
    public boolean learn(ComplexRune rune, int stars, int symbols) {
        boolean fresh = !knows(rune);
        this.stars.merge(rune, Math.max(1, Math.min(3, stars)), Math::max);
        if (symbols > 0) {
            best.merge(rune, symbols, Math::min);
        }
        return fresh;
    }

    public boolean forget(ComplexRune rune) {
        best.remove(rune);
        seen.remove(rune);
        return stars.remove(rune) != null;
    }

    public int runeCount() {
        return stars.size();
    }

    /** Learned runes in the order of discovery. */
    public List<ComplexRune> runes() {
        return List.copyOf(stars.keySet());
    }

    public boolean knowsItem(ResourceLocation item) {
        return items.contains(item.toString());
    }

    /** @return whether the item was new */
    public boolean learnItem(ResourceLocation item) {
        return items.add(item.toString());
    }

    public void forgetItems() {
        items.clear();
    }

    public Set<String> items() {
        return Collections.unmodifiableSet(items);
    }

    /** Sections of this rune's parchment the ink has passed (a copy). */
    public BitSet seen(ComplexRune rune) {
        BitSet bits = seen.get(rune);
        return bits == null ? new BitSet() : (BitSet) bits.clone();
    }

    /** @return whether anything was new */
    public boolean see(ComplexRune rune, int[] cells) {
        BitSet bits = seen.computeIfAbsent(rune, r -> new BitSet());
        boolean changed = false;
        for (int cell : cells) {
            if (!bits.get(cell)) {
                bits.set(cell);
                changed = true;
            }
        }
        return changed;
    }

    public boolean hasNote(DiaryNote note) {
        return notes.contains(note);
    }

    /** @return whether the note was new */
    public boolean addNote(DiaryNote note) {
        return notes.add(note);
    }

    /** Unlocked notes, newest first. */
    public List<DiaryNote> notesNewestFirst() {
        List<DiaryNote> list = new ArrayList<>(notes);
        Collections.reverse(list);
        return list;
    }

    public boolean isRead(DiaryNote note) {
        return readNotes.contains(note);
    }

    public int unreadNotes() {
        int count = 0;
        for (DiaryNote note : notes) {
            if (!readNotes.contains(note)) {
                count++;
            }
        }
        return count;
    }

    /** @return whether anything changed */
    public boolean markNotesRead() {
        return readNotes.addAll(notes);
    }

    public RuneKnowledge copy() {
        return new RuneKnowledge(starsByKey(), bestByKey(), List.copyOf(items), seenByKey(), keys(notes, DiaryNote::id),
                keys(readNotes, DiaryNote::id));
    }
}
