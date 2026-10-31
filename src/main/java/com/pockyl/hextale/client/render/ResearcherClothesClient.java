package com.pockyl.hextale.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModItems;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * The researcher's clothes worn: instead of the flat armour shell every piece is its own 3D model built in Blockbench
 * (hood {@link ResearcherHoodLayer}, robe {@link ResearcherRobeLayer} with coat tails on the legs, trousers
 * {@link ResearcherTrousersLayer}, boots {@link ResearcherBootsLayer}), all painted on one texture. The same models are
 * the items' icons ({@link ResearcherClothesItemRenderer}).
 */
public final class ResearcherClothesClient {
    /** One texture for all four pieces, whichever armour layer asks for it. */
    public static final ResourceLocation TEXTURE = Hextale.id("textures/entity/equipment/humanoid/researcher_robe.png");

    /** A piece: its model layer, the item, and how its icon is framed (centre height and scale in model pixels). */
    public enum Piece {
        HOOD("researcher_hood", ResearcherHoodLayer::createLayer, () -> ModItems.RESEARCHER_HOOD.get(), -3.5F, 1.0F),
        ROBE("researcher_robe", ResearcherRobeLayer::createLayer, () -> ModItems.RESEARCHER_ROBE.get(), 9.5F, 0.55F),
        TROUSERS("researcher_trousers", ResearcherTrousersLayer::createLayer, () -> ModItems.RESEARCHER_TROUSERS.get(), 16.5F, 1.0F),
        BOOTS("researcher_boots", ResearcherBootsLayer::createLayer, () -> ModItems.RESEARCHER_BOOTS.get(), 20.5F, 1.15F);

        public final ModelLayerLocation layer;
        final Supplier<LayerDefinition> definition;
        final Supplier<Item> item;
        final float centreY;
        final float scale;

        Piece(String id, Supplier<LayerDefinition> definition, Supplier<Item> item, float centreY, float scale) {
            this.layer = new ModelLayerLocation(Hextale.id(id), "main");
            this.definition = definition;
            this.item = item;
            this.centreY = centreY;
            this.scale = scale;
        }
    }

    /** Worn models, baked again whenever the entity models are reloaded. */
    private static final Map<Piece, HumanoidModel<LivingEntity>> WORN = new EnumMap<>(Piece.class);

    private ResearcherClothesClient() {
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (Piece piece : Piece.values()) {
            event.registerLayerDefinition(piece.layer, piece.definition);
        }
    }

    public static void bake(EntityRenderersEvent.AddLayers event) {
        for (Piece piece : Piece.values()) {
            WORN.put(piece, new HumanoidModel<>(withRootHat(event.getEntityModels().bakeLayer(piece.layer))));
        }
    }

    /** 1.21.1's humanoid model wants the hat beside the head (26.x keeps it under the head): an empty one is added. */
    static net.minecraft.client.model.geom.ModelPart withRootHat(net.minecraft.client.model.geom.ModelPart root) {
        if (root.hasChild("hat")) {
            return root;
        }
        java.util.Map<String, net.minecraft.client.model.geom.ModelPart> children = new java.util.HashMap<>();
        for (String name : new String[]{"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            children.put(name, root.getChild(name));
        }
        children.put("hat", new net.minecraft.client.model.geom.ModelPart(java.util.List.of(), java.util.Map.of()));
        return new net.minecraft.client.model.geom.ModelPart(java.util.List.of(), children);
    }

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        for (Piece piece : Piece.values()) {
            com.pockyl.hextale.port.client.SpecialItemModels.claim(piece.item.get());
            event.registerItem(new IClientItemExtensions() {
                // 1.21.1: the icon's special model is drawn by the port's renderer, which needs this same registration.
                @Override
                public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return com.pockyl.hextale.port.client.SpecialItemModels.customRenderer(piece.item.get());
                }

                @Override
                public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                    @Nullable HumanoidModel<LivingEntity> model = WORN.get(piece);
                    return model != null && !entity.isBaby() ? model : original;
                }
            }, piece.item.get());
        }
    }
}
