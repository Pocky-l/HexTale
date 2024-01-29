package com.pocky.hextale.common.blocks;

import com.pocky.hextale.common.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class BuddingHextechBlock extends BuddingAmethystBlock {

    private static final Direction[] DIRECTIONS = Direction.values();

    public BuddingHextechBlock(Properties p_152726_) {
        super(p_152726_);
    }

    public void randomTick(BlockState p_220898_, ServerLevel p_220899_, BlockPos p_220900_, RandomSource p_220901_) {
        if (p_220901_.nextInt(5) == 0) {
            Direction direction = DIRECTIONS[p_220901_.nextInt(DIRECTIONS.length)];
            BlockPos blockpos = p_220900_.relative(direction);
            BlockState blockstate = p_220899_.getBlockState(blockpos);
            Block block = null;
            if (canClusterGrowAtState(blockstate)) {
                block = ModBlocks.SMALL_HEXTECH_BUD.get();
            } else if (blockstate.is(ModBlocks.SMALL_HEXTECH_BUD.get()) && blockstate.getValue(HextechClusterBlock.FACING) == direction) {
                block = ModBlocks.MEDIUM_HEXTECH_BUD.get();
            } else if (blockstate.is(ModBlocks.MEDIUM_HEXTECH_BUD.get()) && blockstate.getValue(HextechClusterBlock.FACING) == direction) {
                block = ModBlocks.LARGE_HEXTECH_BUD.get();
            } else if (blockstate.is(ModBlocks.LARGE_HEXTECH_BUD.get()) && blockstate.getValue(HextechClusterBlock.FACING) == direction) {
                block = ModBlocks.HEXTECH_CLUSTER.get();
            }

            if (block != null) {
                BlockState blockstate1 = block.defaultBlockState().setValue(HextechClusterBlock.FACING, direction).setValue(HextechClusterBlock.WATERLOGGED, Boolean.valueOf(blockstate.getFluidState().getType() == Fluids.WATER));
                p_220899_.setBlockAndUpdate(blockpos, blockstate1);
            }

        }
    }
}
