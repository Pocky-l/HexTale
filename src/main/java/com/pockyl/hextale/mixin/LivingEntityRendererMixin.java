package com.pockyl.hextale.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.hextale.client.render.ShimmerPose;
import com.pockyl.hextale.client.render.StrikeGauntletClient;
import com.pockyl.hextale.client.render.ZiplineSwing;

/**
 * The one place between a mob's turn and its model: a mutated mount's lean and a rider's swing go here. Neither, nothing happens.
 *
 * <p>1.21.1: no render states, so a Strike Dash flight's body turn and the head pitches these poses change go here too.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void hextale$lean(LivingEntity entity, PoseStack pose, float bob, float bodyRot, float partialTick, float scale, CallbackInfo info) {
        ShimmerPose.apply(entity, pose, partialTick);
        ZiplineSwing.apply(entity, pose, bodyRot, partialTick);
        StrikeGauntletClient.flightPose(entity, pose, bodyRot, partialTick);
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"), index = 5)
    private float hextale$look(float headPitch, @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true, ordinal = 1) float partialTick) {
        return StrikeGauntletClient.look(entity, ShimmerPose.look(entity, headPitch, partialTick));
    }
}
