package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.chem.Flask;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModSounds;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The Lab Bench: a two-block dark oak bench with a tiled top, like the Research Table. On the main half (the left one
 * seen from the front, holding the block entity) the flask stands in a ring stand over a small chem burner; on the side
 * half a rack of droppers, an open journal and a beaker. {@link #LIT} while the burner under the flask is lit. Halves are
 * placed and broken together, like a bed; either half opens the screen and feeds hoppers.
 */
public class LabBenchBlock extends BaseEntityBlock implements WorldlyContainerHolder {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<TablePart> PART = EnumProperty.create("part", TablePart.class);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    /** The flask's belly and its mouth, in main block model pixels (see {@link TableGeometry}). */
    public static final Vec3 FLASK = new Vec3(7.5, 18, 7.5);
    public static final Vec3 MOUTH = new Vec3(7.5, 24, 7.5);
    /** Outline boxes in design space {u0, y0, z0, u1, y1, z1}, u along the bench from the viewer's left end. */
    private static final double[][] BOXES = {
            {0, 10, 1, 32, 12, 15},     // top
            {1, 0, 2, 3, 10, 14},       // legs
            {29, 0, 2, 31, 10, 14},
            {3, 12, 4, 12, 24, 11},     // ring stand and flask
            {17, 12, 2, 30, 16, 13},    // dropper rack and beaker
    };
    private static final Map<Direction, VoxelShape> MAIN_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> SIDE_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            MAIN_SHAPES.put(facing, TableGeometry.shape(BOXES, TablePart.MAIN, facing));
            SIDE_SHAPES.put(facing, TableGeometry.shape(BOXES, TablePart.SIDE, facing));
        }
    }

    public LabBenchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, TablePart.MAIN).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, LIT);
    }

    /** The block holding the block entity, for either half. */
    public static BlockPos mainPos(BlockState state, BlockPos pos) {
        return state.getValue(PART) == TablePart.MAIN ? pos : pos.relative(TablePart.SIDE.toOther(state.getValue(FACING)));
    }

    /** Places a whole bench (both halves) with its main block at {@code pos}; used by tests. */
    public static void place(Level level, BlockPos pos, BlockState main) {
        level.setBlock(pos, main.setValue(PART, TablePart.MAIN), Block.UPDATE_ALL);
        level.setBlock(pos.relative(TablePart.MAIN.toOther(main.getValue(FACING))), main.setValue(PART, TablePart.SIDE).setValue(LIT, false),
                Block.UPDATE_ALL);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos side = context.getClickedPos().relative(TablePart.MAIN.toOther(facing));
        Level level = context.getLevel();
        if (!level.getBlockState(side).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(side)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            level.setBlock(pos.relative(TablePart.MAIN.toOther(state.getValue(FACING))), state.setValue(PART, TablePart.SIDE).setValue(LIT, false),
                    Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        }
    }

    /** A half whose partner is gone turns to air (the main half then drops the bench and its contents). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        TablePart part = state.getValue(PART);
        if (direction == part.toOther(state.getValue(FACING))) {
            boolean partner = neighbour.is(this) && neighbour.getValue(PART) != part && neighbour.getValue(FACING) == state.getValue(FACING);
            return partner ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
    }

    /** In creative, breaking the side half must not let the main half drop the bench. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative() && state.getValue(PART) == TablePart.SIDE) {
            BlockPos main = mainPos(state, pos);
            BlockState mainState = level.getBlockState(main);
            if (mainState.is(this) && mainState.getValue(PART) == TablePart.MAIN) {
                level.setBlock(main, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, main, Block.getId(mainState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos main = mainPos(state, pos);
        if (!level.isClientSide() && level.getBlockEntity(main) instanceof LabBenchBlockEntity bench) {
            player.openMenu(bench, buf -> buf.writeBlockPos(main));
        }
        return InteractionResult.SUCCESS;
    }

    /** While the burner is lit: a faint green flame licks the flask, and a hot flask steams. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || !(level.getBlockEntity(pos) instanceof LabBenchBlockEntity bench)) {
            return;
        }
        Vec3 flask = bench.flaskPosition();
        if (random.nextInt(3) == 0) {
            level.addParticle(new DustParticleOptions(0x7CFF6A, 0.4F), flask.x + (random.nextDouble() - 0.5) * 0.06, flask.y - 0.25,
                    flask.z + (random.nextDouble() - 0.5) * 0.06, 0, 0.01, 0);
        }
        Flask contents = bench.flask();
        if (!contents.isEmpty() && contents.temperature() > 55 && random.nextInt(4) == 0) {
            Vec3 mouth = bench.mouthPosition();
            level.addParticle(ParticleTypes.WHITE_SMOKE, mouth.x, mouth.y, mouth.z, 0, 0.02, 0);
        }
        if (random.nextInt(50) == 0) {
            level.playLocalSound(pos, ModSounds.CHEM_BURNER_ROAR.get(), SoundSource.BLOCKS, 0.25F, 1.3F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(PART) == TablePart.MAIN ? MAIN_SHAPES : SIDE_SHAPES).get(state.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == TablePart.MAIN ? new LabBenchBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(PART) != TablePart.MAIN) {
            return null;
        }
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.LAB_BENCH.get(), LabBenchBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.LAB_BENCH.get(), LabBenchBlockEntity::serverTick);
    }

    /** Hoppers and droppers reach the bench's slots through either half. */
    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(mainPos(state, pos)) instanceof LabBenchBlockEntity bench ? bench : null;
    }
}
