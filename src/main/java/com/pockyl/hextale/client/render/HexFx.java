package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.RuneGlyphs;

/**
 * Hextech light effects shared by the renderers: glow sprites, faceted crystal cubes and rune hieroglyphs drawn as
 * glowing strokes. Everything is full bright and uses textures with real alpha (no additive blending over black), so
 * it looks right with shader packs too.
 */
public final class HexFx {
    public static final ResourceLocation HALO = Hextale.id("textures/misc/orb_halo.png");
    public static final ResourceLocation CORE = Hextale.id("textures/misc/orb_core.png");
    public static final ResourceLocation MOTE = Hextale.id("textures/misc/mote.png");
    public static final ResourceLocation WHITE = Hextale.id("textures/misc/white.png");
    /** Four-pointed glints, 4 frames side by side (the glint particle's art). */
    public static final ResourceLocation GLINT = Hextale.id("textures/misc/glint.png");
    public static final int GLINT_FRAMES = 4;
    /** Soft puffs of vapour, 6 frames side by side (the vapour particle's art). */
    public static final ResourceLocation VAPOUR = Hextale.id("textures/misc/vapour.png");
    public static final int VAPOUR_FRAMES = 6;
    public static final int HEXTECH = 0x5CC8F2;

    private static final float[][] CUBE_FACES = {
            {-1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1, 0, 0, 1},
            {1, -1, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 0, 0, -1},
            {1, -1, 1, 1, -1, -1, 1, 1, -1, 1, 1, 1, 1, 0, 0},
            {-1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1, -1, 0, 0},
            {-1, 1, 1, 1, 1, 1, 1, 1, -1, -1, 1, -1, 0, 1, 0},
            {-1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, 1, 0, -1, 0},
    };
    private static final float[][] CUBE_UVS = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};

    /** Animation clock period: game time wraps here so floats keep sub-tick precision (smooth motion in old worlds). */
    private static final long CLOCK_PERIOD = 72000;

    private HexFx() {
    }

    /**
     * Ticks for animations: game time plus the partial tick, wrapped to keep float precision. Raw game time in a world
     * that ran for days loses the fraction of a tick in a float, and spinning items stutter.
     */
    public static float time(@Nullable Level level, float partialTick) {
        return level == null ? partialTick : (level.getGameTime() % CLOCK_PERIOD) + partialTick;
    }

    /** A camera-facing sprite at the current pose origin, drawn with both windings. */
    public static void sprite(PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera, ResourceLocation texture, float size, int color,
                              float alpha) {
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Quaternionf(camera));
        float h = size / 2;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(texture), (pose, consumer) -> {
            vertex(consumer, pose, -h, -h, 0, 1, color, alpha);
            vertex(consumer, pose, h, -h, 1, 1, color, alpha);
            vertex(consumer, pose, h, h, 1, 0, color, alpha);
            vertex(consumer, pose, -h, h, 0, 0, color, alpha);
            vertex(consumer, pose, -h, h, 0, 0, color, alpha);
            vertex(consumer, pose, h, h, 1, 0, color, alpha);
            vertex(consumer, pose, h, -h, 1, 1, color, alpha);
            vertex(consumer, pose, -h, -h, 0, 1, color, alpha);
        });
        poseStack.popPose();
    }

    /**
     * Camera-facing quads cut from a sheet of {@code frames} sprites side by side, in one batch. Each quad is
     * {x, y, z, half size, frame, roll, alpha, rgb} relative to the pose origin.
     */
    public static void sheet(PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera, ResourceLocation texture, int frames,
                             java.util.List<float[]> quads) {
        if (quads.isEmpty()) {
            return;
        }
        Vector3f right = camera.transform(new Vector3f(1, 0, 0));
        Vector3f up = camera.transform(new Vector3f(0, 1, 0));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(texture), (pose, consumer) -> {
            for (float[] q : quads) {
                float s = q[3];
                if (s <= 0.002F || q[6] <= 0.004F) {
                    continue;
                }
                float cos = Mth.cos(q[5]) * s;
                float sin = Mth.sin(q[5]) * s;
                Vector3f r = new Vector3f(right).mul(cos).add(new Vector3f(up).mul(sin));
                Vector3f u = new Vector3f(up).mul(cos).sub(new Vector3f(right).mul(sin));
                float u0 = q[4] / frames;
                float u1 = (q[4] + 1) / frames;
                int color = (int) q[7];
                vertex(consumer, pose, q[0] - r.x - u.x, q[1] - r.y - u.y, q[2] - r.z - u.z, u0, 1, color, q[6]);
                vertex(consumer, pose, q[0] + r.x - u.x, q[1] + r.y - u.y, q[2] + r.z - u.z, u1, 1, color, q[6]);
                vertex(consumer, pose, q[0] + r.x + u.x, q[1] + r.y + u.y, q[2] + r.z + u.z, u1, 0, color, q[6]);
                vertex(consumer, pose, q[0] - r.x + u.x, q[1] - r.y + u.y, q[2] - r.z + u.z, u0, 0, color, q[6]);
            }
        });
    }

    /** A sheet quad: see {@link #sheet}. */
    public static float[] quad(double x, double y, double z, float size, int frame, float roll, float alpha, int rgb) {
        return new float[]{(float) x, (float) y, (float) z, size, frame, roll, alpha, rgb & 0xFFFFFF};
    }

    /** A cube of edge {@code size} (blocks) centred on the pose origin, each face showing the whole core texture. */
    public static void cube(PoseStack poseStack, SubmitNodeCollector collector, float size, int color, float alpha) {
        float h = size / 2;
        int argb = RuneDrawing.argb(color, alpha);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(CORE), (pose, consumer) -> {
            for (float[] f : CUBE_FACES) {
                for (int v = 0; v < 4; v++) {
                    consumer.addVertex(pose, f[v * 3] * h, f[v * 3 + 1] * h, f[v * 3 + 2] * h).setColor(argb)
                            .setUv(CUBE_UVS[v][0], CUBE_UVS[v][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                            .setNormal(pose, f[12], f[13], f[14]);
                }
            }
        });
    }

    /** A faceted hextech crystal: a bright core cube turning inside a fainter, larger one. */
    public static void crystal(PoseStack poseStack, SubmitNodeCollector collector, float time, float speed, float size, int color,
                               float intensity) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * speed));
        poseStack.mulPose(Axis.XP.rotationDegrees(35 + Mth.sin(time * 0.05F) * 12));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45));
        cube(poseStack, collector, size, RuneDrawing.lighten(color, 0.25F * intensity), 1);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * speed * 0.7F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(30));
        cube(poseStack, collector, size * 1.42F, color, 0.28F + 0.12F * intensity);
        poseStack.popPose();
    }

    /**
     * A hextech crystal of additive light: the edges of a hexagonal bipyramid (six round its girdle, twelve to its two
     * points) as fine near-white lines in a soft glow of {@code color}, turned by {@code turn}, {@code radius} blocks to
     * the girdle and {@code height} to each point. {@code eye} is the camera in the pose's space (the pose must not be
     * rotated: turn the crystal with {@code turn}). The gem drawn as light, like the stele runes and the streams.
     */
    public static void lightCrystal(PoseStack poseStack, SubmitNodeCollector collector, Vector3fc eye, Quaternionfc turn, float radius,
                                    float height, int color, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        Vec3[] corner = new Vec3[8];
        for (int i = 0; i < 6; i++) {
            float a = Mth.TWO_PI * i / 6;
            corner[i] = vec(turn.transform(new Vector3f(Mth.cos(a) * radius, 0, Mth.sin(a) * radius)));
        }
        corner[6] = vec(turn.transform(new Vector3f(0, height, 0)));
        corner[7] = vec(turn.transform(new Vector3f(0, -height, 0)));
        int vivid = JarStreamFx.vivid(color);
        int core = RuneDrawing.mix(vivid, 0xFFFFFF, 0.65F);
        Vector3f view = new Vector3f(eye);
        float[] light = {1, 1};
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            for (int i = 0; i < 6; i++) {
                for (Vec3 end : new Vec3[]{corner[(i + 1) % 6], corner[6], corner[7]}) {
                    Vec3[] edge = {corner[i], end};
                    JarStreamFx.strip(consumer, pose, edge, light, view, radius * 0.45F, vivid, 0.22F * alpha);
                    JarStreamFx.strip(consumer, pose, edge, light, view, radius * 0.1F, core, 0.8F * alpha);
                }
            }
        });
    }

    private static Vec3 vec(Vector3f v) {
        return new Vec3(v.x, v.y, v.z);
    }

    /**
     * A rune's hieroglyph as glowing strokes of {@code size} blocks, in the pose's XY plane: a wide faint glow, a
     * narrower one and a near-white core, each fading to nothing at its edges. Drawn with additive light (the
     * {@link HexGlow} type, no depth writes), so the layers never need sorting or depth tests against each other.
     */
    public static void glyph(PoseStack poseStack, SubmitNodeCollector collector, ComplexRune rune, float size, int color, float alpha) {
        glyph(poseStack, collector, rune, size, color, alpha, 1);
    }

    /** As {@link #glyph}, with only the first {@code reveal} (0-1) of the strokes' length drawn - the rune writing itself. */
    public static void glyph(PoseStack poseStack, SubmitNodeCollector collector, ComplexRune rune, float size, int color, float alpha,
                             float reveal) {
        if (alpha <= 0.01F || reveal <= 0) {
            return;
        }
        java.util.List<float[]> strokes = RuneGlyphs.glyph(rune);
        float total = 0;
        for (float[] stroke : strokes) {
            for (int i = 0; i + 3 < stroke.length; i += 2) {
                total += Mth.sqrt(Mth.square(stroke[i + 2] - stroke[i]) + Mth.square(stroke[i + 3] - stroke[i + 1]));
            }
        }
        float budget = total * Math.min(1, reveal);
        int core = RuneDrawing.mix(color, 0xFFFFFF, 0.7F);
        float drawnLength = budget;
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            float left = drawnLength;
            for (float[] stroke : strokes) {
                for (int i = 0; i + 3 < stroke.length && left > 0; i += 2) {
                    float ax = stroke[i];
                    float ay = stroke[i + 1];
                    float bx = stroke[i + 2];
                    float by = stroke[i + 3];
                    float length = Mth.sqrt(Mth.square(bx - ax) + Mth.square(by - ay));
                    if (length > left) {
                        float t = left / length;
                        bx = ax + (bx - ax) * t;
                        by = ay + (by - ay) * t;
                    }
                    left -= length;
                    float x0 = (ax - 0.5F) * size;
                    float y0 = (0.5F - ay) * size;
                    float x1 = (bx - 0.5F) * size;
                    float y1 = (0.5F - by) * size;
                    glowSegment(consumer, pose, x0, y0, x1, y1, size * 0.22F, color, alpha * 0.22F);
                    glowSegment(consumer, pose, x0, y0, x1, y1, size * 0.09F, color, alpha * 0.55F);
                    glowSegment(consumer, pose, x0, y0, x1, y1, size * 0.035F, core, alpha);
                }
            }
        });
    }

    /**
     * The sigil round a rune or a device: a thin hexagon of additive light, {@code radius} to its corners, in the pose's
     * XY plane, with {@code ticks} short marks standing out of its outline (evenly round it, the first on a corner) -
     * the hextech hexagon motif everywhere (2026-10-28, was a round ring). Line width stops growing at a 1.25-block
     * ring, so a device's wide reach stays a fine line.
     */
    public static void sigilRing(PoseStack poseStack, SubmitNodeCollector collector, float radius, int ticks, int color, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        float w = Math.min(radius, 1.25F);
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            for (int i = 0; i < 6; i++) {
                float a0 = Mth.TWO_PI * i / 6;
                float a1 = Mth.TWO_PI * (i + 1) / 6;
                glowSegment(consumer, pose, Mth.cos(a0) * radius, Mth.sin(a0) * radius, Mth.cos(a1) * radius, Mth.sin(a1) * radius,
                        w * 0.08F, color, alpha);
            }
            float apothem = radius * Mth.cos(Mth.PI / 6);
            for (int i = 0; i < ticks; i++) {
                float a = Mth.TWO_PI * i / ticks;
                // Where the mark's ray leaves the hexagon: the outline's distance at this angle.
                float inSide = (a % (Mth.TWO_PI / 6)) - Mth.PI / 6;
                float edge = apothem / Mth.cos(inSide);
                float out = w * 0.12F;
                glowSegment(consumer, pose, Mth.cos(a) * (edge + w * 0.03F), Mth.sin(a) * (edge + w * 0.03F), Mth.cos(a) * (edge + out),
                        Mth.sin(a) * (edge + out), w * 0.07F, color, alpha * 1.3F);
            }
        });
    }

    /**
     * A flat hexagon of additive light, {@code radius} blocks to its corners, in the pose's XY plane: a soft wide glow with
     * a near-white core along the outer line, a fainter inner hexagon turned half a side, and short marks pointing out of
     * the corners - the hextech sigil. Line widths grow with the radius only up to a 1.2 block hexagon, so a great ground
     * sigil stays drawn in fine lines.
     */
    public static void hexRing(PoseStack poseStack, SubmitNodeCollector collector, float radius, int color, float alpha) {
        if (alpha <= 0.01F || radius <= 0.01F) {
            return;
        }
        int core = RuneDrawing.mix(color, 0xFFFFFF, 0.6F);
        float w = Math.min(radius, 1.2F);
        float mark = w * 0.2F;
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            float inner = radius - w * 0.25F;
            for (int i = 0; i < 6; i++) {
                float a0 = Mth.TWO_PI * i / 6;
                float a1 = Mth.TWO_PI * (i + 1) / 6;
                float x0 = Mth.cos(a0) * radius;
                float y0 = Mth.sin(a0) * radius;
                float x1 = Mth.cos(a1) * radius;
                float y1 = Mth.sin(a1) * radius;
                glowSegment(consumer, pose, x0, y0, x1, y1, w * 0.16F, color, alpha * 0.35F);
                glowSegment(consumer, pose, x0, y0, x1, y1, w * 0.045F, core, alpha);
                float b0 = a0 + Mth.PI / 6;
                float b1 = a1 + Mth.PI / 6;
                glowSegment(consumer, pose, Mth.cos(b0) * inner, Mth.sin(b0) * inner, Mth.cos(b1) * inner, Mth.sin(b1) * inner,
                        w * 0.05F, color, alpha * 0.45F);
                float cx = Mth.cos(a0);
                float cy = Mth.sin(a0);
                glowSegment(consumer, pose, cx * (radius + mark * 0.3F), cy * (radius + mark * 0.3F), cx * (radius + mark),
                        cy * (radius + mark), w * 0.06F, core, alpha * 0.8F);
            }
        });
    }

    /**
     * A segment of additive light: full {@code alpha} along its centre line, fading to 0 at both long edges, ends
     * reaching a little past the points so strokes join (drawn without culling).
     */
    public static void glowSegment(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float width,
                                    int color, float alpha) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4F) {
            return;
        }
        float h = width / 2;
        float nx = -dy / length * h;
        float ny = dx / length * h;
        float ex = dx / length * h * 0.5F;
        float ey = dy / length * h * 0.5F;
        x0 -= ex;
        y0 -= ey;
        x1 += ex;
        y1 += ey;
        int centre = argbOf(color, alpha);
        int edge = argbOf(color, 0);
        for (int side = -1; side <= 1; side += 2) {
            float sx = nx * side;
            float sy = ny * side;
            glow(consumer, pose, x0, y0, centre);
            glow(consumer, pose, x0 + sx, y0 + sy, edge);
            glow(consumer, pose, x1 + sx, y1 + sy, edge);
            glow(consumer, pose, x1, y1, centre);
        }
    }

    private static void glow(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, int argb) {
        consumer.addVertex(pose, x, y, 0).setColor(argb);
    }

    private static int argbOf(int rgb, float alpha) {
        return (Math.round(Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** A line segment as a quad of the given width, with square caps that overlap the next segment; both windings. */
    public static void segment(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float z, float width,
                               int color, float alpha) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4F) {
            return;
        }
        float h = width / 2;
        float nx = -dy / length * h;
        float ny = dx / length * h;
        float ex = dx / length * h;
        float ey = dy / length * h;
        x0 -= ex;
        y0 -= ey;
        x1 += ex;
        y1 += ey;
        vertex(consumer, pose, x0 + nx, y0 + ny, z, 0, 0, color, alpha);
        vertex(consumer, pose, x0 - nx, y0 - ny, z, 0, 1, color, alpha);
        vertex(consumer, pose, x1 - nx, y1 - ny, z, 1, 1, color, alpha);
        vertex(consumer, pose, x1 + nx, y1 + ny, z, 1, 0, color, alpha);
        vertex(consumer, pose, x1 + nx, y1 + ny, z, 1, 0, color, alpha);
        vertex(consumer, pose, x1 - nx, y1 - ny, z, 1, 1, color, alpha);
        vertex(consumer, pose, x0 - nx, y0 - ny, z, 0, 1, color, alpha);
        vertex(consumer, pose, x0 + nx, y0 + ny, z, 0, 0, color, alpha);
    }

    /** A thin glowing beam from the pose origin to {@code (dx, dy, dz)}, turned to face the camera along its length. */
    public static void beam(PoseStack poseStack, SubmitNodeCollector collector, float dx, float dy, float dz, float width, int color, float alpha,
                            Vector3fc toCamera) {
        Vector3f along = new Vector3f(dx, dy, dz);
        Vector3f side = new Vector3f(toCamera).cross(along);
        if (side.lengthSquared() < 1.0E-6F) {
            return;
        }
        side.normalize(width / 2);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(HALO), (pose, consumer) -> {
            quad(consumer, pose, side, along, color, alpha);
        });
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Vector3f side, Vector3f along, int color, float alpha) {
        int argb = RuneDrawing.argb(color, alpha);
        float[][] corners = {{-1, 0, 0, 0.5F}, {1, 0, 1, 0.5F}, {1, 1, 1, 0.5F}, {-1, 1, 0, 0.5F}};
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                float[] c = corners[pass == 0 ? k : 3 - k];
                consumer.addVertex(pose, side.x * c[0] + along.x * c[1], side.y * c[0] + along.y * c[1], side.z * c[0] + along.z * c[1])
                        .setColor(argb).setUv(c[2], c[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(pose, 0, 1, 0);
            }
        }
    }

    public static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int color, float alpha) {
        vertex(consumer, pose, x, y, 0, u, v, color, alpha);
    }

    public static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color, float alpha) {
        consumer.addVertex(pose, x, y, z).setColor(RuneDrawing.argb(color, alpha)).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
    }
}
