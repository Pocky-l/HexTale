package com.pockyl.hextale.essence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.Essence;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A liquid of essences, measured in millibuckets: how much of each essence it holds, plus the unknown essence - the dark
 * by-product every separation adds to what it leaves behind. It is one mixed liquid: poured or piped, any part of it
 * keeps its proportions. Boiled out of an item it carries all the item's essences (a mixture); only a liquid of a
 * single essence and nothing unknown - a concentrate - is ink for the Research Table. Immutable.
 */
public final class EssenceMix {
    /** Slots: one per essence ({@link Essence#ordinal()}), then the unknown essence. */
    private static final int UNKNOWN = Essence.values().length;
    private static final int SLOTS = UNKNOWN + 1;
    public static final EssenceMix EMPTY = new EssenceMix(new int[SLOTS]);
    /** The unknown essence's colour: dark and murky. */
    public static final int UNKNOWN_COLOR = 0x2A2433;
    private static final String UNKNOWN_KEY = "unknown";
    public static final Codec<EssenceMix> CODEC = Codec.unboundedMap(Codec.STRING, Codec.intRange(1, Integer.MAX_VALUE))
            .comapFlatMap(EssenceMix::fromSaved, EssenceMix::toSaved);
    /** Sparse on the wire: pairs of slot and amount for the slots that hold anything. */
    public static final StreamCodec<ByteBuf, EssenceMix> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .apply(ByteBufCodecs.list(SLOTS * 2))
            .map(list -> {
                int[] out = new int[SLOTS];
                for (int i = 0; i + 1 < list.size(); i += 2) {
                    if (list.get(i) >= 0 && list.get(i) < SLOTS) {
                        out[list.get(i)] = list.get(i + 1);
                    }
                }
                return new EssenceMix(out);
            }, mix -> {
                java.util.List<Integer> pairs = new java.util.ArrayList<>();
                for (int i = 0; i < SLOTS; i++) {
                    if (mix.amounts[i] > 0) {
                        pairs.add(i);
                        pairs.add(mix.amounts[i]);
                    }
                }
                return pairs;
            });

    private final int[] amounts;

    private EssenceMix(int[] amounts) {
        this.amounts = Arrays.copyOf(amounts, SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            this.amounts[i] = Math.max(0, this.amounts[i]);
        }
    }

    private static DataResult<EssenceMix> fromSaved(Map<String, Integer> saved) {
        int[] out = new int[SLOTS];
        for (Map.Entry<String, Integer> entry : saved.entrySet()) {
            if (entry.getKey().equals(UNKNOWN_KEY)) {
                out[UNKNOWN] = entry.getValue();
                continue;
            }
            Essence essence = Essence.byId(entry.getKey());
            if (essence == null) {
                return DataResult.error(() -> "Unknown essence: " + entry.getKey());
            }
            out[essence.ordinal()] = entry.getValue();
        }
        return DataResult.success(new EssenceMix(out));
    }

    private Map<String, Integer> toSaved() {
        Map<String, Integer> out = new LinkedHashMap<>();
        asMap().forEach((essence, amount) -> out.put(essence.id(), amount));
        if (amounts[UNKNOWN] > 0) {
            out.put(UNKNOWN_KEY, amounts[UNKNOWN]);
        }
        return out;
    }

    /** Millibuckets per essence, indexed by {@link Essence#ordinal()}, then the unknown essence (copied; shorter arrays
     * are padded with zeroes). */
    public static EssenceMix of(int[] amounts) {
        return new EssenceMix(amounts);
    }

    public static EssenceMix of(Map<Essence, Integer> amounts) {
        int[] out = new int[SLOTS];
        amounts.forEach((essence, amount) -> out[essence.ordinal()] = amount);
        return new EssenceMix(out);
    }

    public static EssenceMix pure(Essence essence, int amount) {
        int[] out = new int[SLOTS];
        out[essence.ordinal()] = amount;
        return new EssenceMix(out);
    }

    /** {@code amount} mB of every essence (the unknown one left out): what a Creative Essence Tank holds. */
    public static EssenceMix all(int amount) {
        int[] out = new int[SLOTS];
        Arrays.fill(out, 0, UNKNOWN, amount);
        return new EssenceMix(out);
    }

    /** Only the unknown essence. */
    public static EssenceMix unknown(int amount) {
        int[] out = new int[SLOTS];
        out[UNKNOWN] = amount;
        return new EssenceMix(out);
    }

    /** What an item boils down to: {@code perPoint} mB for every point of each of its essences. */
    public static EssenceMix boiled(ItemEssences essences, int perPoint) {
        int[] out = new int[SLOTS];
        essences.essences().forEach((essence, strength) -> out[essence.ordinal()] += strength * perPoint);
        return new EssenceMix(out);
    }

    /** The known essences in it (the unknown essence left out). */
    public Map<Essence, Integer> asMap() {
        Map<Essence, Integer> out = new EnumMap<>(Essence.class);
        for (Essence essence : Essence.values()) {
            if (amounts[essence.ordinal()] > 0) {
                out.put(essence, amounts[essence.ordinal()]);
            }
        }
        return out;
    }

