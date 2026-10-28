package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hextech Tuning Fork takes - burned in at the Hextech Altar, up to {@link #SLOTS}. Strength = stars, one
 * more with a Lens (1-4). They make it hear farther and more, and show what it heard.
 */
public enum ForkRune implements HolderRune {
    /** It hears farther. */
    SHIFT("air_earth"),
    /** What it hears glows through the stone for a while. */
    INSIGHT("light_earth"),
    /** It hears several things at once, each with its own thread. */
    SIGNAL("light_air"),
    /** Untuned, it hears ores as well as hextech, within part of its reach. */
    GEMSTONE("earth_light"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Blocks it hears without Shift. */
    public static final int REACH = 16;
    /** Blocks Shift adds, by strength. */
    public static final int[] EXTRA_REACH = {6, 10, 14, 18};
    /** Ticks what it heard glows, by strength. */
    public static final int[] GLOW_TICKS = {60, 100, 140, 180};
    /** How many things it hears at once, by strength (one without Signal). */
    public static final int[] VOICES = {2, 3, 4, 5};
    /** Share of the reach within which an untuned fork hears ores, by strength. */
    public static final float[] ORE_SHARE = {0.4F, 0.6F, 0.8F, 1.0F};

    private final ComplexRune rune;

    ForkRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "fork";
    }

    @Nullable
    public static ForkRune of(ComplexRune rune) {
        return HolderRune.find(values(), rune);
    }

    /** Blocks the fork hears at Shift's strength (0 = without it). */
    public static int reach(int strength) {
        return strength <= 0 ? REACH : REACH + HolderRune.at(EXTRA_REACH, strength);
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case SHIFT -> Integer.toString(reach(i));
            case INSIGHT -> HolderRune.seconds(HolderRune.at(GLOW_TICKS, i));
            case SIGNAL -> Integer.toString(HolderRune.at(VOICES, i));
            case GEMSTONE -> HolderRune.percent(HolderRune.at(ORE_SHARE, i));
            case LENS -> "";
        };
    }
}
