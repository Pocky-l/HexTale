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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The Hex Arc Spire: a hexsteel spike wound with brass coils on a magic stone foot, a hextech crystal held at its tip.
 * It strikes the hostile creatures around it with arcs of charge ({@link ArcSpireBlockEntity}); a right click with an
 * empty hand tells what it does now.
 */
public class ArcSpireBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(5, 3, 5, 11, 16, 11));

    public ArcSpireBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ArcSpireBlockEntity spire) {
            player.displayClientMessage(status(spire), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** What it does now, in one line: reach, pace, charge - or why it holds back. */
    public static Component status(ArcSpireBlockEntity spire) {
        if (spire.uncharged()) {
            return Component.translatable("block.hextale.arc_spire.status.uncharged");
        }
        if (spire.waiting()) {
            return Component.translatable("block.hextale.arc_spire.status.waiting");
        }
        int size = spire.radius() * 2 + 1;
        return Component.translatable("block.hextale.arc_spire.status", size, size,
                String.format(Locale.ROOT, "%.2f", spire.interval() / 20.0F), spire.targets(), spire.charges());
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
        return new ArcSpireBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ARC_SPIRE.get(),
                level.isClientSide() ? ArcSpireBlockEntity::clientTick : ArcSpireBlockEntity::serverTick);
    }
}
