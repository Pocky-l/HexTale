package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexFoundryBlock;
import com.pockyl.hextale.block.HexFoundryBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.rune.Essence;

/**
 * A formed Hex Foundry, drawn from its hatch: the whole kiln is one model built round its hollow heart, facing north,
 * turned to the hatch's facing. While it is hot, the glowing parts (the hatch's grille and the melt under the top grate)
 * are drawn again at full light, with a soft ember haze in front of them that swells with the heat; fire essence drawn
 * from a jar shows as the jar's stream into the top grate.
 */
public class HexFoundryRenderer implements BlockEntityRenderer<HexFoundryBlockEntity, HexFoundryRenderer.State> {
    private static final int EMBER = 0xFF8A3C;
    private static final int WHITE_HOT = 0xFFE2A8;

    public HexFoundryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HexFoundryBlockEntity foundry, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(foundry, state, partialTick, cameraPosition, breakProgress);
        Level level = foundry.getLevel();
        BlockPos pos = foundry.getBlockPos();
        state.formed = foundry.getBlockState().getValue(HexFoundryBlock.FORMED);
        state.facing = foundry.getBlockState().getValue(HexFoundryBlock.FACING);
        state.heat = foundry.heat() / (float) HexFoundryBlockEntity.MAX_HEAT;
        state.time = HexFx.time(level, partialTick) + (pos.asLong() & 127);
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        state.heartAt = Vec3.atLowerCornerOf(foundry.heart()).subtract(origin);
        state.grateAt = foundry.grate().subtract(origin).add(0, 0.45, 0);
        state.stream = null;
        BlockPos jar = foundry.drawing();
        if (jar != null && level != null) {
            float age = level.getGameTime() + partialTick - foundry.drewAt();
            state.stream = new JarStreamFx.Stream(JarReach.mouth(jar).subtract(origin), state.grateAt, Essence.FIRE.color(),
                    age / 12F, (age - (HexFoundryBlockEntity.STREAM_TICKS - 14)) / 12F, (int) pos.asLong());
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(state.heartAt.x, state.heartAt.y, state.heartAt.z);
        TableModels.rotateToFacing(poseStack, state.facing);
        TableModels.submit(poseStack, collector, TableModels.HEX_FOUNDRY, state.lightCoords);
        if (state.heat >= HexFoundryBlockEntity.MELT_HEAT / (float) HexFoundryBlockEntity.MAX_HEAT) {
            TableModels.submit(poseStack, collector, TableModels.HEX_FOUNDRY_GLOW, LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();

        if (state.heat > 0.05F) {
            float pulse = 0.85F + 0.15F * Mth.sin(state.time * 0.12F);
            int colour = PixelArt.mix(EMBER, WHITE_HOT, Mth.clamp((state.heat - 0.5F) * 2, 0, 1));
            // The haze in front of the grille: the hatch's own block, a little out of its front face.
            Direction front = state.facing;
            poseStack.pushPose();
            poseStack.translate(0.5 + front.getStepX() * 0.62, 0.42, 0.5 + front.getStepZ() * 0.62);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.9F, colour, 0.4F * state.heat * pulse);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(state.grateAt.x, state.grateAt.y, state.grateAt.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 1.3F, colour, 0.35F * state.heat * pulse);
            poseStack.popPose();
        }
        if (state.stream != null) {
            Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
            JarStreamFx.submit(poseStack, collector, camera.orientation, eye, state.stream, state.time);
        }
    }

    /** The kiln round its heart, and room for a stream from any jar within reach. */
    @Override
    public AABB getRenderBoundingBox(HexFoundryBlockEntity foundry) {
        return new AABB(foundry.heart()).inflate(JarReach.RADIUS + 2);
    }

    public static final class State extends BlockEntityRenderState {
        boolean formed;
        Direction facing = Direction.NORTH;
        float heat;
        float time;
        Vec3 heartAt = Vec3.ZERO;
        Vec3 grateAt = Vec3.ZERO;
        JarStreamFx.@Nullable Stream stream;
    }
}
