package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import com.pockyl.hextale.transport.PylonFlight;

/**
 * A pylon's tendril of light: the magic stretching out of a gem to take hold of an item. Two strands twist around one
 * living line that sways like something reaching (the sway calms where the tendril is held taut), thick and bright at
 * its root, thinning to a fine tip; pulses of light run along it from the root to the tip while it reaches and back while
 * it pulls. Drawn as additive light ({@link HexGlow}) along a leg of a {@link PylonFlight}, between {@code from} and
 * {@code to} (0-1 along the leg); {@code rootAtEnd} says which end grows out of the gem.
 */
public final class TendrilFx {
    private static final int SEGMENTS = 36;

    private TendrilFx() {
    }

    /**
     * @param flow   +1: light runs towards the tip (reaching), -1: towards the root (pulling)
     * @param taut   0 = swaying freely, 1 = pulled straight (it holds something)
     * @param origin the pose origin in world space; positions are drawn relative to it
     */
    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera, Vec3 cameraLocal,
                              PylonFlight flight, int leg, float from, float to, boolean rootAtEnd, int rgb, float time, int seed,
                              float flow, float taut, Vec3 origin, float alpha) {
        if (Math.abs(to - from) < 0.002F || alpha <= 0.01F) {
            return;
        }
        int color = JarStreamFx.vivid(rgb);
        int core = RuneDrawing.mix(color, 0xFFFFFF, 0.7F);
        float phase = (seed & 255) / 255F * Mth.TWO_PI;
        Vector3f eye = new Vector3f((float) cameraLocal.x, (float) cameraLocal.y, (float) cameraLocal.z);
        Vec3 a = flight.path().get(leg);
        Vec3 b = flight.path().get(leg + 1);
        Vec3 along = b.subtract(a);
        Vec3 sideA = new Vec3(-along.z, 0, along.x);
        sideA = sideA.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : sideA.normalize();
        Vec3 sideB = sideA.cross(along.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : along.normalize()).normalize();
        Vec3 sA = sideA;
        Vec3 sB = sideB;
        float length = (float) along.length();
        float sway = (1 - taut) * Math.min(0.16F, 0.05F + 0.03F * length);

        // The spine: from the root (s = 0) to the tip (s = 1), swaying in two directions with waves running to the tip.
        Vec3[] spine = new Vec3[SEGMENTS + 1];
        float[] s = new float[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float k = i / (float) SEGMENTS;
            s[i] = k;
            float t = rootAtEnd ? Mth.lerp(k, to, from) : Mth.lerp(k, from, to);
            float bend = Mth.sin(k * Mth.PI) * sway;
            float w1 = Mth.sin(k * 7 - time * 0.35F + phase);
            float w2 = Mth.cos(k * 5 - time * 0.27F + phase * 1.7F);
            spine[i] = flight.curve(leg, t).subtract(origin).add(sA.scale(w1 * bend)).add(sB.scale(w2 * bend * 0.7));
        }

        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            Vec3[] points = new Vec3[SEGMENTS + 1];
            float[] light = new float[SEGMENTS + 1];
            for (int strand = 0; strand < 2; strand++) {
                float offset = strand * Mth.PI;
                for (int i = 0; i <= SEGMENTS; i++) {
                    float k = s[i];
                    float thickness = 0.045F * (1 - 0.75F * k);
                    float angle = k * 14 - time * 0.3F * flow + offset + phase;
                    points[i] = spine[i].add(sA.scale(Mth.cos(angle) * thickness)).add(sB.scale(Mth.sin(angle) * thickness));
                    light[i] = taper(k) * (0.35F + 0.65F * pulses(k, time, flow, phase));
                }
                JarStreamFx.strip(consumer, pose, points, light, eye, 0.075F, color, 0.5F * alpha);
                JarStreamFx.strip(consumer, pose, points, light, eye, 0.022F, core, 0.9F * alpha);
            }
            for (int i = 0; i <= SEGMENTS; i++) {
                light[i] = taper(s[i]);
            }
            JarStreamFx.strip(consumer, pose, spine, light, eye, 0.13F, color, 0.18F * alpha);
        });
        // The root glows where the tendril leaves the gem, the tip twinkles: a glint over a soft halo at each.
        Vec3 root = spine[0];
        Vec3 tip = spine[SEGMENTS];
        float flicker = 0.85F + 0.15F * Mth.sin(time * 0.6F + phase);
        HexFx.sheet(poseStack, collector, camera, HexFx.GLINT, HexFx.GLINT_FRAMES, java.util.List.of(
                HexFx.quad(root.x, root.y, root.z, 0.11F, 0, time * 0.04F + phase, 0.7F * alpha, core),
                HexFx.quad(tip.x, tip.y, tip.z, 0.085F * flicker, 1, -time * 0.07F, 0.95F * alpha, core)));
        for (Vec3 end : new Vec3[]{root, tip}) {
            poseStack.pushPose();
            poseStack.translate(end.x, end.y, end.z);
            HexFx.sprite(poseStack, collector, camera, HexFx.HALO, end == root ? 0.42F : 0.3F, color, 0.35F * alpha);
            poseStack.popPose();
        }
    }

    /** Bright at the root, thinning to the tip, never quite gone. */
    private static float taper(float k) {
        return 0.4F + 0.6F * (1 - k) * (1 - k);
    }

    /** Light of three pulses running along the tendril: to the tip ({@code flow} > 0) or to the root. */
    private static float pulses(float k, float time, float flow, float phase) {
        float light = 0;
        for (int j = 0; j < 3; j++) {
            float crest = frac(time * 0.06F * flow + j / 3F + phase);
            float d = Math.abs(k - crest);
            d = Math.min(d, 1 - d);
            light += (float) Math.exp(-d * d / 0.006);
        }
        return Math.min(1, light);
    }

    private static float frac(float x) {
        return x - Mth.floor(x);
    }
}
