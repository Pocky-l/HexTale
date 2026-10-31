package com.pockyl.hextale.runework;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes the researcher's clothes take - one on each piece, burned in at the Hextech Altar. Each piece has its own
 * runes to choose from (the same codex rune may mean something else on another piece). Strength = stars (1-3). Effects
 * live in {@link ClothesEffects}.
 */
public enum ClothesRune implements HolderRune {
    /** Hood: in the dark the eyes get used to it at once. */
    HOOD_BANISHING(Piece.HOOD, "light_shadow"),
    /** Hood: picked-up experience grows. */
    HOOD_RADIANCE(Piece.HOOD, "life_light"),
    /** Hood: under water the breath lasts longer. */
    HOOD_BREATH(Piece.HOOD, "air_life"),
    /** Hood: poison, hunger and weakness sometimes wash off. */
    HOOD_PURIFICATION(Piece.HOOD, "light_water"),
    /** Hood: a blow that leaves me weak lets me fade, and a figure of light stays behind for the foes. */
    HOOD_MIRAGE(Piece.HOOD, "air_light"),
    /** Robe: the worn clothes mend themselves now and then. */
    ROBE_MENDING(Piece.ROBE, "life_metal"),
    /** Robe: a blow may start a short regeneration. */
    ROBE_HEALING(Piece.ROBE, "water_life"),
    /** Robe: fire on the robe brings Fire Resistance. */
    ROBE_FIREPROOFING(Piece.ROBE, "life_fire"),
    /** Robe: a blow may glance back at whoever struck. */
    ROBE_MIRROR(Piece.ROBE, "metal_light"),
    /** Robe: whoever strikes me in close combat may get a spark back that numbs them. */
    ROBE_STATIC(Piece.ROBE, "water_spark"),
    /** Trousers: quicker steps. */
    TROUSERS_HASTE(Piece.TROUSERS, "spark_life"),
    /** Trousers: higher jumps. */
    TROUSERS_WINDMILL(Piece.TROUSERS, "air_spark"),
    /** Trousers: blows push me back less. */
    TROUSERS_GRAVITY(Piece.TROUSERS, "earth_air"),
    /** Trousers: I step up whole blocks without jumping. */
    TROUSERS_SHIFT(Piece.TROUSERS, "air_earth"),
    /** Trousers: loose items and experience around me drift into my pockets (not while I sneak). */
    TROUSERS_MAGNET(Piece.TROUSERS, "spark_metal"),
    /** Boots: softer landings. */
    BOOTS_LIFT(Piece.BOOTS, "air_metal"),
    /** Boots: I walk on water as on ground (sneaking, I sink), and in it the water holds me less. */
    BOOTS_WAVEWALK(Piece.BOOTS, "air_water"),
    /** Boots: crops and saplings by my path sometimes shoot up. */
    BOOTS_POLLEN(Piece.BOOTS, "life_air"),
    /** Boots: hot floors burn the feet less. */
    BOOTS_QUENCHING(Piece.BOOTS, "water_fire");

    public static final int SLOTS = 1;
    /** Night Vision kept up in the dark, ticks by strength. */
    public static final int[] NIGHT_SIGHT = {200, 300, 400, 600};
    /** Extra share of picked-up experience. */
    public static final float[] MORE_XP = {0.25F, 0.5F, 0.75F, 1.0F};
    /** Chance a tick under water costs no air. */
    public static final float[] KEEP_AIR = {0.25F, 0.4F, 0.55F, 0.7F};
    /** Durability mended on every worn piece each {@link ClothesEffects#MEND_TICKS}. */
    public static final int[] MEND = {1, 2, 3, 4};
    /** Chance a blow starts Regeneration. */
    public static final float[] REGEN_CHANCE = {0.15F, 0.2F, 0.25F, 0.3F};
    /** Fire Resistance given when fire touches the robe, ticks. */
    public static final int[] FIRE_SHIELD = {100, 160, 220, 280};
    /** Movement speed bonus (share of the base). */
    public static final float[] SPEED = {0.10F, 0.15F, 0.20F, 0.25F};
    /** Jump strength added (the base is 0.42). */
    public static final float[] JUMP = {0.05F, 0.08F, 0.11F, 0.14F};
    /** Knockback resistance added. */
    public static final float[] STEADY = {0.2F, 0.35F, 0.5F, 0.65F};
    /** Share of fall damage taken away. */
    public static final float[] CUSHION = {0.25F, 0.4F, 0.55F, 0.7F};
    /** Water movement efficiency added (Wavewalk, under the water). */
    public static final float[] SWIM = {0.25F, 0.5F, 0.75F, 1.0F};
    /** Movement speed bonus while walking on water (share of the base). */
    public static final float[] WAVE_SPEED = {0.15F, 0.25F, 0.35F, 0.45F};
    /** Chance a close blow sparks back, the spark's damage and the ticks it numbs the attacker (Slowness IV). */
    public static final float[] STATIC_CHANCE = {0.25F, 0.4F, 0.55F, 0.7F};
    public static final float[] STATIC_DAMAGE = {3, 4, 5, 6};
    public static final int[] STATIC_STUN = {30, 40, 50, 60};
    /** Mirage: ticks unseen, and the ticks before it can happen again. */
    public static final int[] MIRAGE_TICKS = {60, 80, 100, 120};
    public static final int[] MIRAGE_COOLDOWN = {1200, 900, 600, 450};
    /** Mirage wakes when a blow leaves less than this share of health. */
    public static final float MIRAGE_HEALTH = 0.4F;
    /** Chance, every {@link ClothesEffects#POLLEN_TICKS} of walking, that a plant nearby gets bone meal. */
    public static final float[] POLLEN = {0.12F, 0.18F, 0.24F, 0.3F};
    /** Chance, each second, that one bad effect washes off. */
    public static final float[] PURIFY = {0.1F, 0.15F, 0.2F, 0.25F};
    /** Chance a blow glances back, and the damage it does to the attacker. */
    public static final float[] MIRROR_CHANCE = {0.15F, 0.2F, 0.25F, 0.3F};
    public static final float[] MIRROR_DAMAGE = {2, 3, 4, 5};
    /** Blocks around me that Magnet draws loose things from. */
    public static final int[] PULL_RADIUS = {4, 6, 8, 10};
    /** Step height added. */
    public static final float[] STEP = {0.5F, 0.6F, 0.8F, 1.0F};
    /** Share of hot floor damage taken away. */
    public static final float[] COOL_FEET = {0.5F, 0.75F, 1.0F, 1.0F};

