package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import com.pockyl.hextale.Config;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import org.jspecify.annotations.Nullable;

/**
 * The Void's rock turned to crystal: on every open side it grows buds into full Void Crystal Clusters, like budding
 * amethyst but slower ({@link Config#BUDDING_VOID_CRYSTAL_GROWTH}). It is found only in the Void, in clutches past the
 * Threshold; it has no drop and no piston moves it, so a player farms it where it lies.
 */
public class BuddingVoidCrystalBlock extends Block {
    private static final Direction[] SIDES = Direction.values();
    private static final int MOTE = 0xB080EC;

    public BuddingVoidCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextDouble() < Config.BUDDING_VOID_CRYSTAL_GROWTH.get()) {
            grow(level, pos, SIDES[random.nextInt(SIDES.length)]);
        }
    }

    /** Grows the crystal on one side of the block at {@code pos} by a stage; false when that side has nowhere to go. */
    public static boolean grow(ServerLevel level, BlockPos pos, Direction side) {
        BlockPos at = pos.relative(side);
        BlockState there = level.getBlockState(at);
        Block next = next(there, side);
        if (next == null) {
            return false;
        }
        level.setBlockAndUpdate(at, next.defaultBlockState().setValue(AmethystClusterBlock.FACING, side)
                .setValue(AmethystClusterBlock.WATERLOGGED, there.getFluidState().getType() == Fluids.WATER));
        level.sendParticles(ModParticles.voidMote(MOTE), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.01);
        level.playSound(null, at, ModSounds.CRYSTAL_RESONATE.get(), SoundSource.BLOCKS, 0.4F, 0.6F + level.getRandom().nextFloat() * 0.2F);
        return true;
    }

    /** What grows out of {@code there} on this side, or null: open space starts a bud, a bud of this side swells. */
    private static @Nullable Block next(BlockState there, Direction side) {
        if (BuddingAmethystBlock.canClusterGrowAtState(there)) {
            return ModBlocks.SMALL_VOID_CRYSTAL_BUD.get();
        }
        if (!there.hasProperty(AmethystClusterBlock.FACING) || there.getValue(AmethystClusterBlock.FACING) != side) {
            return null;
        }
        if (there.is(ModBlocks.SMALL_VOID_CRYSTAL_BUD.get())) {
            return ModBlocks.MEDIUM_VOID_CRYSTAL_BUD.get();
        }
        if (there.is(ModBlocks.MEDIUM_VOID_CRYSTAL_BUD.get())) {
            return ModBlocks.LARGE_VOID_CRYSTAL_BUD.get();
        }
        return there.is(ModBlocks.LARGE_VOID_CRYSTAL_BUD.get()) ? ModBlocks.VOID_CRYSTAL_CLUSTER.get() : null;
    }

    /** Now and then a mote leaves an open side, so a clutch shows from a distance and in the dark. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        Direction side = SIDES[random.nextInt(SIDES.length)];
        if (level.getBlockState(pos.relative(side)).canOcclude()) {
            return;
        }
        double spread = random.nextDouble() - 0.5;
        double across = random.nextDouble() - 0.5;
        double x = side.getStepX() != 0 ? side.getStepX() * 0.6 : spread;
        double y = side.getStepY() != 0 ? side.getStepY() * 0.6 : side.getStepX() != 0 ? spread : across;
        double z = side.getStepZ() != 0 ? side.getStepZ() * 0.6 : across;
        level.addParticle(ModParticles.voidMote(MOTE), pos.getX() + 0.5 + x, pos.getY() + 0.5 + y, pos.getZ() + 0.5 + z,
                side.getStepX() * 0.01, side.getStepY() * 0.01, side.getStepZ() * 0.01);
    }
}
