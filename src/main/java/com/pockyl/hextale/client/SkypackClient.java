package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.item.HextechSkypackItem;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.SkypackRune;

import java.util.HashMap;
import java.util.Map;

/**
 * The client side of the Hextech Skypack. The wearer's own client pushes them like a firework rocket does (player
 * movement is the client's), with a kick when the jets catch; every client draws the jets of the packs it sees - hextech
 * flame, glints and a trail of pale air from both nozzles - and plays their roar as a loop following the flyer.
 */
public final class SkypackClient {
    /** Blocks around the camera whose flyers get jets and sound. */
    private static final double SEEN = 64;
    private static boolean wasThrusting;
    private static final Map<Integer, JetSound> SOUNDS = new HashMap<>();

    private SkypackClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkypackClient::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        push(minecraft.player);
        for (Player player : minecraft.level.players()) {
            if (player.distanceToSqr(minecraft.player) > SEEN * SEEN) {
                continue;
            }
            ItemStack pack = HextechSkypackItem.worn(player);
            if (!pack.isEmpty() && HextechSkypackItem.jetsOn(player, pack)) {
                jets(minecraft.level, player, pack);
                JetSound sound = SOUNDS.get(player.getId());
                if (sound == null || sound.isStopped()) {
                    sound = new JetSound(player);
                    SOUNDS.put(player.getId(), sound);
                    minecraft.getSoundManager().play(sound);
                }
            }
        }
        SOUNDS.values().removeIf(AbstractTickableSoundInstance::isStopped);
    }

    /** The wearer's own thrust: a firework's push towards the look, with Motor's top speed; Bellows' kick at the start. */
    private static void push(LocalPlayer player) {
        ItemStack pack = HextechSkypackItem.worn(player);
        boolean thrusting = !pack.isEmpty() && HextechSkypackItem.jetsOn(player, pack);
        if (thrusting) {
            Vec3 look = player.getLookAngle();
            Vec3 motion = player.getDeltaMovement();
            if (!wasThrusting) {
                motion = motion.add(look.scale(SkypackRune.kick(HextechSkypackItem.strength(pack, SkypackRune.BELLOWS))));
            }
            double power = SkypackRune.thrust(HextechSkypackItem.strength(pack, SkypackRune.MOTOR));
            player.setDeltaMovement(motion.add(
                    look.x * 0.1 + (look.x * power - motion.x) * 0.5,
                    look.y * 0.1 + (look.y * power - motion.y) * 0.5,
                    look.z * 0.1 + (look.z * power - motion.z) * 0.5));
        }
        wasThrusting = thrusting;
    }

    /**
     * Where a flyer's nozzles are: the body lies along the look, the pack on its back, the nozzles at its lower end, one
     * on each side.
     */
    public static Vec3[] nozzles(Player player, float partialTick) {
        Vec3 look = player.getViewVector(partialTick);
        float yaw = player.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 back = right.cross(look);
        if (back.lengthSqr() < 1.0E-4) {
            back = new Vec3(0, 1, 0);
        }
        back = back.normalize();
        Vec3 centre = player.getPosition(partialTick).add(0, 0.3, 0).subtract(look.scale(0.25)).add(back.scale(0.32));
        return new Vec3[]{centre.add(right.scale(0.16)), centre.subtract(right.scale(0.16))};
    }

    /** One tick of both jets: flame and glints rushing back, a trail of pale air; a cough of smoke when the air runs low. */
    private static void jets(Level level, Player player, ItemStack pack) {
        RandomSource random = level.getRandom();
        Vec3 look = player.getLookAngle();
        Vec3 exhaust = look.scale(-0.22);
        boolean low = HextechSkypackItem.air(pack) < HextechSkypackItem.LOW;
        for (Vec3 nozzle : nozzles(player, 1)) {
            if (low && random.nextInt(4) == 0) {
                level.addParticle(ModParticles.vapour(ModParticles.SMOKE), nozzle.x, nozzle.y, nozzle.z, exhaust.x * 0.5, exhaust.y * 0.5, exhaust.z * 0.5);
                continue;
            }
            for (int i = 0; i < 2; i++) {
                double jitter = 0.04;
                level.addParticle(ModParticles.flame(i == 0 ? ModParticles.HEXTECH : ModParticles.HEX_LIGHT), nozzle.x, nozzle.y, nozzle.z,
                        exhaust.x + random.nextGaussian() * jitter, exhaust.y + random.nextGaussian() * jitter, exhaust.z + random.nextGaussian() * jitter);
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(ModParticles.glint(ModParticles.HEX_LIGHT), nozzle.x, nozzle.y, nozzle.z, exhaust.x * 0.4, exhaust.y * 0.4, exhaust.z * 0.4);
            }
            if (random.nextInt(2) == 0) {
                level.addParticle(ModParticles.wisp(Essence.AIR.color()), nozzle.x, nozzle.y, nozzle.z, exhaust.x * 0.15, exhaust.y * 0.15, exhaust.z * 0.15);
            }
            if (random.nextInt(10) == 0) {
                level.addParticle(ModParticles.spark(ModParticles.HEXTECH), nozzle.x, nozzle.y, nozzle.z, 0, 0, 0);
            }
        }
    }

    /** The jets' roar, following a flyer while they burn and fading out when they stop. */
    private static final class JetSound extends AbstractTickableSoundInstance {
        private final Player player;
        private float level;

        JetSound(Player player) {
            super(ModSounds.SKYPACK_THRUST.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
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
            if (player.isRemoved()) {
                stop();
                return;
            }
            ItemStack pack = HextechSkypackItem.worn(player);
            boolean on = !pack.isEmpty() && HextechSkypackItem.jetsOn(player, pack);
            level = Mth.clamp(level + (on ? 0.12F : -0.15F), 0, 1);
            if (level <= 0 && !on) {
                stop();
                return;
            }
            x = player.getX();
            y = player.getY();
            z = player.getZ();
            float speed = (float) player.getDeltaMovement().length();
            volume = 0.15F + level * 0.75F;
            pitch = 0.85F + Mth.clamp(speed / 2.5F, 0, 1) * 0.35F;
        }
    }
}
