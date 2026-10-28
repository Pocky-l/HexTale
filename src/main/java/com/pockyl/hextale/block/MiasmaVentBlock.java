package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * The Miasma Vent: a rusted chemtech stack that breathes choking fumes over the hostile creatures around it
 * ({@link MiasmaVentBlockEntity}), for nothing, for ever. A redstone signal holds it shut ({@link #POWERED}); a right click
 * with an empty hand tells what it does now.
 */
public class MiasmaVentBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** Grated base, the stack with its collars, the cowl and its peak, the two copper feed pipes. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 4, 15), Block.box(5, 4, 5, 11, 14, 11),
            Block.box(4, 14, 4, 12, 15, 12), Block.box(6, 15, 6, 10, 16, 10), Block.box(2, 4, 7, 4, 9, 9), Block.box(12, 4, 9, 14, 7, 11));

    public MiasmaVentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.sendOverlayMessage(status(state));
        }
        return InteractionResult.SUCCESS;
    }

    /** What the vent does now: breathing over its reach, or held shut by a redstone signal. */
    public static Component status(BlockState state) {
        int side = MiasmaVentBlockEntity.RADIUS * 2 + 1;
        return state.getValue(POWERED) ? Component.translatable("block.hextale.miasma_vent.status.shut")
                : Component.translatable("block.hextale.miasma_vent.status", side, side);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MiasmaVentBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MIASMA_VENT.get(),
                level.isClientSide() ? MiasmaVentBlockEntity::clientTick : MiasmaVentBlockEntity::serverTick);
    }

    @Override
    protected com.mojang.serialization.MapCodec<MiasmaVentBlock> codec() {
        return simpleCodec(MiasmaVentBlock::new);
    }
}
