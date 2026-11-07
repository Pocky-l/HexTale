package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.thevoid.boss.Proof;

/**
 * The Proof: the player model with the model player's skin, armour and weapon, every joint a little wrong. Arms and legs
 * too long, the head turned past where a neck stops. Vanilla's own player model and layers do the rest, so a skin, a cape
 * of armour trim and whatever a pack adds to armour rendering all come along.
 */
public class ProofRenderer extends MobRenderer<Proof, AvatarRenderState, ProofRenderer.WrongModel> {
    /** Blocks the longer legs lift it by. */
    private static final double LIFT = 0.17;
    private static final float SCALE = 0.9375F;
    private final PlayerSkinRenderCache skins;

    public ProofRenderer(EntityRendererProvider.Context context) {
        super(context, new WrongModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.skins = context.getPlayerSkinRenderCache();
        addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), part -> new PlayerModel(part, false)),
                context.getEquipmentRenderer()));
        addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(Proof proof, AvatarRenderState state, float partialTick) {
        super.extractRenderState(proof, state, partialTick);
        HumanoidMobRenderer.extractHumanoidRenderState(proof, state, partialTick, itemModelResolver);
        state.skin = skins.getOrDefault(proof.profile()).playerSkin();
        boolean armed = !proof.getMainHandItem().isEmpty();
        state.rightArmPose = armed ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
        state.leftArmPose = proof.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        state.id = proof.getId();
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }

    @Override
    protected void scale(AvatarRenderState state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public Vec3 getRenderOffset(AvatarRenderState state) {
        return super.getRenderOffset(state).add(0, LIFT, 0);
    }

    /** The joints that do not sit right. */
    public static final class WrongModel extends PlayerModel {
        private static final float LIMB = 1.25F;
        private static final float NECK_TURN = 0.9F;

        WrongModel(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            rightArm.yScale = LIMB;
            leftArm.yScale = LIMB;
            rightLeg.yScale = LIMB;
            leftLeg.yScale = LIMB;
            head.yRot += NECK_TURN * Mth.sin(state.ageInTicks * 0.05F);
            head.zRot += 0.25F;
            leftArm.zRot -= 0.2F;
        }
    }
}
