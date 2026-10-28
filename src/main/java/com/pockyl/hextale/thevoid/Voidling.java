package com.pockyl.hextale.thevoid;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.registry.ModParticles;

/**
 * A voidling: a spawn of the Void that a breach pushes out into the world. Bigger than a mite and far worse - a hunched,
 * shelled thing on four sharp legs with a jaw that opens wide; it runs at the nearest player, leaps and bites. It comes
 * out curled up and unfolds as it lands ({@link #emerging}).
 */
public class Voidling extends Monster {
    /** Ticks it takes to unfold after dropping out of a breach. */
    public static final int EMERGE_TICKS = 24;
    private static final int MOTE = 0x2A1C34;
    private static final EntityDataAccessor<Boolean> DATA_EMERGED = SynchedEntityData.defineId(Voidling.class, EntityDataSerializers.BOOLEAN);

    public Voidling(EntityType<? extends Voidling> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_EMERGED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.38F));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** It has just been pushed out of a breach: it falls curled up and unfolds. */
    public void emerged() {
        entityData.set(DATA_EMERGED, true);
    }

    /** 1 just out of the breach, falling to 0 as it unfolds; 0 for one that did not come out of a breach just now. */
    public float emerging(float partialTick) {
        if (!entityData.get(DATA_EMERGED)) {
            return 0;
        }
        return Math.max(0, 1 - (tickCount + partialTick) / EMERGE_TICKS);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        ServerLevel level = (ServerLevel) level();
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            playSound(SoundEvents.PHANTOM_BITE, 0.8F, 0.6F + random.nextFloat() * 0.15F);
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() && random.nextInt(5) == 0) {
            level().addParticle(ModParticles.wisp(MOTE), getRandomX(0.5), getY() + 0.4, getRandomZ(0.5), 0, 0.01, 0);
        }
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) level();
        // Still unfolding: it does not run or bite yet.
        if (emerging(0) > 0) {
            getNavigation().stop();
            return;
        }
        super.customServerAiStep();
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.55F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SPIDER_STEP, 0.25F, 1.3F);
    }
}
