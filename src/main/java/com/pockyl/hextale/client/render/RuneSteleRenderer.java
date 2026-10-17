package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.RuneSteleBlockEntity;
import com.pockyl.hextale.rune.ComplexRune;

/**
 * The rune chosen on a stele hangs over it as its glowing hieroglyph in the rune's colour, facing the viewer, with a
 * soft halo and a thin thread of light down to the stele's socket. A freshly chosen rune
 * unfolds from a point; while a ritual draws on the stele, the glyph burns brighter and pulses.
 */
public class RuneSteleRenderer implements BlockEntityRenderer<RuneSteleBlockEntity, RuneSteleRenderer.State> {
    private static final float ENTRANCE_TICKS = 14;

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
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        ComplexRune rune = state.rune;
        if (rune == null) {
            return;
        }
        float time = state.time + (state.seed & 127);
        float entrance = state.sinceChosen >= 0 && state.sinceChosen < ENTRANCE_TICKS ? state.sinceChosen / ENTRANCE_TICKS : 1;
        entrance = entrance * entrance * (3 - 2 * entrance);
        float pulse = state.active ? 0.8F + 0.2F * Mth.sin(time * 0.5F) : 0.85F + 0.15F * Mth.sin(time * 0.08F);
        int color = rune.color();
        float y = 1.55F + Mth.sin(time * 0.06F) * 0.04F;

        poseStack.pushPose();
        poseStack.translate(0.5, y, 0.5);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, (0.7F + (state.active ? 0.25F : 0)) * entrance, color,
                (state.active ? 0.55F : 0.3F) * pulse);
        // Thread of light down to the socket.
        HexFx.beam(poseStack, collector, 0, -(y - 1.0F), 0, 0.06F * entrance, color, 0.5F * pulse,
                new Vector3f(0, 0, 1).rotate(camera.orientation));
        // Always readable: the glyph faces the viewer and sways a little.
        poseStack.rotate(camera.orientation);
        poseStack.rotate(Axis.ZP.rotationDegrees(Mth.sin(time * 0.05F) * 4));
        float size = 0.46F * entrance * (state.active ? 1.08F : 1);
        float alpha = Math.min(1, entrance * 1.2F) * pulse;
        HexFx.glyph(poseStack, collector, rune, size, color, alpha);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(RuneSteleBlockEntity stele) {
        return new AABB(stele.getBlockPos()).expandTowards(0, 1.2, 0).inflate(0.3);
    }

    public static final class State extends BlockEntityRenderState {
        float time;
        @Nullable
        ComplexRune rune;
        boolean active;
        float sinceChosen;
        int seed;
    }
}
