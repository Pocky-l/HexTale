package com.pockyl.hextale.client.render;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Generated from art/models/equipment/chem_steel_helmet.bbmodel by tools/blockbench/bb_armor_layer.py - edit the Blockbench project and regenerate,
 * never this file.
 */
public final class ChemSteelHelmetLayer {
    private ChemSteelHelmetLayer() {
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F))
                .texOffs(92, 0).addBox(-1.0F, -9.8F, -4.5F, 2.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(80, 29).addBox(-4.5F, -7.0F, -5.6F, 9.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(58, 25).addBox(-2.0F, -2.5F, -5.9F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(101, 29).addBox(-1.0F, -2.2F, -6.9F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(108, 26).addBox(-4.0F, -1.8F, -6.6F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(108, 26).mirror().addBox(2.0F, -1.8F, -6.6F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("leather_helm_neck", CubeListBuilder.create()
                .texOffs(39, 25).addBox(-4.0F, 0.0F, -0.5F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 5.1F, 0.3491F, 0.0F, 0.0F));
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
        return LayerDefinition.create(mesh, 128, 64);
    }
}
