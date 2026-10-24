package com.pockyl.hextale.port.client;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Fills {@link BlockModelRenderState}s like the 26.x resolver.
 *
 * <p>port-of: net.minecraft.client.renderer.block.BlockModelResolver
 */
public final class BlockModelResolver {
    public static final BlockModelResolver INSTANCE = new BlockModelResolver();

    public void update(BlockModelRenderState output, BlockState state, BlockDisplayContext context) {
        output.blockState = state;
    }
}
