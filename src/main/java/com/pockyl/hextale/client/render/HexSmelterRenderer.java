package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexSmelterBlock;
import com.pockyl.hextale.block.HexSmelterBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.registry.ModItems;

/**
 * The Hex Smelter's heart: a hextech gemstone floating between the four prongs of its crown, slowly turning in a halo
 * of the colour of the runes burned into the smelter (hextech cyan without runes). While it smelts the gem spins up,
 * the halo swells and sparks circle it.
 */
public class HexSmelterRenderer implements BlockEntityRenderer<HexSmelterBlockEntity, HexSmelterRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public HexSmelterRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HexSmelterBlockEntity smelter, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(smelter, state, partialTick, cameraPosition, breakProgress);
        Level level = smelter.getLevel();
        itemModelResolver.updateForTopItem(state.gem, new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), ItemDisplayContext.GROUND, level, null,
                (int) smelter.getBlockPos().asLong());
        state.time = HexFx.time(level, partialTick);
        state.lit = smelter.getBlockState().getValue(HexSmelterBlock.LIT);
        state.color = PickaxeRuneTint.blend(smelter.runes());
        // Essence drawn out of a jar: the stream, and the gem swelling while the stream pours into it.
        float head = Mth.lerp(partialTick, smelter.streamHeadO, smelter.streamHead);
        float tail = Mth.lerp(partialTick, smelter.streamTailO, smelter.streamTail);
        BlockPos jar = smelter.streamJar;
        state.stream = null;
        if (head > 0 && jar != null && smelter.streamColor >= 0) {
            Vec3 origin = Vec3.atLowerCornerOf(smelter.getBlockPos());
            state.stream = new JarStreamFx.Stream(JarReach.mouth(jar).subtract(origin),
                    HexSmelterBlockEntity.gemPosition(smelter.getBlockPos()).subtract(origin), smelter.streamColor, head, tail,
                    (int) smelter.getBlockPos().asLong());
        }
        state.flare = Mth.clamp(head * 2 - 1, 0, 1) * (1 - tail);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        if (state.stream != null) {
            JarStreamFx.submit(poseStack, collector, camera.orientation, camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos)), state.stream, time);
        }
        float activity = state.lit ? 1 : 0;
        float bob = Mth.sin(time * 0.07F) * 0.02F;
        poseStack.pushPose();
        poseStack.translate(0.5, 15.5 / 16 + bob, 0.5);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.55F + 0.25F * activity + 0.2F * state.flare, state.color,
                Math.min(1, 0.3F + 0.25F * activity + 0.35F * state.flare));
        if (state.lit) {
            for (int i = 0; i < 4; i++) {
                float angle = time * 0.12F + i * Mth.HALF_PI;
                poseStack.pushPose();
                poseStack.translate(Mth.cos(angle) * 0.22F, Mth.sin(time * 0.2F + i) * 0.05F, Mth.sin(angle) * 0.22F);
                HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, 0.06F, RuneDrawing.lighten(state.color, 0.4F), 0.8F);
                poseStack.popPose();
            }
        }
        poseStack.rotate(Axis.YP.rotationDegrees(time * (state.lit ? 4.5F : 1.2F)));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        poseStack.translate(0, -0.2, 0);
        state.gem.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HexSmelterBlockEntity smelter) {
        AABB box = new AABB(smelter.getBlockPos()).expandTowards(0, 0.6, 0);
        BlockPos source = smelter.streamJar;
        // The stream reaches out to its jar and arches above the straight line.
        return source == null ? box : box.minmax(new AABB(source).expandTowards(0, 1.5, 0)).inflate(0.5, 1.5, 0.5);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState gem = new ItemStackRenderState();
        float time;
        boolean lit;
        int color;
        float flare;
        JarStreamFx.@Nullable Stream stream;
    }
}
