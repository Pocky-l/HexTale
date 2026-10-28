package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;

/**
 * A 26.x entity renderer (extract a render state, then submit it) running as a 1.21.1 immediate renderer.
 *
 * <p>port-of: net.minecraft.client.renderer.entity.EntityRenderer
 */
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> extends net.minecraft.client.renderer.entity.EntityRenderer<T> {
    protected EntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public abstract S createRenderState();

    public void extractRenderState(T entity, S state, float partialTick) {
        state.x = entity.getX(partialTick);
        state.y = entity.getY(partialTick);
        state.z = entity.getZ(partialTick);
        state.ageInTicks = entity.tickCount + partialTick;
        state.partialTick = partialTick;
    }

    public void submit(S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
    }

    protected AABB getBoundingBoxForCulling(T entity, float partialTick) {
        return entity.getBoundingBoxForCulling();
    }

    public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ, float partialTick) {
        if (!entity.shouldRender(camX, camY, camZ)) {
            return false;
        }
        if (!affectedByCulling(entity)) {
            return true;
        }
        return entity.noCulling || frustum.isVisible(getBoundingBoxForCulling(entity, partialTick).inflate(0.5));
    }

    protected boolean affectedByCulling(T entity) {
        return true;
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ) {
        return shouldRender(entity, frustum, camX, camY, camZ, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        S state = createRenderState();
        extractRenderState(entity, state, partialTick);
        state.lightCoords = light;
        submit(state, poseStack, new SubmitNodeCollector(buffers), CameraRenderState.current());
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
