package com.pockyl.hextale.thevoid.boss;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** One quill of a Return Volley: flies straight, sticks in the first block, hurts the first body. A quill carries no fall. */
public class Quill extends ThrowableProjectile {
    private static final int LIFETIME = 50;
    private float damage = 5;

    public Quill(EntityType<? extends Quill> type, Level level) {
        super(type, level);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity instanceof LivingEntity living && Prosector.hittable(living);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level() instanceof ServerLevel level && hit.getEntity() instanceof LivingEntity living) {
            ProsectorDamage.strike(level, living, getOwner() != null ? getOwner() : this, damage);
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (!level().isClientSide()) {
            discard();
        }
    }
}
