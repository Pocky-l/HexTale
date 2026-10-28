package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.ImpulseAim;
import com.pockyl.hextale.block.ImpulseArc;
import com.pockyl.hextale.block.ImpulsePlateBlock;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;

import java.util.ArrayList;
import java.util.List;

/**
 * The Impulse Plate: its launch panel snaps up on its front hinge when it fires and settles back; while a player aims it
 * (or holds the plate, or wears the researcher's goggles) the arc of its throw shows as a row of glowing dots running to
 * a marked ring on the target - red when the block under the crosshair is out of reach.
 */
public class ImpulsePlateRenderer implements BlockEntityRenderer<ImpulsePlateBlock.Plate, ImpulsePlateRenderer.State> {
    /** The panel's hinge on a north-facing plate (front edge), in blocks. */
    private static final float HINGE_Y = 2 / 16F;
    private static final float HINGE_Z = 2 / 16F;
    private static final float FLIP = 65;
    private static final int OUT_OF_REACH = 0xE0605A;
    /** Ticks the arc stays shown after the target changed. */
    private static final int SHOW_AFTER_AIM = 80;

    public ImpulsePlateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ImpulsePlateBlock.Plate plate, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(plate, state, partialTick, cameraPosition, breakProgress);
        Level level = plate.getLevel();
        state.facing = plate.getBlockState().getValue(ImpulsePlateBlock.FACING);
        state.time = HexFx.time(level, partialTick);
        float since = level == null ? 100 : level.getGameTime() - plate.launchedAt + partialTick;
        state.flip = since < 2 ? Mth.sin(since / 2 * Mth.HALF_PI) : since < 4 ? 1 : since < 14 ? 1 - ease((since - 4) / 10) : 0;
        state.arc.clear();
        state.target = null;
        LocalPlayer player = Minecraft.getInstance().player;
        if (level == null || player == null) {
            return;
        }
        BlockPos pos = plate.getBlockPos();
        BlockPos target = plate.target();
        state.reach = true;
        if (pos.equals(ImpulseAim.aiming(player))) {
            HitResult hit = Minecraft.getInstance().hitResult;
            if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK && !block.getBlockPos().equals(pos)) {
                target = block.getBlockPos();
                state.reach = plate.inRange(target);
            }
        } else {
            boolean shown = level.getGameTime() - plate.aimedAt < SHOW_AFTER_AIM
                    || player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.RESEARCHERS_GOGGLES.get())
                    || player.getMainHandItem().is(ModItems.IMPULSE_PLATE.get()) || player.getOffhandItem().is(ModItems.IMPULSE_PLATE.get());
            if (!shown) {
                target = null;
            }
        }
        if (target == null) {
            return;
        }
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        Vec3 from = new Vec3(pos.getX() + 0.5, pos.getY() + HINGE_Y, pos.getZ() + 0.5);
        Vec3 landing = ImpulseArc.landing(level, target);
        int ticks = ImpulseArc.ticks(landing.distanceTo(from), plate.runes().strength(com.pockyl.hextale.runework.ImpulseRune.MOTOR.rune()));
        for (Vec3 at : ImpulseArc.path(from, landing, ticks)) {
            state.arc.add(at.subtract(origin));
        }
        state.target = landing.subtract(origin);
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        TableModels.rotateToFacing(poseStack, state.facing);
        poseStack.translate(0, HINGE_Y, HINGE_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-FLIP * state.flip));
        poseStack.translate(0, -HINGE_Y, -HINGE_Z);
        TableModels.submit(poseStack, collector, TableModels.IMPULSE_PANEL, state.lightCoords);
        poseStack.popPose();
        if (state.target == null) {
            return;
        }
        int color = state.reach ? ModParticles.HEXTECH : OUT_OF_REACH;
        // Dots along the arc, a brighter wave running from the plate to the target.
        List<float[]> dots = new ArrayList<>();
        int count = state.arc.size();
        for (int i = 1; i < count - 1; i++) {
            Vec3 at = state.arc.get(i);
            float wave = 0.5F + 0.5F * Mth.sin(i * 0.6F - state.time * 0.35F);
            dots.add(HexFx.quad(at.x, at.y + 0.3, at.z, 0.07F + 0.04F * wave, 0, 0, 0.45F + 0.45F * wave, color));
        }
        HexFx.sheet(poseStack, collector, camera.orientation, HexFx.MOTE, 1, dots);
        // The target: a ring lying on it, turning, with a breathing inner ring.
        poseStack.pushPose();
        poseStack.translate(state.target.x, state.target.y + 0.03, state.target.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.time * 1.5F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        HexFx.sigilRing(poseStack, collector, 0.5F, 8, color, 0.8F);
        HexFx.sigilRing(poseStack, collector, 0.28F + 0.04F * Mth.sin(state.time * 0.2F), 0, color, 0.6F);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ImpulsePlateBlock.Plate plate) {
        // Aiming draws to wherever the crosshair is, so the box covers the plate's whole reach.
        return new AABB(plate.getBlockPos()).inflate(plate.range() + 2, ImpulsePlateBlock.REACH_Y + 2, plate.range() + 2);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        float time;
        float flip;
        boolean reach;
        final List<Vec3> arc = new ArrayList<>();
        @Nullable
        Vec3 target;
    }
}
