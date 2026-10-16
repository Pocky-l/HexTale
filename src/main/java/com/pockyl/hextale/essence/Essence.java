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
 * Immutable.
 */
public final class Essence {
    public static final Essence EMPTY = new Essence(new int[SimpleRune.values().length]);
    private static final Codec<SimpleRune> ELEMENT = Codec.STRING.comapFlatMap(id -> {
        SimpleRune rune = SimpleRune.byId(id);
        return rune != null ? DataResult.success(rune) : DataResult.error(() -> "Unknown element: " + id);
    }, SimpleRune::id);
    public static final Codec<Essence> CODEC = Codec.unboundedMap(ELEMENT, Codec.intRange(1, Integer.MAX_VALUE))
            .xmap(Essence::of, Essence::asMap);
    public static final StreamCodec<ByteBuf, Essence> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .apply(ByteBufCodecs.list(SimpleRune.values().length))
            .map(list -> new Essence(list.stream().mapToInt(Integer::intValue).toArray()),
                    essence -> Arrays.stream(essence.drops).boxed().toList());

    private final int[] drops;

    private Essence(int[] drops) {
        this.drops = Arrays.copyOf(drops, SimpleRune.values().length);
    }

    /** Drops per element, indexed by {@link SimpleRune#ordinal()} (copied). */
    public static Essence of(int[] drops) {
        return new Essence(drops);
    }

    public static Essence of(Map<SimpleRune, Integer> drops) {
        int[] out = new int[SimpleRune.values().length];
        drops.forEach((element, amount) -> out[element.ordinal()] = Math.max(0, amount));
        return new Essence(out);
    }

    public static Essence pure(SimpleRune element, int drops) {
        int[] out = new int[SimpleRune.values().length];
        out[element.ordinal()] = drops;
        return new Essence(out);
    }

    /** What an item boils down to: {@code perPoint} drops for every point of each of its elements. */
    public static Essence boiled(ItemElements elements, int perPoint) {
        int[] out = new int[SimpleRune.values().length];
        elements.elements().forEach((element, strength) -> out[element.ordinal()] += strength * perPoint);
        return new Essence(out);
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
        int total = 0;
        for (int d : drops) {
            total += d;
        }
        return total;
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

    /** The only element of a concentrate, or null for a mixture or nothing. */
    @Nullable
    public SimpleRune pureElement() {
        if (kinds() != 1) {
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
        return new Essence(out);
    }

    public Essence minus(Essence other) {
        int[] out = drops.clone();
        for (int i = 0; i < out.length; i++) {
            out[i] = Math.max(0, out[i] - other.drops[i]);
        }
        return new Essence(out);
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
        return new Essence(out);
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
        return (int) r << 16 | (int) g << 8 | (int) b;
    }

    /** "Life 8, Earth 8" with coloured names. */
    public MutableComponent describe() {
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
        return o instanceof Essence other && Arrays.equals(drops, other.drops);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(drops);
    }

    @Override
    public String toString() {
        return asMap().toString();
    }
}
