package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import com.pockyl.hextale.port.client.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import com.pockyl.hextale.chem.Zipline;

/**
 * Draws a Chain Launcher's chain the way the vanilla chain block is drawn: two crossed strips with the iron chain
 * texture, one link pattern per block, following the line's sag; the hologram of an aimed line is a strip of chemtech
 * light with dashes running towards the target.
 */
public final class ChainRender {
    /**
     * The culling type, because every strip is drawn from both sides: the plain cutout type culls nothing and lights back
     * faces darker, so the two coincident sides would z-fight in two shades and the chain would flicker.
     */
    private static final RenderType CHAIN = RenderTypes.entityCutoutCull(ResourceLocation.withDefaultNamespace("textures/block/chain.png"));
    /** Half the strip's width: the chain texture's links are 3 px wide. */
    private static final float HALF = 1.5F / 16;
    /** The texture's two link columns. */
    private static final float U_A = 0;
    private static final float U_B = 3 / 16F;
    private static final float U_WIDTH = 3 / 16F;
    public static final int GOOD = 0x8CFF6A;
    public static final int BAD = 0xFF5A48;

    private ChainRender() {
    }

    /** Points every block or so along the line from t 0 to {@code upTo}, relative to {@code origin}. */
    public static Vec3[] points(Zipline.Line line, double upTo, Vec3 origin) {
        double length = line.length() * upTo;
        int segments = Math.clamp((long) Math.ceil(length), 1, 200);
        Vec3[] points = new Vec3[segments + 1];
        for (int i = 0; i <= segments; i++) {
            points[i] = line.point(upTo * i / segments).subtract(origin);
        }
        return points;
    }

    /** The light along the points (world positions = origin + point). */
    public static int[] light(BlockAndTintGetter level, Vec3[] points, Vec3 origin) {
        int[] light = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            light[i] = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, BlockPos.containing(points[i].add(origin)));
        }
        return light;
    }

    /** The chain itself along {@code points} (in the pose's space). */
    public static void submitChain(PoseStack poseStack, SubmitNodeCollector collector, Vec3[] points, int[] light) {
        // A chain is never longer than a line may be; anything else is broken data and would flood the vertex buffer.
        if (points.length < 2 || points[0].distanceTo(points[points.length - 1]) > Zipline.MAX_LENGTH * 2) {
            return;
        }
        collector.submitCustomGeometry(poseStack, CHAIN, (pose, consumer) -> chain(consumer, pose, points, light));
    }

    private static void chain(VertexConsumer consumer, PoseStack.Pose pose, Vec3[] points, int[] light) {
        double v = 0;
        for (int i = 0; i < points.length - 1; i++) {
            Vec3 a = points[i];
            Vec3 b = points[i + 1];
            Vec3 along = b.subtract(a);
            double length = along.length();
            if (length < 1.0E-4) {
                continue;
            }
            Vec3 dir = along.scale(1 / length);
            // Two strips at right angles, both containing the line: one across the horizontal, one across that.
            Vec3 flat = dir.cross(new Vec3(0, 1, 0));
            if (flat.lengthSqr() < 1.0E-6) {
                flat = new Vec3(1, 0, 0);
            }
            flat = flat.normalize().scale(HALF);
            Vec3 upright = dir.cross(flat).normalize().scale(HALF);
            float v0 = (float) v;
            float v1 = (float) (v + length);
            v += length;
            quad(consumer, pose, a, b, flat, U_A, v0, v1, light[i], light[i + 1]);
            quad(consumer, pose, a, b, upright, U_B, v0, v1, light[i], light[i + 1]);
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 side, float u, float v0, float v1,
                             int lightA, int lightB) {
        // The texture repeats every block: entity textures clamp, so whole tiles are split where v crosses a whole number.
        float from = v0;
        while (from < v1 - 1.0E-4F) {
            float tile = (float) Math.floor(from);
            float to = Math.min(v1, tile + 1);
            float f0 = (from - v0) / (v1 - v0);
            float f1 = (to - v0) / (v1 - v0);
            Vec3 p0 = a.add(b.subtract(a).scale(f0));
            Vec3 p1 = a.add(b.subtract(a).scale(f1));
            int l0 = f0 < 0.5F ? lightA : lightB;
            int l1 = f1 < 0.5F ? lightA : lightB;
            float t0 = from - tile;
            float t1 = to - tile;
            Vec3[] corners = {p0.subtract(side), p0.add(side), p1.add(side), p1.subtract(side)};
            float[][] uv = {{u, t0}, {u + U_WIDTH, t0}, {u + U_WIDTH, t1}, {u, t1}};
            int[] lights = {l0, l0, l1, l1};
            for (int pass = 0; pass < 2; pass++) {
                for (int k = 0; k < 4; k++) {
                    int j = pass == 0 ? k : 3 - k;
                    Vec3 c = corners[j];
                    consumer.addVertex(pose, (float) c.x, (float) c.y, (float) c.z).setColor(0xFFFFFFFF).setUv(uv[j][0], uv[j][1])
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(lights[j]).setNormal(pose, 0, 1, 0);
                }
            }
            from = to;
        }
    }

    /**
     * The hologram of an aimed line along {@code points}: a soft strip of light in {@code color} with brighter dashes
     * running towards the target ({@code time} in ticks).
     */
    public static void submitGhost(PoseStack poseStack, SubmitNodeCollector collector, Vec3[] points, Vec3 camera, int color, float time) {
        if (points.length < 2) {
            return;
        }
        // Finer points than one a block, so the dashes read.
        int fine = (points.length - 1) * 3;
        Vec3[] dense = new Vec3[fine + 1];
        float[] glow = new float[fine + 1];
        for (int i = 0; i <= fine; i++) {
            float f = i / 3F;
            int k = Math.min(points.length - 2, (int) f);
            dense[i] = points[k].add(points[k + 1].subtract(points[k]).scale(f - k));
            float dash = (float) Math.sin((i / 3F - time * 0.35F) * 1.6F);
            glow[i] = 0.28F + Math.max(0, dash) * 0.35F;
        }
        Vector3f eye = new Vector3f((float) camera.x, (float) camera.y, (float) camera.z);
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> JarStreamFx.strip(consumer, pose, dense, glow, eye, 0.12F, color, 0.9F));
    }

    /** A ring of light on the face the claw would bite: centre {@code at}, lying across {@code normal}. */
    public static void submitMark(PoseStack poseStack, SubmitNodeCollector collector, Vec3 at, Vec3 normal, int color, float time) {
        int steps = 24;
        float radius = 0.32F + 0.04F * (float) Math.sin(time * 0.25F);
        Vec3 a = Math.abs(normal.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = normal.cross(a).normalize();
        Vec3 w = normal.cross(u).normalize();
        Vec3[] ring = new Vec3[steps + 1];
        float[] glow = new float[steps + 1];
        for (int i = 0; i <= steps; i++) {
            double angle = Math.PI * 2 * i / steps;
            ring[i] = at.add(u.scale(Math.cos(angle) * radius)).add(w.scale(Math.sin(angle) * radius)).add(normal.scale(0.02));
            glow[i] = 0.6F;
        }
        Vector3f flat = new Vector3f((float) normal.x, (float) normal.y, (float) normal.z);
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> JarStreamFx.flatStrip(consumer, pose, ring, glow, flat, 0.07F, color, 0.9F));
    }
}
