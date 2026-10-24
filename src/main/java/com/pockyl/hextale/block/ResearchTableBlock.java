package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * A two-block dark oak writing desk with brass trim: a drawer cabinet and a bookshelf under the top, a book rest for
 * the diary, an inkwell, a leather writing pad and a brass arm from which the unstable hextech hangs in gimbal rings
 * (see its renderer). The main block (the left half seen from the front) holds the block entity; both halves open the
 * drawing screen and feed hoppers. Halves are placed and broken together, like a bed.
 */
public class ResearchTableBlock extends BaseEntityBlock implements WorldlyContainerHolder {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<TablePart> PART = EnumProperty.create("part", TablePart.class);
    /** The main half glows faintly while an orb of unstable hextech lies on the desk. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int LIGHT = 7;

    public ResearchTableBlock(Properties properties) {
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

    /** Places a whole desk (both halves) with its main block at {@code pos}; used by tests and commands. */
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

    /** A half whose partner is gone turns to air (the main half then drops the desk and its contents). */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        TablePart part = state.getValue(PART);
        if (direction == part.toOther(state.getValue(FACING))) {
            boolean partner = neighbour.is(this) && neighbour.getValue(PART) != part && neighbour.getValue(FACING) == state.getValue(FACING);
            return partner ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    /** In creative, breaking the side half must not let the main half drop the desk. */
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
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TableGeometry.shape(state.getValue(PART), state.getValue(FACING));
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
        return state.getValue(PART) == TablePart.MAIN ? new ResearchTableBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(PART) != TablePart.MAIN) {
            return null;
        }
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.RESEARCH_TABLE.get(), ResearchTableBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.RESEARCH_TABLE.get(), ResearchTableBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos main = mainPos(state, pos);
        if (!level.isClientSide() && level.getBlockEntity(main) instanceof MenuProvider menu) {
            player.openMenu(menu, buf -> buf.writeBlockPos(main));
        }
        return InteractionResult.SUCCESS;
    }

    /** Hoppers and droppers reach the table's slots through either half. */
    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(mainPos(state, pos)) instanceof ResearchTableBlockEntity table ? table : null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<ResearchTableBlock> codec() {
        return simpleCodec(ResearchTableBlock::new);
    }
}
