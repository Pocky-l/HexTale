package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.block.CampfireBlock;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * The Alembic: a glass flask on a brass stand that distils items into elemental ink. Put it on a heat source (fire,
 * a lit campfire, lava or magma) next to a Research Table: studied items given to it dissolve one by one, and the ink
 * runs from its spout into the table's vials. No screen: items go in by hand or by hopper, the flask shows the rest.
 */
public class AlembicBlock extends BaseEntityBlock implements WorldlyContainerHolder {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Heat below: the alembic works. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 12, 13), Block.box(6, 12, 6, 10, 15.5, 10));

    public AlembicBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    /** Whether the block below heats an alembic. */
    public static boolean isHeat(BlockState below) {
        if (below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)) {
            return below.getValue(CampfireBlock.LIT);
        }
        return below.is(BlockTags.FIRE) || below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK);
    }

    /** The spout turns to a Research Table beside it, else towards the player. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (context.getLevel().getBlockState(context.getClickedPos().relative(side)).getBlock() instanceof ResearchTableBlock) {
                facing = side;
                break;
            }
        }
        return defaultBlockState().setValue(FACING, facing)
                .setValue(LIT, isHeat(context.getLevel().getBlockState(context.getClickedPos().below())));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic) {
            alembic.setOwner(player);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        if (direction == Direction.DOWN) {
            return state.setValue(LIT, isHeat(neighbour));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
    }

    /** An item in hand goes into the flask (studied items only). */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return alembic.insert((ServerPlayer) player, stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /** An empty hand takes back what waits in the flask. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic) {
            alembic.takeBack(player);
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
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlembicBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.ALEMBIC.get(), AlembicBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.ALEMBIC.get(), AlembicBlockEntity::serverTick);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic ? alembic : null;
    }
}
