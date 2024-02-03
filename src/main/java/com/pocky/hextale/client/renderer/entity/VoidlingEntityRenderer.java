package com.pocky.hextale.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pocky.hextale.client.model.entity.VoidlingEntityModel;
import com.pocky.hextale.common.entity.GeoVoidlingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VoidlingEntityRenderer extends GeoEntityRenderer<GeoVoidlingEntity> {

    public VoidlingEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new VoidlingEntityModel());
    }

    @Override
    public void render(GeoVoidlingEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.scale(0.5f, 0.5f, 0.5f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
