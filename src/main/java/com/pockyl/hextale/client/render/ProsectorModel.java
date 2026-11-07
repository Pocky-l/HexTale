package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.port.client.KeyframeAnimation;
import com.pockyl.hextale.port.client.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.boss.ProsectorState.Section;
import com.pockyl.hextale.thevoid.boss.StudyKind;

/**
 * The Prosector: a hood of carapace over one pale eye, four instruments on jointed arms, the study sacs on its back.
 * Made in Blockbench ({@code art/models/entity/prosector.bbmodel}, Modded Entity, Mojang mappings). Keyframe animations
 * ({@link ProsectorAnimations}) follow the server's clock: the state's sections are mapped onto each animation's own
 * parts, so the blow in the picture lands on the tick the blow lands, whatever the config says. The idle keeps running
 * under everything and the moves add to it.
 */
public class ProsectorModel extends EntityModel<ProsectorRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Hextale.id("prosector"), "main");
    private final ModelPart iris;
    private final ModelPart eye;
    private final ModelPart[] sacs;
    private final ModelPart[] toGrip;
    private final KeyframeAnimation idle;
    private final KeyframeAnimation idleOpen;
    private final KeyframeAnimation incision;
    private final KeyframeAnimation probe;
    private final KeyframeAnimation retractor;
    private final KeyframeAnimation excision;
    private final KeyframeAnimation section;
    private final KeyframeAnimation riposte;
    private final KeyframeAnimation volley;
    private final KeyframeAnimation charge;
    private final KeyframeAnimation circle;
    private final KeyframeAnimation forgetting;
    private final KeyframeAnimation turnDissection;
    private final KeyframeAnimation turnProof;
    private final KeyframeAnimation death;

    public ProsectorModel(ModelPart modelRoot) {
        super(modelRoot);
        ModelPart root = modelRoot.getChild("root");
        ModelPart body = root.getChild("body");
        eye = body.getChild("eye");
        iris = eye.getChild("iris");
        sacs = new ModelPart[StudyKind.values().length];
        sacs[StudyKind.BLADE.ordinal()] = body.getChild("sac_blade");
        sacs[StudyKind.SHOT.ordinal()] = body.getChild("sac_shot");
        sacs[StudyKind.BLAST.ordinal()] = body.getChild("sac_blast");
        sacs[StudyKind.RUNE.ordinal()] = body.getChild("sac_rune");
        ModelPart arm = body.getChild("forceps_arm");
        ModelPart fore = arm.getChild("forceps_fore");
        ModelPart tool = fore.getChild("forceps_tool");
        toGrip = new ModelPart[] {modelRoot, root, body, arm, fore, tool, tool.getChild("forceps_grip")};
        idle = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.IDLE, modelRoot);
        idleOpen = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.IDLE_OPEN, modelRoot);
        incision = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.INCISION, modelRoot);
        probe = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.PROBE, modelRoot);
        retractor = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.RETRACTOR, modelRoot);
        excision = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.EXCISION, modelRoot);
        section = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.SECTION, modelRoot);
        riposte = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.RIPOSTE, modelRoot);
        volley = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.VOLLEY, modelRoot);
        charge = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.CHARGE, modelRoot);
        circle = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.CIRCLE, modelRoot);
        forgetting = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.FORGETTING, modelRoot);
        turnDissection = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.TURN_DISSECTION, modelRoot);
        turnProof = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.TURN_PROOF, modelRoot);
        death = com.pockyl.hextale.port.client.KeyframeAnimation.bake(ProsectorAnimations.DEATH, modelRoot);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rootBone = root.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition body = rootBone.addOrReplaceChild("body", CubeListBuilder.create().texOffs(138, 67).addBox(-14.0F, -14.0F, -10.0F, 28.0F, 26.0F, 26.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -52.0F, 4.0F));
        PartDefinition hood = body.addOrReplaceChild("hood", CubeListBuilder.create(), PartPose.offset(0.0F, -10.0F, 2.0F));
        PartDefinition hood_crest_r1 = hood.addOrReplaceChild("hood_crest_r1", CubeListBuilder.create().texOffs(53, 67).addBox(0.0F, -10.0F, -20.0F, 0.0F, 12.0F, 42.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -16.0F, 0.0F, 0.1571F, 0.0F, 0.0F));
        PartDefinition spine_b_r1 = hood.addOrReplaceChild("spine_b_r1", CubeListBuilder.create().texOffs(61, 262).addBox(0.0F, -14.0F, -4.0F, 0.0F, 18.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 22.0F, 1.0123F, 0.0F, 0.0F));
        PartDefinition spine_a_r1 = hood.addOrReplaceChild("spine_a_r1", CubeListBuilder.create().texOffs(212, 225).addBox(0.0F, -22.0F, -4.0F, 0.0F, 22.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.0F, 22.0F, 0.5236F, 0.0F, 0.0F));
        PartDefinition back_scale_c_r1 = hood.addOrReplaceChild("back_scale_c_r1", CubeListBuilder.create().texOffs(0, 385).addBox(-8.0F, -1.0F, -1.0F, 16.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 12.0F, 16.0F, -0.1047F, 0.0F, 0.0F));
        PartDefinition back_scale_b_r1 = hood.addOrReplaceChild("back_scale_b_r1", CubeListBuilder.create().texOffs(155, 364).addBox(-11.0F, -1.0F, -1.0F, 22.0F, 13.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 16.0F, 0.2094F, 0.0F, 0.0F));
        PartDefinition back_scale_a_r1 = hood.addOrReplaceChild("back_scale_a_r1", CubeListBuilder.create().texOffs(90, 364).addBox(-13.0F, -2.0F, -2.0F, 26.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -10.0F, 14.0F, 0.384F, 0.0F, 0.0F));
        PartDefinition brow_b_r1 = hood.addOrReplaceChild("brow_b_r1", CubeListBuilder.create().texOffs(0, 320).addBox(-1.0F, -3.0F, -15.0F, 20.0F, 6.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -18.0F, -0.3142F, -0.1396F, 0.2269F));
        PartDefinition brow_a_r1 = hood.addOrReplaceChild("brow_a_r1", CubeListBuilder.create().texOffs(168, 293).addBox(-19.0F, -3.0F, -15.0F, 20.0F, 6.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -18.0F, -0.3142F, 0.1396F, -0.2269F));
        PartDefinition hood_keel_r1 = hood.addOrReplaceChild("hood_keel_r1", CubeListBuilder.create().texOffs(127, 176).addBox(-4.0F, -4.0F, -22.0F, 8.0F, 5.0F, 36.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -14.0F, 0.0F, 0.1222F, 0.0F, 0.0F));
        PartDefinition hood_slope_b_r1 = hood.addOrReplaceChild("hood_slope_b_r1", CubeListBuilder.create().texOffs(113, 126).addBox(-2.0F, -2.0F, -26.0F, 15.0F, 8.0F, 41.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.0F, -6.0F, 0.0F, 0.0873F, 0.0F, 0.5934F));
        PartDefinition hood_slope_a_r1 = hood.addOrReplaceChild("hood_slope_a_r1", CubeListBuilder.create().texOffs(0, 126).addBox(-13.0F, -2.0F, -26.0F, 15.0F, 8.0F, 41.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.0F, -6.0F, 0.0F, 0.0873F, 0.0F, -0.5934F));
        PartDefinition hood_roof_r1 = hood.addOrReplaceChild("hood_roof_r1", CubeListBuilder.create().texOffs(0, 176).addBox(-11.0F, -6.0F, -26.0F, 22.0F, 7.0F, 41.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0873F, 0.0F, 0.0F));
        PartDefinition cheek_a = hood.addOrReplaceChild("cheek_a", CubeListBuilder.create(), PartPose.offset(-17.0F, 0.0F, 0.0F));
        PartDefinition tusk_a_r1 = cheek_a.addOrReplaceChild("tusk_a_r1", CubeListBuilder.create().texOffs(57, 225).addBox(0.0F, -2.0F, -12.0F, 0.0F, 22.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 26.0F, -24.0F, 0.0F, 0.1745F, -0.1745F));
        PartDefinition fringe_a_r1 = cheek_a.addOrReplaceChild("fringe_a_r1", CubeListBuilder.create().texOffs(203, 0).addBox(0.0F, -4.0F, -14.0F, 0.0F, 32.0F, 26.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 16.0F, -6.0F, 0.0F, 0.0F, -0.1047F));
        PartDefinition cheek_a_plate_r1 = cheek_a.addOrReplaceChild("cheek_a_plate_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -28.0F, 6.0F, 30.0F, 36.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2793F, -0.1571F));
        PartDefinition cheek_b = hood.addOrReplaceChild("cheek_b", CubeListBuilder.create(), PartPose.offset(17.0F, 0.0F, 0.0F));
        PartDefinition tusk_b_r1 = cheek_b.addOrReplaceChild("tusk_b_r1", CubeListBuilder.create().texOffs(86, 225).addBox(0.0F, -2.0F, -12.0F, 0.0F, 22.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 26.0F, -24.0F, 0.0F, -0.1745F, 0.1745F));
        PartDefinition fringe_b_r1 = cheek_b.addOrReplaceChild("fringe_b_r1", CubeListBuilder.create().texOffs(0, 67).addBox(0.0F, -4.0F, -14.0F, 0.0F, 32.0F, 26.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 16.0F, -6.0F, 0.0F, 0.0F, 0.1047F));
        PartDefinition cheek_b_plate_r1 = cheek_b.addOrReplaceChild("cheek_b_plate_r1", CubeListBuilder.create().texOffs(85, 0).addBox(-4.0F, -2.0F, -28.0F, 6.0F, 30.0F, 36.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2793F, 0.1571F));
        PartDefinition jaw = body.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(115, 225).addBox(-10.0F, -2.0F, -26.0F, 20.0F, 7.0F, 28.0F, new CubeDeformation(0.0F))
                .texOffs(78, 418).addBox(-10.0F, -9.0F, -26.5F, 20.0F, 9.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 14.0F, 4.0F));
        PartDefinition eye = body.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(0, 262).addBox(-10.0F, -10.0F, -6.0F, 20.0F, 20.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.0F, -10.0F));
        PartDefinition iris = eye.addOrReplaceChild("iris", CubeListBuilder.create().texOffs(0, 403).addBox(-6.0F, -6.0F, 1.0F, 12.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -8.0F));
        PartDefinition sac_blade = body.addOrReplaceChild("sac_blade", CubeListBuilder.create().texOffs(147, 320).addBox(-4.0F, -7.0F, -2.0F, 8.0F, 12.0F, 8.0F, new CubeDeformation(-0.5F)), PartPose.offset(-10.0F, -10.0F, 18.0F));
        PartDefinition sac_shot = body.addOrReplaceChild("sac_shot", CubeListBuilder.create().texOffs(180, 320).addBox(-4.0F, -7.0F, -2.0F, 8.0F, 12.0F, 8.0F, new CubeDeformation(-0.5F)), PartPose.offset(10.0F, -10.0F, 18.0F));
        PartDefinition sac_blast = body.addOrReplaceChild("sac_blast", CubeListBuilder.create().texOffs(213, 320).addBox(-4.0F, -7.0F, -2.0F, 8.0F, 12.0F, 8.0F, new CubeDeformation(-0.5F)), PartPose.offset(-10.0F, 6.0F, 18.0F));
        PartDefinition sac_rune = body.addOrReplaceChild("sac_rune", CubeListBuilder.create().texOffs(0, 343).addBox(-4.0F, -7.0F, -2.0F, 8.0F, 12.0F, 8.0F, new CubeDeformation(-0.5F)), PartPose.offset(10.0F, 6.0F, 18.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition tail_ring_b_r1 = tail.addOrReplaceChild("tail_ring_b_r1", CubeListBuilder.create().texOffs(0, 293).addBox(-8.5F, -1.0F, -7.0F, 17.0F, 10.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, -0.1745F, 0.0F, 0.0F));
        PartDefinition tail_ring_a_r1 = tail.addOrReplaceChild("tail_ring_a_r1", CubeListBuilder.create().texOffs(86, 262).addBox(-11.0F, -2.0F, -9.0F, 22.0F, 10.0F, 20.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1047F, 0.0F, 0.0F));
        PartDefinition tail_low = tail.addOrReplaceChild("tail_low", CubeListBuilder.create(), PartPose.offset(0.0F, 13.0F, 0.0F));
        PartDefinition tail_ring_d_r1 = tail_low.addOrReplaceChild("tail_ring_d_r1", CubeListBuilder.create().texOffs(210, 364).addBox(-3.5F, -1.0F, -3.0F, 7.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, -0.3491F, 0.0F, 0.0F));
        PartDefinition tail_ring_c_r1 = tail_low.addOrReplaceChild("tail_ring_c_r1", CubeListBuilder.create().texOffs(73, 320).addBox(-6.0F, -1.0F, -5.0F, 12.0F, 10.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.2443F, 0.0F, 0.0F));
        PartDefinition tendril_c1 = tail_low.addOrReplaceChild("tendril_c1", CubeListBuilder.create().texOffs(122, 320).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 16.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 9.0F, 6.0F, -0.3142F, 0.0F, 0.0F));
        PartDefinition tendril_c2 = tendril_c1.addOrReplaceChild("tendril_c2", CubeListBuilder.create().texOffs(234, 385).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 11.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(59, 385).addBox(-1.0F, 9.0F, -1.0F, 2.0F, 15.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(231, 262).addBox(0.0F, 6.0F, -4.0F, 0.0F, 20.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 14.0F, 0.0F, 0.2443F, 0.0F, 0.0F));
        PartDefinition tendril_a1 = tail.addOrReplaceChild("tendril_a1", CubeListBuilder.create().texOffs(118, 293).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 18.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F, 8.0F, -4.0F, 0.0F, 0.0F, -0.2094F));
        PartDefinition tendril_a2 = tendril_a1.addOrReplaceChild("tendril_a2", CubeListBuilder.create().texOffs(200, 385).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 11.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(41, 385).addBox(-1.0F, 9.0F, -1.0F, 2.0F, 15.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(197, 262).addBox(0.0F, 6.0F, -4.0F, 0.0F, 20.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, -0.1745F, 0.0F, 0.1745F));
        PartDefinition tendril_b1 = tail.addOrReplaceChild("tendril_b1", CubeListBuilder.create().texOffs(143, 293).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 18.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0F, 8.0F, -4.0F, 0.0F, 0.0F, 0.2094F));
        PartDefinition tendril_b2 = tendril_b1.addOrReplaceChild("tendril_b2", CubeListBuilder.create().texOffs(217, 385).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 11.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(50, 385).addBox(-1.0F, 9.0F, -1.0F, 2.0F, 15.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(214, 262).addBox(0.0F, 6.0F, -4.0F, 0.0F, 20.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, -0.1745F, 0.0F, -0.1745F));
        PartDefinition scalpel_arm = body.addOrReplaceChild("scalpel_arm", CubeListBuilder.create().texOffs(33, 343).addBox(-5.0F, -5.0F, -5.0F, 9.0F, 10.0F, 10.0F, new CubeDeformation(0.1F))
                .texOffs(130, 403).addBox(-21.0F, -3.0F, -3.0F, 18.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(119, 418).addBox(-33.0F, -2.0F, -2.0F, 14.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(68, 385).addBox(-36.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-18.0F, -8.0F, -4.0F, 0.0F, -0.4887F, 0.6283F));
        PartDefinition scalpel_fore = scalpel_arm.addOrReplaceChild("scalpel_fore", CubeListBuilder.create().texOffs(72, 343).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(21, 364).addBox(-1.5F, 14.0F, -1.5F, 3.0F, 16.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(67, 293).addBox(-11.0F, 2.0F, 0.0F, 10.0F, 26.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-32.0F, 0.0F, 0.0F, -0.5934F, 0.0F, -1.0123F));
        PartDefinition scalpel_tool = scalpel_fore.addOrReplaceChild("scalpel_tool", CubeListBuilder.create().texOffs(55, 403).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(170, 0).addBox(0.0F, 3.0F, -8.0F, 0.0F, 43.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 30.0F, 0.0F, -0.8029F, 0.0F, 0.2443F));
        PartDefinition forceps_arm = body.addOrReplaceChild("forceps_arm", CubeListBuilder.create().texOffs(89, 343).addBox(-4.0F, -5.0F, -5.0F, 9.0F, 10.0F, 10.0F, new CubeDeformation(0.1F))
                .texOffs(179, 403).addBox(3.0F, -3.0F, -3.0F, 18.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(156, 418).addBox(19.0F, -2.0F, -2.0F, 14.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(101, 385).addBox(28.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(18.0F, -8.0F, -4.0F, 0.0F, 0.4887F, -0.6283F));
        PartDefinition forceps_fore = forceps_arm.addOrReplaceChild("forceps_fore", CubeListBuilder.create().texOffs(128, 343).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(34, 364).addBox(-1.5F, 14.0F, -1.5F, 3.0F, 16.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(88, 293).addBox(1.0F, 2.0F, 0.0F, 10.0F, 26.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(32.0F, 0.0F, 0.0F, -0.5934F, 0.0F, 1.0123F));
        PartDefinition forceps_tool = forceps_fore.addOrReplaceChild("forceps_tool", CubeListBuilder.create().texOffs(80, 403).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 30.0F, 0.0F, -0.8029F, 0.0F, -0.2443F));
        PartDefinition forceps_jaw_a = forceps_tool.addOrReplaceChild("forceps_jaw_a", CubeListBuilder.create().texOffs(171, 262).addBox(-4.0F, -1.0F, 0.0F, 6.0F, 29.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.1396F));
        PartDefinition forceps_jaw_b = forceps_tool.addOrReplaceChild("forceps_jaw_b", CubeListBuilder.create().texOffs(184, 262).addBox(-2.0F, -1.0F, 0.0F, 6.0F, 29.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 4.0F, 0.0F, 0.0F, 0.0F, -0.1396F));
        PartDefinition forceps_grip = forceps_tool.addOrReplaceChild("forceps_grip", CubeListBuilder.create().texOffs(35, 429).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 18.0F, 0.0F));
        PartDefinition hook_arm = body.addOrReplaceChild("hook_arm", CubeListBuilder.create().texOffs(145, 343).addBox(-5.0F, -5.0F, -5.0F, 9.0F, 10.0F, 10.0F, new CubeDeformation(0.1F))
                .texOffs(0, 418).addBox(-17.0F, -2.5F, -2.5F, 14.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(193, 418).addBox(-29.0F, -1.5F, -1.5F, 14.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(134, 385).addBox(-32.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-16.0F, 10.0F, -2.0F, 0.0F, -0.4538F, -0.2443F));
        PartDefinition hook_fore = hook_arm.addOrReplaceChild("hook_fore", CubeListBuilder.create().texOffs(29, 403).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(47, 364).addBox(-1.5F, 8.0F, -1.5F, 3.0F, 16.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(184, 343).addBox(-11.0F, 2.0F, 0.0F, 10.0F, 20.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-28.0F, 0.0F, 0.0F, -0.5934F, 0.0F, -0.5236F));
        PartDefinition hook_tool = hook_fore.addOrReplaceChild("hook_tool", CubeListBuilder.create().texOffs(0, 225).addBox(-24.0F, -2.0F, 0.0F, 28.0F, 36.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, -0.6632F, 0.0F, 0.0F));
        PartDefinition probe_arm = body.addOrReplaceChild("probe_arm", CubeListBuilder.create().texOffs(205, 343).addBox(-4.0F, -5.0F, -5.0F, 9.0F, 10.0F, 10.0F, new CubeDeformation(0.1F))
                .texOffs(39, 418).addBox(3.0F, -2.5F, -2.5F, 14.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 429).addBox(15.0F, -1.5F, -1.5F, 14.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(167, 385).addBox(24.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(16.0F, 10.0F, -2.0F, 0.0F, 0.4538F, 0.2443F));
        PartDefinition probe_fore = probe_arm.addOrReplaceChild("probe_fore", CubeListBuilder.create().texOffs(42, 403).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(60, 364).addBox(-1.5F, 8.0F, -1.5F, 3.0F, 16.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 364).addBox(1.0F, 2.0F, 0.0F, 10.0F, 20.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(28.0F, 0.0F, 0.0F, -0.5934F, 0.0F, 0.5236F));
        PartDefinition probe_tool = probe_fore.addOrReplaceChild("probe_tool", CubeListBuilder.create().texOffs(105, 403).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(73, 364).addBox(-2.0F, 3.0F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(109, 293).addBox(-0.75F, 16.0F, -0.75F, 1.5F, 24.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, -0.733F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 256, 512);
    }

    /** Moves the pose to the point between the forceps' jaws. */
    public void translateToGrip(PoseStack poseStack) {
        for (ModelPart part : toGrip) {
            part.translateAndRotate(poseStack);
        }
    }

    /** Milliseconds into an animation cut into the three parts of a move, in seconds. */
    private static long at(ProsectorRenderer.State state, float startup, float active, float recovery) {
        float seconds = switch (state.section) {
            case STARTUP -> state.progress * startup;
            case ACTIVE -> startup + state.progress * active;
            case RECOVERY -> startup + active + state.progress * recovery;
            case NONE -> 0;
        };
        return (long) (seconds * 1000);
    }

    /** Milliseconds into a state that is one piece. */
    private static long whole(ProsectorRenderer.State state, float seconds) {
        return (long) (state.progress * seconds * 1000);
    }

    @Override
    public void setupAnim(ProsectorRenderer.State state) {
        super.setupAnim(state);
        long own = (long) (state.ageInTicks * 50);
        // The turn carries the hood open by itself; from then on the open idle holds it.
        float hood = switch (state.boss) {
            case TURN_DISSECTION -> 0;
            default -> state.hood;
        };
        if (hood < 1) {
            idle.apply(own, 1 - hood);
        }
        if (hood > 0) {
            idleOpen.apply(own, hood);
        }
        switch (state.boss) {
            case INCISION -> incision.apply(at(state, 1.25F, 0.1F, 1), 1);
            case PROBE -> probe.apply(at(state, 1.1F, 3.5F, 0.75F), 1);
            case RETRACTOR -> retractor.apply(at(state, 1, 0.5F, 0.75F), 1);
            case EXCISION -> excision.apply(at(state, 3, 0.25F, 5), 1);
            case SECTION -> section.apply(at(state, 0.8F, 0.3F, 0.9F), 1);
            case RIPOSTE -> riposte.apply(at(state, 0.85F, 0.45F, 0.75F), 1);
            case VOLLEY -> volley.apply(at(state, 1, 1.5F, 0.75F), 1);
            case CHARGE -> charge.apply(at(state, 1, 0.25F, 0.75F), 1);
            case CIRCLE -> circle.apply(at(state, 1, 0.75F, 0.75F), 1);
            case REELING -> forgetting.apply(whole(state, 1.5F), 0.6F);
            case FORGETTING, FALLEN -> forgetting.apply(whole(state, 5), 1);
            case TURN_DISSECTION -> turnDissection.apply(whole(state, 4), 1);
            case TURN_PROOF -> turnProof.apply(whole(state, 6), 1);
            case PROVING -> turnProof.apply(6000, 1);
            case DYING -> death.apply(whole(state, 6.5F), 1);
            default -> {
            }
        }
        // The eye keeps on its prey; shut, the iris draws in.
        eye.yRot += Mth.clamp(state.yRot, -35, 35) * Mth.DEG_TO_RAD;
        eye.xRot += Mth.clamp(state.xRot, -30, 30) * Mth.DEG_TO_RAD;
        float open = 0.3F + 0.7F * state.open;
        iris.xScale *= open;
        iris.yScale *= open;
        for (int i = 0; i < sacs.length; i++) {
            float swell = 1 + 0.35F * state.fills[i];
            sacs[i].xScale *= swell;
            sacs[i].yScale *= swell;
            sacs[i].zScale *= swell;
        }
    }
}
