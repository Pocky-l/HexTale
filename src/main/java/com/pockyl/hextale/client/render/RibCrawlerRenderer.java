package com.pockyl.hextale.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.pockyl.hextale.port.client.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import com.pockyl.hextale.port.client.LivingEntityRenderState;
import net.minecraft.client.renderer.RenderType;
import com.pockyl.hextale.port.client.RenderTypes;
import net.minecraft.resources.ResourceLocation;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.realm.RibCrawler;

/**
 * Draws a Rib Crawler, its throat, jaw slit, segment seams and knees glowing in the dark. Hanging under a ceiling it is
 * drawn upside down, legs gripping the block above and its chitin back turned to whoever walks below - the colour of
 * the ribs around it.
 */
public class RibCrawlerRenderer extends MobRenderer<RibCrawler, RibCrawlerRenderer.State, RibCrawlerModel> {
    public static final ResourceLocation TEXTURE = Hextale.id("textures/entity/rib_crawler.png");
    private static final RenderType GLOW = RenderTypes.eyes(Hextale.id("textures/entity/rib_crawler_glow.png"));

    public static class State extends LivingEntityRenderState {
        /** Hangs upside down under a block. */
        public boolean clinging;
        /** 1 as it lets go, toward 0 while it falls; 0 once landed. */
        public float drop;
        /** 1 on landing, 0 once it has sprung open. */
        public float uncoil;
        /** 0 -> 1 through a bite. */
        public float bite;
        /** Which of its idles this crawler keeps, and its phase. */
        public int idleVariant;
    }

    public RibCrawlerRenderer(EntityRendererProvider.Context context) {
        super(context, new RibCrawlerModel(context.bakeLayer(RibCrawlerModel.LAYER)), 0.7F);
        addLayer(new Glow(this));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RibCrawler crawler, State state, float partialTick) {
        super.extractRenderState(crawler, state, partialTick);
        state.clinging = crawler.isClinging();
        state.drop = crawler.dropping(partialTick);
        state.uncoil = crawler.uncoiling(partialTick);
        state.bite = crawler.getAttackAnim(partialTick);
        state.idleVariant = crawler.getId();
    }

    /**
     * Vanilla's upside-down drawing (the one of named mobs) puts its feet against the block above the hitbox. (1.21.1:
     * that check is static, so the clinging crawler is turned over here the same way.)
     */
    @Override
    protected void setupRotations(RibCrawler crawler, com.mojang.blaze3d.vertex.PoseStack poseStack, float bob, float yBodyRot, float partialTick,
            float scale) {
        super.setupRotations(crawler, poseStack, bob, yBodyRot, partialTick, scale);
        if (crawler.isClinging() && crawler.deathTime <= 0 && !isEntityUpsideDown(crawler)) {
            poseStack.translate(0.0F, (crawler.getBbHeight() + 0.1F) / scale, 0.0F);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
        }
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(State state) {
        return TEXTURE;
    }

    private static final class Glow extends EyesLayer<RibCrawler, MobRenderer.Adapter<RibCrawler, State>> {
        Glow(RenderLayerParent<RibCrawler, MobRenderer.Adapter<RibCrawler, State>> renderer) {
            super(renderer);
        }

        @Override
        public RenderType renderType() {
            return GLOW;
        }
    }
}
