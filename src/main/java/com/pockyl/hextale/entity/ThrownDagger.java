package com.pockyl.hextale.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.runework.RuneTier;

/**
 * A thrown Tendril Dagger. It flies like a trident, bites the first creature it meets (or sticks in a block) and always
 * comes back to its thrower along a tendril of hextech light - at once after a bite, after a short rest in a block, or
 * when it flew too far. The better the dagger, the harder it bites and the faster it returns. Nobody else can pick it up.
 */
public class ThrownDagger extends AbstractArrow {
    /** Damage of a thrown bite, by level (1-4). */
    public static final float[] BITE = {6, 7.5F, 9, 10.5F};
    /** Pull back towards the thrower each tick, by level. */
    private static final double[] RETURN = {0.06, 0.075, 0.09, 0.11};
    /** Ticks it rests in a block before it comes back. */
    public static final int REST_TICKS = 10;
    /** Flying further than this from the thrower calls it back. */
    public static final double REACH = 32;
    private boolean returning;

    public ThrownDagger(EntityType<? extends ThrownDagger> type, Level level) {
        super(type, level);
    }

    public ThrownDagger(Level level, LivingEntity owner, ItemStack dagger) {
        super(ModEntities.THROWN_DAGGER.get(), owner, level, dagger, null);
    }

    public boolean returning() {
        return returning || isNoPhysics();
    }

    private int grade() {
        return RuneTier.tier(getPickupItemStackOrigin());
    }

    @Override
    public void tick() {
        Entity owner = getOwner();
        if (!level().isClientSide()) {
            if (owner == null || !owner.isAlive() || owner instanceof Player player && player.isSpectator()) {
                // Nobody to come back to: it drops where it is.
                if (level() instanceof ServerLevel server && pickup == Pickup.ALLOWED) {
                    spawnAtLocation(getPickupItem(), 0.1F);
                }
                discard();
                return;
            }
            if (inGroundTime > REST_TICKS || distanceTo(owner) > REACH) {
                returning = true;
            }
        }
        if (returning() && owner != null) {
            setNoPhysics(true);
            Vec3 way = owner.getEyePosition().subtract(position());
            double pull = RETURN[Math.max(0, Math.min(RETURN.length - 1, grade() - 1))];
            setDeltaMovement(getDeltaMovement().scale(0.9).add(way.normalize().scale(pull)));
            if (!(owner instanceof Player) && way.length() < owner.getBbWidth() + 1) {
                discard();
                return;
            }
        }
        super.tick();
    }

    @Override
    protected @Nullable EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
        return returning() ? null : super.findHitEntity(from, to);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        Entity owner = getOwner();
        DamageSource source = damageSources().thrown(this, owner == null ? this : owner);
        float damage = BITE[Math.max(0, Math.min(BITE.length - 1, grade() - 1))];
        returning = true;
        if (target.hurt(source, damage) && target instanceof LivingEntity living) {
            doPostHurtEffects(living);
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ModParticles.glint(ModParticles.HEXTECH), getX(), getY(), getZ(), 6, 0.15, 0.15, 0.15, 0.03);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.5F, 1.5F);
        }
        setDeltaMovement(getDeltaMovement().multiply(-0.05, -0.1, -0.05));
    }

    @Override
    protected boolean tryPickup(Player player) {
        return ownedBy(player) && (super.tryPickup(player) || returning() && player.getInventory().add(getPickupItem()));
    }

    @Override
    public void playerTouch(Player player) {
        if (ownedBy(player)) {
            super.playerTouch(player);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.TENDRIL_DAGGER.get());
    }

    @Override
    public ItemStack getWeaponItem() {
        return getPickupItemStackOrigin();
    }

    /** Whether it rests in a block (the renderer stops its spin). */
    public boolean resting() {
        return inGround;
    }

    /** The dagger as an item, for the renderer. */
    public ItemStack dagger() {
        return getPickupItemStackOrigin();
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    public void tickDespawn() {
        // It always comes back: never despawns while its thrower lives.
    }

    @Override
    public boolean shouldRender(double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueInput input = ValueInput.of(portTag, registryAccess());
        super.readAdditionalSaveData(input.tag());
        returning = input.getBooleanOr("returning", false);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, registryAccess());
        super.addAdditionalSaveData(output.tag());
        output.putBoolean("returning", returning);
    }
}
