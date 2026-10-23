package com.pockyl.hextale.client.render;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Generated from art/models/equipment/researcher_robe.bbmodel by tools/blockbench/bb_armor_layer.py - edit the Blockbench project and regenerate,
 * never this file.
 */
public final class ResearcherRobeLayer {
    private ResearcherRobeLayer() {
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.55F))
                .texOffs(110, 0).addBox(-2.5F, 0.5F, -3.05F, 2.0F, 11.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(110, 0).mirror().addBox(0.5F, 0.5F, -3.05F, 2.0F, 11.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(68, 22).addBox(-1.5F, 0.5F, -3.8F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(76, 22).addBox(-0.5F, 1.0F, -4.3F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(36, 22).addBox(-4.0F, 11.0F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.8F))
                .texOffs(60, 22).addBox(-1.5F, 10.5F, -3.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(80, 22).addBox(-4.0F, 11.5F, 2.7F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(90, 22).addBox(-4.0F, 11.5F, 2.7F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.2F))
                .texOffs(114, 22).addBox(1.8F, 12.4F, -3.9F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(118, 22).addBox(1.8F, 11.4F, -3.9F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.1F))
                .texOffs(62, 17).addBox(-5.0F, -0.5F, 2.6F, 10.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("collar_back", CubeListBuilder.create()
                .texOffs(62, 11).addBox(-5.0F, -3.0F, -0.5F, 10.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.5F, 4.7F, 0.1047F, 0.0F, 0.0F));
        body.addOrReplaceChild("collar_left", CubeListBuilder.create()
                .texOffs(94, 0).addBox(-0.5F, -3.0F, -1.8F, 1.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.7F, 0.5F, 0.0F, 0.0F, 0.0F, 0.1396F));
        body.addOrReplaceChild("collar_right", CubeListBuilder.create()
                .texOffs(94, 0).mirror().addBox(-0.5F, -3.0F, -1.8F, 1.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.7F, 0.5F, 0.0F, 0.0F, 0.0F, -0.1396F));
        body.addOrReplaceChild("cape_front_r", CubeListBuilder.create()
                .texOffs(84, 0).addBox(-2.2F, 0.0F, -0.5F, 4.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.8F, -0.5F, -2.8F, -0.0698F, 0.0F, 0.0F));
        body.addOrReplaceChild("cape_front_l", CubeListBuilder.create()
                .texOffs(84, 0).mirror().addBox(-1.8F, 0.0F, -0.5F, 4.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.8F, -0.5F, -2.8F, -0.0698F, 0.0F, 0.0F));
        body.addOrReplaceChild("cape_back", CubeListBuilder.create()
                .texOffs(62, 0).addBox(-5.0F, 0.0F, -0.5F, 10.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -0.5F, 2.8F, 0.0698F, 0.0F, 0.0F));
        body.addOrReplaceChild("scroll", CubeListBuilder.create()
                .texOffs(100, 22).addBox(-2.5F, -1.0F, -1.0F, 5.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, 12.5F, 3.9F, 0.0F, 0.0F, -0.2094F));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.55F))
                .texOffs(24, 16).addBox(-3.0F, 7.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.7F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("pauldron_r", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.8F, -0.5F, -2.8F, 5.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.1047F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.55F)).mirror(false)
                .texOffs(24, 16).mirror().addBox(-1.0F, 7.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.7F)).mirror(false),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("pauldron_l", CubeListBuilder.create()
                .texOffs(40, 0).mirror().addBox(-1.2F, -0.5F, -2.8F, 5.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.0F, -0.1047F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        rightLeg.addOrReplaceChild("skirt_r_front", CubeListBuilder.create()
                .texOffs(0, 17).addBox(-2.5F, 0.0F, -0.5F, 5.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.1F, 0.0F, -2.5F, -0.0873F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("skirt_r_side", CubeListBuilder.create()
                .texOffs(12, 17).addBox(-0.5F, 0.0F, -2.5F, 1.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0873F));
        rightLeg.addOrReplaceChild("skirt_r_back", CubeListBuilder.create()
                .texOffs(24, 22).addBox(-2.5F, 0.0F, -0.5F, 5.0F, 11.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.1F, 0.0F, 2.5F, 0.0873F, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        leftLeg.addOrReplaceChild("skirt_l_front", CubeListBuilder.create()
                .texOffs(0, 17).mirror().addBox(-2.5F, 0.0F, -0.5F, 5.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.1F, 0.0F, -2.6F, -0.0873F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("skirt_l_side", CubeListBuilder.create()
                .texOffs(12, 17).mirror().addBox(-0.5F, 0.0F, -2.5F, 1.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.0873F));
        leftLeg.addOrReplaceChild("skirt_l_back", CubeListBuilder.create()
                .texOffs(24, 22).mirror().addBox(-2.5F, 0.0F, -0.5F, 5.0F, 11.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.1F, 0.0F, 2.6F, 0.0873F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }
}
