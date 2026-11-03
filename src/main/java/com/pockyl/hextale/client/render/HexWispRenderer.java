package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexWispBlock;

/**
 * A hex wisp: a small magic flame of cold hextech fire, drawn as additive light ({@link HexGlow}) with no texture. Three
 * tongues of flame one inside the other - a deep blue outer one, a bright blue middle and a white core (a dyed wisp takes
 * its own three, {@link HexWispBlock.Flame}) - each a real teardrop body of revolution whose surface glows where it faces
 * the eye and fades at its rim, so the flame is round from every side, from above too. The outer tongues twist into
 * licks, the tip leans and wanders round like a candle's in a draught, embers rise off it on all sides and fade, and a
 * faint round glow sits behind it. It hangs under the ceiling when there is one, grows in when it appears, and every
 * wisp keeps its own phase, so a lit tunnel never flickers in step.
 */
public class HexWispRenderer implements BlockEntityRenderer<HexWispBlock.Glow, HexWispRenderer.State> {
    private static final int EMBERS = 4;
    /** Ticks the flame takes to grow in. */
    private static final float GROW = 14;

    public HexWispRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HexWispBlock.Glow wisp, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(wisp, state, partialTick, cameraPosition, breakProgress);
        Level level = wisp.getLevel();
        float time = HexFx.time(level, partialTick);
        if (wisp.clientBorn < 0 || wisp.clientBorn > time) {
            wisp.clientBorn = time;
        }
        BlockPos pos = wisp.getBlockPos();
        state.time = time;
        state.age = time - wisp.clientBorn;
        state.phase = (Mth.getSeed(pos) & 0xFFFF) / 65536.0F * 400;
        HexWispBlock.Flame flame = wisp.getBlockState().getBlock() instanceof HexWispBlock block ? block.flame() : HexWispBlock.Flame.HEXTECH;
        state.deep = flame.deep();
        state.middle = flame.middle();
        state.core = flame.core();
        state.eyeX = (float) (cameraPosition.x - pos.getX());
        state.eyeY = (float) (cameraPosition.y - pos.getY());
        state.eyeZ = (float) (cameraPosition.z - pos.getZ());
        state.distance = (float) Math.sqrt(state.eyeX * state.eyeX + state.eyeY * state.eyeY + state.eyeZ * state.eyeZ);
        state.ceiling = level != null && level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float t = state.time + state.phase;
        float grow = Mth.clamp(state.age / GROW, 0, 1);
        grow = Math.max(0.02F, 1 - (1 - grow) * (1 - grow));
        // A calm candle: slow unrelated waves for the height (nothing faster than about a second), slower ones for the lean.
        float flicker = 1 + 0.05F * Mth.sin(t * 0.23F) + 0.03F * Mth.sin(t * 0.41F + 1.3F) + 0.015F * Mth.sin(t * 0.67F + 2.2F);
        float leanX = 0.03F * Mth.sin(t * 0.07F) + 0.015F * Mth.sin(t * 0.19F + 0.7F);
        float leanZ = 0.03F * Mth.cos(t * 0.06F + 1.9F) + 0.015F * Mth.sin(t * 0.16F + 2.6F);
        float breath = 1 + 0.04F * Mth.sin(t * 0.11F + 0.4F);
        float baseY = state.ceiling ? 0.36F : 0.3F;
        // The eye in the flame's own frame (the pose only moves and scales it), for the glow's soft rim.
        Vector3f eye = new Vector3f(state.eyeX - 0.5F, state.eyeY - baseY, state.eyeZ - 0.5F).div(grow);
        // Far flames get fewer rings and sides.
        boolean near = state.distance < 20;
        int sides = near ? 16 : 9;
        int rings = near ? 12 : 7;
        Vector3f right = camera.orientation.transform(new Vector3f(1, 0, 0));
        Vector3f up = camera.orientation.transform(new Vector3f(0, 1, 0));

