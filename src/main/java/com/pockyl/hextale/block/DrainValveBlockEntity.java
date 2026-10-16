package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.registry.ModBlockEntities;

/**
 * An open Drain Valve takes what the pipes bring - up to {@value #DROPS_PER_TICK} mB of essence and
 * {@value #WATER_PER_TICK} mB of water a tick - and pours it out of its spout. The spilled essence is gone for now;
 * later it will taint the world around.
 */
public class DrainValveBlockEntity extends BlockEntity implements EssenceNetwork.Receiver, EssenceNetwork.WaterReceiver {
    public static final int DROPS_PER_TICK = 4;
    public static final int WATER_PER_TICK = 100;
    /** Block event: a splash of this colour pours out (param = RGB). */
    private static final int EVENT_POUR = 1;
    private static final int WATER_COLOR = 0x3F76E4;

    private long tick = Long.MIN_VALUE;
    private int dropsThisTick;
    private int waterThisTick;
    private long lastSplash = Long.MIN_VALUE / 2;

    public DrainValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAIN_VALVE.get(), pos, state);
    }

    /** An open valve drains the tanks at its sides (piped or right beside it). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DrainValveBlockEntity valve) {
        if (!state.getValue(DrainValveBlock.OPEN) || level.getGameTime() % EssenceTankBlockEntity.FLOW_EVERY != 0) {
            return;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            EssenceMix drawn = EssenceNetwork.draw(level, pos, side, EssenceTankBlockEntity.FLOW, valve);
            if (!drawn.isEmpty()) {
                valve.splash(drawn.color());
            }
        }
    }

    private boolean open() {
        return getBlockState().getValue(DrainValveBlock.OPEN);
    }

    /** Starts a new tick's allowance when the game time moved on. */
    private void roll() {
        long now = level == null ? 0 : level.getGameTime();
        if (now != tick) {
            tick = now;
            dropsThisTick = 0;
            waterThisTick = 0;
        }
    }

    @Override
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
        if (!open() || !side.getAxis().isHorizontal()) {
            return 0;
        }
        roll();
        int taken = Math.min(offer.total(), DROPS_PER_TICK - dropsThisTick);
        if (taken > 0 && !simulate) {
            dropsThisTick += taken;
            splash(offer.color());
        }
        return Math.max(0, taken);
    }

    @Override
    public int receiveWater(Direction side, int amount, boolean simulate) {
        if (!open() || !side.getAxis().isHorizontal()) {
            return 0;
        }
        roll();
        int taken = Math.min(amount, WATER_PER_TICK - waterThisTick);
        if (taken > 0 && !simulate) {
            waterThisTick += taken;
            splash(WATER_COLOR);
        }
        return Math.max(0, taken);
    }

    /** Tells clients now and then to show the liquid pouring out of the spout. */
    private void splash(int color) {
        if (level != null && level.getGameTime() - lastSplash >= 4) {
            lastSplash = level.getGameTime();
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_POUR, color);
            if (level.getRandom().nextInt(4) == 0) {
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.25F, 1.4F);
            }
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id != EVENT_POUR || level == null || !level.isClientSide()) {
            return super.triggerEvent(id, param);
        }
        RandomSource random = level.getRandom();
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() - 0.05;
        double z = worldPosition.getZ() + 0.5;
        for (int i = 0; i < 4; i++) {
            if (param == WATER_COLOR) {
                level.addParticle(ParticleTypes.FALLING_WATER, x + (random.nextDouble() - 0.5) * 0.1, y, z + (random.nextDouble() - 0.5) * 0.1, 0, 0, 0);
            } else {
                level.addParticle(new DustParticleOptions(param, 0.8F), x + (random.nextDouble() - 0.5) * 0.12, y - random.nextDouble() * 0.4,
                        z + (random.nextDouble() - 0.5) * 0.12, 0, -0.2, 0);
            }
        }
        return true;
    }
}
