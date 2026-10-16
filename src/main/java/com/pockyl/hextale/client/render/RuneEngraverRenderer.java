package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.LightCoordsUtil;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.RuneEngraverBlock;
import com.pockyl.hextale.block.RuneEngraverBlockEntity;

/** The tool clamped in the Rune Engraver's vise, lying flat between the jaws; it flares up right after an engraving. */
public class RuneEngraverRenderer implements BlockEntityRenderer<RuneEngraverBlockEntity, RuneEngraverRenderer.State> {
    private static final int FLASH_TICKS = 16;
    private final ItemModelResolver itemModelResolver;

    public RuneEngraverRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RuneEngraverBlockEntity engraver, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(engraver, state, partialTicks, cameraPosition, breakProgress);
        state.facing = engraver.getBlockState().getValue(RuneEngraverBlock.FACING);
        itemModelResolver.updateForTopItem(state.tool, engraver.tool(), ItemDisplayContext.FIXED, engraver.getLevel(), null,
                (int) engraver.getBlockPos().asLong());
        long since = engraver.getLevel() == null ? FLASH_TICKS : engraver.getLevel().getGameTime() - engraver.engravedAt();
        state.flash = since >= 0 && since < FLASH_TICKS;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.tool.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 13.25F / 16.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, -state.facing.toYRot());
        poseStack.rotateDegrees(Axis.XP, 90.0F);
        poseStack.rotateDegrees(Axis.ZP, 45.0F);
        poseStack.scale(0.55F, 0.55F, 0.55F);
        int light = state.flash ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords;
        state.tool.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState tool = new ItemStackRenderState();
        Direction facing = Direction.NORTH;
        boolean flash;
    }
}
