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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexFurnaceBlock;
import com.pockyl.hextale.block.HexFurnaceBlockEntity;
import com.pockyl.hextale.registry.ModItems;

/**
 * The Hex Furnace's heart: a hextech crystal floating between the four prongs of its crown, slowly turning in a cyan
 * halo. While the furnace burns the crystal spins up, the halo swells and sparks circle it.
 */
public class HexFurnaceRenderer implements BlockEntityRenderer<HexFurnaceBlockEntity, HexFurnaceRenderer.State> {
    private static final int HEXTECH = 0x5CC7F2;
    private final ItemModelResolver itemModelResolver;

    public HexFurnaceRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HexFurnaceBlockEntity furnace, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(furnace, state, partialTick, cameraPosition, breakProgress);
        Level level = furnace.getLevel();
        itemModelResolver.updateForTopItem(state.gem, new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), ItemDisplayContext.GROUND, level, null,
                (int) furnace.getBlockPos().asLong());
        state.time = HexFx.time(level, partialTick);
        state.lit = furnace.getBlockState().getValue(HexFurnaceBlock.LIT);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        float activity = state.lit ? 1 : 0;
        float bob = Mth.sin(time * 0.07F) * 0.02F;
        poseStack.pushPose();
        poseStack.translate(0.5, 15.5 / 16 + bob, 0.5);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.55F + 0.25F * activity, HEXTECH, 0.3F + 0.25F * activity);
        if (state.lit) {
            for (int i = 0; i < 4; i++) {
                float angle = time * 0.12F + i * Mth.HALF_PI;
                poseStack.pushPose();
                poseStack.translate(Mth.cos(angle) * 0.22F, Mth.sin(time * 0.2F + i) * 0.05F, Mth.sin(angle) * 0.22F);
                HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, 0.06F, RuneDrawing.lighten(HEXTECH, 0.4F), 0.8F);
                poseStack.popPose();
            }
        }
        poseStack.rotate(Axis.YP.rotationDegrees(time * (state.lit ? 4.5F : 1.2F)));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        poseStack.translate(0, -0.2, 0);
        state.gem.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HexFurnaceBlockEntity furnace) {
        return new AABB(furnace.getBlockPos()).expandTowards(0, 0.6, 0);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState gem = new ItemStackRenderState();
        float time;
        boolean lit;
    }
}
