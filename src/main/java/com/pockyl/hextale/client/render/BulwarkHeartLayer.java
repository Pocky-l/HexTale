package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.runework.BulwarkArmorItem;
import com.pockyl.hextale.runework.BulwarkHeart;

/**
 * The crystal heart of a worn Bulwark cuirass, lit from inside by its charge: the cuirass drawn again with its glow sheet
 * as light, brighter the fuller the heart, breathing slowly; a cracked heart stays dark.
 *
 * <p>1.21.1: a player render layer that reads the player directly (26.x extracts into the avatar render state).
 */
public class BulwarkHeartLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** What the layer needs, read off the player. */
    public record Lit(ResourceLocation glow, float brightness) {
    }

    public BulwarkHeartLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new BulwarkHeartLayer(renderer));
            }
        }
    }

    private static @Nullable Lit extract(LivingEntity entity, float partialTick) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!BulwarkArmorItem.isHeart(chest)) {
            return null;
        }
        BulwarkHeart.Charge charge = BulwarkHeart.of(chest);
        float fill = charge.cracked() ? 0 : charge.charge() / (float) BulwarkHeart.MAX;
        float breath = 0.85F + 0.15F * Mth.sin(HexFx.time(entity.level(), partialTick) * 0.12F);
        return fill <= 0 ? null : new Lit(BulwarkClient.heartGlow(chest), Mth.clamp(0.25F + 0.75F * fill, 0, 1) * breath);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void copyPose(HumanoidModel from, HumanoidModel to) {
        from.copyPropertiesTo(to);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int lightCoords, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Lit lit = extract(player, partialTick);
        HumanoidModel<LivingEntity> model = BulwarkClient.heart;
        if (lit == null || model == null || player.isInvisible()) {
            return;
        }
        copyPose(getParentModel(), model);
        int tint = (Math.round(lit.brightness() * 255) << 24) | 0xFFFFFF;
        model.renderToBuffer(poseStack, buffers.getBuffer(HexFx.glow(lit.glow())), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint);
    }
}
