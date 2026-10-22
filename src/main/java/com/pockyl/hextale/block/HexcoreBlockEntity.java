package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.registry.ModBlockEntities;

/** The Hexcore's block entity. It holds no state yet: it exists so the floating matrix can be drawn. */
public class HexcoreBlockEntity extends BlockEntity {
    public HexcoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEXCORE.get(), pos, state);
    }
}
