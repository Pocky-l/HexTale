package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/**
 * Essence drawn out of a jar into a machine ({@link com.pockyl.hextale.essence.JarReach}): a glowing thread arching from
 * the jar's lid into the machine, with blobs of essence running along it and twisting around it, a small whirl over
 * the jar where the essence leaves it and a glow where it arrives. The thread grows out of the jar when the draw starts
 * ({@code head} 0 -> 1) and runs into the machine when it ends ({@code tail} 0 -> 1). Everything full bright, real
 * alpha (looks right with shader packs), positions relative to the pose origin.
 */
public final class JarStreamFx {
    private static final int SEGMENTS = 28;
    private static final int BLOBS = 9;
    /** Ticks a blob takes from the jar to the machine. */
    private static final float TRAVEL_TICKS = 26;

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
        double distance = from.distanceTo(to);
        Vec3 control = from.add(to).scale(0.5).add(0, 0.45 + 0.16 * distance, 0);
        Vec3 along = to.subtract(from);
        Vec3 side = new Vec3(-along.z, 0, along.x);
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        float phase = (stream.seed() & 255) / 255F;

        // The thread: a soft wide glow and a bright narrow core, gently swaying.
        Vec3[] points = new Vec3[SEGMENTS + 1];
        float[] fade = new float[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = Mth.lerp(i / (float) SEGMENTS, tail, head);
            float sway = Mth.sin(t * Mth.PI) * 0.035F * Mth.sin(t * 9 - time * 0.25F + phase * 6.28F);
            points[i] = curve(from, control, to, t).add(side.scale(sway)).add(0, sway * 0.6, 0);
            // Thin out at both ends, and at the growing head and the shrinking tail.
            fade[i] = Math.min(1, Math.min((t - tail) / 0.06F + (tail <= 0 ? 0.4F : 0), (head - t) / 0.06F + (head >= 1 ? 0.4F : 0)));
            fade[i] = Mth.clamp(fade[i], 0, 1);
        }
        Vector3f eye = new Vector3f((float) cameraLocal.x, (float) cameraLocal.y, (float) cameraLocal.z);
        int core = RuneDrawing.lighten(stream.color(), 0.45F);
        float pulse = 0.85F + 0.15F * Mth.sin(time * 0.3F + phase * 6.28F);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(HexFx.HALO), (pose, consumer) -> {
            ribbon(consumer, pose, points, fade, eye, 0.2F, stream.color(), 0.22F * pulse);
            ribbon(consumer, pose, points, fade, eye, 0.07F, core, 0.75F * pulse);
        });

        // Blobs of essence running along the thread, some twisting around it.
        for (int k = 0; k < BLOBS; k++) {
            float t = frac(time / TRAVEL_TICKS + k / (float) BLOBS + phase);
            if (t < tail || t > head) {
                continue;
            }
            float ends = Math.min(1, Math.min(t / 0.08F, (1 - t) / 0.06F));
            float twist = k % 3 == 0 ? 0.07F : 0;
            float angle = t * 14 + time * 0.35F + k;
            Vec3 at = curve(from, control, to, t).add(side.scale(Mth.cos(angle) * twist)).add(0, Mth.sin(angle) * twist, 0);
            float size = (k % 3 == 0 ? 0.07F : 0.1F) * (0.85F + 0.25F * Mth.sin(time * 0.5F + k * 1.7F));
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            HexFx.sprite(poseStack, collector, camera, HexFx.HALO, size * 3.2F, stream.color(), 0.3F * ends);
            HexFx.sprite(poseStack, collector, camera, HexFx.MOTE, size, core, 0.95F * ends);
            poseStack.popPose();
        }

        // The whirl over the jar while the essence still leaves it.
        if (tail < 0.05F) {
            float strength = 1 - tail / 0.05F;
            poseStack.pushPose();
            poseStack.translate(from.x, from.y, from.z);
            HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.45F, stream.color(), 0.25F * strength * pulse);
            for (int k = 0; k < 5; k++) {
                float rise = frac(time / 18 + k / 5F + phase);
                float angle = time * 0.45F + k * Mth.TWO_PI / 5 + rise * 4;
                float radius = 0.2F * (1 - rise);
                poseStack.pushPose();
                poseStack.translate(Mth.cos(angle) * radius, rise * 0.3F - 0.06F, Mth.sin(angle) * radius);
                HexFx.sprite(poseStack, collector, camera, HexFx.MOTE, 0.06F, core, 0.9F * strength * Math.min(1, (1 - rise) * 2));
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        // A glow where it pours into the machine, once the thread has reached it.
        if (head >= 1) {
            poseStack.pushPose();
            poseStack.translate(to.x, to.y, to.z);
            HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.5F + 0.1F * Mth.sin(time * 0.6F), stream.color(), 0.3F * pulse);
            poseStack.popPose();
        }
    }

    private static Vec3 curve(Vec3 a, Vec3 c, Vec3 b, float t) {
        float u = 1 - t;
        return a.scale(u * u).add(c.scale(2 * u * t)).add(b.scale(t * t));
    }

    private static float frac(float x) {
        return x - Mth.floor(x);
    }

    /** A camera-facing strip through the points, its alpha scaled per point; across it the halo texture's soft profile. */
    private static void ribbon(VertexConsumer consumer, PoseStack.Pose pose, Vec3[] points, float[] fade, Vector3f eye, float width, int color,
                               float alpha) {
        Vector3f[] left = new Vector3f[points.length];
        Vector3f[] right = new Vector3f[points.length];
        for (int i = 0; i < points.length; i++) {
            Vec3 prev = points[Math.max(0, i - 1)];
            Vec3 next = points[Math.min(points.length - 1, i + 1)];
            Vector3f dir = new Vector3f((float) (next.x - prev.x), (float) (next.y - prev.y), (float) (next.z - prev.z));
            Vector3f p = new Vector3f((float) points[i].x, (float) points[i].y, (float) points[i].z);
            Vector3f toEye = new Vector3f(eye).sub(p);
            Vector3f across = dir.cross(toEye);
            if (across.lengthSquared() < 1.0E-8F) {
                across.set(0, 1, 0);
            }
            across.normalize(width / 2);
            left[i] = new Vector3f(p).add(across);
            right[i] = new Vector3f(p).sub(across);
        }
        for (int i = 0; i < points.length - 1; i++) {
            float a0 = alpha * fade[i];
            float a1 = alpha * fade[i + 1];
            if (a0 <= 0.001F && a1 <= 0.001F) {
                continue;
            }
            // Both windings: the type culls back faces.
            vertex(consumer, pose, left[i], 0, a0, color);
            vertex(consumer, pose, right[i], 1, a0, color);
            vertex(consumer, pose, right[i + 1], 1, a1, color);
            vertex(consumer, pose, left[i + 1], 0, a1, color);
            vertex(consumer, pose, left[i + 1], 0, a1, color);
            vertex(consumer, pose, right[i + 1], 1, a1, color);
            vertex(consumer, pose, right[i], 1, a0, color);
            vertex(consumer, pose, left[i], 0, a0, color);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vector3f at, float u, float alpha, int color) {
        consumer.addVertex(pose, at.x, at.y, at.z).setColor(RuneDrawing.argb(color, alpha)).setUv(u, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }
}
