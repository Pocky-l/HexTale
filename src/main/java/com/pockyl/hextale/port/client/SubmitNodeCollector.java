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

    @FunctionalInterface
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer buffer);
    }
}
