package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.item.HextechSkypackItem;
import com.pockyl.hextale.item.SkypackFlight;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.SkypackRune;

import java.util.HashMap;
import java.util.Map;

/**
 * The client side of the Hextech Skypack. Every client follows each wearer's flight ({@link SkypackFlight}); during the
 * boost of a launch the wearer's own client pushes them like a firework rocket does (player movement is the client's),
 * with a kick when the jets catch, and every client plays the jets' roar following the flyer (the jets themselves are
 * drawn on the wearer by {@code SkypackLayer}). Then only gliding.
 */
public final class SkypackClient {
    /** Blocks around the camera whose flyers get jets and sound. */
    private static final double SEEN = 64;
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
        long now = minecraft.level.getGameTime();
        for (Player player : minecraft.level.players()) {
            ItemStack pack = HextechSkypackItem.worn(player);
            if (pack.isEmpty()) {
                SkypackFlight.forget(player);
                continue;
            }
            boolean launch = SkypackFlight.tick(player, pack, now);
            if (player == minecraft.player) {
                push(minecraft.player, pack, launch, now);
            }
            int left = SkypackFlight.boostLeft(player, now);
            if (player.distanceToSqr(minecraft.player) > SEEN * SEEN) {
                continue;
            }
            if (left > 0) {
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

    /**
     * The wearer's own thrust while the boost of a launch lasts: a firework's push towards the look with Motor's top
     * speed, and Bellows' kick on the launch itself. After the boost the pack only glides.
     */
    private static void push(LocalPlayer player, ItemStack pack, boolean launch, long now) {
        if (!SkypackFlight.boosting(player, now)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement();
        if (launch) {
            motion = motion.add(look.scale(SkypackRune.kick(HextechSkypackItem.strength(pack, SkypackRune.BELLOWS))));
        }
        double power = SkypackRune.thrust(HextechSkypackItem.strength(pack, SkypackRune.MOTOR));
        player.setDeltaMovement(motion.add(
                look.x * 0.1 + (look.x * power - motion.x) * 0.5,
                look.y * 0.1 + (look.y * power - motion.y) * 0.5,
                look.z * 0.1 + (look.z * power - motion.z) * 0.5));
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
            boolean on = SkypackFlight.boostLeft(player, player.level().getGameTime()) > 0;
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
