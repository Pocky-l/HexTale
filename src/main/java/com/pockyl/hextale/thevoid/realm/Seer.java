package com.pockyl.hextale.thevoid.realm;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModEffects;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.VoidMite;
import com.pockyl.hextale.thevoid.Voidling;

/**
 * A Seer: an eye of the Void in a cage of chitin petals, drifting over the plates. It never attacks. When it sees a
 * player it stops, opens and stares; if the player stays in its sight it touches them with a thin violet beam and they
 * are marked ({@link Marked}): the Void's creatures around come for them. Then it drifts off and keeps its distance.
 */
public class Seer extends Monster {
    /** Reach of its eye. */
    public static final double SIGHT = 24;
    /** Ticks it stares before it marks; the beam's length; the mark's length. */
    public static final int STARE_TICKS = 50;
    public static final int BEAM_TICKS = 15;
    public static final int MARK_TICKS = 600;
    /** Ticks after a mark before it marks again. */
    private static final int REST_TICKS = 900;
    /** Creatures within this reach of a marked one come for it. */
    public static final double CALL_REACH = 40;
    private static final EntityDataAccessor<Float> DATA_OPEN = SynchedEntityData.defineId(Seer.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MARKING = SynchedEntityData.defineId(Seer.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MARK_TARGET = SynchedEntityData.defineId(Seer.class, EntityDataSerializers.INT);
    private float openO;
    private float markingO;
    private int rest;
    private int fleeing;

    public Seer(EntityType<? extends Seer> type, Level level) {
        super(type, level);
        this.xpReward = 6;
        this.moveControl = new Drift(this);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, SIGHT).add(Attributes.ATTACK_DAMAGE, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OPEN, 0.0F);
        builder.define(DATA_MARKING, 0.0F);
        builder.define(DATA_MARK_TARGET, -1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Flee());
        goalSelector.addGoal(2, new Stare());
        goalSelector.addGoal(8, new Wander());
    }

    /** Petals 0 shut -> 1 open. */
    public float open(float partialTick) {
        return Mth.lerp(partialTick, openO, entityData.get(DATA_OPEN));
    }

    /** 0..1 while its beam touches someone. */
    public float marking(float partialTick) {
        return Mth.lerp(partialTick, markingO, entityData.get(DATA_MARKING));
    }

    /** Id of the entity its beam touches, -1 none. */
    public int markTargetId() {
        return entityData.get(DATA_MARK_TARGET);
    }

    private void setSynced(EntityDataAccessor<Float> key, float value) {
        if (entityData.get(key) != value) {
            entityData.set(key, value);
        }
    }

    @Override
    public void tick() {
        openO = entityData.get(DATA_OPEN);
        markingO = entityData.get(DATA_MARKING);
        super.tick();
        setNoGravity(true);
        if (!level().isClientSide()) {
            if (rest > 0) {
                rest--;
            }
            if (fleeing > 0) {
                fleeing--;
            }
            // Petals ease toward their goal: open while it stares, shut otherwise.
            boolean staring = goalSelector.getAvailableGoals().stream().anyMatch(g -> g.isRunning() && g.getGoal() instanceof Stare);
            float open = entityData.get(DATA_OPEN);
            setSynced(DATA_OPEN, staring ? Math.min(1, open + 0.1F) : Math.max(0, open - 0.05F));
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) {
            fleeing = 100;
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.SEER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SEER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SEER_DEATH.get();
    }

    /** The first ground under it, up to {@code reach} blocks down, or null. */
    private @Nullable Integer groundBelow(BlockPos from, int reach) {
        BlockPos.MutableBlockPos pos = from.mutable();
        for (int i = 0; i < reach; i++) {
            pos.move(0, -1, 0);
            if (!level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) {
                return pos.getY();
            }
        }
        return null;
    }

    /** It stops, opens and stares at a player in sight; long enough, and the beam marks them. */
    private final class Stare extends Goal {
        private @Nullable Player target;
        private int time;

        Stare() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (rest > 0 || fleeing > 0 || !(level() instanceof ServerLevel server)) {
                return false;
            }
            target = server.getNearestPlayer(Seer.this, SIGHT);
            return target != null && sees(target);
        }

        private boolean sees(Player player) {
            return !player.isCreative() && !player.isSpectator() && player.isAlive() && distanceTo(player) <= SIGHT && hasLineOfSight(player);
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && fleeing == 0 && sees(target) && time < STARE_TICKS + BEAM_TICKS;
        }

        @Override
        public void start() {
            time = 0;
            getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0);
            playSound(ModSounds.SEER_OPEN.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            setDeltaMovement(getDeltaMovement().scale(0.8));
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            time++;
            if (time == STARE_TICKS) {
                entityData.set(DATA_MARK_TARGET, target.getId());
                playSound(ModSounds.SEER_MARK.get(), 1.2F, 1.0F);
                target.addEffect(new MobEffectInstance(ModEffects.VOID_MARKED, MARK_TICKS, 0, false, true, true), Seer.this);
                if (level() instanceof ServerLevel server) {
                    Marked.call(server, target);
                }
                if (target instanceof net.minecraft.server.level.ServerPlayer player) {
                    com.pockyl.hextale.quest.Quests.fire(player, com.pockyl.hextale.quest.Quests.VOID_MARKED);
                }
            }
            if (time >= STARE_TICKS) {
                float k = (time - STARE_TICKS) / (float) BEAM_TICKS;
                setSynced(DATA_MARKING, 1 - Math.abs(k * 2 - 1));
            }
        }

        @Override
        public void stop() {
            if (time >= STARE_TICKS) {
                rest = REST_TICKS;
                fleeing = 160;
            }
            target = null;
            entityData.set(DATA_MARK_TARGET, -1);
            setSynced(DATA_MARKING, 0);
        }
    }

    /** Away from the nearest player, keeping its height. */
    private final class Flee extends Goal {
        Flee() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return fleeing > 0;
        }

        @Override
        public void tick() {
            Player player = level().getNearestPlayer(Seer.this, SIGHT);
            if (player == null || tickCount % 10 != 0) {
                return;
            }
            Vec3 away = position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1.0E-4) {
                away = new Vec3(1, 0, 0);
            }
            Vec3 to = position().add(away.normalize().scale(10));
            getMoveControl().setWantedPosition(to.x, height(BlockPos.containing(to)), to.z, 1.6);
        }
    }

    /** Slow drifting over the plates, 3-10 blocks above the ground. */
    private final class Wander extends Goal {
        Wander() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !getMoveControl().hasWanted() && random.nextInt(reducedTickDelay(20)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            for (int i = 0; i < 4; i++) {
                BlockPos test = blockPosition().offset(random.nextInt(17) - 8, random.nextInt(7) - 3, random.nextInt(17) - 8);
                if (level().isEmptyBlock(test)) {
                    getMoveControl().setWantedPosition(test.getX() + 0.5, height(test), test.getZ() + 0.5, 0.6);
                    getLookControl().setLookAt(test.getX() + 0.5, test.getY() + 0.5, test.getZ() + 0.5, 30.0F, 20.0F);
                    return;
                }
            }
        }
    }

    /** A height 3-10 blocks over the ground under {@code pos}; over the abyss it keeps its own height. */
    private double height(BlockPos pos) {
        Integer ground = groundBelow(pos.above(4), 24);
        if (ground == null) {
            return getY();
        }
        return Mth.clamp(pos.getY() + 0.5, ground + 3.5, ground + 10.5);
    }

    /** Smooth drifting toward the wanted point, like a vex but bumping into what is in the way. */
    private static final class Drift extends MoveControl {
        Drift(Seer seer) {
            super(seer);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.9));
                return;
            }
            Vec3 delta = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
            double length = delta.length();
            if (length < 0.5 || mob.horizontalCollision) {
                operation = Operation.WAIT;
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5));
                return;
            }
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.92).add(delta.scale(speedModifier * 0.012 / length)));
            if (mob.getTarget() == null) {
                Vec3 move = mob.getDeltaMovement();
                mob.setYRot(-((float) Mth.atan2(move.x, move.z)) * Mth.RAD_TO_DEG);
                mob.yBodyRot = mob.getYRot();
            }
        }
    }

    /** The Seer's mark: every so often the Void's creatures around the marked one turn on them. */
    public static final class Marked extends MobEffect {
        public Marked(int color) {
            super(MobEffectCategory.HARMFUL, color);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
            return tickCount % 10 == 0;
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
            // A faint violet flare round the marked one, seen by everyone; every two seconds the Void's creatures are called.
            level.sendParticles(com.pockyl.hextale.registry.ModParticles.voidMark(0xA25CE8), mob.getX(), mob.getY() + mob.getBbHeight() * 0.6,
                    mob.getZ(), 3, mob.getBbWidth() * 0.6, mob.getBbHeight() * 0.35, mob.getBbWidth() * 0.6, 0.01);
            if (mob instanceof Player player && !player.isCreative() && !player.isSpectator() && mob.tickCount % 40 < 10) {
                call(level, player);
            }
            return true;
        }

        /** Every Void creature within {@link #CALL_REACH} that has no prey yet takes the marked one. */
        static void call(ServerLevel level, Player marked) {
            AABB reach = marked.getBoundingBox().inflate(CALL_REACH);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, reach, m -> m instanceof Voidling || m instanceof VoidMite || m instanceof RibCrawler)) {
                if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
                    mob.setTarget(marked);
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return false;
    }
}
