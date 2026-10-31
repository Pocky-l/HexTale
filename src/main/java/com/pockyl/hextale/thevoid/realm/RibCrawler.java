package com.pockyl.hextale.thevoid.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModSounds;

/**
 * A Rib Crawler: a long segmented thing of the Void that hangs upside down under ribs and plates. It waits for someone
 * to pass beneath, clicks dryly overhead for a moment (the only warning) and drops; it lands coiled, springs open and
 * runs at its prey, climbing walls like a spider. Left alone it finds a ceiling again.
 */
public class RibCrawler extends Monster {
    /** Ticks it clicks before it lets go. */
    public static final int CLICK_TICKS = 24;
    /** Ticks it takes to spring open after landing. */
    public static final int UNCOIL_TICKS = 20;
    /** How far it looks up for a ceiling to hang from. */
    public static final int CEILING_REACH = 12;
    /** Prey this far to the side (and below it) makes it drop. */
    private static final double DROP_REACH = 4.5;
    private static final double DROP_DEPTH = 16;
    private static final byte CLIMBING = 1;
    private static final byte CLINGING = 2;
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(RibCrawler.class, EntityDataSerializers.BYTE);
    /** Server tick its drop began at (-1 none); the client counts from the moment it learns of it. */
    private static final EntityDataAccessor<Integer> DATA_DROP = SynchedEntityData.defineId(RibCrawler.class, EntityDataSerializers.INT);
    /** Server tick it landed at (-1 none). */
    private static final EntityDataAccessor<Integer> DATA_LAND = SynchedEntityData.defineId(RibCrawler.class, EntityDataSerializers.INT);
    private int clicking = -1;
    private int idle;
    private int dropSeen = -1;
    private int landSeen = -1;

