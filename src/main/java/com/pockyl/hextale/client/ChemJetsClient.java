package com.pockyl.hextale.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.chem.ChemFuel;
import com.pockyl.hextale.chem.ChemGear;
import com.pockyl.hextale.chem.ChemJets;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.JetKey;
import com.pockyl.hextale.chem.PneumaticBoots;
import com.pockyl.hextale.chem.TankFuel;
import com.pockyl.hextale.network.JetFlamePayload;
import com.pockyl.hextale.network.JetModePayload;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.HashMap;
import java.util.Map;

/**
 * The client side of the Chem Jet Tank. For the player at the keys: the jump key after the boots, the hover key, and the
 * thrust (player movement is the client's). For every wearer near the camera: the heat the flames on {@code ChemTankLayer}
 * are drawn by, exhaust from both nozzles and the roar. Others' jets come from the server ({@link JetFlamePayload}); the
 * flames die by themselves when its reminders stop, so a wearer leaving view leaves no burning ghost.
 */
public final class ChemJetsClient {
    public static final KeyMapping HOVER = new KeyMapping("key.hextale.jet_hover", InputConstants.Type.KEYSYM, InputConstants.KEY_H,
            BreachCinematic.CATEGORY);
    /** Blocks around the camera whose jets are seen and heard. */
    private static final double SEEN = 64;
    /** Nozzle mouths from the feet (A8's numbers): height, behind the body's centre, to each side. */
    private static final double NOZZLE_Y = 0.8;
    private static final double NOZZLE_BACK = 0.38;
    private static final double NOZZLE_SIDE = 0.28;
    /** Ticks without a reminder after which another wearer's jets count as out. */
    private static final int SILENCE_TICKS = ChemJets.REMINDER_TICKS * 2 + 10;
    private static final Map<Integer, Long> HEARD = new HashMap<>();
    private static final Map<Integer, Float> HEAT = new HashMap<>();
    private static final Map<Integer, Roar> ROARS = new HashMap<>();
    private static final JetKey KEY = new JetKey();
    private static boolean hover;
    private static ChemJets.Mode sent = ChemJets.Mode.OFF;

