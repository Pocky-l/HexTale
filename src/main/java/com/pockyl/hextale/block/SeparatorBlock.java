package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * The Separator: draws one element out of a mixture with a reagent. Its front (facing the player who placed it) lets the
 * rest of the mixture out, its top the drawn-out concentrate; pipes bring the mixture in at the back and the sides.
 */
public class SeparatorBlock extends BaseEntityBlock implements WorldlyContainerHolder, EssenceMachine {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final Direction[] PORT_FACES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP};

    public SeparatorBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
        for (Direction face : PORT_FACES) {
            state = state.setValue(PORTS.get(face), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        for (Direction face : PORT_FACES) {
            builder.add(PORTS.get(face));
        }
    }

    /** Every face but the bottom: the mixture comes in at the back and the sides, the rest leaves at the front, the
     * concentrate at the top. */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return face != Direction.DOWN;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return EssenceMachine.connect(state, direction, neighbour);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return EssenceMachine.connectAll(context.getLevel(), context.getClickedPos(),
                defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SeparatorBlockEntity separator) {
            player.openMenu(separator, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return EssenceMachine.rotatePorts(state.setValue(FACING, rotation.rotate(state.getValue(FACING))), rotation);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SeparatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.SEPARATOR.get(), SeparatorBlockEntity::serverTick);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SeparatorBlockEntity separator ? separator : null;
    }
}
