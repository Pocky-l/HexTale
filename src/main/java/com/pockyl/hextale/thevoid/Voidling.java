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
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * A voidling: a spawn of the Void that a breach pushes out into the world. Bigger than a mite and far worse - a hunched,
 * shelled thing on four sharp legs with a jaw that opens wide; it runs at the nearest player, leaps and bites. It comes
 * out curled up and unfolds as it lands ({@link #emerging}).
 *
 * <p>Like the Void's spawn of old it grows on magic: near a breach it feeds ({@link #FEED_REACH}), and every creature it
 * kills feeds it more. At {@link #GROWN_AT} it molts into a grown one, at {@link #ELDER_AT} into an elder - bigger,
 * tougher, harder hitting, with more chitin to take; a grown one no longer wanders off. A molt is seen: it curls up and
 * shakes for {@link #MOLT_TICKS} ticks, its old shell bursting off in dark flakes.
 */
public class Voidling extends Monster {
    /** Ticks it takes to unfold after dropping out of a breach. */
    public static final int EMERGE_TICKS = 24;
    private static final int MOTE = 0x2A1C34;
    private static final EntityDataAccessor<Boolean> DATA_EMERGED = SynchedEntityData.defineId(Voidling.class, EntityDataSerializers.BOOLEAN);
    /** 0 young, 1 grown, 2 elder. */
    private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(Voidling.class, EntityDataSerializers.INT);
    /** The tick count its last molt began at (-1 none). */
    private static final EntityDataAccessor<Integer> DATA_MOLT = SynchedEntityData.defineId(Voidling.class, EntityDataSerializers.INT);
    public static final int YOUNG = 0;
    public static final int GROWN = 1;
    public static final int ELDER = 2;
    /** Feeding it needs to molt into a grown one, into an elder (one point a tick near a breach). */
    public static final int GROWN_AT = 6000;
    public static final int ELDER_AT = 18000;
    /** Feeding a kill gives it. */
    public static final int KILL_FEED = 1500;
    /** A breach this close feeds it. */
    public static final double FEED_REACH = 32;
    public static final int MOLT_TICKS = 30;
    private static final double[] HEALTH = {18, 32, 50};
    private static final double[] DAMAGE = {4, 6, 9};
    private static final double[] ARMOUR = {2, 4, 6};
    private static final double[] SCALE = {1.0, 1.35, 1.7};
    /** Chitin it drops on top of its loot table, by stage. */
    private static final int[] EXTRA_CHITIN = {0, 1, 2};
    private int fed;

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
        builder.define(DATA_STAGE, YOUNG);
        builder.define(DATA_MOLT, -1);
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

    public int stage() {
        return entityData.get(DATA_STAGE);
    }

    /** 0..1..0 while it molts (it curls and shakes), 0 otherwise. */
    public float molting(float partialTick) {
        int start = entityData.get(DATA_MOLT);
        if (start < 0) {
            return 0;
        }
        float k = (tickCount - start + partialTick) / MOLT_TICKS;
        return k >= 1 || k < 0 ? 0 : 1 - Math.abs(k * 2 - 1);
    }

    /** Sets its stage at once (a fed breach gives birth to a grown one), with the body that goes with it. */
    public void setStage(int stage) {
        int clamped = Math.max(YOUNG, Math.min(ELDER, stage));
        entityData.set(DATA_STAGE, clamped);
        applyStage(clamped);
        fed = Math.max(fed, clamped == ELDER ? ELDER_AT : clamped == GROWN ? GROWN_AT : 0);
        if (clamped > YOUNG) {
            setPersistenceRequired();
        }
    }

    private void applyStage(int stage) {
        setBase(Attributes.MAX_HEALTH, HEALTH[stage]);
        setBase(Attributes.ATTACK_DAMAGE, DAMAGE[stage]);
        setBase(Attributes.ARMOR, ARMOUR[stage]);
        setBase(Attributes.SCALE, SCALE[stage]);
    }

    private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        var instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /** It feeds; enough and it molts into the next stage. */
    public void feed(ServerLevel level, int amount) {
        if (stage() >= ELDER) {
            return;
        }
        fed += amount;
        int next = fed >= ELDER_AT ? ELDER : fed >= GROWN_AT ? GROWN : YOUNG;
        if (next > stage()) {
            molt(level, next);
        }
    }

    private void molt(ServerLevel level, int next) {
        float healthShare = getHealth() / getMaxHealth();
        setStage(next);
        setHealth(getMaxHealth() * Math.max(healthShare, 0.5F));
        entityData.set(DATA_MOLT, tickCount);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.VOIDLING_GROW.get(), getSoundSource(), 1.0F, 1.1F - next * 0.15F);
        level.sendParticles(ModParticles.vapour(MOTE), getX(), getY() + getBbHeight() * 0.5, getZ(), 18, getBbWidth() * 0.5,
                getBbHeight() * 0.4, getBbWidth() * 0.5, 0.03);
        level.sendParticles(ModParticles.glint(0xF23CE6), getX(), getY() + getBbHeight() * 0.5, getZ(), 10, getBbWidth() * 0.5,
                getBbHeight() * 0.4, getBbWidth() * 0.5, 0.05);
        Quests.fireNear(level, position(), 32, Quests.VOIDLING_GREW);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 20 == 0 && stage() < ELDER && level() instanceof ServerLevel server
                && !server.getEntitiesOfClass(TheVoid.class, new AABB(blockPosition()).inflate(FEED_REACH), TheVoid::rooted).isEmpty()) {
            feed(server, 20);
        }
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
        feed(level, KILL_FEED);
        return super.killedEntity(level, entity, source);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int extra = EXTRA_CHITIN[stage()] + (stage() == ELDER ? random.nextInt(2) : 0);
        if (killedByPlayer && extra > 0) {
            spawnAtLocation(level, new net.minecraft.world.item.ItemStack(ModItems.VOID_CHITIN.get(), extra));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("stage", stage());
        output.putInt("fed", fed);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int stage = input.getIntOr("stage", YOUNG);
        entityData.set(DATA_STAGE, Math.max(YOUNG, Math.min(ELDER, stage)));
        applyStage(stage());
        fed = input.getIntOr("fed", 0);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
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
    protected void customServerAiStep(ServerLevel level) {
        // Still unfolding (or molting): it does not run or bite yet.
        if (emerging(0) > 0 || molting(0) > 0) {
            getNavigation().stop();
            return;
        }
        super.customServerAiStep(level);
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
