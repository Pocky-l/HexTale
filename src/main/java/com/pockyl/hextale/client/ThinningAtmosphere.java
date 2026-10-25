package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.Thinning;

/**
 * What a player feels where the edge of the world wears thin. Haze: a faint lilac veil low over the ground and now and
 * then a dark mote sinking into the earth. Miasma: the veil thickens, the fog closes in and turns ashen violet, rare
 * magenta glints show where the Void looks through, and the land murmurs far off. Every puff is placed by the value at
 * its own spot, so the haze thins out towards the edge of the sick land instead of following the player.
 */
public final class ThinningAtmosphere {
    /** The haze over the ground and the motes the earth swallows. */
    private static final int HAZE = 0x6A5A7E;
    private static final int MOTE = 0x2A1C34;
    private static final int VOID_GLINT = 0xD0469E;
    /** Blocks around the player where haze is placed. */
    private static final int REACH = 22;
    private static float local;
    private static float previous;
    private static int murmurIn = 200;

    private ThinningAtmosphere() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(ThinningAtmosphere::fogColour);
        NeoForge.EVENT_BUS.addListener(ThinningAtmosphere::fog);
    }

    private static void tick() {
        previous = local;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            local = 0;
            ClientThinning.clear();
            return;
        }
        local += (ClientThinning.at(player.getX(), player.getZ()) - local) * 0.03F;
        if (minecraft.isPaused() || !ClientThinning.any()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < 6; i++) {
            double x = player.getX() + (random.nextDouble() * 2 - 1) * REACH;
            double z = player.getZ() + (random.nextDouble() * 2 - 1) * REACH;
            float value = ClientThinning.at(x, z);
            float haze = Mth.clamp((value - 40) / (Thinning.Stage.MIASMA.from + 100), 0, 1);
            if (haze <= 0 || random.nextFloat() > haze * 0.55F) {
                continue;
            }
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
            double y = ground + 0.15 + random.nextDouble() * (0.6 + haze);
            level.addParticle(ModParticles.haze(HAZE), x, y, z, (random.nextDouble() - 0.5) * 0.01, 0, (random.nextDouble() - 0.5) * 0.01);
            float sink = Mth.clamp((value - Thinning.Stage.HAZE.from) / (Thinning.Stage.BLIGHT.from), 0, 1);
            if (random.nextFloat() < 0.08F + sink * 0.3F) {
                // A mote drawn into the earth: spawned where it falls in, its speed is the offset to its start.
                level.addParticle(ModParticles.voidMote(MOTE), x, ground + 0.05, z,
                        (random.nextDouble() - 0.5) * 2, 1.2 + random.nextDouble() * 1.5, (random.nextDouble() - 0.5) * 2);
            }
            if (value >= Thinning.Stage.MIASMA.from && random.nextFloat() < 0.02F + sink * 0.04F) {
                level.addParticle(ModParticles.glint(VOID_GLINT), x, ground + 0.2 + random.nextDouble() * 0.8, z, 0, 0.003, 0);
            }
        }
        murmur(level, player, random);
    }

    /** Where the land has gone to miasma, it murmurs now and then from somewhere around. */
    private static void murmur(ClientLevel level, Player player, RandomSource random) {
        if (local < Thinning.Stage.MIASMA.from * 0.8F) {
            murmurIn = Math.max(murmurIn, 120);
            return;
        }
        if (--murmurIn > 0) {
            return;
        }
        float k = Mth.clamp((local - Thinning.Stage.MIASMA.from) / (Thinning.Stage.BREACH.from - Thinning.Stage.MIASMA.from), 0, 1);
        murmurIn = 360 + random.nextInt(480) - (int) (k * 240);
        double angle = random.nextDouble() * Math.PI * 2;
        double x = player.getX() + Math.cos(angle) * 10;
        double z = player.getZ() + Math.sin(angle) * 10;
        level.playLocalSound(x, player.getY() - 2, z, ModSounds.THINNING_MURMUR.get(), SoundSource.AMBIENT, 0.35F + 0.45F * k,
                0.85F + random.nextFloat() * 0.15F, false);
    }

    private static float strength(float partialTick) {
        return Mth.lerp(partialTick, previous, local);
    }

    private static void fogColour(ViewportEvent.ComputeFogColor event) {
        float value = strength((float) event.getPartialTick());
        float s = Mth.clamp((value - Thinning.Stage.HAZE.from) / (Thinning.Stage.BREACH.from - Thinning.Stage.HAZE.from), 0, 1) * 0.5F;
        if (s <= 0.001F) {
            return;
        }
        event.setRed(Mth.lerp(s, event.getRed(), 0.30F));
        event.setGreen(Mth.lerp(s, event.getGreen(), 0.25F));
        event.setBlue(Mth.lerp(s, event.getBlue(), 0.36F));
    }

    private static void fog(ViewportEvent.RenderFog event) {
        float value = strength((float) event.getPartialTick());
        float s = Mth.clamp((value - Thinning.Stage.MIASMA.from) / (Thinning.Stage.BREACH.from - Thinning.Stage.MIASMA.from), 0, 1) * 0.7F;
        if (s <= 0.001F) {
            return;
        }
        event.setFarPlaneDistance(Mth.lerp(s, event.getFarPlaneDistance(), Math.min(event.getFarPlaneDistance(), 40)));
        event.setNearPlaneDistance(Mth.lerp(s, event.getNearPlaneDistance(), Math.min(event.getNearPlaneDistance(), 4)));
    }
}
