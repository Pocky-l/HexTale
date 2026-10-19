package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import com.pockyl.hextale.Hextale;

public final class ModTags {
    /** Blocks that lightning, led in by a lightning rod standing on them, shatters into unstable hextech. */
    public static final TagKey<Block> AWAKENS_INTO_HEXTECH = TagKey.create(Registries.BLOCK, Hextale.id("awakens_into_hextech"));
    /** Blocks a lightning wave runs through: everything that awakens plus budding amethyst, which is never shattered. */
    public static final TagKey<Block> CARRIES_LIGHTNING = TagKey.create(Registries.BLOCK, Hextale.id("carries_lightning"));
    /** What repairs hextech tools. */
    public static final TagKey<Item> HEXTECH_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Hextale.id("hextech_tool_materials"));
    /** Stone that drinks spilled essence and becomes magic stone (block tag; the item tag of the same name feeds the
     *  crafting recipe). */
    public static final TagKey<Block> SOAKS_ESSENCE = TagKey.create(Registries.BLOCK, Hextale.id("soaks_essence"));
    /** Plants a Whirl Reaper reaps in place by their age, besides every crop block: nether wart, cocoa, sweet berries. */
    public static final TagKey<Block> WHIRL_CROPS = TagKey.create(Registries.BLOCK, Hextale.id("whirl_crops"));
    /** Fruits on a stem: a Whirl Reaper lifts them off when a stem holds them. */
    public static final TagKey<Block> WHIRL_GOURDS = TagKey.create(Registries.BLOCK, Hextale.id("whirl_gourds"));
    /** Plants that grow in a column: a Whirl Reaper cuts all but the lowest block. */
    public static final TagKey<Block> WHIRL_STALKS = TagKey.create(Registries.BLOCK, Hextale.id("whirl_stalks"));

    private ModTags() {
    }
}
