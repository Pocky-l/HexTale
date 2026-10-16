package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import com.pockyl.hextale.Hextale;

public final class ModTags {
    /** Blocks that lightning, led in by a lightning rod standing on them, shatters into unstable hextech. */
    public static final TagKey<Block> AWAKENS_INTO_HEXTECH = TagKey.create(Registries.BLOCK, Hextale.id("awakens_into_hextech"));
    /** What repairs hextech tools. */
    public static final TagKey<Item> HEXTECH_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Hextale.id("hextech_tool_materials"));
    /** Blocks whose nearness to a lightning strike is worth a diary note. */

    private ModTags() {
    }
}