    public RibCrawler(EntityType<? extends RibCrawler> type, Level level) {
        super(type, level);
        this.xpReward = 7;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.ARMOR, 4.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS, (byte) 0);
        builder.define(DATA_DROP, -1);
        builder.define(DATA_LAND, -1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (target, level) -> !isClinging()));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        // Born on the ground, it goes up under the nearest ceiling at once.
        if (reason != EntitySpawnReason.SPAWN_ITEM_USE && reason != EntitySpawnReason.COMMAND) {
            BlockPos ceiling = ceilingAbove(level, blockPosition());
            if (ceiling != null) {
                setPos(getX(), ceiling.getY() - getBbHeight(), getZ());
                setClinging(true);
            }
        }
        return data;
    }

    /** The first solid block over {@code from} with open air under it, or null. */
    static @Nullable BlockPos ceilingAbove(net.minecraft.world.level.LevelReader level, BlockPos from) {
        BlockPos.MutableBlockPos pos = from.mutable();
        for (int i = 1; i <= CEILING_REACH; i++) {
            pos.move(0, 1, 0);
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty()) {
                return i >= 2 && state.isFaceSturdy(level, pos, net.minecraft.core.Direction.DOWN) ? pos.immutable() : null;
            }
        }
        return null;
    }

    public boolean isClimbing() {
        return (entityData.get(DATA_FLAGS) & CLIMBING) != 0;
    }

    /** It hangs upside down under a block, its back against it. */
    public boolean isClinging() {
        return (entityData.get(DATA_FLAGS) & CLINGING) != 0;
    }

    private void setFlag(byte flag, boolean on) {
        byte flags = entityData.get(DATA_FLAGS);
        entityData.set(DATA_FLAGS, on ? (byte) (flags | flag) : (byte) (flags & ~flag));
    }

    public void setClinging(boolean clinging) {
        setFlag(CLINGING, clinging);
        setNoGravity(clinging);
        if (clinging) {
            setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            getNavigation().stop();
        }
    }

    /** 1 at the start of a drop, 0 once it has landed (and for one that did not drop). */
    public float dropping(float partialTick) {
        int start = level().isClientSide() ? dropSeen : entityData.get(DATA_DROP);
        if (start < 0 || (level().isClientSide() ? landSeen : entityData.get(DATA_LAND)) >= start) {
            return 0;
        }
        return Math.max(0.2F, 1 - (tickCount - start + partialTick) / 30.0F);
    }

    /** 1 on landing, 0 once it has sprung open. */
    public float uncoiling(float partialTick) {
        int start = level().isClientSide() ? landSeen : entityData.get(DATA_LAND);
        if (start < 0) {
            return 0;
        }
        return Math.max(0, 1 - (tickCount - start + partialTick) / UNCOIL_TICKS);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!level().isClientSide() || tickCount == 0) {
            return;
        }
        if (DATA_DROP.equals(key) && entityData.get(DATA_DROP) >= 0) {
            dropSeen = tickCount;
        } else if (DATA_LAND.equals(key) && entityData.get(DATA_LAND) >= 0) {
            landSeen = tickCount;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            setFlag(CLIMBING, horizontalCollision && !isClinging());
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (isClinging()) {
            hang(level);
            return;
        }
        int drop = entityData.get(DATA_DROP);
        if (drop >= 0 && entityData.get(DATA_LAND) < drop && onGround()) {
            entityData.set(DATA_LAND, tickCount);
            playSound(ModSounds.RIB_CRAWLER_LAND.get(), 0.9F, 0.9F + random.nextFloat() * 0.2F);
        }
        if (uncoiling(0) > 0 || dropping(0) > 0) {
            getNavigation().stop();
            return;
        }
        super.customServerAiStep(level);
        // Nobody to hunt for a while: back up under a ceiling.
        idle = getTarget() == null ? idle + 1 : 0;
        if (idle > 200 && tickCount % 20 == 0) {
            BlockPos ceiling = ceilingAbove(level, blockPosition());
            if (ceiling != null && ceiling.getY() - getY() <= 3) {
                setPos(getX(), ceiling.getY() - getBbHeight(), getZ());
                setClinging(true);
                idle = 0;
            }
        }
    }

    /** Hanging: it waits for prey under it, clicks, then lets go. */
    private void hang(ServerLevel level) {
        BlockPos above = BlockPos.containing(getX(), getBoundingBox().maxY + 0.05, getZ());
        if (level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
            letGo();
            return;
        }
        if (clicking >= 0) {
            if (++clicking >= CLICK_TICKS) {
                letGo();
            } else if (clicking % 6 == 0) {
                playSound(ModSounds.RIB_CRAWLER_CLICK.get(), 0.7F, 0.9F + random.nextFloat() * 0.3F);
            }
            return;
        }
        if (tickCount % 5 != 0) {
            return;
        }
        AABB below = new AABB(getX() - DROP_REACH, getY() - DROP_DEPTH, getZ() - DROP_REACH, getX() + DROP_REACH, getY(), getZ() + DROP_REACH);
        for (Player player : level.getEntitiesOfClass(Player.class, below, p -> !p.isSpectator() && !p.isCreative())) {
            if (hasLineOfSight(player)) {
                clicking = 0;
                setTarget(player);
                playSound(ModSounds.RIB_CRAWLER_CLICK.get(), 0.7F, 1.0F);
                return;
            }
        }
    }

    private void letGo() {
        clicking = -1;
        setClinging(false);
        entityData.set(DATA_DROP, tickCount);
        playSound(ModSounds.RIB_CRAWLER_DROP.get(), 1.0F, 1.0F);
        if (getTarget() instanceof net.minecraft.server.level.ServerPlayer player) {
            com.pockyl.hextale.quest.Quests.fire(player, com.pockyl.hextale.quest.Quests.CRAWLER_DROPPED);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && isClinging()) {
            letGo();
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        // It drops on purpose: it lands coiled and takes no harm.
        return false;
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            playSound(ModSounds.RIB_CRAWLER_BITE.get(), 0.9F, 0.9F + random.nextFloat() * 0.15F);
        }
        return hit;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !isClinging() && super.removeWhenFarAway(distSqr);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("clinging", isClinging());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setClinging(input.getBooleanOr("clinging", false));
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isClinging() ? null : ModSounds.RIB_CRAWLER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RIB_CRAWLER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RIB_CRAWLER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.RIB_CRAWLER_STEP.get(), 0.35F, 1.0F);
    }
}
