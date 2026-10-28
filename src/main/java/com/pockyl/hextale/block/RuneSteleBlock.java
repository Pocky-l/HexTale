package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.OpenStelePayload;

import org.jspecify.annotations.Nullable;

/**
 * A rune stele: a standing stone two blocks tall - a stepped plinth, a slab with a channel carved down its faces, and a
 * ring head with a brass-lined window where the chosen rune turns. Use it to choose one of your runes for the ritual
 * circle; sneak-use clears it. {@code lit} (on both halves) while it carries a rune. The rune and its block entity live in
 * the {@link DoubleBlockHalf#LOWER} half; the upper half passes a click down, and breaking either half breaks both and
 * drops one stele. A stele whose upper half cannot stand (one built before steles stood two blocks tall, under a
 * ceiling) keeps working on its own and grows its upper half once there is room.
 */
public class RuneSteleBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /** How soon a stele without its upper half looks for room again. */
    private static final int GROW_TICKS = 100;

    public RuneSteleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, HALF);
    }

    public static boolean upper(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    /** The lower half's position (where the block entity lives) for a block of either half. */
    public static BlockPos base(BlockState state, BlockPos pos) {
        return upper(state) ? pos.below() : pos;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= (level.getMaxBuildHeight() - 1) || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /** Whether the upper half may stand over {@code lower}: the space is in the world and free (air, grass, snow...). */
    static boolean roomAbove(LevelReader level, BlockPos lower) {
        BlockPos above = lower.above();
        return above.getY() <= (level.getMaxBuildHeight() - 1) && level.getBlockState(above).canBeReplaced();
    }

    /** A lower half set without its top (a command, a structure, a stele from an older world) grows it soon. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!upper(state) && !level.isClientSide() && !(level.getBlockState(pos.above()).is(this))) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (upper(state) || level.getBlockState(pos.above()).is(this)) {
            return;
        }
        if (roomAbove(level, pos)) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        } else {
            level.scheduleTick(pos, this, GROW_TICKS);
        }
    }

    /** The upper half cannot stand without its base; the base stands alone (it grows its upper half again in time). */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (upper(state) && direction == Direction.DOWN && !(neighborState.is(this) && !upper(neighborState))) {
            return Blocks.AIR.defaultBlockState();
        }
        if (!upper(state) && direction == Direction.UP && !neighborState.is(this)) {
            level.scheduleTick(pos, this, GROW_TICKS);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * Breaking the upper half breaks the base too: it drops the stele when the player could have harvested it, and
     * nothing in creative. Breaking the base takes the upper half with it through {@link #updateShape}.
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
        BlockPos base = base(state, pos);
        if (!(level.getBlockEntity(base) instanceof RuneSteleBlockEntity stele)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            if (stele.rune() != null) {
                stele.choose(null, 1);
                level.playSound(null, base, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.7F, 0.8F);
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendToPlayer(serverPlayer, new OpenStelePayload(base));
        }
        return InteractionResult.SUCCESS;
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
        return ModelShapes.of(state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return upper(state) ? null : new RuneSteleBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<RuneSteleBlock> codec() {
        return simpleCodec(RuneSteleBlock::new);
    }
}
