package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The Hex Arc Spire: two blocks tall - a stone plinth, a hexsteel mast wound with brass coils that narrow upward, a
 * needle and a pronged crown, a hextech crystal held over it. It strikes the hostile creatures around it with
 * arcs of charge ({@link ArcSpireBlockEntity}, on the {@link DoubleBlockHalf#LOWER} half only; the upper half passes a
 * click down). Breaking either half breaks both and drops one spire. A spire whose upper half cannot stand (an old
 * one-block spire under a ceiling) keeps working on its own and grows its upper half once there is room.
 */
public class ArcSpireBlock extends BaseEntityBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /** Plinth, its step and the foot of the mast with the lower coils. */
    private static final VoxelShape LOWER = Shapes.or(Block.box(1, 0, 1, 15, 2, 15), Block.box(3, 2, 3, 13, 5, 13),
            Block.box(4, 5, 4, 12, 16, 12));
    /** The mast with the upper coils, then the needle and the crown. */
    private static final VoxelShape UPPER = Shapes.or(Block.box(5, 0, 5, 11, 12, 11), Block.box(6, 12, 6, 10, 15, 10));

    public ArcSpireBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    public static boolean upper(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    /** The lower half's position (where the block entity lives) for a block of either half. */
    public static BlockPos base(BlockState state, BlockPos pos) {
        return upper(state) ? pos.below() : pos;
    }

    /** The spire's work at a block of either half, or null (an upper half left without its base). */
    public static @Nullable ArcSpireBlockEntity spireAt(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(base(state, pos)) instanceof ArcSpireBlockEntity spire ? spire : null;
    }

    /** Whether the upper half may stand over {@code lower}: the space is in the world and free (air, grass, snow...). */
    static boolean roomAbove(Level level, BlockPos lower) {
        BlockPos above = lower.above();
        return above.getY() <= (level.getMaxBuildHeight() - 1) && level.getBlockState(above).canBeReplaced();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        return pos.getY() < (level.getMaxBuildHeight() - 1) && level.getBlockState(pos.above()).canBeReplaced(context) ? defaultBlockState() : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /** The upper half cannot stand without its base; the base stands alone (it grows its upper half again in time). */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (upper(state) && direction == Direction.DOWN && !(neighborState.is(this) && !upper(neighborState))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * Breaking the upper half breaks the base too: it drops the spire (with its runes) when the player could have
     * harvested it, and nothing in creative. Breaking the base takes the upper half with it through {@link #updateShape}.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && upper(state)) {
            BlockPos below = pos.below();
            BlockState base = level.getBlockState(below);
            if (base.is(this) && !upper(base)) {
                if (player.getAbilities().instabuild || !player.hasCorrectToolForDrops(base)) {
                    level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    level.levelEvent(player, 2001, below, Block.getId(base));
                } else {
                    level.destroyBlock(below, true, player);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            ArcSpireBlockEntity spire = spireAt(level, pos, state);
            if (spire != null) {
                player.displayClientMessage(status(spire), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** What it does now, in one line: reach, pace, charge - or why it holds back. */
    public static Component status(ArcSpireBlockEntity spire) {
        if (spire.uncharged()) {
            return Component.translatable("block.hextale.arc_spire.status.uncharged");
        }
        if (spire.waiting()) {
            return Component.translatable("block.hextale.arc_spire.status.waiting");
        }
        int size = spire.radius() * 2 + 1;
        return Component.translatable("block.hextale.arc_spire.status", size, size,
                String.format(Locale.ROOT, "%.2f", spire.interval() / 20.0F), spire.targets(), spire.charges());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return upper(state) ? UPPER : LOWER;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return upper(state) ? null : new ArcSpireBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (upper(state)) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.ARC_SPIRE.get(),
                level.isClientSide() ? ArcSpireBlockEntity::clientTick : ArcSpireBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<ArcSpireBlock> codec() {
        return simpleCodec(ArcSpireBlock::new);
    }
}
