package com.pockyl.hextale.block;

import com.pockyl.hextale.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Chem Lumen: a gas street lamp on a floor, a wall or a ceiling, built once and burning for good. A green flame in
 * a glass chimney, and smoke that leaves through the slit under the vent cap.
 */
public class ChemLumenBlock extends FaceAttachedHorizontalDirectionalBlock {
    /** A greenish grey: gas smoke, not chimney soot. */
    private static final int SMOKE = 0x7C8A6E;
    /** The flame's tip and the slit under the cap, in px of the north-facing model. */
    private static final double HEAD_Z = 6;
    private static final double HANGING_Z = 8;
    private static final double TIP_Y = 12;
    private static final double HANGING_TIP_Y = 7;
    private static final double SLIT_Y = 14.5;
    private static final double HANGING_SLIT_Y = 9.5;
    private static final double SLIT_RADIUS = 3;
    /** The shade is four sloped planes, which the shape generator cannot see: its plate is added here, per way the head leans. */
    private static final VoxelShape SHADE_NORTH = Block.box(3, 13, 1, 13, 14, 11);
    private static final VoxelShape SHADE_EAST = Block.box(5, 13, 3, 15, 14, 13);
    private static final VoxelShape SHADE_SOUTH = Block.box(3, 13, 5, 13, 14, 15);
    private static final VoxelShape SHADE_WEST = Block.box(1, 13, 3, 11, 14, 13);
    private static final VoxelShape SHADE_HANGING = Block.box(3, 8, 3, 13, 9, 13);
    private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    public ChemLumenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACE, AttachFace.FLOOR).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }

    /** It takes the face clicked if it can, then the others in the order the player looks; on a floor it leans to the player. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction towards : context.getNearestLookingDirections()) {
            BlockState state = towards.getAxis().isHorizontal()
                    ? defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, towards.getOpposite())
                    : defaultBlockState().setValue(FACE, towards == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR)
                    .setValue(FACING, context.getHorizontalDirection().getOpposite());
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        return null;
    }

    /** A wall has to be solid; floors and ceilings only under the middle, as for a lantern (a post, a chain). */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction towards = getConnectedDirection(state).getOpposite();
        BlockPos support = pos.relative(towards);
        return towards.getAxis().isHorizontal() ? level.getBlockState(support).isFaceSturdy(level, support, towards.getOpposite())
                : Block.canSupportCenter(level, support, towards.getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.computeIfAbsent(state, s -> Shapes.or(ModelShapes.of(s), shade(s)));
    }

    private static VoxelShape shade(BlockState state) {
        if (state.getValue(FACE) == AttachFace.CEILING) {
            return SHADE_HANGING;
        }
        return switch (state.getValue(FACING)) {
            case EAST -> SHADE_EAST;
            case SOUTH -> SHADE_SOUTH;
            case WEST -> SHADE_WEST;
            default -> SHADE_NORTH;
        };
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean hanging = state.getValue(FACE) == AttachFace.CEILING;
        double headZ = hanging ? HANGING_Z : HEAD_Z;
        if (random.nextInt(4) == 0) {
            // A thread of soot inside the chimney, seen through the clear window.
            double[] tip = turned(state, 8, headZ);
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + tip[0], pos.getY() + (hanging ? HANGING_TIP_Y : TIP_Y) / 16, pos.getZ() + tip[1], 0, 0.005, 0);
        }
        if (random.nextInt(3) == 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            double[] slit = turned(state, 8 + Math.cos(angle) * SLIT_RADIUS, headZ + Math.sin(angle) * SLIT_RADIUS);
            level.addParticle(ModParticles.vapour(SMOKE), pos.getX() + slit[0], pos.getY() + (hanging ? HANGING_SLIT_Y : SLIT_Y) / 16, pos.getZ() + slit[1],
                    (random.nextDouble() - 0.5) * 0.004, 0.012 + random.nextDouble() * 0.008, (random.nextDouble() - 0.5) * 0.004);
        }
    }

    /** A point of the north-facing model in px, as blocks from the cell's corner once the blockstate has turned it. */
    private static double[] turned(BlockState state, double x, double z) {
        double dx = (x - 8) / 16;
        double dz = (z - 8) / 16;
        return switch (state.getValue(FACING)) {
            case EAST -> new double[]{0.5 - dz, 0.5 + dx};
            case SOUTH -> new double[]{0.5 - dx, 0.5 - dz};
            case WEST -> new double[]{0.5 + dz, 0.5 - dx};
            default -> new double[]{0.5 + dx, 0.5 + dz};
        };
    }
}
