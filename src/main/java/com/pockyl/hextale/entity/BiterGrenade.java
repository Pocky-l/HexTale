package com.pockyl.hextale.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * A thrown Biter Grenade. The first creature it hits gets its jaw: it rides there, chattering faster and faster, and after
 * {@link #FUSE} ticks it bursts. The blast only hurts: blocks stay whole and the thrower's pets are spared (the thrower is
 * not). A throw that hits nobody lands unspent and drops as an item. If the bitten creature dies first the grenade falls
 * off still armed and bursts where it lies.
 */
public class BiterGrenade extends ThrowableItemProjectile {
    /** Ticks between the bite and the blast. */
    public static final int FUSE = 40;
    public static final float RADIUS = 3.5F;
    /** Damage at the grenade, and at the very edge of the blast. */
    public static final float BLAST = 12.0F;
    public static final float EDGE = 4.0F;
    private static final float PUSH = 0.9F;
    private static final int GREEN = 0x8CE050;
    /**
     * The fuse values the jaw snaps shut on. The ticks of {@code biter_grenade_fuse} in hextale_sounds.py fall on the same
     * beats, so change both together.
     */
    private static final int[] BEATS = {32, 26, 21, 17, 14, 12, 10, 8, 6, 4, 2};
    private static final EntityDataAccessor<Integer> FUSE_LEFT = SynchedEntityData.defineId(BiterGrenade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(BiterGrenade.class, EntityDataSerializers.INT);
    /** Where it holds on, from the target's feet, in the target's own turn (it stays on the same flank as the body turns). */
    private static final EntityDataAccessor<Vector3fc> HOLD = SynchedEntityData.defineId(BiterGrenade.class, EntityDataSerializers.VECTOR3);

    public BiterGrenade(EntityType<? extends BiterGrenade> type, Level level) {
        super(type, level);
    }

    public BiterGrenade(Level level, LivingEntity thrower, ItemStack stack) {
        super(ModEntities.BITER_GRENADE.get(), thrower, level, stack);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUSE_LEFT, -1);
        builder.define(TARGET, -1);
        builder.define(HOLD, new Vector3f());
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BITER_GRENADE.get();
    }

    /** Heavier than a snowball: it arcs like a Chem Vial. */
    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    /** Ticks left to the blast, or -1 while it is in flight. */
    public int fuseLeft() {
        return entityData.get(FUSE_LEFT);
    }

    /** Whether the jaw snaps shut at this value of the fuse. */
    public static boolean beat(int left) {
        for (int beat : BEATS) {
            if (beat == left) {
                return true;
            }
        }
        return false;
    }

