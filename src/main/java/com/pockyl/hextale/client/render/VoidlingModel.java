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
 * The voidling: a glossy pointed dome with three eyes over a skull of a mouth - a row of long fangs and two tusks
 * hanging from it - carried high on four long scythe legs that arch above it, a fringe of violet barbs along their
 * insides. Geometry from the Blockbench project {@code art/models/entity/voidling.bbmodel} (Modded Entity, exported with
 * Mojang mappings). It scuttles on two alternating pairs of legs, the jaw drops and the tusks spread when it bites, and
 * right after a breach pushes it out it hangs with its legs folded and unfolds them.
 */
public class VoidlingModel extends EntityModel<VoidlingRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Hextale.id("voidling"), "main");
    private static final String[] LEGS = {"right_front", "left_front", "right_hind", "left_hind"};
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart rightTusk;
    private final ModelPart leftTusk;
    /** Right front, left front, right hind, left hind. */
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] shins = new ModelPart[4];
    private final ModelPart[] claws = new ModelPart[4];

    public VoidlingModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        jaw = head.getChild("jaw");
        rightTusk = head.getChild("right_tusk");
        leftTusk = head.getChild("left_tusk");
        for (int i = 0; i < 4; i++) {
            legs[i] = body.getChild(LEGS[i] + "_leg");
            shins[i] = legs[i].getChild(LEGS[i] + "_shin");
            claws[i] = shins[i].getChild(LEGS[i] + "_claw");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 4.0F, 9.0F)
                .texOffs(29, 30).addBox(-3.0F, -5.0F, -4.0F, 6.0F, 2.0F, 7.0F)
                .texOffs(15, 41).addBox(-2.0F, -7.0F, -3.0F, 4.0F, 2.0F, 4.0F)
                .texOffs(29, 51).addBox(-1.0F, -9.0F, -2.5F, 2.0F, 2.0F, 2.0F)
                .texOffs(32, 41).addBox(-3.0F, -3.0F, 4.0F, 6.0F, 3.0F, 3.0F)
                .texOffs(38, 51).addBox(-2.0F, -3.0F, 7.0F, 4.0F, 2.0F, 2.0F)
                .texOffs(0, 30).addBox(-3.0F, 1.0F, -3.0F, 6.0F, 2.0F, 8.0F), PartPose.offset(0.0F, 15.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 51).addBox(-3.0F, -0.5F, -2.5F, 6.0F, 3.0F, 3.0F)
                .texOffs(46, 63).addBox(-3.0F, 2.0F, -2.6F, 6.0F, 3.0F, 0.0F), PartPose.offset(0.0F, 1.0F, -3.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 68).addBox(-2.5F, 0.0F, -2.0F, 5.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 3.0F, 0.0F));
        head.addOrReplaceChild("right_tusk", CubeListBuilder.create().texOffs(19, 51).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, 1.0F, -2.0F, 0.2618F, 0.0F, -0.1047F));
        head.addOrReplaceChild("left_tusk", CubeListBuilder.create().texOffs(24, 51).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(3.0F, 1.0F, -2.0F, 0.2618F, 0.0F, 0.1047F));
        leg(body, "right_front", -1, -2.5F, 0, 58, 15, 68, 0, 0, 35, 16, 56, 30);
        leg(body, "left_front", 1, -2.5F, 23, 58, 32, 68, 9, 0, 40, 16, 0, 41);
        leg(body, "right_hind", -1, 2.5F, 0, 63, 0, 72, 18, 0, 45, 16, 5, 41);
        leg(body, "left_hind", 1, 2.5F, 23, 63, 17, 72, 27, 0, 50, 16, 10, 41);
        return LayerDefinition.create(mesh, 64, 128);
    }

    /**
     * A leg on one side (-1 right, 1 left) at {@code z}: the thigh raised steeply out of the body with its barbed fringe,
     * the long shin with its fringe, the scythe tip curving in; front legs lean forward, hind legs back.
     */
    private static void leg(PartDefinition body, String name, int side, float z, int thighU, int thighV, int thighFringeU, int thighFringeV,
                            int shinU, int shinV, int shinFringeU, int shinFringeV, int clawU, int clawV) {
        float lean = z < 0 ? 0.3491F : -0.3491F;
        PartDefinition thigh = body.addOrReplaceChild(name + "_leg", CubeListBuilder.create()
                        .texOffs(thighU, thighV).addBox(side < 0 ? -9.0F : 0.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F)
                        .texOffs(thighFringeU, thighFringeV).addBox(side < 0 ? -8.5F : 0.5F, 1.0F, 0.0F, 8.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(4.0F * side, -1.0F, z, lean, 0.0F, -1.2217F * side));
        PartDefinition shin = thigh.addOrReplaceChild(name + "_shin", CubeListBuilder.create()
                        .texOffs(shinU, shinV).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 13.0F, 2.0F)
                        .texOffs(shinFringeU, shinFringeV).addBox(side < 0 ? 1.0F : -3.0F, 0.0F, 0.0F, 2.0F, 12.0F, 0.0F),
                PartPose.offsetAndRotation(8.5F * side, 0.0F, 0.0F, -1.75F * lean, 0.0F, 0.9599F * side));
        shin.addOrReplaceChild(name + "_claw", CubeListBuilder.create().texOffs(clawU, clawV).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 12.5F, 0.0F, 0.0F, 0.0F, 0.5236F * side));
    }

    @Override
    public void setupAnim(VoidlingRenderer.State state) {
        super.setupAnim(state);
        float walk = state.walkAnimationPos * 1.3F;
        float amount = Math.min(1, state.walkAnimationSpeed * 1.6F);
        float age = state.ageInTicks;
        for (int i = 0; i < 4; i++) {
            // Diagonal pairs step together: right front with left hind. A stepping leg swings and lifts.
            float phase = walk + (i == 0 || i == 3 ? 0 : Mth.PI);
            float side = i % 2 == 0 ? -1 : 1;
            legs[i].yRot += Mth.cos(phase) * 0.4F * amount;
            legs[i].zRot -= Math.max(0, Mth.sin(phase)) * 0.35F * amount * side;
            // At rest the tips twitch a little, testing the ground.
            claws[i].zRot += Mth.sin(age * 0.45F + i * 1.9F) * 0.05F * side;
        }
        body.y -= Math.abs(Mth.sin(walk)) * 0.5F * amount;
        body.y += Mth.sin(age * 0.1F) * 0.25F;
        head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.4F;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.3F;

        // The mouth: a slow working of the jaw, gaping in a bite with the tusks spreading.
        float bite = Mth.sin(state.bite * Mth.PI);
        jaw.xRot = 0.05F + 0.05F * Mth.sin(age * 0.2F) + bite * 0.9F;
        rightTusk.zRot -= bite * 0.45F;
        leftTusk.zRot += bite * 0.45F;
        rightTusk.xRot -= bite * 0.35F;
        leftTusk.xRot -= bite * 0.35F;

        // Just pushed out of a breach: the legs folded up over the dome, shins tucked in - and unfolding.
        float curl = state.emerge * state.emerge;
        if (curl > 0) {
            for (int i = 0; i < 4; i++) {
                float side = i % 2 == 0 ? -1 : 1;
                legs[i].zRot -= 0.35F * curl * side;
                shins[i].zRot -= 1.2F * curl * side;
                claws[i].zRot -= 0.8F * curl * side;
            }
            jaw.xRot += 0.4F * curl;
        }
    }
}
