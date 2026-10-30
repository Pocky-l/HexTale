package com.pockyl.hextale.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.runework.RuneTier;

/**
 * Bulwark armour worn: every piece is its own 3D model built in Blockbench (one generated layer per piece holding the
 * parts of all four levels - each level's sheet leaves the other levels' parts see-through), painted with the sheet of
 * the piece's level ({@code textures/entity/equipment/bulwark/<level>_<piece>.png}). The same models are the items'
 * icons ({@link BulwarkItemRenderer}); the cuirass's crystal heart glows by its charge ({@link BulwarkHeartLayer}).
 */
public final class BulwarkClient {
    /** A piece: its model layer, its four items, and how its icon is framed (centre height and scale in model pixels). */
    public enum Piece {
        HELMET("bulwark_helmet", BulwarkHelmetLayer::createLayer, -3.5F, 1.0F, List.of(ModItems.BULWARK_HELMET,
                ModItems.REFINED_BULWARK_HELMET, ModItems.RADIANT_BULWARK_HELMET, ModItems.RIFTFORGED_BULWARK_HELMET)),
        CHESTPLATE("bulwark_chestplate", BulwarkChestplateLayer::createLayer, 6F, 0.75F, List.of(ModItems.BULWARK_CHESTPLATE,
                ModItems.REFINED_BULWARK_CHESTPLATE, ModItems.RADIANT_BULWARK_CHESTPLATE, ModItems.RIFTFORGED_BULWARK_CHESTPLATE)),
        LEGGINGS("bulwark_leggings", BulwarkLeggingsLayer::createLayer, 16.5F, 0.9F, List.of(ModItems.BULWARK_LEGGINGS,
                ModItems.REFINED_BULWARK_LEGGINGS, ModItems.RADIANT_BULWARK_LEGGINGS, ModItems.RIFTFORGED_BULWARK_LEGGINGS)),
        BOOTS("bulwark_boots", BulwarkBootsLayer::createLayer, 20.5F, 1.1F, List.of(ModItems.BULWARK_BOOTS,
                ModItems.REFINED_BULWARK_BOOTS, ModItems.RADIANT_BULWARK_BOOTS, ModItems.RIFTFORGED_BULWARK_BOOTS));

        public final ModelLayerLocation layer;
        final Supplier<LayerDefinition> definition;
        final float centreY;
        final float scale;
        final List<? extends Supplier<? extends Item>> items;

        Piece(String id, Supplier<LayerDefinition> definition, float centreY, float scale, List<? extends Supplier<? extends Item>> items) {
            this.layer = new ModelLayerLocation(Hextale.id(id), "main");
            this.definition = definition;
            this.centreY = centreY;
            this.scale = scale;
            this.items = items;
        }

        String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final String[] LEVELS = {"base", "refined", "radiant", "riftforged"};

    /** Worn models, baked again whenever the entity models are reloaded. */
    private static final Map<Piece, HumanoidModel<HumanoidRenderState>> WORN = new EnumMap<>(Piece.class);
    /** The cuirass again, drawn with light only: its heart's glow. */
    @Nullable
    static HumanoidModel<HumanoidRenderState> heart;

    private BulwarkClient() {
    }

    /** The sheet a piece of this level is painted with. */
    public static Identifier texture(Piece piece, ItemStack stack) {
        return Hextale.id("textures/entity/equipment/bulwark/" + LEVELS[RuneTier.tier(stack) - 1] + "_" + piece.id() + ".png");
    }

    /** The glow sheet of the cuirass's heart (the riftforged heart is the Void's). */
    public static Identifier heartGlow(ItemStack stack) {
        return Hextale.id(RuneTier.tier(stack) >= 4 ? "textures/entity/equipment/bulwark/riftforged_heart_glow.png"
                : "textures/entity/equipment/bulwark/heart_glow.png");
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (Piece piece : Piece.values()) {
            event.registerLayerDefinition(piece.layer, piece.definition);
        }
    }

    public static void bake(EntityRenderersEvent.AddLayers event) {
        for (Piece piece : Piece.values()) {
            WORN.put(piece, new HumanoidModel<>(event.getEntityModels().bakeLayer(piece.layer)));
        }
        heart = new HumanoidModel<>(event.getEntityModels().bakeLayer(Piece.CHESTPLATE.layer), HexFx::glow);
    }

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        for (Piece piece : Piece.values()) {
            IClientItemExtensions worn = new IClientItemExtensions() {
                @Override
                public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
                    @Nullable HumanoidModel<HumanoidRenderState> model = WORN.get(piece);
                    return model != null && layerType != EquipmentClientInfo.LayerType.HUMANOID_BABY ? model : original;
                }

                @Override
                public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer,
                                                  Identifier fallback) {
                    return texture(piece, stack);
                }
            };
            event.registerItem(worn, piece.items.stream().map(Supplier::get).toArray(Item[]::new));
        }
    }
}
