package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

/**
 * The Hextech Lamppost: two blocks tall - a brass-and-walnut post on a magic stone foot and a lamp head with a refined
 * gemstone in it. Its light burns off the dirt of the work around it ({@link HextechLamppostBlockEntity}, on the
 * {@link DoubleBlockHalf#LOWER} half only; the upper half passes a click down). Breaking either half breaks both and drops
 * one lamppost. {@link #LIT} on both halves while the gem has light essence to burn; only the head shines.
 */
public class HextechLamppostBlock extends BaseEntityBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public HextechLamppostBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, LIT);
    }

    public static boolean upper(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }

    /** The lower half's position (where the block entity lives) for a block of either half. */
    public static BlockPos base(BlockState state, BlockPos pos) {
        return upper(state) ? pos.below() : pos;
    }

    public static @Nullable HextechLamppostBlockEntity postAt(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(base(state, pos)) instanceof HextechLamppostBlockEntity post ? post : null;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        return pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context) ? defaultBlockState() : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /** The head cannot stand without its post. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (upper(state) && direction == Direction.DOWN && !(neighborState.is(this) && !upper(neighborState))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /**
     * Breaking the head breaks the post too: it drops the lamppost (with its runes) when the player could have harvested
     * it, and nothing in creative. Breaking the post takes the head with it through {@link #updateShape}.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && upper(state)) {
            BlockPos below = pos.below();
            BlockState base = level.getBlockState(below);
            if (base.is(this) && !upper(base)) {
                if (player.preventsBlockDrops() || !player.hasCorrectToolForDrops(base)) {
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
            HextechLamppostBlockEntity post = postAt(level, pos, state);
            if (post != null) {
                player.sendOverlayMessage(post.status());
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** A lit head sheds a cold mote now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!upper(state) || !state.getValue(LIT) || random.nextInt(4) != 0) {
            return;
        }
        Vec3 gem = HextechLamppostBlockEntity.gem(pos.below());
        level.addParticle(ModParticles.glint(ModParticles.HEXTECH), gem.x + (random.nextDouble() - 0.5) * 0.25, gem.y - 0.1,
                gem.z + (random.nextDouble() - 0.5) * 0.25, 0, -0.005, 0);
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return upper(state) ? null : new HextechLamppostBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (upper(state) || level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.HEXTECH_LAMPPOST.get(), HextechLamppostBlockEntity::serverTick);
    }
}
