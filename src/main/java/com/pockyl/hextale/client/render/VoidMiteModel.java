package com.pockyl.hextale.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

import com.pockyl.hextale.Hextale;

/**
 * The Void Mite: an abdomen under a lilac shell plate, a small head with two mandibles, six spindly legs. Geometry from
 * the Blockbench project {@code art/models/entity/void_mite.bbmodel} (Modded Entity, exported with Mojang mappings);
 * the legs scuttle in two alternating tripods while it walks, and twitch a little while it waits.
 */
public class VoidMiteModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Hextale.id("void_mite"), "main");
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[6];

    public VoidMiteModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        for (int i = 0; i < 3; i++) {
            legs[i] = body.getChild("right_leg_" + i);
            legs[i + 3] = body.getChild("left_leg_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -2.0F, -1.0F, 5.0F, 3.0F, 6.0F)
                .texOffs(0, 9).addBox(-2.0F, -3.0F, 0.0F, 4.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 22.0F, 0.0F));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(16, 9).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 3.0F)
                .texOffs(28, 9).addBox(0.5F, 0.0F, -4.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(28, 11).addBox(-1.5F, 0.0F, -4.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -1.0F));
        float[] z = {-0.5F, 1.5F, 3.5F};
        float[] spread = {0.4363F, 0.0F, -0.4363F};
        int[][] uv = {{0, 14}, {10, 14}, {20, 14}, {0, 16}, {10, 16}, {20, 16}};
        for (int i = 0; i < 3; i++) {
            body.addOrReplaceChild("right_leg_" + i, CubeListBuilder.create().texOffs(uv[i][0], uv[i][1])
                    .addBox(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(2.5F, 0.0F, z[i], 0.0F, spread[i], 0.5236F));
            body.addOrReplaceChild("left_leg_" + i, CubeListBuilder.create().texOffs(uv[i + 3][0], uv[i + 3][1])
                    .addBox(-4.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.5F, 0.0F, z[i], 0.0F, -spread[i], -0.5236F));
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos * 1.8F;
        float amount = Math.min(1, state.walkAnimationSpeed * 1.5F);
        for (int i = 0; i < 6; i++) {
            // Two tripods: front and back of one side move with the middle leg of the other.
            float phase = walk + ((i % 3 == 1) == (i < 3) ? 0 : Mth.PI);
            float side = i < 3 ? 1 : -1;
            legs[i].yRot += Mth.cos(phase) * 0.45F * amount * side;
            legs[i].zRot += Math.max(0, Mth.sin(phase)) * 0.35F * amount * side;
            legs[i].zRot += Mth.sin(state.ageInTicks * 0.6F + i * 1.7F) * 0.04F * side;
        }
        head.yRot = Mth.sin(state.ageInTicks * 0.35F) * 0.12F;
    }
}
