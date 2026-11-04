package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.garden.EssenceSprinklerBlockEntity;

/** The Essence Sprinkler's head: still between sprays, it whirls round twice after each and slows to a stop. */
public class EssenceSprinklerRenderer implements BlockEntityRenderer<EssenceSprinklerBlockEntity, EssenceSprinklerRenderer.State> {
    /** The riser's axis, in block units. */
    private static final float AXIS = 0.5F;

    public static final class State extends BlockEntityRenderState {
        float spin;
    }

    public EssenceSprinklerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EssenceSprinklerBlockEntity sprinkler, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(sprinkler, state, partialTick, cameraPosition, breakProgress);
        state.spin = sprinkler.spin(partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(AXIS, 0, AXIS);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
        poseStack.translate(-AXIS, 0, -AXIS);
        TableModels.submit(poseStack, collector, TableModels.SPRINKLER_HEAD, state.lightCoords);
        poseStack.popPose();
    }
}
