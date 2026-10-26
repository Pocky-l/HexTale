package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * A hextech cluster: a druse of amethyst the strike woke all the way, growing out of the walls of a struck geode's crater
 * and hollow. It glows cold blue, throws sparks off its tips and now and then crackles; it breaks into an unstable orb.
 */
public class HextechClusterBlock extends AmethystClusterBlock {
    public HextechClusterBlock(Properties properties) {
        super(7.0F, 3.0F, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) {
            return;
        }
        Direction facing = state.getValue(FACING);
        double reach = 0.25 + random.nextDouble() * 0.2;
        level.addParticle(ModParticles.spark(ModParticles.HEXTECH),
                pos.getX() + 0.5 + facing.getStepX() * reach + (random.nextDouble() - 0.5) * 0.4,
                pos.getY() + 0.5 + facing.getStepY() * reach + (random.nextDouble() - 0.5) * 0.4,
                pos.getZ() + 0.5 + facing.getStepZ() * reach + (random.nextDouble() - 0.5) * 0.4,
                facing.getStepX() * 0.02, facing.getStepY() * 0.02, facing.getStepZ() * 0.02);
        if (random.nextInt(20) == 0) {
            level.playLocalSound(pos, ModSounds.LAMP_FLICKER.get(), SoundSource.BLOCKS, 0.3F, 0.8F + random.nextFloat() * 0.3F, false);
        }
    }
}
