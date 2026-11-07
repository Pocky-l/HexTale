package com.pockyl.hextale.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

import com.pockyl.hextale.runework.StrikeGauntletItem;

/**
 * The Strike Gauntlet's wind-up seen on a player: while it charges, the gauntleted arm comes up at shoulder height and
 * aims the fist where the player looks, a little across the body, trembling harder with every stage. One-handed - the
 * other arm stays as it is. In flight the body lies along the way ({@link StrikeGauntletClient}) and the gauntleted arm
 * reaches out past the head, fist first. Added to {@link HumanoidModel.ArmPose} by {@code META-INF/enumextensions.json};
 * this class is loaded with that enum, so it holds nothing else.
 */
public final class StrikeGauntletPose {
    public static final EnumProxy<HumanoidModel.ArmPose> CHARGE = new EnumProxy<>(HumanoidModel.ArmPose.class,
            false, false, (IArmPoseTransformer) StrikeGauntletPose::charge);
    public static final EnumProxy<HumanoidModel.ArmPose> DASH = new EnumProxy<>(HumanoidModel.ArmPose.class,
            false, false, (IArmPoseTransformer) StrikeGauntletPose::dash);

    private StrikeGauntletPose() {
    }

    private static void dash(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        part.xRot = Mth.PI + Mth.sin(state.ageInTicks * 2.7F) * 0.03F;
        part.yRot = 0;
        part.zRot = 0.14F * side;
    }

    private static void charge(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        // The arm comes up during the first stage, then holds the aim.
        float raised = Mth.clamp(state.ticksUsingItem / StrikeGauntletItem.STAGE_TICKS, 0, 1);
        int stage = StrikeGauntletItem.stage((int) state.ticksUsingItem);
        float shake = Mth.sin(state.ageInTicks * 2.7F) * 0.025F * stage;
        part.xRot = Mth.lerp(raised, part.xRot, model.head.xRot - Mth.HALF_PI + 0.15F) + shake;
        part.yRot = Mth.lerp(raised, part.yRot, model.head.yRot - 0.3F * side);
        part.zRot += shake * 0.5F * side;
    }
}
