package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.essence.EssenceTank;

/**
 * The liquids in the Separator's glass vessel, layered like in a separating funnel: the mixture at the bottom and the
 * drawn-out concentrate floating on it. The vessel holds both tanks; each layer is as high as its share of them.
 */
public class SeparatorRenderer implements BlockEntityRenderer<SeparatorBlockEntity, SeparatorRenderer.State> {
    // Inside the vessel glass (model: x/z 3-13, y 1-12), inset so no face lies in the glass planes.
    private static final float MIN = 3.5F / 16;
    private static final float MAX = 12.5F / 16;
    private static final float BOTTOM = 1.1F / 16;
    private static final float TOP = 11.9F / 16;

    public static final class State extends BlockEntityRenderState {
        float mix;
        float extract;
        int mixColor;
        int extractColor;
    }

    public SeparatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SeparatorBlockEntity separator, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(separator, state, partialTick, cameraPosition, breakProgress);
        EssenceTank mix = separator.mix();
        EssenceTank extract = separator.extract();
        float room = mix.capacity() + extract.capacity();
        state.mix = mix.isEmpty() ? 0 : mix.amount() / room;
        state.extract = extract.isEmpty() ? 0 : extract.amount() / room;
        state.mixColor = mix.isEmpty() ? 0 : mix.contents().color();
        state.extractColor = extract.isEmpty() ? 0 : extract.contents().color();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float height = TOP - BOTTOM;
        float mixTop = BOTTOM + height * Math.min(1, state.mix);
        float extractTop = Math.min(TOP, mixTop + height * state.extract);
        if (state.mix > 0) {
            LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, BOTTOM, mixTop, RuneDrawing.argb(state.mixColor, 0.5F));
        }
        if (state.extract > 0) {
            // A hair above the mixture's surface, so the two layers never share a plane.
            LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, mixTop + 0.1F / 16, extractTop, RuneDrawing.argb(state.extractColor, 0.7F));
        }
    }
}
