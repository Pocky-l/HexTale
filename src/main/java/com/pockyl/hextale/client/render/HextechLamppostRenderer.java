package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.HextechLamppostBlock;
import com.pockyl.hextale.block.HextechLamppostBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.rune.Essence;

import org.jspecify.annotations.Nullable;

/**
 * The light of a Hextech Lamppost: a soft cold halo breathing round the gem while it is lit; when it burns off the dirt
 * of the work around it, the gem flares and a plum ring of the drawn-in dark turns round it for a moment; a stream of
 * light essence runs into the head from the jar it drew from.
 */
public class HextechLamppostRenderer implements BlockEntityRenderer<HextechLamppostBlockEntity, HextechLamppostRenderer.State> {
    /** Ticks a burn's flare lasts. */
    private static final float FLARE_TICKS = 18;

    public static final class State extends BlockEntityRenderState {
        float time;
        boolean lit;
        float flare;
        Vec3 gem = Vec3.ZERO;
        JarStreamFx.@Nullable Stream stream;
    }

    public HextechLamppostRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HextechLamppostBlockEntity post, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(post, state, partialTick, cameraPosition, breakProgress);
        Level level = post.getLevel();
        BlockPos pos = post.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        int seed = (int) pos.asLong();
        state.time = HexFx.time(level, partialTick) + (seed & 127);
        state.lit = post.getBlockState().getValue(HextechLamppostBlock.LIT);
        state.gem = HextechLamppostBlockEntity.gem(pos).subtract(origin);
        float now = level == null ? 0 : level.getGameTime() + partialTick;
        float since = now - post.burnedAt();
        state.flare = since >= FLARE_TICKS || since < 0 ? 0 : 1 - since / FLARE_TICKS;
        state.stream = null;
        BlockPos jar = post.drawing();
        if (jar != null) {
            float age = now - post.drewAt();
            state.stream = new JarStreamFx.Stream(JarReach.mouth(jar).subtract(origin), state.gem, Essence.LIGHT.color(), age / 12F,
                    (age - (HextechLamppostBlockEntity.STREAM_TICKS - 14)) / 12F, seed);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.lit || state.flare > 0) {
            poseStack.pushPose();
            poseStack.translate(state.gem.x, state.gem.y, state.gem.z);
            float breath = 0.9F + 0.1F * Mth.sin(state.time * 0.05F);
            if (state.lit) {
                HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.55F * breath, ModParticles.HEXTECH, 0.35F);
            }
            if (state.flare > 0) {
                float f = state.flare * state.flare;
                HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.5F + 0.5F * (1 - state.flare), ModParticles.HEX_LIGHT,
                        0.6F * f);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(state.time * 2.4F));
                HexFx.sigilRing(poseStack, collector, 0.3F + 0.15F * (1 - state.flare), 6, HextechLamppostBlockEntity.PLUM, 0.5F * f);
            }
            poseStack.popPose();
        }
        if (state.stream != null) {
            Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
            JarStreamFx.submit(poseStack, collector, camera.orientation, eye, state.stream, state.time);
        }
    }

    @Override
    public AABB getRenderBoundingBox(HextechLamppostBlockEntity post) {
        AABB box = new AABB(post.getBlockPos()).expandTowards(0, 1, 0).inflate(0.6);
        BlockPos jar = post.drawing();
        return jar == null ? box : box.minmax(new AABB(jar).inflate(1));
    }
}
