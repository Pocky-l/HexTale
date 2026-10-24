package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.essence.JarReach;

/**
 * Essence drawn out of a jar into a machine ({@link JarReach}) as living liquid: a chain of glowing drops (the
 * essence wisp's frames, {@link #BLOB}) crawls along the arc from the jar's lid into the machine - each drop wobbling
 * out of line, changing shape and turning, the chain swelling and thinning like a pulse. Under it runs a faint
 * channel of additive light ({@link HexGlow}). Over the lid the essence gathers into a drop that bulges and lets the
 * chain go; at the machine the drops sink into a soft glow. The stream grows out of the jar when the draw starts
 * ({@code head} 0 -> 1) and runs into the machine when it ends ({@code tail} 0 -> 1). Wisps shed along the way are
 * particles ({@link JarReach#shed}). Positions are relative to the pose origin.
 */
public final class JarStreamFx {
    /** Eight frames of a living drop side by side (from the Krita particle art). */
    public static final ResourceLocation BLOB = Hextale.id("textures/misc/essence_blob.png");
    private static final int BLOB_FRAMES = 8;
    private static final int SEGMENTS = 32;
    /** Ticks a drop takes from the jar to the machine. */
    private static final float TRAVEL_TICKS = 46;
    /** Drops per block of arc. */
    private static final float DROPS_PER_BLOCK = 7;

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
        int color = liquid(stream.color());
        int glow = vivid(stream.color());
        float phase = (stream.seed() & 255) / 255F * Mth.TWO_PI;
        Vector3f eye = new Vector3f((float) cameraLocal.x, (float) cameraLocal.y, (float) cameraLocal.z);
        Vec3 along = to.subtract(from);
        Vec3 sideA = new Vec3(-along.z, 0, along.x);
        sideA = sideA.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : sideA.normalize();
        Vec3 sideB = sideA.cross(along.normalize()).normalize();
        float length = (float) along.length() + 1;

        // The channel: a faint band of light under the drops, brighter where the pulse runs.
        Vec3 sideAf = sideA;
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            Vec3[] points = new Vec3[SEGMENTS + 1];
            float[] light = new float[SEGMENTS + 1];
            for (int i = 0; i <= SEGMENTS; i++) {
                float t = Mth.lerp(i / (float) SEGMENTS, tail, head);
                points[i] = JarReach.arc(from, to, t).add(sideAf.scale(0.02F * Mth.sin(t * 9 - time * 0.15F + phase)));
                light[i] = ends(t, tail, head) * (0.45F + 0.55F * pulse(t, time, phase));
            }
            strip(consumer, pose, points, light, eye, 0.2F, glow, 0.16F);
            strip(consumer, pose, points, light, eye, 0.05F, glow, 0.3F);
        });

        // The drops: each rides the arc at its own pace, wobbling around it; the chain swells with the pulse.
        int drops = Math.max(5, Math.round(length * DROPS_PER_BLOCK));
        java.util.List<float[]> quads = new java.util.ArrayList<>(drops + 1);
        for (int k = 0; k < drops; k++) {
            float jitter = hash(stream.seed() * 31 + k);
            float t = frac(time / (TRAVEL_TICKS * length * 0.5F) + (k + jitter * 0.6F) / drops);
            if (t < tail || t > head) {
                continue;
            }
            float wobble = 0.045F + 0.035F * Mth.sin(t * Mth.PI);
            float angle = time * 0.13F + k * 2.4F + phase;
            Vec3 at = JarReach.arc(from, to, t).add(sideA.scale(Mth.cos(angle) * wobble)).add(sideB.scale(Mth.sin(angle * 1.3F) * wobble));
            float size = (0.065F + 0.05F * jitter) * (0.75F + 0.45F * pulse(t, time, phase)) * ends(t, tail, head)
                    * (0.7F + 0.3F * Mth.sin(t * Mth.PI));
            int frame = Math.floorMod((int) (time / 4) + k * 3, BLOB_FRAMES);
            quads.add(HexFx.quad(at.x, at.y, at.z, size, frame, angle * 0.4F, 0.92F, color));
        }
        // The drop gathering over the lid, swelling and letting the chain go.
        if (tail < 0.05F) {
            float strength = (1 - tail / 0.05F) * Math.min(1, head * 5);
            float swell = 0.13F + 0.035F * Mth.sin(time * 0.35F + phase);
            quads.add(HexFx.quad(from.x, from.y + 0.05, from.z, swell * strength, Math.floorMod((int) (time / 5), BLOB_FRAMES), time * 0.02F,
                    0.95F, color));
        }
        HexFx.sheet(poseStack, collector, camera, BLOB, BLOB_FRAMES, quads);
        if (head >= 0.98F) {
            float strength = 1 - tail;
            float beat = pulse(1, time, phase);
            poseStack.pushPose();
            poseStack.translate(to.x, to.y, to.z);
            HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.55F + 0.2F * beat, glow, (0.35F + 0.35F * beat) * strength);
            HexFx.sprite(poseStack, collector, camera, HexFx.MOTE, 0.16F + 0.06F * beat, RuneDrawing.mix(glow, 0xFFFFFF, 0.5F),
                    0.8F * strength);
            poseStack.popPose();
        }
        if (tail < 0.05F) {
            poseStack.pushPose();
            poseStack.translate(from.x, from.y + 0.04, from.z);
            HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.6F, glow, 0.3F * (1 - tail / 0.05F) * Math.min(1, head * 5));
            poseStack.popPose();
        }
    }

    /** The pulse running towards the machine at {@code t} (0-1 along the arc): 0 between beats, 1 on a beat. */
    private static float pulse(float t, float time, float phase) {
        float beat = frac(time / 30 + phase);
        float d = Math.abs(t - beat);
        d = Math.min(d, 1 - d);
        return (float) Math.exp(-d * d / 0.01);
    }

    /** A stable 0-1 number for a drop. */
    private static float hash(int seed) {
        int h = seed * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA77;
        h ^= h >>> 13;
        return (h & 0xFFFF) / 65535F;
    }

    /** The colour the drops are tinted: the essence's own colour lifted a little, so dark essences still read. */
    private static int liquid(int rgb) {
        return RuneDrawing.mix(rgb, vivid(rgb), 0.45F);
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
    static int vivid(int rgb) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        int max = Math.max(1, Math.max(r, Math.max(g, b)));
        float scale = 255F / max;
        return Math.min(255, Math.round(r * scale)) << 16 | Math.min(255, Math.round(g * scale)) << 8 | Math.min(255, Math.round(b * scale));
    }

    /** A camera-facing strip of additive light through the points: full light on its centre line, none at its edges. */
    static void strip(VertexConsumer consumer, PoseStack.Pose pose, Vec3[] points, float[] light, Vector3f eye, float width, int color,
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

    private static int argb(int rgb, float alpha) {
        return (Math.round(Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vector3f at, int argb) {
        consumer.addVertex(pose, at.x, at.y, at.z).setColor(argb);
    }
}
