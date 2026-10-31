package com.pockyl.hextale.thevoid.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModFluids;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;

/**
 * Void Ichor: the Void's own blood, pooled in the hollows of its plates - thick, slow, glowing magenta. It creeps
 * rather than flows, one wades through it as through honey and cannot swim in it; it does not drown and does not burn.
 */
public final class VoidIchor {
    private VoidIchor() {
    }

    public static FluidType type() {
        return new FluidType(FluidType.Properties.create()
                .descriptionId("block.hextale.void_ichor")
                .density(3000).viscosity(8000).temperature(320).lightLevel(10)
                .motionScale(0.0022).canSwim(false).canDrown(false).canExtinguish(false).canHydrate(false)
                .canConvertToSource(false).supportsBoating(false).fallDistanceModifier(0.2F)
                .pathType(PathType.DAMAGE_CAUTIOUS).adjacentPathType(null)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA));
    }

    public static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(ModFluids.VOID_ICHOR_TYPE, ModFluids.VOID_ICHOR, ModFluids.FLOWING_VOID_ICHOR)
                .bucket(ModItems.VOID_ICHOR_BUCKET).block(ModBlocks.VOID_ICHOR)
                .slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30).explosionResistance(100);
    }

    /** The ichor standing in the world: now and then a bubble of light rises out of it. */
    public static class Block extends LiquidBlock {
        public Block(FlowingFluid fluid, Properties properties) {
            super(fluid, properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(24) == 0 && level.getBlockState(pos.above()).isAir()) {
                level.addParticle(ModParticles.glint(random.nextBoolean() ? 0xFF7AE6 : 0xE040C8),
                        pos.getX() + random.nextDouble(), pos.getY() + 0.95, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
            }
        }
    }
}
