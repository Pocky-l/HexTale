package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import com.pockyl.hextale.port.client.LivingEntityRenderState;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.registry.ModAttachments;

/**
 * The Void Mites latched onto a player, drawn where they hold on: on the back, the chest and the shoulders, small and
 * twitching their legs. One spot per mite, in a fixed order, so a player can count them.
 *
 * <p>1.21.1: a player render layer that reads the player directly (26.x extracts into the avatar render state).
 */
public class VoidMiteLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float SCALE = 0.5F;
    /** Where each mite holds on, in the body's space (blocks, y down): x, y, z and which way its back faces. */
    private static final float[][] SPOTS = {
            {0.1F, 0.22F, 0.13F, 1}, {-0.12F, 0.45F, 0.13F, 1}, {-0.1F, 0.25F, -0.13F, -1}, {0.11F, 0.5F, -0.13F, -1},
            {0.17F, 0.0F, 0.0F, 0}, {-0.17F, 0.0F, 0.0F, 0}, {0.0F, 0.62F, 0.13F, 1}, {0.02F, 0.38F, -0.13F, -1},
            {0.1F, 0.7F, 0.13F, 1}};
    private static final float[] YAWS = {20, -35, 160, 210, 90, -80, 5, 185, 40};
    private final VoidMiteModel model;
    private final LivingEntityRenderState mite = new LivingEntityRenderState();

    public VoidMiteLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer, VoidMiteModel model) {
        super(renderer);
        this.model = model;
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        VoidMiteModel model = new VoidMiteModel(event.getEntityModels().bakeLayer(VoidMiteModel.LAYER));
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new VoidMiteLayer(renderer, model));
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int lightCoords, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        int mites = player.getData(ModAttachments.VOID_MITES);
        if (mites <= 0 || player.isInvisible()) {
            return;
        }
        SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
        ModelPart body = getParentModel().body;
        for (int i = 0; i < Math.min(mites, SPOTS.length); i++) {
            float[] spot = SPOTS[i];
            poseStack.pushPose();
            body.translateAndRotate(poseStack);
            poseStack.translate(spot[0], spot[1], spot[2]);
            // Its back faces away from the body: out of the back (+z), the chest (-z) or up from a shoulder.
            if (spot[3] > 0) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            } else if (spot[3] < 0) {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(YAWS[i]));
            poseStack.scale(SCALE, SCALE, SCALE);
            poseStack.translate(0, -1.5F + 1 / 16F, 0);
            mite.ageInTicks = ageInTicks + i * 13;
            mite.walkAnimationPos = ageInTicks * 0.15F + i;
            mite.walkAnimationSpeed = 0.15F;
            collector.submitModel(model, mite, poseStack, VoidMiteRenderer.TEXTURE, lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
