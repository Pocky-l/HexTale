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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.XpCondenserBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.FunnelTimeline;

import java.util.ArrayList;
import java.util.List;

/**
 * The Experience Condenser: the experience it holds glows in the glass column as a green-gold liquid that rises and
 * falls smoothly with the fill, bubbles drifting up through it. Every drink ({@link XpCondenserBlockEntity.Shown}) is a
 * string of motes arcing from where the orb would have been into the crown on the cap.
 */
public class XpCondenserRenderer implements BlockEntityRenderer<XpCondenserBlockEntity, XpCondenserRenderer.State> {
    /** The inside of the glass (block units). */
    private static final float MIN = 4 / 16F;
    private static final float MAX = 12 / 16F;
    private static final float BOTTOM = 3 / 16F;
    private static final float TOP = 12 / 16F;
    public XpCondenserRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(XpCondenserBlockEntity condenser, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(condenser, state, partialTick, cameraPosition, breakProgress);
        Level level = condenser.getLevel();
        BlockPos pos = condenser.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        int seed = (int) pos.asLong();
        state.seed = seed;
        state.time = HexFx.time(level, partialTick) + (seed & 127);
        state.fill = Mth.lerp(partialTick, condenser.liquidBefore, condenser.liquid);
        state.intakeAt = XpCondenserBlockEntity.intake(pos).subtract(origin);
        float now = level == null ? 0 : level.getGameTime() + partialTick;
        state.motes.clear();
        for (XpCondenserBlockEntity.Shown stream : condenser.shown) {
            float age = now - stream.start();
            int count = FunnelTimeline.motes(stream.points());
            for (int i = 0; i < count; i++) {
                float t = (age - i * 1.2F) / 14F;
                if (t >= 0 && t <= 1) {
                    float angle = i * 2.1F + (stream.start() & 7);
                    Vec3 from = stream.from().subtract(origin).add(Mth.cos(angle) * 0.2, 0.1, Mth.sin(angle) * 0.2);
                    state.motes.add(JarReach.arc(from, state.intakeAt, FunnelTimeline.smooth(t)));
                }
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        if (state.fill > 0.002F) {
            float top = BOTTOM + (TOP - BOTTOM) * state.fill + Mth.sin(time * 0.1F) * 0.004F;
            LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, BOTTOM, top,
                    RuneDrawing.argb(FunnelTimeline.EXPERIENCE, 0.85F), time, state.seed, 3);
        }
        for (Vec3 mote : state.motes) {
            poseStack.pushPose();
            poseStack.translate(mote.x, mote.y, mote.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, 0.11F, FunnelTimeline.EXPERIENCE, 0.9F);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(XpCondenserBlockEntity condenser) {
        AABB box = new AABB(condenser.getBlockPos()).inflate(0.5).expandTowards(0, 0.6, 0);
        for (XpCondenserBlockEntity.Shown stream : condenser.shown) {
            box = box.minmax(AABB.ofSize(stream.from(), 1, 1, 1));
        }
        return box;
    }

    public static final class State extends BlockEntityRenderState {
        final List<Vec3> motes = new ArrayList<>();
        float time;
        float fill;
        int seed;
        Vec3 intakeAt = Vec3.ZERO;
    }
}
