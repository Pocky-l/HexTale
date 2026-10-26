package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

/**
 * A lightning scar: a thin burnt line on the ground with a faint blue thread of charge still in it, left where a bolt ran
 * over the earth. Lies flat like redstone dust and joins the scars beside it, so a run of them draws a branching
 * Lichtenberg figure. Struck geodes lay them out from their craters; it wipes away at a touch and drops nothing.
 */
public class LightningScarBlock extends Block {
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 0.25);

    public LightningScarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return joined(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    /** The state with its arms reaching every scar beside it. */
    public BlockState joined(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), level.getBlockState(pos.relative(direction)).is(this));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN) {
            return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
        }
        if (direction.getAxis().isHorizontal()) {
            return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), neighborState.is(this));
        }
        return state;
    }

    /** Now and then the charge left in the line crawls along it as a spark. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(14) == 0) {
            level.addParticle(ModParticles.spark(ModParticles.HEXTECH), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.06,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.015, 0);
        }
    }
}
