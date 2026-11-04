package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.block.ChemBurnerBlock;
import com.pockyl.hextale.block.ChemVatBlock;
import com.pockyl.hextale.block.ChemVatBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.multiblock.MultiblockCore;
import com.pockyl.hextale.multiblock.MultiblockPartBlockEntity;
import com.pockyl.hextale.multiblock.MultiblockPattern;
import com.pockyl.hextale.multiblock.Multiblock;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A formed Chem Vat, drawn from its valve: the blocks it was built of (formed parts are invisible), the burner under it
 * lit while the bath works, and the bath in its hollow middle - acid green turning violet with shimmer, its level
 * showing how full it is, bubbling while it works.
 */
public class ChemVatRenderer implements BlockEntityRenderer<ChemVatBlockEntity, ChemVatRenderer.State> {
    private static final float INSET = 0.04F;

    public ChemVatRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChemVatBlockEntity vat, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(vat, state, partialTick, cameraPosition, breakProgress);
        state.parts.clear();
        Level level = vat.getLevel();
        MultiblockCore.Formed formed = vat.formed();
        state.formed = level != null && formed != null && vat.getBlockState().getValue(ChemVatBlock.FORMED);
        if (!state.formed) {
            return;
        }
        BlockPos pos = vat.getBlockPos();
        state.burning = vat.burning();
        state.time = HexFx.time(level, partialTick);
        state.heartAt = Vec3.atLowerCornerOf(vat.heart().subtract(pos));
        int doses = vat.acid() + vat.shimmer();
        state.fill = doses / (2F * ChemVatBlockEntity.MAX_DOSES);
        state.colour = doses == 0 ? Brew.ACID.color()
                : PixelArt.mix(Brew.ACID.color(), Brew.REFINED_SHIMMER.color(), vat.shimmer() / (float) doses);
        Multiblock multiblock = Multiblocks.byId(formed.multiblock());
        if (multiblock == null) {
            return;
        }
        for (MultiblockPattern.Cell cell : multiblock.pattern().cells()) {
            BlockPos at = formed.placement().at(cell.x(), cell.y(), cell.z());
            if (at.equals(pos) || !(level.getBlockEntity(at) instanceof MultiblockPartBlockEntity part)) {
                continue;
            }
            BlockState original = part.original();
            if (original.isAir()) {
                continue;
            }
            if (original.hasProperty(ChemBurnerBlock.LIT)) {
                original = original.setValue(ChemBurnerBlock.LIT, state.burning);
            }
            state.parts.add(new Part(Vec3.atLowerCornerOf(at.subtract(pos)), original, net.minecraft.client.renderer.LevelRenderer.getLightColor(level, at)));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        for (Part part : state.parts) {
            poseStack.pushPose();
            poseStack.translate(part.offset.x, part.offset.y, part.offset.z);
            TableModels.submitState(poseStack, collector, part.state, part.light);
            poseStack.popPose();
        }
        if (state.fill > 0) {
            poseStack.pushPose();
            poseStack.translate(state.heartAt.x, state.heartAt.y, state.heartAt.z);
            float top = 0.05F + 0.9F * state.fill;
            LiquidBox.submit(poseStack, collector, INSET, INSET, 1 - INSET, 1 - INSET, INSET, top, 0xD0 << 24 | state.colour,
                    state.time, state.blockPos.asLong(), state.burning ? 4 : 0);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(ChemVatBlockEntity vat) {
        return new AABB(vat.heart()).inflate(1.5);
    }

    record Part(Vec3 offset, BlockState state, int light) {
    }

    public static final class State extends BlockEntityRenderState {
        boolean formed;
        boolean burning;
        float time;
        float fill;
        int colour;
        Vec3 heartAt = Vec3.ZERO;
        final List<Part> parts = new ArrayList<>();
    }
}
