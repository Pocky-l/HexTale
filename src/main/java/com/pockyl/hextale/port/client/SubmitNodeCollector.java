package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FormattedCharSequence;

/**
 * The 26.x deferred draw collector over a 1.21.1 {@link MultiBufferSource}: every submit draws at once into the
 * buffer of its render type, which the level renderer flushes in render type order as before.
 *
 * <p>port-of: net.minecraft.client.renderer.SubmitNodeCollector
 */
public final class SubmitNodeCollector {
    private final MultiBufferSource buffers;

    public SubmitNodeCollector(MultiBufferSource buffers) {
        this.buffers = buffers;
    }

    public MultiBufferSource buffers() {
        return buffers;
    }

    public VertexConsumer buffer(RenderType type) {
        return buffers.getBuffer(type);
    }

    public void submitCustomGeometry(PoseStack poseStack, RenderType type, CustomGeometryRenderer renderer) {
        renderer.render(poseStack.last(), buffers.getBuffer(type));
    }

    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean dropShadow,
                           Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        Font font = Minecraft.getInstance().font;
        if (outlineColor != 0) {
            font.drawInBatch8xOutline(text, x, y, color, outlineColor, poseStack.last().pose(), buffers, lightCoords);
        } else {
            font.drawInBatch(text, x, y, color, dropShadow, poseStack.last().pose(), buffers, displayMode, backgroundColor, lightCoords);
        }
    }

    /** An entity model posed from its state, drawn cut out with its texture. */
    public <S> void submitModel(EntityModel<S> model, S state, PoseStack poseStack, net.minecraft.resources.ResourceLocation texture,
                                int lightCoords, int overlayCoords, int outlineColor) {
        model.setupAnim(state);
        model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), lightCoords, overlayCoords, -1);
    }

    /** Like vanilla's falling block renderer: the block's model in its own render types, lit by the client level. */
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState state, int outlineColor) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || state.blockState.getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) {
            return;
        }
        var dispatcher = minecraft.getBlockRenderer();
        var model = dispatcher.getBlockModel(state.blockState);
        long seed = state.blockState.getSeed(state.randomSeedPos);
        for (RenderType type : model.getRenderTypes(state.blockState, net.minecraft.util.RandomSource.create(seed),
                net.neoforged.neoforge.client.model.data.ModelData.EMPTY)) {
            dispatcher.getModelRenderer().tesselateBlock(minecraft.level, model, state.blockState, state.blockPos, poseStack,
                    buffers.getBuffer(net.neoforged.neoforge.client.RenderTypeHelper.getMovingBlockRenderType(type)), false,
                    net.minecraft.util.RandomSource.create(), seed, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    net.neoforged.neoforge.client.model.data.ModelData.EMPTY, type);
        }
    }

    @FunctionalInterface
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer buffer);
    }
}
