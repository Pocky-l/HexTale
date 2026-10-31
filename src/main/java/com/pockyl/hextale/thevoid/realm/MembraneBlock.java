package com.pockyl.hextale.thevoid.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The skin the Void stretches between its ribs: translucent and taut, it gives a little: a fall onto it hurts half as much;
 * neighbouring skin of any kind shows no face between them, so a sheet reads as one sheet.
 */
public class MembraneBlock extends HalfTransparentBlock {
    public MembraneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbour, Direction direction) {
        return neighbour.getBlock() instanceof MembraneBlock && !WornMembraneBlock.torn(neighbour) || super.skipRendering(state, neighbour, direction);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 0.5F, level.damageSources().fall());
    }
}
