package com.pockyl.hextale.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.pockyl.hextale.port.client.MobRenderer;
import com.pockyl.hextale.port.client.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.VoidMite;

/** Draws a Void Mite crawling over the ground. */
public class VoidMiteRenderer extends MobRenderer<VoidMite, LivingEntityRenderState, VoidMiteModel> {
    public static final ResourceLocation TEXTURE = Hextale.id("textures/entity/void_mite.png");

    public VoidMiteRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidMiteModel(context.bakeLayer(VoidMiteModel.LAYER)), 0.25F);
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
