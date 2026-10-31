package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.realm.VoidPassage;
import com.pockyl.hextale.thevoid.realm.VoidZones;

/**
 * The sky of the Void (the dimension's {@code neoforge:custom_skybox} is {@code hextale:void}): a box round the camera
 * painted by {@code shaders/core/void_sky.fsh} - the turning vortex overhead, the haze and far silhouettes at the
 * horizon, the abyss glowing below. With a shader pack on, the pack draws the world with its own programs and would
 * give the Void an overworld sky; so the pack's sky is left to it and ours is drawn over the finished picture
 * afterwards ({@code OVER_PACK}), only where the world left the sky open.
 *
 * <p>1.21.1: the dimension's special effects {@code hextale:void} (its type's {@code effects}) draw the sky with a core
 * shader. They also stand in for what 26.x keeps in the dimension's and zones' environment attributes: no clouds,
 * nether-like shading, the violet floor of the lightmap and the fog distances of each zone. With a shader pack on, the
 * pack's sky stays (1.21.1 Iris cannot be handed our programs, so nothing is drawn over the pack). A 1.21.1 biome has
 * one ambient particle: the zones' second, rarer one (the amber motes) rises from here.
 */
public final class VoidSky extends DimensionSpecialEffects {
    /** The darkest the Void's light gets ({@code minecraft:visual/ambient_light_color} of 26.x). */
    private static final Vector3f AMBIENT = new Vector3f(0x3a / 255F, 0x2c / 255F, 0x46 / 255F);
    /**
     * Half the box's side: far enough that the view bobbing's small sideways shift of the camera does not slide the
     * painted sky against the world. The shaders pin the box onto the far plane, so it is never clipped there.
     */
    private static final float SIZE = 512;
    /** Set when drawing failed once: the plain sky is left for the rest of the session. */
    private static boolean broken;
    private static @Nullable ShaderInstance shader;
    /** The zones' second ambient particle (26.x: the second entry of their ambient particles), 0.12 as likely as the first. */
    private static final float AMBER_SHARE = 0.12F;
    private static final int AMBER = -997232;

    private VoidSky() {
        super(Float.NaN, false, SkyType.NONE, false, true);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterShadersEvent event) -> {
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(), Hextale.id("void_sky"), DefaultVertexFormat.POSITION),
                        loaded -> shader = loaded);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        modBus.addListener((RegisterDimensionSpecialEffectsEvent event) -> event.register(Hextale.id("void"), new VoidSky()));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, VoidSky::fog);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> amberMotes());
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker, float skyLight, int pixelX,
            int pixelY, Vector3f colors) {
        colors.max(AMBIENT);
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelView, Camera camera, Matrix4f projection,
            boolean isFoggy, Runnable setupFog) {
        ShaderInstance program = shader;
        if (broken || program == null || com.pockyl.hextale.client.IrisCompat.shadersOn()) {
            return false;
        }
        try {
            setupFog.run();
            draw(program, modelView);
            return true;
        } catch (RuntimeException e) {
            broken = true;
            Hextale.LOGGER.warn("Could not draw the Void's sky; the plain one stays from now on", e);
            return false;
        }
    }

    private static void draw(ShaderInstance program, Matrix4f modelView) {
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.mul(modelView);
        RenderSystem.applyModelViewMatrix();
        // Drawn first, on the far plane: nothing to test against, and it writes no depth.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> program);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        box(builder);
        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    /** The six inner faces of a box round the camera. */
    private static void box(BufferBuilder builder) {
        float s = SIZE;
        float[][] faces = {
                {-s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s},
                {-s, s, -s, -s, s, s, s, s, s, s, s, -s},
                {-s, -s, -s, -s, s, -s, s, s, -s, s, -s, -s},
                {-s, -s, s, s, -s, s, s, s, s, -s, s, s},
                {-s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s},
                {s, -s, -s, s, s, -s, s, s, s, s, -s, s}};
        for (float[] face : faces) {
            for (int i = 0; i < 12; i += 3) {
                builder.addVertex(face[i], face[i + 1], face[i + 2]);
            }
        }
    }

    /** Amber motes among each zone's own: as many tries as vanilla's ambient particles make, 0.12 as likely. */
    private static void amberMotes() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused() || !level.dimension().equals(VoidPassage.LEVEL)) {
            return;
        }
        net.minecraft.util.RandomSource random = level.random;
        net.minecraft.core.BlockPos centre = minecraft.player.blockPosition();
        net.minecraft.core.BlockPos.MutableBlockPos at = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int i = 0; i < 667 * 2; i++) {
            int range = i % 2 == 0 ? 16 : 32;
            at.set(centre.getX() + random.nextInt(range) - random.nextInt(range), centre.getY() + random.nextInt(range) - random.nextInt(range),
                    centre.getZ() + random.nextInt(range) - random.nextInt(range));
            if (random.nextFloat() >= AMBER_SHARE) {
                continue;
            }
            var settings = level.getBiome(at).value().getAmbientParticle();
            if (settings.isPresent() && settings.get().canSpawn(random) && !level.getBlockState(at).isCollisionShapeFullBlock(level, at)) {
                level.addParticle(net.minecraft.core.particles.ColorParticleOption.create(com.pockyl.hextale.registry.ModParticles.VOID_DRIFT.get(), AMBER),
                        at.getX() + random.nextDouble(), at.getY() + random.nextDouble(), at.getZ() + random.nextDouble(), 0, 0, 0);
            }
        }
    }

    /** The fog of the zone the camera is in (26.x: the zones' fog distance attributes); fog banks close in after it. */
    private static void fog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().equals(VoidPassage.LEVEL) || event.getType() != FogType.NONE) {
            return;
        }
        Vec3 at = event.getCamera().getPosition();
        float start, end;
        switch (VoidZones.zone(at.x, at.z)) {
            case THRESHOLD -> { start = 32; end = 190; }
            case REACH -> { start = 28; end = 170; }
            case SHOALS -> { start = 24; end = 160; }
            case DRIFT -> { start = 20; end = 140; }
            default -> { start = 22; end = 150; }
        }
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), end));
        event.setNearPlaneDistance(Math.min(start, event.getFarPlaneDistance() * 0.8F));
        event.setCanceled(true);
    }
}
