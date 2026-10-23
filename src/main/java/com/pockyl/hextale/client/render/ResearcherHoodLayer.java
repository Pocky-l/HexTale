package com.pockyl.hextale.client.render;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Generated from art/models/equipment/researcher_hood.bbmodel by tools/blockbench/bb_armor_layer.py - edit the Blockbench project and regenerate,
 * never this file.
 */
public final class ResearcherHoodLayer {
    private ResearcherHoodLayer() {
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 36).addBox(-5.5F, -9.6F, -4.6F, 11.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(42, 36).addBox(4.6F, -8.6F, -4.6F, 1.0F, 9.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(42, 36).mirror().addBox(-5.6F, -8.6F, -4.6F, 1.0F, 9.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(64, 36).addBox(-5.5F, -8.6F, 4.6F, 11.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(64, 47).addBox(-5.5F, -9.6F, -5.6F, 11.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(88, 36).addBox(4.6F, -7.6F, -5.6F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(88, 36).mirror().addBox(-5.6F, -7.6F, -5.6F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(114, 36).addBox(-1.0F, -10.0F, -6.2F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(114, 40).addBox(-0.5F, -9.5F, -6.6F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("tail_a", CubeListBuilder.create()
                .texOffs(92, 36).addBox(-4.0F, -10.6F, 3.6F, 8.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2443F, 0.0F, 0.0F));
        head.addOrReplaceChild("tail_b", CubeListBuilder.create()
                .texOffs(92, 49).addBox(-3.0F, -10.6F, 4.6F, 6.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4538F, 0.0F, 0.0F));
        head.addOrReplaceChild("flare_l", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-0.5F, 0.0F, -4.6F, 1.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.1F, 0.4F, 0.0F, 0.0F, 0.0F, -0.2618F));
        head.addOrReplaceChild("flare_r", CubeListBuilder.create()
                .texOffs(0, 48).mirror().addBox(-0.5F, 0.0F, -4.6F, 1.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.1F, 0.4F, 0.0F, 0.0F, 0.0F, 0.2618F));
        head.addOrReplaceChild("flare_back", CubeListBuilder.create()
                .texOffs(64, 51).addBox(-5.5F, 0.0F, -0.5F, 11.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.4F, 5.1F, 0.2618F, 0.0F, 0.0F));
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
        return LayerDefinition.create(mesh, 128, 128);
    }
}
