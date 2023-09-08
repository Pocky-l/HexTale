package com.pocky.hextale.common.blocks;

import com.pocky.hextale.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class SuspiciousRemainsGravel extends BrushableBlock implements Fallable, EntityBlock {


    public SuspiciousRemainsGravel(Block p_277629_, Properties p_277373_, SoundEvent p_278060_, SoundEvent p_277352_) {
        super(p_277629_, p_277373_, p_278060_, p_277352_);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.SUSPICIOUS_REMAINS_GRAVEL_BLOCK.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }


}
