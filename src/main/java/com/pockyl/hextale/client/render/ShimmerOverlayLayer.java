package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.animal.feline.AdultCatModel;
import net.minecraft.client.model.animal.feline.AdultFelineModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.CamelRenderer;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.DonkeyRenderer;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ShimmerMutations;

/**
 * The veins and pink eyes of a mutated animal: the animal's own model drawn again with an overlay sheet, then with its
 * glowing eyes. The sheets fit adult models, and only adults can be dosed. A Panther's saddle is a sheet of its own on a
 * copy of the cat's body, a little larger.
 */
public class ShimmerOverlayLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    public static final ContextKey<Boolean> MUTATED = new ContextKey<>(Hextale.id("shimmer_mutated"));
    public static final ContextKey<Boolean> SADDLE = new ContextKey<>(Hextale.id("shimmer_saddle"));
    private static final Identifier SADDLE_SHEET = Hextale.id("textures/entity/shimmer/panther_saddle.png");
    /** How far the saddle stands off the cat's body, in model pixels. */
    private static final float SADDLE_LIFT = 0.15F;
    private static final int ORDER = 1;
    private final Identifier overlay;
    private final Identifier glow;

    public ShimmerOverlayLayer(RenderLayerParent<S, M> renderer, String sheet) {
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
        MeshDefinition cat = AdultFelineModel.createBodyMesh(new CubeDeformation(SADDLE_LIFT));
        PartDefinition root = cat.getRoot();
        for (String part : new String[]{"head", "tail1", "tail2", "left_hind_leg", "right_hind_leg", "left_front_leg", "right_front_leg"}) {
            root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(cat, 64, 32).apply(AdultCatModel.CAT_TRANSFORMER).bakeRoot();
    }

    private static void mark(LivingEntity entity, LivingEntityRenderState state) {
        state.setRenderData(MUTATED, ShimmerMutations.of(entity) == null ? null : Boolean.TRUE);
    }

    public static void registerModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(WolfRenderer.class, ShimmerOverlayLayer::mark);
        event.registerEntityModifier(ParrotRenderer.class, (Parrot parrot, ParrotRenderState state) -> {
            mark(parrot, state);
            ShimmerPose.raptor(parrot, state);
        });
        event.registerEntityModifier(HorseRenderer.class, ShimmerOverlayLayer::mark);
        event.registerEntityModifier(CamelRenderer.class, ShimmerOverlayLayer::mark);
        event.registerEntityModifier(new com.google.common.reflect.TypeToken<DonkeyRenderer<?>>() {
        }, ShimmerOverlayLayer::mark);
        event.registerEntityModifier(CatRenderer.class, (Cat cat, CatRenderState state) -> {
            mark(cat, state);
            state.setRenderData(SADDLE, cat.getItemBySlot(EquipmentSlot.SADDLE).isEmpty() ? null : Boolean.TRUE);
            ShimmerPose.panther(cat, state);
        });
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
        if (state.getRenderData(MUTATED) == null || state.isBaby || state.isInvisible) {
            return;
        }
        renderColoredCutoutModel(getParentModel(), overlay, poseStack, collector, lightCoords, state, -1, ORDER);
        collector.order(ORDER + 1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(glow), lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
    }

    /** The Panther's saddle: the cat's body once more, painted only where the saddle lies. */
    private static final class SaddleLayer extends RenderLayer<CatRenderState, AbstractFelineModel<CatRenderState>> {
        private final AdultCatModel model = new AdultCatModel(saddleRoot());

        SaddleLayer(RenderLayerParent<CatRenderState, AbstractFelineModel<CatRenderState>> renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, CatRenderState state, float yRot, float xRot) {
            if (state.getRenderData(SADDLE) != null && !state.isBaby) {
                coloredCutoutModelCopyLayerRender(model, SADDLE_SHEET, poseStack, collector, lightCoords, state, -1, ORDER + 2);
            }
        }
    }
}
