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

/**
 * The Seer: one eye the size of a player's head - a pale lilac organ with a magenta-violet iris and a slit pupil - in a
 * cage of six thin curved chitin petals rooted in a knot of flesh behind it, four tendrils dangling from the knot. The
 * eye turns on its own toward whatever the Seer looks at; the cage opens and shuts round it. Made in Blockbench
 * ({@code art/models/entity/seer.bbmodel}, Modded Entity, Mojang mappings); its keyframe animations
 * ({@link SeerAnimations}) play as vanilla ones: one of two idles at the Seer's own phase (calmer while it stares), the
 * open pose weighed by the petals' clock, the mark by the beam's strength, the trailing tendrils by its speed.
 */
public class SeerModel extends EntityModel<SeerRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Hextale.id("seer"), "main");
    private static final long OPEN_MILLIS = 500L;
    private static final long MARK_MILLIS = 250L;
    private static final long DRIFT_MILLIS = 500L;
    private final ModelPart eye;
    private final KeyframeAnimation[] idles;
    private final KeyframeAnimation open;
    private final KeyframeAnimation mark;
    private final KeyframeAnimation drift;

    public SeerModel(ModelPart root) {
        super(root);
        eye = root.getChild("body").getChild("eye");
        idles = new KeyframeAnimation[] {SeerAnimations.IDLE.bake(root), SeerAnimations.IDLE_2.bake(root)};
        open = SeerAnimations.OPEN.bake(root);
        mark = SeerAnimations.MARK.bake(root);
        drift = SeerAnimations.DRIFT.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 17.0F, 0.0F));
        PartDefinition eye = body.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.5F, -3.5F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(29, 15).addBox(-4.5F, -2.5F, -2.5F, 9.0F, 5.0F, 5.0F, new CubeDeformation(0.02F))
                .texOffs(29, 0).addBox(-2.5F, -4.5F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.04F))
                .texOffs(0, 15).addBox(-2.5F, -2.5F, -4.5F, 5.0F, 5.0F, 9.0F, new CubeDeformation(0.06F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition pupil = eye.addOrReplaceChild("pupil", CubeListBuilder.create().texOffs(33, 84).addBox(-0.5F, -2.0F, -0.02F, 1.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -4.6F));
        PartDefinition socket = body.addOrReplaceChild("socket", CubeListBuilder.create().texOffs(0, 30).addBox(-2.5F, -2.5F, -1.0F, 5.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(5, 76).addBox(-1.5F, -1.5F, 2.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offset(0.0F, 0.0F, 4.0F));
        PartDefinition petal_0 = socket.addOrReplaceChild("petal_0", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.5236F));
        PartDefinition petal_0_a = petal_0.addOrReplaceChild("petal_0_a", CubeListBuilder.create().texOffs(24, 30).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.5F, -0.4538F, 0.0F, 0.0F));
        PartDefinition petal_0_b = petal_0_a.addOrReplaceChild("petal_0_b", CubeListBuilder.create().texOffs(43, 30).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.22F)), PartPose.offsetAndRotation(0.0F, 0.0F, -6.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition petal_0_c = petal_0_b.addOrReplaceChild("petal_0_c", CubeListBuilder.create().texOffs(23, 76).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.5F, 0.6981F, 0.0F, 0.0F));
        PartDefinition petal_1 = socket.addOrReplaceChild("petal_1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.5708F));
        PartDefinition petal_1_a = petal_1.addOrReplaceChild("petal_1_a", CubeListBuilder.create().texOffs(0, 40).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.5F, -0.4538F, 0.0F, 0.0F));
        PartDefinition petal_1_b = petal_1_a.addOrReplaceChild("petal_1_b", CubeListBuilder.create().texOffs(19, 40).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.22F)), PartPose.offsetAndRotation(0.0F, 0.0F, -6.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition petal_1_c = petal_1_b.addOrReplaceChild("petal_1_c", CubeListBuilder.create().texOffs(34, 76).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.5F, 0.6981F, 0.0F, 0.0F));
        PartDefinition petal_2 = socket.addOrReplaceChild("petal_2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 2.618F));
        PartDefinition petal_2_a = petal_2.addOrReplaceChild("petal_2_a", CubeListBuilder.create().texOffs(38, 40).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.5F, -0.4538F, 0.0F, 0.0F));
        PartDefinition petal_2_b = petal_2_a.addOrReplaceChild("petal_2_b", CubeListBuilder.create().texOffs(0, 49).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.22F)), PartPose.offsetAndRotation(0.0F, 0.0F, -6.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition petal_2_c = petal_2_b.addOrReplaceChild("petal_2_c", CubeListBuilder.create().texOffs(45, 76).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.5F, 0.6981F, 0.0F, 0.0F));
        PartDefinition petal_3 = socket.addOrReplaceChild("petal_3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 3.6652F));
        PartDefinition petal_3_a = petal_3.addOrReplaceChild("petal_3_a", CubeListBuilder.create().texOffs(19, 49).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.5F, -0.4538F, 0.0F, 0.0F));
        PartDefinition petal_3_b = petal_3_a.addOrReplaceChild("petal_3_b", CubeListBuilder.create().texOffs(38, 49).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.22F)), PartPose.offsetAndRotation(0.0F, 0.0F, -6.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition petal_3_c = petal_3_b.addOrReplaceChild("petal_3_c", CubeListBuilder.create().texOffs(0, 84).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.5F, 0.6981F, 0.0F, 0.0F));
        PartDefinition petal_4 = socket.addOrReplaceChild("petal_4", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 4.7124F));
        PartDefinition petal_4_a = petal_4.addOrReplaceChild("petal_4_a", CubeListBuilder.create().texOffs(0, 58).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.5F, -0.4538F, 0.0F, 0.0F));
        PartDefinition petal_4_b = petal_4_a.addOrReplaceChild("petal_4_b", CubeListBuilder.create().texOffs(19, 58).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.22F)), PartPose.offsetAndRotation(0.0F, 0.0F, -6.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition petal_4_c = petal_4_b.addOrReplaceChild("petal_4_c", CubeListBuilder.create().texOffs(11, 84).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.5F, 0.6981F, 0.0F, 0.0F));
        PartDefinition petal_5 = socket.addOrReplaceChild("petal_5", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 5.7596F));
        PartDefinition petal_5_a = petal_5.addOrReplaceChild("petal_5_a", CubeListBuilder.create().texOffs(38, 58).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.2F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.5F, -0.4538F, 0.0F, 0.0F));
        PartDefinition petal_5_b = petal_5_a.addOrReplaceChild("petal_5_b", CubeListBuilder.create().texOffs(0, 67).addBox(-1.0F, -0.5F, -6.5F, 2.0F, 1.0F, 7.0F, new CubeDeformation(-0.22F)), PartPose.offsetAndRotation(0.0F, 0.0F, -6.0F, 0.5934F, 0.0F, 0.0F));
        PartDefinition petal_5_c = petal_5_b.addOrReplaceChild("petal_5_c", CubeListBuilder.create().texOffs(22, 84).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.5F, 0.6981F, 0.0F, 0.0F));
        PartDefinition tendril_0 = socket.addOrReplaceChild("tendril_0", CubeListBuilder.create().texOffs(24, 67).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(1.5F, 2.0F, 2.0F, 0.2094F, 0.0F, -0.1047F));
        PartDefinition tendril_0_2 = tendril_0.addOrReplaceChild("tendril_0_2", CubeListBuilder.create().texOffs(29, 67).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.18F)), PartPose.offsetAndRotation(0.0F, 4.5F, 0.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition tendril_0_3 = tendril_0_2.addOrReplaceChild("tendril_0_3", CubeListBuilder.create().texOffs(18, 76).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(-0.26F)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.1396F, 0.0F, 0.0F));
        PartDefinition tendril_1 = socket.addOrReplaceChild("tendril_1", CubeListBuilder.create().texOffs(34, 67).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-0.5F, 2.0F, 2.5F, 0.2793F, 0.0F, 0.0349F));
        PartDefinition tendril_1_2 = tendril_1.addOrReplaceChild("tendril_1_2", CubeListBuilder.create().texOffs(39, 67).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.18F)), PartPose.offsetAndRotation(0.0F, 4.5F, 0.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition tendril_1_3 = tendril_1_2.addOrReplaceChild("tendril_1_3", CubeListBuilder.create().texOffs(44, 67).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.26F)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.1396F, 0.0F, 0.0F));
        PartDefinition tendril_2 = socket.addOrReplaceChild("tendril_2", CubeListBuilder.create().texOffs(49, 67).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.5F, 2.0F, 2.0F, 0.3491F, 0.0F, -0.0349F));
        PartDefinition tendril_2_2 = tendril_2.addOrReplaceChild("tendril_2_2", CubeListBuilder.create().texOffs(54, 67).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.18F)), PartPose.offsetAndRotation(0.0F, 4.5F, 0.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition tendril_2_3 = tendril_2_2.addOrReplaceChild("tendril_2_3", CubeListBuilder.create().texOffs(19, 67).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(-0.26F)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.1396F, 0.0F, 0.0F));
        PartDefinition tendril_3 = socket.addOrReplaceChild("tendril_3", CubeListBuilder.create().texOffs(59, 67).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-1.5F, 2.0F, 2.5F, 0.4189F, 0.0F, 0.1047F));
        PartDefinition tendril_3_2 = tendril_3.addOrReplaceChild("tendril_3_2", CubeListBuilder.create().texOffs(0, 76).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(-0.18F)), PartPose.offsetAndRotation(0.0F, 4.5F, 0.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition tendril_3_3 = tendril_3_2.addOrReplaceChild("tendril_3_3", CubeListBuilder.create().texOffs(19, 30).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(-0.26F)), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.1396F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 128);
    }

    @Override
    public void setupAnim(SeerRenderer.State state) {
        super.setupAnim(state);
        long ownTime = (long) (state.ageInTicks * 50) + state.idleVariant * 1777L;
        // While it stares the eye holds on its prey: the idle's darting fades out with the opening.
        idles[Math.floorMod(state.idleVariant, idles.length)].apply(ownTime, 1 - 0.8F * state.open);
        // The cage's pose follows the petals' clock both ways: opening and shutting are the same pose, weighed.
        if (state.open > 0) {
            open.apply(OPEN_MILLIS, state.open);
        }
        float drifting = Math.min(1, state.walkAnimationSpeed * 3);
        if (drifting > 0) {
            drift.apply(DRIFT_MILLIS, drifting);
        }
        if (state.marking > 0) {
            mark.apply(MARK_MILLIS, state.marking);
        }
        eye.yRot += state.yRot * Mth.DEG_TO_RAD;
        eye.xRot += state.xRot * Mth.DEG_TO_RAD;
    }
}