    public int get(Essence essence) {
        return amounts[essence.ordinal()];
    }

    /** Millibuckets of the unknown essence. */
    public int unknown() {
        return amounts[UNKNOWN];
    }

    public boolean isUnknown() {
        return amounts[UNKNOWN] > 0;
    }

    /** All the slots, the unknown essence last (copied). */
    public int[] amounts() {
        return amounts.clone();
    }

    /** Millibuckets of the whole liquid. */
    public int total() {
        int total = 0;
        for (int a : amounts) {
            total += a;
        }
        return total;
    }

    public boolean isEmpty() {
        return total() == 0;
    }

    /** How many essences it holds (the unknown one counts). */
    public int kinds() {
        int kinds = 0;
        for (int a : amounts) {
            if (a > 0) {
                kinds++;
            }
        }
        return kinds;
    }

    /** The only essence of a concentrate, or null for a mixture, anything with unknown essence in it, or nothing. */
    @Nullable
    public Essence pureEssence() {
        if (amounts[UNKNOWN] > 0 || kinds() != 1) {
            return null;
        }
        for (Essence essence : Essence.values()) {
            if (amounts[essence.ordinal()] > 0) {
                return essence;
            }
        }
        return null;
    }

    public EssenceMix plus(EssenceMix other) {
        int[] out = amounts.clone();
        for (int i = 0; i < SLOTS; i++) {
            out[i] += other.amounts[i];
        }
        return new EssenceMix(out);
    }

    /** Whether this holds at least as much of every essence as {@code other}. */
    public boolean holds(EssenceMix other) {
        for (int i = 0; i < SLOTS; i++) {
            if (amounts[i] < other.amounts[i]) {
                return false;
            }
        }
        return true;
    }

    /** How many times over this holds {@code other} (an empty {@code other} fits any number of times). */
    public int timesHolds(EssenceMix other) {
        int times = Integer.MAX_VALUE;
        for (int i = 0; i < SLOTS; i++) {
            if (other.amounts[i] > 0) {
                times = Math.min(times, amounts[i] / other.amounts[i]);
            }
        }
        return times;
    }

    public EssenceMix times(int factor) {
        int[] out = amounts.clone();
        for (int i = 0; i < SLOTS; i++) {
            out[i] *= factor;
        }
        return new EssenceMix(out);
    }

    public EssenceMix minus(EssenceMix other) {
        int[] out = amounts.clone();
        for (int i = 0; i < SLOTS; i++) {
            out[i] = Math.max(0, out[i] - other.amounts[i]);
        }
        return new EssenceMix(out);
    }

    /**
     * A part of {@code amount} mB with the same proportions (largest remainders get the rounding), so pouring a mixture
     * never changes what it is made of.
     */
    public EssenceMix part(int amount) {
        int total = total();
        if (amount >= total) {
            return this;
        }
        if (amount <= 0) {
            return EMPTY;
        }
        int[] out = new int[SLOTS];
        double[] rest = new double[SLOTS];
        int given = 0;
        for (int i = 0; i < SLOTS; i++) {
            double exact = amounts[i] * (double) amount / total;
            out[i] = (int) Math.floor(exact);
            rest[i] = exact - out[i];
            given += out[i];
        }
        while (given < amount) {
            int best = -1;
            for (int i = 0; i < SLOTS; i++) {
                if (out[i] < amounts[i] && (best < 0 || rest[i] > rest[best])) {
                    best = i;
                }
            }
            out[best]++;
            rest[best] = -1;
            given++;
        }
        return new EssenceMix(out);
    }

    /** The colour of the liquid: its essences' colours (the unknown one dark) mixed by their amounts. */
    public int color() {
        int total = total();
        if (total == 0) {
            return 0x9FB8C4;
        }
        double r = 0;
        double g = 0;
        double b = 0;
        for (int i = 0; i < SLOTS; i++) {
            int color = i == UNKNOWN ? UNKNOWN_COLOR : Essence.values()[i].color();
            double w = amounts[i] / (double) total;
            r += (color >> 16 & 0xFF) * w;
            g += (color >> 8 & 0xFF) * w;
            b += (color & 0xFF) * w;
        }
        return (int) r << 16 | (int) g << 8 | (int) b;
    }

    /** "Life 8, Earth 6, Unknown essence 2" with coloured names (millibuckets). */
    public MutableComponent describe() {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (Map.Entry<Essence, Integer> entry : asMap().entrySet()) {
            if (!first) {
                out.append(", ");
            }
            out.append(ResearchText.essence(entry.getKey())).append(" " + entry.getValue());
            first = false;
        }
        if (amounts[UNKNOWN] > 0) {
            if (!first) {
                out.append(", ");
            }
            out.append(Component.translatable("essence.hextale.unknown").withColor(0x6A5A7A)).append(" " + amounts[UNKNOWN]);
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EssenceMix other && Arrays.equals(amounts, other.amounts);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(amounts);
    }

    @Override
    public String toString() {
        return asMap() + (amounts[UNKNOWN] > 0 ? " + unknown " + amounts[UNKNOWN] : "");
    }
}
