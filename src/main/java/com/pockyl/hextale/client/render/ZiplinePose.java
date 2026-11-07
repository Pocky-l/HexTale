package com.pockyl.hextale.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * A rider hanging from a zipline trolley: the arm straight up over the head, a little inwards to the grip, swaying
 * with the ride. Added to {@link HumanoidModel.ArmPose} by {@code META-INF/enumextensions.json}; this class is loaded
 * with that enum, so it holds nothing else.
 */
public final class ZiplinePose {
    public static final EnumProxy<HumanoidModel.ArmPose> HANG = new EnumProxy<>(HumanoidModel.ArmPose.class,
            false, false, (IArmPoseTransformer) ZiplinePose::hang);

    private ZiplinePose() {
    }

    private static void hang(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        part.xRot = (float) Math.PI + Mth.sin(state.ageInTicks * 0.15F) * 0.04F;
        part.yRot = 0;
        part.zRot = -0.28F * side;
    }
}
