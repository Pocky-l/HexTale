package com.pockyl.hextale.mixin;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.hextale.chem.ShimmerMutations;

/** A Shimmer Steed keeps its rider in deep water and swims at the surface instead of sinking. */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin extends Animal {
    @Unique
    private static final double HEXTALE_SWIM_LIFT = 0.06;

    protected AbstractHorseMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean dismountsUnderwater() {
        return !ShimmerMutations.is(this, ShimmerMutations.Kind.STEED) && super.dismountsUnderwater();
    }

    @Inject(method = "tickRidden", at = @At("HEAD"))
    private void hextale$swim(Player controller, Vec3 riddenInput, CallbackInfo info) {
        if (com.pockyl.hextale.port.PortEntities.canSimulateMovement(this) && ShimmerMutations.is(this, ShimmerMutations.Kind.STEED) && getFluidHeight(FluidTags.WATER) > getFluidJumpThreshold()) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, Math.max(motion.y, 0) + HEXTALE_SWIM_LIFT, motion.z);
        }
    }
}
