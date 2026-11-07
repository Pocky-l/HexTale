package com.pockyl.hextale.chem;

import com.pockyl.hextale.entity.GrappleHook;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.StrikeDash;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The pneumatics built into the chem-steel boots, running on the worn tank: one more jump in the air for one action of
 * fuel, as high as the fuel allows, and falls that hurt less (the action is the one every hit on chemtech armour
 * spends, see {@link ChemGear}).
 *
 * <p>Movement is the client's: {@code ChemGearClient} reads the jump key and lifts its own player, then asks the server
 * ({@code PneumaticJumpPayload}), which checks everything again, spends the fuel and shows the puff to everyone.
 *
 * <p>Order of the jump key: the boots' jump comes first, so a press in the air never opens a glider while the jump is
 * left. A glider opens on the next press, and the jets (held jump) take over after the boots.
 */
public final class PneumaticBoots {
    private static final int RING = 10;
    /** Wearers who spent their jump since they last stood on something (server thread only). */
    private static final Set<Player> SPENT = Collections.newSetFromMap(new WeakHashMap<>());

    private PneumaticBoots() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PneumaticBoots::onPlayerTick);
    }

    /** Blocks the jump in the air lifts its wearer with this fuel. */
    public static double jumpHeight(ChemFuel fuel) {
        return switch (fuel) {
            case EMBER_OIL -> 1.0;
            case RAW_SHIMMER -> 1.25;
            case REFINED_SHIMMER -> 1.5;
        };
    }

    /** Upward speed that reaches {@link #jumpHeight} under player gravity and air drag. */
    public static double jumpSpeed(ChemFuel fuel) {
        return switch (fuel) {
            case EMBER_OIL -> 0.3681;
            case RAW_SHIMMER -> 0.4196;
            case REFINED_SHIMMER -> 0.4634;
        };
    }

    /** Share of a fall's damage the boots take away with this fuel. */
    public static float cushion(ChemFuel fuel) {
        return switch (fuel) {
            case EMBER_OIL -> 0.5F;
            case RAW_SHIMMER -> 0.65F;
            case REFINED_SHIMMER -> 0.8F;
        };
    }

    public static boolean worn(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.CHEM_STEEL_BOOTS.get());
    }

    /** Whether the player stands on or in something that gives the jump back. */
    public static boolean rests(Player player) {
        return player.onGround() || player.isInLiquid() || player.onClimbable();
    }

    /**
     * Whether the boots may fire now, spent jump aside. Both sides ask. Players who may fly are left out: their double
     * tap belongs to the flight. A rope in use takes the jump key too.
     */
    public static boolean canJump(Player player) {
        return worn(player) && ChemGear.power(player) != null && !rests(player) && !player.getAbilities().mayfly && !player.isPassenger()
                && !player.isFallFlying() && !StrikeDash.flying(player) && GrappleHook.of(player) == null;
    }

    /** Whether the boots still have their jump in this flight. Server only: the client keeps its own count. */
    public static boolean jumpLeft(Player player) {
        return !SPENT.contains(player) && canJump(player);
    }

    /** The server's half of a jump: checks it, spends the fuel, forgets the fall so far, shows the puff. */
    public static boolean jump(Player player) {
        ChemFuel fuel = ChemGear.power(player);
        if (!(player.level() instanceof ServerLevel level) || fuel == null || SPENT.contains(player) || !canJump(player)) {
            return false;
        }
        SPENT.add(player);
        ChemGear.spend(player, 1);
        player.resetFallDistance();
        heelJets(level, player, fuel);
        ring(level, player.position().add(0, 0.05, 0), 0.09);
        sound(level, player, ModSounds.CHEM_BOOTS_PUFF.get());
        return true;
    }

    /** Gives the jump back when the player {@link #rests}. */
    public static void refresh(Player player) {
        if (rests(player)) {
            SPENT.remove(player);
        }
    }

    /** Takes its share off a fall; the caller spends the action of the hit. */
    static void soften(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        ChemFuel fuel = ChemGear.power(entity);
        if (fuel == null || !worn(entity) || !event.getSource().is(DamageTypeTags.IS_FALL) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        event.setAmount(event.getAmount() * (1 - cushion(fuel)));
        ring(level, entity.position().add(0, 0.1, 0), 0.07);
        sound(level, entity, ModSounds.CHEM_BOOTS_HISS.get());
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) {
            refresh(event.getEntity());
        }
    }

    /** Two short jets down from the heels: steam with a trace of the fuel burnt. */
    private static void heelJets(ServerLevel level, Player player, ChemFuel fuel) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw)).scale(0.14);
        for (int heel = -1; heel <= 1; heel += 2) {
            Vec3 at = player.position().add(side.scale(heel));
            for (int i = 0; i < 4; i++) {
                int colour = i == 0 ? fuel.brew().color() : ModParticles.STEAM;
                level.sendParticles(ModParticles.vapour(colour), at.x, at.y + 0.05, at.z, 0,
                        level.getRandom().nextGaussian() * 0.02, -0.1 - i * 0.03, level.getRandom().nextGaussian() * 0.02, 1);
            }
        }
    }

    /** A flat ring of steam running outwards. */
    private static void ring(ServerLevel level, Vec3 at, double speed) {
        for (int i = 0; i < RING; i++) {
            float angle = Mth.TWO_PI * i / RING;
            level.sendParticles(ModParticles.vapour(ModParticles.STEAM), at.x, at.y, at.z, 0, Mth.cos(angle) * speed, 0, Mth.sin(angle) * speed, 1);
        }
    }

    private static void sound(ServerLevel level, LivingEntity entity, SoundEvent sound) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, 1, 0.9F + level.getRandom().nextFloat() * 0.2F);
    }
}
