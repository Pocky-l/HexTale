package com.pockyl.hextale.runework;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.network.WeaponFxPayload;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The boots' Wavewalk: the wearer walks on water as on ground - quicker there by the rune's bonus - unless they sneak,
 * then they sink and swim as usual (the rune also makes the water hold them less, see {@link ClothesEffects}). Caught
 * just under the surface, the water lifts them back onto it. Every stride spreads a ripple of light with a few droplets;
 * now and then the water laps softly under the feet.
 *
 * <p>Movement is simulated where the player is authoritative (their own client, a server-run player), so the carrying
 * runs there; the server sees where the wearer stands and draws the ripples. There is no hook to make water solid for a
 * player, so the feet are held on the surface tick by tick: set onto it, falling stopped, standing on ground.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class Wavewalk {
    private static final Identifier SPEED = Hextale.id("clothes/boots_wavewalk_speed");
    /** How far under the surface the feet may slip and still be set back onto it. */
    private static final double CATCH = 0.6;
    /** Down to this depth the water pushes the wearer up to its surface (deeper is swimming on purpose). */
    private static final double LIFT = 1.8;
    /** Blocks walked on water between two ripples. */
    private static final double STRIDE = 1.3;
    /** Ticks between the slow ripples under someone standing still on the water. */
    private static final int STILL_RIPPLE = 30;
    /** Every this many ripples the water laps under the feet. */
    private static final int LAP_EVERY = 3;
    /** Server: each wearer's last ripple. */
    private static final Map<Player, Stride> STRIDES = new WeakHashMap<>();

    private static final class Stride {
        Vec3 last = Vec3.ZERO;
        long at;
        int count;
        boolean started;
    }

    private Wavewalk() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        int stars = ClothesEffects.strength(player, ClothesRune.BOOTS_WAVEWALK);
        boolean walking = stars > 0 && able(player) && carry(player);
        speed(player, walking ? stars : 0);
        if (player.level() instanceof ServerLevel level) {
            if (walking) {
                ripples(level, player, stars);
            } else {
                STRIDES.remove(player);
            }
        }
    }

    /** Whether the wearer may be carried now: on foot, not sneaking, not flying, gliding or riding. */
    private static boolean able(Player player) {
        return !player.isSpectator() && !player.isShiftKeyDown() && !player.getAbilities().flying && !player.isFallFlying()
                && !player.isPassenger() && !player.isSwimming() && !player.onClimbable();
    }

    /**
     * Whether the wearer stands on water this tick. Where their movement is simulated, the feet are set onto the surface
     * (falling stopped, on ground) when they are at it, slipped a little under it or about to fall through it; a little
     * deeper, the water lifts them up.
     */
    static boolean carry(Player player) {
        Vec3 motion = player.getDeltaMovement();
        double surface = surface(player.level(), player.position(), Math.min(0, motion.y));
        if (Double.isNaN(surface)) {
            return false;
        }
        double above = player.getY() - surface;
        boolean simulated = player.canSimulateMovement();
        if (above >= -CATCH && above <= Math.max(0.12, 0.01 - motion.y) && (motion.y <= 0.05 || above < 0)) {
            if (simulated) {
                player.setPos(player.getX(), surface, player.getZ());
                player.setDeltaMovement(motion.x, 0, motion.z);
                player.setOnGround(true);
            }
            // On both sides: the server never sees this player land (the water is no ground to it), so a walk of jumps
            // over the water would add up to a deadly fall at the shore.
            player.resetFallDistance();
            return true;
        }
        if (simulated && above < -CATCH && above > -LIFT) {
            player.setDeltaMovement(motion.x, Math.max(motion.y, 0.2), motion.z);
        }
        return false;
    }

    /**
     * The height of the open water surface under (or just over) the feet, NaN when there is none: the top of the highest
     * water within {@link #LIFT} over the feet and a little under them, with nothing solid in the water block itself.
     */
    static double surface(Level level, Vec3 feet, double falling) {
        int top = Mth.floor(feet.y + LIFT);
        int bottom = Mth.floor(feet.y + falling - 0.5);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y >= bottom; y--) {
            pos.set(Mth.floor(feet.x), y, Mth.floor(feet.z));
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.is(FluidTags.WATER)) {
                continue;
            }
            if (y == top && level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                return Double.NaN;
            }
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return Double.NaN;
            }
            return y + fluid.getHeight(level, pos);
        }
        return Double.NaN;
    }

    /** The bonus to speed while on the water, added or taken away (only when it changes, so nothing is resent). */
    private static void speed(Player player, int stars) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (stars <= 0) {
            if (speed.hasModifier(SPEED)) {
                speed.removeModifier(SPEED);
            }
            return;
        }
        double bonus = HolderRune.at(ClothesRune.WAVE_SPEED, stars);
        AttributeModifier now = speed.getModifier(SPEED);
        if (now == null || now.amount() != bonus) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** A ripple at every stride, and a slow one now and then under someone standing still. */
    private static void ripples(ServerLevel level, Player player, int stars) {
        Stride stride = STRIDES.computeIfAbsent(player, p -> new Stride());
        Vec3 at = player.position();
        long now = level.getGameTime();
        double dx = at.x - stride.last.x;
        double dz = at.z - stride.last.z;
        if (!stride.started || dx * dx + dz * dz >= STRIDE * STRIDE) {
            stride.started = true;
            stride.last = at;
            stride.at = now;
            stride.count++;
            ripple(level, at, stars, 1, stride.count % LAP_EVERY == 1);
        } else if (now - stride.at >= STILL_RIPPLE) {
            stride.at = now;
            ripple(level, at, stars, 0.6F, false);
        }
    }

    private static void ripple(ServerLevel level, Vec3 at, int stars, float size, boolean lap) {
        WeaponFxPayload.send(level, WeaponFxPayload.Kind.RIPPLE, at, at, ClothesRune.BOOTS_WAVEWALK.rune().color(), size * (1.1F + 0.15F * stars));
        if (lap) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 0.15F,
                    1.3F + level.getRandom().nextFloat() * 0.3F);
        }
    }

    /** Whether the player stands on the water by the rune now (tests). */
    public static boolean onWater(Player player) {
        return ClothesEffects.strength(player, ClothesRune.BOOTS_WAVEWALK) > 0 && able(player)
                && !Double.isNaN(surface(player.level(), player.position(), 0))
                && Math.abs(player.getY() - surface(player.level(), player.position(), 0)) < 0.13;
    }
}
