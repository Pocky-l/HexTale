package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.entity.DustCharge;

/**
 * A Dust Charge: in flight the item sprite; stuck, the paper cartridge model lying with its base on the block face it hit
 * (built with the base on its -Z face at (8, 8, 0) px, pointing out along +Z).
 */
public class DustChargeRenderer extends ThrownItemRenderer<DustCharge> {
    public DustChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static final class State extends ThrownItemRenderState {
        Direction stuck;
    }

    @Override
    public ThrownItemRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DustCharge charge, ThrownItemRenderState state, float partialTicks) {
        super.extractRenderState(charge, state, partialTicks);
        ((State) state).stuck = charge.lit() ? charge.stuckFace() : null;
    }

    @Override
    public void submit(ThrownItemRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Direction face = ((State) state).stuck;
        if (face == null) {
            super.submit(state, poseStack, collector, camera);
            return;
        }
        Vector3f out = face.step();
        poseStack.pushPose();
        // The entity hangs 0.1 off the face; the cartridge's base lies on the face itself.
        poseStack.translate(-out.x * 0.1F, -out.y * 0.1F, -out.z * 0.1F);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), out));
        poseStack.translate(-0.5F, -0.5F, 0);
        TableModels.submit(poseStack, collector, TableModels.DUST_CHARGE_STUCK, state.lightCoords);
        poseStack.popPose();
    }
}
