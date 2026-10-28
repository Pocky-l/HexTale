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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexWorkbenchBlock;
import com.pockyl.hextale.block.HexWorkbenchBlockEntity;

/**
 * The Hex Workbench's grid: the items lie flat in the nine cells carved into the worktop. A craft lights the carved
 * channels from the edge inwards, a hexagon flares in every filled cell and the light gathers into the middle and leaps
 * up; a crystal soaking into charge sinks into the worktop as a hexagon of light closing in on the middle.
 */
public class HexWorkbenchRenderer implements BlockEntityRenderer<HexWorkbenchBlockEntity, HexWorkbenchRenderer.State> {
    /** Cell pitch on the worktop (the carved grid has 4 px cells with 1 px channels). */
    private static final float CELL = 5 / 16.0F;
    private static final int CRAFT_TICKS = HexWorkbenchBlockEntity.CRAFT_SHOWN_TICKS;
    private static final float SOAK_TICKS = HexWorkbenchBlockEntity.SOAK_SHOWN_TICKS;
    /** The crystal's charge sinking in: pale hextech. */
    private static final int SOAK_COLOR = 0x9FE6FF;
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
        state.facing = bench.getBlockState().getValue(HexWorkbenchBlock.FACING);
        state.craft = level == null ? 0 : bench.flash(level, partialTick, CRAFT_TICKS);
        state.soak = level == null ? Float.MAX_VALUE : bench.sinceSoak(level, partialTick);
        state.time = HexFx.time(level, partialTick);
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
        if (state.craft > 0 || state.soak < SOAK_TICKS) {
            poseStack.pushPose();
            poseStack.translate(0, 1.006, 0);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            if (state.craft > 0) {
                craft(poseStack, collector, state);
            }
            if (state.soak < SOAK_TICKS) {
                float k = state.soak / SOAK_TICKS;
                poseStack.mulPose(Axis.ZP.rotation(state.time * 0.05F));
                HexFx.hexRing(poseStack, collector, 0.5F * (1 - k * k) + 0.05F, SOAK_COLOR, 0.8F * (1 - k));
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** The craft's light, in the worktop's plane: channels lit from the edge in, cells flaring, the middle leaping up. */
    private static void craft(PoseStack poseStack, SubmitNodeCollector collector, State state) {
        float age = (1 - state.craft) * CRAFT_TICKS;
        float run = Math.min(1, age / 6F);
        float fade = state.craft;
        int core = RuneDrawing.mix(HexFx.HEXTECH, 0xFFFFFF, 0.6F);
        float edge = 1.5F * CELL;
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            // The four channels between the cells, light running in from both ends.
            for (int k = -1; k <= 1; k += 2) {
                float c = k * CELL / 2;
                float reach = edge * (1 - run);
                for (int end = -1; end <= 1; end += 2) {
                    HexFx.glowSegment(consumer, pose, c, end * edge, c, end * reach, 0.06F, HexFx.HEXTECH, 0.5F * fade);
                    HexFx.glowSegment(consumer, pose, end * edge, c, end * reach, c, 0.06F, HexFx.HEXTECH, 0.5F * fade);
                    HexFx.glowSegment(consumer, pose, c, end * edge, c, end * reach, 0.018F, core, 0.9F * fade);
                    HexFx.glowSegment(consumer, pose, end * edge, c, end * reach, c, 0.018F, core, 0.9F * fade);
                }
            }
        });
        for (int i = 0; i < HexWorkbenchBlockEntity.GRID; i++) {
            if (state.cells[i].isEmpty()) {
                continue;
            }
            int col = i % 3;
            int row = i / 3;
            // Cells flare one after another, the corners last, then everything is pulled to the middle.
            float delay = (Math.abs(col - 1) + Math.abs(row - 1)) * 1.5F;
            float lit = Mth.clamp((age - delay) / 3F, 0, 1);
            float pull = Mth.clamp((age - 8) / 6F, 0, 1);
            poseStack.pushPose();
            poseStack.translate((1 - col) * CELL * (1 - pull), (1 - row) * CELL * (1 - pull), 0);
            HexFx.hexRing(poseStack, collector, 0.11F * (1 + 0.3F * lit), HexFx.HEXTECH, 0.9F * lit * fade);
            poseStack.popPose();
        }
        if (age > 12) {
            float leap = Mth.clamp((age - 12) / 10F, 0, 1);
            HexFx.hexRing(poseStack, collector, 0.15F + 0.45F * leap, core, (1 - leap));
        }
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState[] cells = new ItemStackRenderState[HexWorkbenchBlockEntity.GRID];
        Direction facing = Direction.NORTH;
        float craft;
        float soak = Float.MAX_VALUE;
        float time;

        public State() {
            for (int i = 0; i < cells.length; i++) {
                cells[i] = new ItemStackRenderState();
            }
        }
    }
}
