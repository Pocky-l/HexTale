package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.registry.ModBlockEntities;

/** A pump in water pushes {@value #WATER_PER_PUSH} mB of water up its pipe every {@value #EVERY} ticks. */
public class PumpBlockEntity extends BlockEntity {
    public static final int WATER_PER_PUSH = 100;
    public static final int EVERY = 5;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMP.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PumpBlockEntity pump) {
        if (level.getGameTime() % EVERY == 0 && state.getValue(PumpBlock.WATERLOGGED)) {
            EssenceNetwork.pushWater(level, pos, Direction.UP, WATER_PER_PUSH);
        }
    }
}
