package com.pockyl.hextale.mixin;

import java.util.HashSet;
import java.util.Set;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.hextale.Hextale;

/**
 * The Void is a dimension of our own, and the game counts every dimension it does not know as "experimental": a world
 * with one asks for a backup before it opens. Ours are stable. No event or data field reaches this check, hence a mixin.
 * 1.21.1 also calls the whole set experimental when there are more than the three vanilla dimensions (26.x only asks
 * that those three are there), so ours are left out of that count too.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {
    @Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
    private static void hextale$ourDimensionsAreStable(ResourceKey<LevelStem> key, LevelStem dimension, CallbackInfoReturnable<Lifecycle> cir) {
        if (key.location().getNamespace().equals(Hextale.MOD_ID)) {
            cir.setReturnValue(Lifecycle.stable());
        }
    }

    @ModifyVariable(method = "bake", at = @At("STORE"), ordinal = 0)
    private Lifecycle hextale$ourDimensionsDoNotCount(Lifecycle lifecycle, @Local(argsOnly = true) Registry<LevelStem> stems) {
        if (lifecycle == Lifecycle.stable()) {
            return lifecycle;
        }
        Set<ResourceKey<LevelStem>> others = new HashSet<>(stems.registryKeySet());
        others.addAll(((WorldDimensions) (Object) this).dimensions().keySet());
        others.removeIf(key -> key.location().getNamespace().equals(Hextale.MOD_ID));
        return others.equals(Set.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END)) ? Lifecycle.stable() : lifecycle;
    }
}
