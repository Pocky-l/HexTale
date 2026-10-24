package com.pockyl.hextale.port.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What a 26.x block entity renderer extracts before drawing.
 *
 * <p>port-of: net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
 */
public class BlockEntityRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public BlockState blockState = Blocks.AIR.defaultBlockState();
    public @Nullable BlockEntityType<?> blockEntityType;
    public int lightCoords;
    public ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress;

    public static void extractBase(BlockEntity blockEntity, BlockEntityRenderState state,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        state.blockPos = blockEntity.getBlockPos();
        state.blockState = blockEntity.getBlockState();
        state.blockEntityType = blockEntity.getType();
        state.lightCoords = blockEntity.getLevel() != null
                ? LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos()) : LightTexture.FULL_BRIGHT;
        state.breakProgress = breakProgress;
    }
}
