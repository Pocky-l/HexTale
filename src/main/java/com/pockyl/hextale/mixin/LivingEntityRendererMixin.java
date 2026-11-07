package com.pockyl.hextale.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.hextale.client.render.ShimmerPose;
import com.pockyl.hextale.client.render.ZiplineSwing;

/** The one place between a mob's turn and its model: a mutated mount's lean and a rider's swing go here. Neither, nothing happens. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void hextale$lean(LivingEntityRenderState state, PoseStack pose, float bodyRot, float scale, CallbackInfo info) {
        ShimmerPose.apply(state, pose);
        ZiplineSwing.apply(state, pose);
    }
}
