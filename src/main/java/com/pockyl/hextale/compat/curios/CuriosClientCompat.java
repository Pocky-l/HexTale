package com.pockyl.hextale.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModItems;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Client side of the optional Curios support: goggles in a Curios slot are drawn on the head like worn ones. Over a
 * helmet they use a slightly larger head so the two do not flicker into each other. Only called when Curios is loaded.
 */
public final class CuriosClientCompat {
    /** The goggles' head over a helmet (vanilla outer armour is 1.0). */
    private static final ModelLayerLocation OVER_HELMET = new ModelLayerLocation(Hextale.id("curio_goggles"), "over_helmet");

    private CuriosClientCompat() {
    }

    public static void register(IEventBus modBus) {
        if (ModList.get().isLoaded("curios")) {
            Api.register(modBus);
        }
    }

    /** Everything that names Curios classes; loaded only when Curios is there. */
    private static final class Api {
        static void register(IEventBus modBus) {
            modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> event.registerLayerDefinition(OVER_HELMET,
                    () -> LayerDefinition.create(HumanoidModel.createArmorMeshSet(LayerDefinitions.INNER_ARMOR_DEFORMATION,
                            new CubeDeformation(1.25F)).head(), 64, 32)));
            modBus.addListener((FMLClientSetupEvent event) ->
                    ICurioRenderer.register(ModItems.RESEARCHERS_GOGGLES.get(), GogglesRenderer::new));
        }
    }

    private static final class GogglesRenderer implements ICurioRenderer {
        private final HumanoidModel<HumanoidRenderState> head;
        private final HumanoidModel<HumanoidRenderState> overHelmet;

        GogglesRenderer() {
            var models = net.minecraft.client.Minecraft.getInstance().getEntityModels();
            this.head = new HumanoidModel<>(models.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_ARMOR.head()));
            this.overHelmet = new HumanoidModel<>(models.bakeLayer(OVER_HELMET));
        }

        @Override
        public <S extends LivingEntityRenderState, M extends EntityModel<? super S>> void render(ItemStack stack, SlotContext slotContext,
                PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, S renderState,
                RenderLayerParent<S, M> renderLayerParent, EntityRendererProvider.Context context, float yRotation, float xRotation) {
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (!(renderState instanceof HumanoidRenderState humanoid) || equippable == null || equippable.assetId().isEmpty()
                    || humanoid.headEquipment.is(stack.getItem())) {
                return;
            }
            boolean helmet = humanoid.headEquipment.has(DataComponents.EQUIPPABLE)
                    && humanoid.headEquipment.get(DataComponents.EQUIPPABLE).slot() == EquipmentSlot.HEAD;
            context.getEquipmentRenderer().renderLayers(EquipmentClientInfo.LayerType.HUMANOID, equippable.assetId().get(),
                    helmet ? overHelmet : head, humanoid, stack, poseStack, submitNodeCollector, packedLight, humanoid.outlineColor);
        }
    }
}
