package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * The Rusted Railing: rails along the edges of a cell, not through its middle, so a one-block catwalk stays walkable. The
 * edge is the one nearest the click, and another railing placed in the cell adds an edge. The straight rail has no end
 * faces: every free rail end needs its post, which {@link #withPosts} works out from the neighbours. As tall as a fence for
 * anything walking into it, and the corner brace is left out of that. Over a catwalk in the lower half of its cell the rail
 * stands {@link #LOW}, on the deck: its pieces reach 8 px down into the cell below, as high over the deck as ever.
 */
public class RustedRailingBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty POST_NE = BooleanProperty.create("post_ne");
    public static final BooleanProperty POST_SE = BooleanProperty.create("post_se");
    public static final BooleanProperty POST_SW = BooleanProperty.create("post_sw");
    public static final BooleanProperty POST_NW = BooleanProperty.create("post_nw");
    public static final BooleanProperty LOW = BooleanProperty.create("low");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape COLLISION_NORTH = Block.box(0, 0, 0, 16, 24, 3);
    private static final VoxelShape COLLISION_EAST = Block.box(13, 0, 0, 16, 24, 16);
    private static final VoxelShape COLLISION_SOUTH = Block.box(0, 0, 13, 16, 24, 16);
    private static final VoxelShape COLLISION_WEST = Block.box(0, 0, 0, 3, 24, 16);
    private static final VoxelShape COLLISION_NORTH_LOW = Block.box(0, 0, 0, 16, 16, 3);
    private static final VoxelShape COLLISION_EAST_LOW = Block.box(13, 0, 0, 16, 16, 16);
    private static final VoxelShape COLLISION_SOUTH_LOW = Block.box(0, 0, 13, 16, 16, 16);
    private static final VoxelShape COLLISION_WEST_LOW = Block.box(0, 0, 0, 3, 16, 16);

    public RustedRailingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
                .setValue(POST_NE, false).setValue(POST_SE, false).setValue(POST_SW, false).setValue(POST_NW, false).setValue(LOW, false).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, POST_NE, POST_SE, POST_SW, POST_NW, LOW, WATERLOGGED);
    }

    private static BooleanProperty edge(Direction side) {
        return switch (side) {
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    /** The post on the corner between this side and the next one clockwise. */
    private static BooleanProperty post(Direction side) {
        return switch (side) {
            case EAST -> POST_SE;
            case SOUTH -> POST_SW;
            case WEST -> POST_NW;
            default -> POST_NE;
        };
    }

    /** The edge of the cell nearest the click; a click dead on the diagonal goes by the way the player looks. */
    private static Direction edgeAt(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        double x = context.getClickLocation().x - pos.getX() - 0.5;
        double z = context.getClickLocation().z - pos.getZ() - 0.5;
        if (Math.abs(x) == Math.abs(z)) {
            return context.getHorizontalDirection();
        }
        if (Math.abs(x) > Math.abs(z)) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        }
        return z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Whether a neighbour's rail runs on from this one: on the same side and at the same height. */
    private boolean railRunsOn(BlockState state, Direction side, boolean low) {
        return state.is(this) && state.getValue(edge(side)) && state.getValue(LOW) == low;
    }

    /** Whether the cell below ends in a deck 8 px under this one's floor. */
    private static boolean overLowDeck(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof RustedCatwalkBlock && below.getValue(RustedCatwalkBlock.HALF) == Half.BOTTOM;
    }

    /**
     * A corner gets a post when exactly one of its two sides has a rail and that rail does not run on into the neighbour
     * past the corner. Two rails meeting at the corner are the corner piece's work. A neighbour at the other height does
     * not continue the rail, so a step between low and normal rails has a post on either end.
     */
    private BlockState withPosts(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            Direction next = side.getClockWise();
            boolean here = state.getValue(edge(side));
            boolean there = state.getValue(edge(next));
            boolean free = here != there && !railRunsOn(level.getBlockState(pos.relative(here ? next : side)), here ? side : next, state.getValue(LOW));
            state = state.setValue(post(side), free);
        }
        return state;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState here = context.getLevel().getBlockState(pos);
        BlockState state = here.is(this) ? here
                : defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(pos).is(Fluids.WATER));
        return withPosts(state.setValue(edge(edgeAt(context)), true).setValue(LOW, overLowDeck(context.getLevel(), pos)), context.getLevel(), pos);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return context.getItemInHand().is(asItem()) && !state.getValue(edge(edgeAt(context)));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return direction == Direction.UP ? state : withPosts(state.setValue(LOW, overLowDeck(level, pos)), level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean low = state.getValue(LOW);
        VoxelShape shape = Shapes.empty();
        shape = state.getValue(NORTH) ? Shapes.or(shape, low ? COLLISION_NORTH_LOW : COLLISION_NORTH) : shape;
        shape = state.getValue(EAST) ? Shapes.or(shape, low ? COLLISION_EAST_LOW : COLLISION_EAST) : shape;
        shape = state.getValue(SOUTH) ? Shapes.or(shape, low ? COLLISION_SOUTH_LOW : COLLISION_SOUTH) : shape;
        return state.getValue(WEST) ? Shapes.or(shape, low ? COLLISION_WEST_LOW : COLLISION_WEST) : shape;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState turned = state;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            Direction to = rotation.rotate(side);
            turned = turned.setValue(edge(to), state.getValue(edge(side))).setValue(post(to), state.getValue(post(side)));
        }
        return turned;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        BlockState turned = state;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            turned = turned.setValue(edge(mirror.mirror(side)), state.getValue(edge(side)))
                    .setValue(post(mirror.mirror(side.getClockWise())), state.getValue(post(side)));
        }
        return turned;
    }
}
