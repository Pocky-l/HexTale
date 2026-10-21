package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/**
 * A whirl of hextech wind in the same light as the jar streams ({@link HexGlow}, additive, nothing painted over the
 * world): four strands winding up a funnel that widens towards the top, its axis swaying, a brighter core strand at
 * each, sparks riding up the strands and a turning sigil on the ground under it. Positions are relative to the pose
 * origin; {@code size} scales the funnel's width (0.35 = a small whirl in flight), {@code alpha} fades all of it.
 */
public final class WhirlFx {
    /** Pale hextech wind: the wind essence's white pushed towards the crystal's cyan so it reads in daylight. */
    public static final int WIND = 0x8FE6FF;
    private static final int STRANDS = 4;
    private static final int SEGMENTS = 28;
    private static final int SPARKS = 6;
    private static final int PUFFS = 12;
    /** The puffs: white air with a breath of the wind's blue. */
    private static final int PUFF = 0xE6F7FF;

    private WhirlFx() {
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera, Vec3 cameraLocal, Vec3 foot,
                              float height, float size, float alpha, float time, int seed) {
        if (alpha <= 0.01F) {
            return;
        }
        int color = WIND;
        int core = RuneDrawing.mix(color, 0xFFFFFF, 0.7F);
        float phase = (seed & 255) / 255F * Mth.TWO_PI;
        Vector3f eye = new Vector3f((float) cameraLocal.x, (float) cameraLocal.y, (float) cameraLocal.z);
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            Vec3[] points = new Vec3[SEGMENTS + 1];
            float[] light = new float[SEGMENTS + 1];
            for (int strand = 0; strand < STRANDS; strand++) {
                float offset = strand * Mth.TWO_PI / STRANDS + phase;
                for (int i = 0; i <= SEGMENTS; i++) {
                    float t = i / (float) SEGMENTS;
                    points[i] = helix(foot, height, size, t, time, offset);
                    // Thin at the foot, full in the middle, thinning out at the top; a wave of light runs up.
                    float ends = Math.min(1, t / 0.15F) * Math.min(1, (1 - t) / 0.2F + 0.15F);
                    float wave = 0.35F + 0.65F * Mth.square(Mth.sin((t * 2.2F - time * 0.09F + strand * 0.5F) * Mth.PI));
                    light[i] = ends * wave;
                }
                JarStreamFx.strip(consumer, pose, points, light, eye, 0.09F * size + 0.02F, color, 0.35F * alpha);
                JarStreamFx.strip(consumer, pose, points, light, eye, 0.022F, core, 0.6F * alpha);
            }
        });
        // The body of the wind: puffs of air swept round the funnel and up, swelling and thinning as they rise.
        java.util.List<float[]> puffs = new java.util.ArrayList<>(PUFFS);
        for (int k = 0; k < PUFFS; k++) {
            float t = frac(time * 0.022F + k / (float) PUFFS + phase * 0.1F);
            Vec3 at = helix(foot, height, size, t, time, k * 2.3F + phase);
            float fade = Math.min(1, t / 0.15F) * Math.min(1, (1 - t) / 0.35F);
            puffs.add(HexFx.quad(at.x, at.y, at.z, (0.12F + 0.3F * t) * Math.max(0.45F, size), k % HexFx.VAPOUR_FRAMES,
                    -time * 0.08F + k * 1.7F, 0.38F * fade * alpha, PUFF));
        }
        HexFx.sheet(poseStack, collector, camera, HexFx.VAPOUR, HexFx.VAPOUR_FRAMES, puffs);
        // Glints riding up the strands, twinkling.
        java.util.List<float[]> glints = new java.util.ArrayList<>(SPARKS);
        for (int k = 0; k < SPARKS; k++) {
            float t = frac(time * 0.035F + k / (float) SPARKS + phase);
            Vec3 at = helix(foot, height, size, t, time, k * 1.7F + phase);
            float twinkle = 0.55F + 0.45F * Mth.sin(time * 0.8F + k * 2.1F);
            glints.add(HexFx.quad(at.x, at.y, at.z, (0.06F + 0.05F * size) * twinkle, k % HexFx.GLINT_FRAMES, time * 0.06F + k,
                    alpha * Math.min(1, (1 - t) * 3), core));
        }
        HexFx.sheet(poseStack, collector, camera, HexFx.GLINT, HexFx.GLINT_FRAMES, glints);
        // The sigil on the ground under the funnel.
        poseStack.pushPose();
        poseStack.translate(foot.x, foot.y + 0.01, foot.z);
        poseStack.rotate(Axis.XP.rotationDegrees(90));
        poseStack.rotate(Axis.ZP.rotation(time * 0.2F + phase));
        HexFx.sigilRing(poseStack, collector, 0.18F + 0.32F * size, 6, color, 0.6F * alpha);
        poseStack.popPose();
    }

    /** A point of one strand at {@code t} (0 foot - 1 top): round the swaying axis, the funnel widening upwards. */
    static Vec3 helix(Vec3 foot, float height, float size, float t, float time, float offset) {
        float radius = size * (0.1F + 0.5F * (float) Math.pow(t, 1.4));
        float angle = t * Mth.TWO_PI * (1.6F + height * 0.35F) - time * 0.55F + offset;
        double sway = 0.07 * size * t;
        double ax = Mth.sin(time * 0.11F + offset * 0.1F) * sway;
        double az = Mth.cos(time * 0.13F) * sway;
        return foot.add(ax + Mth.cos(angle) * radius, t * height * Math.max(0.5F, size), az + Mth.sin(angle) * radius);
    }

    private static float frac(float x) {
        return x - Mth.floor(x);
    }
}
