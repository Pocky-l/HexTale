package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.rune.Essence;

import java.util.Map;

/**
 * A chalk sigil: one cell of a line drawn on the floor with Binding Chalk. Lines join their neighbours like redstone dust
 * (no climbing). A line alone does nothing; a line closed round at least one cell of floor is a binding figure
 * ({@link ChalkFigure}): all its cells wake ({@link #ACTIVE}, a faint shadow glow) and what stands inside is held.
 * No collision, no drops; a touch wipes a cell, and the figure opens.
 */
public class ChalkSigilBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);
    /** Part of a closed figure. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final Map<Direction, BooleanProperty> SIDES = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH,
            Direction.WEST, WEST);
    private static final VoxelShape SHAPE = com.pockyl.hextale.port.PortShapes.column(16.0, 0.0, 1.0);
    public static final int SHADOW = Essence.SHADOW.color();

    public ChalkSigilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
                .setValue(VARIANT, 0).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, VARIANT, ACTIVE);
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

    /** A new cell joined to the cells already drawn around it. */
    public BlockState drawn(BlockGetter level, BlockPos pos, RandomSource random) {
        BlockState state = defaultBlockState().setValue(VARIANT, random.nextInt(4));
        for (Map.Entry<Direction, BooleanProperty> side : SIDES.entrySet()) {
            state = state.setValue(side.getValue(), level.getBlockState(pos.relative(side.getKey())).is(this));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        BooleanProperty side = SIDES.get(direction);
        if (side == null) {
            return state;
        }
        boolean joined = neighborState.is(this);
        if (state.getValue(side) != joined) {
            // The drawing changed: the figure is looked at again next tick.
            level.scheduleTick(pos, this, 1);
            return state.setValue(side, joined);
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    /** The figure this cell belongs to is looked at: it wakes or sleeps, and its first cell holds what stands in it. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ChalkFigure.settle(level, pos);
    }

    /** A woken figure breathes faint shadow off its lines. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE) && random.nextInt(5) == 0) {
            level.addParticle(ModParticles.glint(SHADOW), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.05,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
        }
    }
}
