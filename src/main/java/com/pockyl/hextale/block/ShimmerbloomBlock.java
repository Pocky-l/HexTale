package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

/**
 * Shimmerbloom: the glowing purple flower of deep caves, the root of shimmer. It grows on cave floors and hangs from
 * cave ceilings ({@link #HANGING}), breathes soft violet motes and lights the stone around it. Bone meal makes a new
 * bloom take root somewhere close by, on a floor or a ceiling it could hold to.
 */
public class ShimmerbloomBlock extends Block implements BonemealableBlock {
    public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
    private static final VoxelShape FLOOR = Block.column(10.0, 0.0, 12.0);
    private static final VoxelShape CEILING = Block.column(10.0, 4.0, 16.0);
    private static final int SPREAD_TRIES = 12;
    private static final int SPREAD_RADIUS = 3;

    public ShimmerbloomBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HANGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HANGING);
    }

    /** Whether a bloom (hanging or standing) could grow here. */
    public static boolean canGrow(LevelReader level, BlockPos pos, boolean hanging) {
        if (level.isWaterAt(pos)) {
            return false;
        }
        return hanging ? Block.canSupportCenter(level, pos.above(), Direction.DOWN)
                : level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canGrow(level, pos, state.getValue(HANGING));
    }

    /** Placed against a ceiling it hangs; anywhere else it stands, if the block below holds it. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean ceiling = context.getClickedFace() == Direction.DOWN;
        for (boolean hanging : ceiling ? new boolean[] {true, false} : new boolean[] {false, true}) {
            if (canGrow(level, pos, hanging)) {
                return defaultBlockState().setValue(HANGING, hanging);
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return direction.getAxis() == Direction.Axis.Y && !canSurvive(state, level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? CEILING : FLOOR;
    }

    /** Violet motes drift up from the petals (and fall from a hanging bloom), now and then far from the flower. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean hanging = state.getValue(HANGING);
        if (random.nextInt(3) == 0) {
            double y = pos.getY() + (hanging ? 0.35 : 0.55) + random.nextDouble() * 0.2;
            level.addParticle(ModParticles.SHIMMER_MOTE.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, y,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, hanging ? -0.01 : 0.012, 0);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ModParticles.SHIMMER_MOTE.get(), pos.getX() - 1.5 + random.nextDouble() * 4,
                    pos.getY() + random.nextDouble() * 2.5, pos.getZ() - 1.5 + random.nextDouble() * 4, 0, 0.005, 0);
        }
    }

    // ---- Bone meal: a new bloom close by --------------------------------------------------------------------------------

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        spread(level, random, pos);
    }

    /** Tries a few spots around for a new bloom; returns where it took root, or null. */
    public static @Nullable BlockPos spread(ServerLevel level, RandomSource random, BlockPos pos) {
        for (int i = 0; i < SPREAD_TRIES; i++) {
            BlockPos at = pos.offset(random.nextInt(SPREAD_RADIUS * 2 + 1) - SPREAD_RADIUS, random.nextInt(5) - 2,
                    random.nextInt(SPREAD_RADIUS * 2 + 1) - SPREAD_RADIUS);
            if (!level.getBlockState(at).isAir()) {
                continue;
            }
            boolean hanging = random.nextInt(3) == 0;
            if (!canGrow(level, at, hanging)) {
                hanging = !hanging;
            }
            if (canGrow(level, at, hanging)) {
                level.setBlock(at, ModBlocks.SHIMMERBLOOM.get().defaultBlockState().setValue(HANGING, hanging), Block.UPDATE_ALL);
                level.sendParticles(ModParticles.SHIMMER_MOTE.get(), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.01);
                return at;
            }
        }
        return null;
    }
}
