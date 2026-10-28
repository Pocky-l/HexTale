package com.pockyl.hextale.runework;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * The runes the hextech axe, shovel and sword take, like the pickaxe's: up to {@link PickaxeRunes#SLOTS} burned in at
 * the Hextech Altar, kept in the same {@code hextale:pickaxe_runes} component. Each rune belongs to the tools it names
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
    /** A blow may jump on as lightning to the creatures around. */
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
    ENGRAVING("light_metal", Tool.SWORD);

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
    public static final float[] ARC_CHANCE = {0.10F, 0.15F, 0.20F, 0.25F};
    public static final int[] ARC_TARGETS = {2, 3, 4, 5};
    public static final float[] ARC_DAMAGE = {3F, 4F, 5F, 6F};
    public static final int[] LOOTING = {1, 2, 3, 4};
    public static final float[] EXPERIENCE = {1.5F, 2F, 2.5F, 3F};
    public static final float[] RAGE = {0.2F, 0.3F, 0.4F, 0.5F};
    public static final int[] RAGE_TICKS = {120, 160, 200, 240};
    public static final float[] FIRST_STRIKE = {0.3F, 0.5F, 0.75F, 1.0F};
    public static final float[] HEAD = {0.01F, 0.02F, 0.03F, 0.04F};

    private final ComplexRune rune;
    private final Tool[] tools;

    ToolRune(String key, Tool... tools) {
        this.rune = Objects.requireNonNull(ComplexRunes.byKey(key), key);
        this.tools = tools;
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
        };
    }

    /** The three hextech tools besides the pickaxe. */
    public enum Tool {
        AXE,
        SHOVEL,
        SWORD
    }
}
