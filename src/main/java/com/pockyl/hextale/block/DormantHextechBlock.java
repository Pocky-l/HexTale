package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.registry.ModParticles;

/**
 * Dormant hextech: amethyst that a lightning strike changed long ago, found in struck geodes. It keeps a faint charge -
 * a spark now and then crawls out over a free face, with a quiet crystal hum - and breaks into unstable hextech orbs
 * (the loot table; Silk Touch keeps the block). A rod's strike wakes it like amethyst ({@code awakens_into_hextech}).
 */
public class DormantHextechBlock extends Block {
    public DormantHextechBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) {
            return;
        }
        Direction face = Direction.getRandom(random);
        BlockPos out = pos.relative(face);
        if (level.getBlockState(out).isSolidRender()) {
            return;
        }
        double x = pos.getX() + 0.5 + face.getStepX() * 0.52 + (face.getStepX() == 0 ? random.nextDouble() - 0.5 : 0);
        double y = pos.getY() + 0.5 + face.getStepY() * 0.52 + (face.getStepY() == 0 ? random.nextDouble() - 0.5 : 0);
        double z = pos.getZ() + 0.5 + face.getStepZ() * 0.52 + (face.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0);
        level.addParticle(ModParticles.spark(ModParticles.HEXTECH), x, y, z,
                face.getStepX() * 0.02, face.getStepY() * 0.02, face.getStepZ() * 0.02);
        if (random.nextInt(12) == 0) {
            level.playLocalSound(pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.25F, 0.6F + random.nextFloat() * 0.2F, false);
        }
    }
}
