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

import com.pockyl.hextale.garden.ChemPlanterBlockEntity;

/** The brew in a Chem Planter's tank, seen through its glass sides: its level and colour, gently bubbling. */
public class ChemPlanterRenderer implements BlockEntityRenderer<ChemPlanterBlockEntity, ChemPlanterRenderer.State> {
    /** The hollow behind the glass, in block units (the model leaves it free). */
    public static final float MIN = 2.01F / 16;
    public static final float MAX = 13.99F / 16;
    public static final float BOTTOM = 5.01F / 16;
    public static final float TOP = 10.9F / 16;

    public static final class State extends BlockEntityRenderState {
        float fill;
        int color;
        float time;
        long seed;
    }

    public ChemPlanterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChemPlanterBlockEntity planter, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(planter, state, partialTick, cameraPosition, breakProgress);
        state.fill = planter.fill();
        state.color = planter.dose().color();
        state.time = planter.getLevel() == null ? 0 : planter.getLevel().getGameTime() + partialTick;
        state.seed = planter.getBlockPos().asLong();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fill <= 0) {
            return;
        }
        float top = BOTTOM + (TOP - BOTTOM) * state.fill;
        LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, BOTTOM, top, RuneDrawing.argb(state.color, 0.8F), state.time, state.seed, 3);
    }
}
