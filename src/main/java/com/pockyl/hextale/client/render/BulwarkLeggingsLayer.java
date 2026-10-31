package com.pockyl.hextale.client.render;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Generated from art/models/equipment/bulwark_leggings.bbmodel by tools/blockbench/bb_armor_layer.py - edit the Blockbench project and regenerate,
 * never this file.
 */
public final class BulwarkLeggingsLayer {
    private BulwarkLeggingsLayer() {
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(18, 0).addBox(-4.0F, 11.0F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.75F))
                .texOffs(57, 6).addBox(-1.0F, 12.0F, -3.1F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.5F))
                .texOffs(43, 6).addBox(-1.5F, 5.0F, -3.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(54, 0).addBox(-2.1F, 4.5F, -3.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        rightLeg.addOrReplaceChild("tasset_r", CubeListBuilder.create()
                .texOffs(43, 0).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.1F, 1.0F, -2.5F, -0.1745F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("hip_plate_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.6F, 0.5F, 0.0F, 0.0F, 0.0F, 0.1745F));
        rightLeg.addOrReplaceChild("t3_knee_wing_r", CubeListBuilder.create()
                .texOffs(11, 0).addBox(0.0F, -2.0F, 0.0F, 0.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.2F, 8.0F, -2.0F, 0.0F, -0.2618F, 0.0F));
        rightLeg.addOrReplaceChild("t4_knee_spike_r", CubeListBuilder.create()
                .texOffs(52, 6).addBox(-0.5F, -3.0F, -0.6F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 7.0F, -3.0F, 0.5236F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("t4_hip_spike_r", CubeListBuilder.create()
                .texOffs(18, 7).addBox(-3.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.1F, 2.5F, 0.0F, 0.0F, 0.0F, 0.3491F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.5F)).mirror(false)
                .texOffs(43, 6).mirror().addBox(-1.5F, 5.0F, -3.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(54, 0).mirror().addBox(-1.9F, 4.5F, -3.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        leftLeg.addOrReplaceChild("tasset_l", CubeListBuilder.create()
                .texOffs(43, 0).mirror().addBox(-2.0F, 0.0F, -0.5F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.1F, 1.0F, -2.5F, -0.1745F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("hip_plate_l", CubeListBuilder.create()
                .texOffs(0, 0).mirror().addBox(-0.5F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.6F, 0.5F, 0.0F, 0.0F, 0.0F, -0.1745F));
        leftLeg.addOrReplaceChild("t3_knee_wing_l", CubeListBuilder.create()
                .texOffs(11, 0).mirror().addBox(0.0F, -2.0F, 0.0F, 0.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.2F, 8.0F, -2.0F, 0.0F, 0.2618F, 0.0F));
        leftLeg.addOrReplaceChild("t4_knee_spike_l", CubeListBuilder.create()
                .texOffs(52, 6).mirror().addBox(-0.5F, -3.0F, -0.6F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 7.0F, -3.0F, 0.5236F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("t4_hip_spike_l", CubeListBuilder.create()
                .texOffs(18, 7).mirror().addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.1F, 2.5F, 0.0F, 0.0F, 0.0F, -0.3491F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
