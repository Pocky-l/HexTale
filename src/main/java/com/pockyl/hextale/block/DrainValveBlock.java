package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * The Drain Valve: pipes (or a tank right beside it) join it at its sides, and while it is open whatever they bring -
 * essence or water - pours out of its spout into the world, and it drains the Essence Tanks it is joined to (what that
 * spilled essence does to the world comes later). Using it turns the handwheel: open or shut.
 */
public class DrainValveBlock extends BaseEntityBlock implements EssenceMachine {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 5, 5, 11, 11, 11), Block.box(7, 0, 7, 9, 5, 9),
            Block.box(4, 11, 4, 12, 14, 12));

    public DrainValveBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(OPEN, false);
        for (Direction face : Direction.Plane.HORIZONTAL) {
            state = state.setValue(PORTS.get(face), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
        for (Direction face : Direction.Plane.HORIZONTAL) {
            builder.add(PORTS.get(face));
        }
    }

    /** The four sides: the spout is below and the handwheel on top. */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return face.getAxis().isHorizontal();
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            boolean open = !state.getValue(OPEN);
            level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
            level.playSound(null, pos, open ? SoundEvents.COPPER_TRAPDOOR_OPEN : SoundEvents.COPPER_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 1.1F);
            player.sendOverlayMessage(Component.translatable(open ? "block.hextale.drain_valve.open" : "block.hextale.drain_valve.shut"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = SHAPE;
        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (state.getValue(PORTS.get(face))) {
                shape = Shapes.or(shape, switch (face) {
                    case NORTH -> Block.box(5, 5, 0, 11, 11, 5);
                    case SOUTH -> Block.box(5, 5, 11, 11, 11, 16);
                    case WEST -> Block.box(0, 5, 5, 5, 11, 11);
                    default -> Block.box(11, 5, 5, 16, 11, 11);
                });
            }
        }
        return shape;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrainValveBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.DRAIN_VALVE.get(), DrainValveBlockEntity::serverTick);
    }
}
