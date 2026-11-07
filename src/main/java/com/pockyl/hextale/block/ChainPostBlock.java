package com.pockyl.hextale.block;

import com.pockyl.hextale.item.ChainLauncherItem;
import com.pockyl.hextale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * The Chain Post: a thin two-block pole with a hook on top that a Chain Launcher shot raises where its user stands,
 * and on a floor or a ceiling it hits (hanging down from a ceiling). Nothing bumps into it: it has an outline to click
 * and break, but no collision. It holds any number of lines, so shooting again
 * from a post joins the lines there. It has no item and drops nothing; it goes when its last line comes down. The
 * half touching the floor / ceiling is the base and keeps the lines ({@link ChainAnchorBlockEntity}). A click hangs
 * the player on the line that runs nearest to where they look.
 */
public class ChainPostBlock extends Block implements EntityBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    /** The outlines follow the Blockbench models chain_post_base / chain_post_top (a hanging post is them upside down). */
    private static final VoxelShape POLE = Block.box(7, 0, 7, 9, 16, 9);
    private static final VoxelShape STANDING_BASE = Shapes.or(POLE, Block.box(4, 0, 4, 12, 1, 12), Block.box(5, 1, 5, 11, 3, 11));
    private static final VoxelShape STANDING_TIP = Shapes.or(Block.box(7, 0, 7, 9, 13, 9), Block.box(6, 13, 6, 10, 16, 10));
    private static final VoxelShape HANGING_BASE = Shapes.or(POLE, Block.box(4, 15, 4, 12, 16, 12), Block.box(5, 13, 5, 11, 15, 11));
    private static final VoxelShape HANGING_TIP = Shapes.or(Block.box(7, 3, 7, 9, 16, 9), Block.box(6, 0, 6, 10, 3, 10));

    public ChainPostBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(HANGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, HANGING);
    }

    /** Whether this half touches the floor / ceiling (and keeps the post's lines). */
    public static boolean isBase(BlockState state) {
        return state.getValue(HANGING) == (state.getValue(HALF) == DoubleBlockHalf.UPPER);
    }

    /** The outward normal of the face the post is fixed to: up for a standing post, down for a hanging one. */
    public static Direction face(BlockState state) {
        return state.getValue(HANGING) ? Direction.DOWN : Direction.UP;
    }

    /** The base half of the post that has a half at {@code pos}. */
    public static BlockPos base(BlockPos pos, BlockState state) {
        return isBase(state) ? pos.immutable() : pos.relative(face(state).getOpposite()).immutable();
    }

    /** Raises a post at {@code base} fixed to the face with outward normal {@code face} (UP or DOWN). */
    public static void place(Level level, BlockPos base, Direction face, BlockState post) {
        boolean hanging = face == Direction.DOWN;
        BlockState baseState = post.setValue(HANGING, hanging).setValue(HALF, hanging ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
        BlockState tipState = post.setValue(HANGING, hanging).setValue(HALF, hanging ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
        level.setBlock(base, baseState, Block.UPDATE_CLIENTS);
        level.setBlock(base.relative(face), tipState, Block.UPDATE_ALL);
        level.updateNeighborsAt(base, post.getBlock(), null);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(HANGING)) {
            return isBase(state) ? HANGING_BASE : HANGING_TIP;
        }
        return isBase(state) ? STANDING_BASE : STANDING_TIP;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Direction tipward = face(state);
        Direction toOther = isBase(state) ? tipward : tipward.getOpposite();
        if (direction == toOther && !(neighbourState.is(this) && neighbourState.getValue(HANGING) == state.getValue(HANGING)
                && neighbourState.getValue(HALF) != state.getValue(HALF))) {
            return Blocks.AIR.defaultBlockState();
        }
        if (isBase(state) && direction == tipward.getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!isBase(state)) {
            return true;
        }
        Direction face = face(state);
        return Block.canSupportCenter(level, pos.relative(face.getOpposite()), face);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        // With a launcher in hand a click shoots (from this post when standing at it), it does not hang on.
        return stack.getItem() instanceof ChainLauncherItem ? InteractionResult.PASS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos base = base(pos, state);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer rider && server.getBlockEntity(base) instanceof ChainAnchorBlockEntity anchor) {
            anchor.hangOn(server, rider);
        }
        return level.getBlockEntity(base) instanceof ChainAnchorBlockEntity anchor && anchor.holdsAny() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isBase(state) ? new ChainAnchorBlockEntity(pos, state) : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || !isBase(state) || type != ModBlockEntities.CHAIN_ANCHOR.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<ChainAnchorBlockEntity>) ChainAnchorBlockEntity::serverTick;
    }
}
