package com.pockyl.hextale.chem;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Lab Bench's flask as pure rules, run by the server and, between its updates, by the screen. Drops of essence fall
 * in; a flame under it (five settings) pulls the temperature towards its heat; every pair of essences that make a compound
 * reacts while the temperature lies in that compound's zone ({@link Reactions}), turning one part of each parent into two
 * parts of the compound. Reactions give off heat, so a busy flask runs hotter than its flame; gas from airy products and
 * searing heat build pressure, which a cool flask slowly lets go and the vent lets out at once (losing some of every
 * essence with the vapour). At {@link #BURST} the flask bursts.
 */
public final class Flask {
    /** Parts the flask holds; one drop is one part. */
    public static final float CAPACITY = 32;
    public static final float AMBIENT = 10;
    public static final int FLAMES = 5;
    /** The temperature each flame setting pulls towards: the middle of each zone. */
    private static final float[] FLAME_HEAT = {AMBIENT, 30, 50, 70, 90};
    public static final float BURST = 100;
    /** Share of the way to the flame's heat the temperature goes each tick. */
    private static final float WARMING = 0.03F;
    /** A drop of room-warm essence cools the flask by this share of its difference to the room. */
    private static final float DROP_COOLING = 0.04F;
    /** Share of the smaller parent that reacts each tick, and the least that reacts. */
    private static final float RATE = 0.05F;
    private static final float MIN_RATE = 0.02F;
    /** Degrees each part of a parent that reacts gives off. */
    private static final float EXOTHERM = 1.6F;
    /** Pressure each part of gas formed adds. */
    private static final float GAS_PRESSURE = 3.5F;
    /** Pressure per tick for each degree above the hot zone. */
    private static final float SEARING_PRESSURE = 0.025F;
    private static final float SEARING_FROM = 80;
    /** Pressure a flask below the hot zone lets go each tick. */
    private static final float RELIEF = 0.12F;
    private static final float RELIEF_BELOW = 60;
    public static final float VENT_RELIEF = 40;
    private static final float VENT_COOLING = 15;
    /** Share of every essence that leaves with the vapour when venting. */
    public static final float VENT_LOSS = 0.15F;
    private static final float EMPTY = 0.001F;
    private static final Codec<Map<String, Float>> AMOUNTS = Codec.unboundedMap(Codec.STRING, Codec.FLOAT);

    private final float[] amounts = new float[Essence.COUNT];
    private float temperature = AMBIENT;
    private float pressure;
    private int flame;

    /** What runes change: how fast reactions run and how fast pressure builds (factors). */
    public record Tuning(float rate, float pressure) {
        public static final Tuning NONE = new Tuning(1, 1);
    }

    public float amount(Essence essence) {
        return amounts[essence.ordinal()];
    }

    public float total() {
        float total = 0;
        for (float amount : amounts) {
            total += amount;
        }
        return total;
    }

    public boolean isEmpty() {
        return total() <= EMPTY;
    }

    public float temperature() {
        return temperature;
    }

    public float pressure() {
        return pressure;
    }

    public int flame() {
        return flame;
    }

    public void setFlame(int flame) {
        this.flame = Math.clamp(flame, 0, FLAMES - 1);
    }

    public static float flameHeat(int flame) {
        return FLAME_HEAT[Math.clamp(flame, 0, FLAMES - 1)];
    }

    /** Whether anything still changes in it: liquid, heat above the room, pressure or a lit flame. */
    public boolean active() {
        return !isEmpty() || flame > 0 || temperature > AMBIENT + 0.5F || pressure > 0;
    }

    public boolean burst() {
        return pressure >= BURST;
    }

    /** The essences in it, most first. */
    public List<Essence> contents() {
        List<Essence> out = new ArrayList<>();
        for (Essence essence : Essence.values()) {
            if (amounts[essence.ordinal()] > EMPTY) {
                out.add(essence);
            }
        }
        out.sort((a, b) -> Float.compare(amounts[b.ordinal()], amounts[a.ordinal()]));
        return out;
    }

    /** Share of the liquid that is {@code essence} (0 when empty). */
    public float purity(Essence essence) {
        float total = total();
        return total <= EMPTY ? 0 : amounts[essence.ordinal()] / total;
    }

    /** The liquid's colour: its essences' colours mixed by amount. */
    public int color() {
        float total = total();
        if (total <= EMPTY) {
            return 0xCFE6EE;
        }
        float r = 0;
        float g = 0;
        float b = 0;
        for (Essence essence : Essence.values()) {
            float share = amounts[essence.ordinal()] / total;
            if (share > 0) {
                int c = essence.color();
                r += (c >> 16 & 255) * share;
                g += (c >> 8 & 255) * share;
                b += (c & 255) * share;
            }
        }
        return Math.round(r) << 16 | Math.round(g) << 8 | Math.round(b);
    }

    /** A drop falls in; refused when the flask is full. */
    public boolean drop(Essence essence) {
        if (total() + 1 > CAPACITY + EMPTY) {
            return false;
        }
        amounts[essence.ordinal()] += 1;
        temperature += (AMBIENT - temperature) * DROP_COOLING;
        return true;
    }

    /** The vent lets the pressure out: the flask cools and some of everything leaves with the vapour. */
    public void vent() {
        pressure = Math.max(0, pressure - VENT_RELIEF);
        temperature = Math.max(AMBIENT, temperature - VENT_COOLING);
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = clean(amounts[i] * (1 - VENT_LOSS));
        }
    }

    /** Empties the flask; the glass stays as hot as it was. */
    public void pour() {
        java.util.Arrays.fill(amounts, 0);
        pressure = 0;
    }

    /** A burst flask: nothing is left of it, the flame goes out. */
    public void shatter() {
        pour();
        temperature = AMBIENT;
        flame = 0;
    }

    /**
     * One tick: heat follows the flame, every pair that makes a compound in the zone the temperature is in reacts, the
     * pressure rises or falls.
     *
     * @return the compounds that formed this tick
     */
    public List<Essence> tick(Tuning tuning) {
        temperature += (flameHeat(flame) - temperature) * WARMING;
        List<Essence> formed = react(tuning);
        if (temperature > SEARING_FROM) {
            pressure += (temperature - SEARING_FROM) * SEARING_PRESSURE * tuning.pressure();
        } else if (temperature < RELIEF_BELOW) {
            pressure = Math.max(0, pressure - RELIEF);
        }
        temperature = Math.clamp(temperature, 0, Reactions.MAX_TEMPERATURE);
        return formed;
    }

    private List<Essence> react(Tuning tuning) {
        int zone = Reactions.zoneAt(temperature);
        List<Essence> present = contents();
        List<Essence[]> pairs = new ArrayList<>();
        List<Float> wanted = new ArrayList<>();
        float[] demand = new float[Essence.COUNT];
        for (int i = 0; i < present.size(); i++) {
            for (int j = i + 1; j < present.size(); j++) {
                Essence a = present.get(i);
                Essence b = present.get(j);
                Essence product = Reactions.product(a, b);
                if (product == null || Reactions.zone(product) != zone) {
                    continue;
                }
                float least = Math.min(amounts[a.ordinal()], amounts[b.ordinal()]);
                float dx = Math.min(least, (least * RATE + MIN_RATE) * tuning.rate());
                pairs.add(new Essence[]{a, b, product});
                wanted.add(dx);
                demand[a.ordinal()] += dx;
                demand[b.ordinal()] += dx;
            }
        }
        if (pairs.isEmpty()) {
            return List.of();
        }
        // A parent in several reactions at once shares itself out; none may take more than there is.
        Map<Essence, Float> made = new LinkedHashMap<>();
        float[] start = amounts.clone();
        for (int k = 0; k < pairs.size(); k++) {
            Essence[] pair = pairs.get(k);
            float dx = wanted.get(k);
            for (int p = 0; p < 2; p++) {
                float need = demand[pair[p].ordinal()];
                if (need > start[pair[p].ordinal()]) {
                    dx = Math.min(dx, wanted.get(k) * start[pair[p].ordinal()] / need);
                }
            }
            if (dx <= 0) {
                continue;
            }
            amounts[pair[0].ordinal()] = clean(amounts[pair[0].ordinal()] - dx);
            amounts[pair[1].ordinal()] = clean(amounts[pair[1].ordinal()] - dx);
            amounts[pair[2].ordinal()] += 2 * dx;
            temperature += EXOTHERM * dx;
            if (Reactions.gas(pair[2])) {
                pressure += GAS_PRESSURE * dx * tuning.pressure();
            }
            made.merge(pair[2], dx, Float::sum);
        }
        return List.copyOf(made.keySet());
    }

    private static float clean(float amount) {
        return amount <= EMPTY ? 0 : amount;
    }

    public Flask copy() {
        Flask copy = new Flask();
        System.arraycopy(amounts, 0, copy.amounts, 0, amounts.length);
        copy.temperature = temperature;
        copy.pressure = pressure;
        copy.flame = flame;
        return copy;
    }

    // ---- Saving ----------------------------------------------------------------------------------------------------------

    public void save(ValueOutput output) {
        Map<String, Float> map = new LinkedHashMap<>();
        for (Essence essence : Essence.values()) {
            if (amounts[essence.ordinal()] > 0) {
                map.put(essence.id(), amounts[essence.ordinal()]);
            }
        }
        output.store("amounts", AMOUNTS, map);
        output.putFloat("temperature", temperature);
        output.putFloat("pressure", pressure);
        output.putInt("flame", flame);
    }

    public void load(ValueInput input) {
        java.util.Arrays.fill(amounts, 0);
        input.read("amounts", AMOUNTS).ifPresent(map -> map.forEach((id, amount) -> {
            Essence essence = Essence.byId(id);
            if (essence != null && Float.isFinite(amount)) {
                amounts[essence.ordinal()] = Math.clamp(amount, 0, CAPACITY);
            }
        }));
        temperature = Math.clamp(input.getFloatOr("temperature", AMBIENT), 0, Reactions.MAX_TEMPERATURE);
        pressure = Math.clamp(input.getFloatOr("pressure", 0), 0, BURST);
        setFlame(input.getIntOr("flame", 0));
    }
}
