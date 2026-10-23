package com.pockyl.hextale.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModItems;

import org.jspecify.annotations.Nullable;

/**
 * The Researcher's Robe worn: instead of the flat armour shell, a 3D robe built in Blockbench
 * ({@link ResearcherRobeLayer}) - high collar, short cape, shoulder drapes, belt with a pouch, scroll and vial, and
 * coat tails hung on the legs so they swing with every step. Its texture is the robe's equipment layer.
 */
public final class ResearcherRobeClient {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Hextale.id("researcher_robe"), "main");
    /** Baked again whenever the entity models are reloaded. */
    private static @Nullable HumanoidModel<HumanoidRenderState> model;

    private ResearcherRobeClient() {
    }

    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, ResearcherRobeLayer::createLayer);
    }

    public static void bake(EntityRenderersEvent.AddLayers event) {
        model = new HumanoidModel<>(event.getEntityModels().bakeLayer(LAYER));
    }

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
                return model != null && layerType == EquipmentClientInfo.LayerType.HUMANOID ? model : original;
            }
        }, ModItems.RESEARCHER_ROBE.get());
    }
}
