package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A 26.x block entity renderer (extract a render state, then submit it) running as a 1.21.1 immediate renderer:
 * {@link #render} does both in one go. The light 1.21.1 passes in replaces the extracted one (computed the same way).
 *
 * <p>port-of: net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 */
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState>
        extends net.minecraft.client.renderer.blockentity.BlockEntityRenderer<T> {
    S createRenderState();

    default void extractRenderState(T blockEntity, S state, float partialTick, Vec3 cameraPosition,
                                    ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(blockEntity, state, breakProgress);
    }

    void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera);

    @Override
    default void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        CameraRenderState camera = CameraRenderState.current();
        S state = createRenderState();
        extractRenderState(blockEntity, state, partialTick, camera.pos, null);
        state.lightCoords = light;
        submit(state, poseStack, new SubmitNodeCollector(buffers), camera);
    }

    default boolean shouldRenderOffScreen() {
        return false;
    }

    @Override
    default boolean shouldRenderOffScreen(T blockEntity) {
        return shouldRenderOffScreen();
    }
}
