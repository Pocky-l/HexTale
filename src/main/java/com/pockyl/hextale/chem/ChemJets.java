package com.pockyl.hextale.chem;

import com.pockyl.hextale.entity.GrappleHook;
import com.pockyl.hextale.network.JetFlamePayload;
import com.pockyl.hextale.runework.StrikeDash;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The Chem Jet Tank's flight, on the worn tank's fuel: jump held in the air thrusts, the hover key holds height. One
 * action per {@link #TICKS_PER_ACTION} ticks of burn, never any Thinning.
 *
 * <p>Movement is the client's, like every piece of gear: {@code ChemJetsClient} reads the keys, pushes its own player
 * with {@link #thrust} and tells the server what it wants ({@code JetModePayload}). The client only asks. The server
 * checks the rules again every tick, keeps the count of fuel itself, and waives the floating kick only for ticks it was
 * paid for. A client that says nothing and flies anyway gets nothing here.
 *
 * <p>Order of the jump key: the boots' jump first ({@link PneumaticBoots#jumpLeft}), then the jets and a glider share it: a
 * tap opens the glider, a hold fires the jets ({@link JetKey}). The client keeps the press from the vanilla glider check
 * until it knows which, and {@code SkypackFlight} holds a launch while the jets burn.
 */
public final class ChemJets {
    /** Ticks of burn one action of fuel pays for. */
    public static final int TICKS_PER_ACTION = 4;
    /** How fast a hovering wearer sinks while sneaking (blocks a tick). */
    public static final double SINK = 0.15;
    /** Ticks between reminders to the clients that a burn goes on; a client that hears none lets the flames die. */
    public static final int REMINDER_TICKS = 20;
    /** Upward speed gained in a tick of thrust, the tick's gravity included. */
    private static final double LIFT = 0.18;
    /** Vertical speed a hover takes away in a tick when it has to come down to its target. */
    private static final double BRAKE = 0.12;
    /** Sideways speed gained in a tick. */
    private static final double PUSH = 0.06;
    /** A burn forgets the fall only at this speed down or slower (blocks a tick): a last-moment tap saves nobody. */
    private static final double SAFE_SPEED = 0.5;
    /** Integrated server and client tick in different threads; their entities are different objects. */
    private static final Map<LivingEntity, Burn> BURNS = Collections.synchronizedMap(new WeakHashMap<>());

    /** What the wearer's keys ask of the jets. */
    public enum Mode {
        OFF, THRUST, HOVER
    }

    private static final class Burn {
        Mode mode = Mode.OFF;
        boolean burning;
        int ticks;
        double lastY = Double.NaN;
    }

    private ChemJets() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ChemJets::beforeTick);
        NeoForge.EVENT_BUS.addListener(ChemJets::afterTick);
    }

    /** Top climbing speed with this fuel (blocks a tick). */
    public static double climbSpeed(ChemFuel fuel) {
        return switch (fuel) {
            case EMBER_OIL -> 0.3;
            case RAW_SHIMMER -> 0.45;
            case REFINED_SHIMMER -> 0.6;
        };
    }

    /** Top speed along the ground the jets push to with this fuel (blocks a tick). */
    public static double cruiseSpeed(ChemFuel fuel) {
        return switch (fuel) {
            case EMBER_OIL -> 0.35;
            case RAW_SHIMMER -> 0.45;
            case REFINED_SHIMMER -> 0.55;
        };
    }

    /** Whether the wearer has jets with fuel and is somewhere they can fire. Both sides ask. */
    public static boolean canFire(Player player) {
        return ChemTank.hasJets(ChemTank.worn(player)) && ChemGear.power(player) != null && !PneumaticBoots.rests(player)
                && !player.getAbilities().flying && !player.isPassenger() && !player.isFallFlying() && !player.isDeadOrDying()
                && !StrikeDash.flying(player) && GrappleHook.of(player) == null;
    }

    /** Whether a glider on the wearer could open now, Skypack with air or elytra alike: a tap of jump then belongs to it. */
    public static boolean glider(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (com.pockyl.hextale.port.PortEntities.canGlideUsing(player.getItemBySlot(slot), slot)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the entity's jets burn now, as far as this side knows. */
    public static boolean burning(LivingEntity entity) {
        Burn burn = BURNS.get(entity);
        return burn != null && burn.burning;
    }

    /** Sets what this client shows of a wearer's jets. */
    public static void show(Player player, boolean burning) {
        if (burning) {
            BURNS.computeIfAbsent(player, entity -> new Burn()).burning = true;
        } else {
            BURNS.remove(player);
        }
    }

    /** The server takes note of what the wearer's keys ask; {@link #tick} decides. */
    public static void ask(Player player, Mode mode) {
        BURNS.computeIfAbsent(player, entity -> new Burn()).mode = mode;
    }

    /**
     * The motion after a tick of the jets. {@code wish} is the way the keys steer, flat and of length 1, or zero. Thrust
     * climbs to the fuel's top speed and leaves a faster rise alone; a hover aims at standing still, at the top speed
     * with {@code up}, at {@link #SINK} down with {@code sink}.
     */
    public static Vec3 thrust(Vec3 motion, Vec3 wish, ChemFuel fuel, boolean hover, boolean up, boolean sink) {
        double climb = climbSpeed(fuel);
        double y;
        if (hover) {
            double target = up ? climb : sink ? -SINK : 0;
            y = motion.y + Math.clamp(target - motion.y, -BRAKE, LIFT);
        } else {
            y = Math.max(motion.y, Math.min(climb, motion.y + LIFT));
        }
        double missing = cruiseSpeed(fuel) - (motion.x * wish.x + motion.z * wish.z);
        double push = Math.clamp(missing, 0, PUSH);
        return new Vec3(motion.x + wish.x * push, y, motion.z + wish.z * push);
    }

    /** One server tick of a wearer: decides whether the jets burn, pays for it, tells the clients. */
    public static void tick(ServerPlayer player) {
        Burn burn = BURNS.get(player);
        if (burn == null) {
            return;
        }
        boolean burning = allowed(player, burn.mode);
        if (burning) {
            pay(player, burn);
            if (player.connection != null) {
                player.connection.aboveGroundTickCount = 0;
            }
            if (!(burn.lastY - player.getY() > SAFE_SPEED)) {
                player.resetFallDistance();
            }
        }
        burn.lastY = player.getY();
        if (burning != burn.burning || burning && player.tickCount % REMINDER_TICKS == 0) {
            tell(player, burning);
        }
        burn.burning = burning;
        if (!burning && burn.mode == Mode.OFF && burn.ticks == 0) {
            BURNS.remove(player);
        }
    }

    /** The others around the wearer; the wearer's own client knows. Game-test players have no channel. */
    private static void tell(ServerPlayer player, boolean burning) {
        JetFlamePayload payload = new JetFlamePayload(player.getId(), burning);
        if (player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayersTrackingEntity(player, payload);
        }
    }

    /** Thrust belongs to the jump key, so it waits for the boots' jump; a hover has a key of its own. */
    private static boolean allowed(Player player, Mode mode) {
        return mode != Mode.OFF && canFire(player) && (mode == Mode.HOVER || !PneumaticBoots.jumpLeft(player));
    }

    /** The count runs on over short burns, so tapping is not free. */
    private static void pay(Player player, Burn burn) {
        if (++burn.ticks >= TICKS_PER_ACTION) {
            burn.ticks = 0;
            ChemGear.spend(player, 1);
        }
    }

    /** Holds the server's copy of a burning wearer still, so that only the client's positions move it (see StrikeDash). */
    private static void beforeTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && burning(player)) {
            player.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void afterTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }
}
