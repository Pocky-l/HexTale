package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes a Hex Smelter takes - burned in at the Hextech Altar like a pickaxe's, up to {@link #SLOTS}. Strength =
 * stars, one more with a Lens (1-4); the numbers per strength live here.
 */
public enum SmelterRune {
    /** Less heat per item. */
    TEMPERING("earth_fire"),
    /** Faster smelting. */
    BELLOWS("air_fire"),
    /** A chance of one more piece from ores and raw metals. */
    FORGE("metal_fire"),
    /** The other runes work one star stronger. */
    LENS("water_light");

    public static final int SLOTS = 3;
    /** Share of the heat an item still needs, by strength. */
    private static final float[] HEAT = {0.75F, 0.6F, 0.45F, 0.35F};
    /** How many times faster, by strength. */
    private static final float[] SPEED = {1.5F, 2.0F, 2.5F, 3.0F};
    /** Chance of one more piece, by strength. */
    private static final float[] EXTRA = {0.15F, 0.3F, 0.45F, 0.6F};
    /** mB of the rune's own essence per star, by its chapter (I, II, III). */
    private static final int[] MB_PER_STAR = {12, 18, 24};

    private final ComplexRune rune;

    SmelterRune(String key) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    public ComplexRune rune() {
        return rune;
    }

    @Nullable
    public static SmelterRune of(ComplexRune rune) {
        for (SmelterRune value : values()) {
            if (value.rune == rune) {
                return value;
            }
        }
        return null;
    }

    /** What burning this rune in with these stars costs: its own essence (the compound of its primals). */
    public EssenceMix cost(int stars) {
        Essence pair = Objects.requireNonNull(Essence.pair(rune.action(), rune.target()), rune.key());
        return EssenceMix.pure(pair, MB_PER_STAR[ResearchBook.chapter(rune) - 1] * stars);
    }

    public static float heatShare(int strength) {
        return strength <= 0 ? 1 : HEAT[Math.min(4, strength) - 1];
    }

    public static float speed(int strength) {
        return strength <= 0 ? 1 : SPEED[Math.min(4, strength) - 1];
    }

    public static float extra(int strength) {
        return strength <= 0 ? 0 : EXTRA[Math.min(4, strength) - 1];
    }

    public String descriptionKey() {
        return "smelter_rune.hextale." + name().toLowerCase(Locale.ROOT) + ".desc";
    }

    public String valueKey() {
        return "smelter_rune.hextale." + name().toLowerCase(Locale.ROOT) + ".value";
    }

    /** The number behind a strength, for {@link #valueKey()}. */
    public String amount(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case TEMPERING -> Math.round((1 - heatShare(i)) * 100) + "%";
            case BELLOWS -> trim(speed(i));
            case FORGE -> Math.round(extra(i) * 100) + "%";
            case LENS -> "";
        };
    }

    private static String trim(float value) {
        return value == Math.round(value) ? Integer.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }
}
