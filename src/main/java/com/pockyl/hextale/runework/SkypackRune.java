package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hextech Skypack takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars (1-3);
 * index 0 of the thrust table is the pack without the rune. They make flying faster, longer and safer: stronger jets, a
 * harder kick at the start, air saved, soft landings, no fear of fire.
 */
public enum SkypackRune implements HolderRune {
    /** The jets push harder: a higher top speed. */
    MOTOR("metal_spark"),
    /** The jets catch with a kick that throws me forward. */
    BELLOWS("air_fire"),
    /** A second of thrust may cost no air. */
    DAM("earth_water"),
    /** Walls and the ground hit softer while I fly and right after. */
    BREATH("air_life"),
    /** Fire Resistance while the jets burn. */
    FIREPROOFING("life_fire");

    public static final int SLOTS = 3;
    /** Speed (blocks a tick) the jets drive towards, by strength; a firework rocket drives towards 1.5. */
    private static final float[] THRUST = {1.3F, 1.5F, 1.7F, 1.9F, 2.1F};
    /** The kick when the jets catch, blocks a tick along the look, by strength (index 0 = without the rune). */
    private static final float[] KICK = {0.35F, 0.7F, 1.0F, 1.3F, 1.6F};
    public static final float[] THRIFT = {0.2F, 0.35F, 0.5F, 0.6F};
    /** Share of the damage of a wall or a landing taken away. */
    public static final float[] CUSHION = {0.25F, 0.5F, 0.75F, 1.0F};
    /** Ticks of Fire Resistance kept up while flying (what is left after landing). */
    public static final int[] FIRE_TICKS = {100, 140, 180, 220};

    private final ComplexRune rune;

    SkypackRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "skypack";
    }

    @Nullable
    public static SkypackRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static float thrust(int strength) {
        return THRUST[Mth.clamp(strength, 0, 4)];
    }

    public static float kick(int strength) {
        return KICK[Mth.clamp(strength, 0, 4)];
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case MOTOR -> HolderRune.percent(thrust(i) / thrust(0) - 1);
            case BELLOWS -> HolderRune.factor(kick(i) / kick(0));
            case DAM -> HolderRune.percent(HolderRune.at(THRIFT, i));
            case BREATH -> HolderRune.percent(HolderRune.at(CUSHION, i));
            case FIREPROOFING -> HolderRune.seconds(HolderRune.at(FIRE_TICKS, i));
        };
    }
}
