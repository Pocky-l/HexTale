package com.pockyl.hextale.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTags;
import com.pockyl.hextale.thevoid.Thinning;
import com.pockyl.hextale.world.WorldEdits;

import java.util.ArrayList;
import java.util.List;

/**
 * A thrown Dust Charge: hextech dust packed tight. It sticks where it lands, hisses and sparks for {@link #FUSE} ticks,
 * then bursts in a cold flash that cracks the rock around it ({@code hextale:blastable}: natural stone, ores, gravel...)
 * as if an iron pickaxe had mined it - the drops fall where the blocks were. Builds and other blocks stay whole; creatures
 * close by take a small sting and a push. Like all magic, it thins the world a little.
 */
public class DustCharge extends ThrowableItemProjectile {
    /** Ticks it hisses before it bursts. */
    public static final int FUSE = 30;
    /** Blocks it cracks around itself. */
    public static final float RADIUS = 2.5F;
    /** The hardest block it still cracks (obsidian and the like stay). */
    public static final float MAX_HARDNESS = 10.0F;
    private static final float STING = 2.0F;
    private static final float PUSH = 0.7F;
    private static final float THINNING = 4.0F;
    private static final EntityDataAccessor<Integer> FUSE_LEFT = SynchedEntityData.defineId(DustCharge.class, EntityDataSerializers.INT);
    /** The face of the block it stuck to (the renderer lays the cartridge on it). */
    private static final EntityDataAccessor<net.minecraft.core.Direction> STUCK_FACE = SynchedEntityData.defineId(DustCharge.class,
            EntityDataSerializers.DIRECTION);

    public DustCharge(EntityType<? extends DustCharge> type, Level level) {
        super(type, level);
    }

    public DustCharge(Level level, LivingEntity thrower, ItemStack stack) {
        super(ModEntities.DUST_CHARGE.get(), thrower, level);
        setItem(stack);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUSE_LEFT, -1);
        builder.define(STUCK_FACE, net.minecraft.core.Direction.UP);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.DUST_CHARGE.get();
    }

    public net.minecraft.core.Direction stuckFace() {
        return entityData.get(STUCK_FACE);
    }

    /** Whether it has stuck and is hissing. */
    public boolean lit() {
        return entityData.get(FUSE_LEFT) >= 0;
    }

    @Override
    public void tick() {
        int left = entityData.get(FUSE_LEFT);
        if (left < 0) {
            super.tick();
            if (level().isClientSide() && tickCount % 2 == 0) {
                level().addParticle(ModParticles.spark(ModParticles.HEXTECH), getX(), getY(), getZ(), 0, 0, 0);
            }
            return;
        }
        // Stuck: it does not move; the hiss quickens as the fuse burns down.
        if (level().isClientSide()) {
            if (random.nextInt(Math.max(1, left / 8 + 1)) == 0) {
                level().addParticle(ModParticles.spark(ModParticles.HEXTECH), getX(), getY() + 0.1, getZ(),
                        (random.nextDouble() - 0.5) * 0.1, 0.05, (random.nextDouble() - 0.5) * 0.1);
            }
            if (tickCount % 3 == 0) {
                level().addParticle(ModParticles.vapour(ModParticles.SMOKE), getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
            }
            return;
        }
        if (left % 10 == 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.TNT_PRIMED, SoundSource.NEUTRAL, 0.35F, 1.6F + (FUSE - left) * 0.01F);
        }
        if (left == 0 && level() instanceof ServerLevel level) {
            burst(level, position(), this, getOwner(), WorldEdits.actor(level, getOwner(), ownerUUID));
            discard();
            return;
        }
        entityData.set(FUSE_LEFT, left - 1);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        // It bounces off creatures and keeps falling.
        setDeltaMovement(getDeltaMovement().scale(-0.2));
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide()) {
            Vec3 at = hit.getLocation().add(net.minecraft.world.phys.Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.1));
            setPos(at.x, at.y, at.z);
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
            entityData.set(STUCK_FACE, hit.getDirection());
            entityData.set(FUSE_LEFT, FUSE);
            level().playSound(null, at.x, at.y, at.z, ModSounds.LAMP_FLICKER.get(), SoundSource.NEUTRAL, 0.8F, 1.2F);
        }
    }

    /**
     * The burst at {@code centre}: cracks the blastable blocks within {@link #RADIUS} (dropping what an iron pickaxe would
     * get), stings and pushes what lives close by, and shows a cold flash. Returns the cracked positions.
     */
    public static List<BlockPos> burst(ServerLevel level, Vec3 centre, Entity source, Entity owner) {
        return burst(level, centre, source, owner, WorldEdits.actor(level, owner, null));
    }

    /**
     * {@link #burst(ServerLevel, Vec3, Entity, Entity)} cracking only the blocks {@code actor} may break (the thrower, so
     * a claim keeps its rock).
     */
    public static List<BlockPos> burst(ServerLevel level, Vec3 centre, Entity source, Entity owner, Player actor) {
        List<BlockPos> cracked = new ArrayList<>();
        BlockPos middle = BlockPos.containing(centre);
        int r = (int) Math.ceil(RADIUS);
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        for (BlockPos pos : BlockPos.betweenClosed(middle.offset(-r, -r, -r), middle.offset(r, r, r))) {
            double distance = Math.sqrt(pos.distToCenterSqr(centre)) + level.getRandom().nextFloat() * 0.6;
            if (distance > RADIUS + 0.3) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (!state.is(ModTags.BLASTABLE) || hardness < 0 || hardness > MAX_HARDNESS || !WorldEdits.mayBreak(level, pos, actor)) {
                continue;
            }
            Block.dropResources(state, level, pos, level.getBlockEntity(pos), owner, pick);
            level.levelEvent(2001, pos, Block.getId(state));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            cracked.add(pos.immutable());
        }
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(middle).inflate(RADIUS + 1))) {
            Vec3 away = victim.position().subtract(centre);
            double length = Math.max(0.5, away.length());
            victim.hurt(level.damageSources().indirectMagic(source, owner), STING);
            victim.push(away.x / length * PUSH, 0.3, away.z / length * PUSH);
        }
        level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), centre.x, centre.y, centre.z, 0, RADIUS + 1, 0, 0, 1);
        level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), centre.x, centre.y, centre.z, 30, RADIUS / 2, RADIUS / 2, RADIUS / 2, 0.6);
        level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), centre.x, centre.y, centre.z, 16, RADIUS / 2, RADIUS / 2, RADIUS / 2, 0.05);
        level.sendParticles(ModParticles.vapour(ModParticles.SMOKE), centre.x, centre.y, centre.z, 12, RADIUS / 2, RADIUS / 3, RADIUS / 2, 0.02);
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.ORB_CRACK.get(), SoundSource.NEUTRAL, 1.2F, 0.7F);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.5F, 1.5F);
        Thinning.add(level, middle, THINNING, Thinning.Source.DUST_CHARGE);
        return cracked;
    }
}
