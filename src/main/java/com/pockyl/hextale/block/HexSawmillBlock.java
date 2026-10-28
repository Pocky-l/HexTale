package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The Hex Sawmill: a dark oak stump with a hextech saw disc turning in a slot, under a brass guard with a crystal floating
 * over it. It stands in a grove, fells the grown trees around it and plants them again ({@link HexSawmillBlockEntity});
 * a right click with an empty hand tells what it does now.
 */
public class HexSawmillBlock extends BaseEntityBlock {

    public HexSawmillBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HexSawmillBlockEntity mill) {
            player.sendOverlayMessage(status(mill));
        }
        return InteractionResult.SUCCESS;
    }

    /** What it does now, in one line: field, pace, wind - or why it stands still. */
    public static Component status(HexSawmillBlockEntity mill) {
        if (mill.becalmed()) {
            return Component.translatable("block.hextale.hex_sawmill.status.becalmed");
        }
        int size = mill.radius() * 2 + 1;
        return Component.translatable("block.hextale.hex_sawmill.status", size, size,
                String.format(Locale.ROOT, "%.1f", mill.interval() / 20.0F), mill.wind() / HexSawmillBlockEntity.MB_PER_TREE);
    }

    /** A block set or broken next to it: look again soon. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level.getBlockEntity(pos) instanceof HexSawmillBlockEntity mill) {
            mill.wake();
        }
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
        return new HexSawmillBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.HEX_SAWMILL.get(),
                level.isClientSide() ? HexSawmillBlockEntity::clientTick : HexSawmillBlockEntity::serverTick);
    }
}
