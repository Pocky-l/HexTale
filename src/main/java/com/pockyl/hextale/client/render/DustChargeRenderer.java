package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
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

    /** 1.21.1: drawn at once from the entity (26.x extracts the stuck face into a render state first). */
    @Override
    public void render(DustCharge charge, float yaw, float partialTick, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffers,
                       int light) {
        Direction face = charge.lit() ? charge.stuckFace() : null;
        if (face == null) {
            super.render(charge, yaw, partialTick, poseStack, buffers, light);
            return;
        }
        Vector3f out = face.step();
        poseStack.pushPose();
        // The entity hangs 0.1 off the face; the cartridge's base lies on the face itself.
        poseStack.translate(-out.x * 0.1F, -out.y * 0.1F, -out.z * 0.1F);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), out));
        poseStack.translate(-0.5F, -0.5F, 0);
        TableModels.submit(poseStack, new SubmitNodeCollector(buffers), TableModels.DUST_CHARGE_STUCK, light);
        poseStack.popPose();
    }
}
