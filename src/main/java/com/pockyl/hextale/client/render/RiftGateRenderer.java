package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.RiftGateBlock;

/**
 * The surface of a Rift Gate's ring, in the stele's light ({@link HexGlow}, additive):
 * <ul>
 * <li>linked and paid: a slow whirl of rift light - four arms spiralling into a bright core, a hexagon turning at the
 * ring's lip and a smaller one turning against it inside, the light breathing;</li>
 * <li>linked but dry: the same whirl, dim and grey, stuttering;</li>
 * <li>the partner gone: no whirl, only the hexagon at the lip flickering in broken pieces;</li>
 * <li>a crossing (sent or arrived): the whirl flares, its lip opens out and the core flashes.</li>
 * </ul>
 * Drawn a hair in front of and behind the ring's membrane, so it shows from both sides without fighting it.
 */
public class RiftGateRenderer implements BlockEntityRenderer<RiftGateBlock.Gate, RiftGateRenderer.State> {
    /** The ring's opening: centred on the block's middle, 5 px to its inner lip. */
    private static final float LIP = 5 / 16F;
    private static final float CENTRE_Y = 0.5F;
    private static final int ARMS = 4;
    private static final int ARM_STEPS = 14;
    private static final float FLARE_TICKS = 16;
    private static final int DRY = 0x7A6E86;

    public RiftGateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RiftGateBlock.Gate gate, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(gate, state, partialTick, cameraPosition, breakProgress);
        Level level = gate.getLevel();
        state.time = HexFx.time(level, partialTick);
        state.facing = gate.getBlockState().getValue(RiftGateBlock.FACING);
        state.linked = gate.getBlockState().getValue(RiftGateBlock.LINKED);
        state.dry = gate.dry();
        state.forsaken = !state.linked && gate.link() != null;
        float since = level == null ? FLARE_TICKS : level.getGameTime() - gate.crossedAt + partialTick;
        state.flare = since >= 0 && since < FLARE_TICKS ? 1 - since / FLARE_TICKS : 0;
        state.seed = (int) gate.getBlockPos().asLong();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.linked && !state.forsaken) {
            return;
        }
        poseStack.pushPose();
        TableModels.rotateToFacing(poseStack, state.facing);
        for (float side : new float[] {0.52F, 0.48F}) {
            poseStack.pushPose();
            poseStack.translate(0.5, CENTRE_Y, side);
            if (state.forsaken) {
                broken(poseStack, collector, state);
            } else {
                whirl(poseStack, collector, state);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void whirl(PoseStack poseStack, SubmitNodeCollector collector, State state) {
        float time = state.time + (state.seed & 63);
        int color = state.dry ? DRY : RiftGateBlock.RIFT;
        int core = RuneDrawing.mix(color, 0xFFFFFF, state.dry ? 0.2F : 0.55F);
        float breathe = 0.8F + 0.2F * Mth.sin(time * 0.07F);
        // A dry gate stutters: its light catches and drops out.
        float stutter = state.dry ? (Mth.sin(time * 0.9F) > 0.2F ? 0.55F : 0.15F) : 1;
        float alpha = (state.dry ? 0.5F : 0.85F) * breathe * stutter + 0.5F * state.flare;
        float spin = time * (state.dry ? 0.01F : 0.035F) * (1 + 2 * state.flare);
        float lip = LIP * (1 + 0.15F * state.flare);
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            // Arms of light spiralling in from the lip, brightest where they sink into the core.
            for (int arm = 0; arm < ARMS; arm++) {
                float start = spin + arm * Mth.TWO_PI / ARMS;
                float px = 0;
                float py = 0;
                for (int i = 0; i <= ARM_STEPS; i++) {
                    float k = i / (float) ARM_STEPS;
                    float r = lip * 0.95F * (1 - k);
                    float a = start + k * 2.4F;
                    float x = Mth.cos(a) * r;
                    float y = Mth.sin(a) * r;
                    if (i > 0) {
                        float light = alpha * (0.25F + 0.75F * k);
                        HexFx.glowSegment(consumer, pose, px, py, x, y, 0.06F + 0.03F * k, color, 0.45F * light);
                        HexFx.glowSegment(consumer, pose, px, py, x, y, 0.016F, core, 0.8F * light);
                    }
                    px = x;
                    py = y;
                }
            }
        });
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation(-spin * 0.6F));
        HexFx.hexRing(poseStack, collector, lip, color, 0.7F * alpha);
        poseStack.mulPose(Axis.ZP.rotation(spin * 1.6F + Mth.PI / 6));
        HexFx.hexRing(poseStack, collector, lip * 0.55F, color, 0.4F * alpha);
        poseStack.popPose();
        // The core: a small bright hexagon, flashing on a crossing.
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation(spin * 2));
        HexFx.hexRing(poseStack, collector, 0.04F + 0.06F * state.flare, core, Math.min(1, alpha + state.flare));
        poseStack.popPose();
    }

    /** The partner is gone: only pieces of the lip's hexagon flicker. */
    private static void broken(PoseStack poseStack, SubmitNodeCollector collector, State state) {
        float time = state.time + (state.seed & 63);
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            for (int i = 0; i < 6; i++) {
                float flicker = Mth.sin(time * 1.3F + i * 2.7F);
                if (flicker < 0.55F) {
                    continue;
                }
                float a0 = Mth.TWO_PI * i / 6;
                float a1 = Mth.TWO_PI * (i + 1) / 6;
                HexFx.glowSegment(consumer, pose, Mth.cos(a0) * LIP, Mth.sin(a0) * LIP, Mth.cos(a1) * LIP, Mth.sin(a1) * LIP, 0.05F,
                        RiftGateBlock.RIFT, 0.6F * (flicker - 0.55F) / 0.45F);
            }
        });
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return false;
    }

    public static final class State extends BlockEntityRenderState {
        float time;
        Direction facing = Direction.NORTH;
        boolean linked;
        boolean dry;
        boolean forsaken;
        float flare;
        int seed;
    }
}
