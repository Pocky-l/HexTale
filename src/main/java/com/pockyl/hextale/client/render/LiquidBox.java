package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;
import com.pockyl.hextale.Hextale;

import java.util.ArrayList;
import java.util.List;

/**
 * Glowing liquid essence filling a box inside a glass vessel. The animated {@code block/essence_liquid} sprite (light
 * greys) is tinted with the liquid's colour: the surface is lighter, the sides darken towards the bottom like a deep
 * liquid, and optional bubbles rise through it. Coordinates are in block units. Every face is drawn with both windings,
 * so the liquid shows through the glass from any side.
 */
public final class LiquidBox {
    private static final Identifier LIQUID = Hextale.id("block/essence_liquid");
    /** Vanilla's white bubble (a ring with a glint), tinted by the liquid. */
    public static final Identifier BUBBLE = Identifier.withDefaultNamespace("textures/particle/bubble_white.png");
    /** Vanilla's bubble bursting, frame by frame. */
    private static final Identifier[] BUBBLE_POP = new Identifier[5];
    /** How much lighter than the liquid a bubble's ring is. */
    private static final float BUBBLE_LIGHT = 0.55F;

    static {
        for (int i = 0; i < BUBBLE_POP.length; i++) {
            BUBBLE_POP[i] = Identifier.withDefaultNamespace("textures/particle/bubble_pop_" + i + ".png");
        }
    }
    /** The surface catches light, the depths darken. */
    private static final float SURFACE_LIGHT = 0.28F;
    private static final float DEPTH_SHADE = 0.6F;

    private LiquidBox() {
    }

    private static TextureAtlasSprite sprite() {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(LIQUID);
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, float minX, float minZ, float maxX, float maxZ,
                              float bottom, float top, int argb) {
        submit(poseStack, collector, minX, minZ, maxX, maxZ, bottom, top, argb, 0, 0, 0);
    }

