package com.pockyl.hextale.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.TankFuel;
import com.pockyl.hextale.registry.ModItems;

/**
 * Chem-steel armour worn: every piece is its own 3D model built in Blockbench, all four painted on one sheet
 * ({@code textures/entity/equipment/humanoid/chem_steel.png}). The liquid behind their glass is a second sheet drawn
 * again in the colour of the fuel the wearer runs on ({@link ChemSteelFuelLayer}); an empty tank leaves the glass dark.
 * The same models are the items' icons ({@link ChemSteelItemRenderer}).
 */
public final class ChemSteelClient {
    public static final Identifier TEXTURE = Hextale.id("textures/entity/equipment/humanoid/chem_steel.png");
    public static final Identifier FUEL_TEXTURE = Hextale.id("textures/entity/equipment/humanoid/chem_steel_fuel.png");

    /** A piece: its model layer, its items, and how its icon is framed (centre height and scale in model pixels). */
    public enum Piece {
        HELMET("chem_steel_helmet", ChemSteelHelmetLayer::createLayer, -3.5F, 1.0F, List.of(ModItems.CHEM_STEEL_HELMET)),
        CHESTPLATE("chem_steel_chestplate", ChemSteelChestplateLayer::createLayer, 6F, 0.75F,
                List.of(ModItems.CHEM_STEEL_CHESTPLATE, ModItems.CHEM_STEEL_TANK_CHESTPLATE, ModItems.CHEM_JET_TANK_CHESTPLATE)),
        LEGGINGS("chem_steel_leggings", ChemSteelLeggingsLayer::createLayer, 16.5F, 0.9F, List.of(ModItems.CHEM_STEEL_LEGGINGS)),
        BOOTS("chem_steel_boots", ChemSteelBootsLayer::createLayer, 20.5F, 1.1F, List.of(ModItems.CHEM_STEEL_BOOTS));

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

        /** The piece this item is, or null. */
        static @Nullable Piece of(ItemStack stack) {
            for (Piece piece : values()) {
                for (Supplier<? extends Item> item : piece.items) {
                    if (stack.is(item.get())) {
                        return piece;
                    }
                }
            }
            return null;
        }
    }

    /** Worn models, baked again whenever the entity models are reloaded. */
    private static final Map<Piece, HumanoidModel<HumanoidRenderState>> WORN = new EnumMap<>(Piece.class);
    /** The same models for the fuel pass, drawn the armour way so they lie exactly over the worn ones. */
    static final Map<Piece, HumanoidModel<HumanoidRenderState>> FUEL = new EnumMap<>(Piece.class);

    private ChemSteelClient() {
    }

    /** The fuel's colour as an opaque tint, or 0 when there is none to show. */
    static int fuelTint(TankFuel fuel) {
        return fuel.isEmpty() ? 0 : ARGB.opaque(fuel.color());
    }

    /** Shimmer lights itself; ember oil does not. */
    static boolean glows(TankFuel fuel) {
        return !fuel.isEmpty() && fuel.fuel().brew() != Brew.EMBER_OIL;
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (Piece piece : Piece.values()) {
            event.registerLayerDefinition(piece.layer, piece.definition);
        }
    }

    public static void bake(EntityRenderersEvent.AddLayers event) {
        for (Piece piece : Piece.values()) {
            WORN.put(piece, new HumanoidModel<>(event.getEntityModels().bakeLayer(piece.layer)));
            FUEL.put(piece, new HumanoidModel<>(event.getEntityModels().bakeLayer(piece.layer), RenderTypes::armorCutoutNoCull));
        }
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
                    return type == EquipmentClientInfo.LayerType.HUMANOID_BABY ? fallback : TEXTURE;
                }
            };
            event.registerItem(worn, piece.items.stream().map(Supplier::get).toArray(Item[]::new));
        }
    }
}
