package com.pockyl.hextale.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import com.pockyl.hextale.Hextale;

/**
 * The voidling: a hunched shell with three hooked blades on the back, a crested head with a jaw that opens wide and two
 * mandibles, a ringed tail and four sharp legs. Geometry from the Blockbench project
 * {@code art/models/entity/voidling.bbmodel} (Modded Entity, exported with Mojang mappings). It scuttles on two
 * alternating pairs of legs, the jaw gapes when it bites, and right after a breach pushes it out it hangs curled up and
 * unfolds.
 */
public class VoidlingModel extends EntityModel<VoidlingRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Hextale.id("voidling"), "main");
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart rightMandible;
    private final ModelPart leftMandible;
    private final ModelPart tail;
    /** Right front, left front, right hind, left hind. */
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] shins = new ModelPart[4];

    public VoidlingModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        jaw = head.getChild("jaw");
        rightMandible = head.getChild("right_mandible");
        leftMandible = head.getChild("left_mandible");
        tail = body.getChild("tail");
        String[] names = {"right_front", "left_front", "right_hind", "left_hind"};
        for (int i = 0; i < 4; i++) {
            legs[i] = body.getChild(names[i] + "_leg");
            shins[i] = legs[i].getChild(names[i] + "_shin");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(13, 0).addBox(-3.5F, -2.0F, -4.0F, 7.0F, 5.0F, 6.0F)
                .texOffs(0, 34).addBox(-4.0F, -3.0F, -4.5F, 8.0F, 2.0F, 6.0F)
                .texOffs(0, 0).addBox(0.0F, -8.0F, -3.0F, 0.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 17.0F, 0.0F));
        body.addOrReplaceChild("spike_left_r1", CubeListBuilder.create().texOffs(36, 13).addBox(0.0F, -3.5F, -2.0F, 0.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(2.5F, -2.5F, 1.0F, 0.0F, 0.0F, 0.3491F));
        body.addOrReplaceChild("spike_right_r1", CubeListBuilder.create().texOffs(25, 13).addBox(0.0F, -3.5F, -2.0F, 0.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(-2.5F, -2.5F, 1.0F, 0.0F, 0.0F, -0.3491F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-3.0F, -2.0F, -4.5F, 6.0F, 4.0F, 5.0F)
                .texOffs(36, 43).addBox(-3.5F, -3.0F, -4.0F, 7.0F, 2.0F, 4.0F)
                .texOffs(54, 34).addBox(0.0F, -5.0F, -4.0F, 0.0F, 3.0F, 4.0F), PartPose.offset(0.0F, 0.0F, -4.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 43).addBox(-2.5F, 0.0F, -5.0F, 5.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, 1.5F, 0.0F));
        head.addOrReplaceChild("right_mandible", CubeListBuilder.create()
                .texOffs(0, 51).addBox(-0.5F, -0.5F, -2.5F, 1.0F, 1.0F, 3.0F)
                .texOffs(44, 56).addBox(0.5F, -0.5F, -3.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-2.5F, 2.0F, -4.5F));
        head.addOrReplaceChild("left_mandible", CubeListBuilder.create()
                .texOffs(9, 51).addBox(-0.5F, -0.5F, -2.5F, 1.0F, 1.0F, 3.0F)
                .texOffs(49, 56).addBox(-1.5F, -0.5F, -3.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(2.5F, 2.0F, -4.5F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 13).addBox(-3.0F, -1.0F, -0.5F, 6.0F, 4.0F, 6.0F)
                .texOffs(29, 34).addBox(-3.5F, -2.0F, 0.0F, 7.0F, 2.0F, 5.0F)
                .texOffs(21, 43).addBox(-1.5F, 0.5F, 5.0F, 3.0F, 2.0F, 4.0F)
                .texOffs(47, 13).addBox(0.0F, -3.0F, 5.0F, 0.0F, 4.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 2.0F));
        leg(body, "right_front", 18, 51, 23, 24, 35, 51, -1, -2.5F, -0.4363F);
        leg(body, "left_front", 40, 51, 32, 24, 57, 51, 1, -2.5F, 0.4363F);
        leg(body, "right_hind", 0, 56, 41, 24, 17, 56, -1, 2.0F, 0.4363F);
        leg(body, "left_hind", 22, 56, 50, 24, 39, 56, 1, 2.0F, -0.4363F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** A leg on one side (-1 right, 1 left): the upper part raised out of the body, the shin with its claw. */
    private static void leg(PartDefinition body, String name, int legU, int legV, int shinU, int shinV, int clawU, int clawV, int side, float z,
                            float yaw) {
        PartDefinition leg = body.addOrReplaceChild(name + "_leg", CubeListBuilder.create().texOffs(legU, legV)
                        .addBox(side < 0 ? -6.0F : 0.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F * side, 0.5F, z, 0.0F, yaw, -0.6109F * side));
        leg.addOrReplaceChild(name + "_shin", CubeListBuilder.create()
                        .texOffs(shinU, shinV).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 7.0F, 2.0F)
                        .texOffs(clawU, clawV).addBox(-0.5F, 6.5F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(5.5F * side, 0.0F, 0.0F, 0.0F, 0.0F, 0.4014F * side));
    }

    @Override
    public void setupAnim(VoidlingRenderer.State state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos * 1.5F;
        float amount = Math.min(1, state.walkAnimationSpeed * 1.6F);
        float age = state.ageInTicks;
        for (int i = 0; i < 4; i++) {
            // Diagonal pairs step together: right front with left hind.
            float phase = walk + (i == 0 || i == 3 ? 0 : Mth.PI);
            float side = i % 2 == 0 ? -1 : 1;
            legs[i].yRot += Mth.cos(phase) * 0.5F * amount * side;
            legs[i].zRot -= Math.max(0, Mth.sin(phase)) * 0.45F * amount * side;
            shins[i].zRot += Mth.sin(age * 0.5F + i * 1.9F) * 0.03F * side;
        }
        body.y -= Math.abs(Mth.sin(walk)) * 0.6F * amount;
        head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        tail.yRot = Mth.sin(age * 0.12F) * 0.08F + Mth.cos(walk) * 0.15F * amount;
        tail.xRot = Mth.sin(age * 0.09F) * 0.05F;

        // The jaw: a slow breath of opening and closing, gaping wide in a bite; the mandibles scissor with it.
        float bite = Mth.sin(state.bite * Mth.PI);
        jaw.xRot = 0.08F + 0.06F * Mth.sin(age * 0.2F) + bite * 0.9F;
        rightMandible.yRot = -0.15F - bite * 0.5F + Mth.sin(age * 0.3F) * 0.08F;
        leftMandible.yRot = 0.15F + bite * 0.5F - Mth.sin(age * 0.3F) * 0.08F;
        head.xRot -= bite * 0.25F;

        // Just pushed out of a breach: curled up - legs folded in under the shell, head and tail tucked - and unfolding.
        float curl = state.emerge * state.emerge;
        if (curl > 0) {
            for (int i = 0; i < 4; i++) {
                float side = i % 2 == 0 ? -1 : 1;
                legs[i].zRot += 1.3F * curl * side;
                shins[i].zRot += 1.1F * curl * side;
            }
            head.xRot += 0.6F * curl;
            tail.xRot -= 0.7F * curl;
            jaw.xRot += 0.4F * curl;
        }
    }
}