    /**
     * @param time    game time in ticks (with the partial tick), drives the bubbles
     * @param seed    spreads the bubbles of different vessels apart
     * @param bubbles how many bubbles rise at once (0 for none)
     */
    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, float minX, float minZ, float maxX, float maxZ,
                              float bottom, float top, int argb, float time, long seed, int bubbles) {
        if (top - bottom <= 0.001F) {
            return;
        }
        TextureAtlasSprite sprite = sprite();
        int alpha = argb >>> 24;
        int rgb = argb & 0xFFFFFF;
        int surface = alpha << 24 | RuneDrawing.mix(rgb, 0xFFFFFF, SURFACE_LIGHT);
        int deep = alpha << 24 | RuneDrawing.mix(rgb, 0x000000, 1 - DEPTH_SHADE);
        int high = argb;
        // The sides' colour runs from the surface down to the depth over the whole vessel, not over this box only.
        Box box = new Box(minX, minZ, maxX, maxZ, bottom, top, sprite);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS), (pose, consumer) -> {
            box.top(consumer, pose, surface);
            box.sides(consumer, pose, high, deep);
        });
        rise(poseStack, collector, box, time, seed, bubbles, rgb, alpha);
    }

    /**
     * Liquid in a vessel whose vertical edges are cut back by {@code cut} (the Essence Jar): its outline steps in at the
     * corners like the glass, so no corner of the liquid stands outside the vessel. Bubbles rise in the middle.
     */
    public static void submitCutCorners(PoseStack poseStack, SubmitNodeCollector collector, float minX, float minZ, float maxX,
                                        float maxZ, float cut, float bottom, float top, int argb, float time, long seed, int bubbles) {
        if (top - bottom <= 0.001F) {
            return;
        }
        TextureAtlasSprite sprite = sprite();
        int alpha = argb >>> 24;
        int rgb = argb & 0xFFFFFF;
        int surface = alpha << 24 | RuneDrawing.mix(rgb, 0xFFFFFF, SURFACE_LIGHT);
        int deep = alpha << 24 | RuneDrawing.mix(rgb, 0x000000, 1 - DEPTH_SHADE);
        float x0 = minX + cut;
        float x1 = maxX - cut;
        float z0 = minZ + cut;
        float z1 = maxZ - cut;
        // Round the outline the same way as a box's walls (north edge first, towards +x), two corners per cut.
        float[][] outline = {{x0, minZ}, {x1, minZ}, {x1, z0}, {maxX, z0}, {maxX, z1}, {x1, z1}, {x1, maxZ}, {x0, maxZ}, {x0, z1},
                {minX, z1}, {minX, z0}, {x0, z0}};
        Box middle = new Box(x0, z0, x1, z1, bottom, top, sprite);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS), (pose, consumer) -> {
            new Box(x0, minZ, x1, maxZ, bottom, top, sprite).top(consumer, pose, surface);
            new Box(minX, z0, x0, z1, bottom, top, sprite).top(consumer, pose, surface);
            new Box(x1, z0, maxX, z1, bottom, top, sprite).top(consumer, pose, surface);
            for (int i = 0; i < outline.length; i++) {
                middle.wall(consumer, pose, outline[i], outline[(i + 1) % outline.length], argb, deep);
            }
        });
        rise(poseStack, collector, middle, time, seed, bubbles, rgb, alpha);
    }

    /** Bubbles rising from the bottom to the surface, each starting over somewhere else when it gets there. */
    private static void rise(PoseStack poseStack, SubmitNodeCollector collector, Box box, float time, long seed, int bubbles, int rgb, int alpha) {
        if (bubbles <= 0) {
            return;
        }
        List<float[]> quads = new ArrayList<>(bubbles);
        int color = RuneDrawing.mix(rgb, 0xFFFFFF, BUBBLE_LIGHT);
        for (int index = 0; index < bubbles; index++) {
            float period = 40 + (Math.floorMod(seed * 31 + index * 17, 23));
            float phase = (time + index * 37 + Math.floorMod(seed, 97)) / period;
            int cycle = (int) Math.floor(phase);
            float t = phase - cycle;
            long h = seed * 6364136223846793005L + index * 1442695040888963407L + cycle * 2862933555777941L;
            float fx = ((h >>> 20) & 1023) / 1023F;
            float fz = ((h >>> 40) & 1023) / 1023F;
            // Half the quad: the ring fills 6 of the texture's 8 pixels, so a bubble looks 1.5-2.4 px across.
            float half = (1 + ((h >>> 10) & 3) * 0.2F) / 16;
            float pad = half * 0.75F + 0.3F / 16;
            if (box.top - box.bottom < pad * 3 || box.maxX - box.minX < pad * 2 || box.maxZ - box.minZ < pad * 2) {
                continue;
            }
            float x = Mth.lerp(fx, box.minX + pad, box.maxX - pad) + Mth.sin(t * 9 + index) * 0.25F / 16;
            float z = Mth.lerp(fz, box.minZ + pad, box.maxZ - pad);
            float y = Mth.lerp(t, box.bottom + pad, box.top - pad);
            quads.add(HexFx.quad(x, y, z, half, 0, 0, alpha / 255F * 0.9F * Math.min(1, (1 - t) * 4), color));
        }
        bubbles(poseStack, collector, quads);
    }

    /** Vanilla bubbles facing the camera, in one batch: quads as {@link HexFx#quad} makes them (frame 0). */
    public static void bubbles(PoseStack poseStack, SubmitNodeCollector collector, List<float[]> quads) {
        HexFx.sheet(poseStack, collector, Minecraft.getInstance().gameRenderer.mainCamera().rotation(), BUBBLE, 1, quads);
    }

    /**
     * One vanilla bubble sitting on a surface at the pose origin: it swells for the first {@code swell} of its life
     * ({@code t} 0-1), then bursts through vanilla's five pop frames. {@code half} is half the bubble's quad.
     */
    public static void surfaceBubble(PoseStack poseStack, SubmitNodeCollector collector, float t, float swell, float half, int rgb, float alpha) {
        int color = RuneDrawing.mix(rgb, 0xFFFFFF, BUBBLE_LIGHT);
        Quaternionfc camera = Minecraft.getInstance().gameRenderer.mainCamera().rotation();
        if (t < swell) {
            float size = half * (0.4F + 0.6F * Mth.sqrt(t / swell));
            HexFx.sheet(poseStack, collector, camera, BUBBLE, 1, List.of(HexFx.quad(0, size * 0.5F, 0, size, 0, 0, alpha, color)));
        } else {
            // The pop frames are 16 px with the burst in their middle: twice the bubble's quad shows it at the same size.
            int frame = Math.min(BUBBLE_POP.length - 1, (int) ((t - swell) / (1 - swell) * BUBBLE_POP.length));
            HexFx.sheet(poseStack, collector, camera, BUBBLE_POP[frame], 1, List.of(HexFx.quad(0, half * 0.5F, 0, half * 2, 0, 0, alpha, color)));
        }
    }

    /** A box of liquid with its texture coordinates taken from where it sits in the block. */
    private record Box(float minX, float minZ, float maxX, float maxZ, float bottom, float top, TextureAtlasSprite sprite) {
        float u(float fraction) {
            return sprite.getU0() + (sprite.getU1() - sprite.getU0()) * Mth.clamp(fraction, 0, 1);
        }

        float v(float fraction) {
            return sprite.getV0() + (sprite.getV1() - sprite.getV0()) * Mth.clamp(fraction, 0, 1);
        }

        void top(VertexConsumer consumer, PoseStack.Pose pose, int argb) {
            float[][] corners = {{minX, minZ}, {maxX, minZ}, {maxX, maxZ}, {minX, maxZ}};
            for (int i = 0; i < 4; i++) {
                vertex(consumer, pose, corners[i][0], top, corners[i][1], u(corners[i][0]), v(corners[i][1]), argb, 0, 1, 0);
            }
            for (int i = 3; i >= 0; i--) {
                vertex(consumer, pose, corners[i][0], top, corners[i][1], u(corners[i][0]), v(corners[i][1]), argb, 0, -1, 0);
            }
        }

        /** The four walls, light at the top edge and dark at the bottom edge. */
        void sides(VertexConsumer consumer, PoseStack.Pose pose, int high, int deep) {
            float[][] corners = {{minX, minZ}, {maxX, minZ}, {maxX, maxZ}, {minX, maxZ}};
            for (int i = 0; i < 4; i++) {
                wall(consumer, pose, corners[i], corners[(i + 1) % 4], high, deep);
            }
        }

        /** A wall from corner a to corner b of an outline running round like the box's (north wall towards +x first). */
        void wall(VertexConsumer consumer, PoseStack.Pose pose, float[] a, float[] b, int high, int deep) {
            boolean alongX = a[1] == b[1];
            float ua = u(alongX ? a[0] : a[1]);
            float ub = u(alongX ? b[0] : b[1]);
            float vTop = v(1 - top);
            float vBottom = v(1 - bottom);
            // The edge's direction turned to the outside of the outline.
            float nx = Math.signum(b[1] - a[1]);
            float nz = -Math.signum(b[0] - a[0]);
            // Outside winding, then inside winding.
            vertex(consumer, pose, a[0], bottom, a[1], ua, vBottom, deep, nx, 0, nz);
            vertex(consumer, pose, b[0], bottom, b[1], ub, vBottom, deep, nx, 0, nz);
            vertex(consumer, pose, b[0], top, b[1], ub, vTop, high, nx, 0, nz);
            vertex(consumer, pose, a[0], top, a[1], ua, vTop, high, nx, 0, nz);
            vertex(consumer, pose, a[0], top, a[1], ua, vTop, high, -nx, 0, -nz);
            vertex(consumer, pose, b[0], top, b[1], ub, vTop, high, -nx, 0, -nz);
            vertex(consumer, pose, b[0], bottom, b[1], ub, vBottom, deep, -nx, 0, -nz);
            vertex(consumer, pose, a[0], bottom, a[1], ua, vBottom, deep, -nx, 0, -nz);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb,
                               float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, nx, ny, nz);
    }
}
