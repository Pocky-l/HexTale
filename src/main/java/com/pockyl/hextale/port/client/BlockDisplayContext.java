package com.pockyl.hextale.port.client;

/**
 * Where a block model is shown (26.x); 1.21.1 has one way to draw a lone block, so it carries nothing.
 *
 * <p>port-of: net.minecraft.client.renderer.block.model.BlockDisplayContext
 */
public final class BlockDisplayContext {
    private BlockDisplayContext() {
    }

    public static BlockDisplayContext create() {
        return new BlockDisplayContext();
    }
}
