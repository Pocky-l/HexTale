package com.pockyl.hextale.port;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BushBlock;

/**
 * A plant that stands on a block below it; Minecraft 1.21.1 calls it a bush.
 *
 * <p>port-of: net.minecraft.world.level.block.VegetationBlock
 */
public abstract class VegetationBlock extends BushBlock {
    protected VegetationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " has no type codec");
    }
}
