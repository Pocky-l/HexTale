package com.pockyl.hextale.thevoid.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;
import com.pockyl.hextale.registry.ModEffects;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.boss.ProsectorState.Section;
import com.pockyl.hextale.thevoid.boss.attack.Attack;
import com.pockyl.hextale.thevoid.boss.attack.AttackType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The Prosector, the first boss: the Void's researcher, who studies whoever comes to its table the way the mod's player
 * studies magic. What hurts it fills a study ({@link Study}); a full study is written down as a Specimen, and while that
 * Specimen lives the kind does little and comes back as a mimic attack. Breaking the Specimen makes it forget and throws
 * it open. Three phases change the question: Observation, Dissection (the ground is cut) and Proof (a copy of the player).
 *
 * <p>Shape of the code: one synced state ({@link ProsectorState} + {@link Section}, {@link #DATA_STATE}), server-owned
 * timing ({@link #DATA_STATE_TIME}, {@link #DATA_STATE_LENGTH}); {@link Attack} classes run the moves; {@link Decider} picks
 * them; {@link DamageGate} is the hurt gate. There is no arena: a Seer's Eye calls it to any spot of any world, it fights
 * round that spot on whatever ground is there ({@link Surface}), and it never touches a block ({@link CutGround}).
 */
public class Prosector extends Monster {
    /** Blocks over the ground it stays at when it is open. */
    private static final double LOW_HEIGHT = 0.6;
    /** Blocks over the ground it goes up to behind the Proof. */
    private static final double HIGH_HEIGHT = 11;
    /** How far inside the fight's edge it stops following. */
    private static final double ROAM_MARGIN = 10;
    /** How far up and down from the spot still counts as the fight. */
    private static final double FIGHT_HEIGHT = 48;
    /** Rings the ground is marked in as it comes up, and the share of the ritual it stays under them. */
    private static final int RINGS = 6;
    private static final float BURIED_SHARE = 0.6F;
    /** Where its jars hang: blocks from it, and over the ground. */
    private static final double SHELF_NEAR = 10;
    private static final double SHELF_FAR = 15;
    private static final double SHELF_HEIGHT = 1.8;
    /** How often the players of the fight are looked at. */
    private static final int ROOM_CHECK = 10;
    /** Ticks a dull thud waits before the next one, so a fast weapon does not rattle. */
    private static final int DULL_GAP = 8;
    /** Rings that close in on it as it dies. */
    private static final int DEATH_RINGS = 4;
    /** Ticks a Specimen may stay out of sight of the world before it counts as lost. */
    private static final int LOST_SPECIMEN = 200;

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE_TIME = SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE_LENGTH = SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.INT);
    /** The four studies, a byte each. */
    private static final EntityDataAccessor<Integer> DATA_STUDY = SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WRITTEN = SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HELD_KIND = SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.INT);
    /** Where the move in progress points: the end of the line, the beam, the hooks. */
    private static final EntityDataAccessor<Vector3f> DATA_AIM = SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.VECTOR3);
    private static final List<EntityDataAccessor<ItemStack>> DATA_HELD = List.of(
            SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.ITEM_STACK),
            SynchedEntityData.defineId(Prosector.class, EntityDataSerializers.ITEM_STACK));

    /** Its four instruments, each with its own value for the model. */
    public enum Arm {
        SCALPEL, FORCEPS, HOOK, PROBE
    }

    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.hextale.prosector"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_6);
    private final Study study = new Study();
    private final DamageGate gate = new DamageGate();
    private final Decider decider = new Decider();
    private final Deque<ProsectorState> scripted = new ArrayDeque<>();
    private final Map<StudyKind, UUID> specimens = new EnumMap<>(StudyKind.class);
    private final Map<StudyKind, Integer> specimenLost = new EnumMap<>(StudyKind.class);
    private final Map<UUID, Float> dealt = new HashMap<>();
    private final CutGround cut = new CutGround();
    private @Nullable BlockPos home;
    private @Nullable Attack attack;
    private @Nullable UUID proof;
    private @Nullable Vec3 lastShotFrom;
    /** Where Forgetting drags it. */
    private Vec3 fallTo = Vec3.ZERO;
    /** Turns already made: 1 after Dissection, 2 after Proof. */
    private int turnsDone;
    private boolean lastStand;
    private int stateTicks;
    private int gap;
    private int emptyRoom;
    private int lastDull;
    private boolean floorHealth;
    private float passing = -1;
    private float healthFloor;
    /** How much tougher more players make it: what reaches it is divided by this. */
    private float bulk = 1;
    private @Nullable DamageSource killedBy;

    public Prosector(EntityType<? extends Prosector> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1000.0).add(Attributes.ARMOR, 14.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, ProsectorState.DORMANT.ordinal());
        builder.define(DATA_STATE_TIME, 0);
        builder.define(DATA_STATE_LENGTH, 1);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_STUDY, 0);
        builder.define(DATA_WRITTEN, 0);
        builder.define(DATA_HELD_KIND, -1);
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_AIM, new Vector3f());
        DATA_HELD.forEach(key -> builder.define(key, ItemStack.EMPTY));
    }

    // ---- what the model and the renderer read ---------------------------------------------------------------------

    public ProsectorState state() {
        return ProsectorState.byId(entityData.get(DATA_STATE) & 0xFF);
    }

    public Section section() {
        return Section.byId(entityData.get(DATA_STATE) >> 8);
    }

    /** Ticks since the state or section began, on either side, without a counter of its own to lose. */
    public float stateTicks(float partialTick) {
        return (int) level().getGameTime() - entityData.get(DATA_STATE_TIME) + partialTick;
    }

    /** How long this state or section lasts. */
    public int stateLength() {
        return entityData.get(DATA_STATE_LENGTH);
    }

    /** 0 at the start of the state or section, 1 at its end. */
    public float stateProgress(float partialTick) {
        return Mth.clamp(stateTicks(partialTick) / Math.max(1, stateLength()), 0, 1);
    }

    /** 1 Observation, 2 Dissection, 3 Proof. */
    public int phase() {
        return entityData.get(DATA_PHASE);
    }

    /** How full a study sac is, 0..1. */
    public float studyFill(StudyKind kind) {
        return (entityData.get(DATA_STUDY) >> kind.ordinal() * 8 & 0xFF) / 255F;
    }

    public boolean written(StudyKind kind) {
        return (entityData.get(DATA_WRITTEN) & 1 << kind.ordinal()) != 0;
    }

    /** The copy in its forceps: of the move in progress, else of the last thing written down. Empty without one. */
    public ItemStack heldCopy() {
        int kind = entityData.get(DATA_HELD_KIND);
        return kind < 0 ? ItemStack.EMPTY : entityData.get(DATA_HELD.get(kind));
    }

    public ItemStack heldCopy(StudyKind kind) {
        return entityData.get(DATA_HELD.get(kind.ordinal()));
    }

    public Vec3 aim() {
        Vector3fc aim = entityData.get(DATA_AIM);
        return new Vec3(aim.x(), aim.y(), aim.z());
    }

    public int targetId() {
        return entityData.get(DATA_TARGET);
    }

    /** Height of the ground under it. */
    public double floorHeight() {
        return Surface.height(level(), getX(), getZ(), getY());
    }

    /**
     * How far an instrument is out: 0 folded, 1 at full reach. Read from the state and its section, so the same on every
     * client without more data. The scalpel stays out through Excision's recovery: it is stuck in the floor.
     */
    public float armReach(Arm arm, float partialTick) {
        float progress = stateProgress(partialTick);
        float swing = switch (section()) {
            case STARTUP -> progress;
            case ACTIVE -> 1;
            case RECOVERY -> 1 - progress;
            case NONE -> 0;
        };
        ProsectorState state = state();
        return switch (arm) {
            case SCALPEL -> state == ProsectorState.EXCISION && section() == Section.RECOVERY ? 1
                    : state == ProsectorState.INCISION || state == ProsectorState.EXCISION || state == ProsectorState.SECTION ? swing : 0;
            case FORCEPS -> state == ProsectorState.RIPOSTE || state == ProsectorState.CHARGE ? swing : heldCopy().isEmpty() ? 0 : 0.35F;
            case HOOK -> state == ProsectorState.RETRACTOR ? swing : state == ProsectorState.REELING ? 1 - progress : 0;
            case PROBE -> state == ProsectorState.PROBE || state == ProsectorState.VOLLEY || state == ProsectorState.CIRCLE ? swing : 0;
        };
    }

    /** How open its eye is: shut asleep, wide when it is thrown open, clouding as it dies. */
    public float eyeOpen(float partialTick) {
        ProsectorState state = state();
        return switch (state) {
            case DORMANT, RISING -> 0;
            case WAKING -> stateProgress(partialTick);
            case FORGETTING, FALLEN, TURN_DISSECTION, TURN_PROOF -> 1;
            case DYING -> 1 - stateProgress(partialTick);
            default -> phase() >= 2 ? 0.9F : 0.7F;
        };
    }

    /** The carapace hood: folded over the eye in Observation, unfolded since the turn into Dissection. */
    public float hoodOpen(float partialTick) {
        return state() == ProsectorState.TURN_DISSECTION ? stateProgress(partialTick) : phase() >= 2 ? 1 : 0;
    }

    // ---- what the attacks use --------------------------------------------------------------------------------------

    /** The cells Section has opened. */
    public CutGround cut() {
        return cut;
    }

    public Study study() {
        return study;
    }

    public DamageGate gate() {
        return gate;
    }

    public Decider decider() {
        return decider;
    }

    public boolean lastStand() {
        return lastStand;
    }

    public @Nullable Attack attack() {
        return attack;
    }

    public int turnsDone() {
        return turnsDone;
    }

    public boolean turnQueued() {
        return scripted.stream().anyMatch(state -> state == ProsectorState.TURN_DISSECTION || state == ProsectorState.TURN_PROOF);
    }

    public void setAim(Vec3 at) {
        entityData.set(DATA_AIM, new Vector3f((float) at.x, (float) at.y, (float) at.z));
    }

    /** The tick count a timing of the table really takes now: the last stand runs faster. */
    public int timed(int ticks) {
        return lastStand ? Math.max(1, Math.round(ticks * ProsectorConfig.LAST_STAND_TEMPO.get().floatValue())) : ticks;
    }

    /** Where a move comes from: its eye, or the Proof it works through. */
    public Vec3 attackOrigin() {
        Proof hand = proofEntity();
        return hand != null ? hand.position().add(0, hand.getBbHeight() * 0.7, 0) : position().add(0, getBbHeight() * 0.6, 0);
    }

    /** The spot on the ground under a point. */
    public Vec3 groundUnder(Vec3 at) {
        return groundAt(at.x, at.z, at.y);
    }

    /** The spot on the ground at (x, z), the one nearest under the height {@code near}. */
    public Vec3 groundAt(double x, double z, double near) {
        return new Vec3(x, Surface.height(level(), x, z, near), z);
    }

    public void lookAt(Vec3 at) {
        Vec3 from = getEyePosition();
        Vec3 to = at.subtract(from);
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90;
        float pitch = (float) -(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * Mth.RAD_TO_DEG);
        setYRot(yaw);
        setXRot(pitch);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    /** Eases its speed toward {@code to}. It passes through blocks: a pillar never walls it in. */
    public void flyTo(Vec3 to, double speed) {
        Vec3 delta = to.subtract(position());
        double length = delta.length();
        Vec3 want = length < 0.05 ? Vec3.ZERO : delta.scale(Math.min(speed, length * 0.3) / length);
        setDeltaMovement(getDeltaMovement().scale(0.5).add(want.scale(0.5)));
    }

    public void hold() {
        setDeltaMovement(getDeltaMovement().scale(0.6));
    }

    public void playAt(Vec3 at, SoundEvent sound, float volume, float pitch) {
        level().playSound(null, at.x, at.y, at.z, sound, getSoundSource(), volume, pitch);
    }

    /** Whether its moves may touch this creature: everyone but its own, and creative players stay out of it. */
    public static boolean hittable(LivingEntity living) {
        if (!living.isAlive() || living instanceof Prosector || living instanceof Specimen || living instanceof Proof) {
            return false;
        }
        return !(living instanceof Player player) || !player.isCreative() && !player.isSpectator();
    }

    public List<LivingEntity> victims(AABB box) {
        return level().getEntitiesOfClass(LivingEntity.class, box, Prosector::hittable);
    }

    public @Nullable LivingEntity target() {
        return getTarget();
    }

    public Vec3 lastShotFrom() {
        return lastShotFrom;
    }

    // ---- ticks -----------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        noPhysics = true;
        setNoGravity(true);
        super.tick();
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) level();
        stateTicks++;
        gate.tick(state() == ProsectorState.FORGETTING ? ProsectorConfig.FORGET_FACTOR.get().floatValue() : 1);
        decider.tick();
        cut.tick(level, this);
        tickRoom(level);
        healFromJars(level);
        queueTurns();
        ensureSpecimens(level);
        switch (state()) {
            case DORMANT -> dormant(level);
            case RISING -> rising(level);
            case WAKING -> waking(level);
            case HOVER -> hover(level);
            case PROVING -> proving(level);
            case FORGETTING -> forgetting();
            case REELING -> holdLow(ProsectorConfig.REEL_TICKS.get());
            case FALLEN -> fallen();
            case TURN_DISSECTION -> turnDissection(level);
            case TURN_PROOF -> turnProof(level);
            default -> {
                if (attack != null) {
                    runAttack(level);
                } else {
                    idle();
                }
            }
        }
    }

    private void dormant(ServerLevel level) {
        flyTo(homePoint(ProsectorConfig.HOVER_HEIGHT.get()), 0.1);
        if (tickCount % ROOM_CHECK == 0 && !playersNearHome(level).isEmpty()) {
            wake(level);
        }
    }

    /**
     * Calls one to a spot: it starts under the ground and comes up through it while the ground is marked ring by ring. The
     * spot is its home from now on, and the middle of the fight.
     */
    public static Prosector summon(ServerLevel level, BlockPos spot) {
        Prosector boss = ModEntities.PROSECTOR.get().create(level);
        boss.home = spot;
        boss.setPos(spot.getX() + 0.5, spot.getY() - boss.getBbHeight() - 1, spot.getZ() + 0.5);
        level.addFreshEntity(boss);
        boss.enter(ProsectorState.RISING, Section.NONE, ProsectorConfig.SUMMON_TICKS.get());
        boss.playAt(Vec3.atBottomCenterOf(spot), ModSounds.PROSECTOR_SUMMON.get(), 3.0F, 1.0F);
        return boss;
    }

    private void rising(ServerLevel level) {
        int length = stateLength();
        int every = Math.max(1, length / RINGS);
        if (stateTicks % every == 0 && stateTicks / every <= RINGS) {
            int ring = stateTicks / every;
            Vec3 middle = homePoint(0);
            playAt(middle, ModSounds.PROSECTOR_SUMMON_RING.get(), 2.5F, 0.8F + 0.1F * ring);
            ProsectorMarkerPayload.send(level, Marker.SHAKE, middle, middle, 0.5F, 10, 0);
            ProsectorMarkerPayload.send(level, Marker.HEX, middle, middle, HexCells.REACH * ring / (float) RINGS, length - stateTicks + 30, 0xA25CE8);
        }
        if (stateTicks >= length * BURIED_SHARE) {
            flyTo(homePoint(ProsectorConfig.HOVER_HEIGHT.get()), 0.2);
        } else {
            hold();
        }
        if (stateTicks >= length) {
            wake(level);
        }
    }

    /** Called up, or someone came near: it opens its eye. */
    public void wake(ServerLevel level) {
        enter(ProsectorState.WAKING, Section.NONE, 60);
        scaleToRoom(level);
        playAt(position(), ModSounds.PROSECTOR_WAKE.get(), 2.0F, 1.0F);
        ProsectorMarkerPayload.send(level, Marker.TURN, position(), position(), 8, 40, 0xA25CE8);
    }

    private void waking(ServerLevel level) {
        hold();
        if (stateTicks >= stateLength()) {
            pickTarget(level);
            idle();
        }
    }

    private void hover(ServerLevel level) {
        LivingEntity target = targetOrNew(level);
        if (target != null) {
            lookAt(target.getEyePosition());
            flyTo(keepingAway(target), 0.2);
        } else {
            flyTo(homePoint(ProsectorConfig.HOVER_HEIGHT.get()), 0.15);
        }
        if (--gap <= 0 && !scripted.isEmpty()) {
            startScripted(level);
            return;
        }
        if (gap <= 0 && target != null) {
            startAttack(level, decider.choose(this, target), target);
        }
    }

    private void proving(ServerLevel level) {
        flyTo(overGround(HIGH_HEIGHT), 0.1);
        Proof hand = proofEntity();
        if (hand == null) {
            proofFell(level);
            return;
        }
        LivingEntity target = targetOrNew(level);
        hand.setTarget(target);
        if (--gap <= 0 && target != null) {
            startAttack(level, decider.chooseMimic(this, target), target);
        }
    }

    private void fallen() {
        flyTo(overGround(LOW_HEIGHT), 0.25);
        if (stateTicks >= stateLength()) {
            idle();
        }
    }

    /** The sinew it lost drags it low over the ground toward the broken jar, so the one who broke it can reach the window. */
    private void forgetting() {
        flyTo(groundAt(fallTo.x, fallTo.z, fallTo.y).add(0, LOW_HEIGHT, 0), 0.4);
        if (stateTicks >= ProsectorConfig.FORGET_TICKS.get()) {
            idle();
        }
    }

    private void holdLow(int ticks) {
        flyTo(overGround(LOW_HEIGHT), 0.2);
        if (stateTicks >= ticks) {
            idle();
        }
    }

    /** Back to between moves, with a pause of the table's length. */
    public void idle() {
        attack = null;
        int min = timed(ProsectorConfig.GAP_MIN.get());
        int max = Math.max(min, timed(ProsectorConfig.GAP_MAX.get()));
        gap = !scripted.isEmpty() ? 1 : min + (max > min ? random.nextInt(max - min + 1) : 0);
        enter(proofEntity() != null ? ProsectorState.PROVING : ProsectorState.HOVER, Section.NONE, gap);
    }

    /** How long it hangs before the next move. */
    public void pause(int ticks) {
        gap = ticks;
    }

    /** The turns and moves waiting for their turn, in order. */
    public List<ProsectorState> scripted() {
        return List.copyOf(scripted);
    }

    /** Starts a move at once, past the Decider. */
    public void startMove(ServerLevel level, AttackType type, LivingEntity target) {
        setTarget(target);
        attack = null;
        startAttack(level, type, target);
    }

    private void startAttack(ServerLevel level, @Nullable AttackType type, LivingEntity target) {
        if (type == null) {
            gap = 10;
            return;
        }
        attack = type.create(this);
        decider.chosen(type);
        StudyKind kind = type.mimicOf();
        if (kind != null) {
            entityData.set(DATA_HELD_KIND, kind.ordinal());
        }
        attack.start(level, target);
    }

    private void runAttack(ServerLevel level) {
        if (proof != null && proofEntity() == null) {
            attack.cancel(level);
            proofFell(level);
            return;
        }
        if (attack.tick(level)) {
            idle();
        }
    }

    /** A move ended in a way that leaves it reeling for {@code ticks}: the Retractor's hooks snagged. */
    public void reel() {
        attack = null;
        enter(ProsectorState.REELING, Section.NONE, ProsectorConfig.REEL_TICKS.get());
    }

    /** Sets the one synced state; the attack classes go through here as well. */
    public void enter(ProsectorState state, Section section, int length) {
        entityData.set(DATA_STATE, state.ordinal() | section.ordinal() << 8);
        entityData.set(DATA_STATE_TIME, (int) level().getGameTime());
        entityData.set(DATA_STATE_LENGTH, Math.max(1, length));
        stateTicks = 0;
    }

    /** Where it was called to, or where it first stood when it came some other way. */
    private BlockPos home() {
        if (home == null) {
            home = blockPosition();
        }
        return home;
    }

    private Vec3 homePoint(double height) {
        BlockPos spot = home();
        return groundAt(spot.getX() + 0.5, spot.getZ() + 0.5, spot.getY()).add(0, height, 0);
    }

    /** The point this high over the ground right under it. */
    private Vec3 overGround(double height) {
        return groundUnder(position()).add(0, height, 0);
    }

    /** A place round the target at its usual distance, sliding sideways a little so it never hangs still. */
    private Vec3 keepingAway(LivingEntity target) {
        Vec3 from = target.position().subtract(position()).multiply(1, 0, 1);
        if (from.lengthSqr() < 1.0E-3) {
            from = new Vec3(1, 0, 0);
        }
        Vec3 way = from.normalize();
        Vec3 side = new Vec3(-way.z, 0, way.x).scale(Mth.sin(tickCount * 0.03F) * 3);
        Vec3 spot = clampToFight(target.position().subtract(way.scale(ProsectorConfig.KEEP_AWAY.get())).add(side));
        return groundAt(spot.x, spot.z, target.getY()).add(0, ProsectorConfig.HOVER_HEIGHT.get(), 0);
    }

    /** Pulls a point back toward the spot it was called to, so it stays well inside the fight. */
    private Vec3 clampToFight(Vec3 at) {
        BlockPos spot = home();
        double dx = at.x - (spot.getX() + 0.5);
        double dz = at.z - (spot.getZ() + 0.5);
        double away = Math.sqrt(dx * dx + dz * dz);
        double roam = ProsectorConfig.FIGHT_RADIUS.get() - ROAM_MARGIN;
        if (away <= roam) {
            return at;
        }
        double scale = roam / away;
        return new Vec3(spot.getX() + 0.5 + dx * scale, at.y, spot.getZ() + 0.5 + dz * scale);
    }

    private void startScripted(ServerLevel level) {
        ProsectorState turn = scripted.poll();
        if (turn == ProsectorState.TURN_DISSECTION) {
            enter(turn, Section.NONE, ProsectorConfig.TURN2_TICKS.get());
            playAt(position(), ModSounds.PROSECTOR_TURN_DISSECTION.get(), 2.5F, 1.0F);
        } else if (turn == ProsectorState.TURN_PROOF) {
            enter(turn, Section.NONE, ProsectorConfig.TURN3_TICKS.get());
            playAt(position(), ModSounds.PROSECTOR_TURN_PROOF.get(), 2.5F, 1.0F);
        }
        bar.setDarkenScreen(true);
        ProsectorMarkerPayload.send(level, Marker.TURN, position(), position(), 16, 40, 0xE040C8);
    }

    private void turnDissection(ServerLevel level) {
        hold();
        int length = stateLength();
        if (stateTicks == length / 2) {
            setPhase(2);
            scoreGround(level);
            flingSpecimens(level);
        }
        if (stateTicks >= length) {
            turnsDone = 1;
            bar.setDarkenScreen(false);
            idle();
        }
    }

    /** The turn into Dissection shows the grid it will cut the ground by from now on. */
    private void scoreGround(ServerLevel level) {
        Vec3 middle = groundUnder(position());
        for (HexCells.Cell cell : HexCells.cells()) {
            Vec3 at = groundAt(middle.x + HexCells.cellX(cell), middle.z + HexCells.cellZ(cell), middle.y);
            ProsectorMarkerPayload.send(level, Marker.HEX, at, at, HexCells.CELL, ProsectorConfig.TURN2_TICKS.get(), 0xE040C8);
        }
    }

    private void turnProof(ServerLevel level) {
        flyTo(overGround(HIGH_HEIGHT), 0.25);
        int length = stateLength();
        if (stateTicks == length / 2) {
            setPhase(3);
            buildProof(level);
        }
        if (stateTicks >= length) {
            turnsDone = 2;
            bar.setDarkenScreen(false);
            idle();
        }
    }

    private void setPhase(int phase) {
        entityData.set(DATA_PHASE, phase);
    }

    /** Crossing a threshold queues the turn, even from a jump past both of them. */
    private void queueTurns() {
        float share = getHealth() / getMaxHealth();
        if (!state().shielded() && !lastStand) {
            if (turnsDone < 1 && share <= ProsectorConfig.PHASE2_AT.get() && !scripted.contains(ProsectorState.TURN_DISSECTION)) {
                scripted.addLast(ProsectorState.TURN_DISSECTION);
            }
            if (turnsDone < 2 && share <= ProsectorConfig.PHASE3_AT.get() && !scripted.contains(ProsectorState.TURN_PROOF)) {
                scripted.addLast(ProsectorState.TURN_PROOF);
            }
        }
    }

    // ---- the fight's ground ----------------------------------------------------------------------------------------

    /** Within reach of the spot it was called to. */
    public boolean inFight(Vec3 at) {
        BlockPos spot = home();
        double dx = at.x - (spot.getX() + 0.5);
        double dz = at.z - (spot.getZ() + 0.5);
        double radius = ProsectorConfig.FIGHT_RADIUS.get();
        return dx * dx + dz * dz <= radius * radius && Math.abs(at.y - spot.getY()) <= FIGHT_HEIGHT;
    }

    private static boolean counts(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    public List<ServerPlayer> playersInFight(ServerLevel level) {
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (counts(player) && inFight(player.position())) {
                players.add(player);
            }
        }
        return players;
    }

    private List<ServerPlayer> playersNearHome(ServerLevel level) {
        Vec3 spot = Vec3.atCenterOf(home());
        double wake = ProsectorConfig.WAKE_RADIUS.get();
        return level.players().stream().filter(player -> counts(player) && player.distanceToSqr(spot) <= wake * wake).toList();
    }

    private void tickRoom(ServerLevel level) {
        if (tickCount % ROOM_CHECK == 0) {
            updateBar(level);
            if (state() != ProsectorState.DORMANT) {
                emptyRoom = playersInFight(level).isEmpty() ? emptyRoom + ROOM_CHECK : 0;
                if (emptyRoom >= ProsectorConfig.EMPTY_TICKS.get()) {
                    giveUp(level);
                }
            }
        }
    }

    private void updateBar(ServerLevel level) {
        bar.setProgress(getHealth() / getMaxHealth());
        boolean fight = state() != ProsectorState.DORMANT;
        for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
            if (!fight || !player.isAlive() || player.level() != level || !inFight(player.position())) {
                bar.removePlayer(player);
            }
        }
        if (fight) {
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && inFight(player.position())) {
                    bar.addPlayer(player);
                }
            }
        }
    }

    /**
     * Nobody is left: it falls back on the last milestone it reached, lets the Specimens go, closes the ground and sleeps at
     * the spot it was called to until someone comes back. Progress is kept: a fight left in the Proof's phase comes back at
     * the Proof's threshold, not above it.
     */
    public void giveUp(ServerLevel level) {
        if (attack != null) {
            attack.cancel(level);
            attack = null;
        }
        for (StudyKind kind : StudyKind.values()) {
            Specimen specimen = specimen(kind);
            if (specimen != null) {
                specimen.discard();
            }
        }
        specimens.clear();
        study.forgetAll();
        syncStudy();
        Proof hand = proofEntity();
        if (hand != null) {
            hand.discard();
        }
        proof = null;
        scripted.clear();
        if (turnsDone >= 2 && !lastStand) {
            turnsDone = 1;
            setPhase(2);
        }
        setHealth(Math.min(getMaxHealth(), milestone()));
        gate.clear();
        dealt.clear();
        emptyRoom = 0;
        cut.clear(level, position());
        setTarget(null);
        bar.setDarkenScreen(false);
        entityData.set(DATA_HELD_KIND, -1);
        enter(ProsectorState.DORMANT, Section.NONE, 1);
    }

    /** The health it keeps through a retreat: the lowest threshold already passed, or all of it before the first. */
    public float milestone() {
        float max = getMaxHealth();
        float share = getHealth() / max;
        float phase3 = ProsectorConfig.PHASE3_AT.get().floatValue();
        float phase2 = ProsectorConfig.PHASE2_AT.get().floatValue();
        return max * (share <= phase3 ? phase3 : share <= phase2 ? phase2 : 1);
    }

    /** Its body by the config, the share of health it has left kept, and more players make it tougher. */
    private void scaleToRoom(ServerLevel level) {
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        float share = getHealth() / getMaxHealth();
        int extra = Math.max(0, playersInFight(level).size() - 1);
        bulk = 1 + extra * ProsectorConfig.PER_PLAYER.get().floatValue();
        health.setBaseValue(ProsectorConfig.HEALTH.get());
        setHealth(getMaxHealth() * share);
        AttributeInstance armour = getAttribute(Attributes.ARMOR);
        if (armour != null) {
            armour.setBaseValue(ProsectorConfig.ARMOUR.get());
        }
    }

    /** What a hit is divided by: 1 alone, more with every extra player. */
    public float bulk() {
        return bulk;
    }

    /** Every living jar feeds it, up to the last milestone: leaving them alone costs the fight. */
    private void healFromJars(ServerLevel level) {
        if (tickCount % 20 != 0 || state() == ProsectorState.DORMANT || isDeadOrDying()) {
            return;
        }
        float top = milestone();
        int jars = 0;
        for (StudyKind kind : study.writtenKinds()) {
            jars += specimen(kind) != null ? 1 : 0;
        }
        if (jars > 0 && getHealth() < top) {
            setHealth(Math.min(top, getHealth() + jars * ProsectorConfig.SPECIMEN_HEAL.get().floatValue()));
            Vec3 body = position().add(0, getBbHeight() * 0.6, 0);
            ProsectorMarkerPayload.send(level, Marker.GLYPH, body, body, 1.6F, 14, 0xE040C8);
        }
    }

    @Override
    public void checkDespawn() {
        if (level().getDifficulty() == Difficulty.PEACEFUL && level() instanceof ServerLevel level) {
            giveUp(level);
            discard();
        }
    }

    // ---- targets ---------------------------------------------------------------------------------------------------

    private @Nullable LivingEntity targetOrNew(ServerLevel level) {
        LivingEntity target = getTarget();
        boolean lost = target == null || !hittable(target) || !inFight(target.position());
        if (lost || tickCount % 60 == 0) {
            pickTarget(level);
        }
        return getTarget();
    }

    /** The one who hurt it most and is still here, half the time; otherwise whoever is nearest. Pets and golems count. */
    private void pickTarget(ServerLevel level) {
        LivingEntity best = null;
        float most = 0;
        for (Map.Entry<UUID, Float> entry : dealt.entrySet()) {
            if (entry.getValue() > most && level.getEntity(entry.getKey()) instanceof LivingEntity living && hittable(living) && inFight(living.position())) {
                best = living;
                most = entry.getValue();
            }
        }
        if (best == null || random.nextBoolean()) {
            double nearest = Double.MAX_VALUE;
            for (ServerPlayer player : playersInFight(level)) {
                double distance = player.distanceToSqr(this);
                if (distance < nearest) {
                    nearest = distance;
                    best = player;
                }
            }
        }
        setTarget(best);
        entityData.set(DATA_TARGET, best == null ? -1 : best.getId());
    }

    public @Nullable ServerPlayer topDealer(ServerLevel level) {
        ServerPlayer best = null;
        float most = -1;
        for (ServerPlayer player : playersInFight(level)) {
            float hurt = dealt.getOrDefault(player.getUUID(), 0F);
            if (hurt > most) {
                most = hurt;
                best = player;
            }
        }
        return best;
    }

    // ---- being hurt ------------------------------------------------------------------------------------------------

    public boolean shielded() {
        return state().shielded() || proof != null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            floorHealth = false;
            return super.hurt(source, amount);
        }
        if (isDeadOrDying() || source.getEntity() instanceof Prosector || source.getEntity() instanceof Proof) {
            return false;
        }
        // Nothing but its own fight reaches it: no walls keep a bow out, so distance has to.
        if (shielded() || source.getEntity() != null && !inFight(source.getEntity().position())) {
            dull((ServerLevel) level(), source);
            return false;
        }
        float before = getHealth();
        passing = -1;
        healthFloor = healthFloor();
        floorHealth = true;
        boolean hurt = super.hurt(source, amount);
        floorHealth = false;
        if (hurt && passing >= 0) {
            gate.commit(passing);
            learn((ServerLevel) level(), source, before - getHealth());
        }
        return hurt;
    }

    /**
     * The last word on a hit, called from the lowest-priority {@code LivingIncomingDamageEvent} so rune bonuses and the
     * like are already in it. Fires before the hit cooldown is known: the bucket is charged after the hit landed.
     */
    public float gated(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        StudyKind kind = StudyKind.of(source);
        boolean written = study.written(kind);
        float forgetting = state() == ProsectorState.FORGETTING ? ProsectorConfig.FORGET_FACTOR.get().floatValue() : 1;
        float pending = turnQueued() ? ProsectorConfig.PENDING_FACTOR.get().floatValue() : 1;
        float factor = written ? ProsectorConfig.RESIST.get().floatValue() : 1;
        passing = gate.pass(amount / bulk, factor, forgetting, pending);
        if ((written || passing <= DamageGate.TRICKLE) && level() instanceof ServerLevel level) {
            dull(level, source);
        }
        return passing;
    }

    /** Whatever happens to the health in a gated hit, it stops at the next turn: the turns cannot be skipped. */
    private float healthFloor() {
        float max = getMaxHealth();
        if (turnsDone < 1) {
            return max * ProsectorConfig.PHASE3_AT.get().floatValue();
        }
        return turnsDone < 2 || !lastStand ? 1 : 0;
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(floorHealth ? Math.max(health, healthFloor) : health);
    }

    /** Never silent: a dull thud and a glyph at the hit say this tool does nothing now, and what does. */
    private void dull(ServerLevel level, DamageSource source) {
        if (tickCount - lastDull < DULL_GAP) {
            return;
        }
        lastDull = tickCount;
        Vec3 at = source.getSourcePosition() != null ? source.getSourcePosition() : getEyePosition();
        Vec3 near = position().add(0, getBbHeight() * 0.5, 0).add(at.subtract(position()).multiply(1, 0, 1).normalize().scale(getBbWidth() * 0.5));
        playAt(near, ModSounds.PROSECTOR_DULL.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
        ProsectorMarkerPayload.send(level, Marker.GLYPH, near, near, 1.2F, 12, StudyKind.of(source).color());
    }

    private void learn(ServerLevel level, DamageSource source, float lost) {
        StudyKind kind = StudyKind.of(source);
        Entity attacker = source.getEntity();
        if (attacker != null) {
            dealt.merge(attacker.getUUID(), lost, Float::sum);
            if (attacker instanceof LivingEntity living && hittable(living) && getTarget() == null) {
                setTarget(living);
            }
        }
        if (kind == StudyKind.SHOT && attacker != null) {
            lastShotFrom = attacker.getEyePosition();
        }
        ItemStack copy = kind.copyFrom(source);
        if (!copy.isEmpty()) {
            study.remember(kind, copy);
            if (!ItemStack.isSameItemSameComponents(entityData.get(DATA_HELD.get(kind.ordinal())), copy)) {
                entityData.set(DATA_HELD.get(kind.ordinal()), copy);
            }
        }
        float share = lost / (getMaxHealth() * ProsectorConfig.STUDY_SHARE.get().floatValue());
        boolean room = study.writtenCount() < ProsectorConfig.maxWritten(phase());
        if (study.add(kind, share, room)) {
            writeDown(level, kind);
        }
        syncStudy();
    }

    private void syncStudy() {
        int fills = 0;
        int mask = 0;
        for (StudyKind kind : StudyKind.values()) {
            fills |= (int) (Mth.clamp(study.fill(kind), 0, 1) * 255) << kind.ordinal() * 8;
            mask |= study.written(kind) ? 1 << kind.ordinal() : 0;
        }
        entityData.set(DATA_STUDY, fills);
        entityData.set(DATA_WRITTEN, mask);
    }

    /** A full study: the kind flies off as a Specimen and hangs over the ground, tied to it by a sinew. */
    private void writeDown(ServerLevel level, StudyKind kind) {
        study.write(kind);
        Specimen specimen = ModEntities.SPECIMEN.get().create(level);
        if (specimen == null) {
            return;
        }
        Vec3 from = position().add(0, getBbHeight() * 0.6, 0);
        specimen.setPos(from);
        specimen.setup(this, kind, study.copy(kind), freeShelf());
        level.addFreshEntity(specimen);
        specimens.put(kind, specimen.getUUID());
        specimenLost.remove(kind);
        entityData.set(DATA_HELD_KIND, kind.ordinal());
        playAt(from, ModSounds.PROSECTOR_WRITE.get(), 1.6F, 1.0F);
        ProsectorMarkerPayload.send(level, Marker.GLYPH, from, from, 3, 24, kind.color());
        Component line = Component.translatable("message.hextale.prosector.written." + kind.name().toLowerCase(java.util.Locale.ROOT));
        playersInFight(level).forEach(player -> player.displayClientMessage(line, true));
    }

    private Vec3 freeShelf() {
        List<Vec3> shelves = ringOfShelves();
        Vec3 chosen = shelves.get(random.nextInt(shelves.size()));
        double best = -1;
        for (Vec3 shelf : shelves) {
            double nearest = Double.MAX_VALUE;
            for (StudyKind kind : StudyKind.values()) {
                Specimen taken = specimen(kind);
                if (taken != null) {
                    nearest = Math.min(nearest, shelf.distanceToSqr(taken.shelf()));
                }
            }
            double score = nearest + random.nextDouble();
            if (score > best) {
                best = score;
                chosen = shelf;
            }
        }
        return chosen;
    }

    /** The places a jar may hang: a ring round it, each within a walk, on the ground that is there. */
    private List<Vec3> ringOfShelves() {
        List<Vec3> shelves = new ArrayList<>();
        Vec3 middle = groundUnder(position());
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30);
            double reach = i % 2 == 0 ? SHELF_NEAR : SHELF_FAR;
            Vec3 at = clampToFight(middle.add(Math.cos(angle) * reach, 0, Math.sin(angle) * reach));
            shelves.add(groundAt(at.x, at.z, middle.y).add(0, SHELF_HEIGHT, 0));
        }
        return shelves;
    }

    public @Nullable Specimen specimen(StudyKind kind) {
        UUID id = specimens.get(kind);
        return id != null && level() instanceof ServerLevel level && level.getEntity(id) instanceof Specimen specimen ? specimen : null;
    }

    /** The turn into Dissection throws every Specimen to a new place, whole again. */
    private void flingSpecimens(ServerLevel level) {
        for (StudyKind kind : study.writtenKinds()) {
            Specimen specimen = specimen(kind);
            if (specimen != null) {
                specimen.setHealth(specimen.getMaxHealth());
                specimen.flyTo(freeShelf());
            }
        }
    }

    /** A Specimen whose entity went missing, by a chunk unloading for good or a command, simply no longer counts. */
    private void ensureSpecimens(ServerLevel level) {
        if (tickCount % 20 != 0) {
            return;
        }
        for (StudyKind kind : study.writtenKinds()) {
            if (specimen(kind) != null) {
                specimenLost.remove(kind);
            } else if (specimenLost.merge(kind, 20, Integer::sum) >= LOST_SPECIMEN) {
                specimenLost.remove(kind);
                specimens.remove(kind);
                study.forget(kind);
                entityData.set(DATA_HELD.get(kind.ordinal()), ItemStack.EMPTY);
                syncStudy();
            }
        }
    }

    /** Someone broke the Specimen of this kind: it forgets, and is thrown open. */
    public void forget(ServerLevel level, StudyKind kind, Vec3 at) {
        specimens.remove(kind);
        study.forget(kind);
        entityData.set(DATA_HELD.get(kind.ordinal()), ItemStack.EMPTY);
        if (entityData.get(DATA_HELD_KIND) == kind.ordinal()) {
            entityData.set(DATA_HELD_KIND, study.writtenKinds().stream().findFirst().map(Enum::ordinal).orElse(-1));
        }
        syncStudy();
        playAt(at, ModSounds.PROSECTOR_SPECIMEN_BREAK.get(), 1.5F, 1.0F);
        ProsectorMarkerPayload.send(level, Marker.BREAK, at, at, 3, 20, kind.color());
        ProsectorState state = state();
        if (state.shielded() || state == ProsectorState.FALLEN || state == ProsectorState.DYING) {
            return;
        }
        if (attack != null) {
            attack.cancel(level);
            attack = null;
        }
        fallTo = clampToFight(at);
        enter(ProsectorState.FORGETTING, Section.NONE, ProsectorConfig.FORGET_TICKS.get());
        playAt(position(), ModSounds.PROSECTOR_FORGET.get(), 2.0F, 1.0F);
        ProsectorMarkerPayload.send(level, Marker.TURN, position(), position(), 6, 30, kind.color());
    }

    // ---- the Proof -------------------------------------------------------------------------------------------------

    /** Whether this is the Proof of the fight now running. */
    public boolean proving(Proof hand) {
        return hand.getUUID().equals(proof);
    }

    /** Whether it is high up, working through a Proof. */
    public boolean proving() {
        return proof != null;
    }

    public @Nullable Proof proofEntity() {
        return proof != null && level() instanceof ServerLevel level && level.getEntity(proof) instanceof Proof hand && hand.isAlive() ? hand : null;
    }

    private void buildProof(ServerLevel level) {
        Proof hand = ModEntities.PROOF.get().create(level);
        if (hand == null) {
            return;
        }
        Vec3 at = groundUnder(position()).add(0, 0.1, 0);
        hand.setPos(at);
        hand.copyOf(this, topDealer(level));
        level.addFreshEntity(hand);
        proof = hand.getUUID();
        playAt(at, ModSounds.PROSECTOR_PROOF_FORM.get(), 2.0F, 1.0F);
        ProsectorMarkerPayload.send(level, Marker.TURN, at, at, 5, 30, 0xE8E0D0);
    }

    private void proofFell(ServerLevel level) {
        attack = null;
        proof = null;
        lastStand = true;
        enter(ProsectorState.FALLEN, Section.NONE, ProsectorConfig.FALLEN_TICKS.get());
        playAt(position(), ModSounds.PROSECTOR_FALL.get(), 2.5F, 1.0F);
        ProsectorMarkerPayload.send(level, Marker.SHAKE, position(), position(), 1, 30, 0);
    }

    // ---- death -----------------------------------------------------------------------------------------------------

    @Override
    public void die(DamageSource source) {
        boolean first = !dead;
        killedBy = source;
        super.die(source);
        if (first && level() instanceof ServerLevel level) {
            if (attack != null) {
                attack.cancel(level);
                attack = null;
            }
            for (StudyKind kind : StudyKind.values()) {
                Specimen specimen = specimen(kind);
                if (specimen != null) {
                    specimen.discard();
                }
            }
            Proof hand = proofEntity();
            if (hand != null) {
                hand.discard();
            }
            cut.clear(level, position());
            enter(ProsectorState.DYING, Section.NONE, ProsectorConfig.DEATH_TICKS.get());
            playAt(position(), ModSounds.PROSECTOR_DEATH.get(), 2.5F, 1.0F);
        }
    }

    /** The loot waits for the end of the long death; see {@link ProsectorRewards}. */
    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (!(level() instanceof ServerLevel level) || isRemoved()) {
            return;
        }
        bar.setProgress(0);
        setDeltaMovement(0, -0.01, 0);
        int length = ProsectorConfig.DEATH_TICKS.get();
        // Rings of light close in on it, one after another, as its eye clouds.
        int beat = length / (DEATH_RINGS + 1);
        if (deathTime % beat == 0 && deathTime / beat <= DEATH_RINGS) {
            Vec3 ground = floorPoint();
            float size = HexCells.REACH * (DEATH_RINGS + 1 - deathTime / beat) / (float) DEATH_RINGS;
            ProsectorMarkerPayload.send(level, Marker.HEX, ground, ground, size, beat, 0xA25CE8);
        }
        if (deathTime >= length - ProsectorRewards.XP_STREAM) {
            ProsectorRewards.streamXp(level, this, floorPoint());
        }
        if (deathTime >= length) {
            ProsectorRewards.finish(level, this, floorPoint(), killedBy);
            bar.removeAllPlayers();
            level.broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    private Vec3 floorPoint() {
        return groundUnder(position());
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.removeAllPlayers();
        super.remove(reason);
    }

    // ---- what it does not take -------------------------------------------------------------------------------------

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        Holder<MobEffect> holder = effect.getEffect();
        return !holder.equals(MobEffects.POISON) && !holder.equals(MobEffects.WITHER) && !holder.equals(MobEffects.LEVITATION)
                && !holder.equals(ModEffects.VOID_MARKED) && super.canBeAffected(effect);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL)
                || super.isInvulnerableTo(source);
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        ServerLevel level = (ServerLevel) level();
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return state().shielded() ? null : ModSounds.PROSECTOR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PROSECTOR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PROSECTOR_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    // ---- save ------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, registryAccess());
        super.addAdditionalSaveData(output.tag());
        output.putInt("turns_done", turnsDone);
        output.putBoolean("last_stand", lastStand);
        output.putInt("phase", phase());
        output.storeNullable("home", BlockPos.CODEC, home);
        study.save(output);
        for (Map.Entry<StudyKind, UUID> entry : specimens.entrySet()) {
            output.store("specimen_" + entry.getKey().ordinal(), UUIDUtil.CODEC, entry.getValue());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueInput input = ValueInput.of(portTag, registryAccess());
        super.readAdditionalSaveData(input.tag());
        turnsDone = input.getIntOr("turns_done", 0);
        lastStand = input.getBooleanOr("last_stand", false);
        setPhase(input.getIntOr("phase", 1));
        home = input.read("home", BlockPos.CODEC).orElse(home);
        study.load(input);
        for (StudyKind kind : StudyKind.values()) {
            Optional<UUID> id = input.read("specimen_" + kind.ordinal(), UUIDUtil.CODEC);
            id.ifPresent(uuid -> specimens.put(kind, uuid));
            entityData.set(DATA_HELD.get(kind.ordinal()), study.copy(kind));
        }
        syncStudy();
        // A fight is never resumed mid-move: it wakes again when someone comes near, and a Proof left over from before the
        // reload no longer belongs to it, so the turn into Proof comes again.
        if (turnsDone >= 2 && !lastStand) {
            turnsDone = 1;
            setPhase(2);
        }
        enter(ProsectorState.DORMANT, Section.NONE, 1);
    }

    /** Kinds that are written down, for tests and the Decider. */
    public Set<StudyKind> writtenKinds() {
        return study.writtenKinds();
    }
}
