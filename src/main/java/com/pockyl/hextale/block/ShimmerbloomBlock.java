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
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

/**
 * Shimmerbloom: the glowing purple flower of deep caves, the root of shimmer. It grows on cave floors, hangs from
 * cave ceilings ({@link #HANGING}) and sticks out of cave walls ({@link #WALL}: the side it holds to), breathes soft
 * violet motes and lights the stone around it. Bone meal makes a new bloom take root somewhere close by, on a floor, a
 * ceiling or a wall it could hold to.
 */
public class ShimmerbloomBlock extends Block implements BonemealableBlock {
    public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
    /** The wall a bloom holds to; {@link Wall#NONE} for a standing or hanging bloom. */
    public static final EnumProperty<Wall> WALL = EnumProperty.create("wall", Wall.class);
    private static final VoxelShape FLOOR = Block.column(10.0, 0.0, 12.0);
    private static final VoxelShape CEILING = Block.column(10.0, 4.0, 16.0);
    private static final VoxelShape[] WALLS = {
            Block.box(3.0, 3.0, 0.0, 13.0, 13.0, 12.0),
            Block.box(3.0, 3.0, 4.0, 13.0, 13.0, 16.0),
            Block.box(0.0, 3.0, 3.0, 12.0, 13.0, 13.0),
            Block.box(4.0, 3.0, 3.0, 16.0, 13.0, 13.0)};
    private static final int SPREAD_TRIES = 12;
    private static final int SPREAD_RADIUS = 3;

    public ShimmerbloomBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HANGING, false).setValue(WALL, Wall.NONE));
    }

    /** The side a wall bloom holds to. */
    public enum Wall implements StringRepresentable {
        NONE(null), NORTH(Direction.NORTH), SOUTH(Direction.SOUTH), WEST(Direction.WEST), EAST(Direction.EAST);

        @Nullable
        private final Direction side;

        Wall(@Nullable Direction side) {
            this.side = side;
        }

        /** The direction of the block it holds to, or null. */
        public @Nullable Direction side() {
            return side;
        }

        public static Wall of(Direction side) {
            for (Wall wall : values()) {
                if (wall.side == side) {
                    return wall;
                }
            }
            return NONE;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HANGING, WALL);
    }

    /** A bloom holding to the wall on this side of it. */
    public BlockState onWall(Direction side) {
        return defaultBlockState().setValue(WALL, Wall.of(side));
    }

    /** Whether a bloom could hold to the wall on this side of the spot. */
    public static boolean canHold(LevelReader level, BlockPos pos, Direction side) {
        BlockPos wall = pos.relative(side);
        return side.getAxis().isHorizontal() && !level.isWaterAt(pos) && level.getBlockState(wall).isFaceSturdy(level, wall, side.getOpposite());
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
        Direction side = state.getValue(WALL).side();
        return side != null ? canHold(level, pos, side) : canGrow(level, pos, state.getValue(HANGING));
    }

    /** The side the bloom holds to: below, above or its wall. */
    private static Direction support(BlockState state) {
        Direction side = state.getValue(WALL).side();
        return side != null ? side : state.getValue(HANGING) ? Direction.UP : Direction.DOWN;
    }

    /** Placed against a wall it holds to it, against a ceiling it hangs; anywhere else it stands, if the block below holds it. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clicked = context.getClickedFace();
        if (clicked.getAxis().isHorizontal() && canHold(level, pos, clicked.getOpposite())) {
            return onWall(clicked.getOpposite());
        }
        boolean ceiling = clicked == Direction.DOWN;
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
        return direction == support(state) && !canSurvive(state, level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Wall wall = state.getValue(WALL);
        if (wall != Wall.NONE) {
            return WALLS[wall.ordinal() - 1];
        }
        return state.getValue(HANGING) ? CEILING : FLOOR;
    }

    /** Violet motes drift up from the petals (and fall from a hanging bloom), now and then far from the flower. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean hanging = state.getValue(HANGING);
        Direction wall = state.getValue(WALL).side();
        if (random.nextInt(3) == 0) {
            // The petals sit out from the wall a wall bloom holds to.
            double cx = wall != null ? 0.5 - wall.getStepX() * 0.2 : 0.5;
            double cz = wall != null ? 0.5 - wall.getStepZ() * 0.2 : 0.5;
            double y = pos.getY() + (wall != null ? 0.45 : hanging ? 0.35 : 0.55) + random.nextDouble() * 0.2;
            level.addParticle(ModParticles.SHIMMER_MOTE.get(), pos.getX() + cx - 0.2 + random.nextDouble() * 0.4, y,
                    pos.getZ() + cz - 0.2 + random.nextDouble() * 0.4, 0, hanging && wall == null ? -0.01 : 0.012, 0);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ModParticles.SHIMMER_MOTE.get(), pos.getX() - 1.5 + random.nextDouble() * 4,
                    pos.getY() + random.nextDouble() * 2.5, pos.getZ() - 1.5 + random.nextDouble() * 4, 0, 0.005, 0);
        }
    }

    // ---- Bone meal: a new bloom close by --------------------------------------------------------------------------------

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
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
            BlockState bloom = null;
            Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            if (random.nextInt(3) == 0 && canHold(level, at, side)) {
                bloom = ModBlocks.SHIMMERBLOOM.get().onWall(side);
            } else if (canGrow(level, at, hanging)) {
                bloom = ModBlocks.SHIMMERBLOOM.get().defaultBlockState().setValue(HANGING, hanging);
            } else if (canHold(level, at, side)) {
                bloom = ModBlocks.SHIMMERBLOOM.get().onWall(side);
            }
            if (bloom != null) {
                level.setBlock(at, bloom, Block.UPDATE_ALL);
                level.sendParticles(ModParticles.SHIMMER_MOTE.get(), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.01);
                return at;
            }
        }
        return null;
    }
}
