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
import net.minecraft.util.Mth;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.realm.RibCrawler;

/**
 * The Rib Crawler: twelve segments, each a band of husk-coloured chitin arched over dark flesh like a rib, with a
 * dorsal spine and two thin jointed legs; a head split into two hinged jaw plates over a magenta throat, sickle fangs
 * and feelers in front, two trailing cerci behind. The segments hang in two chains from the middle (forward to the head,
 * backward to the tail), so a few degrees per joint bend the whole body into waves, a rearing front or a coil. Made in
 * Blockbench ({@code art/models/entity/rib_crawler.bbmodel}, Modded Entity, Mojang mappings); its keyframe animations
 * ({@link RibCrawlerAnimations}) play as vanilla ones: one of two idles at the crawler's own phase, faded out by the walk,
 * the upside-down hang, the drop and the landing over the entity's own clocks, the bite over the swing.
 */
public class RibCrawlerModel extends EntityModel<RibCrawlerRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Hextale.id("rib_crawler"), "main");
    /** Degrees of look between two snaps of the head. */
    private static final float HEAD_STEP = 12.0F;
    private static final float ATTACK_MILLIS = 400.0F;
    private static final float DROP_MILLIS = 1500.0F;
    private static final float UNCOIL_MILLIS = 1000.0F;
    private final ModelPart head;
    private final KeyframeAnimation[] idles;
    private final KeyframeAnimation walk;
    private final KeyframeAnimation cling;
    private final KeyframeAnimation drop;
    private final KeyframeAnimation uncoil;
    private final KeyframeAnimation attack;

    public RibCrawlerModel(ModelPart root) {
        super(root);
        ModelPart front = root.getChild("body").getChild("s6");
        for (int i = 5; i >= 1; i--) {
            front = front.getChild("s" + i);
        }
        head = front.getChild("head");
        idles = new KeyframeAnimation[] {RibCrawlerAnimations.IDLE.bake(root), RibCrawlerAnimations.IDLE_2.bake(root)};
        walk = RibCrawlerAnimations.WALK.bake(root);
        cling = RibCrawlerAnimations.CLING.bake(root);
        drop = RibCrawlerAnimations.DROP.bake(root);
        uncoil = RibCrawlerAnimations.UNCOIL.bake(root);
        attack = RibCrawlerAnimations.ATTACK.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 20.0F, 0.0F));
        PartDefinition s6 = body.addOrReplaceChild("s6", CubeListBuilder.create().texOffs(19, 28).addBox(-3.0F, -1.5F, -3.0F, 6.0F, 3.0F, 3.0F, new CubeDeformation(0.06F))
                .texOffs(72, 51).addBox(-3.0F, -2.4F, -2.75F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition spine_s6_r1 = s6.addOrReplaceChild("spine_s6_r1", CubeListBuilder.create().texOffs(0, 42).addBox(-0.5F, -2.0F, -1.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, -2.2F, -1.5F, 0.5585F, 0.0F, 0.0F));
        PartDefinition ribtip_s6_l_r1 = s6.addOrReplaceChild("ribtip_s6_l_r1", CubeListBuilder.create().texOffs(0, 55).addBox(-0.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(4.7839F, -0.8832F, -1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s6_l_r1 = s6.addOrReplaceChild("rib_s6_l_r1", CubeListBuilder.create().texOffs(111, 51).addBox(0.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(2.5F, -1.9F, -1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s6_r_r1 = s6.addOrReplaceChild("ribtip_s6_r_r1", CubeListBuilder.create().texOffs(100, 51).addBox(-2.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-4.7839F, -0.8832F, -1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s6_r_r1 = s6.addOrReplaceChild("rib_s6_r_r1", CubeListBuilder.create().texOffs(89, 51).addBox(-3.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-2.5F, -1.9F, -1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition s5 = s6.addOrReplaceChild("s5", CubeListBuilder.create().texOffs(0, 28).addBox(-3.0F, -1.5F, -3.0F, 6.0F, 3.0F, 3.0F, new CubeDeformation(0.12F))
                .texOffs(11, 51).addBox(-3.0F, -2.4F, -2.75F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -3.0F));
        PartDefinition spine_s5_r1 = s5.addOrReplaceChild("spine_s5_r1", CubeListBuilder.create().texOffs(120, 63).addBox(-0.5F, -1.0F, -1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, -2.2F, -1.5F, 0.5585F, 0.0F, 0.0F));
        PartDefinition ribtip_s5_l_r1 = s5.addOrReplaceChild("ribtip_s5_l_r1", CubeListBuilder.create().texOffs(61, 51).addBox(-0.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(4.7839F, -0.8832F, -1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s5_l_r1 = s5.addOrReplaceChild("rib_s5_l_r1", CubeListBuilder.create().texOffs(50, 51).addBox(0.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(2.5F, -1.9F, -1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s5_r_r1 = s5.addOrReplaceChild("ribtip_s5_r_r1", CubeListBuilder.create().texOffs(39, 51).addBox(-2.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-4.7839F, -0.8832F, -1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s5_r_r1 = s5.addOrReplaceChild("rib_s5_r_r1", CubeListBuilder.create().texOffs(28, 51).addBox(-3.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-2.5F, -1.9F, -1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition s4 = s5.addOrReplaceChild("s4", CubeListBuilder.create().texOffs(109, 20).addBox(-3.0F, -1.5F, -3.0F, 6.0F, 3.0F, 3.0F, new CubeDeformation(0.06F))
                .texOffs(72, 47).addBox(-3.0F, -2.4F, -2.75F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -3.0F));
        PartDefinition spine_s4_r1 = s4.addOrReplaceChild("spine_s4_r1", CubeListBuilder.create().texOffs(118, 35).addBox(-0.5F, -2.0F, -1.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, -2.2F, -1.5F, 0.5585F, 0.0F, 0.0F));
        PartDefinition ribtip_s4_l_r1 = s4.addOrReplaceChild("ribtip_s4_l_r1", CubeListBuilder.create().texOffs(0, 51).addBox(-0.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(4.7839F, -0.8832F, -1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s4_l_r1 = s4.addOrReplaceChild("rib_s4_l_r1", CubeListBuilder.create().texOffs(111, 47).addBox(0.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(2.5F, -1.9F, -1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s4_r_r1 = s4.addOrReplaceChild("ribtip_s4_r_r1", CubeListBuilder.create().texOffs(100, 47).addBox(-2.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-4.7839F, -0.8832F, -1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s4_r_r1 = s4.addOrReplaceChild("rib_s4_r_r1", CubeListBuilder.create().texOffs(89, 47).addBox(-3.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-2.5F, -1.9F, -1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition s3 = s4.addOrReplaceChild("s3", CubeListBuilder.create().texOffs(92, 20).addBox(-2.5F, -1.5F, -3.0F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.12F))
                .texOffs(11, 47).addBox(-3.0F, -2.4F, -2.75F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -3.0F));
        PartDefinition spine_s3_r1 = s3.addOrReplaceChild("spine_s3_r1", CubeListBuilder.create().texOffs(113, 63).addBox(-0.5F, -1.0F, -1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, -2.2F, -1.5F, 0.5585F, 0.0F, 0.0F));
        PartDefinition ribtip_s3_l_r1 = s3.addOrReplaceChild("ribtip_s3_l_r1", CubeListBuilder.create().texOffs(61, 47).addBox(-0.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(4.7839F, -0.8832F, -1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s3_l_r1 = s3.addOrReplaceChild("rib_s3_l_r1", CubeListBuilder.create().texOffs(50, 47).addBox(0.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(2.5F, -1.9F, -1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s3_r_r1 = s3.addOrReplaceChild("ribtip_s3_r_r1", CubeListBuilder.create().texOffs(39, 47).addBox(-2.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-4.7839F, -0.8832F, -1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s3_r_r1 = s3.addOrReplaceChild("rib_s3_r_r1", CubeListBuilder.create().texOffs(28, 47).addBox(-3.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-2.5F, -1.9F, -1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition s2 = s3.addOrReplaceChild("s2", CubeListBuilder.create().texOffs(75, 20).addBox(-2.5F, -1.5F, -3.0F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.06F))
                .texOffs(70, 42).addBox(-2.5F, -2.4F, -2.75F, 5.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -3.0F));
        PartDefinition spine_s2_r1 = s2.addOrReplaceChild("spine_s2_r1", CubeListBuilder.create().texOffs(111, 35).addBox(-0.5F, -2.0F, -1.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, -2.2F, -1.5F, 0.5585F, 0.0F, 0.0F));
        PartDefinition ribtip_s2_l_r1 = s2.addOrReplaceChild("ribtip_s2_l_r1", CubeListBuilder.create().texOffs(0, 47).addBox(-0.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(4.2839F, -0.8832F, -1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s2_l_r1 = s2.addOrReplaceChild("rib_s2_l_r1", CubeListBuilder.create().texOffs(107, 42).addBox(0.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(2.0F, -1.9F, -1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s2_r_r1 = s2.addOrReplaceChild("ribtip_s2_r_r1", CubeListBuilder.create().texOffs(96, 42).addBox(-2.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-4.2839F, -0.8832F, -1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s2_r_r1 = s2.addOrReplaceChild("rib_s2_r_r1", CubeListBuilder.create().texOffs(85, 42).addBox(-3.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-2.0F, -1.9F, -1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition s1 = s2.addOrReplaceChild("s1", CubeListBuilder.create().texOffs(60, 20).addBox(-2.0F, -1.5F, -3.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.12F))
                .texOffs(21, 42).addBox(-2.0F, -2.4F, -2.75F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -3.0F));
        PartDefinition ribtip_s1_l_r1 = s1.addOrReplaceChild("ribtip_s1_l_r1", CubeListBuilder.create().texOffs(61, 42).addBox(-0.5F, -0.5F, -1.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(2.8703F, -1.2899F, -1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s1_l_r1 = s1.addOrReplaceChild("rib_s1_l_r1", CubeListBuilder.create().texOffs(52, 42).addBox(0.0F, -0.5F, -1.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(1.5F, -1.9F, -1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s1_r_r1 = s1.addOrReplaceChild("ribtip_s1_r_r1", CubeListBuilder.create().texOffs(43, 42).addBox(-1.5F, -0.5F, -1.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-2.8703F, -1.2899F, -1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s1_r_r1 = s1.addOrReplaceChild("rib_s1_r_r1", CubeListBuilder.create().texOffs(34, 42).addBox(-2.0F, -0.5F, -1.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-1.5F, -1.9F, -1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition leg_s1_r = s1.addOrReplaceChild("leg_s1_r", CubeListBuilder.create().texOffs(21, 67).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-1.5F, 0.75F, -0.25F, 0.0F, -0.4538F, 0.6109F));
        PartDefinition knee_s1_r = leg_s1_r.addOrReplaceChild("knee_s1_r", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, -0.2269F, 0.0F, -0.2618F));
        PartDefinition leg_s1_l = s1.addOrReplaceChild("leg_s1_l", CubeListBuilder.create().texOffs(38, 67).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(1.5F, 0.75F, -0.25F, 0.0F, 0.4538F, -0.6109F));
        PartDefinition knee_s1_l = leg_s1_l.addOrReplaceChild("knee_s1_l", CubeListBuilder.create().texOffs(5, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, -0.2269F, 0.0F, 0.2618F));
        PartDefinition head = s1.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 35).addBox(-1.5F, -0.75F, -4.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -3.0F));
        PartDefinition jaw_r = head.addOrReplaceChild("jaw_r", CubeListBuilder.create(), PartPose.offset(-0.5F, 0.0F, 0.0F));
        PartDefinition jaw_plate_r_r1 = jaw_r.addOrReplaceChild("jaw_plate_r_r1", CubeListBuilder.create().texOffs(72, 10).addBox(-4.0F, -0.5F, 0.0F, 4.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.4F, -1.5F, -6.5F, 0.1396F, -0.1745F, 0.0F));
        PartDefinition jaw_flesh_r_r1 = jaw_r.addOrReplaceChild("jaw_flesh_r_r1", CubeListBuilder.create().texOffs(0, 10).addBox(-3.0F, -1.5F, 0.0F, 3.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.25F, 0.0F, -5.5F, 0.0F, -0.1745F, 0.0F));
        PartDefinition fang_r = jaw_r.addOrReplaceChild("fang_r", CubeListBuilder.create(), PartPose.offset(-0.75F, 1.75F, -5.0F));
        PartDefinition fang_r_r1 = fang_r.addOrReplaceChild("fang_r_r1", CubeListBuilder.create().texOffs(47, 35).addBox(-2.0F, 0.0F, -5.5F, 4.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2443F, 0.0F, 0.0F));
        PartDefinition antenna_r = head.addOrReplaceChild("antenna_r", CubeListBuilder.create().texOffs(89, 35).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(-1.75F, -1.75F, -3.0F, -0.5236F, 0.3142F, 0.0F));
        PartDefinition antenna_tip_r = antenna_r.addOrReplaceChild("antenna_tip_r", CubeListBuilder.create().texOffs(30, 20).addBox(-0.5F, -0.5F, -5.5F, 1.0F, 1.0F, 6.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.3491F, 0.2443F, 0.0F));
        PartDefinition jaw_l = head.addOrReplaceChild("jaw_l", CubeListBuilder.create(), PartPose.offset(0.5F, 0.0F, 0.0F));
        PartDefinition jaw_plate_l_r1 = jaw_l.addOrReplaceChild("jaw_plate_l_r1", CubeListBuilder.create().texOffs(95, 10).addBox(0.0F, -0.5F, 0.0F, 4.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.4F, -1.5F, -6.5F, 0.1396F, 0.1745F, 0.0F));
        PartDefinition jaw_flesh_l_r1 = jaw_l.addOrReplaceChild("jaw_flesh_l_r1", CubeListBuilder.create().texOffs(19, 10).addBox(0.0F, -1.5F, 0.0F, 3.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.25F, 0.0F, -5.5F, 0.0F, 0.1745F, 0.0F));
        PartDefinition fang_l = jaw_l.addOrReplaceChild("fang_l", CubeListBuilder.create(), PartPose.offset(0.75F, 1.75F, -5.0F));
        PartDefinition fang_l_r1 = fang_l.addOrReplaceChild("fang_l_r1", CubeListBuilder.create().texOffs(68, 35).addBox(-2.0F, 0.05F, -5.5F, 4.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2443F, 0.0F, 0.0F));
        PartDefinition antenna_l = head.addOrReplaceChild("antenna_l", CubeListBuilder.create().texOffs(100, 35).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(1.75F, -1.75F, -3.0F, -0.5236F, -0.3142F, 0.0F));
        PartDefinition antenna_tip_l = antenna_l.addOrReplaceChild("antenna_tip_l", CubeListBuilder.create().texOffs(45, 20).addBox(-0.5F, -0.5F, -5.5F, 1.0F, 1.0F, 6.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.3491F, -0.2443F, 0.0F));
        PartDefinition leg_s2_r = s2.addOrReplaceChild("leg_s2_r", CubeListBuilder.create().texOffs(55, 67).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-2.0F, 0.75F, -0.25F, 0.0F, -0.3713F, 0.6109F));
        PartDefinition knee_s2_r = leg_s2_r.addOrReplaceChild("knee_s2_r", CubeListBuilder.create().texOffs(10, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, -0.1856F, 0.0F, -0.2618F));
        PartDefinition leg_s2_l = s2.addOrReplaceChild("leg_s2_l", CubeListBuilder.create().texOffs(72, 67).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(2.0F, 0.75F, -0.25F, 0.0F, 0.3713F, -0.6109F));
        PartDefinition knee_s2_l = leg_s2_l.addOrReplaceChild("knee_s2_l", CubeListBuilder.create().texOffs(15, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, -0.1856F, 0.0F, 0.2618F));
        PartDefinition leg_s3_r = s3.addOrReplaceChild("leg_s3_r", CubeListBuilder.create().texOffs(89, 67).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-2.0F, 0.75F, -0.25F, 0.0F, -0.2888F, 0.6109F));
        PartDefinition knee_s3_r = leg_s3_r.addOrReplaceChild("knee_s3_r", CubeListBuilder.create().texOffs(20, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, -0.1444F, 0.0F, -0.2618F));
        PartDefinition leg_s3_l = s3.addOrReplaceChild("leg_s3_l", CubeListBuilder.create().texOffs(106, 67).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(2.0F, 0.75F, -0.25F, 0.0F, 0.2888F, -0.6109F));
        PartDefinition knee_s3_l = leg_s3_l.addOrReplaceChild("knee_s3_l", CubeListBuilder.create().texOffs(25, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, -0.1444F, 0.0F, 0.2618F));
        PartDefinition leg_s4_r = s4.addOrReplaceChild("leg_s4_r", CubeListBuilder.create().texOffs(0, 71).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-2.5F, 0.75F, -0.25F, 0.0F, -0.2063F, 0.6109F));
        PartDefinition knee_s4_r = leg_s4_r.addOrReplaceChild("knee_s4_r", CubeListBuilder.create().texOffs(30, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, -0.1031F, 0.0F, -0.2618F));
        PartDefinition leg_s4_l = s4.addOrReplaceChild("leg_s4_l", CubeListBuilder.create().texOffs(17, 71).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(2.5F, 0.75F, -0.25F, 0.0F, 0.2063F, -0.6109F));
        PartDefinition knee_s4_l = leg_s4_l.addOrReplaceChild("knee_s4_l", CubeListBuilder.create().texOffs(35, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, -0.1031F, 0.0F, 0.2618F));
        PartDefinition leg_s5_r = s5.addOrReplaceChild("leg_s5_r", CubeListBuilder.create().texOffs(34, 71).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-2.5F, 0.75F, -0.25F, 0.0F, -0.1238F, 0.6109F));
        PartDefinition knee_s5_r = leg_s5_r.addOrReplaceChild("knee_s5_r", CubeListBuilder.create().texOffs(40, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, -0.0619F, 0.0F, -0.2618F));
        PartDefinition leg_s5_l = s5.addOrReplaceChild("leg_s5_l", CubeListBuilder.create().texOffs(51, 71).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(2.5F, 0.75F, -0.25F, 0.0F, 0.1238F, -0.6109F));
        PartDefinition knee_s5_l = leg_s5_l.addOrReplaceChild("knee_s5_l", CubeListBuilder.create().texOffs(45, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, -0.0619F, 0.0F, 0.2618F));
        PartDefinition leg_s6_r = s6.addOrReplaceChild("leg_s6_r", CubeListBuilder.create().texOffs(68, 71).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-2.5F, 0.75F, -0.25F, 0.0F, -0.0413F, 0.6109F));
        PartDefinition knee_s6_r = leg_s6_r.addOrReplaceChild("knee_s6_r", CubeListBuilder.create().texOffs(50, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, -0.0206F, 0.0F, -0.2618F));
        PartDefinition leg_s6_l = s6.addOrReplaceChild("leg_s6_l", CubeListBuilder.create().texOffs(85, 71).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(2.5F, 0.75F, -0.25F, 0.0F, 0.0413F, -0.6109F));
        PartDefinition knee_s6_l = leg_s6_l.addOrReplaceChild("knee_s6_l", CubeListBuilder.create().texOffs(55, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, -0.0206F, 0.0F, 0.2618F));
        PartDefinition s7 = body.addOrReplaceChild("s7", CubeListBuilder.create().texOffs(38, 28).addBox(-3.0F, -1.5F, 0.0F, 6.0F, 3.0F, 3.0F, new CubeDeformation(0.12F))
                .texOffs(11, 55).addBox(-3.0F, -2.4F, 0.25F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition spine_s7_r1 = s7.addOrReplaceChild("spine_s7_r1", CubeListBuilder.create().texOffs(0, 67).addBox(-0.5F, -1.0F, -1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, -2.2F, 1.5F, 0.5585F, 0.0F, 0.0F));
        PartDefinition ribtip_s7_l_r1 = s7.addOrReplaceChild("ribtip_s7_l_r1", CubeListBuilder.create().texOffs(61, 55).addBox(-0.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(4.7839F, -0.8832F, 1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s7_l_r1 = s7.addOrReplaceChild("rib_s7_l_r1", CubeListBuilder.create().texOffs(50, 55).addBox(0.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(2.5F, -1.9F, 1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s7_r_r1 = s7.addOrReplaceChild("ribtip_s7_r_r1", CubeListBuilder.create().texOffs(39, 55).addBox(-2.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-4.7839F, -0.8832F, 1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s7_r_r1 = s7.addOrReplaceChild("rib_s7_r_r1", CubeListBuilder.create().texOffs(28, 55).addBox(-3.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-2.5F, -1.9F, 1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition s8 = s7.addOrReplaceChild("s8", CubeListBuilder.create().texOffs(57, 28).addBox(-3.0F, -1.5F, 0.0F, 6.0F, 3.0F, 3.0F, new CubeDeformation(0.06F))
                .texOffs(72, 55).addBox(-3.0F, -2.4F, 0.25F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition spine_s8_r1 = s8.addOrReplaceChild("spine_s8_r1", CubeListBuilder.create().texOffs(7, 42).addBox(-0.5F, -2.0F, -1.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, -2.2F, 1.5F, 0.5585F, 0.0F, 0.0F));
        PartDefinition ribtip_s8_l_r1 = s8.addOrReplaceChild("ribtip_s8_l_r1", CubeListBuilder.create().texOffs(0, 59).addBox(-0.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(4.7839F, -0.8832F, 1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s8_l_r1 = s8.addOrReplaceChild("rib_s8_l_r1", CubeListBuilder.create().texOffs(111, 55).addBox(0.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(2.5F, -1.9F, 1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s8_r_r1 = s8.addOrReplaceChild("ribtip_s8_r_r1", CubeListBuilder.create().texOffs(100, 55).addBox(-2.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-4.7839F, -0.8832F, 1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s8_r_r1 = s8.addOrReplaceChild("rib_s8_r_r1", CubeListBuilder.create().texOffs(89, 55).addBox(-3.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-2.5F, -1.9F, 1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition s9 = s8.addOrReplaceChild("s9", CubeListBuilder.create().texOffs(76, 28).addBox(-3.0F, -1.5F, 0.0F, 6.0F, 3.0F, 3.0F, new CubeDeformation(0.12F))
                .texOffs(11, 59).addBox(-3.0F, -2.4F, 0.25F, 6.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition spine_s9_r1 = s9.addOrReplaceChild("spine_s9_r1", CubeListBuilder.create().texOffs(7, 67).addBox(-0.5F, -1.0F, -1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, -2.2F, 1.5F, 0.5585F, 0.0F, 0.0F));
        PartDefinition ribtip_s9_l_r1 = s9.addOrReplaceChild("ribtip_s9_l_r1", CubeListBuilder.create().texOffs(61, 59).addBox(-0.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(4.7839F, -0.8832F, 1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s9_l_r1 = s9.addOrReplaceChild("rib_s9_l_r1", CubeListBuilder.create().texOffs(50, 59).addBox(0.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(2.5F, -1.9F, 1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s9_r_r1 = s9.addOrReplaceChild("ribtip_s9_r_r1", CubeListBuilder.create().texOffs(39, 59).addBox(-2.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-4.7839F, -0.8832F, 1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s9_r_r1 = s9.addOrReplaceChild("rib_s9_r_r1", CubeListBuilder.create().texOffs(28, 59).addBox(-3.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-2.5F, -1.9F, 1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition s10 = s9.addOrReplaceChild("s10", CubeListBuilder.create().texOffs(95, 28).addBox(-2.5F, -1.5F, 0.0F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.06F))
                .texOffs(72, 59).addBox(-2.5F, -2.4F, 0.25F, 5.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition spine_s10_r1 = s10.addOrReplaceChild("spine_s10_r1", CubeListBuilder.create().texOffs(14, 42).addBox(-0.5F, -2.0F, -1.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, -2.2F, 1.5F, 0.5585F, 0.0F, 0.0F));
        PartDefinition ribtip_s10_l_r1 = s10.addOrReplaceChild("ribtip_s10_l_r1", CubeListBuilder.create().texOffs(0, 63).addBox(-0.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(4.2839F, -0.8832F, 1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s10_l_r1 = s10.addOrReplaceChild("rib_s10_l_r1", CubeListBuilder.create().texOffs(109, 59).addBox(0.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(2.0F, -1.9F, 1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s10_r_r1 = s10.addOrReplaceChild("ribtip_s10_r_r1", CubeListBuilder.create().texOffs(98, 59).addBox(-2.5F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-4.2839F, -0.8832F, 1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s10_r_r1 = s10.addOrReplaceChild("rib_s10_r_r1", CubeListBuilder.create().texOffs(87, 59).addBox(-3.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-2.0F, -1.9F, 1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition s11 = s10.addOrReplaceChild("s11", CubeListBuilder.create().texOffs(0, 35).addBox(-2.5F, -1.5F, 0.0F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.12F))
                .texOffs(11, 63).addBox(-2.0F, -2.4F, 0.25F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition spine_s11_r1 = s11.addOrReplaceChild("spine_s11_r1", CubeListBuilder.create().texOffs(14, 67).addBox(-0.5F, -1.0F, -1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, -2.2F, 1.5F, 0.5585F, 0.0F, 0.0F));
        PartDefinition ribtip_s11_l_r1 = s11.addOrReplaceChild("ribtip_s11_l_r1", CubeListBuilder.create().texOffs(55, 63).addBox(-0.5F, -0.5F, -1.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(3.7839F, -0.8832F, 1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s11_l_r1 = s11.addOrReplaceChild("rib_s11_l_r1", CubeListBuilder.create().texOffs(44, 63).addBox(0.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(1.5F, -1.9F, 1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s11_r_r1 = s11.addOrReplaceChild("ribtip_s11_r_r1", CubeListBuilder.create().texOffs(35, 63).addBox(-1.5F, -0.5F, -1.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-3.7839F, -0.8832F, 1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s11_r_r1 = s11.addOrReplaceChild("rib_s11_r_r1", CubeListBuilder.create().texOffs(24, 63).addBox(-3.0F, -0.5F, -1.25F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-1.5F, -1.9F, 1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition s12 = s11.addOrReplaceChild("s12", CubeListBuilder.create().texOffs(17, 35).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.06F))
                .texOffs(64, 63).addBox(-2.0F, -2.4F, 0.25F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition ribtip_s12_l_r1 = s12.addOrReplaceChild("ribtip_s12_l_r1", CubeListBuilder.create().texOffs(104, 63).addBox(-0.5F, -0.5F, -1.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(2.8703F, -1.2899F, 1.5F, 0.0F, 0.0F, 1.0821F));
        PartDefinition rib_s12_l_r1 = s12.addOrReplaceChild("rib_s12_l_r1", CubeListBuilder.create().texOffs(95, 63).addBox(0.0F, -0.5F, -1.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(1.5F, -1.9F, 1.5F, 0.0F, 0.0F, 0.4189F));
        PartDefinition ribtip_s12_r_r1 = s12.addOrReplaceChild("ribtip_s12_r_r1", CubeListBuilder.create().texOffs(86, 63).addBox(-1.5F, -0.5F, -1.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.05F)), PartPose.offsetAndRotation(-2.8703F, -1.2899F, 1.5F, 0.0F, 0.0F, -1.0821F));
        PartDefinition rib_s12_r_r1 = s12.addOrReplaceChild("rib_s12_r_r1", CubeListBuilder.create().texOffs(77, 63).addBox(-2.0F, -0.5F, -1.25F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.02F)), PartPose.offsetAndRotation(-1.5F, -1.9F, 1.5F, 0.0F, 0.0F, -0.4189F));
        PartDefinition leg_s12_r = s12.addOrReplaceChild("leg_s12_r", CubeListBuilder.create().texOffs(34, 77).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-1.5F, 0.75F, 2.75F, 0.0F, 0.4538F, 0.6109F));
        PartDefinition knee_s12_r = leg_s12_r.addOrReplaceChild("knee_s12_r", CubeListBuilder.create().texOffs(110, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.2269F, 0.0F, -0.2618F));
        PartDefinition leg_s12_l = s12.addOrReplaceChild("leg_s12_l", CubeListBuilder.create().texOffs(51, 77).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(1.5F, 0.75F, 2.75F, 0.0F, -0.4538F, -0.6109F));
        PartDefinition knee_s12_l = leg_s12_l.addOrReplaceChild("knee_s12_l", CubeListBuilder.create().texOffs(115, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.2269F, 0.0F, 0.2618F));
        PartDefinition tail = s12.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition cercus_r = tail.addOrReplaceChild("cercus_r", CubeListBuilder.create().texOffs(38, 10).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 7.0F, new CubeDeformation(-0.15F)), PartPose.offsetAndRotation(-1.25F, 0.25F, -0.5F, -0.1745F, -0.3491F, 0.0F));
        PartDefinition cercus_tip_r = cercus_r.addOrReplaceChild("cercus_tip_r", CubeListBuilder.create().texOffs(0, 20).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, 6.5F, 0.2094F, -0.1745F, 0.0F));
        PartDefinition cercus_l = tail.addOrReplaceChild("cercus_l", CubeListBuilder.create().texOffs(55, 10).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 7.0F, new CubeDeformation(-0.15F)), PartPose.offsetAndRotation(1.25F, 0.25F, -0.5F, -0.1745F, 0.3491F, 0.0F));
        PartDefinition cercus_tip_l = cercus_l.addOrReplaceChild("cercus_tip_l", CubeListBuilder.create().texOffs(15, 20).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, 6.5F, 0.2094F, 0.1745F, 0.0F));
        PartDefinition leg_s11_r = s11.addOrReplaceChild("leg_s11_r", CubeListBuilder.create().texOffs(0, 77).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-2.0F, 0.75F, 2.75F, 0.0F, 0.3713F, 0.6109F));
        PartDefinition knee_s11_r = leg_s11_r.addOrReplaceChild("knee_s11_r", CubeListBuilder.create().texOffs(100, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.1856F, 0.0F, -0.2618F));
        PartDefinition leg_s11_l = s11.addOrReplaceChild("leg_s11_l", CubeListBuilder.create().texOffs(17, 77).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(2.0F, 0.75F, 2.75F, 0.0F, -0.3713F, -0.6109F));
        PartDefinition knee_s11_l = leg_s11_l.addOrReplaceChild("knee_s11_l", CubeListBuilder.create().texOffs(105, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.1856F, 0.0F, 0.2618F));
        PartDefinition leg_s10_r = s10.addOrReplaceChild("leg_s10_r", CubeListBuilder.create().texOffs(85, 74).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-2.0F, 0.75F, 2.75F, 0.0F, 0.2888F, 0.6109F));
        PartDefinition knee_s10_r = leg_s10_r.addOrReplaceChild("knee_s10_r", CubeListBuilder.create().texOffs(90, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.1444F, 0.0F, -0.2618F));
        PartDefinition leg_s10_l = s10.addOrReplaceChild("leg_s10_l", CubeListBuilder.create().texOffs(102, 74).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(2.0F, 0.75F, 2.75F, 0.0F, -0.2888F, -0.6109F));
        PartDefinition knee_s10_l = leg_s10_l.addOrReplaceChild("knee_s10_l", CubeListBuilder.create().texOffs(95, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.1444F, 0.0F, 0.2618F));
        PartDefinition leg_s9_r = s9.addOrReplaceChild("leg_s9_r", CubeListBuilder.create().texOffs(51, 74).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-2.5F, 0.75F, 2.75F, 0.0F, 0.2063F, 0.6109F));
        PartDefinition knee_s9_r = leg_s9_r.addOrReplaceChild("knee_s9_r", CubeListBuilder.create().texOffs(80, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.1031F, 0.0F, -0.2618F));
        PartDefinition leg_s9_l = s9.addOrReplaceChild("leg_s9_l", CubeListBuilder.create().texOffs(68, 74).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(2.5F, 0.75F, 2.75F, 0.0F, -0.2063F, -0.6109F));
        PartDefinition knee_s9_l = leg_s9_l.addOrReplaceChild("knee_s9_l", CubeListBuilder.create().texOffs(85, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.1031F, 0.0F, 0.2618F));
        PartDefinition leg_s8_r = s8.addOrReplaceChild("leg_s8_r", CubeListBuilder.create().texOffs(17, 74).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-2.5F, 0.75F, 2.75F, 0.0F, 0.1238F, 0.6109F));
        PartDefinition knee_s8_r = leg_s8_r.addOrReplaceChild("knee_s8_r", CubeListBuilder.create().texOffs(70, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0619F, 0.0F, -0.2618F));
        PartDefinition leg_s8_l = s8.addOrReplaceChild("leg_s8_l", CubeListBuilder.create().texOffs(34, 74).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(2.5F, 0.75F, 2.75F, 0.0F, -0.1238F, -0.6109F));
        PartDefinition knee_s8_l = leg_s8_l.addOrReplaceChild("knee_s8_l", CubeListBuilder.create().texOffs(75, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0619F, 0.0F, 0.2618F));
        PartDefinition leg_s7_r = s7.addOrReplaceChild("leg_s7_r", CubeListBuilder.create().texOffs(102, 71).addBox(-6.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(-2.5F, 0.75F, 2.75F, 0.0F, 0.0413F, 0.6109F));
        PartDefinition knee_s7_r = leg_s7_r.addOrReplaceChild("knee_s7_r", CubeListBuilder.create().texOffs(60, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0206F, 0.0F, -0.2618F));
        PartDefinition leg_s7_l = s7.addOrReplaceChild("leg_s7_l", CubeListBuilder.create().texOffs(0, 74).addBox(-0.5F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(2.5F, 0.75F, 2.75F, 0.0F, -0.0413F, -0.6109F));
        PartDefinition knee_s7_l = leg_s7_l.addOrReplaceChild("knee_s7_l", CubeListBuilder.create().texOffs(65, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0206F, 0.0F, 0.2618F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(RibCrawlerRenderer.State state) {
        super.setupAnim(state);
        // Each crawler keeps its own idle at its own phase, so a nest never twitches in step.
        long ownTime = (long) (state.ageInTicks * 50) + state.idleVariant * 1301L;
        if (state.clinging) {
            cling.apply(ownTime, 1);
            return;
        }
        float walking = Math.min(1, state.walkAnimationSpeed * 2.5F);
        idles[Math.floorMod(state.idleVariant, idles.length)].apply(ownTime, 1 - walking);
        walk.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 1.6F, 2.5F);
        head.yRot += Math.round(state.yRot / HEAD_STEP) * HEAD_STEP * Mth.DEG_TO_RAD * 0.6F;
        head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.4F;
        if (state.bite > 0) {
            attack.apply((long) (state.bite * ATTACK_MILLIS), 1);
        }
        // Let go of the ceiling: it curls up on the way down, then springs open where it lands.
        if (state.drop > 0) {
            drop.apply((long) ((1 - state.drop) * DROP_MILLIS), 1);
        }
        if (state.uncoil > 0) {
            uncoil.apply((long) ((1 - state.uncoil) * UNCOIL_MILLIS), 1);
        }
    }
}
