package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.world.MagicTreeFeature;

/**
 * The Magic Tree Sprout: an ordinary sapling that soaked up magic, plant and sap in a crucible. It glimmers, grows like
 * any sapling (light, time or bone meal) and when it shoots up into a magic tree, light runs up the new trunk and the
 * crown sheds its first glowing leaves.
 */
public class MagicSaplingBlock extends SaplingBlock {
    public MagicSaplingBlock(Properties properties) {
        super(MagicTreeFeature.GROWER, properties);
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        boolean ripe = state.getValue(STAGE) == 1;
        super.advanceTree(level, pos, state, random);
        if (ripe && level.getBlockState(pos).is(ModBlocks.MAGIC_LOG.get())) {
            bloom(level, pos);
        }
    }

    /** A tree just grew here: light runs up its trunk, the crown sheds leaves, a low swell of sound. */
    private static void bloom(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        int height = 0;
        while (height < 12 && level.getBlockState(pos.above(height + 1)).is(ModBlocks.MAGIC_LOG.get())) {
            height++;
        }
        for (int y = 0; y <= height; y++) {
            level.sendParticles(ParticleTypes.WAX_ON, x, pos.getY() + y + 0.5, z, 6, 0.35, 0.4, 0.35, 0.02);
        }
        level.sendParticles(ModParticles.MAGIC_LEAF.get(), x, pos.getY() + height + 1.5, z, 40, 2.5, 1.2, 2.5, 0);
        level.sendParticles(ParticleTypes.WAX_ON, x, pos.getY() + height + 1.5, z, 30, 2.0, 1.0, 2.0, 0.05);
        level.playSound(null, pos, ModSounds.BLOCK_AWAKEN.get(), SoundSource.BLOCKS, 1.0F, 0.7F);
        level.playSound(null, pos, SoundEvents.CHERRY_LEAVES_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.4 + random.nextDouble() * 0.4,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
    }
}
