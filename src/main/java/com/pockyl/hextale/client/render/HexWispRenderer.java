package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.CameraRenderState;
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
 * tongues of flame one inside the other - a deep blue outer one, a bright blue middle and a white core - each a teardrop
 * that is brightest inside and fades to nothing at its edge. The flame stands upright and turns to the camera round the
 * vertical only; its tip sways and stretches like a candle's, embers rise off it and fade, and a faint round glow sits
 * behind it. It hangs under the ceiling when there is one, grows in when it appears, and every wisp keeps its own phase,
 * so a lit tunnel never flickers in step.
 */
public class HexWispRenderer implements BlockEntityRenderer<HexWispBlock.Glow, HexWispRenderer.State> {
    private static final int DEEP = 0x3A8CFF;
    private static final int BLUE = 0x5CC8F2;
    private static final int WHITE = 0xE8FAFF;
    private static final int EMBERS = 4;
    /** Ticks the flame takes to grow in. */
    private static final float GROW = 14;
    /** Points on each side of a tongue between its base and its tip. */
    private static final int SIDE = 7;
    /** Points round the rounded base of a tongue. */
    private static final int BASE = 7;

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
        state.ceiling = level != null && level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float t = state.time + state.phase;
        float grow = Mth.clamp(state.age / GROW, 0, 1);
        grow = 1 - (1 - grow) * (1 - grow);
        // A candle's flicker: a few unrelated waves for the height, slower ones for the sway.
        float flicker = 1 + 0.09F * Mth.sin(t * 0.83F) + 0.06F * Mth.sin(t * 2.1F + 1.3F) + 0.04F * Mth.sin(t * 4.7F + 2.2F);
        float sway = 0.035F * Mth.sin(t * 0.13F) + 0.02F * Mth.sin(t * 0.37F + 0.7F);
        float breath = 1 + 0.05F * Mth.sin(t * 0.21F + 0.4F);

        poseStack.pushPose();
        poseStack.translate(0.5, state.ceiling ? 0.36 : 0.3, 0.5);
        poseStack.scale(grow, grow, grow);
        // Upright: turned to the camera round the vertical only.
        Vector3f facing = camera.orientation.transform(new Vector3f(0, 0, 1));
        poseStack.mulPose(Axis.YP.rotation((float) Math.atan2(facing.x, facing.z)));
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            dot(consumer, pose, 0, 0.1F, 0.4F, BLUE, 0.28F);
            // Two side licks that sway against each other, then the flame itself, the bright body and the core.
            float lick = Mth.sin(t * 0.29F + 0.9F);
            tongue(consumer, pose, 0.07F, -0.07F, 0.24F * (1 + 0.25F * lick), sway - 0.05F - 0.03F * lick, 0.75F, DEEP, 0.45F);
            tongue(consumer, pose, 0.07F, 0.07F, 0.24F * (1 - 0.25F * lick), sway + 0.05F - 0.03F * lick, 0.75F, DEEP, 0.45F);
            tongue(consumer, pose, 0.16F * breath, 0, 0.44F * flicker, sway, 0.7F, DEEP, 0.85F);
            tongue(consumer, pose, 0.105F * breath, 0, 0.31F * flicker, sway * 0.75F, 0.75F, BLUE, 0.8F);
            tongue(consumer, pose, 0.055F, 0, 0.15F * (0.5F + 0.5F * flicker), sway * 0.4F, 0.8F, WHITE, 0.95F);
            for (int i = 0; i < EMBERS; i++) {
                // Each ember rises over 40 ticks from inside the flame, drifting with the sway, and fades at the top.
                float rise = (t / 40 + (float) i / EMBERS) % 1;
                float x = Mth.sin(t * 0.09F + i * 2.3F) * 0.05F + sway * (1 + rise * 2);
                float alpha = Mth.sin(rise * Mth.PI) * 0.9F;
                dot(consumer, pose, x, 0.12F + rise * 0.5F, 0.022F * (1 - rise * 0.4F), rise < 0.35F ? WHITE : BLUE, alpha);
            }
        });
        poseStack.popPose();
    }

    /**
     * One tongue of flame in the pose's XY plane: a rounded base of {@code radius} round {@code (x, 0)} and full sides
     * that narrow to a tip {@code height} up ({@code taper} below 1 keeps them round, above 1 hollows them), the tip
     * bent sideways by {@code sway} (the more, the higher). A fan from a point low inside it: full {@code alpha} there,
     * none at the edge.
     */
    private static void tongue(VertexConsumer consumer, PoseStack.Pose pose, float radius, float x, float height, float sway, float taper,
                               int color, float alpha) {
        int points = BASE + SIDE * 2 + 1;
        float[] xs = new float[points];
        float[] ys = new float[points];
        int n = 0;
        // Up the right side to the tip, down the left side, then round the base back to the start.
        for (int j = 0; j < SIDE; j++) {
            float s = (float) j / SIDE;
            xs[n] = x + radius * (float) Math.pow(1 - s, taper) + sway * s * s;
            ys[n++] = height * s;
        }
        xs[n] = x + sway;
        ys[n++] = height;
        for (int j = SIDE - 1; j >= 0; j--) {
            float s = (float) j / SIDE;
            xs[n] = x - radius * (float) Math.pow(1 - s, taper) + sway * s * s;
            ys[n++] = height * s;
        }
        for (int k = 1; k <= BASE; k++) {
            float angle = Mth.PI + Mth.PI * k / (BASE + 1);
            xs[n] = x + Mth.cos(angle) * radius;
            ys[n++] = Mth.sin(angle) * radius;
        }
        float cx = x + sway * 0.1F;
        float cy = height * 0.18F;
        int centre = argb(color, alpha);
        int edge = argb(color, 0);
        for (int i = 0; i < points; i++) {
            int next = (i + 1) % points;
            consumer.addVertex(pose, cx, cy, 0).setColor(centre);
            consumer.addVertex(pose, xs[i], ys[i], 0).setColor(edge);
            consumer.addVertex(pose, xs[next], ys[next], 0).setColor(edge);
            consumer.addVertex(pose, cx, cy, 0).setColor(centre);
        }
    }

    /** A round soft dot of light: full {@code alpha} in the middle, none at {@code radius}. */
    private static void dot(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float radius, int color, float alpha) {
        int centre = argb(color, alpha);
        int edge = argb(color, 0);
        int sides = 12;
        for (int k = 0; k < sides; k++) {
            float a0 = Mth.TWO_PI * k / sides;
            float a1 = Mth.TWO_PI * (k + 1) / sides;
            consumer.addVertex(pose, x, y, 0).setColor(centre);
            consumer.addVertex(pose, x + Mth.cos(a0) * radius, y + Mth.sin(a0) * radius, 0).setColor(edge);
            consumer.addVertex(pose, x + Mth.cos(a1) * radius, y + Mth.sin(a1) * radius, 0).setColor(edge);
            consumer.addVertex(pose, x, y, 0).setColor(centre);
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
    }
}
