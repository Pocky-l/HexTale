package com.pockyl.hextale.thevoid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
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
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTags;

/**
 * A Void Mite: a small creeping thing of the Void that swarms over blighted land at dusk. It hunts the nearest player
 * and, once it reaches them, does not bite - it latches on and stays, drinking their life away ({@link VoidMites}).
 * By day, idle on the Void's ground, it burrows back into it.
 */
public class VoidMite extends Monster {
    /** Ticks before a mite that found no one goes back into the ground. */
    private static final int LIFETIME = 6000;
    private static final int MOTE = 0x2A1C34;
    private int life;

    public VoidMite(EntityType<? extends VoidMite> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 0.0).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** It does not bite: it latches onto the player and is gone from the world. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator() && VoidMites.latch(player)) {
            discard();
            return true;
        }
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (random.nextInt(4) == 0) {
                level().addParticle(ModParticles.wisp(MOTE), getRandomX(0.4), getY() + 0.2, getRandomZ(0.4), 0, 0.01, 0);
            }
            return;
        }
        life++;
        boolean idle = getTarget() == null;
        boolean day = level().isBrightOutside() && level().canSeeSky(blockPosition());
        if (idle && (life > LIFETIME || (day && random.nextInt(60) == 0)) && level().getBlockState(blockPosition().below()).is(ModTags.VOID_GROUND)) {
            burrow((ServerLevel) level());
        }
    }

    /** Back into the ground it came from. */
    private void burrow(ServerLevel level) {
        level.sendParticles(ModParticles.voidMote(MOTE), getX(), getY() + 0.05, getZ(), 8, 0.25, 0.05, 0.25, 0);
        playSound(ModSounds.VOID_MITE_STEP.get(), 0.8F, 0.8F);
        discard();
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.VOID_MITE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.VOID_MITE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.VOID_MITE_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.VOID_MITE_STEP.get(), 0.3F, 1.0F);
    }

    @Override
    public void tick() {
        yBodyRot = getYRot();
        super.tick();
    }

    @Override
    public void setYBodyRot(float yBodyRot) {
        setYRot(yBodyRot);
        super.setYBodyRot(yBodyRot);
    }
}
