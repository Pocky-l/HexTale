package com.pockyl.hextale.client.render;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Generated from art/models/equipment/chem_steel_chestplate.bbmodel by tools/blockbench/bb_armor_layer.py - edit the Blockbench project and regenerate,
 * never this file.
 */
public final class ChemSteelChestplateLayer {
    private ChemSteelChestplateLayer() {
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(33, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.5F))
                .texOffs(109, 11).addBox(-4.0F, 1.0F, -3.3F, 8.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(39, 30).addBox(-4.0F, 8.5F, -3.2F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(25, 25).addBox(1.5F, 3.0F, -4.1F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(7, 31).addBox(1.5F, 2.0F, -4.1F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.15F))
                .texOffs(14, 31).addBox(1.5F, 7.0F, -4.1F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.15F))
                .texOffs(0, 25).addBox(-4.0F, -0.5F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.6F))
                .texOffs(124, 26).addBox(2.0F, 0.0F, -3.9F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(42, 17).addBox(2.0F, -1.6F, -3.4F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(75, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.5F))
                .texOffs(25, 17).addBox(-3.0F, 7.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.6F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("plate_pauldron_r", CubeListBuilder.create()
                .texOffs(58, 15).addBox(-4.0F, -1.5F, -3.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.5F, 0.0F, 0.0F, 0.0F, -0.1745F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(75, 0).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.5F)).mirror(false)
                .texOffs(25, 17).mirror().addBox(-1.0F, 7.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.6F)).mirror(false),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("plate_pauldron_l", CubeListBuilder.create()
                .texOffs(58, 15).mirror().addBox(-2.0F, -1.5F, -3.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.5F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }
}
