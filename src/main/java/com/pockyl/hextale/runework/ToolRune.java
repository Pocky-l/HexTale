package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes the hextech axe, shovel and sword take, like the pickaxe's: as many as the tool's tier holds
 * ({@link RuneTier}), burned in at the Rune Circle, kept in the same {@code hextale:pickaxe_runes} component. Each rune belongs to the tools it names
 * (Mending and Haste to more than one). Strength = stars (1-3). The effects live in {@link ToolEffects}.
 */
public enum ToolRune implements HolderRune {
    // Shared
    /** Wear may be shrugged off and mended a point. */
    MENDING("life_metal", Tool.AXE, Tool.SHOVEL, Tool.SWORD),
    /** A chance of Haste while chopping or digging. */
    HASTE("spark_life", Tool.AXE, Tool.SHOVEL),
    // Axe
    /** The whole tree comes down at one blow, its leaves too (sneak for one block). */
    GRAVITY("earth_air", Tool.AXE),
    /** A tree cut at its foot is planted again. */
    GROWTH("life_earth", Tool.AXE),
    /** Leaves fall as with Fortune: more saplings, sticks and apples. */
    POLLEN("life_air", Tool.AXE),
    /** A blow may eat through armour: more damage, the armour wears. */
    CORROSION("water_metal", Tool.AXE),
    // Shovel
    /** Soil, sand and gravel may hide a treasure. */
    CACHE("earth_shadow", Tool.SHOVEL),
    /** Digs 3 x 3 (sneak for one block). */
    EROSION("water_earth", Tool.SHOVEL),
    /** Grass and soil give seeds, bulbs and saplings. */
    FERTILITY("earth_life", Tool.SHOVEL),
    // Sword
    /** A blow may heal the wielder. */
    WITHERING("shadow_life", Tool.SWORD),
    /** A blow may let out a spark: it bites the struck creature and leaps on to the ones around. */
    CHAIN_LIGHTNING("spark_air", Tool.SWORD),
    /** More loot from creatures, as with Looting. */
    SPOILS("earth_shadow", Tool.SWORD),
    /** More experience from creatures. */
    RADIANCE("life_light", Tool.SWORD),
    /** A kill may fan the wielder's strength. */
    BELLOWS("air_fire", Tool.SWORD),
    /** The first blow on an unhurt creature strikes harder. */
    FLASH("spark_light", Tool.SWORD),
    /** A kill may leave the creature's head. */
    ENGRAVING("light_metal", Tool.SWORD),
    // Weapon powers: one per weapon (they clash), worked by hand
    /** A charged swing at the ground sends a wave through it: the ground heaves, creatures on its path are hurt and tossed. */
    SHOCKWAVE("earth_spark", true, Tool.AXE, Tool.SWORD),
    /** Right click at a creature: a step through the shadow to its back and a blow. */
    BLINK("spark_shadow", true, Tool.SWORD),
    // Sword, in battle
    /** A creature it kills bursts, hurting the ones around. */
    DETONATOR("spark_fire", Tool.SWORD),
    /** A blow may open a whirlpool at the struck creature that drags the others in. */
    WHIRLPOOL("water_shadow", Tool.SWORD),
    /** Every third blow in a row sweeps all round in a circle of light. */
    WHIRLWIND("air_spark", Tool.SWORD),
    // Blades (the Tendril Dagger and the Strike Gauntlet take the sword's runes too)
    /** A blow from behind strikes harder. */
    BACKSTAB("shadow_light", Tool.SWORD),
    /** A blow may open a wound that keeps bleeding. */
    BLEEDING("shadow_metal", Tool.SWORD);

