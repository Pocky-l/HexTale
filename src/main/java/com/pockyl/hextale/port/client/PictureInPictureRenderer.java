package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A 26.x picture-in-picture renderer drawn straight into the GUI on 1.21.1: the scene goes under the same transform
 * 26.x uses for its texture (box centre, {@code scale} GUI pixels per block, z towards the viewer), clipped to the box
 * and lifted in front of the screen behind it.
 *
 * <p>port-of: net.minecraft.client.gui.render.pip.PictureInPictureRenderer
 */
public abstract class PictureInPictureRenderer<T extends PictureInPictureRenderState> {
    private static final Map<Class<?>, PictureInPictureRenderer<?>> RENDERERS = new HashMap<>();
    /** How far in front of the GUI behind it the scene sits. */
    private static final float DEPTH = 150;

    public abstract Class<T> getRenderStateClass();

    protected abstract void renderToTexture(T state, PoseStack poseStack, SubmitNodeCollector collector);

    protected abstract String getTextureLabel();

    protected float getTranslateY(int height, int guiScale) {
        return height;
    }

    /** What 26.x's {@code RegisterPictureInPictureRenderersEvent#register} did. */
    public static <T extends PictureInPictureRenderState> void register(Class<T> type, Supplier<? extends PictureInPictureRenderer<T>> factory) {
        RENDERERS.put(type, factory.get());
    }

    @SuppressWarnings("unchecked")
    static <T extends PictureInPictureRenderState> void draw(GuiGraphics graphics, T state) {
        PictureInPictureRenderer<T> renderer = (PictureInPictureRenderer<T>) RENDERERS.get(state.getClass());
        if (renderer == null) {
            return;
        }
        int guiScale = (int) Minecraft.getInstance().getWindow().getGuiScale();
        int width = state.x1() - state.x0();
        int height = state.y1() - state.y0();
        graphics.flush();
        graphics.enableScissor(state.x0(), state.y0(), state.x1(), state.y1());
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(state.x0() + width / 2.0F, state.y0() + renderer.getTranslateY(height * guiScale, guiScale) / guiScale, DEPTH);
        poseStack.scale(state.scale(), state.scale(), -state.scale());
        renderer.renderToTexture(state, poseStack, new SubmitNodeCollector(graphics.bufferSource()));
        graphics.flush();
        poseStack.popPose();
        graphics.disableScissor();
        Lighting.setupFor3DItems();
    }
}
