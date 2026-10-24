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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.PedestalBlockEntity;
import com.pockyl.hextale.infusion.Ritual;

/**
 * A pedestal's item floats and turns over its hextech crystal. When the altar draws it, the item lifts, spins up and
 * shrinks into a flare over the pedestal for the length of one draw.
 */
public class PedestalRenderer implements BlockEntityRenderer<PedestalBlockEntity, PedestalRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public PedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = ItemModelResolver.INSTANCE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PedestalBlockEntity pedestal, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pedestal, state, partialTick, cameraPosition, breakProgress);
        Level level = pedestal.getLevel();
        state.time = HexFx.time(level, partialTick);
        state.sinceDraw = level == null ? -1 : level.getGameTime() - pedestal.drawnAt() + partialTick;
        state.seed = (int) pedestal.getBlockPos().asLong();
        itemModelResolver.updateForTopItem(state.item, pedestal.item(), ItemDisplayContext.GROUND, level, null, state.seed);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float draw = state.sinceDraw >= 0 && state.sinceDraw < Ritual.TICKS_PER_ITEM + 2 ? state.sinceDraw / Ritual.TICKS_PER_ITEM : 0;
        float time = state.time + (state.seed & 63);
        poseStack.pushPose();
        poseStack.translate(0.5, 1.12 + Mth.sin(time * 0.09F) * 0.035F + draw * 0.35F, 0.5);
        if (draw > 0) {
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.5F + 0.4F * draw, 0x9FE6FF, 0.6F * draw);
        }
        if (!state.item.isEmpty()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(time * (1.8F + 20 * draw)));
            float scale = 0.7F * (1 - 0.6F * draw * draw);
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0, -0.15, 0);
            state.item.submit(poseStack, collector, draw > 0 ? LightTexture.FULL_BRIGHT : state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float time;
        float sinceDraw;
        int seed;
    }
}
