package com.pockyl.hextale.runework;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ImpulsePlateBlock;
import com.pockyl.hextale.network.StrikeDashPayload;
import com.pockyl.hextale.network.WeaponFxPayload;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * The Strike Gauntlet's flight: the released charge throws its wearer along the look at a steady speed, a longer charge
 * for longer, like a Riptide trident without the rain. The wearer's own client flies it, so there is no lag; the server
 * follows the real path, hurts every creature on it once and ends it at a wall or when the time is up, with the old
 * punch: rock broken where the flight met it, the creatures ahead thrown back. A flight that meets the ground or a
 * ceiling at a flat angle skims along it. Falling does not hurt during the flight nor at the first landing after it.
 *
 * <p>Only the side that really moves the wearer may push them: {@code isLocalInstanceAuthoritative}, not
 * {@code canSimulateMovement}, which is also true for a server player and would fly them a step ahead of their client.
 * The server runs its own {@code travel} for its copy of a player, so that copy is held still during a flight: under
 * gravity it would sink below the real path, and the blow at the end of it would land a block too low.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class StrikeDash {
    /** Blocks per tick: Riptide III starts at 3 and slows down, this one holds its pace. */
    public static final double SPEED = 2.2;
    /** Ticks of flight by charge stage: about 11, 20 and 29 blocks. */
    public static final int[] TICKS = {5, 9, 13};
    /** Damage to each creature on the path by charge stage. */
    public static final float[] PATH_DAMAGE = {8, 12, 16};
    /** How far from the wearer's middle the path reaches beyond a creature's box. */
    private static final double PATH_WIDTH = 1.1;
    /** The share of the speed the wearer keeps when the flight ends in the open, to coast out of it. */
    private static final double CARRY = 0.35;
    /** Flatter than this (the vertical share of the way), the ground or a ceiling is skimmed, not struck. */
    private static final double SKIM = 0.6;
    /** A wall this close to the wearer's middle ends the flight. */
    private static final double WALL_REACH = 1.2;
    /** The server waits this much longer than the client flies: the last positions arrive late. */
    private static final int LAG_TICKS = 2;
    /** Other players' clients keep the pose this much longer, until the server's word that it is over. */
    private static final int SHOWN_TICKS = 6;
    /** Hardly moving for this many ticks, the flight is over: something the wall check did not see holds the wearer. */
    private static final int STALL_TICKS = 4;
    /** A jump longer than this in a tick is a teleport: the flight just stops. */
    private static final double MAX_STEP = 12;
    /** How long after the flight the first landing stays soft. */
    private static final int SOFT_TICKS = 100;
    private static final double LIFT = 1.1;

    /**
     * Flights by player, one map a side: an entity equals its other side's copy in a single-player game (same id), and
     * each map is used on its own thread only.
     */
    private static final Map<Player, Flight> SERVER = new WeakHashMap<>();
    private static final Map<Player, Flight> CLIENT = new WeakHashMap<>();
    /** Set by the client: draws this tick's stretch of a flight. */
    public static TrailDrawer trail = (flight, from, to) -> {
    };

    @FunctionalInterface
    public interface TrailDrawer {
        void draw(Flight flight, Vec3 from, Vec3 to);
    }

    /** One flight. {@code left} counts ticks down; {@code struck} holds the ids of the creatures already hurt. */
    public static final class Flight {
        Vec3 dir;
        final int stage;
        final float power;
        final Set<Integer> struck = new HashSet<>();
        int left;
        int age;
        int stalled;
        Vec3 last;

        Flight(Vec3 dir, int stage, float power, int left, Vec3 last) {
            this.dir = dir;
            this.stage = stage;
            this.power = power;
            this.left = left;
            this.last = last;
        }

        public Vec3 dir() {
            return dir;
        }

        public int stage() {
            return stage;
        }

        public int age() {
            return age;
        }
    }

    private StrikeDash() {
    }

    private static Map<Player, Flight> flights(Level level) {
        return level.isClientSide() ? CLIENT : SERVER;
    }

    public static @Nullable Flight of(Player player) {
        return flights(player.level()).get(player);
    }

    public static boolean flying(Player player) {
        return of(player) != null;
    }

    private static Vec3 middle(Player player) {
        return player.getBoundingBox().getCenter();
    }

    private static Vec3 flat(Vec3 dir) {
        Vec3 level = new Vec3(dir.x, 0, dir.z);
        return level.lengthSqr() < 1.0E-4 ? dir : level.normalize();
    }

    /** Takes off along the look, on both sides: the client moves the wearer, the server only keeps count. */
    static void start(Player player, int stage) {
        Vec3 dir = player.getLookAngle();
        boolean grounded = player.onGround();
        if (grounded && dir.y < 0 && dir.y > -SKIM) {
            dir = flat(dir);
        }
        Level level = player.level();
        Flight flight = new Flight(dir, stage, StrikeGauntletItem.power(player),
                TICKS[stage - 1] + (level.isClientSide() ? 0 : LAG_TICKS), middle(player));
        flights(level).put(player, flight);
        player.stopFallFlying();
        player.setForcedPose(Pose.SPIN_ATTACK);
        cushion(player);
        if (com.pockyl.hextale.port.PortEntities.canSimulateMovement(player) && grounded && Math.abs(dir.y) < 0.35) {
            // Off the ground first, as a Riptide does, or the first bump would end the flight.
            player.move(MoverType.SELF, new Vec3(0, LIFT, 0));
        }
        if (level instanceof ServerLevel server) {
            Vec3 at = player.position().add(0, 0.9, 0);
            WeaponFxPayload.send(server, WeaponFxPayload.Kind.BURST, at, at, ModParticles.HEXTECH, 1 + 0.5F * stage);
            if (grounded) {
                WeaponRunes.quake(server, player.position(), 2, ModParticles.HEXTECH, stage);
            }
            server.playSound(null, player, ModSounds.STRIKE_GAUNTLET_DASH.get(), SoundSource.PLAYERS, 0.7F, 1.15F - 0.1F * stage);
            StrikeDashPayload.send(server, player, flight.dir, stage, true, false);
        }
    }

    /** Another player's flight seen on this client: the pose and the trail, no motion. */
    public static void shown(Player player, Vec3 dir, int stage) {
        flights(player.level()).put(player, new Flight(dir, stage, 1, TICKS[stage - 1] + LAG_TICKS + SHOWN_TICKS, middle(player)));
    }

    /** No fall during the flight, and the first landing after it is soft (see {@code GadgetEvents}). */
    private static void cushion(Player player) {
        player.resetFallDistance();
        if (!player.level().isClientSide()) {
            ImpulsePlateBlock.SOFT.put(player.getUUID(), player.level().getGameTime() + SOFT_TICKS);
        }
    }

    private static void close(Player player) {
        flights(player.level()).remove(player);
        if (player.getForcedPose() == Pose.SPIN_ATTACK) {
            player.setForcedPose(null);
        }
    }

    /** Ends a flight on this client, coasting out of it in the open or stopped by what it met. */
    public static void stop(Player player, boolean blocked) {
        Flight flight = of(player);
        if (flight == null) {
            return;
        }
        close(player);
        if (com.pockyl.hextale.port.PortEntities.canSimulateMovement(player)) {
            player.setDeltaMovement(blocked ? Vec3.ZERO : flight.dir.scale(SPEED * CARRY));
        }
    }

    /** The wall ahead, if any. Ground or a ceiling met at a flat angle only levels the flight out. */
    private static @Nullable BlockHitResult wall(Player player, Flight flight) {
        Vec3 from = middle(player);
        BlockHitResult hit = player.level().clip(new ClipContext(from, from.add(flight.dir.scale(WALL_REACH)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        if (hit.getDirection().getAxis() == Direction.Axis.Y && Math.abs(flight.dir.y) < SKIM) {
            flight.dir = flat(flight.dir);
            return null;
        }
        return hit;
    }

    /** Whether something other than the flight's end takes the wearer out of it. */
    private static boolean interrupted(Player player) {
        return player.isPassenger() || player.isSleeping() || !player.isAlive() || player.isSpectator();
    }

    /** Client side, leaving a world: the next one may give the same player the same id. */
    public static void forget() {
        CLIENT.clear();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SERVER.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void beforeTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        Flight flight = of(player);
        if (flight == null) {
            return;
        }
        if (com.pockyl.hextale.port.PortEntities.canSimulateMovement(player)) {
            wall(player, flight);
            player.setDeltaMovement(flight.dir.scale(SPEED));
            player.resetFallDistance();
        } else if (!player.level().isClientSide()) {
            player.setDeltaMovement(Vec3.ZERO);
        }
    }

    @SubscribeEvent
    public static void afterTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer server) {
            tick(server);
        } else if (player.level().isClientSide()) {
            clientTick(player);
        }
    }

    private static void clientTick(Player player) {
        Flight flight = of(player);
        if (flight == null) {
            return;
        }
        Vec3 here = middle(player);
        if (here.distanceTo(flight.last) < MAX_STEP) {
            trail.draw(flight, flight.last, here);
        }
        flight.last = here;
        flight.age++;
        boolean blocked = com.pockyl.hextale.port.PortEntities.canSimulateMovement(player) && (player.horizontalCollision && !player.minorHorizontalCollision
                || player.verticalCollision && Math.abs(flight.dir.y) >= SKIM);
        if (blocked || interrupted(player) || --flight.left <= 0) {
            stop(player, blocked);
        }
    }

    /** One server tick of a flight, after the wearer's client moved them. Public for the game tests. */
    public static void tick(ServerPlayer player) {
        Flight flight = SERVER.get(player);
        if (flight == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        cushion(player);
        if (player.connection != null) {
            player.connection.aboveGroundTickCount = 0;
        }
        Vec3 here = middle(player);
        double moved = here.distanceTo(flight.last);
        if (moved > MAX_STEP || interrupted(player)) {
            close(player);
            StrikeDashPayload.send(level, player, flight.dir, flight.stage, false, false);
            return;
        }
        strike(level, player, flight, flight.last, here);
        flight.last = here;
        flight.age++;
        flight.stalled = moved < SPEED * 0.2 ? flight.stalled + 1 : 0;
        BlockHitResult wall = wall(player, flight);
        if (wall != null || --flight.left <= 0 || flight.stalled >= STALL_TICKS) {
            finish(level, player, flight, wall);
        }
    }

    /** Every creature the wearer passed this tick, each once in a flight: hurt and thrown aside. */
    private static void strike(ServerLevel level, Player player, Flight flight, Vec3 from, Vec3 to) {
        float damage = PATH_DAMAGE[flight.stage - 1] * flight.power;
        List<LivingEntity> met = level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(PATH_WIDTH + 2), target -> {
            AABB box = target.getBoundingBox().inflate(PATH_WIDTH);
            return !flight.struck.contains(target.getId()) && ToolEffects.mayStrike(target, player)
                    && (box.contains(from) || box.contains(to) || box.clip(from, to).isPresent());
        });
        if (met.isEmpty()) {
            return;
        }
        ToolEffects.quietly(() -> {
            for (LivingEntity target : met) {
                flight.struck.add(target.getId());
                if (target.hurt(level.damageSources().playerAttack(player), damage)) {
                    Vec3 off = target.position().subtract(from);
                    Vec3 aside = off.subtract(flight.dir.scale(off.dot(flight.dir))).multiply(1, 0, 1);
                    aside = aside.lengthSqr() < 1.0E-4 ? Vec3.ZERO : aside.normalize();
                    target.push(aside.x * 0.8 + flight.dir.x * 0.5, 0.35, aside.z * 0.8 + flight.dir.z * 0.5);
                    target.hurtMarked = true;
                }
                Vec3 at = target.position().add(0, target.getBbHeight() / 2, 0);
                WeaponFxPayload.send(level, WeaponFxPayload.Kind.BURST, at, at, ModParticles.HEXTECH, 1.4F);
                level.playSound(null, at.x, at.y, at.z, ModSounds.STRIKE_GAUNTLET_FIST.get(), SoundSource.PLAYERS, 0.35F, 1.1F);
            }
        });
    }

    /** The end of the path: the old punch. Rock breaks where the flight met it; the creatures ahead are thrown back. */
    private static void finish(ServerLevel level, ServerPlayer player, Flight flight, @Nullable BlockHitResult wall) {
        close(player);
        cushion(player);
        Vec3 at = wall != null ? wall.getLocation() : middle(player);
        if (wall != null) {
            StrikeGauntletItem.smash(level, player, wall.getBlockPos(), wall.getDirection(), flight.stage);
        }
        StrikeGauntletItem.wave(level, player, at, flight.dir, flight.stage, flight.power, wall != null, flight.struck);
        WeaponFxPayload.send(level, WeaponFxPayload.Kind.BURST, at, at, ModParticles.HEXTECH, 2 + flight.stage);
        WeaponRunes.quake(level, at, 2 + flight.stage, ModParticles.HEXTECH, flight.stage);
        level.playSound(null, at.x, at.y, at.z, ModSounds.STRIKE_GAUNTLET_IMPACT.get(), SoundSource.PLAYERS, 0.5F + 0.15F * flight.stage, 1);
        StrikeDashPayload.send(level, player, flight.dir, flight.stage, false, wall != null);
    }
}
