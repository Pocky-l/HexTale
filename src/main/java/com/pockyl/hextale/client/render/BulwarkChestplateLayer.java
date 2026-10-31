package com.pockyl.hextale.client.render;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Generated from art/models/equipment/bulwark_chestplate.bbmodel by tools/blockbench/bb_armor_layer.py - edit the Blockbench project and regenerate,
 * never this file.
 */
public final class BulwarkChestplateLayer {
    private BulwarkChestplateLayer() {
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(1.0F))
                .texOffs(89, 0).addBox(-4.0F, 1.0F, -3.4F, 8.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(78, 9).addBox(-2.0F, 2.0F, -4.2F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(11, 10).addBox(-1.0F, 2.5F, -4.6F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(110, 14).addBox(-4.0F, 8.0F, -3.3F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(34, 0).addBox(-4.0F, 1.0F, 2.3F, 8.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(57, 28).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 1.0F, 6.0F, new CubeDeformation(0.4F))
                .texOffs(100, 8).addBox(-3.5F, 1.0F, -3.8F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(100, 8).mirror().addBox(2.5F, 1.0F, -3.8F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(76, 0).addBox(-3.0F, 0.0F, -3.5F, 6.0F, 8.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(14, 44).addBox(-1.5F, 1.5F, -4.8F, 3.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 44).addBox(-2.0F, 3.5F, -5.0F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("t3_gorget", CubeListBuilder.create()
                .texOffs(110, 8).addBox(-4.0F, -4.0F, -0.5F, 8.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 1.0F, 2.5F, -0.2618F, 0.0F, 0.0F));
        body.addOrReplaceChild("t4_dorsal_1", CubeListBuilder.create()
                .texOffs(18, 10).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 1.0F, 3.5F, 0.3491F, 0.0F, 0.0F));
        body.addOrReplaceChild("t4_dorsal_2", CubeListBuilder.create()
                .texOffs(25, 10).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 4.0F, 3.5F, 0.3491F, 0.0F, 0.0F));
        body.addOrReplaceChild("t4_claw_r", CubeListBuilder.create()
                .texOffs(105, 8).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.0F, 4.0F, -3.9F, 0.0F, 0.0F, -0.1745F));
        body.addOrReplaceChild("t4_claw_l", CubeListBuilder.create()
                .texOffs(105, 8).mirror().addBox(-0.5F, -2.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, 4.0F, -3.9F, 0.0F, 0.0F, 0.1745F));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F))
                .texOffs(108, 0).addBox(-3.0F, 7.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.5F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("pauldron_top_r", CubeListBuilder.create()
                .texOffs(57, 15).addBox(-6.5F, -1.0F, -3.5F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, -3.5F, 0.0F, 0.0F, 0.0F, -0.2618F));
        rightArm.addOrReplaceChild("pauldron_low_r", CubeListBuilder.create()
                .texOffs(86, 18).addBox(-4.5F, 0.5F, -3.5F, 5.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.5F, -1.5F, 0.0F, 0.0F, 0.0F, -0.5236F));
        rightArm.addOrReplaceChild("t2_fin_r", CubeListBuilder.create()
                .texOffs(89, 8).addBox(-0.5F, -3.0F, -2.0F, 1.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.0F, -3.5F, 0.0F, 0.0F, 0.0F, -0.2618F));
        rightArm.addOrReplaceChild("t3_wing_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, -6.0F, -2.0F, 0.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.0F, -2.0F, 1.0F, 0.0F, 0.0F, -0.3491F));
        rightArm.addOrReplaceChild("t4_spike_r1", CubeListBuilder.create()
                .texOffs(32, 10).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.5F, -3.0F, -1.0F, 0.0F, 0.0F, -0.3491F));
        rightArm.addOrReplaceChild("t4_spike_r2", CubeListBuilder.create()
                .texOffs(37, 10).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.5F, -2.5F, 1.0F, 0.2618F, 0.0F, -0.6109F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F)).mirror(false)
                .texOffs(108, 0).mirror().addBox(-1.0F, 7.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.5F)).mirror(false),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("pauldron_top_l", CubeListBuilder.create()
                .texOffs(57, 15).mirror().addBox(-0.5F, -1.0F, -3.5F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.0F, -3.5F, 0.0F, 0.0F, 0.0F, 0.2618F));
        leftArm.addOrReplaceChild("pauldron_low_l", CubeListBuilder.create()
                .texOffs(86, 18).mirror().addBox(-0.5F, 0.5F, -3.5F, 5.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.5F, -1.5F, 0.0F, 0.0F, 0.0F, 0.5236F));
        leftArm.addOrReplaceChild("t2_fin_l", CubeListBuilder.create()
                .texOffs(89, 8).mirror().addBox(-0.5F, -3.0F, -2.0F, 1.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, -3.5F, 0.0F, 0.0F, 0.0F, 0.2618F));
        leftArm.addOrReplaceChild("t3_wing_l", CubeListBuilder.create()
                .texOffs(0, 0).mirror().addBox(0.0F, -6.0F, -2.0F, 0.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, -2.0F, 1.0F, 0.0F, 0.0F, 0.3491F));
        leftArm.addOrReplaceChild("t4_spike_l1", CubeListBuilder.create()
                .texOffs(32, 10).mirror().addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.5F, -3.0F, -1.0F, 0.0F, 0.0F, 0.3491F));
        leftArm.addOrReplaceChild("t4_spike_l2", CubeListBuilder.create()
                .texOffs(37, 10).mirror().addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.5F, -2.5F, 1.0F, 0.2618F, 0.0F, 0.6109F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }
}
