package com.pockyl.hextale.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * Raw hextech thrown at something: the orb cannot take the blow and shatters into a pinch of glittering hextech dust.
 * A creature it hits takes a small arcane sting.
 */
public class ThrownHextech extends ThrowableItemProjectile {
    /** Dust a shattered orb leaves: 2 or 3 pinches. */
    public static final int MIN_DUST = 2;
    public static final int MAX_DUST = 3;
    private static final float STING = 2.0F;

    public ThrownHextech(EntityType<? extends ThrownHextech> type, Level level) {
        super(type, level);
    }

    public ThrownHextech(Level level, LivingEntity thrower, ItemStack stack) {
        super(ModEntities.THROWN_HEXTECH.get(), thrower, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.UNSTABLE_HEXTECH.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && tickCount % 2 == 0) {
            level().addParticle(ModParticles.spark(ModParticles.HEXTECH), getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level() instanceof ServerLevel level) {
            hit.getEntity().hurtServer(level, damageSources().indirectMagic(this, getOwner()), STING);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel level) {
            // Step back from a block face so the dust does not appear inside the block.
            Vec3 at = hit instanceof BlockHitResult block
                    ? hit.getLocation().add(block.getDirection().getUnitVec3().scale(0.2)) : hit.getLocation();
            shatter(level, at);
            discard();
        }
    }

    /** The orb breaks into dust: shards, sparks, a crack, and the dust scattered over the spot. */
    public static void shatter(ServerLevel level, Vec3 at) {
        int dust = MIN_DUST + level.getRandom().nextInt(MAX_DUST - MIN_DUST + 1);
        for (int i = 0; i < dust; i++) {
            ItemEntity pinch = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(ModItems.HEXTECH_DUST.get()));
            pinch.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.15, 0.15, (level.getRandom().nextDouble() - 0.5) * 0.15);
            level.addFreshEntity(pinch);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState()),
                at.x, at.y, at.z, 16, 0.15, 0.15, 0.15, 0.15);
        level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 18, 0.2, 0.2, 0.2, 0.5);
        level.sendParticles(ModParticles.glint(0x98E3FB), at.x, at.y, at.z, 10, 0.3, 0.25, 0.3, 0.03);
        level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, at.y, at.z, 0, 0.9, 0, 0, 1);
        level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 4, 0.05, 0.05, 0.05, 0.08);
        level.playSound(null, at.x, at.y, at.z, ModSounds.ORB_CRACK.get(), SoundSource.NEUTRAL, 0.9F, 1.25F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.0F, 1.1F);
    }
}
