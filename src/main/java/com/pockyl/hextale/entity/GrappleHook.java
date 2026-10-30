package com.pockyl.hextale.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.item.GrappleGauntletItem;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * The Grapple Gauntlet's hook: a hextech claw on a cord of light. It flies straight from the glove, bites into the first
 * block it meets and holds there while the wearer keeps the glove clenched (use held); the rope between them is a real
 * rope - it only pulls when stretched (see {@link GrappleGauntletItem#rope}). Let go and it springs back to the glove.
 *
 * <p>The rope's length lives on the side that moves the wearer (their own client); that side reports it to the server
 * ({@link #reportLength}), which shares it with everyone else through {@link #LENGTH} so other players see the rope
 * sag or pull tight the same way.
 */
public class GrappleHook extends Projectile {
    public static final byte FLYING = 0;
    public static final byte ATTACHED = 1;
    public static final byte RETRACTING = 2;
    /** Blocks a tick the claw flies. */
    public static final double SPEED = 3.6;
    /** Blocks a tick it springs back to the glove. */
    private static final double RETURN_SPEED = 3.0;
    /** Shortest rope. */
    public static final double MIN_LENGTH = 1.2;
    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Direction> FACE = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.DIRECTION);
    /** The longest rope and the reel speed: synced, because the wearer's own client is the side that reels the rope. */
    private static final EntityDataAccessor<Float> REACH = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> REEL = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.FLOAT);
    /** Each side's hooks by their owner, so a player's tick finds its rope without a search. */
    private static final Map<UUID, GrappleHook> CLIENT_HOOKS = new ConcurrentHashMap<>();
    private static final Map<UUID, GrappleHook> SERVER_HOOKS = new ConcurrentHashMap<>();

    private boolean breath;
    /** Strength of the Gravity rune (yank a bitten creature to the wearer) and of Blades (cut it); 0 = the claw passes creatures. */
    private int gravity;
    private int blades;
    /** The rope's length on this side, or -1 until the side that moves the wearer has measured it. */
    private double length = -1;
    private Vec3 launch = Vec3.ZERO;
    @Nullable
    private BlockPos bitten;
    private int stateTicks;
    /** The tick the last reel sound played (server: for the other players). */
    public long reelSoundAt;

    public GrappleHook(EntityType<? extends GrappleHook> type, Level level) {
        super(type, level);
    }

    /** A hook thrown by {@code owner} from {@code from} along {@code direction}. */
    public GrappleHook(Level level, Player owner, Vec3 from, Vec3 direction, double reach, double reel, boolean breath, int gravity,
                       int blades) {
        this(ModEntities.GRAPPLE_HOOK.get(), level);
        setOwner(owner);
        setPos(from);
        launch = from;
        entityData.set(REACH, (float) reach);
        entityData.set(REEL, (float) reel);
        this.breath = breath;
        this.gravity = gravity;
        this.blades = blades;
        setDeltaMovement(direction.normalize().scale(SPEED));
        updateRotation();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, FLYING);
        builder.define(LENGTH, -1F);
        builder.define(FACE, Direction.UP);
        // The longest rope (the Shift rune's reach; also how far the claw flies) and the blocks a tick it shortens
        // while reeling in (the Motor rune).
        builder.define(REACH, 32F);
        builder.define(REEL, 0.35F);
    }

    private static Map<UUID, GrappleHook> hooks(Level level) {
        return level.isClientSide() ? CLIENT_HOOKS : SERVER_HOOKS;
    }

    /** The hook {@code player} has out on this side, if any. */
    @Nullable
    public static GrappleHook of(Player player) {
        Map<UUID, GrappleHook> hooks = hooks(player.level());
        GrappleHook hook = hooks.get(player.getUUID());
        if (hook != null && (hook.isRemoved() || hook.level() != player.level())) {
            hooks.remove(player.getUUID(), hook);
            return null;
        }
        return hook;
    }

    public byte state() {
        return entityData.get(STATE);
    }

    public boolean attached() {
        return state() == ATTACHED;
    }

    /** The face of the block the claw bit into. */
    public Direction face() {
        return entityData.get(FACE);
    }

    public double reach() {
        return entityData.get(REACH);
    }

    public double reelSpeed() {
        return entityData.get(REEL);
    }

    public boolean breath() {
        return breath;
    }

    /** The rope's length as this side knows it: measured here, or as the wearer's client reported it; -1 if unknown. */
    public double length() {
        return length >= 0 ? length : entityData.get(LENGTH);
    }

    /** Whether this side has measured the rope itself (it moves the wearer). */
    public boolean measured() {
        return length >= 0;
    }

    public void setLength(double value) {
        length = Math.max(MIN_LENGTH, Math.min(reach(), value));
        if (!level().isClientSide()) {
            entityData.set(LENGTH, (float) length);
        }
    }

    /** Server: the wearer's client says how long the rope is now. */
    public void reportLength(double value) {
        entityData.set(LENGTH, (float) Math.max(MIN_LENGTH, Math.min(reach(), value)));
    }

    @Nullable
    public Player owner() {
        return getOwner() instanceof Player player ? player : null;
    }

    /** With Gravity or Blades the claw bites creatures too (never players, bosses or its own wearer). */
    @Override
    protected boolean canHitEntity(Entity entity) {
        return (gravity > 0 || blades > 0) && entity instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive()
                && !(entity instanceof Player) && entity != getOwner() && !entity.typeHolder().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Player owner = owner();
        if (owner != null) {
            hooks(level()).put(owner.getUUID(), this);
        }
        stateTicks++;
        switch (state()) {
            case FLYING -> fly(owner);
            case ATTACHED -> hold(owner);
            default -> springBack(owner);
        }
    }

    private void fly(@Nullable Player owner) {
        Vec3 motion = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity, ClipContext.Block.COLLIDER);
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
            if (level() instanceof ServerLevel server) {
                seize(server, entityHit.getEntity(), owner);
            }
            return;
        }
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            if (!level().isClientSide()) {
                bite(blockHit);
            } else {
                setPos(hit.getLocation());
                setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
        setPos(position().add(motion));
        updateRotation();
        if (!level().isClientSide()) {
            if (owner == null || !GrappleGauntletItem.holding(owner) || position().distanceTo(launch) > reach()) {
                retract();
            }
        } else {
            level().addParticle(ModParticles.glint(ModParticles.HEX_LIGHT), getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    /** The claw bites into the block it hit: it stays there, and the rope is measured from the wearer. */
    private void bite(BlockHitResult hit) {
        Vec3 at = hit.getLocation().add(hit.getDirection().getUnitVec3().scale(0.05));
        setPos(at);
        setDeltaMovement(Vec3.ZERO);
        bitten = hit.getBlockPos();
        entityData.set(FACE, hit.getDirection());
        entityData.set(STATE, ATTACHED);
        stateTicks = 0;
        Player owner = owner();
        if (owner != null) {
            entityData.set(LENGTH, (float) Math.max(MIN_LENGTH, Math.min(reach(), GrappleGauntletItem.grip(owner).distanceTo(at))));
        }
        if (level() instanceof ServerLevel server) {
            BlockState state = server.getBlockState(hit.getBlockPos());
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 10, 0.1, 0.1, 0.1, 0.15);
            server.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.12);
            server.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, at.y, at.z, 0, 0.5, 0, 0, 1);
            server.playSound(null, at.x, at.y, at.z, ModSounds.GRAPPLE_ATTACH.get(), SoundSource.PLAYERS, 0.8F, 0.9F + random.nextFloat() * 0.2F);
        }
    }

    /**
     * The claw bites a creature: Blades cut it, Gravity yanks it towards the wearer; then the claw springs back. Both
     * flash the rune's light where it bit.
     */
    private void seize(ServerLevel level, Entity target, @Nullable Player owner) {
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
        setPos(at);
        if (blades > 0) {
            target.hurtServer(level, owner != null ? damageSources().playerAttack(owner) : damageSources().magic(),
                    com.pockyl.hextale.runework.HolderRune.at(com.pockyl.hextale.runework.GrappleRune.CUT, blades));
            com.pockyl.hextale.runework.ToolEffects.puff(level, at, com.pockyl.hextale.runework.GrappleRune.BLADES.rune(), 6, 0.25);
            com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.GUST, at, 0xD8DEE6, 0.5F);
        }
        if (gravity > 0 && owner != null && target.isAlive()) {
            Vec3 way = owner.position().subtract(target.position());
            double pull = com.pockyl.hextale.runework.HolderRune.at(com.pockyl.hextale.runework.GrappleRune.PULL, gravity);
            Vec3 yank = way.multiply(1, 0, 1).normalize().scale(pull).add(0, 0.35 + Math.max(0, way.y) * 0.08, 0);
            target.setDeltaMovement(yank);
            target.needsSync = true;
            com.pockyl.hextale.runework.ToolEffects.puff(level, at, com.pockyl.hextale.runework.GrappleRune.GRAVITY.rune(), 6, 0.25);
            com.pockyl.hextale.network.EssenceFxPayload.pour(level, at, owner.position().add(0, 1, 0),
                    com.pockyl.hextale.runework.GrappleRune.GRAVITY.rune().color(), 8);
        }
        level.playSound(null, at.x, at.y, at.z, ModSounds.GRAPPLE_ATTACH.get(), SoundSource.PLAYERS, 0.8F, 1.3F + random.nextFloat() * 0.1F);
        retract();
    }

    private void hold(@Nullable Player owner) {
        if (level().isClientSide()) {
            return;
        }
        boolean held = owner != null && GrappleGauntletItem.holding(owner) && owner.level() == level()
                && owner.distanceTo(this) < reach() + 8;
        boolean bite = bitten == null || !level().getBlockState(bitten).getCollisionShape(level(), bitten).isEmpty();
        if (!held || !bite) {
            retract();
        }
    }

    private void springBack(@Nullable Player owner) {
        noPhysics = true;
        if (owner == null || owner.level() != level() || stateTicks > 12) {
            if (!level().isClientSide()) {
                discard();
            }
            return;
        }
        Vec3 way = GrappleGauntletItem.grip(owner).subtract(position());
        if (way.length() < RETURN_SPEED + 0.5) {
            if (!level().isClientSide()) {
                discard();
            }
            return;
        }
        setDeltaMovement(way.normalize().scale(RETURN_SPEED));
        setPos(position().add(getDeltaMovement()));
    }

    /** Lets go: the claw springs back to the glove and vanishes there. */
    public void retract() {
        if (state() == RETRACTING) {
            return;
        }
        entityData.set(STATE, RETRACTING);
        stateTicks = 0;
        Player owner = owner();
        if (owner != null && level() instanceof ServerLevel server) {
            server.playSound(null, owner.getX(), owner.getY() + 1, owner.getZ(), ModSounds.GRAPPLE_RELEASE.get(), SoundSource.PLAYERS, 0.6F,
                    0.95F + random.nextFloat() * 0.1F);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == STATE) {
            stateTicks = 0;
            if (state() != ATTACHED) {
                length = -1;
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }
}
