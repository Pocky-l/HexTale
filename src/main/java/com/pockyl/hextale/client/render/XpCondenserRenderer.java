package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.XpCondenserBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.FunnelTimeline;
import com.pockyl.hextale.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * The Experience Condenser: the experience it holds glows in the glass column as a green-gold liquid that rises and
 * falls smoothly with the fill, bubbles drifting up through it. A hextech crystal turns over the cap, its halo greener
 * the fuller the glass. Every drink ({@link XpCondenserBlockEntity.Shown}) is a string of motes arcing from where the
 * orb would have been into the crystal.
 */
public class XpCondenserRenderer implements BlockEntityRenderer<XpCondenserBlockEntity, XpCondenserRenderer.State> {
    /** The inside of the glass (block units). */
    private static final float MIN = 3 / 16F;
    private static final float MAX = 13 / 16F;
    private static final float BOTTOM = 2 / 16F;
    private static final float TOP = 13 / 16F;
    private final ItemModelResolver itemModelResolver;

    public XpCondenserRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
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
        state.crystalAt = XpCondenserBlockEntity.crystal(pos).subtract(origin);
        itemModelResolver.updateForTopItem(state.crystal, new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), ItemDisplayContext.GROUND, level,
                null, seed);
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
                    state.motes.add(JarReach.arc(from, state.crystalAt, FunnelTimeline.smooth(t)));
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
        Vec3 crystal = state.crystalAt;
        poseStack.pushPose();
        poseStack.translate(crystal.x, crystal.y + Mth.sin(time * 0.07F) * 0.02F, crystal.z);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.4F + 0.3F * state.fill, FunnelTimeline.EXPERIENCE,
                0.2F + 0.45F * state.fill);
        poseStack.rotate(Axis.YP.rotationDegrees(time * 1.2F));
        poseStack.scale(0.32F, 0.32F, 0.32F);
        poseStack.translate(0, -0.2, 0);
        state.crystal.submit(poseStack, collector, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
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
        final ItemStackRenderState crystal = new ItemStackRenderState();
        final List<Vec3> motes = new ArrayList<>();
        float time;
        float fill;
        int seed;
        Vec3 crystalAt = Vec3.ZERO;
    }
}
