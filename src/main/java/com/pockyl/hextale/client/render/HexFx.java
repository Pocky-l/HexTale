package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
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
    public static final Identifier HALO = Hextale.id("textures/misc/orb_halo.png");
    public static final Identifier CORE = Hextale.id("textures/misc/orb_core.png");
    public static final Identifier MOTE = Hextale.id("textures/misc/mote.png");
    public static final Identifier WHITE = Hextale.id("textures/misc/white.png");
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
    public static void sprite(PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera, Identifier texture, float size, int color,
                              float alpha) {
        poseStack.pushPose();
        poseStack.rotate(camera);
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

    /** A cube of edge {@code size} (blocks) centred on the pose origin, each face showing the whole core texture. */
    public static void cube(PoseStack poseStack, SubmitNodeCollector collector, float size, int color, float alpha) {
        float h = size / 2;
        int argb = RuneDrawing.argb(color, alpha);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(CORE), (pose, consumer) -> {
            for (float[] f : CUBE_FACES) {
                for (int v = 0; v < 4; v++) {
                    consumer.addVertex(pose, f[v * 3] * h, f[v * 3 + 1] * h, f[v * 3 + 2] * h).setColor(argb)
                            .setUv(CUBE_UVS[v][0], CUBE_UVS[v][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                            .setNormal(pose, f[12], f[13], f[14]);
                }
            }
        });
    }

    /** A faceted hextech crystal: a bright core cube turning inside a fainter, larger one. */
    public static void crystal(PoseStack poseStack, SubmitNodeCollector collector, float time, float speed, float size, int color,
                               float intensity) {
        poseStack.pushPose();
        poseStack.rotate(Axis.YP.rotationDegrees(time * speed));
        poseStack.rotate(Axis.XP.rotationDegrees(35 + Mth.sin(time * 0.05F) * 12));
        poseStack.rotate(Axis.ZP.rotationDegrees(45));
        cube(poseStack, collector, size, RuneDrawing.lighten(color, 0.25F * intensity), 1);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.rotate(Axis.YP.rotationDegrees(-time * speed * 0.7F));
        poseStack.rotate(Axis.ZP.rotationDegrees(30));
        cube(poseStack, collector, size * 1.42F, color, 0.28F + 0.12F * intensity);
        poseStack.popPose();
    }

    /**
     * A rune's hieroglyph as glowing strokes of {@code size} blocks, in the pose's XY plane (a soft wide stroke under a
     * bright thin one).
     */
    public static void glyph(PoseStack poseStack, SubmitNodeCollector collector, ComplexRune rune, float size, int color, float alpha) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(WHITE), (pose, consumer) -> {
            for (float[] stroke : RuneGlyphs.glyph(rune)) {
                for (int i = 0; i + 3 < stroke.length; i += 2) {
                    float x0 = (stroke[i] - 0.5F) * size;
                    float y0 = (0.5F - stroke[i + 1]) * size;
                    float x1 = (stroke[i + 2] - 0.5F) * size;
                    float y1 = (0.5F - stroke[i + 3]) * size;
                    segment(consumer, pose, x0, y0, x1, y1, size * 0.11F, color, alpha * 0.25F);
                    segment(consumer, pose, x0, y0, x1, y1, size * 0.045F, RuneDrawing.lighten(color, 0.5F), alpha);
                }
            }
        });
    }

    /** A line segment as a quad of the given width, with square caps that overlap the next segment; both windings. */
    public static void segment(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float width, int color,
                               float alpha) {
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
        vertex(consumer, pose, x0 + nx, y0 + ny, 0, 0, color, alpha);
        vertex(consumer, pose, x0 - nx, y0 - ny, 0, 1, color, alpha);
        vertex(consumer, pose, x1 - nx, y1 - ny, 1, 1, color, alpha);
        vertex(consumer, pose, x1 + nx, y1 + ny, 1, 0, color, alpha);
        vertex(consumer, pose, x1 + nx, y1 + ny, 1, 0, color, alpha);
        vertex(consumer, pose, x1 - nx, y1 - ny, 1, 1, color, alpha);
        vertex(consumer, pose, x0 - nx, y0 - ny, 0, 1, color, alpha);
        vertex(consumer, pose, x0 + nx, y0 + ny, 0, 0, color, alpha);
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
                        .setColor(argb).setUv(c[2], c[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                        .setNormal(pose, 0, 1, 0);
            }
        }
    }

    public static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int color, float alpha) {
        consumer.addVertex(pose, x, y, 0).setColor(RuneDrawing.argb(color, alpha)).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
    }
}
