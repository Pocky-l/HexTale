package com.pockyl.hextale.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.network.VoidCurrentsPayload;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.realm.VoidCurrents;
import com.pockyl.hextale.thevoid.realm.VoidPassage;

/**
 * The currents of the Void on the client: it carries its own player along the one it is in (sneaking lets go), draws
 * the streaks running along every current in sight (thinning before one breaks, none while it is broken) and plays the
 * rush of riding and the sinking whine of a current about to drop its rider.
 */
public final class VoidCurrentsClient {
    private static final int COLOUR = 0xC8BAD8;
    private static final double SIGHT = 72;
    private static List<VoidCurrents.Current> currents = List.of();
    private static VoidCurrents.Current ridden;
    private static long warned = -1;
    private static Rush rush;

    private VoidCurrentsClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> tick());
    }

    public static void accept(VoidCurrentsPayload payload) {
        currents = payload.currents();
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !minecraft.level.dimension().equals(VoidPassage.LEVEL)) {
            ridden = null;
            if (minecraft.level == null) {
                currents = List.of();
            }
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        long time = minecraft.level.getGameTime();
        ridden = null;
        for (VoidCurrents.Current current : currents) {
            int state = current.state(time);
            streaks(minecraft, player, current, state);
            if (state == 2 || player.isSpectator() || player.isShiftKeyDown() || player.getAbilities().flying) {
                continue;
            }
            double[] near = current.nearest(player.position().add(0, player.getBbHeight() * 0.5, 0), VoidCurrents.RADIUS);
            if (near[0] < 0) {
                continue;
            }
            ridden = current;
            carry(player, current, (int) near[0], near[1], state);
            if (state == 1 && warned != time / VoidCurrents.PERIOD) {
                warned = time / VoidCurrents.PERIOD;
                player.playSound(ModSounds.VOID_CURRENT_BREAK.get(), 0.8F, 1.0F);
            }
            break;
        }
        if (ridden != null && (rush == null || rush.isStopped())) {
            rush = new Rush();
            minecraft.getSoundManager().play(rush);
        }
    }

    /** Along the current, pulled gently back to its line; slower while it is about to break. */
    private static void carry(LocalPlayer player, VoidCurrents.Current current, int segment, double share, int state) {
        Vec3 along = current.tangent(segment);
        Vec3 line = current.at(segment, share);
        Vec3 centre = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Vec3 back = line.subtract(centre).scale(0.08);
        // Near the end it lets go gently, so the rider steps out onto the plate.
        double left = (current.points() - 1 - segment - share) / Math.max(1, current.points() - 1);
        double speed = VoidCurrents.SPEED * (state == 1 ? 0.6 : 1) * Math.min(1, left * 8 + 0.2);
        Vec3 want = along.scale(speed).add(back);
        player.setDeltaMovement(player.getDeltaMovement().scale(0.65).add(want.scale(0.35)));
        player.resetFallDistance();
    }

    private static void streaks(Minecraft minecraft, LocalPlayer player, VoidCurrents.Current current, int state) {
        if (state == 2) {
            return;
        }
        RandomSource random = player.getRandom();
        int count = Math.max(1, current.points() / 12) * (state == 1 ? 1 : 3);
        for (int k = 0; k < count; k++) {
            int i = random.nextInt(Math.max(1, current.points() - 1));
            Vec3 at = current.at(i, random.nextDouble());
            if (at.distanceToSqr(player.position()) > SIGHT * SIGHT) {
                continue;
            }
            Vec3 along = current.tangent(i).scale((state == 1 ? 0.25 : 0.55) + random.nextDouble() * 0.25);
            double r = VoidCurrents.RADIUS * 0.8;
            minecraft.level.addParticle(ModParticles.voidCurrent(COLOUR), at.x + (random.nextDouble() - 0.5) * r, at.y + (random.nextDouble() - 0.5) * r,
                    at.z + (random.nextDouble() - 0.5) * r, along.x, along.y, along.z);
        }
    }

    /** The rush of riding a current: follows the player, fades out once it no longer rides. */
    private static final class Rush extends AbstractTickableSoundInstance {
        private static final float FULL = 0.45F;

        Rush() {
            super(ModSounds.VOID_CURRENT.get(), SoundSource.AMBIENT, RandomSource.create());
            looping = true;
            delay = 0;
            volume = 0.01F;
            relative = true;
            x = 0;
            y = 0;
            z = 0;
        }

        @Override
        public void tick() {
            volume = ridden != null ? Math.min(FULL, volume + 0.06F) : volume - 0.03F;
            if (volume <= 0) {
                stop();
            }
        }
    }
}
