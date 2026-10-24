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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The Whirl Reaper: a brass mast on a magic stone foot with wind vanes turning round it and a hextech crystal floating
 * over it. It stands among the beds and sends whirls of wind to whatever is ripe ({@link WhirlReaperBlockEntity});
 * a right click with an empty hand tells what it does now.
 */
public class WhirlReaperBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(6, 3, 6, 10, 13, 10));

    public WhirlReaperBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WhirlReaperBlockEntity reaper) {
            player.displayClientMessage(status(reaper), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** What it does now, in one line: field, pace, wind - or why it stands still. */
    public static Component status(WhirlReaperBlockEntity reaper) {
        if (reaper.becalmed()) {
            return Component.translatable("block.hextale.whirl_reaper.status.becalmed");
        }
        int size = reaper.radius() * 2 + 1;
        return Component.translatable("block.hextale.whirl_reaper.status", size, size,
                String.format(Locale.ROOT, "%.2f", reaper.interval() / 20.0F), reaper.charges());
    }

    /** A plant set or broken next to it: look again soon. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level.getBlockEntity(pos) instanceof WhirlReaperBlockEntity reaper) {
            reaper.wake();
        }
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
        return new WhirlReaperBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.WHIRL_REAPER.get(),
                level.isClientSide() ? WhirlReaperBlockEntity::clientTick : WhirlReaperBlockEntity::serverTick);
    }
}
