package com.pockyl.hextale.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
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
import com.pockyl.hextale.runework.BulwarkArmorItem;
import com.pockyl.hextale.runework.RuneTier;

/**
 * Bulwark armour worn: every piece is its own 3D model built in Blockbench (one generated layer per piece holding the
 * parts of all four levels - each level's sheet leaves the other levels' parts see-through), painted with the sheet of
 * the piece's level ({@code textures/entity/equipment/bulwark/<level>_<piece>.png}). The same models are the items'
 * icons ({@link BulwarkItemRenderer}); the cuirass's crystal heart glows by its charge ({@link BulwarkHeartLayer}).
 *
 * <p>1.21.1: the sheet comes from {@link BulwarkArmorItem#getArmorTexture} (the item, not the client extensions), and
 * the icons' special model is drawn by the port's item renderer, handed out by these same extensions.
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

    /** Worn models, baked again whenever the entity models are reloaded. */
    private static final Map<Piece, HumanoidModel<LivingEntity>> WORN = new EnumMap<>(Piece.class);
    /** The cuirass again, drawn with light only: its heart's glow. */
    @Nullable
    static HumanoidModel<LivingEntity> heart;

    private BulwarkClient() {
    }

    /** The sheet a piece of this level is painted with. */
    public static ResourceLocation texture(Piece piece, ItemStack stack) {
        return BulwarkArmorItem.wornTexture(com.pockyl.hextale.port.ArmorType.valueOf(piece.name()), stack);
    }

    /** The glow sheet of the cuirass's heart (the riftforged heart is the Void's). */
    public static ResourceLocation heartGlow(ItemStack stack) {
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
            WORN.put(piece, new HumanoidModel<>(ResearcherClothesClient.withRootHat(event.getEntityModels().bakeLayer(piece.layer))));
        }
        heart = new HumanoidModel<>(ResearcherClothesClient.withRootHat(event.getEntityModels().bakeLayer(Piece.CHESTPLATE.layer)), HexFx::glow);
    }

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        for (Piece piece : Piece.values()) {
            Item[] items = piece.items.stream().map(Supplier::get).toArray(Item[]::new);
            for (Item item : items) {
                com.pockyl.hextale.port.client.SpecialItemModels.claim(item);
            }
            IClientItemExtensions worn = new IClientItemExtensions() {
                // 1.21.1: the icon's special model is drawn by the port's renderer, which needs this same registration.
                @Override
                public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return com.pockyl.hextale.port.client.SpecialItemModels.customRenderer(items[0]);
                }

                @Override
                public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                    @Nullable HumanoidModel<LivingEntity> model = WORN.get(piece);
                    return model != null && !entity.isBaby() ? model : original;
                }
            };
            event.registerItem(worn, items);
        }
    }
}
