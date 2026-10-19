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
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexWorkbenchBlock;
import com.pockyl.hextale.block.HexWorkbenchBlockEntity;

/**
 * The Hex Workbench at work: the grid's items lie on the worktop in their cells, and the crystal of its socket floats in
 * the iron clamp at the back, its halo as bright as the charge left. A craft flashes a turning sigil ring over the
 * worktop.
 */
public class HexWorkbenchRenderer implements BlockEntityRenderer<HexWorkbenchBlockEntity, HexWorkbenchRenderer.State> {
    private static final int HEXTECH = 0x5CC7F2;
    private static final float CELL = 0.17F;
    private static final int FLASH_TICKS = 16;
    private final ItemModelResolver itemModelResolver;

    public HexWorkbenchRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HexWorkbenchBlockEntity bench, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(bench, state, partialTick, cameraPosition, breakProgress);
        Level level = bench.getLevel();
        int seed = (int) bench.getBlockPos().asLong();
        for (int i = 0; i < HexWorkbenchBlockEntity.GRID; i++) {
            itemModelResolver.updateForTopItem(state.cells[i], bench.getItem(i), ItemDisplayContext.FIXED, level, null, seed + i);
        }
        ItemStack crystal = bench.getItem(HexWorkbenchBlockEntity.CRYSTAL);
        itemModelResolver.updateForTopItem(state.crystal, crystal, ItemDisplayContext.GROUND, level, null, seed);
        state.facing = bench.getBlockState().getValue(HexWorkbenchBlock.FACING);
        state.time = HexFx.time(level, partialTick);
        state.charge = Mth.clamp(bench.chargeAvailable() / (float) HexWorkbenchBlockEntity.GEMSTONE_CHARGE, 0, 1);
        state.flash = level == null ? 0 : bench.flash(level, partialTick, FLASH_TICKS);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        // The bench faces the worker: rows run from its back (row 0) to its front.
        poseStack.rotate(Axis.YP.rotationDegrees(-((state.facing.toYRot() + 180) % 360)));
        for (int i = 0; i < HexWorkbenchBlockEntity.GRID; i++) {
            if (state.cells[i].isEmpty()) {
                continue;
            }
            int col = i % 3;
            int row = i / 3;
            poseStack.pushPose();
            poseStack.translate((1 - col) * CELL, 1.0 + 0.012 + i * 0.0005, (1 - row) * CELL - 0.02);
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            poseStack.rotate(Axis.ZP.rotationDegrees(180));
            poseStack.scale(0.22F, 0.22F, 0.22F);
            state.cells[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (state.flash > 0) {
            poseStack.pushPose();
            poseStack.translate(0, 1.03, -0.02);
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            poseStack.rotate(Axis.ZP.rotationDegrees(state.time * 6));
            HexFx.sigilRing(poseStack, collector, 0.3F + 0.08F * (1 - state.flash), 6, HEXTECH, state.flash);
            poseStack.popPose();
        }
        poseStack.popPose();

        Vec3 at = HexWorkbenchBlockEntity.crystalPosition(net.minecraft.core.BlockPos.ZERO, stateFor(state.facing));
        poseStack.pushPose();
        poseStack.translate(at.x, at.y + Mth.sin(state.time * 0.08F) * 0.02F, at.z);
        float glow = state.charge + state.flash * 0.5F;
        if (glow > 0.01F) {
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.3F + 0.25F * glow, HEXTECH, Math.min(1, 0.15F + 0.5F * glow));
        }
        if (!state.crystal.isEmpty()) {
            poseStack.rotate(Axis.YP.rotationDegrees(state.time * (1.5F + 6 * state.flash)));
            poseStack.scale(0.4F, 0.4F, 0.4F);
            poseStack.translate(0, -0.2, 0);
            state.crystal.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        }
        poseStack.popPose();
    }

    private static net.minecraft.world.level.block.state.BlockState stateFor(Direction facing) {
        return com.pockyl.hextale.registry.ModBlocks.HEX_WORKBENCH.get().defaultBlockState().setValue(HexWorkbenchBlock.FACING, facing);
    }

    @Override
    public AABB getRenderBoundingBox(HexWorkbenchBlockEntity bench) {
        return new AABB(bench.getBlockPos()).expandTowards(0, 0.6, 0);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] cells = new ItemStackRenderState[HexWorkbenchBlockEntity.GRID];
        final ItemStackRenderState crystal = new ItemStackRenderState();
        Direction facing = Direction.NORTH;
        float time;
        float charge;
        float flash;

        public State() {
            for (int i = 0; i < cells.length; i++) {
                cells[i] = new ItemStackRenderState();
            }
        }
    }
}
