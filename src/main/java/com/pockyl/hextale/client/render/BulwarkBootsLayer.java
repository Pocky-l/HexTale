package com.pockyl.hextale.client.render;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Generated from art/models/equipment/bulwark_boots.bbmodel by tools/blockbench/bb_armor_layer.py - edit the Blockbench project and regenerate,
 * never this file.
 */
public final class BulwarkBootsLayer {
    private BulwarkBootsLayer() {
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F))
                .texOffs(40, 8).addBox(-2.1F, 10.0F, -3.6F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(9, 0).addBox(-3.0F, 6.0F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.15F))
                .texOffs(34, 0).addBox(-3.0F, 8.5F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.1F))
                .texOffs(27, 8).addBox(-2.1F, 11.0F, 1.6F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(9, 8).addBox(-1.5F, 6.0F, -3.6F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(59, 0).addBox(0.5F, 11.0F, -4.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(59, 3).addBox(-1.5F, 11.0F, -4.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        rightLeg.addOrReplaceChild("t3_ankle_wing_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, -2.0F, 0.0F, 0.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.3F, 9.0F, -1.0F, 0.0F, -0.3491F, 0.0F));
        rightLeg.addOrReplaceChild("t4_spur_r", CubeListBuilder.create()
                .texOffs(18, 8).addBox(-0.5F, -0.5F, -0.2F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 10.0F, 3.0F, 0.3491F, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(1.0F)).mirror(false)
                .texOffs(40, 8).mirror().addBox(-1.9F, 10.0F, -3.6F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(9, 0).mirror().addBox(-3.0F, 6.0F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.15F)).mirror(false)
                .texOffs(34, 0).mirror().addBox(-3.0F, 8.5F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.1F)).mirror(false)
                .texOffs(27, 8).mirror().addBox(-1.9F, 11.0F, 1.6F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(9, 8).mirror().addBox(-1.5F, 6.0F, -3.6F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(59, 0).mirror().addBox(-1.5F, 11.0F, -4.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(59, 3).mirror().addBox(0.5F, 11.0F, -4.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        leftLeg.addOrReplaceChild("t3_ankle_wing_l", CubeListBuilder.create()
                .texOffs(0, 0).mirror().addBox(0.0F, -2.0F, 0.0F, 0.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.3F, 9.0F, -1.0F, 0.0F, 0.3491F, 0.0F));
        leftLeg.addOrReplaceChild("t4_spur_l", CubeListBuilder.create()
                .texOffs(18, 8).mirror().addBox(-0.5F, -0.5F, -0.2F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 10.0F, 3.0F, 0.3491F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