    /** Chance of Haste per block (shared with the pickaxe's). */
    public static final float[] HASTE_CHANCE = {0.04F, 0.07F, 0.10F, 0.13F};
    public static final int HASTE_TICKS = 160;
    public static final float[] MEND = {0.10F, 0.18F, 0.25F, 0.32F};
    public static final int[] TREE = {32, 64, 128, 256};
    public static final float[] FREE_SAPLING = {0.25F, 0.5F, 0.75F, 1.0F};
    public static final int[] LEAF_FORTUNE = {1, 2, 3, 4};
    public static final float[] CORRODE = {0.10F, 0.18F, 0.25F, 0.32F};
    public static final float[] TREASURE = {0.02F, 0.035F, 0.05F, 0.065F};
    public static final float[] SEEDS = {0.05F, 0.08F, 0.11F, 0.14F};
    public static final float[] HEAL_CHANCE = {0.10F, 0.15F, 0.20F, 0.25F};
    public static final int[] HEAL = {2, 2, 3, 4};
    public static final float[] ARC_CHANCE = {0.20F, 0.30F, 0.40F, 0.50F};
    public static final int[] ARC_TARGETS = {2, 3, 4, 5};
    public static final float[] ARC_DAMAGE = {3F, 4F, 5F, 6F};
    public static final int[] LOOTING = {1, 2, 3, 4};
    public static final float[] EXPERIENCE = {1.5F, 2F, 2.5F, 3F};
    public static final float[] RAGE = {0.2F, 0.3F, 0.4F, 0.5F};
    public static final int[] RAGE_TICKS = {120, 160, 200, 240};
    public static final float[] FIRST_STRIKE = {0.3F, 0.5F, 0.75F, 1.0F};
    /** Backstab: extra damage of a blow from behind. */
    public static final float[] BACK_BONUS = {0.25F, 0.4F, 0.6F, 0.8F};
    /** Bleeding: chance a blow opens a wound, and how long it bleeds (ticks). */
    public static final float[] BLEED_CHANCE = {0.2F, 0.3F, 0.4F, 0.5F};
    public static final int[] BLEED_TICKS = {60, 80, 100, 120};
    public static final float[] HEAD = {0.05F, 0.10F, 0.15F, 0.20F};
    /** Shockwave: blocks it runs, damage, half width (0 = one block wide), ticks the weapon rests after it. */
    public static final int[] WAVE_LENGTH = {6, 8, 10, 12};
    public static final float[] WAVE_DAMAGE = {4F, 5.5F, 7F, 8F};
    public static final int[] WAVE_HALF_WIDTH = {0, 0, 1, 1};
    public static final int[] WAVE_REST = {50, 40, 30, 25};
    /** Blink: reach in blocks, the blow's bonus, rest ticks. */
    public static final int[] BLINK_RANGE = {8, 11, 14, 16};
    public static final float[] BLINK_BONUS = {0.3F, 0.5F, 0.75F, 1.0F};
    public static final int[] BLINK_REST = {80, 65, 50, 40};
    /** Detonator: blast radius and damage. */
    public static final float[] BLAST_RADIUS = {2.5F, 3F, 3.5F, 4F};
    public static final float[] BLAST_DAMAGE = {3F, 5F, 7F, 8F};
    /** Whirlpool: chance per blow, pull radius, ticks it lasts. */
    public static final float[] VORTEX_CHANCE = {0.12F, 0.18F, 0.25F, 0.3F};
    public static final float[] VORTEX_RADIUS = {4F, 5F, 6F, 6F};
    public static final int[] VORTEX_TICKS = {40, 50, 60, 60};
    /** Whirlwind: the sweep's reach and damage. */
    public static final float[] SWEEP_RADIUS = {3F, 3.5F, 4F, 4.5F};
    public static final float[] SWEEP_DAMAGE = {3F, 4.5F, 6F, 7F};

    private final ComplexRune rune;
    private final boolean power;
    private final Tool[] tools;

    ToolRune(String key, Tool... tools) {
        this(key, false, tools);
    }

    ToolRune(String key, boolean power, Tool... tools) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
        this.power = power;
        this.tools = tools;
    }

    /** Whether it is worked by hand (a swing at the ground, a right click): a weapon takes one such rune. */
    public boolean power() {
        return power;
    }

    /** Whether the two runes cannot share a tool: two powers. */
    public boolean clashes(ToolRune other) {
        return other != this && power && other.power;
    }

    @Override
    public ComplexRune rune() {
        return rune;
    }

    @Override
    public String prefix() {
        return "tool";
    }

    public boolean on(Tool tool) {
        for (Tool t : tools) {
            if (t == tool) {
                return true;
            }
        }
        return false;
    }

    /** The rune of this tool that is this codex rune, or null. */
    @Nullable
    public static ToolRune of(Tool tool, ComplexRune rune) {
        for (ToolRune value : values()) {
            if (value.rune == rune && value.on(tool)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String amountText(int strength) {
        int i = Mth.clamp(strength, 1, 4);
        return switch (this) {
            case MENDING -> HolderRune.percent(HolderRune.at(MEND, i));
            case HASTE -> HolderRune.percent(HolderRune.at(HASTE_CHANCE, i));
            case GRAVITY -> Integer.toString(HolderRune.at(TREE, i));
            case GROWTH -> HolderRune.percent(HolderRune.at(FREE_SAPLING, i));
            case POLLEN -> HolderRune.roman(HolderRune.at(LEAF_FORTUNE, i));
            case CORROSION -> HolderRune.percent(HolderRune.at(CORRODE, i));
            case CACHE -> HolderRune.percent(HolderRune.at(TREASURE, i));
            case EROSION -> i >= 3 ? "3×3×2" : "3×3";
            case FERTILITY -> HolderRune.percent(HolderRune.at(SEEDS, i));
            case WITHERING -> HolderRune.percent(HolderRune.at(HEAL_CHANCE, i));
            case CHAIN_LIGHTNING -> HolderRune.percent(HolderRune.at(ARC_CHANCE, i));
            case SPOILS -> HolderRune.roman(HolderRune.at(LOOTING, i));
            case RADIANCE -> HolderRune.factor(HolderRune.at(EXPERIENCE, i));
            case BELLOWS -> HolderRune.percent(HolderRune.at(RAGE, i));
            case FLASH -> "+" + HolderRune.percent(HolderRune.at(FIRST_STRIKE, i));
            case ENGRAVING -> HolderRune.percent(HolderRune.at(HEAD, i));
            case SHOCKWAVE -> Integer.toString(HolderRune.at(WAVE_LENGTH, i));
            case BLINK -> Integer.toString(HolderRune.at(BLINK_RANGE, i));
            case DETONATOR -> HolderRune.factor(HolderRune.at(BLAST_RADIUS, i));
            case WHIRLPOOL -> HolderRune.percent(HolderRune.at(VORTEX_CHANCE, i));
            case WHIRLWIND -> HolderRune.factor(HolderRune.at(SWEEP_RADIUS, i));
            case BACKSTAB -> "+" + HolderRune.percent(HolderRune.at(BACK_BONUS, i));
            case BLEEDING -> HolderRune.percent(HolderRune.at(BLEED_CHANCE, i));
        };
    }

    /** The three hextech tools besides the pickaxe. */
    public enum Tool {
        AXE,
        SHOVEL,
        SWORD
    }
}
