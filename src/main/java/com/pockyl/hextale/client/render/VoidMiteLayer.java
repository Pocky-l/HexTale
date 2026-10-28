package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModAttachments;

/**
 * The Void Mites latched onto a player, drawn where they hold on: on the back, the chest and the shoulders, small and
 * twitching their legs. One spot per mite, in a fixed order, so a player can count them.
 */
public class VoidMiteLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final ContextKey<Integer> MITES = new ContextKey<>(Hextale.id("void_mites"));
    private static final float SCALE = 0.5F;
    /** Where each mite holds on, in the body's space (blocks, y down): x, y, z and which way its back faces. */
    private static final float[][] SPOTS = {
            {0.1F, 0.22F, 0.13F, 1}, {-0.12F, 0.45F, 0.13F, 1}, {-0.1F, 0.25F, -0.13F, -1}, {0.11F, 0.5F, -0.13F, -1},
            {0.17F, 0.0F, 0.0F, 0}, {-0.17F, 0.0F, 0.0F, 0}, {0.0F, 0.62F, 0.13F, 1}, {0.02F, 0.38F, -0.13F, -1},
            {0.1F, 0.7F, 0.13F, 1}};
    private static final float[] YAWS = {20, -35, 160, 210, 90, -80, 5, 185, 40};
    private final VoidMiteModel model;
    private final LivingEntityRenderState mite = new LivingEntityRenderState();

    public VoidMiteLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, VoidMiteModel model) {
        super(renderer);
        this.model = model;
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        VoidMiteModel model = new VoidMiteModel(event.getEntityModels().bakeLayer(VoidMiteModel.LAYER));
        for (var skin : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new VoidMiteLayer(renderer, model));
            }
        }
    }

    public static void registerModifier(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                int mites = avatar.getData(ModAttachments.VOID_MITES);
                state.setRenderData(MITES, mites > 0 ? mites : null);
            }
        });
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        Integer mites = state.getRenderData(MITES);
        if (mites == null || state.isInvisible) {
            return;
        }
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
            mite.ageInTicks = state.ageInTicks + i * 13;
            mite.walkAnimationPos = state.ageInTicks * 0.15F + i;
            mite.walkAnimationSpeed = 0.15F;
            collector.submitModel(model, mite, poseStack, VoidMiteRenderer.TEXTURE, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
            poseStack.popPose();
        }
    }
}
