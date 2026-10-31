package com.pockyl.hextale.thevoid.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.registry.ModParticles;

/**
 * Riftmetal grown into chitin. A vein gives itself away in the dark: violet sparks jump off its open faces now and then,
 * so one sees it glitter on a rib's flank from across a gap.
 */
public class RiftOreBlock extends DropExperienceBlock {
    private static final int SPARK = 0xB07CFF;

    public RiftOreBlock(Properties properties) {
        super(UniformInt.of(4, 9), properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }
        Direction side = Direction.getRandom(random);
        BlockPos out = pos.relative(side);
        if (!level.getBlockState(out).isAir()) {
            return;
        }
        double x = pos.getX() + 0.5 + side.getStepX() * 0.55 + (side.getStepX() == 0 ? random.nextDouble() - 0.5 : 0);
        double y = pos.getY() + 0.5 + side.getStepY() * 0.55 + (side.getStepY() == 0 ? random.nextDouble() - 0.5 : 0);
        double z = pos.getZ() + 0.5 + side.getStepZ() * 0.55 + (side.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0);
        level.addParticle(ModParticles.glint(SPARK), x, y, z, side.getStepX() * 0.02, side.getStepY() * 0.02 + 0.005, side.getStepZ() * 0.02);
    }
}
