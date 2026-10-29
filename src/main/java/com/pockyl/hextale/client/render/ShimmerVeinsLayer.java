package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ShimmerVeins;

/**
 * Shimmer in a player's veins, drawn over the skin: violet veins creeping up from the wrists, at the most the eyes turn
 * pink (one texture per number of veins). While the body is fed they glow full-bright and pulse slowly like a heartbeat;
 * unfed they stay, dimmed to a grey-violet in the world's light.
 *
 * <p>1.21.1: a player render layer that reads the attachment off the player directly (26.x extracts it into the avatar
 * render state).
 */
public class ShimmerVeinsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[ShimmerVeins.MAX];
    /** The unfed veins' tint. */
    private static final int DIM = 0xFF9A8AA6;

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = Hextale.id("textures/entity/shimmer_veins_" + (i + 1) + ".png");
        }
    }

    public ShimmerVeinsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new ShimmerVeinsLayer(renderer));
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ShimmerVeins.Veins veins = player.getData(com.pockyl.hextale.registry.ModAttachments.SHIMMER_VEINS);
        if (veins.count() <= 0 || player.isInvisible()) {
            return;
        }
        int count = Mth.clamp(veins.count(), 1, ShimmerVeins.MAX);
        boolean fed = veins.fed(player.level().getGameTime());
        int color = DIM;
        int light = packedLight;
        if (fed) {
            // A slow double beat, like a pulse.
            float t = ageInTicks * 0.09F;
            float beat = Math.max(0, Mth.sin(t)) * 0.6F + Math.max(0, Mth.sin(t + 0.7F)) * 0.4F;
            int v = (int) (190 + 65 * beat);
            color = 0xFF000000 | v << 16 | v << 8 | v;
            light = LightTexture.FULL_BRIGHT;
        }
        renderColoredCutoutModel(getParentModel(), TEXTURES[count - 1], poseStack, buffer, light, player, color);
    }
}
