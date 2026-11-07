package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import com.pockyl.hextale.port.client.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ShimmerMutations;
import com.pockyl.hextale.port.PortSaddle;

/**
 * The veins and pink eyes of a mutated animal: the animal's own model drawn again with an overlay sheet, then with its
 * glowing eyes. The sheets fit adult models, and only adults can be dosed. A Panther's saddle is a sheet of its own on a
 * copy of the cat's body, a little larger.
 *
 * <p>1.21.1: the layers read the entity (26.x: render state data set by render state modifiers).
 */
public class ShimmerOverlayLayer<T extends LivingEntity, M extends net.minecraft.client.model.EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation SADDLE_SHEET = Hextale.id("textures/entity/shimmer/panther_saddle.png");
    /** How far the saddle stands off the cat's body, in model pixels. */
    private static final float SADDLE_LIFT = 0.15F;
    private final ResourceLocation overlay;
    private final ResourceLocation glow;

    public ShimmerOverlayLayer(RenderLayerParent<T, M> renderer, String sheet) {
        super(renderer);
        overlay = Hextale.id("textures/entity/shimmer/" + sheet + ".png");
        glow = Hextale.id("textures/entity/shimmer/" + sheet + "_glow.png");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void add(EntityRenderersEvent.AddLayers event, EntityType<?> type, String sheet) {
        if (event.getRenderer(type) instanceof LivingEntityRenderer renderer) {
            renderer.addLayer(new ShimmerOverlayLayer(renderer, sheet));
        }
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        add(event, EntityType.WOLF, "wolf");
        add(event, EntityType.CAT, "cat");
        add(event, EntityType.PARROT, "parrot");
        add(event, EntityType.HORSE, "horse");
        add(event, EntityType.DONKEY, "horse");
        add(event, EntityType.MULE, "horse");
        add(event, EntityType.CAMEL, "camel");
        if (event.getRenderer(EntityType.CAT) instanceof CatRenderer cat) {
            cat.addLayer(new SaddleLayer(cat));
        }
    }

    /** The cat's parts with nothing left but the body, on the 64 x 32 sheet the cat's own texture has. */
    private static ModelPart saddleRoot() {
        MeshDefinition cat = OcelotModel.createBodyMesh(new CubeDeformation(SADDLE_LIFT));
        PartDefinition root = cat.getRoot();
        for (String part : new String[]{"head", "tail1", "tail2", "left_hind_leg", "right_hind_leg", "left_front_leg", "right_front_leg"}) {
            root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(cat, 64, 32).bakeRoot();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int lightCoords, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (ShimmerMutations.of(entity) == null || entity.isBaby() || entity.isInvisible()) {
            return;
        }
        renderColoredCutoutModel(getParentModel(), overlay, poseStack, buffers, lightCoords, entity, -1);
        getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderTypes.eyes(glow)), lightCoords, OverlayTexture.NO_OVERLAY);
    }

    /** The Panther's saddle: the cat's body once more, painted only where the saddle lies. */
    private static final class SaddleLayer extends RenderLayer<Cat, CatModel<Cat>> {
        private final CatModel<Cat> model = new CatModel<>(saddleRoot());

        SaddleLayer(RenderLayerParent<Cat, CatModel<Cat>> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int lightCoords, Cat cat, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (PortSaddle.has(cat) && !cat.isBaby()) {
                coloredCutoutModelCopyLayerRender(getParentModel(), model, SADDLE_SHEET, poseStack, buffers, lightCoords, cat, limbSwing, limbSwingAmount,
                        ageInTicks, netHeadYaw, headPitch, partialTick, -1);
            }
        }
    }
}
