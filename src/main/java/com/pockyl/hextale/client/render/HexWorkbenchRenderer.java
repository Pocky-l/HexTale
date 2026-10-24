package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.ItemModelResolver;
import com.pockyl.hextale.port.client.ItemStackRenderState;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexWorkbenchBlock;
import com.pockyl.hextale.block.HexWorkbenchBlockEntity;

/** The Hex Workbench's grid: the items lie flat in the nine cells carved into the worktop. */
public class HexWorkbenchRenderer implements BlockEntityRenderer<HexWorkbenchBlockEntity, HexWorkbenchRenderer.State> {
    /** Cell pitch on the worktop (the carved grid has 4 px cells with 1 px channels). */
    private static final float CELL = 5 / 16.0F;
    private final ItemModelResolver itemModelResolver;

    public HexWorkbenchRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = ItemModelResolver.INSTANCE;
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
        state.facing = bench.getBlockState().getValue(HexWorkbenchBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        TableModels.rotateToFacing(poseStack, state.facing);
        poseStack.translate(0.5, 0, 0.5);
        for (int i = 0; i < HexWorkbenchBlockEntity.GRID; i++) {
            if (state.cells[i].isEmpty()) {
                continue;
            }
            // Row 0 is the far side of the worktop, column 0 the worker's left.
            int col = i % 3;
            int row = i / 3;
            poseStack.pushPose();
            poseStack.translate((1 - col) * CELL, 1.0 + 0.012 + i * 0.0005, (1 - row) * CELL);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.scale(0.22F, 0.22F, 0.22F);
            state.cells[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] cells = new ItemStackRenderState[HexWorkbenchBlockEntity.GRID];
        Direction facing = Direction.NORTH;

        public State() {
            for (int i = 0; i < cells.length; i++) {
                cells[i] = new ItemStackRenderState();
            }
        }
    }
}
