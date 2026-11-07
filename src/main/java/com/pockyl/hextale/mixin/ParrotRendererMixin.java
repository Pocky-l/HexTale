package com.pockyl.hextale.mixin;

import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.world.entity.animal.Parrot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.hextale.client.render.ShimmerPose;

/** 1.21.1: the parrot model's wing angle comes from the renderer's bob (26.x: the render state's flap angle). */
@Mixin(ParrotRenderer.class)
public abstract class ParrotRendererMixin {
    @Inject(method = "getBob(Lnet/minecraft/world/entity/animal/Parrot;F)F", at = @At("HEAD"), cancellable = true)
    private void hextale$flap(Parrot parrot, float partialTick, CallbackInfoReturnable<Float> info) {
        Float flap = ShimmerPose.flap(parrot, partialTick);
        if (flap != null) {
            info.setReturnValue(flap);
        }
    }
}