        poseStack.pushPose();
        poseStack.translate(0.5, baseY, 0.5);
        poseStack.scale(grow, grow, grow);
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            dot(consumer, pose, right, up, 0, 0.12F, 0, 0.42F, state.middle, 0.24F);
            // The outer flame licks and twists, the bright body less, the core hardly.
            tongue(consumer, pose, eye, sides, rings, 0.15F * breath, 0.46F * flicker, leanX, leanZ, 0.15F, t, 0.55F, state.deep, 0.62F);
            tongue(consumer, pose, eye, sides, rings, 0.1F * breath, 0.32F * flicker, leanX * 0.75F, leanZ * 0.75F, 0.08F, t + 40, 0.65F,
                    state.middle, 0.62F);
            tongue(consumer, pose, eye, sides, rings, 0.05F, 0.16F * (0.5F + 0.5F * flicker), leanX * 0.4F, leanZ * 0.4F, 0.04F, t + 90, 0.8F,
                    state.core, 0.85F);
            dot(consumer, pose, right, up, leanX * 0.1F, 0.07F, leanZ * 0.1F, 0.07F, state.core, 0.5F);
            for (int i = 0; i < EMBERS; i++) {
                // Each ember rises over 40 ticks from inside the flame on its own side, drifting with the lean, and fades.
                float cycle = t / 40 + (float) i / EMBERS;
                float rise = cycle % 1;
                float side = i * 2.4F + Mth.floor(cycle) * 1.7F;
                float drift = 0.03F + rise * 0.05F;
                float x = Mth.cos(side) * drift + leanX * (1 + rise * 2);
                float z = Mth.sin(side) * drift + leanZ * (1 + rise * 2);
                float alpha = Mth.sin(rise * Mth.PI) * 0.9F;
                dot(consumer, pose, right, up, x, 0.12F + rise * 0.5F, z, 0.022F * (1 - rise * 0.4F), rise < 0.35F ? state.core : state.middle, alpha);
            }
        });
        poseStack.popPose();
    }

    /**
     * One tongue of flame as a body of revolution: a rounded bottom {@code radius * 0.6} under the base, the widest ring a
     * quarter of the way up, then sides narrowing to a tip {@code height} up ({@code taper} below 1 keeps them full), bent
     * by the lean (the more, the higher). {@code licks} twists the upper part into three tongues that turn with time. Drawn
     * as additive light: each point shines with {@code alpha} where the surface faces the eye and fades to nothing where
     * it turns away, so from any side it reads as a glowing volume with a soft edge, brighter where it is thicker.
     */
    private static void tongue(VertexConsumer consumer, PoseStack.Pose pose, Vector3f eye, int sides, int rings, float radius, float height,
                               float leanX, float leanZ, float licks, float t, float taper, int color, float alpha) {
        float bottom = -radius * 0.6F;
        float[][] px = new float[rings + 1][sides];
        float[][] py = new float[rings + 1][sides];
        float[][] pz = new float[rings + 1][sides];
        for (int j = 0; j <= rings; j++) {
            float u = (float) j / rings;
            float shape = u < 0.25F ? Mth.sin(Mth.HALF_PI * u / 0.25F) : (float) Math.pow((1 - u) / 0.75F, taper);
            float y = Mth.lerp(u, bottom, height);
            float lean = Math.max(0, y) / height;
            float cx = leanX * lean * lean;
            float cz = leanZ * lean * lean;
            for (int k = 0; k < sides; k++) {
                float angle = Mth.TWO_PI * k / sides;
                float r = radius * shape * (1 + licks * u * Mth.sin(3 * angle + t * 0.05F + u * 4));
                px[j][k] = cx + Mth.cos(angle) * r;
                py[j][k] = y;
                pz[j][k] = cz + Mth.sin(angle) * r;
            }
        }
        int[][] colors = new int[rings + 1][sides];
        Vector3f normal = new Vector3f();
        Vector3f view = new Vector3f();
        for (int j = 0; j <= rings; j++) {
            float u = (float) j / rings;
            // The tip and the very bottom thin out.
            float fade = Mth.clamp((1 - u) * 3, 0, 1) * Mth.clamp(u * 6, 0, 1) * 0.7F + 0.3F * Mth.clamp((1 - u) * 1.5F, 0, 1);
            int below = Math.max(0, j - 1);
            int above = Math.min(rings, j + 1);
            for (int k = 0; k < sides; k++) {
                int prev = (k + sides - 1) % sides;
                int next = (k + 1) % sides;
                // Normal = (round the ring) x (up the side); its sign does not matter, only how square it faces the eye.
                float ax = px[j][next] - px[j][prev];
                float ay = py[j][next] - py[j][prev];
                float az = pz[j][next] - pz[j][prev];
                float bx = px[above][k] - px[below][k];
                float by = py[above][k] - py[below][k];
                float bz = pz[above][k] - pz[below][k];
                normal.set(ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx);
                view.set(eye.x - px[j][k], eye.y - py[j][k], eye.z - pz[j][k]);
                float facing = normal.lengthSquared() < 1.0E-12F || view.lengthSquared() < 1.0E-12F ? 1
                        : Math.abs(normal.normalize().dot(view.normalize()));
                colors[j][k] = argb(color, alpha * fade * facing * facing);
            }
        }
        for (int j = 0; j < rings; j++) {
            for (int k = 0; k < sides; k++) {
                int next = (k + 1) % sides;
                consumer.addVertex(pose, px[j][k], py[j][k], pz[j][k]).setColor(colors[j][k]);
                consumer.addVertex(pose, px[j][next], py[j][next], pz[j][next]).setColor(colors[j][next]);
                consumer.addVertex(pose, px[j + 1][next], py[j + 1][next], pz[j + 1][next]).setColor(colors[j + 1][next]);
                consumer.addVertex(pose, px[j + 1][k], py[j + 1][k], pz[j + 1][k]).setColor(colors[j + 1][k]);
            }
        }
    }

    /** A round soft dot of light facing the camera: full {@code alpha} in the middle, none at {@code radius}. */
    private static void dot(VertexConsumer consumer, PoseStack.Pose pose, Vector3f right, Vector3f up, float x, float y, float z, float radius,
                            int color, float alpha) {
        int centre = argb(color, alpha);
        int edge = argb(color, 0);
        int sides = 12;
        for (int k = 0; k < sides; k++) {
            float a0 = Mth.TWO_PI * k / sides;
            float a1 = Mth.TWO_PI * (k + 1) / sides;
            float c0 = Mth.cos(a0) * radius;
            float s0 = Mth.sin(a0) * radius;
            float c1 = Mth.cos(a1) * radius;
            float s1 = Mth.sin(a1) * radius;
            consumer.addVertex(pose, x, y, z).setColor(centre);
            consumer.addVertex(pose, x + right.x * c0 + up.x * s0, y + right.y * c0 + up.y * s0, z + right.z * c0 + up.z * s0).setColor(edge);
            consumer.addVertex(pose, x + right.x * c1 + up.x * s1, y + right.y * c1 + up.y * s1, z + right.z * c1 + up.z * s1).setColor(edge);
            consumer.addVertex(pose, x, y, z).setColor(centre);
        }
    }

    private static int argb(int rgb, float alpha) {
        return (Math.round(Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    public static final class State extends BlockEntityRenderState {
        float time;
        float age;
        float phase;
        boolean ceiling;
        int deep;
        int middle;
        int core;
        float eyeX;
        float eyeY;
        float eyeZ;
        float distance;
    }
}