    private ChemJetsClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(HOVER));
        NeoForge.EVENT_BUS.addListener(ChemJetsClient::afterTick);
    }

    /**
     * One tick of the player at the keys, before the player's own tick; {@code booted} = the boots took this press. A press
     * in the air the jets can answer is kept from the vanilla glider check: with a glider on, the tap that ends in time
     * opens it from here ({@link JetKey}), and a hold goes to the jets.
     */
    static void control(Minecraft minecraft, LocalPlayer player, boolean down, boolean pressed, boolean booted) {
        boolean ready = ChemJets.canFire(player);
        while (HOVER.consumeClick()) {
            if (ready) {
                hover = !hover;
                player.displayClientMessage(Component.translatable(hover ? "item.hextale.chem_gear.hover_on" : "item.hextale.chem_gear.hover_off"), true);
            }
        }
        hover &= ready;
        boolean hidden = ready && !player.getAbilities().mayfly;
        boolean tap = KEY.update(down, pressed, booted || PneumaticBoots.rests(player), hidden && !hover && ChemJets.glider(player));
        if (pressed && !booted && hidden) {
            ChemGearClient.hidePress(player);
        }
        if (tap && !player.onClimbable() && player.tryToStartFallFlying()) {
            player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            ready = false;
        }
        boolean up = ready && down && KEY.lit();
        ChemFuel fuel = ChemGear.power(player);
        if (up && !hover && ChemGearClient.airJumpLeft(player)) {
            // Jump held since the ground: the boots still go first.
            ChemGearClient.airJump(player, fuel);
            KEY.reset();
            up = false;
        }
        ChemJets.Mode mode = hover ? ChemJets.Mode.HOVER : up ? ChemJets.Mode.THRUST : ChemJets.Mode.OFF;
        if (mode != ChemJets.Mode.OFF) {
            player.setDeltaMovement(ChemJets.thrust(player.getDeltaMovement(), wish(player), fuel, hover, up, player.input.shiftKeyDown));
        }
        if (mode != sent) {
            sent = mode;
            PacketDistributor.sendToServer(new JetModePayload(mode));
        }
        ChemJets.show(player, mode != ChemJets.Mode.OFF);
    }

    /** The way the movement keys steer, flat, along the player's facing; zero with no key down. */
    private static Vec3 wish(LocalPlayer player) {
        Vec2 keys = player.input.getMoveVector();
        Vec3 forward = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 left = new Vec3(forward.z, 0, -forward.x);
        Vec3 way = forward.scale(keys.y).add(left.scale(keys.x));
        return way.lengthSqr() < 1.0E-4 ? Vec3.ZERO : way.normalize();
    }

    /** Heat of an entity's jets, 0 to 1, for the flames on its back. */
    public static float heat(LivingEntity entity) {
        return HEAT.getOrDefault(entity.getId(), 0F);
    }

    /** Runs on the client thread: a wearer's jets as the server tells them. */
    public static void receive(JetFlamePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || payload.entity() == minecraft.player.getId()) {
            return;
        }
        Entity entity = minecraft.level.getEntity(payload.entity());
        if (entity instanceof Player player) {
            ChemJets.show(player, payload.burning());
            HEARD.put(player.getId(), minecraft.level.getGameTime());
        }
    }

    private static void afterTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            HEARD.clear();
            HEAT.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        long now = minecraft.level.getGameTime();
        for (Player player : minecraft.level.players()) {
            Long heard = HEARD.get(player.getId());
            if (player != minecraft.player && heard != null && now - heard > SILENCE_TICKS) {
                ChemJets.show(player, false);
                HEARD.remove(player.getId());
            }
            boolean burning = ChemJets.burning(player);
            float heat = Mth.clamp(heat(player) + (burning ? 0.25F : -0.2F), 0, 1);
            if (heat > 0) {
                HEAT.put(player.getId(), heat);
            } else {
                HEAT.remove(player.getId());
            }
            if (burning && player.distanceToSqr(minecraft.player) <= SEEN * SEEN) {
                exhaust(minecraft, player);
                if (!ROARS.containsKey(player.getId())) {
                    Roar roar = new Roar(player);
                    ROARS.put(player.getId(), roar);
                    minecraft.getSoundManager().play(roar);
                }
            }
        }
        ROARS.values().removeIf(AbstractTickableSoundInstance::isStopped);
    }

    /** Exhaust in the fuel's colour from both mouths, thinning to smoke; the flames themselves are on the layer. */
    private static void exhaust(Minecraft minecraft, Player player) {
        TankFuel fuel = ChemTank.contents(ChemTank.worn(player));
        if (fuel.isEmpty()) {
            return;
        }
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 back = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw)).scale(NOZZLE_BACK);
        Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw)).scale(NOZZLE_SIDE);
        Vec3 motion = player.getDeltaMovement();
        for (int nozzle = -1; nozzle <= 1; nozzle += 2) {
            Vec3 at = player.position().add(0, NOZZLE_Y, 0).add(back).add(side.scale(nozzle));
            double spread = (player.getRandom().nextDouble() - 0.5) * 0.04;
            minecraft.level.addParticle(ModParticles.vapour(fuel.color()), at.x, at.y - 0.25, at.z, motion.x * 0.5 + spread, motion.y * 0.5 - 0.22,
                    motion.z * 0.5 - spread);
            if (player.getRandom().nextInt(3) == 0) {
                minecraft.level.addParticle(ModParticles.vapour(ModParticles.SMOKE), at.x, at.y - 0.6, at.z, spread, -0.12, -spread);
            }
        }
    }

    /** The jets' low roar, following a wearer while they burn and fading out when they stop. */
    private static final class Roar extends AbstractTickableSoundInstance {
        private final Player player;
        private float level;

        Roar(Player player) {
            super(ModSounds.CHEM_JETS_ROAR.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }

        @Override
        public void tick() {
            boolean on = ChemJets.burning(player);
            level = Mth.clamp(level + (on ? 0.15F : -0.2F), 0, 1);
            if (player.isRemoved() || level <= 0 && !on) {
                stop();
                return;
            }
            x = player.getX();
            y = player.getY();
            z = player.getZ();
            volume = 0.02F + level * 0.24F;
            pitch = 0.92F + Mth.clamp((float) player.getDeltaMovement().length(), 0, 1) * 0.16F;
        }
    }
}
