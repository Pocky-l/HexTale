package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.thevoid.boss.Proof;

import java.util.UUID;

/**
 * The Proof: the player model with the model player's skin, armour and weapon, every joint a little wrong. Arms and legs
 * too long, the head turned past where a neck stops. Vanilla's own player model and layers do the rest, so a skin, a cape
 * of armour trim and whatever a pack adds to armour rendering all come along.
 *
 * <p>1.21.1: the skin is read from the player list by the model player's id; one who left wears the default skin of that id.
 */
public class ProofRenderer extends HumanoidMobRenderer<Proof, ProofRenderer.WrongModel> {
    /** Blocks the longer legs lift it by. */
    private static final double LIFT = 0.17;
    private static final float SCALE = 0.9375F;

    public ProofRenderer(EntityRendererProvider.Context context) {
        super(context, new WrongModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(Proof proof) {
        UUID id = proof.profile();
        if (id == null) {
            return DefaultPlayerSkin.getDefaultTexture();
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(id);
        return info != null ? info.getSkin().texture() : DefaultPlayerSkin.get(id).texture();
    }

    @Override
    protected void scale(Proof proof, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public Vec3 getRenderOffset(Proof proof, float partialTick) {
        return super.getRenderOffset(proof, partialTick).add(0, LIFT, 0);
    }

    /** The joints that do not sit right. */
    public static final class WrongModel extends PlayerModel<Proof> {
        private static final float LIMB = 1.25F;
        private static final float NECK_TURN = 0.9F;

        WrongModel(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(Proof proof, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            rightArmPose = proof.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            leftArmPose = proof.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            super.setupAnim(proof, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            for (ModelPart limb : new ModelPart[]{rightArm, leftArm, rightLeg, leftLeg, rightSleeve, leftSleeve, rightPants, leftPants}) {
                limb.yScale = LIMB;
            }
            head.yRot += NECK_TURN * Mth.sin(ageInTicks * 0.05F);
            head.zRot += 0.25F;
            leftArm.zRot -= 0.2F;
            // 1.21.1: the second skin layer is not a child of what it covers.
            hat.copyFrom(head);
            leftSleeve.copyFrom(leftArm);
        }
    }
}
