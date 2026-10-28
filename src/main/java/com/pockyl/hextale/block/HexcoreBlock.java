package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * The Hexcore: the Hextech Altar after its heart woke up - a rune matrix floating over the altar's body, its brass claws
 * opened wide round it (the altar's block shape). It is used like the altar (item in hand onto it, sneak-use or redstone
 * to start); the matrix is drawn by {@link com.pockyl.hextale.client.render.HexcoreRenderer}.
 */
public class HexcoreBlock extends HextechAltarBlock {
    public HexcoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HexcoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.HEXCORE.get(), HextechAltarBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.HEXCORE.get(), HextechAltarBlockEntity::serverTick);
    }
}
