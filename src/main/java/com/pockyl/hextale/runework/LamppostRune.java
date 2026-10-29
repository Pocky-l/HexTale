package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hextech Lamppost takes - burned in in a Rune Circle (the lamppost alone in it), up to {@link #SLOTS}; a
 * broken lamppost keeps them. Strength = stars (1-3). Its light reaches further and burns off more of the dirt of the
 * work around it, now and then for nothing; people in its light heal, and hostile things in it stand out.
 */
public enum LamppostRune implements HolderRune {
    /** The light reaches further. */
    SHIFT("air_earth"),
    /** It burns off a larger share of the Thinning around it. */
    PURIFICATION("light_water"),
    /** Now and then a burn costs no light essence. */
    DAM("earth_water"),
    /** Players in its light keep Regeneration. */
    HEALING("water_life"),
    /** Hostile creatures in its light glow, seen through walls. */
    INSIGHT("light_earth");

    public static final int SLOTS = 3;
    /** Reach of the light in blocks every way, index 0 = without Shift. */
    public static final int[] REACH = {8, 10, 12, 14, 16};
    /** Share of every gain of the Thinning in its light it burns off, index 0 = without Purification. */
    public static final float[] SHARE = {0.35F, 0.45F, 0.52F, 0.6F, 0.65F};
    public static final float[] FREE = {0.2F, 0.3F, 0.4F, 0.5F};
    /** Regeneration kept on the players in its light, in ticks. */
    public static final int[] HEALING_TICKS = {60, 100, 140, 180};
    /** Glowing kept on the hostiles in its light, in ticks. */
    public static final int[] GLOW_TICKS = {60, 100, 140, 180};

    private final ComplexRune rune;

    LamppostRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "lamppost";
    }

    @Nullable
    public static LamppostRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    public static int reach(int shift) {
        return REACH[Mth.clamp(shift, 0, REACH.length - 1)];
    }

    public static float share(int purification) {
        return SHARE[Mth.clamp(purification, 0, SHARE.length - 1)];
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(reach(i));
            case PURIFICATION -> HolderRune.percent(share(i));
            case DAM -> HolderRune.percent(HolderRune.at(FREE, i));
            case HEALING -> HolderRune.seconds(HolderRune.at(HEALING_TICKS, i));
            case INSIGHT -> HolderRune.seconds(HolderRune.at(GLOW_TICKS, i));
        };
    }
}
