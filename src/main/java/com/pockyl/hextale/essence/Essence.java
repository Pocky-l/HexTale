package com.pockyl.hextale.essence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A liquid of the elements: how many drops of each element it holds. Boiled out of an item it carries all the item's
 * elements at once (a mixture); only a liquid of a single element - a concentrate - can be used at the Research Table.
 * What is left after a separation is an unknown essence: it still holds its drops (another reagent can draw them out),
 * but it shows only as "unknown essence" and is never a concentrate. Immutable.
 */
public final class Essence {
    public static final Essence EMPTY = new Essence(new int[SimpleRune.values().length], false);
    /** The map key that marks an unknown essence in the saved form. */
    private static final String UNKNOWN_KEY = "unknown";
    private static final Codec<SimpleRune> ELEMENT = Codec.STRING.comapFlatMap(id -> {
        SimpleRune rune = SimpleRune.byId(id);
        return rune != null ? DataResult.success(rune) : DataResult.error(() -> "Unknown element: " + id);
    }, SimpleRune::id);
    public static final Codec<Essence> CODEC = Codec.unboundedMap(Codec.STRING, Codec.intRange(1, Integer.MAX_VALUE))
            .comapFlatMap(Essence::fromSaved, Essence::toSaved);
    public static final StreamCodec<ByteBuf, Essence> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(SimpleRune.values().length)),
            essence -> Arrays.stream(essence.drops).boxed().toList(),
            ByteBufCodecs.BOOL, Essence::isUnknown,
            (list, unknown) -> new Essence(list.stream().mapToInt(Integer::intValue).toArray(), unknown));

    private final int[] drops;
    private final boolean unknown;

    private Essence(int[] drops, boolean unknown) {
        this.drops = Arrays.copyOf(drops, SimpleRune.values().length);
        this.unknown = unknown && total(this.drops) > 0;
    }

    private static DataResult<Essence> fromSaved(Map<String, Integer> saved) {
        int[] out = new int[SimpleRune.values().length];
        for (Map.Entry<String, Integer> entry : saved.entrySet()) {
            if (entry.getKey().equals(UNKNOWN_KEY)) {
                continue;
            }
            SimpleRune rune = SimpleRune.byId(entry.getKey());
            if (rune == null) {
                return DataResult.error(() -> "Unknown element: " + entry.getKey());
            }
            out[rune.ordinal()] = entry.getValue();
        }
        return DataResult.success(new Essence(out, saved.containsKey(UNKNOWN_KEY)));
    }

    private Map<String, Integer> toSaved() {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        asMap().forEach((element, drops) -> out.put(element.id(), drops));
        if (unknown) {
            out.put(UNKNOWN_KEY, 1);
        }
        return out;
    }

    /** Drops per element, indexed by {@link SimpleRune#ordinal()} (copied). */
    public static Essence of(int[] drops) {
        return new Essence(drops, false);
    }

    /** Drops per element as {@link #of(int[])}, marked unknown when {@code unknown}. */
    public static Essence of(int[] drops, boolean unknown) {
        return new Essence(drops, unknown);
    }

    public static Essence of(Map<SimpleRune, Integer> drops) {
        int[] out = new int[SimpleRune.values().length];
        drops.forEach((element, amount) -> out[element.ordinal()] = Math.max(0, amount));
        return new Essence(out, false);
    }

    public static Essence pure(SimpleRune element, int drops) {
        int[] out = new int[SimpleRune.values().length];
        out[element.ordinal()] = drops;
        return new Essence(out, false);
    }

    /** Whether this is an unknown essence (what is left after a separation). */
    public boolean isUnknown() {
        return unknown;
    }

    /** The same drops as an unknown essence. */
    public Essence asUnknown() {
        return new Essence(drops, true);
    }

    private static int total(int[] drops) {
        int total = 0;
        for (int d : drops) {
            total += d;
        }
        return total;
    }

    /** What an item boils down to: {@code perPoint} drops for every point of each of its elements. */
    public static Essence boiled(ItemElements elements, int perPoint) {
        int[] out = new int[SimpleRune.values().length];
        elements.elements().forEach((element, strength) -> out[element.ordinal()] += strength * perPoint);
        return new Essence(out, false);
    }

    public Map<SimpleRune, Integer> asMap() {
        Map<SimpleRune, Integer> out = new EnumMap<>(SimpleRune.class);
        for (SimpleRune element : SimpleRune.values()) {
            if (drops[element.ordinal()] > 0) {
                out.put(element, drops[element.ordinal()]);
            }
        }
        return out;
    }

    public int get(SimpleRune element) {
        return drops[element.ordinal()];
    }

    public int[] drops() {
        return drops.clone();
    }

    public int total() {
        return total(drops);
    }

    public boolean isEmpty() {
        return total() == 0;
    }

    /** How many elements it holds. */
    public int kinds() {
        int kinds = 0;
        for (int d : drops) {
            if (d > 0) {
                kinds++;
            }
        }
        return kinds;
    }

    /** The only element of a concentrate, or null for a mixture, an unknown essence or nothing. */
    @Nullable
    public SimpleRune pureElement() {
        if (unknown || kinds() != 1) {
            return null;
        }
        for (SimpleRune element : SimpleRune.values()) {
            if (drops[element.ordinal()] > 0) {
                return element;
            }
        }
        return null;
    }

    public Essence plus(Essence other) {
        int[] out = drops.clone();
        for (int i = 0; i < out.length; i++) {
            out[i] += other.drops[i];
        }
        return new Essence(out, unknown || other.unknown);
    }

    public Essence minus(Essence other) {
        int[] out = drops.clone();
        for (int i = 0; i < out.length; i++) {
            out[i] = Math.max(0, out[i] - other.drops[i]);
        }
        return new Essence(out, unknown);
    }

    /**
     * A part of {@code amount} drops with the same proportions (largest remainders get the rounding), so pouring a
     * mixture never changes what it is made of.
     */
    public Essence part(int amount) {
        int total = total();
        if (amount >= total) {
            return this;
        }
        if (amount <= 0) {
            return EMPTY;
        }
        int[] out = new int[drops.length];
        double[] rest = new double[drops.length];
        int given = 0;
        for (int i = 0; i < drops.length; i++) {
            double exact = drops[i] * (double) amount / total;
            out[i] = (int) Math.floor(exact);
            rest[i] = exact - out[i];
            given += out[i];
        }
        while (given < amount) {
            int best = -1;
            for (int i = 0; i < drops.length; i++) {
                if (out[i] < drops[i] && (best < 0 || rest[i] > rest[best])) {
                    best = i;
                }
            }
            out[best]++;
            rest[best] = -1;
            given++;
        }
        return new Essence(out, unknown);
    }

    /** The colour of the liquid: its elements' colours mixed by their drops. */
    public int color() {
        int total = total();
        if (total == 0) {
            return 0x9FB8C4;
        }
        double r = 0;
        double g = 0;
        double b = 0;
        for (SimpleRune element : SimpleRune.values()) {
            double w = drops[element.ordinal()] / (double) total;
            r += (element.color() >> 16 & 0xFF) * w;
            g += (element.color() >> 8 & 0xFF) * w;
            b += (element.color() & 0xFF) * w;
        }
        if (unknown) {
            // Murky: halfway to a muddy grey.
            r = (r + 0x6A) / 2;
            g = (g + 0x66) / 2;
            b = (b + 0x5C) / 2;
        }
        return (int) r << 16 | (int) g << 8 | (int) b;
    }

    /** "Life 8, Earth 8" with coloured names; "Unknown essence 8" for an unknown essence. */
    public MutableComponent describe() {
        if (unknown) {
            return Component.translatable("essence.hextale.unknown").withColor(color()).append(" " + total());
        }
        MutableComponent out = Component.empty();
        boolean first = true;
        for (Map.Entry<SimpleRune, Integer> entry : asMap().entrySet()) {
            if (!first) {
                out.append(", ");
            }
            out.append(ResearchText.element(entry.getKey())).append(" " + entry.getValue());
            first = false;
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Essence other && unknown == other.unknown && Arrays.equals(drops, other.drops);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(drops) * 2 + (unknown ? 1 : 0);
    }

    @Override
    public String toString() {
        return (unknown ? "unknown " : "") + asMap();
    }
}
