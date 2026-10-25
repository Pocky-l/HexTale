package com.pockyl.hextale.thevoid;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.registry.ModParticles;

/**
 * Soil and rock the Void has touched: dark, cracked with lilac, and every now and then a dark mote is drawn down into
 * it. Nothing of the world grows on it any more; only the Void's own growth takes root.
 */
public class VoidSoilBlock extends Block {
    private static final int MOTE = 0x24162E;

    public VoidSoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(14) != 0 || !level.getBlockState(pos.above()).isAir()) {
            return;
        }
        double x = pos.getX() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();
        // A mote drawn into the ground: spawned where it falls in, its speed is the offset to its start.
        level.addParticle(ModParticles.voidMote(MOTE), x, pos.getY() + 1.02, z,
                (random.nextDouble() - 0.5) * 1.5, 1 + random.nextDouble(), (random.nextDouble() - 0.5) * 1.5);
    }
}
