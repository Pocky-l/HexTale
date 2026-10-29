package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.RuneSteleBlock;
import com.pockyl.hextale.block.RuneSteleBlockEntity;
import com.pockyl.hextale.rune.ComplexRune;

/**
 * The stele's body (its block model is empty), turned to face the centre of its Rune Circle at any angle. The rune chosen on a stele hangs in the window of the stele's head as its glowing hieroglyph in the rune's colour,
 * facing the viewer, inside a slowly turning sigil ring, with a soft halo and a few motes circling it. A freshly chosen rune writes itself stroke
 * by stroke while the ring opens; while a ritual draws on the stele, everything burns brighter and pulses. Glyph and ring are additive light (no sorting, no flicker).
 */
public class RuneSteleRenderer implements BlockEntityRenderer<RuneSteleBlockEntity, RuneSteleRenderer.State> {
    private static final float ENTRANCE_TICKS = 24;

    public RuneSteleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RuneSteleBlockEntity stele, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(stele, state, partialTick, cameraPosition, breakProgress);
        Level level = stele.getLevel();
        float time = HexFx.time(level, partialTick);
        state.time = time;
        state.rune = stele.rune();
        state.active = level != null && level.getGameTime() < stele.activeUntil();
        state.sinceChosen = level == null ? -1 : level.getGameTime() - stele.chosenAt() + partialTick;
        state.seed = (int) stele.getBlockPos().asLong();
        state.yaw = stele.bodyYaw();
        BlockState block = stele.getBlockState();
        state.lit = block.hasProperty(RuneSteleBlock.LIT) && block.getValue(RuneSteleBlock.LIT);
        state.upperLight = level == null ? state.lightCoords : net.minecraft.client.renderer.LevelRenderer.getLightCoords(level, stele.getBlockPos().above());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // The body, both halves, turned to the circle the stele serves.
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(state.yaw + 180)));
        poseStack.translate(-0.5, 0, -0.5);
        TableModels.submit(poseStack, collector, state.lit ? TableModels.STELE_LOWER_LIT : TableModels.STELE_LOWER, state.lightCoords);
        poseStack.translate(0, 1, 0);
        TableModels.submit(poseStack, collector, state.lit ? TableModels.STELE_UPPER_LIT : TableModels.STELE_UPPER, state.upperLight);
        poseStack.popPose();
        ComplexRune rune = state.rune;
        if (rune == null) {
            return;
        }
        float time = state.time + (state.seed & 127);
        float entrance = state.sinceChosen >= 0 && state.sinceChosen < ENTRANCE_TICKS ? state.sinceChosen / ENTRANCE_TICKS : 1;
        entrance = 1 - (1 - entrance) * (1 - entrance);
        float pulse = state.active ? 0.8F + 0.2F * Mth.sin(time * 0.5F) : 0.85F + 0.15F * Mth.sin(time * 0.08F);
        int color = rune.color();
        float y = RuneSteleBlockEntity.GLYPH_Y + Mth.sin(time * 0.06F) * 0.02F;

        poseStack.pushPose();
        poseStack.translate(0.5, y, 0.5);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, (0.6F + (state.active ? 0.25F : 0)) * entrance, color,
                (state.active ? 0.55F : 0.3F) * pulse);
        // Motes circling the rune on a tilted orbit.
        for (int i = 0; i < 4; i++) {
            float angle = time * (state.active ? 0.09F : 0.035F) + i * Mth.HALF_PI;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * 0.3F, Mth.sin(angle * 2 + i) * 0.05F, Mth.sin(angle) * 0.3F);
            float twinkle = 0.55F + 0.45F * Mth.sin(time * 0.3F + i * 1.9F);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, 0.06F, RuneDrawing.lighten(color, 0.4F), 0.7F * twinkle * entrance);
            poseStack.popPose();
        }
        // A rune standing in the air, not a sticker on the screen: upright, turning only round its own axis to face the
        // viewer (readable from every side), a hexagon round it; it sways a little.
        double dx = camera.pos.x - (state.blockPos.getX() + 0.5);
        double dz = camera.pos.z - (state.blockPos.getZ() + 0.5);
        poseStack.mulPose(Axis.YP.rotation((float) Mth.atan2(dx, dz)));
        poseStack.translate(0, 0, 0.03F);
        float size = 0.36F * (state.active ? 1.08F : 1);
        float alpha = pulse * (state.active ? 1 : 0.85F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * (state.active ? 1.2F : 0.35F)));
        HexFx.hexRing(poseStack, collector, 0.25F * (0.6F + 0.4F * entrance), color, 0.35F * alpha * entrance);
        poseStack.popPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.05F) * 3));
        HexFx.glyph(poseStack, collector, rune, size, color, alpha, entrance);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(RuneSteleBlockEntity stele) {
        return new AABB(stele.getBlockPos()).expandTowards(0, 1.1, 0).inflate(0.3);
    }

    public static final class State extends BlockEntityRenderState {
        float time;
        @Nullable
        ComplexRune rune;
        boolean active;
        float sinceChosen;
        int seed;
        float yaw;
        boolean lit;
        int upperLight;
    }
}
