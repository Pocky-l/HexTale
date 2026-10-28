package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.network.EssenceFxPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Essence on the move, from {@link EssenceFxPayload}, in the stele's light ({@link HexGlow}):
 * <ul>
 * <li>a pulse running through brass pipes - light shining out round the tube, brightest at its leading edge, from
 * the joint at the source to the joint at the receiver;</li>
 * <li>a pour: the jar stream's living liquid ({@link JarStreamFx}) from one point into another, growing out, flowing
 * and running in.</li>
 * </ul>
 */
public final class EssenceFx {
    /** Blocks a pulse runs per tick, and the length of its glow. */
    private static final float PULSE_SPEED = 0.45F;
    private static final float PULSE_LENGTH = 1.5F;
    private static final float GROW_TICKS = 8;
    private static final int MAX_SHOWN = 128;
    private static final int SAMPLES = 9;

    private static final List<Shown> SHOWN = new ArrayList<>();
    private static @Nullable ClientLevel level;

    /** One effect: a pulse's polyline (with its length) or a pour, from {@code start} (client game time). */
    record Shown(EssenceFxPayload payload, Vec3[] line, float length, long start, int seed) {
        float lasts() {
            return payload.kind() == EssenceFxPayload.PIPE ? (length + PULSE_LENGTH) / PULSE_SPEED + 1 : payload.ticks() + GROW_TICKS + 2;
        }
    }

    private EssenceFx() {
    }

    public static void accept(EssenceFxPayload payload) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null) {
            return;
        }
        if (current != level) {
            SHOWN.clear();
            level = current;
        }
        if (SHOWN.size() >= MAX_SHOWN) {
            SHOWN.remove(0);
        }
        Vec3[] line = payload.kind() == EssenceFxPayload.PIPE ? line(payload.path()) : new Vec3[0];
        float length = 0;
        for (int i = 1; i < line.length; i++) {
            length += (float) line[i].distanceTo(line[i - 1]);
        }
        SHOWN.add(new Shown(payload, line, length, current.getGameTime(), current.getRandom().nextInt()));
    }

    /** The pulse's way: from the joint of the source and the first pipe, through the pipes' centres, to the last joint. */
    private static Vec3[] line(List<BlockPos> path) {
        if (path.size() < 2) {
            return new Vec3[0];
        }
        List<Vec3> points = new ArrayList<>();
        points.add(Vec3.atCenterOf(path.get(0)).add(Vec3.atCenterOf(path.get(1))).scale(0.5));
        for (int i = 1; i < path.size() - 1; i++) {
            points.add(Vec3.atCenterOf(path.get(i)));
        }
        points.add(Vec3.atCenterOf(path.get(path.size() - 2)).add(Vec3.atCenterOf(path.getLast())).scale(0.5));
        return points.toArray(Vec3[]::new);
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        if (SHOWN.isEmpty()) {
            return;
        }
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null || current != level) {
            SHOWN.clear();
            return;
        }
        LevelRenderState state = event.getLevelRenderState();
        CameraRenderState camera = state.cameraRenderState;
        float now = state.gameTime + state.worldPartialTicks;
        SHOWN.removeIf(shown -> now - shown.start() > shown.lasts() || now < shown.start() - 1);
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (Shown shown : SHOWN) {
            float age = Math.max(0, now - shown.start());
            if (shown.payload().kind() == EssenceFxPayload.PIPE) {
                pulse(poseStack, collector, camera.pos, shown, age);
            } else {
                EssenceFxPayload payload = shown.payload();
                JarStreamFx.Stream stream = new JarStreamFx.Stream(payload.from().subtract(camera.pos), payload.to().subtract(camera.pos),
                        payload.color(), age / GROW_TICKS, (age - payload.ticks()) / GROW_TICKS, shown.seed());
                JarStreamFx.submit(poseStack, collector, camera.orientation, Vec3.ZERO, stream, now);
            }
        }
    }

    private static void pulse(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera, Shown shown, float age) {
        float head = age * PULSE_SPEED;
        float from = Math.max(0, head - PULSE_LENGTH);
        float to = Math.min(shown.length(), head);
        if (to <= from + 0.01F) {
            return;
        }
        Vec3[] points = new Vec3[SAMPLES];
        float[] light = new float[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            float k = i / (float) (SAMPLES - 1);
            float distance = from + (to - from) * k;
            points[i] = at(shown.line(), distance).subtract(camera);
            // Brightest at the leading edge, a tail fading behind it; a soft start and end at the joints.
            float behind = (head - distance) / PULSE_LENGTH;
            light[i] = Mth.clamp(1 - behind, 0, 1) * Mth.clamp(behind * 6, 0, 1) + (i == SAMPLES - 1 ? 0.2F : 0);
        }
        int color = JarStreamFx.vivid(shown.payload().color());
        int core = RuneDrawing.mix(color, 0xFFFFFF, 0.6F);
        Vector3f eye = new Vector3f();
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            // The pipe hides the centre line: the light that shows is the halo round the tube, so it is wide and strong.
            JarStreamFx.strip(consumer, pose, points, light, eye, 0.55F, color, 0.4F);
            JarStreamFx.strip(consumer, pose, points, light, eye, 0.28F, core, 0.5F);
        });
    }

    /** The point {@code distance} blocks along the polyline. */
    private static Vec3 at(Vec3[] line, float distance) {
        float left = distance;
        for (int i = 1; i < line.length; i++) {
            float segment = (float) line[i].distanceTo(line[i - 1]);
            if (left <= segment) {
                return line[i - 1].lerp(line[i], segment < 1.0E-4F ? 0 : left / segment);
            }
            left -= segment;
        }
        return line[line.length - 1];
    }
}
