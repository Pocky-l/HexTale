package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * A brass pipe for essence. Pipes carry no liquid themselves: a machine pushing into a pipe reaches every vessel the
 * connected pipes touch (see {@code EssenceNetwork}). Each side shows whether it joins a pipe or a machine.
 */
public class BrassPipeBlock extends Block {
    public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);

    public BrassPipeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : SIDES.values()) {
            state = state.setValue(side, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIDES.values().toArray(BooleanProperty[]::new));
    }

    public static boolean connects(BlockState neighbour) {
        return neighbour.getBlock() instanceof BrassPipeBlock || neighbour.getBlock() instanceof EssenceMachine;
    }

    private BlockState connected(LevelReader level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(SIDES.get(direction), connects(level.getBlockState(pos.relative(direction))));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connected(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return state.setValue(SIDES.get(direction), connects(neighbour));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(SIDES.get(direction))) {
                shape = Shapes.or(shape, Block.box(
                        direction.getStepX() < 0 ? 0 : direction.getStepX() > 0 ? 11 : 5,
                        direction.getStepY() < 0 ? 0 : direction.getStepY() > 0 ? 11 : 5,
                        direction.getStepZ() < 0 ? 0 : direction.getStepZ() > 0 ? 11 : 5,
                        direction.getStepX() < 0 ? 5 : direction.getStepX() > 0 ? 16 : 11,
                        direction.getStepY() < 0 ? 5 : direction.getStepY() > 0 ? 16 : 11,
                        direction.getStepZ() < 0 ? 5 : direction.getStepZ() > 0 ? 16 : 11));
            }
        }
        return shape;
    }
}
