package com.pockyl.hextale.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.pockyl.hextale.chem.ShimmerMutations;
import com.pockyl.hextale.chem.ShimmerRide;

/** A Shimmer Raptor carries its owner, and it no longer fits on a shoulder. */
@Mixin(Parrot.class)
public abstract class ParrotMixin extends ShoulderRidingEntity {
    protected ParrotMixin(EntityType<? extends ShoulderRidingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        Player rider = ShimmerRide.rider(this);
        return rider != null ? rider : super.getControllingPassenger();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        Vec3 seat = ShimmerRide.seat(this, dimensions, scale);
        return seat != null ? seat : super.getPassengerAttachmentPoint(passenger, dimensions, scale);
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        return ShimmerRide.input(controller);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return ShimmerRide.raptorSpeed(this);
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        setRot(controller.getYRot(), controller.getXRot() * 0.5F);
        yRotO = yBodyRot = yHeadRot = getYRot();
        ShimmerRide.fly(this, controller);
    }

    @Override
    protected void updateControlFlags() {
        super.updateControlFlags();
        if (ShimmerRide.rider(this) != null) {
            goalSelector.setControlFlag(Goal.Flag.MOVE, false);
            goalSelector.setControlFlag(Goal.Flag.JUMP, false);
            goalSelector.setControlFlag(Goal.Flag.LOOK, false);
        }
    }

    @Override
    public boolean canSitOnShoulder() {
        return !ShimmerMutations.is(this, ShimmerMutations.Kind.RAPTOR) && super.canSitOnShoulder();
    }
}
