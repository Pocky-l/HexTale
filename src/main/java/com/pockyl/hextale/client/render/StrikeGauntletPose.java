package com.pockyl.hextale.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

import com.pockyl.hextale.runework.StrikeGauntletItem;

/**
 * The Strike Gauntlet's wind-up seen on a player: while it charges, the gauntleted arm comes up at shoulder height and
 * aims the fist where the player looks, a little across the body, trembling harder with every stage. One-handed - the
 * other arm stays as it is. Added to {@link HumanoidModel.ArmPose} by {@code META-INF/enumextensions.json}; this class
 * is loaded with that enum, so it holds nothing else.
 *
 * <p>1.21.1: the pose reads the entity (26.x: the humanoid render state); the arm pose takes one flag (two-handed).
 */
public final class StrikeGauntletPose {
    public static final EnumProxy<HumanoidModel.ArmPose> CHARGE = new EnumProxy<>(HumanoidModel.ArmPose.class,
            false, (IArmPoseTransformer) StrikeGauntletPose::charge);

    private StrikeGauntletPose() {
    }

    private static void charge(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float ticksUsingItem = entity.getTicksUsingItem() + (entity.isUsingItem() ? partialTick : 0);
        float ageInTicks = entity.tickCount + partialTick;
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        // The arm comes up during the first stage, then holds the aim.
        float raised = Mth.clamp(ticksUsingItem / StrikeGauntletItem.STAGE_TICKS, 0, 1);
        int stage = StrikeGauntletItem.stage((int) ticksUsingItem);
        float shake = Mth.sin(ageInTicks * 2.7F) * 0.025F * stage;
        part.xRot = Mth.lerp(raised, part.xRot, model.head.xRot - Mth.HALF_PI + 0.15F) + shake;
        part.yRot = Mth.lerp(raised, part.yRot, model.head.yRot - 0.3F * side);
        part.zRot += shake * 0.5F * side;
    }
}
