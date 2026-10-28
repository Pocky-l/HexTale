package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * A pump in water pushes {@value #WATER_PER_PUSH} mB of water up its pipe every {@value #EVERY} ticks; with no brass pipe
 * on top it fills the tank or pipe of another mod standing there.
 */
public class PumpBlockEntity extends BlockEntity {
    public static final int WATER_PER_PUSH = 100;
    public static final int EVERY = 5;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMP.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PumpBlockEntity pump) {
        if (level.getGameTime() % EVERY == 0 && state.getValue(PumpBlock.WATERLOGGED)) {
            int moved = EssenceNetwork.pushWater(level, pos, Direction.UP, WATER_PER_PUSH);
            if (moved == 0) {
                moved = fillAbove(level, pos);
            }
            // Every fourth push is a full stroke: heard and seen.
            if (moved > 0 && level.getGameTime() % (EVERY * 4) == 0 && level instanceof ServerLevel serverLevel) {
                level.playSound(null, pos, ModSounds.PUMP_STROKE.get(), SoundSource.BLOCKS, 0.45F, 0.9F + level.getRandom().nextFloat() * 0.2F);
                stroke(serverLevel, pos);
            }
        }
    }

    /**
     * A full stroke: the water round the pump is drawn in - bubbles converge on its intake from every side - and a puff
     * of spray leaves the outlet on top.
     */
    public static void stroke(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.3;
        double z = pos.getZ() + 0.5;
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4 + level.getRandom().nextDouble() * 0.4;
            double r = 0.75;
            // Count 0: the "spread" is the bubble's speed, aimed at the intake.
            level.sendParticles(ModParticles.bubble(ModParticles.WATER), x + Math.cos(angle) * r, y + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    z + Math.sin(angle) * r, 0, -Math.cos(angle), 0.1, -Math.sin(angle), 0.12);
        }
        level.sendParticles(ModParticles.drop(ModParticles.WATER), x, pos.getY() + 1.0, z, 4, 0.12, 0.05, 0.12, 0.08);
        level.sendParticles(ModParticles.glint(ModParticles.WATER), x, pos.getY() + 0.95, z, 2, 0.1, 0.05, 0.1, 0.01);
    }

    private static int fillAbove(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).getBlock() instanceof BrassPipeBlock) {
            return 0;
        }
        ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, above, Direction.DOWN);
        if (target == null) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = target.insert(FluidResource.of(Fluids.WATER), WATER_PER_PUSH, transaction);
            if (moved > 0) {
                transaction.commit();
            }
            return moved;
        }
    }
}
