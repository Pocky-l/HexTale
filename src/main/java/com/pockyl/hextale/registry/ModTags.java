package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.pockyl.hextale.Hextale;

public final class ModTags {
    /** Blocks that lightning, led in by a lightning rod standing on them, shatters into unstable hextech. */
    public static final TagKey<Block> AWAKENS_INTO_HEXTECH = TagKey.create(Registries.BLOCK, Hextale.id("awakens_into_hextech"));
    /** Blocks whose nearness to a lightning strike is worth a diary note. */
    public static final TagKey<Block> RESONANT_CRYSTALS = TagKey.create(Registries.BLOCK, Hextale.id("resonant_crystals"));

    private ModTags() {
    }
}
