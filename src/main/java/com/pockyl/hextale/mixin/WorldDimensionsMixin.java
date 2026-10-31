package com.pockyl.hextale.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.hextale.Hextale;

/**
 * The Void is a dimension of our own, and the game counts every dimension it does not know as "experimental": a world
 * with one asks for a backup before it opens. Ours are stable. No event or data field reaches this check, hence a mixin.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {
    @Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
    private static void hextale$ourDimensionsAreStable(ResourceKey<LevelStem> key, LevelStem dimension, CallbackInfoReturnable<Lifecycle> cir) {
        if (key.identifier().getNamespace().equals(Hextale.MOD_ID)) {
            cir.setReturnValue(Lifecycle.stable());
        }
    }
}
