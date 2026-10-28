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
import com.pockyl.hextale.port.ResourceHandler;
import com.pockyl.hextale.port.FluidResource;
import com.pockyl.hextale.port.Transaction;

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
                serverLevel.sendParticles(ModParticles.bubble(ModParticles.WATER), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 6, 0.25, 0.1, 0.25, 0.05);
                serverLevel.sendParticles(ModParticles.drop(ModParticles.WATER), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.15, 0.05, 0.15, 0.1);
            }
        }
    }

    private static int fillAbove(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).getBlock() instanceof BrassPipeBlock) {
            return 0;
        }
        ResourceHandler<FluidResource> target = com.pockyl.hextale.port.ResourceHandler.ofFluidBlock(level, above, Direction.DOWN);
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
