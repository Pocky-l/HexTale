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
 * The Rune Circle: a carved stone disc lying in the floor, where runes are burned into tools and devices and copied onto
 * blank scrolls. Rune steles stand round it; the Hextech Altar only makes things. It is used like the altar (item in hand
 * onto it, sneak-use with an empty hand or redstone to start).
 */
public class RuneCircleBlock extends HextechAltarBlock {
    public RuneCircleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RuneCircleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.RUNE_CIRCLE.get(), HextechAltarBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.RUNE_CIRCLE.get(), HextechAltarBlockEntity::serverTick);
    }
}
