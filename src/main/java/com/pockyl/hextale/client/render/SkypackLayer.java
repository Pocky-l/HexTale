package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.HextechSkypackItem;
import com.pockyl.hextale.item.SkypackFlight;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;

/**
 * The Hextech Skypack worn on a player's back: its item model strapped to the body (it follows every pose, the elytra
 * dive included) and, through the boost of a launch, the jets - everything drawn on the player, so it stays with them
 * however fast they fly. Each nozzle: a long plume of hextech flame (an animated sprite on three crossed planes, a blue
 * body and a white core, additive), a hot glow at the mouth, glints running down the flame, and a ring of light that
 * bursts out when the jets catch. The jets thin out and shorten over the boost's last ticks. The skypack hides the cape.
 */
public class SkypackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    /** What the layer needs, read off the player when its render state is extracted. */
    public record Worn(ItemStackRenderState item, int boostLeft, boolean low, float time, float speed) {
    }

    public static final ContextKey<Worn> WORN = new ContextKey<>(Hextale.id("skypack"));
    private static final ResourceLocation JET = Hextale.id("textures/misc/skypack_jet.png");
    private static final int JET_FRAMES = 6;
    /** The pack's scale on the back (the model is 16 px wide with its little wings). */
    private static final float SCALE = 0.8F;
    /** Where the model's centre sits in the body's space (blocks; y down, +z = the back). */
    private static final float Y = 4.5F / 16;
    private static final float Z = 4.6F / 16;
    /** The two nozzles' mouths in the model's pixels. */
    private static final float[][] NOZZLES = {{5.5F, 0, 7.5F}, {10.5F, 0, 7.5F}};
    /** Ticks at the end of a boost over which the jets fade; ticks at its start the ignition ring lasts. */
    private static final int FADE_TICKS = 10;
    private static final int RING_TICKS = 8;
    private static final int FLAME = 0x4FB8F0;
    private static final int CORE = 0xE4FAFF;
    private static final int GLINTS = 5;

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
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
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
        state.setRenderData(WORN, new Worn(item, SkypackFlight.boostLeft(entity, entity.level().getGameTime()),
                HextechSkypackItem.air(pack) < HextechSkypackItem.LOW, HexFx.time(entity.level(), state.partialTick),
                (float) entity.getDeltaMovement().length()));
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
        if (worn.boostLeft() > 0) {
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

    /** One jet out of a nozzle, pointing down the model (towards the wearer's feet). */
    private static void jet(PoseStack poseStack, SubmitNodeCollector collector, Worn worn, int index) {
        float t = worn.time() + index * 7.3F;
        float fade = Math.min(1, worn.boostLeft() / (float) FADE_TICKS);
        float cough = worn.low() && Mth.sin(t * 0.9F) > 0.4F ? 0.45F : 1;
        float strength = fade * cough;
        float flicker = 0.92F + 0.08F * Mth.sin(t * 2.3F) * Mth.sin(t * 1.1F + 1);
        float length = (0.75F + Mth.clamp(worn.speed() / 2.0F, 0, 1) * 0.55F) * flicker * (0.35F + 0.65F * strength);
        int frame = Mth.floor(t * 0.7F) % JET_FRAMES;
        Quaternionf camera = facing(poseStack);

        // The plume: three planes crossed on the jet's axis, slowly turning; a wide blue body, then a narrower white core.
        collector.submitCustomGeometry(poseStack, HexGlow.textured(JET), (pose, consumer) -> {
            for (int k = 0; k < 3; k++) {
                float angle = k * Mth.PI / 3 + t * 0.05F;
                plume(consumer, pose, angle, 0.62F, length, frame, FLAME, 0.85F * strength);
                plume(consumer, pose, angle, 0.3F, length * 0.7F, (frame + 3) % JET_FRAMES, CORE, 0.7F * strength);
            }
        });
        // A hot glow sitting in the nozzle's mouth.
        HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.42F + 0.04F * Mth.sin(t * 3.1F), FLAME, 0.75F * strength);
        HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.18F, CORE, 0.9F * strength);
        // Glints carried down the flame, shrinking as they go.
        List<float[]> glints = new ArrayList<>();
        for (int g = 0; g < GLINTS; g++) {
            float along = frac(t * 0.07F + g / (float) GLINTS + index * 0.13F);
            float spin = g * 2.4F + t * 0.2F;
            float wide = 0.05F * along;
            glints.add(HexFx.quad(Mth.cos(spin) * wide, -along * length, Mth.sin(spin) * wide, 0.05F * (1 - along * 0.7F),
                    (g + frame) % HexFx.GLINT_FRAMES, spin, strength * (1 - along), CORE));
        }
        HexFx.sheet(poseStack, collector, camera, HexFx.GLINT, HexFx.GLINT_FRAMES, glints);
        // The ignition: a ring of light bursting out of the nozzle across the jet.
        int age = SkypackFlight.BOOST_TICKS - worn.boostLeft();
        if (age < RING_TICKS) {
            float u = (age + frac(worn.time())) / RING_TICKS;
            poseStack.pushPose();
            poseStack.translate(0, -0.06F - u * 0.25F, 0);
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            HexFx.sigilRing(poseStack, collector, 0.08F + u * 0.32F, 6, FLAME, 1 - u);
            poseStack.popPose();
        }
    }

    /** One plane of the plume through the jet's axis at {@code angle}: the sprite's top at the nozzle, its tip below. */
    private static void plume(VertexConsumer consumer, PoseStack.Pose pose, float angle, float width, float length, int frame, int rgb,
                              float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        float x = Mth.cos(angle) * width / 2;
        float z = Mth.sin(angle) * width / 2;
        float u0 = frame / (float) JET_FRAMES;
        float u1 = (frame + 1) / (float) JET_FRAMES;
        // The sprite's nozzle end sits a little above the mouth, inside the bell.
        float top = 0.04F;
        vertex(consumer, pose, -x, top, -z, u0, 0, rgb, alpha);
        vertex(consumer, pose, x, top, z, u1, 0, rgb, alpha);
        vertex(consumer, pose, x, top - length, z, u1, 1, rgb, alpha);
        vertex(consumer, pose, -x, top - length, -z, u0, 1, rgb, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int rgb, float alpha) {
        consumer.addVertex(pose, x, y, z).setColor((Math.round(Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF)).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }

    /** The camera's rotation seen from inside the current pose: sprites turned by it face the camera. */
    private static Quaternionf facing(PoseStack poseStack) {
        Quaternionf local = poseStack.last().pose().getNormalizedRotation(new Quaternionf()).conjugate();
        return local.mul(Minecraft.getInstance().gameRenderer.mainCamera().rotation());
    }

    private static float frac(float value) {
        return value - Mth.floor(value);
    }
}
