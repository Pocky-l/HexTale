package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.runework.BulwarkArmorItem;
import com.pockyl.hextale.runework.BulwarkHeart;

/**
 * The crystal heart of a worn Bulwark cuirass, lit from inside by its charge: the cuirass drawn again with its glow sheet
 * as light, brighter the fuller the heart, breathing slowly; a cracked heart stays dark.
 */
public class BulwarkHeartLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** What the layer needs, read off the player when its render state is extracted. */
    public record Lit(Identifier glow, float brightness) {
    }

    public static final ContextKey<Lit> LIT = new ContextKey<>(Hextale.id("bulwark_heart"));

    public BulwarkHeartLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new BulwarkHeartLayer(renderer));
            }
        }
    }

    public static void registerModifier(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & net.minecraft.client.entity.ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                extract(avatar, state);
            }
        });
    }

    private static void extract(LivingEntity entity, AvatarRenderState state) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!BulwarkArmorItem.isHeart(chest)) {
            state.setRenderData(LIT, null);
            return;
        }
        BulwarkHeart.Charge charge = BulwarkHeart.of(chest);
        float fill = charge.cracked() ? 0 : charge.charge() / (float) BulwarkHeart.MAX;
        float breath = 0.85F + 0.15F * Mth.sin(HexFx.time(entity.level(), state.partialTick) * 0.12F);
        state.setRenderData(LIT, fill <= 0 ? null : new Lit(BulwarkClient.heartGlow(chest), Mth.clamp(0.25F + 0.75F * fill, 0, 1) * breath));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        Lit lit = state.getRenderData(LIT);
        var model = BulwarkClient.heart;
        if (lit == null || model == null || state.isInvisible) {
            return;
        }
        model.setupAnim(state);
        int tint = (Math.round(lit.brightness() * 255) << 24) | 0xFFFFFF;
        collector.submitModel(model, state, poseStack, HexFx.glow(lit.glow()), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint, null, state.outlineColor, null);
    }
}
