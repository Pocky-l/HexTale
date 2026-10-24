package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.essence.EssenceTank;

/**
 * The liquids in the Separator's flask, layered like in a separating funnel: the mixture and the rest at the bottom (in
 * the mixture's colour while there is any), the drawn-out concentrate floating on them. Each layer is as high as its
 * share of all three tanks. They fill the flask's clear waist, the window between its frosted cone and shoulders.
 */
public class SeparatorRenderer implements BlockEntityRenderer<SeparatorBlockEntity, SeparatorRenderer.State> {
    // Inside the flask's waist (model-gen FLASK_TIERS: x/z 3-13, y 6-10), inset so no face lies in the glass planes;
    // the top stays under the brass ring so a floating concentrate is always in sight.
    private static final float MIN = 3.5F / 16;
    private static final float MAX = 12.5F / 16;
    private static final float BOTTOM = 6.1F / 16;
    private static final float TOP = 9.9F / 16;
    /** The waist's corners are cut back by a pixel; the liquid follows them. */
    private static final float CUT = 1F / 16;

    public static final class State extends BlockEntityRenderState {
        float mix;
        float extract;
        int mixColor;
        int extractColor;
        float time;
        long seed;
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
        EssenceTank rest = separator.rest();
        float room = mix.capacity() + extract.capacity() + rest.capacity();
        state.mix = (mix.amount() + rest.amount()) / room;
        state.extract = extract.isEmpty() ? 0 : extract.amount() / room;
        state.mixColor = !mix.isEmpty() ? mix.contents().color() : rest.isEmpty() ? 0 : rest.contents().color();
        state.extractColor = extract.isEmpty() ? 0 : extract.contents().color();
        state.time = separator.getLevel() == null ? 0 : separator.getLevel().getGameTime() + partialTick;
        state.seed = separator.getBlockPos().asLong();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float height = TOP - BOTTOM;
        float mixTop = BOTTOM + height * Math.min(1, state.mix);
        float extractTop = Math.min(TOP, mixTop + height * state.extract);
        if (state.mix > 0) {
            LiquidBox.submitCutCorners(poseStack, collector, MIN, MIN, MAX, MAX, CUT, BOTTOM, mixTop, RuneDrawing.argb(state.mixColor, 0.78F),
                    state.time, state.seed, 2);
        }
        if (state.extract > 0) {
            // A hair above the mixture's surface, so the two layers never share a plane.
            LiquidBox.submitCutCorners(poseStack, collector, MIN, MIN, MAX, MAX, CUT, mixTop + 0.1F / 16, extractTop,
                    RuneDrawing.argb(state.extractColor, 0.85F), state.time, state.seed, 0);
        }
    }
}
