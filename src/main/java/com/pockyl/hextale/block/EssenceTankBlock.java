package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import com.pockyl.hextale.essence.Flasks;
import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * An essence tank: stores a large amount of one liquid (whatever flows in mixes). Pipes fill it through the top and the
 * sides; it lets its liquid out at the bottom. A bottle of essence by hand pours in, a glass bottle draws out; an empty
 * hand reads it.
 */
public class EssenceTankBlock extends BaseEntityBlock implements EssenceMachine {
    public EssenceTankBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (Direction face : Direction.Plane.HORIZONTAL) {
            state = state.setValue(PORTS.get(face), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            builder.add(PORTS.get(face));
        }
    }

    /** Every face: in at the top and the sides, out at the bottom. Only the side joints show on the model. */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return EssenceMachine.connectAll(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return EssenceMachine.connect(state, direction, neighbour);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return EssenceMachine.rotatePorts(state, rotation);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof EssenceTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (Flasks.use(player, hand, stack, tank.tank())) {
            tank.setChanged();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EssenceTankBlockEntity tank) {
            player.sendOverlayMessage(Flasks.describe(tank.tank()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ESSENCE_TANK.get(), EssenceTankBlockEntity::serverTick);
    }
}
