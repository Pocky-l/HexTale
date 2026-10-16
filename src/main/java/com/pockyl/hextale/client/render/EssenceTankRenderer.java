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

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.essence.EssenceTank;

/**
 * The essence inside an Essence Tank: a glowing liquid between the brass base and cap, as high as the tank is full.
 */
public class EssenceTankRenderer implements BlockEntityRenderer<EssenceTankBlockEntity, EssenceTankRenderer.State> {
    // Inside the posts and glass walls (model: posts 0-2 / 14-16, walls at 1 and 15, base top 2, cap bottom 14),
    // kept a little off the base so the faces never lie in one plane.
    private static final float MIN = 2F / 16;
    private static final float MAX = 14F / 16;
    private static final float BOTTOM = 2.1F / 16;
    private static final float TOP = 13.9F / 16;

    public static final class State extends BlockEntityRenderState {
        float fill;
        int color;
    }

    public EssenceTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EssenceTankBlockEntity tankEntity, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tankEntity, state, partialTick, cameraPosition, breakProgress);
        EssenceTank tank = tankEntity.tank();
        state.fill = tank.isEmpty() ? 0 : tank.amount() / (float) tank.capacity();
        state.color = tank.isEmpty() ? 0 : tank.contents().color();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fill <= 0) {
            return;
        }
        float top = BOTTOM + (TOP - BOTTOM) * Math.min(1, state.fill);
        LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, BOTTOM, top, RuneDrawing.argb(state.color, 0.55F));
    }
}
