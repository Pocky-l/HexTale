package com.pockyl.hextale.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Client side of the optional Curios support: goggles in a Curios slot are drawn on the head like worn ones. Over a
 * helmet they use a slightly larger head so the two do not flicker into each other. Only called when Curios is loaded.
 * (1.21.1: drawn as the goggles' armour layer on the head, posed like the wearer's.)
 */
public final class CuriosClientCompat {
    /** The goggles' head over a helmet (vanilla outer armour is 1.0). */
    private static final ModelLayerLocation OVER_HELMET = new ModelLayerLocation(Hextale.id("curio_goggles"), "over_helmet");
    private static final ResourceLocation TEXTURE = Hextale.id("textures/models/armor/researchers_goggles_layer_1.png");

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
                    () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.25F), 0.0F), 64, 32)));
            modBus.addListener((FMLClientSetupEvent event) ->
                    CuriosRendererRegistry.register(ModItems.RESEARCHERS_GOGGLES.get(), GogglesRenderer::new));
        }
    }

    private static final class GogglesRenderer implements ICurioRenderer {
        private final HumanoidModel<LivingEntity> head;
        private final HumanoidModel<LivingEntity> overHelmet;

        GogglesRenderer() {
            var models = net.minecraft.client.Minecraft.getInstance().getEntityModels();
            this.head = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
            this.overHelmet = new HumanoidModel<>(models.bakeLayer(OVER_HELMET));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends LivingEntity, M extends net.minecraft.client.model.EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack,
                RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            LivingEntity wearer = slotContext.entity();
            ItemStack worn = wearer.getItemBySlot(EquipmentSlot.HEAD);
            if (worn.is(stack.getItem()) || !(renderLayerParent.getModel() instanceof HumanoidModel<?> parent)) {
                return;
            }
            boolean helmet = worn.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.HEAD;
            HumanoidModel<LivingEntity> model = helmet ? overHelmet : head;
            ((HumanoidModel<LivingEntity>) parent).copyPropertiesTo(model);
            model.setAllVisible(false);
            model.head.visible = true;
            model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.armorCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        }
    }
}
