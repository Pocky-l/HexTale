package com.pockyl.hextale.client.render;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Generated from art/models/equipment/bulwark_helmet.bbmodel by tools/blockbench/bb_armor_layer.py - edit the Blockbench project and regenerate,
 * never this file.
 */
public final class BulwarkHelmetLayer {
    private BulwarkHelmetLayer() {
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F))
                .texOffs(0, 17).addBox(-5.0F, -6.0F, -5.0F, 10.0F, 1.0F, 10.0F, new CubeDeformation(0.3F))
                .texOffs(0, 29).addBox(-1.0F, -9.7F, -4.6F, 2.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(21, 29).addBox(-5.6F, -4.5F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(21, 29).mirror().addBox(4.6F, -4.5F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(0, 44).addBox(-4.0F, -7.5F, -5.9F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(41, 17).addBox(-0.5F, -12.0F, -3.5F, 1.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(30, 29).addBox(-5.4F, -3.0F, -4.2F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(30, 29).mirror().addBox(4.4F, -3.0F, -4.2F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(34, 39).addBox(-1.5F, -7.5F, -5.9F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(19, 44).addBox(-1.0F, -7.0F, -6.3F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("neck_guard", CubeListBuilder.create()
                .texOffs(0, 39).addBox(-4.0F, 0.0F, -0.5F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 4.5F, 0.3491F, 0.0F, 0.0F));
        head.addOrReplaceChild("t3_wing_r", CubeListBuilder.create()
                .texOffs(33, 0).addBox(-0.4F, -5.0F, -0.5F, 0.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.4F, -5.0F, 0.5F, 0.0F, -0.4363F, 0.0F));
        head.addOrReplaceChild("t3_wing_l", CubeListBuilder.create()
                .texOffs(33, 0).mirror().addBox(0.4F, -5.0F, -0.5F, 0.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.4F, -5.0F, 0.5F, 0.0F, 0.4363F, 0.0F));
        head.addOrReplaceChild("t4_horn_r1", CubeListBuilder.create()
                .texOffs(39, 29).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.5F, -7.0F, 0.0F, -0.4363F, 0.0F, -0.4363F));
        head.addOrReplaceChild("t4_horn_l1", CubeListBuilder.create()
                .texOffs(39, 29).mirror().addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.5F, -7.0F, 0.0F, -0.4363F, 0.0F, 0.4363F));
        head.addOrReplaceChild("t4_horn_r2", CubeListBuilder.create()
                .texOffs(43, 39).addBox(-0.5F, -2.74F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.65F, -9.46F, 1.27F, -0.6109F, 0.0F, -0.1745F));
        head.addOrReplaceChild("t4_horn_l2", CubeListBuilder.create()
                .texOffs(43, 39).mirror().addBox(-0.5F, -2.74F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.65F, -9.46F, 1.27F, -0.6109F, 0.0F, 0.1745F));
        head.addOrReplaceChild("t4_ridge", CubeListBuilder.create()
                .texOffs(19, 39).addBox(-3.0F, 0.0F, -0.4F, 6.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.5F, 5.0F, 0.3491F, 0.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
