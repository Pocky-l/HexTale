package com.pockyl.hextale.block;

import com.pockyl.hextale.item.ChainLauncherItem;
import com.pockyl.hextale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * The Chain Claw: the end of a Chain Launcher's line shot into a wall, a harpoon head bitten into the block. It has no
 * item and drops nothing; it goes with its line. A click hangs the player on the line at this end.
 */
public class ChainClawBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    public ChainClawBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.WALL));
    }

    /** The claw's state for a bite into the face whose outward normal is {@code normal}. */
    public static BlockState facing(BlockState state, Direction normal) {
        return switch (normal) {
            case UP -> state.setValue(FACE, AttachFace.FLOOR).setValue(FACING, Direction.NORTH);
            case DOWN -> state.setValue(FACE, AttachFace.CEILING).setValue(FACING, Direction.NORTH);
            default -> state.setValue(FACE, AttachFace.WALL).setValue(FACING, normal);
        };
    }

    /** The outward normal of the face the claw bit (a lever's rule): the floor points up, the ceiling down. */
    public static Direction normal(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            default -> state.getValue(HorizontalDirectionalBlock.FACING);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        return stack.getItem() instanceof ChainLauncherItem ? InteractionResult.PASS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer rider && server.getBlockEntity(pos) instanceof ChainAnchorBlockEntity anchor) {
            anchor.hangOn(server, rider);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChainAnchorBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != ModBlockEntities.CHAIN_ANCHOR.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<ChainAnchorBlockEntity>) ChainAnchorBlockEntity::serverTick;
    }
}
