package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import com.pockyl.hextale.essence.JarReach;

/**
 * Essence drawn out of a jar into a machine ({@link JarReach}), in the same light as the runes on the steles: additive
 * glow ({@link HexGlow}), nothing painted on top of the world. Three thin strands braid around the arc from the jar's
 * lid into the machine, waves of light run along them towards the machine, twinkling four-pointed sparks ride the
 * waves, a hex sigil turns on the jar's lid and another around the point where the essence pours in. The stream grows
 * out of the jar when the draw starts ({@code head} 0 -> 1) and runs into the machine when it ends ({@code tail} 0 -> 1).
 * Wisps shed along the way are particles ({@link JarReach#shed}). Positions are relative to the pose origin.
 */
public final class JarStreamFx {
    private static final int SEGMENTS = 40;
    private static final int STRANDS = 3;
    private static final int WAVES = 3;
    private static final int SPARKS = 6;
    /** Ticks a wave takes from the jar to the machine. */
    private static final float TRAVEL_TICKS = 24;

    private JarStreamFx() {
    }

    /** A stream, all positions relative to the pose origin. */
    public record Stream(Vec3 from, Vec3 to, int color, float head, float tail, int seed) {
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera, Vec3 cameraLocal, Stream stream,
                              float time) {
        float head = Mth.clamp(stream.head(), 0, 1);
        float tail = Mth.clamp(stream.tail(), 0, 1);
        if (head <= tail + 0.001F) {
            return;
        }
        Vec3 from = stream.from();
        Vec3 to = stream.to();
        int color = vivid(stream.color());
        int core = RuneDrawing.mix(color, 0xFFFFFF, 0.65F);
        float phase = (stream.seed() & 255) / 255F * Mth.TWO_PI;
        Vector3f eye = new Vector3f((float) cameraLocal.x, (float) cameraLocal.y, (float) cameraLocal.z);
        Vector3f right = camera.transform(new Vector3f(1, 0, 0));
        Vector3f up = camera.transform(new Vector3f(0, 1, 0));
        Vec3 along = to.subtract(from);
        Vec3 sideA = new Vec3(-along.z, 0, along.x);
        sideA = sideA.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : sideA.normalize();
        Vec3 sideB = sideA.cross(along.normalize()).normalize();
        Vec3 sideAf = sideA;
        float headF = head;
        float tailF = tail;

        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            // The braid: each strand winds around the arc, thickest in the middle, pinched at the jar and the machine.
            Vec3[] points = new Vec3[SEGMENTS + 1];
            float[] light = new float[SEGMENTS + 1];
            for (int strand = 0; strand < STRANDS; strand++) {
                float offset = strand * Mth.TWO_PI / STRANDS;
                for (int i = 0; i <= SEGMENTS; i++) {
                    float t = Mth.lerp(i / (float) SEGMENTS, tailF, headF);
                    float radius = 0.025F + 0.075F * Mth.sin(t * Mth.PI);
                    float angle = t * 16 - time * 0.22F + offset + phase;
                    points[i] = JarReach.arc(from, to, t).add(sideAf.scale(Mth.cos(angle) * radius)).add(sideB.scale(Mth.sin(angle) * radius));
                    light[i] = ends(t, tailF, headF) * (0.3F + 0.7F * waves(t, time, phase));
                }
                strip(consumer, pose, points, light, eye, 0.07F, color, 0.55F);
                strip(consumer, pose, points, light, eye, 0.022F, core, 0.9F);
            }
            // Sparks riding the waves, twinkling.
            for (int k = 0; k < SPARKS; k++) {
                float t = frac(time / TRAVEL_TICKS + k / (float) SPARKS + phase);
                if (t < tailF || t > headF) {
                    continue;
                }
                float twinkle = 0.6F + 0.4F * Mth.sin(time * 0.9F + k * 2.3F);
                float angle = t * 16 - time * 0.22F + k * 2.1F + phase;
                Vec3 at = JarReach.arc(from, to, t).add(sideAf.scale(Mth.cos(angle) * 0.06F)).add(sideB.scale(Mth.sin(angle) * 0.06F));
                star(consumer, pose, at, right, up, (k % 2 == 0 ? 0.22F : 0.15F) * twinkle, time * 0.05F + k, color, core,
                        ends(t, tailF, headF));
            }
            // The light gathering over the jar and pouring into the machine.
            if (tailF < 0.05F) {
                float strength = 1 - tailF / 0.05F;
                star(consumer, pose, from.add(0, 0.06, 0), right, up, 0.35F + 0.08F * Mth.sin(time * 0.4F), -time * 0.03F, color, core,
                        0.7F * strength);
            }
            if (headF >= 0.98F) {
                float pulse = waves(1, time, phase);
                star(consumer, pose, to, right, up, 0.4F + 0.25F * pulse, time * 0.04F, color, core, (0.5F + 0.5F * pulse) * (1 - tailF));
            }
        });

        // Hex sigils: one lying on the jar's lid, one around the point where the essence pours in.
        if (tail < 0.05F) {
            float strength = (1 - tail / 0.05F) * Math.min(1, head * 4);
            poseStack.pushPose();
            poseStack.translate(from.x, from.y - 0.01, from.z);
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            poseStack.rotate(Axis.ZP.rotation(time * 0.05F + phase));
            HexFx.sigilRing(poseStack, collector, 0.26F, 6, color, 0.75F * strength);
            poseStack.popPose();
        }
        if (head >= 0.98F) {
            float pulse = waves(1, time, phase);
            poseStack.pushPose();
            poseStack.translate(to.x, to.y - 0.12, to.z);
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            poseStack.rotate(Axis.ZP.rotation(-time * 0.07F - phase));
            HexFx.sigilRing(poseStack, collector, 0.3F + 0.03F * pulse, 6, color, (0.55F + 0.4F * pulse) * (1 - tail));
            poseStack.popPose();
        }
    }

    /** Light of the waves running towards the machine at {@code t} (0-1 along the arc): 0 between them, 1 on a crest. */
    private static float waves(float t, float time, float phase) {
        float light = 0;
        for (int j = 0; j < WAVES; j++) {
            float crest = frac(time / TRAVEL_TICKS + j / (float) WAVES + phase);
            float d = Math.abs(t - crest);
            d = Math.min(d, 1 - d);
            light += (float) Math.exp(-d * d / 0.004);
        }
        return Math.min(1, light);
    }

    /** Thins the stream out at its ends and at the growing head / running-out tail. */
    private static float ends(float t, float tail, float head) {
        float a = tail <= 0 ? Math.min(1, t / 0.04F + 0.5F) : (t - tail) / 0.08F;
        float b = head >= 1 ? Math.min(1, (1 - t) / 0.04F + 0.5F) : (head - t) / 0.08F;
        return Mth.clamp(Math.min(a, b), 0, 1);
    }

    private static float frac(float x) {
        return x - Mth.floor(x);
    }

    /** Brightens a colour to full value, keeping its hue: dull essence colours would vanish as additive light. */
    private static int vivid(int rgb) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        int max = Math.max(1, Math.max(r, Math.max(g, b)));
        float scale = 255F / max;
        return Math.min(255, Math.round(r * scale)) << 16 | Math.min(255, Math.round(g * scale)) << 8 | Math.min(255, Math.round(b * scale));
    }

    /** A camera-facing strip of additive light through the points: full light on its centre line, none at its edges. */
    private static void strip(VertexConsumer consumer, PoseStack.Pose pose, Vec3[] points, float[] light, Vector3f eye, float width, int color,
                              float alpha) {
        Vector3f[] centre = new Vector3f[points.length];
        Vector3f[] across = new Vector3f[points.length];
        for (int i = 0; i < points.length; i++) {
            Vec3 prev = points[Math.max(0, i - 1)];
            Vec3 next = points[Math.min(points.length - 1, i + 1)];
            Vector3f dir = new Vector3f((float) (next.x - prev.x), (float) (next.y - prev.y), (float) (next.z - prev.z));
            centre[i] = new Vector3f((float) points[i].x, (float) points[i].y, (float) points[i].z);
            Vector3f side = dir.cross(new Vector3f(eye).sub(centre[i]));
            if (side.lengthSquared() < 1.0E-10F) {
                side.set(0, 1, 0);
            }
            across[i] = side.normalize(width / 2);
        }
        for (int i = 0; i < points.length - 1; i++) {
            int c0 = argb(color, alpha * light[i]);
            int c1 = argb(color, alpha * light[i + 1]);
            if ((c0 >>> 24) == 0 && (c1 >>> 24) == 0) {
                continue;
            }
            int e = argb(color, 0);
            for (int side = -1; side <= 1; side += 2) {
                Vector3f a0 = new Vector3f(across[i]).mul(side).add(centre[i]);
                Vector3f a1 = new Vector3f(across[i + 1]).mul(side).add(centre[i + 1]);
                vertex(consumer, pose, centre[i], c0);
                vertex(consumer, pose, a0, e);
                vertex(consumer, pose, a1, e);
                vertex(consumer, pose, centre[i + 1], c1);
            }
        }
    }

    /** A twinkling four-pointed star of light facing the camera: a soft disc and four thin rays, turned by {@code spin}. */
    private static void star(VertexConsumer consumer, PoseStack.Pose pose, Vec3 at, Vector3f right, Vector3f up, float size, float spin,
                             int color, int core, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        Vector3f c = new Vector3f((float) at.x, (float) at.y, (float) at.z);
        int centre = argb(color, alpha * 0.55F);
        int edge = argb(color, 0);
        int segments = 12;
        float disc = size * 0.35F;
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            vertex(consumer, pose, c, centre);
            vertex(consumer, pose, onPlane(c, right, up, a0, disc), edge);
            vertex(consumer, pose, onPlane(c, right, up, a1, disc), edge);
            vertex(consumer, pose, c, centre);
        }
        int ray = argb(core, alpha);
        for (int k = 0; k < 4; k++) {
            float a = spin + k * Mth.HALF_PI;
            float length = size * (k % 2 == 0 ? 0.5F : 0.36F);
            Vector3f tip = onPlane(c, right, up, a, length);
            Vector3f left = onPlane(c, right, up, a + Mth.HALF_PI, size * 0.045F);
            Vector3f rightSide = onPlane(c, right, up, a - Mth.HALF_PI, size * 0.045F);
            vertex(consumer, pose, c, ray);
            vertex(consumer, pose, left, edge);
            vertex(consumer, pose, tip, edge);
            vertex(consumer, pose, rightSide, edge);
        }
    }

    private static Vector3f onPlane(Vector3f centre, Vector3f right, Vector3f up, float angle, float radius) {
        return new Vector3f(right).mul(Mth.cos(angle) * radius).add(new Vector3f(up).mul(Mth.sin(angle) * radius)).add(centre);
    }

    private static int argb(int rgb, float alpha) {
        return (Math.round(Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vector3f at, int argb) {
        consumer.addVertex(pose, at.x, at.y, at.z).setColor(argb);
    }
}
