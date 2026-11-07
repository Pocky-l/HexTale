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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
 * up towards the chain and rides on; only a block too near the chain for that stops the ride in front of it. The server
 * moves it; clients see the vanilla interpolation and the synced direction, speed and draw.
 */
public class ZiplineTrolley extends Entity {
    private static final EntityDataAccessor<Vector3fc> DIRECTION = SynchedEntityData.defineId(ZiplineTrolley.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(ZiplineTrolley.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> BRAKING = SynchedEntityData.defineId(ZiplineTrolley.class, EntityDataSerializers.BOOLEAN);
    /** How far the rider is drawn up from the full hang, to clear the ground under the line. */
    private static final EntityDataAccessor<Float> LIFT = SynchedEntityData.defineId(ZiplineTrolley.class, EntityDataSerializers.FLOAT);
    /** From the chain down to the entity's own position (it hangs from the chain by its wheels). */
    public static final double BELOW_CHAIN = 0.2;
    /** Blocks from an end at which a blocked rider counts as arrived. */
    private static final double ARRIVE_NEAR = 3;
    /** How far a rider can draw up: the feet stay this far under the chain. A block closer to the chain is in the way. */
    private static final double MAX_LIFT = Zipline.HANG - 0.3;
    /** Air kept under the (slightly deflated) body when it is drawn up over a block. */
    private static final double LIFT_GAP = 0.06;
    /** How far ahead the rider sees the ground coming, the step between looks, and the draw that distance buys, per block. */
    private static final double LOOK_AHEAD = 2.5;
    private static final double LOOK_STEP = 0.25;
    private static final double LIFT_PER_BLOCK = 0.8;
    /** Fastest draw up and down, blocks a tick (ground right under the feet overrides the first). */
    private static final double LIFT_RISE = 0.25;
    private static final double LIFT_SINK = 0.06;
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
                    player.needsSync = true;
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
        trolley.place(line);
        double lift = clearance(level, player, line, trolley.t);
        trolley.entityData.set(LIFT, Double.isNaN(lift) ? 0F : (float) lift);
        if (!level.addFreshEntity(trolley)) {
            return false;
        }
        if (!player.startRiding(trolley, true, true)) {
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
        return entityData.get(LIFT);
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
    }

    private void place(Zipline.Line line) {
        Vec3 at = line.point(t);
        setPos(at.x, at.y - BELOW_CHAIN, at.z);
        Vec3 dir = line.tangent(t).normalize();
        entityData.set(DIRECTION, new Vector3f((float) dir.x, (float) dir.y, (float) dir.z));
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
        for (double ahead = LOOK_STEP; way != 0 && ahead <= LOOK_AHEAD; ahead += LOOK_STEP) {
            double at = t + way * ahead / length;
            double needed = at <= 0 || at >= 1 ? Double.NaN : clearance(level, rider, line, at);
            if (Double.isNaN(needed)) {
                break;
            }
            wanted = Math.max(wanted, needed - ahead * LIFT_PER_BLOCK);
        }
        double lift = lift();
        lift = wanted > lift ? Math.max(here, Math.min(wanted, lift + LIFT_RISE)) : Math.max(wanted, lift - LIFT_SINK);
        entityData.set(LIFT, (float) lift);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
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
        Input input = rider instanceof ServerPlayer serverPlayer ? serverPlayer.getLastClientInput() : Input.EMPTY;
        int intent = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        double look = rider.getLookAngle().dot(along);
        int want = intent * (look >= 0 ? 1 : -1);
        boolean braking = false;
        boolean driving = false;
        if (want != 0 && speed * want < -0.01) {
            // Against the motion: the clamp brakes and holds the trolley against gravity.
            speed *= Zipline.BRAKE;
            braking = Math.abs(speed) > 0.03;
        } else {
            ChemFuel fuel = want == 0 ? null : rider.getAbilities().instabuild ? ChemFuel.REFINED_SHIMMER : ChemGear.power(rider);
            boolean uphill = along.y * want > 0.01;
            if (fuel != null) {
                double top = Zipline.motorSpeed(fuel);
                if (speed * want < top) {
                    speed = want > 0 ? Math.min(top, speed + Zipline.MOTOR_PUSH) : Math.max(-top, speed - Zipline.MOTOR_PUSH);
                    driving = true;
                }
            }
            if (fuel == null || !uphill) {
                // Downhill the motor only adds to gravity, past its own speed the trolley runs free.
                speed -= Zipline.GRAVITY * along.y;
            }
            if (want != 0 && fuel == null && uphill && rider.tickCount - noFuelSaid > 60) {
                noFuelSaid = rider.tickCount;
                rider.sendOverlayMessage(Component.translatable("entity.hextale.zipline_trolley.no_fuel"));
            }
        }
        speed *= 0.996;
        if (Math.abs(speed) < 0.003 && Math.abs(along.y) < 0.03) {
            speed = 0;
        }
        speed = Math.clamp(speed, -Zipline.MAX_SPEED, Zipline.MAX_SPEED);

        double next = t + speed / length;
        if (next >= 1 || next <= 0) {
            if (!runOn(server, line, next >= 1, next)) {
                arrive(server, rider, line, next >= 1);
            }
            return;
        }
        boolean free = !Double.isNaN(clearance(server, rider, line, next));
        double toEnd = (speed > 0 ? 1 - t : t) * line.length();
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

    /** The ride reached an end: the rider is set down beside it. */
    private void arrive(ServerLevel level, Player rider, Zipline.Line line, boolean atOther) {
        BlockPos cell = atOther ? other : owner;
        Vec3 back = line.tangent(atOther ? 1 : 0).normalize().scale(atOther ? -1 : 1);
        arrival = safeSpot(level, rider, cell, back, feet(line, atOther ? 1 : 0));
        speed = 0;
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.6F, 0.8F);
        rider.stopRiding();
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        owner = input.read("owner", BlockPos.CODEC).orElse(BlockPos.ZERO);
        other = input.read("other", BlockPos.CODEC).orElse(BlockPos.ZERO);
        t = input.getDoubleOr("t", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("owner", BlockPos.CODEC, owner);
        output.store("other", BlockPos.CODEC, other);
        output.putDouble("t", t);
    }
}
