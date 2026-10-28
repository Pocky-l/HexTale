package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.ItemModelResolver;
import com.pockyl.hextale.port.client.ItemStackRenderState;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HextechPurifierBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.rune.Essence;

/**
 * The Hextech Purifier at work. Its crystal hangs point down from the canopy's hub over the font and turns; as it fills,
 * a plum shadow grows inside its light and the hextech glow around it sinks, until it hangs dark and still - a Clouded
 * Crystal. While it draws, a ring of light turns flat under the crystal and the jar's light essence streams in.
 */
public class HextechPurifierRenderer implements BlockEntityRenderer<HextechPurifierBlockEntity, HextechPurifierRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public HextechPurifierRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = ItemModelResolver.INSTANCE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HextechPurifierBlockEntity purifier, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(purifier, state, partialTick, cameraPosition, breakProgress);
        Level level = purifier.getLevel();
        BlockPos pos = purifier.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        int seed = (int) pos.asLong();
        state.time = HexFx.time(level, partialTick) + (seed & 127);
        state.hasCrystal = !purifier.crystal().isEmpty();
        state.clouding = purifier.clouding();
        state.working = purifier.mode() == HextechPurifierBlockEntity.Mode.WORKING;
        state.color = PickaxeRuneTint.blend(purifier.runes());
        state.crystalAt = HextechPurifierBlockEntity.crystalAt(pos).subtract(origin);
        if (state.hasCrystal) {
            itemModelResolver.updateForTopItem(state.crystal, purifier.crystal(), ItemDisplayContext.GROUND, level, null, seed);
        }
        float now = level == null ? 0 : level.getGameTime() + partialTick;
        state.stream = null;
        BlockPos jar = purifier.drawing();
        if (jar != null) {
            float age = now - purifier.drewAt();
            state.stream = new JarStreamFx.Stream(JarReach.mouth(jar).subtract(origin), state.crystalAt, Essence.LIGHT.color(),
                    age / 12F, (age - (HextechPurifierBlockEntity.STREAM_TICKS - 14)) / 12F, seed);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        Vec3 crystal = state.crystalAt;
        if (state.hasCrystal) {
            float clouding = state.clouding;
            boolean clouded = clouding >= 1;
            // Full of the Void it stops turning and hangs low, a slow breath in it.
            float bob = clouded ? Mth.sin(time * 0.03F) * 0.008F - 0.02F : Mth.sin(time * 0.08F) * 0.015F;
            poseStack.pushPose();
            poseStack.translate(crystal.x, crystal.y + bob, crystal.z);
            float light = 1 - clouding;
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.5F + 0.15F * light, state.color, 0.45F * light);
            if (clouding > 0) {
                float breath = 0.85F + 0.15F * Mth.sin(time * 0.06F);
                HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.3F + 0.4F * clouding, HextechPurifierBlockEntity.PLUM,
                        0.55F * clouding * breath);
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(clouded ? time * 0.2F : time * 1.4F));
            // Point down, like a drop about to fall into the font.
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.scale(0.36F, 0.36F, 0.36F);
            poseStack.translate(0, -0.2, 0);
            state.crystal.submit(poseStack, collector, clouded ? state.lightCoords : LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();

            if (state.working) {
                // A ring of light turns flat on the font's water while it draws.
                poseStack.pushPose();
                poseStack.translate(crystal.x, 4.6 / 16, crystal.z);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(time * 0.9F));
                HexFx.sigilRing(poseStack, collector, 0.36F, 6, state.color, 0.55F * (1 - clouding * 0.6F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-time * 2.1F));
                HexFx.sigilRing(poseStack, collector, 0.2F, 0, HextechPurifierBlockEntity.PLUM, 0.35F + 0.4F * clouding);
                poseStack.popPose();
            }
        }
        if (state.stream != null) {
            Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
            JarStreamFx.submit(poseStack, collector, camera.orientation, eye, state.stream, time);
        }
    }

    @Override
    public AABB getRenderBoundingBox(HextechPurifierBlockEntity purifier) {
        AABB box = new AABB(purifier.getBlockPos()).inflate(0.6).expandTowards(0, 0.8, 0);
        BlockPos jar = purifier.drawing();
        if (jar != null) {
            box = box.minmax(new AABB(jar).inflate(1));
        }
        return box;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState crystal = new ItemStackRenderState();
        float time;
        boolean hasCrystal;
        boolean working;
        float clouding;
        int color;
        Vec3 crystalAt = Vec3.ZERO;
        JarStreamFx.@Nullable Stream stream;
    }
}
