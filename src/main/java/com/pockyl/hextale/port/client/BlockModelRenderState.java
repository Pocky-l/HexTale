package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * A block model resolved for drawing on its own (26.x); on 1.21.1 the block state, drawn by the block renderer in the
 * render types of its model.
 *
 * <p>port-of: net.minecraft.client.renderer.block.BlockModelRenderState
 */
public class BlockModelRenderState {
    BlockState blockState = Blocks.AIR.defaultBlockState();

    public void submitMultiLayer(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outlineColor) {
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockState, poseStack, collector.buffers(), light, overlay,
                ModelData.EMPTY, null);
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outlineColor) {
        submitMultiLayer(poseStack, collector, light, overlay, outlineColor);
    }
}
