package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import com.pockyl.hextale.Hextale;

public final class ModTags {
    /** Blocks that lightning, led in by a lightning rod standing on them, shatters into unstable hextech. */
    public static final TagKey<Block> AWAKENS_INTO_HEXTECH = TagKey.create(Registries.BLOCK, Hextale.id("awakens_into_hextech"));
    /** What a Dust Charge cracks: rock in the ground, ores, gravel - never builds. */
    public static final TagKey<Block> BLASTABLE = TagKey.create(Registries.BLOCK, Hextale.id("blastable"));
    /** What an untuned Tuning Fork hears: hextech in the ground (dormant hextech, woken crystals, budding amethyst). */
    public static final TagKey<Block> RESONANT = TagKey.create(Registries.BLOCK, Hextale.id("resonant"));
    /** Blocks a lightning wave runs through: everything that awakens plus budding amethyst, which is never shattered. */
    public static final TagKey<Block> CARRIES_LIGHTNING = TagKey.create(Registries.BLOCK, Hextale.id("carries_lightning"));
    /** What repairs hextech tools. */
    public static final TagKey<Item> HEXTECH_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Hextale.id("hextech_tool_materials"));
    /** What repairs hexsteel tools and armour. */
    public static final TagKey<Item> REPAIRS_HEXSTEEL = TagKey.create(Registries.ITEM, Hextale.id("repairs_hexsteel"));
    /** Tools and worn pieces of the refined tier: two rune places ({@code runework/RuneTier}). */
    public static final TagKey<Item> REFINED_TIER = TagKey.create(Registries.ITEM, Hextale.id("rune_tier/refined"));
    /** Tools and worn pieces of the radiant tier: three rune places. */
    public static final TagKey<Item> RADIANT_TIER = TagKey.create(Registries.ITEM, Hextale.id("rune_tier/radiant"));
    /** Tools and worn pieces forged with the Void: four rune places. */
    public static final TagKey<Item> RIFTFORGED_TIER = TagKey.create(Registries.ITEM, Hextale.id("rune_tier/riftforged"));
    public static final TagKey<Item> REPAIRS_ARCANE_CLOTH = TagKey.create(Registries.ITEM, Hextale.id("repairs_arcane_cloth"));
    /** Stone that drinks spilled essence and becomes magic stone (block tag; the item tag of the same name feeds the
     *  crafting recipe). */
    public static final TagKey<Block> SOAKS_ESSENCE = TagKey.create(Registries.BLOCK, Hextale.id("soaks_essence"));
    /** Plants a Whirl Reaper reaps in place by their age, besides every crop block: nether wart, cocoa, sweet berries. */
    public static final TagKey<Block> WHIRL_CROPS = TagKey.create(Registries.BLOCK, Hextale.id("whirl_crops"));
    /** Fruits on a stem: a Whirl Reaper lifts them off when a stem holds them. */
    public static final TagKey<Block> WHIRL_GOURDS = TagKey.create(Registries.BLOCK, Hextale.id("whirl_gourds"));
    /** Plants that grow in a column: a Whirl Reaper cuts all but the lowest block. */
    public static final TagKey<Block> WHIRL_STALKS = TagKey.create(Registries.BLOCK, Hextale.id("whirl_stalks"));
    /** Plain item storages a Sorting Matrix sorts into by what they hold (and never fills with what they do not). */
    public static final TagKey<Block> SORTING_STORAGE = TagKey.create(Registries.BLOCK, Hextale.id("sorting_storage"));

    /** Stone a splash of acid eats away (ores and everything else stay). */
    /** Ground the Void has touched: its flora roots only here. */
    public static final TagKey<Block> VOID_GROUND = TagKey.create(Registries.BLOCK, Hextale.id("void_ground"));
    /** Blocks the world's own magic never touches: the blight, the land healing, a Hexcore's lightning. */
    public static final TagKey<Block> VOID_IMMUNE = TagKey.create(Registries.BLOCK, Hextale.id("void_immune"));
    /** Natural ground that blighted land turns into Void Soil / Void Rock. */
    public static final TagKey<Block> BLIGHTS_TO_SOIL = TagKey.create(Registries.BLOCK, Hextale.id("blights_to_soil"));
    public static final TagKey<Block> BLIGHTS_TO_ROCK = TagKey.create(Registries.BLOCK, Hextale.id("blights_to_rock"));
    public static final TagKey<Block> ACID_DISSOLVES = TagKey.create(Registries.BLOCK, Hextale.id("acid_dissolves"));
    /** Catalysts of the combustible mix (coal and charcoal by default). */
    public static final TagKey<Item> FUEL_CATALYSTS = TagKey.create(Registries.ITEM, Hextale.id("fuel_catalysts"));
    /** What the Research Table takes as parchment (paper by default). */
    public static final TagKey<Item> RESEARCH_PAPER = TagKey.create(Registries.ITEM, Hextale.id("research_paper"));
    /** What burns in the Separator's fuel slot (blaze powder by default). */
    public static final TagKey<Item> SEPARATOR_FUELS = TagKey.create(Registries.ITEM, Hextale.id("separator_fuels"));

    /** Every item of a tag as one stack each, for the guide and JEI (empty before tags load). */
    public static java.util.List<net.minecraft.world.item.ItemStack> stacks(TagKey<Item> tag) {
        java.util.List<net.minecraft.world.item.ItemStack> out = new java.util.ArrayList<>();
        for (net.minecraft.core.Holder<Item> item : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            out.add(new net.minecraft.world.item.ItemStack(item));
        }
        return out;
    }

    private ModTags() {
    }
}
