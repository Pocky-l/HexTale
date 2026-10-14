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
 * A rune is learned with 1-3 stars (the best sheet counts). Studied items show their elements, and the parchment of
 * each rune remembers the sections the ink has passed, so walls stay known from sheet to sheet.
 */
public final class RuneKnowledge {
    private static final Codec<Map<String, Integer>> INT_MAP = Codec.unboundedMap(Codec.STRING, Codec.INT);
    private static final Codec<Map<String, List<Long>>> BITS_MAP = Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf());
    public static final Codec<RuneKnowledge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            INT_MAP.optionalFieldOf("stars", Map.of()).forGetter(k -> k.starsByKey()),
            INT_MAP.optionalFieldOf("best", Map.of()).forGetter(k -> k.bestByKey()),
            BITS_MAP.optionalFieldOf("seen", Map.of()).forGetter(RuneKnowledge::seenByKey),
            Codec.STRING.listOf().optionalFieldOf("studied", List.of()).forGetter(k -> List.copyOf(k.studied))
    ).apply(instance, RuneKnowledge::new));

    private static final StreamCodec<ByteBuf, List<String>> STRINGS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(4096));
    private static final StreamCodec<ByteBuf, Map<String, Integer>> INTS =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);
    private static final StreamCodec<ByteBuf, Map<String, List<Long>>> BITS =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(64)));
    public static final StreamCodec<ByteBuf, RuneKnowledge> STREAM_CODEC = StreamCodec.composite(
            INTS, RuneKnowledge::starsByKey,
            INTS, RuneKnowledge::bestByKey,
            BITS, RuneKnowledge::seenByKey,
            STRINGS, k -> List.copyOf(k.studied),
            RuneKnowledge::new);

    /** Learned runes in the order of discovery, with their stars (1-3). */
    private final Map<ComplexRune, Integer> stars = new LinkedHashMap<>();
    /** Fewest symbols of a finished sheet per rune. */
    private final Map<ComplexRune, Integer> best = new HashMap<>();
    /** Ids of items whose elements the researcher has studied at the table: their tooltips show the elements. */
    private final Set<String> studied = new LinkedHashSet<>();
    /** Per rune: parchment sections the ink has passed. */
    private final Map<ComplexRune, BitSet> seen = new HashMap<>();

    public RuneKnowledge() {
    }

    private RuneKnowledge(Map<String, Integer> stars, Map<String, Integer> best,
                          Map<String, List<Long>> seen, List<String> studied) {
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
        this.studied.addAll(studied);
        seen.forEach((key, value) -> {
            ComplexRune rune = ComplexRunes.byKey(key);
            if (rune != null) {
                this.seen.put(rune, BitSet.valueOf(value.stream().mapToLong(Long::longValue).toArray()));
            }
        });
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

    /** Whether the item's elements are studied: only studied items show their step and can write on the parchment. */
    public boolean hasStudied(ResourceLocation item) {
        return studied.contains(item.toString());
    }

    /** @return whether the item was new */
    public boolean study(ResourceLocation item) {
        return studied.add(item.toString());
    }

    public void forgetItems() {
        studied.clear();
    }

    public Set<String> studied() {
        return Collections.unmodifiableSet(studied);
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

    public RuneKnowledge copy() {
        return new RuneKnowledge(starsByKey(), bestByKey(), seenByKey(), List.copyOf(studied));
    }
}
