package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexWispBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * A hex wisp: a small cold flame of hextech light. A white-hot core flickers like a candle inside a blue glow and a wide
 * faint halo; three sparks circle it on a tilted ring and motes rise off it and fade. It bobs gently, clings under the
 * ceiling when there is one, and grows in when it appears. Each wisp keeps its own phase, so a lit tunnel never pulses
 * in step.
 */
public class HexWispRenderer implements BlockEntityRenderer<HexWispBlock.Glow, HexWispRenderer.State> {
    private static final int BLUE = 0x5CC8F2;
    private static final int DEEP = 0x2F86D8;
    private static final int WHITE = 0xE6F8FF;
    private static final int SPARKS = 3;
    private static final int MOTES = 5;
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
        state.ceiling = level != null && level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), net.minecraft.core.Direction.DOWN);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float t = state.time + state.phase;
        float grow = Mth.clamp(state.age / GROW, 0, 1);
        grow = 1 - (1 - grow) * (1 - grow);
        // A candle's flicker: a few unrelated waves.
        float flicker = 0.86F + 0.07F * Mth.sin(t * 0.9F) + 0.05F * Mth.sin(t * 2.3F + 1.3F) + 0.03F * Mth.sin(t * 5.7F + 2.1F);
        poseStack.pushPose();
        poseStack.translate(0.5, (state.ceiling ? 0.66 : 0.5) + Mth.sin(t * 0.07F) * 0.035F, 0.5);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 1.15F * grow, DEEP, 0.22F * flicker);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.55F * grow * flicker, BLUE, 0.5F);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.CORE, 0.2F * grow * flicker, WHITE, 0.95F);

        List<float[]> sparks = new ArrayList<>(SPARKS);
        for (int i = 0; i < SPARKS; i++) {
            float angle = t * 0.08F + i * Mth.TWO_PI / SPARKS;
            float radius = 0.2F * grow;
            float y = Mth.sin(angle + 0.8F) * 0.07F;
            float twinkle = 0.55F + 0.45F * Mth.sin(t * 0.35F + i * 2.1F);
            int frame = Math.floorMod((int) (t / 3) + i, HexFx.GLINT_FRAMES);
            sparks.add(HexFx.quad(Mth.cos(angle) * radius, y, Mth.sin(angle) * radius, 0.045F * grow, frame, angle, 0.9F * twinkle,
                    i == 0 ? WHITE : BLUE));
        }
        HexFx.sheet(poseStack, collector, camera.orientation, HexFx.GLINT, HexFx.GLINT_FRAMES, sparks);

        List<float[]> motes = new ArrayList<>(MOTES);
        for (int i = 0; i < MOTES; i++) {
            // Each mote rises over 50 ticks, swaying, and fades out at the top.
            float rise = (t / 50 + (float) i / MOTES) % 1;
            float sway = t * 0.05F + i * 1.7F;
            float alpha = Mth.sin(rise * Mth.PI) * 0.75F * grow;
            motes.add(HexFx.quad(Mth.cos(sway) * 0.06F * (1 + rise), rise * 0.38F, Mth.sin(sway) * 0.06F * (1 + rise),
                    0.028F * (1 - rise * 0.5F), 0, 0, alpha, rise < 0.3F ? WHITE : BLUE));
        }
        HexFx.sheet(poseStack, collector, camera.orientation, HexFx.MOTE, 1, motes);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        float time;
        float age;
        float phase;
        boolean ceiling;
    }
}
