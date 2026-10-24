package com.pockyl.hextale.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Leaves that let particles fall from their underside now and then, as on Minecraft 26.x.
 *
 * <p>port-of: net.minecraft.world.level.block.FallingParticlesLeavesBlock
 */
public abstract class FallingParticlesLeavesBlock extends LeavesBlock {
    protected final float leafParticleChance;

    public FallingParticlesLeavesBlock(float leafParticleChance, AmbientLeavesBlockSoundPlayer ambientSound, Properties properties) {
        super(properties);
        this.leafParticleChance = leafParticleChance;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        BlockPos below = pos.below();
        if (random.nextFloat() < leafParticleChance && !isFaceFull(level.getBlockState(below).getCollisionShape(level, below), Direction.UP)) {
            spawnFallingLeavesParticle(level, pos, random);
        }
    }

    protected abstract void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random);
}
