package com.pockyl.hextale.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import com.pockyl.hextale.port.client.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.Thinning;

/**
 * What a player feels where the edge of the world wears thin. Every effect is placed by the value at its own spot, so
 * it thins out towards the edge of the sick land instead of following the player.
 * <ul>
 * <li>Haze: a lilac veil low over the ground, dark motes sinking into the earth, the fog leaning to violet.</li>
 * <li>Miasma: the veil thick and knee-high, dark vapours seeping up out of the ground, magenta glints where the Void
 * looks through, the fog closing in, the edges of sight darkening, the land murmuring from somewhere around.</li>
 * <li>Blight and worse: all of it heavier, until the fog is a wall at thirty blocks.</li>
 * </ul>
 */
public final class ThinningAtmosphere {
    private static final int HAZE = 0x5E4C74;
    private static final int MOTE = 0x24162E;
    private static final int SEEP = 0x4A2560;
    private static final int VOID_GLINT = 0xD0469E;
    /** Blocks around the player where effects are placed. */
    private static final int REACH = 24;
    private static final ResourceLocation VIGNETTE = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");
    private static float local;
    private static float previous;
    private static int murmurIn = 200;

    private ThinningAtmosphere() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterGuiLayersEvent event) ->
                event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Hextale.id("thinning_vignette"), (portGraphics, portDelta) -> ThinningAtmosphere.vignette(com.pockyl.hextale.port.client.GuiGraphicsExtractor.of(portGraphics), portDelta)));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientThinning.clear());
        NeoForge.EVENT_BUS.addListener(ThinningAtmosphere::fogColour);
        NeoForge.EVENT_BUS.addListener(ThinningAtmosphere::fog);
    }

    /** 0 at the first haze, 1 at a breach. */
    private static float depth(float value) {
        return Mth.clamp((Thinning.felt(value) - Thinning.Stage.HAZE.felt * 0.5F) / (Thinning.Stage.BREACH.felt - Thinning.Stage.HAZE.felt * 0.5F), 0, 1);
    }

    private static void tick() {
        previous = local;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            // Not here: the values may already have come in while the world loads, so they are kept (cleared on logout).
            local = 0;
            return;
        }
        local += (Thinning.felt(ClientThinning.at(player.getX(), player.getZ())) - local) * 0.03F;
        if (minecraft.isPaused() || !ClientThinning.any()) {
            return;
        }
        RandomSource random = level.getRandom();
        int tries = 6 + Math.round(depth(local) * 18);
        for (int i = 0; i < tries; i++) {
            double x = player.getX() + (random.nextDouble() * 2 - 1) * REACH;
            double z = player.getZ() + (random.nextDouble() * 2 - 1) * REACH;
            float value = Thinning.felt(ClientThinning.at(x, z));
            if (value < Thinning.Stage.HAZE.felt * 0.4F) {
                continue;
            }
            float d = depth(value + Thinning.SAFE);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
            if (random.nextFloat() < 0.35F + d * 0.6F) {
                double y = ground + 0.1 + random.nextDouble() * (0.5 + d * 1.6);
                level.addParticle(ModParticles.haze(HAZE, d), x, y, z, (random.nextDouble() - 0.5) * 0.012, 0,
                        (random.nextDouble() - 0.5) * 0.012);
            }
            if (random.nextFloat() < 0.1F + d * 0.35F) {
                // A mote drawn into the earth: spawned where it falls in, its speed is the offset to its start.
                level.addParticle(ModParticles.voidMote(MOTE), x, ground + 0.05, z,
                        (random.nextDouble() - 0.5) * 2.5, 1.5 + random.nextDouble() * 2, (random.nextDouble() - 0.5) * 2.5);
            }
            if (value >= Thinning.Stage.MIASMA.felt * 0.8F) {
                if (random.nextFloat() < d * 0.3F) {
                    // Dark vapour seeping up out of the ground.
                    level.addParticle(ModParticles.wisp(SEEP), x, ground + 0.05, z, 0, 0.015 + random.nextDouble() * 0.02, 0);
                }
                if (random.nextFloat() < 0.03F + d * 0.08F) {
                    level.addParticle(ModParticles.glint(VOID_GLINT), x, ground + 0.2 + random.nextDouble() * 1.4, z, 0, 0.004, 0);
                }
            }
        }
        murmur(level, player, random);
    }

    /** Where the land has gone to miasma, it murmurs now and then from somewhere around. */
    private static void murmur(ClientLevel level, Player player, RandomSource random) {
        if (local < Thinning.Stage.MIASMA.felt * 0.8F) {
            murmurIn = Math.max(murmurIn, 120);
            return;
        }
        if (--murmurIn > 0) {
            return;
        }
        float k = Mth.clamp((local - Thinning.Stage.MIASMA.felt) / (Thinning.Stage.BREACH.felt - Thinning.Stage.MIASMA.felt), 0, 1);
        murmurIn = 300 + random.nextInt(400) - (int) (k * 200);
        double angle = random.nextDouble() * Math.PI * 2;
        double x = player.getX() + Math.cos(angle) * 10;
        double z = player.getZ() + Math.sin(angle) * 10;
        level.playLocalSound(x, player.getY() - 2, z, ModSounds.THINNING_MURMUR.get(), SoundSource.AMBIENT, 0.45F + 0.5F * k,
                0.85F + random.nextFloat() * 0.15F, false);
    }

    private static float strength(float partialTick) {
        return depth(Mth.lerp(partialTick, previous, local) + Thinning.SAFE);
    }

    private static void fogColour(ViewportEvent.ComputeFogColor event) {
        float s = strength((float) event.getPartialTick());
        if (s <= 0.001F) {
            return;
        }
        float k = (float) Math.sqrt(s) * 0.75F;
        event.setRed(Mth.lerp(k, event.getRed(), 0.26F));
        event.setGreen(Mth.lerp(k, event.getGreen(), 0.20F));
        event.setBlue(Mth.lerp(k, event.getBlue(), 0.33F));
    }

    private static void fog(ViewportEvent.RenderFog event) {
        float s = strength((float) event.getPartialTick());
        if (s <= 0.001F) {
            return;
        }
        float k = (float) Math.sqrt(s) * 0.9F;
        float far = Mth.lerp(s, 96, 28);
        event.setFarPlaneDistance(Mth.lerp(k, event.getFarPlaneDistance(), Math.min(event.getFarPlaneDistance(), far)));
        event.setNearPlaneDistance(Mth.lerp(k, event.getNearPlaneDistance(), Math.min(event.getNearPlaneDistance(), far * 0.1F)));
    }

    /** From the miasma on, the edges of sight darken towards violet. */
    private static void vignette(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        float value = Mth.lerp(delta.getGameTimeDeltaPartialTick(false), previous, local);
        float s = Mth.clamp((value - Thinning.Stage.MIASMA.felt * 0.8F) / (Thinning.Stage.BREACH.felt - Thinning.Stage.MIASMA.felt * 0.8F), 0, 1);
        if (s <= 0.003F) {
            return;
        }
        float k = s * 0.55F;
        // The vignette pipeline darkens by its colour: green most, blue least, so the dark edges lean to violet.
        int color = FastColor.ARGB32.colorFromFloat(1, k * 0.75F, k, k * 0.55F);
        graphics.blit(RenderPipelines.VIGNETTE, VIGNETTE, 0, 0, 0, 0, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(),
                graphics.guiHeight(), color);
    }
}
