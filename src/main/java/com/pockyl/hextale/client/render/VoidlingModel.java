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
import com.pockyl.hextale.thevoid.Voidling;

/**
 * The voidling of the original HexTale, carried over without an animation library: a wedge of a hooded head with three
 * eyes over a fanged mouth and three pincers, two thin forelimbs and four long scythe legs. Geometry converted from its
 * Bedrock model in Blockbench ({@code art/models/entity/voidling.bbmodel}, Modded Entity, Mojang mappings), drawn at half
 * size by the renderer as before. Its own keyframe animations ({@link VoidlingAnimations}) play as vanilla ones: one of
 * three idles, each voidling at its own phase, faded out by the walk; the lunge over a bite's swing, the unfolding out
 * of a breach and the molt over their own clocks. The head does not follow the look smoothly: it holds, then snaps.
 */
public class VoidlingModel extends EntityModel<VoidlingRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Hextale.id("voidling"), "main");
    /** Degrees of look between two snaps of the head. */
    private static final float HEAD_STEP = 15.0F;
    /** Length of {@link VoidlingAnimations#ATTACK}, spread over a bite's swing. */
    private static final float ATTACK_MILLIS = 350.0F;
    private final ModelPart head;
    private final KeyframeAnimation[] idles;
    private final KeyframeAnimation walk;
    private final KeyframeAnimation attack;
    private final KeyframeAnimation emerge;
    private final KeyframeAnimation molt;

    public VoidlingModel(ModelPart root) {
        super(root);
        head = root.getChild("body").getChild("neck").getChild("head");
        idles = new KeyframeAnimation[] {VoidlingAnimations.IDLE.bake(root), VoidlingAnimations.IDLE_2.bake(root),
                VoidlingAnimations.IDLE_3.bake(root)};
        walk = VoidlingAnimations.WALK.bake(root);
        attack = VoidlingAnimations.ATTACK.bake(root);
        emerge = VoidlingAnimations.EMERGE.bake(root);
        molt = VoidlingAnimations.MOLT.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 62).addBox(-4.0F, -9.0F, -1.0F, 8.0F, 8.0F, 9.0F), PartPose.offset(0.0F, 14.0F, 0.0F));
        PartDefinition hands = body.addOrReplaceChild("hands", CubeListBuilder.create(), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition front_right_hand = hands.addOrReplaceChild("front_right_hand", CubeListBuilder.create(), PartPose.offset(-3.0F, -17.5F, -1.6F));
        front_right_hand.addOrReplaceChild("front_right_hand_r1", CubeListBuilder.create().texOffs(25, 57).addBox(0.5F, 0.0F, -10.0F, 1.0F, 1.0F, 9.0F)
                .texOffs(3, 1).addBox(0.5F, 0.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(-0.5F, -1.5F, 1.6F, 0.7321F, 0.3868F, -0.1208F));
        PartDefinition bone7 = front_right_hand.addOrReplaceChild("bone7", CubeListBuilder.create(), PartPose.offset(-1.5F, 4.5F, -5.0F));
        bone7.addOrReplaceChild("bone7_r1", CubeListBuilder.create().texOffs(0, 10).addBox(-0.4F, 0.0F, -5.7F, 1.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.3054F, 0.0F));
        bone7.addOrReplaceChild("bone7_r2", CubeListBuilder.create().texOffs(0, 2).addBox(-0.4005F, 0.1991F, -1.09F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6448F, 0.3868F, -0.1208F));
        PartDefinition bone8 = bone7.addOrReplaceChild("bone8", CubeListBuilder.create(), PartPose.offset(-1.3F, -3.8F, -4.3F));
        bone8.addOrReplaceChild("bone8_r1", CubeListBuilder.create().texOffs(32, 5).addBox(0.0271F, -0.3008F, -12.7283F, 0.0F, 3.0F, 13.0F)
                .texOffs(74, 74).addBox(-0.4729F, -0.3008F, -9.7283F, 1.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(-0.2F, 0.3F, -0.7F, 1.0036F, 0.3054F, 0.0F));
        bone8.addOrReplaceChild("bone8_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-0.4729F, -1.2008F, -0.2283F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(-0.2F, 0.3F, -0.7F, -0.7418F, 0.3054F, 0.0F));
        PartDefinition front_left_hand = hands.addOrReplaceChild("front_left_hand", CubeListBuilder.create(), PartPose.offset(3.0F, -17.5F, -1.6F));
        front_left_hand.addOrReplaceChild("front_left_hand_r1", CubeListBuilder.create().texOffs(13, 85).addBox(-1.5F, 0.0F, -10.0F, 1.0F, 1.0F, 9.0F)
                .texOffs(3, 3).addBox(-1.5F, 0.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(0.5F, -1.5F, 1.6F, 0.7321F, -0.3868F, 0.1208F));
        PartDefinition bone = front_left_hand.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(1.5F, 4.5F, -5.0F));
        bone.addOrReplaceChild("bone_r1", CubeListBuilder.create().texOffs(48, 26).addBox(-0.6F, 0.0F, -5.7F, 1.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, -0.3054F, 0.0F));
        bone.addOrReplaceChild("bone_r2", CubeListBuilder.create().texOffs(3, 5).addBox(-0.5995F, 0.1991F, -1.09F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6448F, -0.3868F, 0.1208F));
        PartDefinition bone6 = bone.addOrReplaceChild("bone6", CubeListBuilder.create(), PartPose.offset(1.3F, -3.8F, -4.3F));
        bone6.addOrReplaceChild("bone6_r1", CubeListBuilder.create().texOffs(0, 34).addBox(-0.0271F, -0.3008F, -12.7283F, 0.0F, 3.0F, 13.0F)
                .texOffs(0, 79).addBox(-0.5271F, -0.3008F, -9.7283F, 1.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(0.2F, 0.3F, -0.7F, 1.0036F, -0.3054F, 0.0F));
        bone6.addOrReplaceChild("bone6_r2", CubeListBuilder.create().texOffs(0, 4).addBox(-0.6774F, -0.7312F, -0.4448F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.15F)), PartPose.offsetAndRotation(0.2F, 0.1F, -0.2F, -0.7418F, -0.3054F, 0.0F));
        PartDefinition legs = body.addOrReplaceChild("legs", CubeListBuilder.create(), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition back_right_leg = legs.addOrReplaceChild("back_right_leg", CubeListBuilder.create(), PartPose.offset(-3.5F, -13.0F, 8.0F));
        back_right_leg.addOrReplaceChild("back_right_leg_r1", CubeListBuilder.create().texOffs(0, 2).addBox(-0.2321F, 0.1764F, 0.7397F, 0.0F, 4.0F, 16.0F)
                .texOffs(30, 52).addBox(-1.2321F, -1.8236F, 0.7397F, 2.0F, 3.0F, 16.0F)
                .texOffs(36, 57).addBox(-1.2321F, -1.8236F, -1.2603F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.8004F, -0.5272F, -0.0517F));
        PartDefinition last_bone2 = back_right_leg.addOrReplaceChild("last_bone2", CubeListBuilder.create(), PartPose.offset(-6.5F, -11.6F, 9.7F));
        last_bone2.addOrReplaceChild("last_bone2_r1", CubeListBuilder.create().texOffs(56, 26).addBox(-0.9768F, -1.5668F, -2.4398F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(-0.4474F, -0.2164F, 0.6619F, 0.5037F, -0.5272F, -0.0517F));
        last_bone2.addOrReplaceChild("last_bone2_r2", CubeListBuilder.create().texOffs(0, 0).addBox(0.0041F, -1.6622F, -4.4628F, 0.0F, 7.0F, 33.0F)
                .texOffs(43, 0).addBox(-0.9959F, -0.6622F, -1.4628F, 2.0F, 3.0F, 23.0F), PartPose.offsetAndRotation(-0.4474F, -0.2164F, 0.6619F, -1.1506F, -0.5117F, -0.0062F));
        PartDefinition back_left_leg = legs.addOrReplaceChild("back_left_leg", CubeListBuilder.create(), PartPose.offset(3.5F, -13.0F, 8.0F));
        back_left_leg.addOrReplaceChild("back_left_leg_r1", CubeListBuilder.create().texOffs(0, 6).addBox(0.2321F, 0.1764F, 0.7397F, 0.0F, 4.0F, 16.0F)
                .texOffs(50, 55).addBox(-0.7679F, -1.8236F, 0.7397F, 2.0F, 3.0F, 16.0F)
                .texOffs(70, 6).addBox(-0.7679F, -1.8236F, -1.2603F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.8004F, 0.5272F, 0.0517F));
        PartDefinition last_bone = back_left_leg.addOrReplaceChild("last_bone", CubeListBuilder.create(), PartPose.offset(6.5F, -11.6F, 9.7F));
        last_bone.addOrReplaceChild("last_bone_r1", CubeListBuilder.create().texOffs(70, 0).addBox(-1.0232F, -1.5668F, -2.4398F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.4474F, -0.2164F, 0.6619F, 0.5037F, 0.5272F, 0.0517F));
        last_bone.addOrReplaceChild("last_bone_r2", CubeListBuilder.create().texOffs(0, 7).addBox(-0.0041F, -1.6622F, -4.4628F, 0.0F, 7.0F, 33.0F)
                .texOffs(43, 26).addBox(-1.0041F, -0.6622F, -1.4628F, 2.0F, 3.0F, 23.0F), PartPose.offsetAndRotation(0.4474F, -0.2164F, 0.6619F, -1.1506F, 0.5117F, 0.0062F));
        PartDefinition front_left_leg = legs.addOrReplaceChild("front_left_leg", CubeListBuilder.create(), PartPose.offset(3.0F, -13.0F, -0.6F));
        front_left_leg.addOrReplaceChild("front_left_leg_r1", CubeListBuilder.create().texOffs(55, 58).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.3F))
                .texOffs(33, 85).addBox(-1.0F, -2.0F, -6.0F, 2.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -0.4F, 0.6129F, -0.7368F, -0.0799F));
        PartDefinition bone2 = front_left_leg.addOrReplaceChild("bone2", CubeListBuilder.create(), PartPose.offset(3.5F, 4.0F, -4.0F));
        bone2.addOrReplaceChild("bone2_r1", CubeListBuilder.create().texOffs(56, 19).addBox(-1.2F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.5F))
                .texOffs(24, 19).addBox(-0.2F, 1.0F, -10.0F, 0.0F, 2.0F, 12.0F)
                .texOffs(22, 71).addBox(-1.2F, -1.0F, -10.0F, 2.0F, 2.0F, 12.0F), PartPose.offsetAndRotation(0.5F, -1.0F, -0.4F, -0.5534F, -0.7256F, -0.1562F));
        PartDefinition bone3 = bone2.addOrReplaceChild("bone3", CubeListBuilder.create(), PartPose.offset(4.7F, -7.0F, -6.4F));
        bone3.addOrReplaceChild("bone3_r1", CubeListBuilder.create().texOffs(0, 29).addBox(-0.6F, -2.0F, -19.0F, 0.0F, 5.0F, 23.0F)
                .texOffs(34, 47).addBox(-1.6F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.3F))
                .texOffs(70, 26).addBox(-1.6F, -1.0F, -12.0F, 2.0F, 2.0F, 13.0F), PartPose.offsetAndRotation(0.8F, 0.0F, 0.0F, 1.1281F, -0.7405F, -0.1584F));
        PartDefinition front_right_leg = legs.addOrReplaceChild("front_right_leg", CubeListBuilder.create(), PartPose.offset(-3.0F, -13.0F, -0.6F));
        front_right_leg.addOrReplaceChild("front_right_leg_r1", CubeListBuilder.create().texOffs(8, 10).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.3F))
                .texOffs(0, 0).addBox(-1.0F, -2.0F, -6.0F, 2.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -0.4F, 0.6129F, 0.7368F, 0.0799F));
        PartDefinition bone4 = front_right_leg.addOrReplaceChild("bone4", CubeListBuilder.create(), PartPose.offset(-3.5F, 4.0F, -4.0F));
        bone4.addOrReplaceChild("bone4_r1", CubeListBuilder.create().texOffs(46, 26).addBox(-0.8F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.5F))
                .texOffs(0, 19).addBox(0.2F, 1.0F, -10.0F, 0.0F, 2.0F, 12.0F)
                .texOffs(70, 52).addBox(-0.8F, -1.0F, -10.0F, 2.0F, 2.0F, 12.0F), PartPose.offsetAndRotation(-0.5F, -1.0F, -0.4F, -0.5534F, 0.7256F, 0.1562F));
        PartDefinition bone5 = bone4.addOrReplaceChild("bone5", CubeListBuilder.create(), PartPose.offset(-4.7F, -7.0F, -6.4F));
        bone5.addOrReplaceChild("bone5_r1", CubeListBuilder.create().texOffs(0, 3).addBox(0.6F, -2.0F, -19.0F, 0.0F, 5.0F, 23.0F)
                .texOffs(26, 47).addBox(-0.4F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.3F))
                .texOffs(70, 0).addBox(-0.4F, -1.0F, -12.0F, 2.0F, 2.0F, 13.0F), PartPose.offsetAndRotation(-0.8F, 0.0F, 0.0F, 1.1281F, 0.7405F, 0.1584F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offset(0.0F, -10.0F, -8.4F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0F, 7.4F));
        head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(50, 74).addBox(-2.2168F, -1.2746F, -4.3725F, 6.0F, 6.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.8229F, 0.6656F, 1.5791F));
        PartDefinition pincles = head.addOrReplaceChild("pincles", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 3.0F, -3.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition front_middle_pincle = pincles.addOrReplaceChild("front_middle_pincle", CubeListBuilder.create(), PartPose.offset(0.0F, 0.1252F, 0.1457F));
        front_middle_pincle.addOrReplaceChild("front_middle_pincle_r1", CubeListBuilder.create().texOffs(32, 21).addBox(-2.0F, 0.0003F, 0.055F, 4.0F, 5.0F, 0.0F), PartPose.offsetAndRotation(0.0F, 3.5F, -5.0F, -2.4871F, 0.0F, 0.0F));
        front_middle_pincle.addOrReplaceChild("front_middle_pincle_r2", CubeListBuilder.create().texOffs(25, 57).addBox(-2.0F, -0.762F, -0.1826F, 4.0F, 6.0F, 0.0F), PartPose.offsetAndRotation(0.0F, 0.5F, -0.7F, -0.9163F, 0.0F, 0.0F));
        PartDefinition front_right_pincle = pincles.addOrReplaceChild("front_right_pincle", CubeListBuilder.create(), PartPose.offset(3.5F, -0.3748F, 0.1457F));
        front_right_pincle.addOrReplaceChild("front_right_pincle_r1", CubeListBuilder.create().texOffs(50, 52).addBox(1.0F, 0.0F, 0.0F, 4.0F, 9.0F, 0.0F), PartPose.offsetAndRotation(-3.5F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));
        PartDefinition front_left_pincle = pincles.addOrReplaceChild("front_left_pincle", CubeListBuilder.create(), PartPose.offset(-3.5F, -0.3748F, 0.1457F));
        front_left_pincle.addOrReplaceChild("front_left_pincle_r1", CubeListBuilder.create().texOffs(0, 57).addBox(-5.0F, 0.0F, 0.0F, 4.0F, 9.0F, 0.0F), PartPose.offsetAndRotation(3.5F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));
        PartDefinition face = head.addOrReplaceChild("face", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -5.0F));
        face.addOrReplaceChild("face_r1", CubeListBuilder.create().texOffs(24, 0).addBox(-8.011F, -5.1785F, -7.0742F, 12.0F, 0.0F, 18.0F)
                .texOffs(6, 110).addBox(-8.0352F, -5.1471F, -7.0438F, 12.0F, 0.0F, 18.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, 0.8021F, 0.504F, -0.437F));
        face.addOrReplaceChild("face_r2", CubeListBuilder.create().texOffs(-18, 110).addBox(-3.9648F, -5.1471F, -7.0438F, 12.0F, 0.0F, 18.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, 0.8021F, -0.504F, 0.437F));
        face.addOrReplaceChild("face_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0131F, -5.21F, -7.1047F, 12.0F, 0.0F, 18.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 5.05F, 0.8021F, -0.504F, 0.437F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(VoidlingRenderer.State state) {
        super.setupAnim(state);
        float walking = Math.min(1, state.walkAnimationSpeed * 2.5F);
        // Each voidling keeps its own idle at its own phase, so a pack never twitches in step.
        long idleTime = (long) (state.ageInTicks * 50) + state.idleVariant * 997L;
        idles[Math.floorMod(state.idleVariant, idles.length)].apply(idleTime, 1 - walking);
        walk.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 2.0F, 2.5F);
        head.yRot += Math.round(state.yRot / HEAD_STEP) * HEAD_STEP * Mth.DEG_TO_RAD * 0.5F;
        head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.4F;

        // A bite: rear up and lunge over the swing.
        if (state.bite > 0) {
            attack.apply((long) (state.bite * ATTACK_MILLIS), 1);
        }
        // Just pushed out of a breach: curled up, it unfolds.
        if (state.emerge > 0) {
            emerge.apply((long) ((1 - state.emerge) * Voidling.EMERGE_TICKS * 50), 1);
        }
        if (state.moltTime >= 0) {
            molt.apply((long) (state.moltTime * Voidling.MOLT_TICKS * 50), 1);
        }
    }
}
