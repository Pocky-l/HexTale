package com.pockyl.hextale.client.render;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.realm.PitMaw;

/**
 * The Pit Maw: eight wedge petals hinged round the rim of a 2.2 block circle. Shut they lie inward over the throat as a
 * low dome of Void soil with a ring of short spines; torn open they stand up and lean out into a funnel of wet plum
 * flesh, chitin at the edges, over a floor of veins that run to the throat - a lip of flesh, eight teeth and a magenta
 * glow. Each petal folds at three joints (base, middle, tip), so the funnel can curl and snap. Made in Blockbench
 * ({@code art/models/entity/pit_maw.bbmodel}, Modded Entity, Mojang mappings); its keyframe animations
 * ({@link PitMawAnimations}) play over the maw's own clocks: the opening and the closing over {@link PitMaw#open}, the
 * pull and the bite added on top of the open pose, the breathing of the dome while it lies in wait.
 */
public class PitMawModel extends EntityModel<PitMawRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Hextale.id("pit_maw"), "main");
    private static final float OPEN_MILLIS = PitMaw.OPEN_TICKS * 50.0F;
    private static final float CLOSE_MILLIS = PitMaw.CLOSE_TICKS * 50.0F;
    private static final float BITE_MILLIS = 400.0F;
    private final KeyframeAnimation hidden;
    private final KeyframeAnimation open;
    private final KeyframeAnimation pull;
    private final KeyframeAnimation bite;
    private final KeyframeAnimation close;

    public PitMawModel(ModelPart root) {
        super(root);
        hidden = PitMawAnimations.HIDDEN.bake(root);
        open = PitMawAnimations.OPEN.bake(root);
        pull = PitMawAnimations.PULL.bake(root);
        bite = PitMawAnimations.BITE.bake(root);
        close = PitMawAnimations.CLOSE.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition maw = root.addOrReplaceChild("maw", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition throat = maw.addOrReplaceChild("throat", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -0.3F, -4.0F, 8.0F, 0.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition core_inner_r1 = throat.addOrReplaceChild("core_inner_r1", CubeListBuilder.create().texOffs(0, 105).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.4F, 0.0F, 0.0F, -0.7854F, 0.0F));
        PartDefinition throat_0 = throat.addOrReplaceChild("throat_0", CubeListBuilder.create().texOffs(13, 112).addBox(-2.0F, -2.0F, 3.5F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.3927F, 0.0F));
        PartDefinition tooth_0 = throat_0.addOrReplaceChild("tooth_0", CubeListBuilder.create().texOffs(36, 119).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.5F, 4.0F, 0.6109F, 0.0F, 0.0F));
        PartDefinition throat_1 = throat.addOrReplaceChild("throat_1", CubeListBuilder.create().texOffs(31, 112).addBox(-2.0F, -2.0F, 3.5F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.1781F, 0.0F));
        PartDefinition tooth_1 = throat_1.addOrReplaceChild("tooth_1", CubeListBuilder.create().texOffs(41, 119).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.5F, 4.0F, 0.6109F, 0.0F, 0.0F));
        PartDefinition throat_2 = throat.addOrReplaceChild("throat_2", CubeListBuilder.create().texOffs(49, 112).addBox(-2.0F, -2.0F, 3.5F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.9635F, 0.0F));
        PartDefinition tooth_2 = throat_2.addOrReplaceChild("tooth_2", CubeListBuilder.create().texOffs(46, 119).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.5F, 4.0F, 0.6109F, 0.0F, 0.0F));
        PartDefinition throat_3 = throat.addOrReplaceChild("throat_3", CubeListBuilder.create().texOffs(67, 112).addBox(-2.0F, -2.0F, 3.5F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.7489F, 0.0F));
        PartDefinition tooth_3 = throat_3.addOrReplaceChild("tooth_3", CubeListBuilder.create().texOffs(51, 119).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.5F, 4.0F, 0.6109F, 0.0F, 0.0F));
        PartDefinition throat_4 = throat.addOrReplaceChild("throat_4", CubeListBuilder.create().texOffs(85, 112).addBox(-2.0F, -2.0F, 3.5F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.5343F, 0.0F));
        PartDefinition tooth_4 = throat_4.addOrReplaceChild("tooth_4", CubeListBuilder.create().texOffs(56, 119).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.5F, 4.0F, 0.6109F, 0.0F, 0.0F));
        PartDefinition throat_5 = throat.addOrReplaceChild("throat_5", CubeListBuilder.create().texOffs(103, 112).addBox(-2.0F, -2.0F, 3.5F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.3197F, 0.0F));
        PartDefinition tooth_5 = throat_5.addOrReplaceChild("tooth_5", CubeListBuilder.create().texOffs(61, 119).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.5F, 4.0F, 0.6109F, 0.0F, 0.0F));
        PartDefinition throat_6 = throat.addOrReplaceChild("throat_6", CubeListBuilder.create().texOffs(0, 119).addBox(-2.0F, -2.0F, 3.5F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.1051F, 0.0F));
        PartDefinition tooth_6 = throat_6.addOrReplaceChild("tooth_6", CubeListBuilder.create().texOffs(66, 119).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.5F, 4.0F, 0.6109F, 0.0F, 0.0F));
        PartDefinition throat_7 = throat.addOrReplaceChild("throat_7", CubeListBuilder.create().texOffs(18, 119).addBox(-2.0F, -2.0F, 3.5F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.8905F, 0.0F));
        PartDefinition tooth_7 = throat_7.addOrReplaceChild("tooth_7", CubeListBuilder.create().texOffs(71, 119).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.5F, 4.0F, 0.6109F, 0.0F, 0.0F));
        PartDefinition petal_0 = maw.addOrReplaceChild("petal_0", CubeListBuilder.create().texOffs(33, 0).addBox(-6.5F, -1.0F, 11.0F, 13.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(31, 72).addBox(-4.0F, -1.25F, 5.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.3927F, 0.0F));
        PartDefinition petal_0_base = petal_0.addOrReplaceChild("petal_0_base", CubeListBuilder.create().texOffs(74, 0).addBox(-6.5F, -1.0F, -5.0F, 13.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.5F, 17.0F, -0.1047F, 0.0F, 0.0F));
        PartDefinition spine_0 = petal_0_base.addOrReplaceChild("spine_0", CubeListBuilder.create().texOffs(26, 112).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.0F, -1.5F, 0.5236F, 0.0F, 0.0F));
        PartDefinition petal_0_mid = petal_0_base.addOrReplaceChild("petal_0_mid", CubeListBuilder.create().texOffs(0, 9).addBox(-4.5F, -1.0F, -5.5F, 9.0F, 2.0F, 6.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(0.0F, 0.0F, -4.5F, -0.1396F, 0.0F, 0.0F));
        PartDefinition petal_0_tip = petal_0_mid.addOrReplaceChild("petal_0_tip", CubeListBuilder.create().texOffs(60, 72).addBox(-3.0F, -1.0F, -4.5F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.04F))
                .texOffs(25, 105).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.1745F, 0.0F, 0.0F));
        PartDefinition petal_1 = maw.addOrReplaceChild("petal_1", CubeListBuilder.create().texOffs(31, 9).addBox(-6.5F, -1.0F, 11.0F, 13.0F, 1.0F, 7.0F, new CubeDeformation(0.05F))
                .texOffs(83, 72).addBox(-4.0F, -1.25F, 5.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.1781F, 0.0F));
        PartDefinition petal_1_base = petal_1.addOrReplaceChild("petal_1_base", CubeListBuilder.create().texOffs(72, 9).addBox(-6.5F, -1.0F, -5.0F, 13.0F, 2.0F, 6.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(0.0F, -1.5F, 17.0F, -0.1047F, 0.0F, 0.0F));
        PartDefinition spine_1 = petal_1_base.addOrReplaceChild("spine_1", CubeListBuilder.create().texOffs(44, 112).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.0F, -1.5F, 0.5236F, 0.0F, 0.0F));
        PartDefinition petal_1_mid = petal_1_base.addOrReplaceChild("petal_1_mid", CubeListBuilder.create().texOffs(0, 18).addBox(-4.5F, -1.0F, -5.5F, 9.0F, 2.0F, 6.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, 0.0F, -4.5F, -0.1396F, 0.0F, 0.0F));
        PartDefinition petal_1_tip = petal_1_mid.addOrReplaceChild("petal_1_tip", CubeListBuilder.create().texOffs(0, 81).addBox(-3.0F, -1.0F, -4.5F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.1F))
                .texOffs(38, 105).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.1745F, 0.0F, 0.0F));
        PartDefinition petal_2 = maw.addOrReplaceChild("petal_2", CubeListBuilder.create().texOffs(31, 18).addBox(-6.5F, -1.0F, 11.0F, 13.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(23, 81).addBox(-4.0F, -1.25F, 5.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.9635F, 0.0F));
        PartDefinition petal_2_base = petal_2.addOrReplaceChild("petal_2_base", CubeListBuilder.create().texOffs(72, 18).addBox(-6.5F, -1.0F, -5.0F, 13.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.5F, 17.0F, -0.1047F, 0.0F, 0.0F));
        PartDefinition spine_2 = petal_2_base.addOrReplaceChild("spine_2", CubeListBuilder.create().texOffs(62, 112).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.0F, -1.5F, 0.5236F, 0.0F, 0.0F));
        PartDefinition petal_2_mid = petal_2_base.addOrReplaceChild("petal_2_mid", CubeListBuilder.create().texOffs(0, 27).addBox(-4.5F, -1.0F, -5.5F, 9.0F, 2.0F, 6.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(0.0F, 0.0F, -4.5F, -0.1396F, 0.0F, 0.0F));
        PartDefinition petal_2_tip = petal_2_mid.addOrReplaceChild("petal_2_tip", CubeListBuilder.create().texOffs(52, 81).addBox(-3.0F, -1.0F, -4.5F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.04F))
                .texOffs(51, 105).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.1745F, 0.0F, 0.0F));
        PartDefinition petal_3 = maw.addOrReplaceChild("petal_3", CubeListBuilder.create().texOffs(31, 27).addBox(-6.5F, -1.0F, 11.0F, 13.0F, 1.0F, 7.0F, new CubeDeformation(0.05F))
                .texOffs(75, 81).addBox(-4.0F, -1.25F, 5.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.7489F, 0.0F));
        PartDefinition petal_3_base = petal_3.addOrReplaceChild("petal_3_base", CubeListBuilder.create().texOffs(72, 27).addBox(-6.5F, -1.0F, -5.0F, 13.0F, 2.0F, 6.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(0.0F, -1.5F, 17.0F, -0.1047F, 0.0F, 0.0F));
        PartDefinition spine_3 = petal_3_base.addOrReplaceChild("spine_3", CubeListBuilder.create().texOffs(80, 112).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.0F, -1.5F, 0.5236F, 0.0F, 0.0F));
        PartDefinition petal_3_mid = petal_3_base.addOrReplaceChild("petal_3_mid", CubeListBuilder.create().texOffs(0, 36).addBox(-4.5F, -1.0F, -5.5F, 9.0F, 2.0F, 6.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, 0.0F, -4.5F, -0.1396F, 0.0F, 0.0F));
        PartDefinition petal_3_tip = petal_3_mid.addOrReplaceChild("petal_3_tip", CubeListBuilder.create().texOffs(104, 81).addBox(-3.0F, -1.0F, -4.5F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.1F))
                .texOffs(64, 105).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.1745F, 0.0F, 0.0F));
        PartDefinition petal_4 = maw.addOrReplaceChild("petal_4", CubeListBuilder.create().texOffs(31, 36).addBox(-6.5F, -1.0F, 11.0F, 13.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(0, 89).addBox(-4.0F, -1.25F, 5.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.5343F, 0.0F));
        PartDefinition petal_4_base = petal_4.addOrReplaceChild("petal_4_base", CubeListBuilder.create().texOffs(72, 36).addBox(-6.5F, -1.0F, -5.0F, 13.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.5F, 17.0F, -0.1047F, 0.0F, 0.0F));
        PartDefinition spine_4 = petal_4_base.addOrReplaceChild("spine_4", CubeListBuilder.create().texOffs(98, 112).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.0F, -1.5F, 0.5236F, 0.0F, 0.0F));
        PartDefinition petal_4_mid = petal_4_base.addOrReplaceChild("petal_4_mid", CubeListBuilder.create().texOffs(0, 45).addBox(-4.5F, -1.0F, -5.5F, 9.0F, 2.0F, 6.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(0.0F, 0.0F, -4.5F, -0.1396F, 0.0F, 0.0F));
        PartDefinition petal_4_tip = petal_4_mid.addOrReplaceChild("petal_4_tip", CubeListBuilder.create().texOffs(29, 89).addBox(-3.0F, -1.0F, -4.5F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.04F))
                .texOffs(77, 105).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.1745F, 0.0F, 0.0F));
        PartDefinition petal_5 = maw.addOrReplaceChild("petal_5", CubeListBuilder.create().texOffs(31, 45).addBox(-6.5F, -1.0F, 11.0F, 13.0F, 1.0F, 7.0F, new CubeDeformation(0.05F))
                .texOffs(52, 89).addBox(-4.0F, -1.25F, 5.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.3197F, 0.0F));
        PartDefinition petal_5_base = petal_5.addOrReplaceChild("petal_5_base", CubeListBuilder.create().texOffs(72, 45).addBox(-6.5F, -1.0F, -5.0F, 13.0F, 2.0F, 6.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(0.0F, -1.5F, 17.0F, -0.1047F, 0.0F, 0.0F));
        PartDefinition spine_5 = petal_5_base.addOrReplaceChild("spine_5", CubeListBuilder.create().texOffs(116, 112).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.0F, -1.5F, 0.5236F, 0.0F, 0.0F));
        PartDefinition petal_5_mid = petal_5_base.addOrReplaceChild("petal_5_mid", CubeListBuilder.create().texOffs(0, 54).addBox(-4.5F, -1.0F, -5.5F, 9.0F, 2.0F, 6.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, 0.0F, -4.5F, -0.1396F, 0.0F, 0.0F));
        PartDefinition petal_5_tip = petal_5_mid.addOrReplaceChild("petal_5_tip", CubeListBuilder.create().texOffs(81, 89).addBox(-3.0F, -1.0F, -4.5F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.1F))
                .texOffs(90, 105).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.1745F, 0.0F, 0.0F));
        PartDefinition petal_6 = maw.addOrReplaceChild("petal_6", CubeListBuilder.create().texOffs(31, 54).addBox(-6.5F, -1.0F, 11.0F, 13.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(0, 97).addBox(-4.0F, -1.25F, 5.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.1051F, 0.0F));
        PartDefinition petal_6_base = petal_6.addOrReplaceChild("petal_6_base", CubeListBuilder.create().texOffs(72, 54).addBox(-6.5F, -1.0F, -5.0F, 13.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.5F, 17.0F, -0.1047F, 0.0F, 0.0F));
        PartDefinition spine_6 = petal_6_base.addOrReplaceChild("spine_6", CubeListBuilder.create().texOffs(13, 119).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.0F, -1.5F, 0.5236F, 0.0F, 0.0F));
        PartDefinition petal_6_mid = petal_6_base.addOrReplaceChild("petal_6_mid", CubeListBuilder.create().texOffs(0, 63).addBox(-4.5F, -1.0F, -5.5F, 9.0F, 2.0F, 6.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(0.0F, 0.0F, -4.5F, -0.1396F, 0.0F, 0.0F));
        PartDefinition petal_6_tip = petal_6_mid.addOrReplaceChild("petal_6_tip", CubeListBuilder.create().texOffs(29, 97).addBox(-3.0F, -1.0F, -4.5F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.04F))
                .texOffs(103, 105).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.1745F, 0.0F, 0.0F));
        PartDefinition petal_7 = maw.addOrReplaceChild("petal_7", CubeListBuilder.create().texOffs(31, 63).addBox(-6.5F, -1.0F, 11.0F, 13.0F, 1.0F, 7.0F, new CubeDeformation(0.05F))
                .texOffs(52, 97).addBox(-4.0F, -1.25F, 5.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.8905F, 0.0F));
        PartDefinition petal_7_base = petal_7.addOrReplaceChild("petal_7_base", CubeListBuilder.create().texOffs(72, 63).addBox(-6.5F, -1.0F, -5.0F, 13.0F, 2.0F, 6.0F, new CubeDeformation(0.06F)), PartPose.offsetAndRotation(0.0F, -1.5F, 17.0F, -0.1047F, 0.0F, 0.0F));
        PartDefinition spine_7 = petal_7_base.addOrReplaceChild("spine_7", CubeListBuilder.create().texOffs(31, 119).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, -1.0F, -1.5F, 0.5236F, 0.0F, 0.0F));
        PartDefinition petal_7_mid = petal_7_base.addOrReplaceChild("petal_7_mid", CubeListBuilder.create().texOffs(0, 72).addBox(-4.5F, -1.0F, -5.5F, 9.0F, 2.0F, 6.0F, new CubeDeformation(0.08F)), PartPose.offsetAndRotation(0.0F, 0.0F, -4.5F, -0.1396F, 0.0F, 0.0F));
        PartDefinition petal_7_tip = petal_7_mid.addOrReplaceChild("petal_7_tip", CubeListBuilder.create().texOffs(81, 97).addBox(-3.0F, -1.0F, -4.5F, 6.0F, 2.0F, 5.0F, new CubeDeformation(0.1F))
                .texOffs(0, 112).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.12F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.1745F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(PitMawRenderer.State state) {
        super.setupAnim(state);
        long ownTime = (long) (state.ageInTicks * 50) + state.idVariant * 2311L;
        if (state.closing && state.open > 0) {
            close.apply((long) ((1 - state.open) * CLOSE_MILLIS), 1);
        } else if (state.open > 0) {
            open.apply((long) (state.open * OPEN_MILLIS), 1);
            if (state.pulling) {
                pull.apply(ownTime, state.open);
            }
            if (state.bite > 0) {
                bite.apply((long) (state.bite * BITE_MILLIS), 1);
            }
        } else {
            hidden.apply(ownTime, 1);
        }
    }
}
