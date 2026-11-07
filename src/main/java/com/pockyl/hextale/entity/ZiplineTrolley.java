package com.pockyl.hextale.entity;

import com.pockyl.hextale.block.ChainAnchorBlockEntity;
import com.pockyl.hextale.chem.ChemFuel;
import com.pockyl.hextale.chem.ChemGear;
import com.pockyl.hextale.chem.Zipline;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import com.pockyl.hextale.port.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * The trolley hook a player hangs from on a Chain Launcher's line. It runs along the line's sag by a parameter t (0 at
 * the end the line was shot from, 1 at the far end): downhill gravity is free (up to {@link Zipline#MAX_SPEED}); forward / back drive the
 * pneumatic motor towards / away from where the rider looks, paid from the worn Chem Tank (speed by fuel tier, one
 * action per {@link Zipline#BLOCKS_PER_ACTION} blocks, no Thinning); a key against the motion brakes. Sneak lets go and
 * keeps the speed. At a post where lines join, the ride runs on along the line that goes on most nearly straight;
 * at a last end, or one with no line onwards, the rider is set down. Over ground close under the line the rider draws
 * up towards the chain and rides on; only a block too near the chain for that stops the ride in front of it. A ride
 * keeps the speed it was mounted with, and a last end slows it softly and sets the rider down with what is left.
 *
 * <p>The server decides the ride. A plain entity has no client interpolation: position packets would set it in steps
 * while the rider and the camera follow the client's ticks, so the grip would jerk against the view. Clients therefore
 * run the same line themselves from the synced ends, t and speed, easing towards the server's t; the trolley, its turn
 * and the rider all come from that one value. Only while the ends are not loaded do packets move it.
 */
public class ZiplineTrolley extends Entity {
    private static final EntityDataAccessor<Vector3f> DIRECTION = SynchedEntityData.defineId(ZiplineTrolley.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(ZiplineTrolley.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> BRAKING = SynchedEntityData.defineId(ZiplineTrolley.class, EntityDataSerializers.BOOLEAN);
    /** How far the rider is drawn up from the full hang, to clear the ground under the line. */
    private static final EntityDataAccessor<Float> LIFT = SynchedEntityData.defineId(ZiplineTrolley.class, EntityDataSerializers.FLOAT);
    /** The line's ends and the place on it, for clients to run the ride themselves. */
    private static final EntityDataAccessor<BlockPos> OWNER = SynchedEntityData.defineId(ZiplineTrolley.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> OTHER = SynchedEntityData.defineId(ZiplineTrolley.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Float> PLACE = SynchedEntityData.defineId(ZiplineTrolley.class, EntityDataSerializers.FLOAT);
    /** From the chain down to the entity's own position (it hangs from the chain by its wheels). */
    public static final double BELOW_CHAIN = 0.2;
    /** Blocks from an end at which a blocked rider counts as arrived. */
    private static final double ARRIVE_NEAR = 3;
    /** How far a rider can draw up: the feet stay this far under the chain. A block closer to the chain is in the way. */
    private static final double MAX_LIFT = Zipline.HANG - 0.3;
    /** Air kept under the (slightly deflated) body when it is drawn up over a block. */
    private static final double LIFT_GAP = 0.06;
    /** How far ahead the rider sees the ground coming at the least, the step between looks, and the most draw per block ahead. */
    private static final double LOOK_AHEAD = 2.5;
    private static final double LOOK_STEP = 0.25;
    private static final double LIFT_PER_BLOCK = 0.8;
    /** Fastest draw up at a standstill and what each block a tick of speed adds, and the fastest let down, blocks a tick. */
    private static final double LIFT_RISE = 0.25;
    private static final double LIFT_RISE_PER_SPEED = 0.35;
    private static final double LIFT_SINK = 0.06;
    /** Speed lost to the line per tick: let go of the keys and the ride coasts far. */
    private static final double DRAG = 0.998;
    /** Speed a rider reaches a last end with, blocks a tick; before it the ride slows at the brake's steady rate. */
    private static final double END_SPEED = 0.2;
    /** Share of the distance to the server's t a client makes up per tick, and the distance in blocks it jumps at once. */
    private static final double FOLLOW = 0.1;
    private static final double FOLLOW_JUMP = 4;
    /** Share of the distance to the server's draw a client makes up per tick. */
    private static final double FOLLOW_LIFT = 0.3;
    /** The rider's body behind the grip: degrees per block a tick of speed gained in a tick, the most, spring and damping. */
    private static final float SWING_PER_PUSH = 420;
    private static final float SWING_MOST = 22;
    private static final float SWING_SPRING = 0.12F;
    private static final float SWING_DAMPING = 0.82F;
    /** Speed a let-go rider keeps, applied the tick after the dismount teleport. */
    private static final Map<UUID, Vec3> FLING = new ConcurrentHashMap<>();
    /** Below this speed a ride does not run on at a junction: a rider swinging to rest between two lines is set down. */
    private static final double ONWARD_SPEED = 0.08;
    /** How straight a line must go on at a junction to take the ride (the cosine of about 70 degrees). */
    private static final double ONWARD_DOT = 0.35;

    /** The line's ends: the one it was shot from (t 0) and the far one (t 1). */
    private BlockPos owner = BlockPos.ZERO;
    private BlockPos other = BlockPos.ZERO;
    private double t;
    /** Signed speed along the line, blocks a tick (+ towards the claw). */
    private double speed;
    private double motorBlocks;
    /** False until the rider first hangs free (a ride may start with the feet on the ground at a post). */
    private boolean cleared;
    private boolean bumped;
    private @Nullable Vec3 arrival;
    private int noFuelSaid;
    /** Client: the line it runs on itself, null while packets move it. */
    private Zipline.@Nullable Line shownLine;
    private double shownT;
    private double shownOldT;
    private double shownLift;
    private double shownSpeed;
    private float swing;
    private float oldSwing;
    private float swingSpeed;

    public ZiplineTrolley(EntityType<? extends ZiplineTrolley> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (!FLING.isEmpty() && event.getEntity() instanceof ServerPlayer player && !player.isPassenger()) {
                Vec3 fling = FLING.remove(player.getUUID());
                if (fling != null) {
                    player.setDeltaMovement(fling);
                    // needsSync would tell only those watching: a player's own client moves them and needs this.
                    player.hurtMarked = true;
                }
            }
        });
    }

    /** Hangs the player at {@code t} on the line shot from {@code owner} to {@code other}; returns whether they now ride. */
    public static boolean mount(ServerLevel level, ServerPlayer player, BlockPos owner, BlockPos other, double t) {
        Zipline.Line line = ChainAnchorBlockEntity.line(level, owner, other);
        if (line == null || player.isPassenger() || player.isSpectator()) {
            return false;
        }
        ZiplineTrolley trolley = new ZiplineTrolley(ModEntities.ZIPLINE_TROLLEY.get(), level);
        trolley.owner = owner.immutable();
        trolley.other = other.immutable();
        trolley.t = Math.clamp(t, 0, 1);
        // Known movement, not delta movement: the server only knows a player's speed from what the client reported.
        trolley.speed = Math.clamp(player.getKnownMovement().dot(line.tangent(trolley.t).normalize()), -Zipline.MAX_SPEED, Zipline.MAX_SPEED);
        trolley.entityData.set(SPEED, (float) trolley.speed);
        trolley.place(line);
        double lift = clearance(level, player, line, trolley.t);
        trolley.entityData.set(LIFT, Double.isNaN(lift) ? 0F : (float) lift);
        if (!level.addFreshEntity(trolley)) {
            return false;
        }
        if (!player.startRiding(trolley, true)) {
            trolley.discard();
            return false;
        }
        level.playSound(null, trolley.getX(), trolley.getY(), trolley.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.6F, 1.1F);
        return true;
    }

    public double t() {
        return t;
    }

    public double speed() {
        return speed;
    }

    public Vector3fc direction() {
        return entityData.get(DIRECTION);
    }

    public float syncedSpeed() {
        return entityData.get(SPEED);
    }

    public boolean braking() {
        return entityData.get(BRAKING);
    }

    /** How far the rider is drawn up from the full hang. */
    public double lift() {
        return shownLine != null ? shownLift : entityData.get(LIFT);
    }

    /** Client: which way the trolley points on this frame, along the line it runs on. */
    public Vec3 shownDirection(float partialTick) {
        if (shownLine != null) {
            return shownLine.tangent(Mth.lerp(partialTick, shownOldT, shownT)).normalize();
        }
        Vector3fc dir = direction();
        return new Vec3(dir.x(), dir.y(), dir.z());
    }

    /** Client: how far the rider's body swings ahead of the grip along the line on this frame, in degrees. */
    public float swing(float partialTick) {
        return Mth.lerp(partialTick, oldSwing, swing);
    }

    /** Where the rider's feet are now. */
    public Vec3 riderFeet() {
        return position().add(0, BELOW_CHAIN - Zipline.HANG + lift(), 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DIRECTION, new Vector3f(1, 0, 0));
        builder.define(SPEED, 0F);
        builder.define(BRAKING, false);
        builder.define(LIFT, 0F);
        builder.define(OWNER, BlockPos.ZERO);
        builder.define(OTHER, BlockPos.ZERO);
        builder.define(PLACE, 0F);
    }

    /** Position packets count only while the client cannot run the line itself. */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (shownLine == null) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
        }
    }

    private void place(Zipline.Line line) {
        Vec3 at = line.point(t);
        setPos(at.x, at.y - BELOW_CHAIN, at.z);
        Vec3 dir = line.tangent(t).normalize();
        entityData.set(DIRECTION, new Vector3f((float) dir.x, (float) dir.y, (float) dir.z));
        entityData.set(OWNER, owner);
        entityData.set(OTHER, other);
        entityData.set(PLACE, (float) t);
    }

    /** Client: one tick of the ride along the line, from the synced speed, easing towards the server's place. */
    private void tickShown() {
        Zipline.Line line = ChainAnchorBlockEntity.line(level(), entityData.get(OWNER), entityData.get(OTHER));
        if (line == null) {
            shownLine = null;
            return;
        }
        double server = entityData.get(PLACE);
        double pace = entityData.get(SPEED);
        if (!line.equals(shownLine)) {
            // A new ride or the next line of a junction: its start is where the last one ended, so nothing jumps.
            boolean first = shownLine == null;
            shownLine = line;
            shownT = server;
            shownOldT = server;
            if (first) {
                shownLift = entityData.get(LIFT);
                shownSpeed = pace;
            }
        }
        double length = Math.max(0.1, line.tangent(shownT).length());
        double ahead = shownT + pace / length;
        double miss = server - ahead;
        ahead = Math.abs(miss) * length > FOLLOW_JUMP ? server : ahead + miss * FOLLOW;
        shownOldT = shownT;
        shownT = Math.clamp(ahead, 0, 1);
        Vec3 at = line.point(shownT);
        setPos(at.x, at.y - BELOW_CHAIN, at.z);
        shownLift += (entityData.get(LIFT) - shownLift) * FOLLOW_LIFT;

        // The body hangs from the grip, so a change of speed swings it behind and it settles back.
        double push = Math.abs(pace) - Math.abs(shownSpeed);
        double way = Math.abs(pace) > 1.0E-4 ? Math.signum(pace) : Math.signum(shownSpeed);
        shownSpeed = pace;
        float target = Mth.clamp((float) (-push * way) * SWING_PER_PUSH, -SWING_MOST, SWING_MOST);
        oldSwing = swing;
        swingSpeed = (swingSpeed + (target - swing) * SWING_SPRING) * SWING_DAMPING;
        swing += swingSpeed;
    }

    /** Where the rider's feet hang at {@code t}. */
    private static Vec3 feet(Zipline.Line line, double t) {
        return line.point(t).subtract(0, Zipline.HANG, 0);
    }

    /** How far up the rider must draw to fit at {@code t} (0 hangs free), or NaN where no draw gets past the blocks there. */
    private static double clearance(ServerLevel level, Player rider, Zipline.Line line, double t) {
        AABB body = rider.getDimensions(rider.getPose()).makeBoundingBox(feet(line, t)).deflate(0.05);
        double lift = 0;
        for (int i = 0; i < 4; i++) {
            double top = Double.NEGATIVE_INFINITY;
            for (VoxelShape shape : level.getBlockCollisions(rider, body.move(0, lift, 0))) {
                top = Math.max(top, shape.max(Direction.Axis.Y));
            }
            if (top == Double.NEGATIVE_INFINITY) {
                return lift;
            }
            lift = top - body.minY + LIFT_GAP;
            if (lift > MAX_LIFT) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    /** Draws the rider up ahead of ground coming under the line and lets them down slowly after it, so nothing jolts. */
    private void settle(ServerLevel level, Player rider, Zipline.Line line, double length) {
        double here = clearance(level, rider, line, t);
        if (Double.isNaN(here)) {
            return;
        }
        double wanted = here;
        int way = (int) Math.signum(speed);
        // At speed the draw must start earlier and climb less per block, or the ground arrives before the feet are up.
        double pace = Math.abs(speed);
        double rise = LIFT_RISE + pace * LIFT_RISE_PER_SPEED;
        double perBlock = pace > 1.0E-3 ? Math.min(LIFT_PER_BLOCK, rise / pace) : LIFT_PER_BLOCK;
        double reach = Math.max(LOOK_AHEAD, MAX_LIFT / perBlock);
        for (double ahead = LOOK_STEP; way != 0 && ahead <= reach; ahead += LOOK_STEP) {
            double at = t + way * ahead / length;
            double needed = at <= 0 || at >= 1 ? Double.NaN : clearance(level, rider, line, at);
            if (Double.isNaN(needed)) {
                break;
            }
            wanted = Math.max(wanted, needed - ahead * perBlock);
        }
        double lift = lift();
        lift = wanted > lift ? Math.max(here, Math.min(wanted, lift + rise)) : Math.max(wanted, lift - LIFT_SINK);
        entityData.set(LIFT, (float) lift);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            tickShown();
            return;
        }
        Entity passenger = getFirstPassenger();
        Zipline.Line line = ChainAnchorBlockEntity.line(server, owner, other);
        if (!(passenger instanceof Player rider) || line == null) {
            ejectPassengers();
            discard();
            return;
        }
        rider.resetFallDistance();
        Vec3 tangent = line.tangent(t);
        double length = Math.max(0.1, tangent.length());
        Vec3 along = tangent.scale(1 / length);
        Input input = rider instanceof ServerPlayer serverPlayer ? com.pockyl.hextale.port.Input.lastClient(serverPlayer) : Input.EMPTY;
        int intent = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        double look = rider.getLookAngle().dot(along);
        int want = intent * (look >= 0 ? 1 : -1);
        boolean braking = false;
        boolean driving = false;
        if (want != 0 && speed * want < -0.01) {
            // Against the motion: a steady brake, and the clamp holds the trolley against gravity.
            speed -= Math.copySign(Math.min(Math.abs(speed), Zipline.BRAKE), speed);
            braking = Math.abs(speed) > 0.03;
        } else {
            ChemFuel fuel = want == 0 ? null : rider.getAbilities().instabuild ? ChemFuel.REFINED_SHIMMER : ChemGear.power(rider);
            boolean uphill = along.y * want > 0.01;
            if (fuel != null) {
                double top = Zipline.motorSpeed(fuel);
                double pace = speed * want;
                if (pace < top) {
                    // Strong from a standstill, easing towards the top.
                    double left = pace <= 0 ? 1 : 1 - pace / top * (pace / top);
                    double push = Zipline.MOTOR_PUSH * Math.max(0.08, left);
                    speed = want > 0 ? Math.min(top, speed + push) : Math.max(-top, speed - push);
                    driving = true;
                }
            }
            if (fuel == null || !uphill) {
                // Downhill the motor only adds to gravity, past its own speed the trolley runs free.
                speed -= Zipline.GRAVITY * along.y;
            }
            if (want != 0 && fuel == null && uphill && rider.tickCount - noFuelSaid > 60) {
                noFuelSaid = rider.tickCount;
                rider.displayClientMessage(Component.translatable("entity.hextale.zipline_trolley.no_fuel"), true);
            }
        }
        speed *= DRAG;
        if (Math.abs(speed) < 0.003 && Math.abs(along.y) < 0.03) {
            speed = 0;
        }
        speed = Math.clamp(speed, -Zipline.MAX_SPEED, Zipline.MAX_SPEED);

        double toEnd = (speed > 0 ? 1 - t : t) * line.length();
        double softest = Math.sqrt(END_SPEED * END_SPEED + 2 * Zipline.BRAKE * toEnd);
        if (Math.abs(speed) > softest && onward(server, line, speed > 0) == null) {
            // A last end ahead: from here on it would take the brake's rate to arrive gently.
            speed = Math.copySign(softest, speed);
        }

        double next = t + speed / length;
        if (next >= 1 || next <= 0) {
            if (!runOn(server, line, next >= 1, next)) {
                arrive(server, rider, line, next >= 1);
            }
            return;
        }
        boolean free = !Double.isNaN(clearance(server, rider, line, next));
        if (!free && cleared && toEnd < ARRIVE_NEAR && onward(server, line, speed > 0) != null) {
            // Close to a junction post a block by the post may be in the way: the ride goes on to the next line.
            free = true;
        }
        if (!free && cleared) {
            if (toEnd < ARRIVE_NEAR) {
                arrive(server, rider, line, speed > 0);
                return;
            }
            if (!bumped && Math.abs(speed) > 0.05) {
                server.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8F, 0.7F);
            }
            bumped = true;
            speed = 0;
        } else {
            bumped = false;
            cleared |= free;
            if (driving) {
                motorBlocks += Math.abs(speed);
                while (motorBlocks >= Zipline.BLOCKS_PER_ACTION) {
                    motorBlocks -= Zipline.BLOCKS_PER_ACTION;
                    ChemGear.spend(rider, 1);
                }
                if (tickCount % 4 == 0) {
                    server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 1, 0.05, 0.02, 0.05, 0.005);
                }
            }
            t = next;
            place(line);
        }
        settle(server, rider, line, length);
        if (braking && tickCount % 5 == 0) {
            server.playSound(null, getX(), getY(), getZ(), ModSounds.ZIPLINE_BRAKE.get(), SoundSource.PLAYERS, 0.3F, 0.9F + random.nextFloat() * 0.2F);
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.25, getZ(), 2, 0.05, 0.02, 0.05, 0.01);
        }
        entityData.set(SPEED, (float) speed);
        entityData.set(BRAKING, braking);
    }

    /** The line leaving the end the ride reaches (the far end when {@code atOther}) most nearly straight on, or null. */
    private ChainAnchorBlockEntity.@Nullable Link onward(ServerLevel level, Zipline.Line line, boolean atOther) {
        BlockPos node = atOther ? other : owner;
        BlockPos from = atOther ? owner : other;
        if (Math.abs(speed) < ONWARD_SPEED || !level.isLoaded(node) || !(level.getBlockEntity(node) instanceof ChainAnchorBlockEntity anchor)) {
            return null;
        }
        Vec3 travel = line.tangent(atOther ? 1 : 0).normalize().scale(atOther ? 1 : -1);
        ChainAnchorBlockEntity.Link best = null;
        double bestDot = ONWARD_DOT;
        for (ChainAnchorBlockEntity.Link link : anchor.links()) {
            if (link.flying() || link.other().equals(from)) {
                continue;
            }
            double dot = link.away().dot(travel);
            if (dot > bestDot) {
                bestDot = dot;
                best = link;
            }
        }
        return best;
    }

    /** At a post where lines join, the ride goes on along the next line with its speed; returns whether it did. */
    private boolean runOn(ServerLevel level, Zipline.Line line, boolean atOther, double next) {
        ChainAnchorBlockEntity.Link link = onward(level, line, atOther);
        if (link == null) {
            return false;
        }
        BlockPos node = atOther ? other : owner;
        Zipline.Line onto = link.line();
        double over = Math.abs(atOther ? next - 1 : next) * line.length() / Math.max(0.1, onto.length());
        owner = link.owner() ? node : link.other();
        other = link.owner() ? link.other() : node;
        double magnitude = Math.abs(speed);
        speed = link.owner() ? magnitude : -magnitude;
        t = link.owner() ? Math.min(0.5, over) : Math.max(0.5, 1 - over);
        cleared = false;
        place(onto);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.4F, 1.2F);
        entityData.set(SPEED, (float) speed);
        return true;
    }

    /** The ride reached an end: the rider is set down beside it and goes on with the speed that is left. */
    private void arrive(ServerLevel level, Player rider, Zipline.Line line, boolean atOther) {
        BlockPos cell = atOther ? other : owner;
        Vec3 back = line.tangent(atOther ? 1 : 0).normalize().scale(atOther ? -1 : 1);
        arrival = safeSpot(level, rider, cell, back, feet(line, atOther ? 1 : 0));
        Vec3 left = back.scale(-Math.abs(speed));
        speed = 0;
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.6F, 0.8F);
        rider.stopRiding();
        if (left.lengthSqr() > 1.0E-4) {
            FLING.put(rider.getUUID(), left);
        }
        discard();
    }

    /** The end's own cell, or a free spot just around it (on the line's side first). */
    private static Vec3 safeSpot(ServerLevel level, Player rider, BlockPos cell, Vec3 back, Vec3 fallback) {
        BlockPos toward = BlockPos.containing(Vec3.atCenterOf(cell).add(back.x, 0, back.z));
        BlockPos[] tries = {cell, cell.below(), toward, toward.below(), cell.above(), toward.above()};
        for (BlockPos pos : tries) {
            Vec3 spot = DismountHelper.findSafeDismountLocation(rider.getType(), level, pos, true);
            if (spot != null) {
                return spot;
            }
        }
        return fallback;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (arrival != null) {
            return arrival;
        }
        return riderFeet();
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && arrival == null && Math.abs(speed) > 0.01) {
            Vector3fc dir = direction();
            FLING.put(passenger.getUUID(), new Vec3(dir.x(), dir.y(), dir.z()).scale(speed));
        }
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        Vec3 feet = riderFeet();
        moveFunction.accept(passenger, feet.x, feet.y, feet.z);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().isEmpty();
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueInput input = ValueInput.of(portTag, registryAccess());
        owner = input.read("owner", BlockPos.CODEC).orElse(BlockPos.ZERO);
        other = input.read("other", BlockPos.CODEC).orElse(BlockPos.ZERO);
        t = input.getDoubleOr("t", 0);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, registryAccess());
        output.store("owner", BlockPos.CODEC, owner);
        output.store("other", BlockPos.CODEC, other);
        output.putDouble("t", t);
    }
}
