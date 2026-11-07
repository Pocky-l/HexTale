package com.pockyl.hextale.thevoid.boss;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.network.BiterBlastPayload;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModSounds;

/**
 * A cyst of a Returned Charge: lobbed in a high arc to land right on the ring the move drew, it bursts with the Biter
 * Grenade's blast because that is what it answers. Never touches a block.
 */
public class Cyst extends ThrowableItemProjectile {
    private static final double GRAVITY = 0.05;
    private float damage = 12;
    private float radius = 4;
    private int flight = 60;

    public Cyst(EntityType<? extends Cyst> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.VOID_PEARL.get();
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    /** Aims it to reach {@code landing} in {@code ticks}, the same sum the Biter Grenade's hop does. */
    public void lob(Vec3 landing, int ticks, float damage, float radius) {
        this.damage = damage;
        this.radius = radius;
        this.flight = ticks + 1;
        Vec3 from = position();
        double carried = (1 - Math.pow(0.99, ticks)) / 0.01;
        double fall = GRAVITY * ticks * ticks / 2;
        setDeltaMovement((landing.x - from.x) / carried, (landing.y - from.y + fall) / carried, (landing.z - from.z) / carried);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && tickCount >= flight) {
            burst(level);
        }
    }

    /** It flies past the Prosector and the Proof that threw it. */
    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity instanceof LivingEntity living && Prosector.hittable(living);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level() instanceof ServerLevel level) {
            burst(level);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (level() instanceof ServerLevel level) {
            burst(level);
        }
    }

    private void burst(ServerLevel level) {
        Vec3 at = position();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(at, radius * 2, radius * 2, radius * 2), Prosector::hittable)) {
            double distance = Math.sqrt(victim.getBoundingBox().distanceToSqr(at));
            if (distance <= radius) {
                float closeness = 1 - (float) distance / radius;
                ProsectorDamage.strike(level, victim, getOwner() != null ? getOwner() : this, Mth.lerp(closeness, damage / 2, damage));
                Vec3 away = victim.position().subtract(at).multiply(1, 0, 1);
                if (away.lengthSqr() > 1.0E-3) {
                    ProsectorDamage.throwAt(victim, away.normalize().scale(0.6 * closeness).add(0, 0.25, 0));
                }
            }
        }
        BiterBlastPayload word = new BiterBlastPayload(at);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) < BiterBlastPayload.SEEN * BiterBlastPayload.SEEN) {
                ModNetwork.sendToPlayer(player, word);
            }
        }
        level.playSound(null, at.x, at.y, at.z, ModSounds.PROSECTOR_CYST_BURST.get(), getSoundSource(), 3.0F, 0.9F);
        discard();
    }
}
