package com.pockyl.hextale.port;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.MultifaceSpreader;

/**
 * A plain multiface block (patches on block faces, no spreading), which Minecraft 26.x can instantiate directly.
 *
 * <p>port-of: net.minecraft.world.level.block.MultifaceBlock
 */
public class MultifaceBlock extends net.minecraft.world.level.block.MultifaceBlock {
    private static final MapCodec<MultifaceBlock> CODEC = simpleCodec(MultifaceBlock::new);
    private final MultifaceSpreader spreader = new MultifaceSpreader(this);

    public MultifaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends MultifaceBlock> codec() {
        return CODEC;
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return spreader;
    }
}
