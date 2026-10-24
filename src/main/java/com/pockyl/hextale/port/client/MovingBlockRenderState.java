package com.pockyl.hextale.port.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

/**
 * A block drawn away from its place (a falling or carried block), as 26.x describes it; {@link SubmitNodeCollector}
 * draws it with the block renderer against the client level.
 *
 * <p>port-of: net.minecraft.client.renderer.block.MovingBlockRenderState
 */
public class MovingBlockRenderState {
    public BlockPos randomSeedPos = BlockPos.ZERO;
    public BlockPos blockPos = BlockPos.ZERO;
    public BlockState blockState = Blocks.AIR.defaultBlockState();
    public @Nullable Holder<Biome> biome;
    public @Nullable LevelLightEngine lightEngine;
}