    /** The creature it is holding on to, or null. */
    @Nullable
    public LivingEntity target() {
        int id = entityData.get(TARGET);
        return id >= 0 && level().getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    /** Where it sits on {@code target} at this moment of the frame (the renderer draws it there, in step with the body). */
    public Vec3 heldAt(LivingEntity target, float partialTick) {
        Vector3fc hold = entityData.get(HOLD);
        float turn = Mth.rotLerp(partialTick, target.yBodyRotO, target.yBodyRot) * Mth.DEG_TO_RAD;
        return target.getPosition(partialTick).add(new Vec3(hold.x(), hold.y(), hold.z()).yRot(-turn));
    }

    @Override
    public void tick() {
        int left = fuseLeft();
        if (left < 0) {
            super.tick();
            if (level().isClientSide() && tickCount % 2 == 0) {
                level().addParticle(ModParticles.vapour(ModParticles.SMOKE), getX(), getY(), getZ(), 0, 0.01, 0);
            }
            return;
        }
        LivingEntity target = target();
        if (target != null) {
            setPos(heldAt(target, 1));
        } else {
            fall();
        }
        if (level().isClientSide()) {
            fuseEffects(left);
            return;
        }
        if (left == FUSE) {
            // One sound as long as the fuse: its ticks are the jaw's beats.
            level().playSound(null, getX(), getY(), getZ(), ModSounds.BITER_GRENADE_FUSE.get(), SoundSource.NEUTRAL, 0.8F, 1.0F);
        }
        if (left == 0 && level() instanceof ServerLevel level) {
            blast(level, position(), this, getOwner(), target);
            discard();
            return;
        }
        entityData.set(FUSE_LEFT, left - 1);
    }

    /** Armed with nothing to hold on to: it drops to the ground and lies there. */
    private void fall() {
        if (!level().isClientSide()) {
            entityData.set(TARGET, -1);
        }
        applyGravity();
        setDeltaMovement(getDeltaMovement().scale(0.98));
        move(MoverType.SELF, getDeltaMovement());
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(0.5, 0, 0.5));
        }
    }

    private void fuseEffects(int left) {
        if (beat(left)) {
            level().addParticle(ModParticles.glint(GREEN), getX(), getY() + 0.1, getZ(), 0, 0, 0);
            level().addParticle(ModParticles.spark(GREEN), getX(), getY() + 0.1, getZ(),
                    (random.nextDouble() - 0.5) * 0.15, 0.08, (random.nextDouble() - 0.5) * 0.15);
        }
        if (tickCount % 4 == 0) {
            level().addParticle(ModParticles.wisp(GREEN), getX(), getY() + 0.1, getZ(), 0, 0.03, 0);
        }
    }

    /** It flies past what it would never bite, so a pet in the way does not stop a throw. */
    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && latchesOn(entity);
    }

    private boolean latchesOn(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        if (!(getOwner() instanceof Player thrower)) {
            return living instanceof Enemy;
        }
        return living instanceof Player other ? other != thrower && thrower.canHarmPlayer(other) : ToolEffects.mayStrike(living, thrower);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide() && hit.getEntity() instanceof LivingEntity target) {
            latch(target);
        }
    }

    /** Bites into the flank it came from. The hit result only says who was hit, so the point comes from the last position. */
    private void latch(LivingEntity target) {
        AABB body = target.getBoundingBox();
        Vec3 at = new Vec3(Mth.clamp(xOld, body.minX, body.maxX), Mth.clamp(yOld, body.minY, body.maxY), Mth.clamp(zOld, body.minZ, body.maxZ));
        Vec3 hold = at.subtract(target.position()).yRot(target.yBodyRot * Mth.DEG_TO_RAD);
        entityData.set(HOLD, new Vector3f((float) hold.x, (float) hold.y, (float) hold.z));
        entityData.set(TARGET, target.getId());
        entityData.set(FUSE_LEFT, FUSE);
        setDeltaMovement(Vec3.ZERO);
        setPos(at);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.spark(GREEN), at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.15);
            level.sendParticles(ModParticles.drop(GREEN), at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.05);
            level.playSound(null, at.x, at.y, at.z, ModSounds.BITER_GRENADE_BITE.get(), SoundSource.NEUTRAL, 0.9F, 0.9F + random.nextFloat() * 0.2F);
        }
    }

    /** It hit nobody: it lands unspent. */
    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level() instanceof ServerLevel level) {
            Vec3 at = hit.getLocation().add(hit.getDirection().getUnitVec3().scale(0.2));
            setPos(at);
            spawnAtLocation(level, getItem().copyWithCount(1));
            level.playSound(null, at.x, at.y, at.z, ModSounds.BITER_GRENADE_CLATTER.get(), SoundSource.NEUTRAL, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            discard();
        }
    }

    /** Whether this creature is a pet of {@code owner}, which the blast spares. */
    public static boolean spared(LivingEntity victim, @Nullable Entity owner) {
        EntityReference<LivingEntity> master = victim instanceof OwnableEntity pet ? pet.getOwnerReference() : null;
        return owner != null && master != null && master.getUUID().equals(owner.getUUID());
    }

    /**
     * The blast at {@code centre}: it hurts and pushes what lives within {@link #RADIUS} and is not behind a wall, the most
     * right at the grenade, and never touches a block. {@code held} is the creature it bit, hit whatever stands between
     * them. Returns who it hurt.
     */
    public static List<LivingEntity> blast(ServerLevel level, Vec3 centre, Entity source, @Nullable Entity owner, @Nullable LivingEntity held) {
        List<LivingEntity> hurt = new ArrayList<>();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(centre, RADIUS * 2, RADIUS * 2, RADIUS * 2))) {
            double distance = Math.sqrt(victim.getBoundingBox().distanceToSqr(centre));
            float seen = victim == held ? 1 : ServerExplosion.getSeenPercent(centre, victim);
            if (distance > RADIUS || seen <= 0 || spared(victim, owner)) {
                continue;
            }
            float closeness = 1 - (float) distance / RADIUS;
            if (victim.hurtServer(level, level.damageSources().explosion(source, owner), Mth.lerp(closeness, EDGE, BLAST) * seen)) {
                hurt.add(victim);
            }
            Vec3 away = victim.position().subtract(centre);
            double length = Math.max(0.5, away.length());
            victim.push(away.x / length * PUSH * closeness, 0.2 + 0.3 * closeness, away.z / length * PUSH * closeness);
            victim.needsSync = true;
        }
        level.sendParticles(ModParticles.ring(GREEN), centre.x, centre.y, centre.z, 0, RADIUS + 1, 0, 0, 1);
        level.sendParticles(ModParticles.flame(GREEN), centre.x, centre.y, centre.z, 14, 0.4, 0.4, 0.4, 0.08);
        level.sendParticles(ModParticles.spark(GREEN), centre.x, centre.y, centre.z, 26, RADIUS / 3, RADIUS / 3, RADIUS / 3, 0.5);
        level.sendParticles(ModParticles.drop(GREEN), centre.x, centre.y, centre.z, 16, 0.5, 0.5, 0.5, 0.3);
        level.sendParticles(ModParticles.vapour(ModParticles.SMOKE), centre.x, centre.y, centre.z, 14, RADIUS / 3, RADIUS / 4, RADIUS / 3, 0.02);
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.BITER_GRENADE_BLAST.get(), SoundSource.NEUTRAL, 1.2F, 0.95F + level.getRandom().nextFloat() * 0.1F);
        return hurt;
    }
}
