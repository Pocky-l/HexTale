package com.pockyl.hextale.thevoid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * The Void breaking into the world: a blocky breach of glowing edges with a whirling depth behind it. It spreads open
 * from its heart, breathes out dark smoke and draws the world's motes in, and throws back - hurt - whatever touches it.
 * Its outline is a {@link VoidLayout} built from the seed (nothing but the numbers is synced). The entity stands at the
 * centre of the breach; {@link #orientation()} turns the breach's plane into the world.
 * <p>
 * A breach the Thinning tore open ({@link #rooted}, see {@link Breaches}) is announced by an omen - the ground shakes,
 * a drone rises, the world's motes are drawn to a point - opens slowly, never closes by itself, keeps feeding the
 * sickness of the land around it and now and then pushes a {@link Voidling} out: the cells bulge and the thing drops out
 * of them.
 */
public class TheVoid extends Entity {
    /** Ticks from the first cell to the whole breach. */
    public static final int OPEN_TICKS = 30;
    /** Ticks from the order to close until it is gone. */
    public static final int CLOSE_TICKS = 24;
    /** Ticks of the omen before a rooted breach tears open. */
    public static final int OMEN_TICKS = 90;
    /** Ticks a rooted breach takes to open: slower than a shard's, it is the moment to be seen. */
    public static final int ROOTED_OPEN_TICKS = 80;
    /** Ticks from the open breach to its first voidling bulging out. */
    public static final int FIRST_PUSH_DELAY = 12;
    /** Ticks the cells bulge before a voidling drops out. */
    public static final int PUSH_TICKS = 34;
    /** Voidlings around a breach before it sends no more. */
    public static final int VOIDLING_LIMIT = 3;
    /** Ticks between two voidlings at least (and up to twice this). */
    public static final int PUSH_PAUSE = 900;
    /** Ticks between two feedings of the land around. */
    public static final int FEED_TICKS = 100;
    /** Thinning a rooted breach adds to its chunk per feeding. */
    public static final float FEED = 1.5F;
    /** A rooted breach whose land is healed below this starves (unless it is held). */
    public static final float FADES_BELOW = 1500;
    /** Ticks a starving breach lasts before it closes. */
    public static final int STARVE_TICKS = 1200;
    /** Ticks between two counts of the anchors around. */
    public static final int ANCHOR_TICKS = 20;
    /** How far an anchor may stand from a breach (blocks, every way) and how near at least (across the ground). */
    public static final int ANCHOR_REACH = 12;
    public static final double ANCHOR_NEAREST = 3;
    /** Sides a breach must be held from: one anchor in every sixth of the circle. */
    public static final int SIDES = 6;
    /** A breach fighting a ring that is not closed yet pushes its spawn out this many times as often. */
    public static final int FIGHT_FACTOR = 3;
    /** Thinning a held breach draws out of the land per mB it eats (unknown essence twice as much). */
    public static final float EAT_DRAIN = 0.5F;
    /** Chunks every way it draws the land from as it eats. */
    public static final int EAT_RADIUS = 2;
    /** How much each anchor's crystal clouds per mB eaten. */
    public static final float EAT_STRAIN = 2;
    /** Eaten essence that brings out a voidling; a big meal (twice that at once) brings out a grown one. */
    public static final int BIRTH_MB = 500;
    /** mB of essence a thrown item is worth per essence point it carries. */
    public static final int ITEM_MEAL = 4;
    /** Magic damage of a touch. */
    public static final float TOUCH_DAMAGE = 4;
    private static final byte REPEL_EVENT = 60;

    public static final int VIOLET = 0x9A3CFF;
    public static final int MAGENTA = 0xF23CE6;
    public static final int PINK = 0xFFB4F4;
    public static final int DEEP = 0x34105A;
    /** The cold violet of a held breach. */
    public static final int COLD = 0x8A7CFF;
    /** The smoke it breathes: nearly black, a little violet. */
    public static final int SMOKE = 0x140A1C;
    public static final int UMBER = 0x2A2014;

    private static final EntityDataAccessor<Integer> DATA_SEED = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHAPE = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> DATA_BORN = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_CLOSE_AT = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_OMEN = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OPEN = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ROOTED = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> DATA_PUSH_AT = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3fc> DATA_PUSH = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.VECTOR3);
    /** Ticks it has starved for (0 while the land feeds it). */
    private static final EntityDataAccessor<Integer> DATA_STARVED = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.INT);
    /** Sides it is held from by anchors (0-6); six = held. */
    private static final EntityDataAccessor<Integer> DATA_HELD = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.INT);
    /** 0 wild, rising to 1 as a held breach cools (and back as it wakes). */
    private static final EntityDataAccessor<Float> DATA_CALM = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.FLOAT);
    /** The game time it last ate. */
    private static final EntityDataAccessor<Long> DATA_ATE = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.LONG);

    private boolean burstShown;
    /** Server: when the next voidling may bulge out. */
    private long nextPush = -1;
    /** Server: what it has eaten since its last birth (mB). */
    private float fed;
    /** Server: the next birth is a grown voidling (a big meal). */
    private boolean bigMeal;
    /** Server: the push under way is a birth from being fed. */
    private boolean feeding;
    /** Server: the anchors that held it at the last count. */
    private final java.util.List<BlockPos> anchors = new java.util.ArrayList<>();
    private long lastAteNote = -1000;

    public TheVoid(EntityType<? extends TheVoid> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A new breach at {@code centre}, facing {@code yaw}, opening from now on. */
    public static TheVoid open(Level level, Vec3 centre, float yaw, VoidShape shape, float size, int seed) {
        TheVoid breach = new TheVoid(ModEntities.THE_VOID.get(), level);
        breach.setPos(centre);
        breach.setYRot(yaw);
        breach.entityData.set(DATA_SHAPE, shape.ordinal());
        breach.entityData.set(DATA_SIZE, Mth.clamp(size, 0.5F, 16));
        breach.entityData.set(DATA_SEED, seed);
        breach.entityData.set(DATA_BORN, level.getGameTime());
        return breach;
    }

    /**
     * A breach the Thinning tears open at {@code centre}: the omen first, then a slow opening, and it stays - feeding the
     * land and sending voidlings out of its front (the side {@code yaw} looks to).
     */
    public static TheVoid rooted(Level level, Vec3 centre, float yaw, VoidShape shape, float size, int seed) {
        TheVoid breach = open(level, centre, yaw, shape, size, seed);
        breach.entityData.set(DATA_ROOTED, true);
        breach.entityData.set(DATA_OMEN, OMEN_TICKS);
        breach.entityData.set(DATA_OPEN, ROOTED_OPEN_TICKS);
        breach.entityData.set(DATA_BORN, level.getGameTime() + OMEN_TICKS);
        breach.nextPush = level.getGameTime() + OMEN_TICKS + ROOTED_OPEN_TICKS + FIRST_PUSH_DELAY;
        return breach;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SEED, 0);
        builder.define(DATA_SHAPE, 0);
        builder.define(DATA_SIZE, 3.0F);
        builder.define(DATA_BORN, 0L);
        builder.define(DATA_CLOSE_AT, -1L);
        builder.define(DATA_OMEN, 0);
        builder.define(DATA_OPEN, OPEN_TICKS);
        builder.define(DATA_ROOTED, false);
        builder.define(DATA_PUSH_AT, -1L);
        builder.define(DATA_PUSH, new Vector3f());
        builder.define(DATA_STARVED, 0);
        builder.define(DATA_HELD, 0);
        builder.define(DATA_CALM, 0F);
        builder.define(DATA_ATE, -1000L);
    }

    public int seed() {
        return entityData.get(DATA_SEED);
    }

    public VoidShape shape() {
        return VoidShape.byOrdinal(entityData.get(DATA_SHAPE));
    }

    public float size() {
        return entityData.get(DATA_SIZE);
    }

    public VoidLayout layout() {
        return VoidLayout.of(shape(), size(), seed());
    }

    /** Torn open by the Thinning: it stays, feeds the land and sends voidlings. */
    public boolean rooted() {
        return entityData.get(DATA_ROOTED);
    }

    /** Ticks since it began to break open, with the partial tick (0 during the omen). */
    public float age(float partialTick) {
        return Math.max(0, level().getGameTime() - entityData.get(DATA_BORN) + partialTick);
    }

    /** The game time it begins to break open (after the omen). */
    public long born() {
        return entityData.get(DATA_BORN);
    }

    /** The game time the omen began (the same as {@link #born()} when there was none). */
    public long omenStart() {
        return born() - entityData.get(DATA_OMEN);
    }

    /** Ticks it takes to open. */
    public int openTicks() {
        return entityData.get(DATA_OPEN);
    }

    /** 0 -> 1 through the omen; 1 once it breaks open (and for a breach without an omen). */
    public float omen(float partialTick) {
        int omen = entityData.get(DATA_OMEN);
        if (omen <= 0) {
            return 1;
        }
        return Mth.clamp((level().getGameTime() - omenStart() + partialTick) / omen, 0, 1);
    }

    /** Still in the omen: nothing of it shows yet. */
    public boolean foretold() {
        return level().getGameTime() < born();
    }

    /** Where a voidling is pushing out (local, blocks), or null when none is. */
    public @Nullable Vector3fc pushPoint() {
        return entityData.get(DATA_PUSH_AT) < 0 ? null : entityData.get(DATA_PUSH);
    }

    /** 0 -> 1 while the cells bulge, falling back as they settle after the voidling is out; 0 when nothing pushes. */
    public float push(float partialTick) {
        long at = entityData.get(DATA_PUSH_AT);
        if (at < 0) {
            return 0;
        }
        float since = level().getGameTime() - at + partialTick;
        if (since <= PUSH_TICKS) {
            float k = Mth.clamp(since / PUSH_TICKS, 0, 1);
            // It strains, gives a little, strains harder.
            return k * k * (0.85F + 0.15F * Mth.sin(since * 0.9F));
        }
        return Mth.clamp(1 - (since - PUSH_TICKS) / 8, 0, 1) * 0.4F;
    }

    /** Sides it is held from by anchors (0-6). */
    public int heldSides() {
        return entityData.get(DATA_HELD);
    }

    /** Held from every side by anchors: it cools, keeps still and eats what it is given. */
    public boolean subdued() {
        return heldSides() >= SIDES;
    }

    /** 0 wild .. 1 cooled. */
    public float calm() {
        return entityData.get(DATA_CALM);
    }

    /** The game time it last ate. */
    public long ateAt() {
        return entityData.get(DATA_ATE);
    }

    /** Sets how long it has starved already (tests). */
    public void setStarved(int ticks) {
        entityData.set(DATA_STARVED, Math.max(0, ticks));
    }

    /** 0 while the land feeds it, rising to 1 as it starves to its end. */
    public float starving() {
        return Mth.clamp(entityData.get(DATA_STARVED) / (float) STARVE_TICKS, 0, 1);
    }

    public boolean closing() {
        return entityData.get(DATA_CLOSE_AT) >= 0;
    }

    /** 0 while open, rising to 1 as it closes. */
    public float closeProgress(float partialTick) {
        long at = entityData.get(DATA_CLOSE_AT);
        if (at < 0) {
            return 0;
        }
        return Mth.clamp((level().getGameTime() - at + partialTick) / CLOSE_TICKS, 0, 1);
    }

    /** How much of the breach shows: 0 -> 1 while opening, back to 0 while closing. */
    public float reveal(float partialTick) {
        return Math.min(Mth.clamp(age(partialTick) / openTicks(), 0, 1), 1 - closeProgress(partialTick));
    }

    /** Starts closing it; the entity goes when the breach has shut. */
    public void close() {
        if (closing()) {
            return;
        }
        entityData.set(DATA_CLOSE_AT, level().getGameTime());
        level().playSound(null, getX(), getY(), getZ(), ModSounds.THE_VOID_CLOSE.get(), SoundSource.AMBIENT,
                0.6F + size() * 0.12F, 1.15F - Math.min(size(), 8) * 0.05F);
    }

    /**
     * How the breach's plane lies in the world: upright and turned to the yaw, or laid flat on the ground. Local x =
     * across the breach, y = along it, z = the plane's normal.
     */
    public Quaternionf orientation() {
        return orientation(getYRot(), shape());
    }

    public static Quaternionf orientation(float yaw, VoidShape shape) {
        Quaternionf q = new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD);
        if (shape.lying()) {
            q.rotateX(Mth.HALF_PI);
        }
        return q;
    }

    /** A local point (blocks) in the world. */
    public Vec3 toWorld(Vector3f local) {
        Vector3f turned = orientation().transform(new Vector3f(local));
        return position().add(turned.x, turned.y, turned.z);
    }

    /** The breach's front: the side a rooted breach sends its voidlings out of. */
    public Vector3f normal() {
        return orientation().transform(new Vector3f(0, 0, 1));
    }

    @Override
    public void tick() {
        // No physics, no fire, no fluids: the breach only hangs where it broke through.
        if (level().isClientSide()) {
            if (foretold()) {
                omenEffects();
            } else {
                clientEffects();
            }
        } else if (closing() && level().getGameTime() - entityData.get(DATA_CLOSE_AT) >= CLOSE_TICKS) {
            discard();
        } else {
            serverTick((ServerLevel) level());
        }
    }

    private void serverTick(ServerLevel level) {
        long now = level.getGameTime();
        if (entityData.get(DATA_OMEN) > 0 && now == born()) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.THE_VOID_OPEN.get(), SoundSource.AMBIENT,
                    1.2F + size() * 0.12F, 0.9F - Math.min(size(), 8) * 0.04F);
        }
        if (closing() || reveal(0) < 0.4F) {
            return;
        }
        touch(level);
        if (!rooted() || age(0) < openTicks()) {
            return;
        }
        if (now % ANCHOR_TICKS == 0) {
            countAnchors(level);
        }
        float calm = calm();
        float wanted = subdued() ? 1 : 0;
        if (calm != wanted) {
            entityData.set(DATA_CALM, wanted > calm ? Math.min(wanted, calm + 1 / 60F) : Math.max(wanted, calm - 1 / 30F));
        }
        if (subdued() && now % 10 == 5) {
            eatThrown(level);
        }
        if (now % FEED_TICKS == 0) {
            if (subdued()) {
                // Held, it neither starves nor feeds on the land.
                entityData.set(DATA_STARVED, 0);
                if (Thinning.get(level, blockPosition()) < FADES_BELOW) {
                    Quests.fireNear(level, position(), Breaches.WATCH_RADIUS, Quests.BREACH_KEPT);
                }
            } else {
                if (starve(level)) {
                    return;
                }
                Thinning.add(level, blockPosition(), FEED);
            }
        }
        long pushAt = entityData.get(DATA_PUSH_AT);
        if (pushAt >= 0) {
            long since = now - pushAt;
            if (since == PUSH_TICKS) {
                emerge(level);
            } else if (since > PUSH_TICKS + 10) {
                entityData.set(DATA_PUSH_AT, -1L);
            }
        } else if (subdued()) {
            // Held, it sends nothing out by itself - only what it was fed into being.
            if (fed >= BIRTH_MB && now % 20 == 0 && maySend(level)) {
                fed -= BIRTH_MB;
                feeding = true;
                startPush(level);
            }
        } else if (nextPush >= 0 && now >= nextPush) {
            if (maySend(level)) {
                startPush(level);
            } else {
                nextPush = now + 100;
            }
        }
    }

    /**
     * Counts the anchors around that hold a fresh crystal, by the sixth of the circle they stand in, and ties a thread to
     * every one. All six sides held: the breach is subdued. Holding wears the anchors' crystals - twice as fast while the
     * breach still fights a ring that is not closed.
     */
    private void countAnchors(ServerLevel level) {
        boolean[] sides = new boolean[SIDES];
        java.util.List<com.pockyl.hextale.block.VoidAnchorBlockEntity> holding = new java.util.ArrayList<>();
        for (com.pockyl.hextale.block.VoidAnchorBlockEntity anchor
                : com.pockyl.hextale.block.VoidAnchorBlockEntity.around(level, blockPosition(), ANCHOR_REACH)) {
            if (!anchor.holds()) {
                continue;
            }
            Vec3 at = Vec3.atCenterOf(anchor.getBlockPos());
            double dx = at.x - getX();
            double dz = at.z - getZ();
            if (dx * dx + dz * dz < ANCHOR_NEAREST * ANCHOR_NEAREST) {
                continue;
            }
            double angle = Math.toDegrees(Math.atan2(dz, dx)) + 360 + 180.0 / SIDES;
            sides[(int) (angle / (360.0 / SIDES)) % SIDES] = true;
            holding.add(anchor);
        }
        int held = 0;
        for (boolean side : sides) {
            held += side ? 1 : 0;
        }
        boolean was = subdued();
        int before = heldSides();
        entityData.set(DATA_HELD, held);
        anchors.clear();
        boolean nowHeld = held >= SIDES;
        for (com.pockyl.hextale.block.VoidAnchorBlockEntity anchor : holding) {
            anchor.tie(level, this, nowHeld, ANCHOR_TICKS * (nowHeld ? 1 : 2));
            anchors.add(anchor.getBlockPos());
        }
        if (nowHeld && !was) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.THE_VOID_SUBDUE.get(), SoundSource.AMBIENT, 1.2F, 1.0F);
            level.sendParticles(ModParticles.ring(com.pockyl.hextale.block.VoidAnchorBlockEntity.COLD), getX(), getY(), getZ(), 0,
                    size() * 0.7, 0, 0, 1);
            entityData.set(DATA_PUSH_AT, -1L);
            Quests.fireNear(level, position(), Breaches.WATCH_RADIUS, Quests.BREACH_SUBDUED);
        } else if (was && !nowHeld) {
            // It wakes: the ring broke.
            level.playSound(null, getX(), getY(), getZ(), ModSounds.THE_VOID_REPEL.get(), SoundSource.AMBIENT, 1.2F, 0.6F);
            nextPush = level.getGameTime() + PUSH_PAUSE / FIGHT_FACTOR;
        } else if (held > 0 && held < SIDES && before == 0) {
            // A ring begins to close: it fights back, sooner than it would have.
            nextPush = Math.min(nextPush < 0 ? Long.MAX_VALUE : nextPush, level.getGameTime() + PUSH_PAUSE / FIGHT_FACTOR / 2);
        }
    }

    /**
     * A held breach eats {@code mb} of essence: it draws the Thinning out of the land around (twice as much for unknown
     * essence), wears its anchors' crystals, and what it ate adds up to a birth. Returns false when it is not held.
     */
    public boolean eat(ServerLevel level, float mb, float unknownShare) {
        if (!subdued() || mb <= 0) {
            return false;
        }
        net.minecraft.world.level.chunk.LevelChunk land = Thinning.sickestAround(level, blockPosition(), EAT_RADIUS);
        if (land != null) {
            Thinning.drain(level, land, mb * EAT_DRAIN * (1 + unknownShare));
        }
        for (BlockPos pos : anchors) {
            if (level.getBlockEntity(pos) instanceof com.pockyl.hextale.block.VoidAnchorBlockEntity anchor) {
                anchor.strain(level, mb * EAT_STRAIN);
            }
        }
        bigMeal |= mb >= BIRTH_MB;
        fed += mb;
        long now = level.getGameTime();
        entityData.set(DATA_ATE, now);
        if (now - lastAteNote > 100) {
            lastAteNote = now;
            level.playSound(null, getX(), getY(), getZ(), ModSounds.THE_VOID_EAT.get(), SoundSource.AMBIENT, 0.8F, 0.9F + random.nextFloat() * 0.2F);
            Quests.fireNear(level, position(), Breaches.WATCH_RADIUS, Quests.VOID_ATE);
        }
        return true;
    }

    /** What is thrown into a held breach is swallowed, worth the essence it carries. */
    private void eatThrown(ServerLevel level) {
        VoidLayout layout = layout();
        float reveal = reveal(0);
        boolean lying = shape().lying();
        Quaternionf inverse = orientation().conjugate(new Quaternionf());
        AABB area = new AABB(position(), position()).inflate(layout.reach() + 1);
        for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
                net.minecraft.world.entity.Entity::isAlive)) {
            Vec3 centre = item.position().add(0, 0.125, 0);
            Vector3f local = inverse.transform(new Vector3f((float) (centre.x - getX()), (float) (centre.y - getY()), (float) (centre.z - getZ())));
            if (Math.abs(local.z) > 0.4F + layout.halfThickness() || !layout.touches(local.x, local.y, 0.25F, 0.25F, reveal)) {
                continue;
            }
            net.minecraft.world.item.ItemStack stack = item.getItem();
            int points = 0;
            for (int amount : com.pockyl.hextale.research.ItemEssenceTable.get(stack.getItem()).essences().values()) {
                points += amount;
            }
            float meal = Math.max(1, points) * ITEM_MEAL * stack.getCount();
            item.discard();
            level.sendParticles(ModParticles.vapour(SMOKE), centre.x, centre.y, centre.z, 6, 0.2, 0.2, 0.2, 0.02);
            level.sendParticles(ModParticles.glint(PINK), centre.x, centre.y, centre.z, 4, 0.2, 0.2, 0.2, 0.02);
            eat(level, meal, 0);
        }
    }

    /**
     * Healed land does not feed it: below {@link #FADES_BELOW} it starves, and after {@link #STARVE_TICKS} it closes for
     * good. Land that sickens again feeds it back. Returns whether it closed.
     */
    private boolean starve(ServerLevel level) {
        int starved = entityData.get(DATA_STARVED);
        if (Thinning.get(level, blockPosition()) >= FADES_BELOW) {
            if (starved > 0) {
                entityData.set(DATA_STARVED, Math.max(0, starved - FEED_TICKS * 2));
            }
            return false;
        }
        starved += FEED_TICKS;
        entityData.set(DATA_STARVED, starved);
        if (starved < STARVE_TICKS) {
            return false;
        }
        close();
        Quests.fireNear(level, position(), Breaches.WATCH_RADIUS, Quests.BREACH_FADED);
        return true;
    }

    /** Throws back, hurt, every creature that touches the breach (the Void's own spawn excepted). */
    private void touch(ServerLevel level) {
        VoidLayout layout = layout();
        float reveal = reveal(0);
        boolean lying = shape().lying();
        Quaternionf inverse = orientation().conjugate(new Quaternionf());
        AABB area = new AABB(position(), position()).inflate(layout.reach() + 1.5);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, TheVoid::touchable)) {
            if (living.getInvulnerableTime() > 10) {
                continue;
            }
            Vec3 centre = living.getBoundingBox().getCenter();
            Vector3f local = inverse.transform(new Vector3f((float) (centre.x - getX()), (float) (centre.y - getY()), (float) (centre.z - getZ())));
            float halfW = living.getBbWidth() * 0.5F;
            float halfH = living.getBbHeight() * 0.5F;
            float depth = (lying ? halfH : halfW) + layout.halfThickness();
            if (Math.abs(local.z) > depth || !layout.touches(local.x, local.y, halfW, lying ? halfW : halfH, reveal)) {
                continue;
            }
            repel(level, living, local);
        }
    }

    private static boolean touchable(LivingEntity living) {
        return living.isAlive() && !(living instanceof Voidling) && !(living instanceof VoidMite) && !living.isSpectator()
                && !(living instanceof Player player && player.isCreative());
    }

    private void repel(ServerLevel level, LivingEntity living, Vector3f local) {
        Vector3f away = orientation().transform(new Vector3f(0, 0, local.z >= 0 ? 1 : -1));
        living.hurtServer(level, level.damageSources().magic(), TOUCH_DAMAGE);
        living.setDeltaMovement(away.x * 0.9, Math.max(0.35, away.y * 0.9), away.z * 0.9);
        living.needsSync = true;
        living.setInvulnerableTime(Math.max(living.getInvulnerableTime(), 20));
        Vec3 at = toWorld(new Vector3f(local.x, local.y, 0));
        level.broadcastEntityEvent(this, REPEL_EVENT);
        level.sendParticles(ModParticles.ring(MAGENTA), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.glint(PINK), at.x, at.y, at.z, 12, 0.25, 0.25, 0.25, 0.02);
        level.playSound(null, at.x, at.y, at.z, ModSounds.THE_VOID_REPEL.get(), SoundSource.HOSTILE, 1.0F, 0.85F + random.nextFloat() * 0.3F);
    }

    /** A voidling may come out: few of them around, a player near to meet it, a world that allows monsters. */
    private boolean maySend(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        AABB around = getBoundingBox().inflate(40);
        if (level.getEntitiesOfClass(Voidling.class, around).size() >= VOIDLING_LIMIT) {
            return false;
        }
        return level.getNearestPlayer(getX(), getY(), getZ(), 48, player -> !player.isSpectator()) != null;
    }

    /** Something starts pushing out of the lower half of the breach. */
    public void startPush(ServerLevel level) {
        VoidLayout layout = layout();
        Vector3f point = layout.samplePoint(random, false);
        for (int i = 0; i < 12 && (point.y > 0 || Math.abs(point.x) > layout.reach() * 0.5F); i++) {
            point = layout.samplePoint(random, false);
        }
        entityData.set(DATA_PUSH, point);
        entityData.set(DATA_PUSH_AT, level.getGameTime());
        Vec3 at = toWorld(point);
        level.playSound(null, at.x, at.y, at.z, ModSounds.VOIDLING_EMERGE.get(), SoundSource.HOSTILE, 1.0F, 0.9F + random.nextFloat() * 0.2F);
    }

    /** The cells give way and the voidling drops out of the front. */
    private void emerge(ServerLevel level) {
        nextPush = level.getGameTime() + PUSH_PAUSE + random.nextInt(PUSH_PAUSE);
        Voidling voidling = ModEntities.VOIDLING.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (voidling == null) {
            return;
        }
        Vector3f normal = normal();
        Vec3 at = toWorld(new Vector3f(entityData.get(DATA_PUSH)));
        voidling.snapTo(at.x + normal.x * 0.45, at.y + normal.y * 0.45 - voidling.getBbHeight() * 0.5, at.z + normal.z * 0.45,
                getYRot(), 0);
        voidling.setYHeadRot(getYRot());
        voidling.setYBodyRot(getYRot());
        voidling.setDeltaMovement(normal.x * 0.32, 0.18 + normal.y * 0.32, normal.z * 0.32);
        voidling.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), EntitySpawnReason.MOB_SUMMONED, null);
        voidling.emerged();
        if (feeding) {
            if (bigMeal) {
                voidling.setStage(Voidling.GROWN);
            }
            Quests.fireNear(level, position(), Breaches.WATCH_RADIUS, Quests.VOID_BIRTH);
        }
        if (feeding || heldSides() > 0) {
            // A ring closing round it makes it fight: the next one comes sooner.
            nextPush = level.getGameTime() + (PUSH_PAUSE + random.nextInt(PUSH_PAUSE)) / FIGHT_FACTOR;
        }
        feeding = false;
        bigMeal = false;
        level.addFreshEntity(voidling);
        level.sendParticles(ModParticles.vapour(SMOKE), at.x, at.y, at.z, 14, 0.3, 0.3, 0.3, 0.03);
        level.sendParticles(ModParticles.glint(MAGENTA), at.x, at.y, at.z, 8, 0.3, 0.3, 0.3, 0.05);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id != REPEL_EVENT) {
            super.handleEntityEvent(id);
            return;
        }
        // The whole rim flares as it throws back what touched it.
        VoidLayout layout = layout();
        for (int i = 0; i < 26; i++) {
            Vec3 at = toWorld(layout.samplePoint(random, true));
            level().addParticle(ModParticles.glint(i % 3 == 0 ? PINK : MAGENTA), at.x, at.y, at.z, 0, 0, 0);
        }
    }

    /**
     * The omen: the ground trembles (dust of the blocks around jumps up), the world's motes are drawn to the point the
     * breach will tear from, faster and closer as it nears, and at the end a seed of its light burns there.
     */
    private void omenEffects() {
        RandomSource random = level().getRandom();
        float k = omen(0);
        Vec3 centre = position();
        float size = size();
        for (float chance = 0.6F + k * 3.5F; chance > 0; chance -= 1) {
            if (random.nextFloat() >= chance) {
                break;
            }
            float reach = (2.5F + random.nextFloat() * 5) * (1.1F - k * 0.4F) * (0.7F + size * 0.1F);
            Vector3f out = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F).normalize(reach);
            int colour = random.nextInt(9) == 0 ? MAGENTA : random.nextInt(3) == 0 ? UMBER : DEEP;
            level().addParticle(ModParticles.voidMote(colour), centre.x, centre.y, centre.z, out.x, out.y, out.z);
        }
        if (random.nextFloat() < 0.25F + k * 0.9F) {
            // The ground shakes: dust of what lies around jumps.
            int x = Mth.floor(centre.x + (random.nextFloat() - 0.5F) * 14);
            int z = Mth.floor(centre.z + (random.nextFloat() - 0.5F) * 14);
            BlockPos ground = new BlockPos(x, level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
            BlockState state = level().getBlockState(ground);
            if (!state.isAir()) {
                for (int i = 0; i < 3; i++) {
                    level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x + random.nextFloat(), ground.getY() + 1.05,
                            z + random.nextFloat(), 0, 0.1 + k * 0.2, 0);
                }
            }
        }
        if (random.nextFloat() < k * 0.35F) {
            Vector3f drift = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F)
                    .normalize(1.5F + random.nextFloat() * 2);
            level().addParticle(ModParticles.vapour(SMOKE), centre.x + drift.x, centre.y + drift.y, centre.z + drift.z,
                    -drift.x * 0.02, -drift.y * 0.02, -drift.z * 0.02);
        }
        if (k > 0.55F && random.nextFloat() < k * 0.5F) {
            level().addParticle(ModParticles.glint(random.nextBoolean() ? MAGENTA : PINK), centre.x, centre.y, centre.z, 0, 0, 0);
        }
    }

    private void clientEffects() {
        RandomSource random = level().getRandom();
        VoidLayout layout = layout();
        float size = size();
        float age = age(0);
        float reveal = reveal(0);
        Vector3f normal = normal();

        Vector3fc pushing = pushPoint();
        float strain = push(0);
        if (pushing != null && strain > 0.2F && random.nextFloat() < strain) {
            // Something strains against the cells: dark smoke squeezes out round it.
            Vec3 at = toWorld(new Vector3f(pushing).add((random.nextFloat() - 0.5F) * 0.8F, (random.nextFloat() - 0.5F) * 0.8F, 0));
            level().addParticle(ModParticles.vapour(random.nextInt(3) == 0 ? DEEP : SMOKE), at.x, at.y, at.z,
                    normal.x * 0.03, normal.y * 0.03 + 0.01, normal.z * 0.03);
        }

        if (age < openTicks() && !closing()) {
            // Breaking through: dark smoke bursts out of the cells as they appear.
            for (int i = 0; i < 3; i++) {
                Vec3 at = toWorld(layout.samplePoint(random, true).mul(Math.max(0.2F, reveal)));
                float push = (random.nextBoolean() ? 1 : -1) * 0.04F;
                level().addParticle(ModParticles.vapour(SMOKE), at.x, at.y, at.z, normal.x * push, normal.y * push, normal.z * push);
            }
            if ((int) age == 2) {
                Vec3 centre = position();
                level().addParticle(ModParticles.ring(MAGENTA), centre.x, centre.y, centre.z, size * 0.6, 0, 0);
            }
        }

        // The world drifting in: motes, mostly dark, spiral into the breach from in front of it and behind it. A held
        // breach draws them slow and cold; one that has just eaten, hungrily.
        float calm = calm();
        boolean eating = level().getGameTime() - ateAt() < 30;
        float pull = (closing() ? 2.5F : 1) * (1 - 0.6F * calm) * (eating ? 3 : 1);
        float chance = Math.min(3, (0.35F + size * 0.22F) * pull * reveal);
        for (; chance > 0; chance -= 1) {
            if (random.nextFloat() >= chance) {
                break;
            }
            Vec3 target = toWorld(layout.samplePoint(random, false));
            float reach = (0.9F + random.nextFloat() * 1.4F) * (0.6F + size * 0.18F);
            Vector3f out = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F);
            out.add(new Vector3f(normal).mul((random.nextBoolean() ? 1 : -1) * 0.7F)).normalize().mul(reach);
            int colour = switch (random.nextInt(7)) {
                case 0 -> random.nextFloat() < calm ? COLD : MAGENTA;
                case 1 -> random.nextFloat() < calm ? COLD : PINK;
                case 2 -> VIOLET;
                case 3 -> UMBER;
                default -> DEEP;
            };
            level().addParticle(ModParticles.voidMote(colour), target.x, target.y, target.z, out.x, out.y, out.z);
        }

        // It breathes out: dark smoke seeps from the edge and drifts away from it, slow and heavy.
        if (random.nextFloat() < (0.3F + size * 0.12F) * reveal) {
            Vector3f edge = layout.samplePoint(random, true);
            Vec3 at = toWorld(edge);
            Vector3f out = orientation().transform(new Vector3f(edge.x, edge.y, 0).normalize(0.025F));
            float side = (random.nextFloat() - 0.5F) * 0.03F;
            level().addParticle(ModParticles.vapour(random.nextInt(4) == 0 ? DEEP : SMOKE), at.x, at.y, at.z,
                    out.x + normal.x * side, out.y + normal.y * side, out.z + normal.z * side);
        }
        // Now and then a sickly spark of its light drifts off the edge (rarely from a held one).
        if (random.nextFloat() < (0.05F + size * 0.02F) * reveal * (1 - 0.8F * calm)) {
            Vec3 at = toWorld(layout.samplePoint(random, true));
            level().addParticle(ModParticles.glint(random.nextBoolean() ? MAGENTA : PINK), at.x, at.y, at.z, 0, 0, 0);
        }

        if (closeProgress(0) > 0.8F && !burstShown) {
            // The last breath as it shuts: a ring pulled tight and a puff of dark.
            burstShown = true;
            Vec3 centre = position();
            level().addParticle(ModParticles.ring(VIOLET), centre.x, centre.y, centre.z, size * 0.5, 0, 0);
            for (int i = 0; i < 10; i++) {
                level().addParticle(ModParticles.vapour(DEEP), centre.x, centre.y, centre.z,
                        (random.nextFloat() - 0.5F) * 0.08, (random.nextFloat() - 0.5F) * 0.08, (random.nextFloat() - 0.5F) * 0.08);
            }
        }
    }

    /** The breach the player looks at within {@code reach} blocks, nearest first. */
    public static @Nullable TheVoid aimedAt(Player player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB area = new AABB(eye, eye.add(look.scale(reach))).inflate(8);
        TheVoid best = null;
        double bestAlong = reach;
        for (TheVoid breach : player.level().getEntitiesOfClass(TheVoid.class, area, breach -> !breach.closing())) {
            Vec3 to = breach.position().subtract(eye);
            double along = to.dot(look);
            if (along < 0 || along > bestAlong) {
                continue;
            }
            double off = to.subtract(look.scale(along)).length();
            if (off <= breach.layout().reach() * 0.8 + 0.5) {
                best = breach;
                bestAlong = along;
            }
        }
        return best;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 64 + size() * 12;
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // Summoned by a command without numbers: a random outline that breaks open from now on.
        entityData.set(DATA_SEED, input.getIntOr("seed", level().getRandom().nextInt()));
        entityData.set(DATA_SHAPE, VoidShape.byId(input.getStringOr("shape", VoidShape.GATE.id())).ordinal());
        entityData.set(DATA_SIZE, Mth.clamp(input.getFloatOr("size", 3), 0.5F, 16));
        entityData.set(DATA_BORN, input.getLongOr("born", level().getGameTime()));
        entityData.set(DATA_CLOSE_AT, input.getLongOr("close_at", -1));
        entityData.set(DATA_ROOTED, input.getBooleanOr("rooted", false));
        entityData.set(DATA_OMEN, input.getIntOr("omen", 0));
        entityData.set(DATA_OPEN, Math.max(1, input.getIntOr("open", OPEN_TICKS)));
        nextPush = input.getLongOr("next_push", rooted() ? level().getGameTime() + PUSH_PAUSE : -1);
        entityData.set(DATA_STARVED, input.getIntOr("starved", 0));
        entityData.set(DATA_HELD, input.getIntOr("held", 0));
        entityData.set(DATA_CALM, input.getFloatOr("calm", 0));
        fed = input.getFloatOr("fed", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("seed", seed());
        output.putString("shape", shape().id());
        output.putFloat("size", size());
        output.putLong("born", entityData.get(DATA_BORN));
        output.putLong("close_at", entityData.get(DATA_CLOSE_AT));
        output.putBoolean("rooted", rooted());
        output.putInt("omen", entityData.get(DATA_OMEN));
        output.putInt("open", openTicks());
        output.putLong("next_push", nextPush);
        output.putInt("starved", entityData.get(DATA_STARVED));
        output.putInt("held", heldSides());
        output.putFloat("calm", calm());
        output.putFloat("fed", fed);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason.shouldDestroy() && level() instanceof ServerLevel level && rooted()) {
            Breaches.forget(level, this);
        }
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }
}