    /** The four pieces of the researcher's clothes. */
    public enum Piece {
        HOOD, ROBE, TROUSERS, BOOTS
    }

    private final Piece piece;
    private final ComplexRune rune;

    ClothesRune(Piece piece, String key) {
        this.piece = piece;
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
    }

    public Piece piece() {
        return piece;
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "clothes";
    }

    /** The piece's rune for a codex rune, or null. */
    @Nullable
    public static ClothesRune of(Piece piece, ComplexRune rune) {
        for (ClothesRune value : values()) {
            if (value.piece == piece && value.rune == rune) {
                return value;
            }
        }
        return null;
    }

    public static ClothesRune[] on(Piece piece) {
        return java.util.Arrays.stream(values()).filter(rune -> rune.piece == piece).toArray(ClothesRune[]::new);
    }

    @Override
    public String amountText(int strength) {
        return switch (this) {
            case HOOD_BANISHING -> HolderRune.seconds(HolderRune.at(NIGHT_SIGHT, strength));
            case HOOD_RADIANCE -> HolderRune.percent(HolderRune.at(MORE_XP, strength));
            case HOOD_BREATH -> HolderRune.percent(HolderRune.at(KEEP_AIR, strength));
            case ROBE_MENDING -> Integer.toString(HolderRune.at(MEND, strength));
            case ROBE_HEALING -> HolderRune.percent(HolderRune.at(REGEN_CHANCE, strength));
            case ROBE_FIREPROOFING -> HolderRune.seconds(HolderRune.at(FIRE_SHIELD, strength));
            case TROUSERS_HASTE -> HolderRune.percent(HolderRune.at(SPEED, strength));
            case TROUSERS_WINDMILL -> HolderRune.percent(HolderRune.at(JUMP, strength) / 0.42F);
            case TROUSERS_GRAVITY -> HolderRune.percent(HolderRune.at(STEADY, strength));
            case BOOTS_LIFT -> HolderRune.percent(HolderRune.at(CUSHION, strength));
            case BOOTS_WAVEWALK -> HolderRune.percent(HolderRune.at(WAVE_SPEED, strength));
            case ROBE_STATIC -> HolderRune.percent(HolderRune.at(STATIC_CHANCE, strength));
            case HOOD_MIRAGE -> HolderRune.seconds(HolderRune.at(MIRAGE_TICKS, strength));
            case BOOTS_POLLEN -> HolderRune.percent(HolderRune.at(POLLEN, strength));
            case HOOD_PURIFICATION -> HolderRune.percent(HolderRune.at(PURIFY, strength));
            case ROBE_MIRROR -> HolderRune.percent(HolderRune.at(MIRROR_CHANCE, strength));
            case TROUSERS_SHIFT -> HolderRune.factor(HolderRune.at(STEP, strength));
            case TROUSERS_MAGNET -> Integer.toString(HolderRune.at(PULL_RADIUS, strength));
            case BOOTS_QUENCHING -> HolderRune.percent(HolderRune.at(COOL_FEET, strength));
        };
    }
}
