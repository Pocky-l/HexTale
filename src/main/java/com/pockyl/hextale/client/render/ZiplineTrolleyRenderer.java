package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import com.pockyl.hextale.entity.ZiplineTrolley;

/**
 * The trolley hook: two grooved wheels riding the chain, side plates and the grip bar the rider holds. The standalone
 * model is built with the chain running along Z through its centre; it is turned along the line and its wheels spin
 * with the speed.
 */
public class ZiplineTrolleyRenderer extends EntityRenderer<ZiplineTrolley, ZiplineTrolleyRenderer.State> {
    public ZiplineTrolleyRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    public static class State extends EntityRenderState {
        Vec3 direction = new Vec3(0, 0, -1);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ZiplineTrolley trolley, State state, float partialTick) {
        super.extractRenderState(trolley, state, partialTick);
        Vector3fc dir = trolley.direction();
        Vec3 d = new Vec3(dir.x(), dir.y(), dir.z());
        state.direction = d.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, -1) : d.normalize();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0, ZiplineTrolley.BELOW_CHAIN, 0);
        poseStack.rotate(ChainAnchorRenderer.turn(state.direction, new Vec3(0, 1, 0)));
        poseStack.translate(-0.5, -0.5, -0.5);
        TableModels.submit(poseStack, collector, TableModels.ZIPLINE_TROLLEY, state.lightCoords);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
