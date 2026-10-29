package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ShimmerVeins;

/**
 * Shimmer in a player's veins, drawn over the skin: violet veins creeping up from the wrists, at the most the eyes turn
 * pink (one texture per number of veins). While the body is fed they glow full-bright and pulse slowly like a heartbeat;
 * unfed they stay, dimmed to a grey-violet in the world's light.
 */
public class ShimmerVeinsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** Veins (1-5) times two, plus one while fed; absent without veins. */
    public static final ContextKey<Integer> VEINS = new ContextKey<>(Hextale.id("shimmer_veins"));
    private static final Identifier[] TEXTURES = new Identifier[ShimmerVeins.MAX];
    /** The unfed veins' tint. */
    private static final int DIM = 0xFF9A8AA6;

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = Hextale.id("textures/entity/shimmer_veins_" + (i + 1) + ".png");
        }
    }

    public ShimmerVeinsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new ShimmerVeinsLayer(renderer));
            }
        }
    }

    public static void registerModifier(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                ShimmerVeins.Veins veins = avatar.getData(com.pockyl.hextale.registry.ModAttachments.SHIMMER_VEINS);
                boolean fed = veins.fed(avatar.level().getGameTime());
                state.setRenderData(VEINS, veins.count() > 0 ? veins.count() * 2 + (fed ? 1 : 0) : null);
            }
        });
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        Integer veins = state.getRenderData(VEINS);
        if (veins == null || state.isInvisible) {
            return;
        }
        int count = Mth.clamp(veins / 2, 1, ShimmerVeins.MAX);
        boolean fed = (veins & 1) == 1;
        int color = DIM;
        int light = lightCoords;
        if (fed) {
            // A slow double beat, like a pulse.
            float t = state.ageInTicks * 0.09F;
            float beat = Math.max(0, Mth.sin(t)) * 0.6F + Math.max(0, Mth.sin(t + 0.7F)) * 0.4F;
            int v = (int) (190 + 65 * beat);
            color = 0xFF000000 | v << 16 | v << 8 | v;
            light = LightCoordsUtil.FULL_BRIGHT;
        }
        renderColoredCutoutModel(getParentModel(), TEXTURES[count - 1], poseStack, collector, light, state, color, 1);
    }
}
