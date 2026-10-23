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
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * Rune Shackles: a nullstone floor plate with a ring of runes cut into it and brass studs at its corners. Creatures
 * that step onto it hang still in its glyph ({@link RuneShacklesBlockEntity}); a right click with an empty hand tells
 * what it holds.
 */
public class RuneShacklesBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public RuneShacklesBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RuneShacklesBlockEntity plate) {
            player.sendOverlayMessage(status(plate));
        }
        return InteractionResult.SUCCESS;
    }

    /** What it does now, in one line: how many it holds and for how long its shadow lasts - or that it has none. */
    public static Component status(RuneShacklesBlockEntity plate) {
        if (plate.dark()) {
            return Component.translatable("block.hextale.rune_shackles.status.dark");
        }
        return Component.translatable("block.hextale.rune_shackles.status", plate.held().length, RuneShacklesBlockEntity.CAPACITY,
                plate.budget() / 20);
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
        return new RuneShacklesBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.RUNE_SHACKLES.get(), RuneShacklesBlockEntity::serverTick);
    }
}
