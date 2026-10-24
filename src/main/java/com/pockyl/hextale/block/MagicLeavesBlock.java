package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import com.pockyl.hextale.port.FallingParticlesLeavesBlock;
import com.pockyl.hextale.port.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.registry.ModParticles;

/**
 * The magic tree's canopy: glowing leaves drift down from it (more often than cherry petals), and now and then a mote of
 * cold light rises out of it.
 */
public class MagicLeavesBlock extends FallingParticlesLeavesBlock {
    private static final float LEAF_CHANCE = 0.06F;
    private static final float MOTE_CHANCE = 0.015F;

    public MagicLeavesBlock(Properties properties) {
        super(LEAF_CHANCE, AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties);
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        ParticleUtils.spawnParticleBelow(level, pos, random, ModParticles.MAGIC_LEAF.get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextFloat() < MOTE_CHANCE && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ModParticles.glint(ModParticles.GOLD), pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
                    0, 0.01 + random.nextDouble() * 0.015, 0);
        }
    }
}
