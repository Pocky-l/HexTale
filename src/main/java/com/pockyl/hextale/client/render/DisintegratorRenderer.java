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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.DisintegratorBlock;

/**
 * The thing being unmade hangs over the bowl's dark floor, turning faster and shrinking as it comes apart, wrapped in a
 * dim halo that deepens towards the end; the next one rises in its place.
 */
public class DisintegratorRenderer implements BlockEntityRenderer<DisintegratorBlock.Bowl, DisintegratorRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public DisintegratorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = ItemModelResolver.INSTANCE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DisintegratorBlock.Bowl bowl, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(bowl, state, partialTick, cameraPosition, breakProgress);
        Level level = bowl.getLevel();
        state.time = HexFx.time(level, partialTick);
        state.progress = Math.min(1, bowl.clientProgress + partialTick / bowl.pace());
        itemModelResolver.updateForTopItem(state.item, bowl.item(), ItemDisplayContext.GROUND, level, null, (int) bowl.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        float p = state.progress;
        // The first fifth it rises out of the floor, then it comes apart.
        float rise = Mth.clamp(p / 0.2F, 0, 1);
        float apart = Mth.clamp((p - 0.2F) / 0.8F, 0, 1);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.42 + 0.28 * rise + Mth.sin(state.time * 0.12F) * 0.02F, 0.5);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.55F + 0.25F * apart, 0x6A5A7A, 0.25F + 0.45F * apart);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.time * (3 + 14 * apart)));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(state.time * 0.2F) * 12 * apart));
        float scale = 0.6F * rise * (1 - 0.85F * apart * apart);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, -0.15, 0);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float time;
        float progress;
    }
}
