package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.SkypackClient;
import com.pockyl.hextale.item.HextechSkypackItem;
import com.pockyl.hextale.item.SkypackFlight;

import org.joml.Vector3f;

/**
 * The Hextech Skypack worn on a player's back: its item model strapped to the body (it follows every pose, the elytra
 * dive included) and, through the boost of a launch, two tongues of hextech flame out of the nozzles - additive light, flickering,
 * longer the faster the flyer goes. The skypack hides the cape it would cut through.
 */
public class SkypackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** What the layer needs, read off the player when its render state is extracted. */
    public record Worn(ItemStackRenderState item, float jets, boolean low, float time, float speed) {
    }

    public static final ContextKey<Worn> WORN = new ContextKey<>(Hextale.id("skypack"));
    /** The pack's scale on the back (the model is 16 px wide with its little wings). */
    private static final float SCALE = 0.8F;
    /** Where the model's centre sits in the body's space (blocks; y down, +z = the back). */
    private static final float Y = 4.5F / 16;
    private static final float Z = 4.6F / 16;
    /** The two nozzles' mouths in the model's pixels. */
    private static final float[][] NOZZLES = {{5.5F, 0, 7.5F}, {10.5F, 0, 7.5F}};
    private static final int FLAME = 0x5CC8F2;
    private static final int CORE = 0xDDF8FF;

    public SkypackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new SkypackLayer(renderer));
            }
        }
    }

    public static void registerModifier(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends net.minecraft.world.entity.Avatar & net.minecraft.client.entity.ClientAvatarEntity> void accept(T avatar,
                                                                                                                          AvatarRenderState state) {
                extract(avatar, state);
            }
        });
    }

    private static void extract(LivingEntity entity, AvatarRenderState state) {
        ItemStack pack = HextechSkypackItem.worn(entity);
        if (pack.isEmpty()) {
            state.setRenderData(WORN, null);
            return;
        }
        ItemStackRenderState item = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(item, pack, ItemDisplayContext.NONE, entity);
        state.showCape = false;
        // The jets burn through a launch's boost and fade over its last ticks.
        int left = SkypackFlight.boostLeft(entity, entity.level().getGameTime());
        float jets = Math.min(1, left / (float) SkypackClient.FADE_TICKS);
        state.setRenderData(WORN, new Worn(item, jets, HextechSkypackItem.air(pack) < HextechSkypackItem.LOW,
                HexFx.time(entity.level(), state.partialTick), (float) entity.getDeltaMovement().length()));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        Worn worn = state.getRenderData(WORN);
        if (worn == null || state.isInvisible) {
            return;
        }
        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0, Y, Z);
        poseStack.scale(SCALE, -SCALE, -SCALE);
        worn.item().submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        if (worn.jets() > 0) {
            for (int i = 0; i < NOZZLES.length; i++) {
                float[] n = NOZZLES[i];
                poseStack.pushPose();
                poseStack.translate(n[0] / 16 - 0.5F, n[1] / 16 - 0.5F, n[2] / 16 - 0.5F);
                jet(poseStack, collector, worn, i);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }

    /** One tongue of flame pointing down out of a nozzle: three crossed planes of light, a wide blue one and a white core. */
    private static void jet(PoseStack poseStack, SubmitNodeCollector collector, Worn worn, int index) {
        float t = worn.time();
        float flicker = 0.85F + 0.15F * Mth.sin(t * 2.7F + index * 1.9F) * Mth.sin(t * 1.3F + index);
        float sputter = worn.low() && Mth.sin(t * 0.9F + index * 2) > 0.3F ? 0.35F : 1;
        float length = (0.32F + Mth.clamp(worn.speed() / 2.0F, 0, 1) * 0.28F) * flicker * sputter * (0.4F + 0.6F * worn.jets());
        float fade = sputter * worn.jets();
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            for (int k = 0; k < 3; k++) {
                float angle = k * Mth.PI / 3 + t * 0.15F;
                Vector3f side = new Vector3f(Mth.cos(angle), 0, Mth.sin(angle));
                tongue(consumer, pose, side, length, 0.11F, FLAME, 0.55F * fade);
                tongue(consumer, pose, side, length * 0.6F, 0.045F, CORE, 0.9F * fade);
            }
        });
    }

    /** A plane from the nozzle down: brightest on its axis at the mouth, fading to nothing at its edges and its tip. */
    private static void tongue(VertexConsumer consumer, PoseStack.Pose pose, Vector3f side, float length, float width, int rgb, float alpha) {
        int mouth = argb(rgb, alpha);
        int none = argb(rgb, 0);
        for (int s = -1; s <= 1; s += 2) {
            float x = side.x * width * s;
            float z = side.z * width * s;
            consumer.addVertex(pose, 0, 0, 0).setColor(mouth);
            consumer.addVertex(pose, x, 0, z).setColor(none);
            consumer.addVertex(pose, x * 0.3F, -length, z * 0.3F).setColor(none);
            consumer.addVertex(pose, 0, -length, 0).setColor(none);
        }
    }

    private static int argb(int rgb, float alpha) {
        return (Math.round(Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }
}
