package com.pockyl.hextale.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.ChemBoilerBlock;
import com.pockyl.hextale.block.ChemBoilerBlockEntity;
import com.pockyl.hextale.registry.ModParticles;

import java.util.List;

/**
 * The steam a Chem Boiler sends to the machines it drives, drawn on the clients from the synced list of driven machines:
 * along a slack arc from the valve to each machine runs a train of puffs - one every two ticks a little further on, so a
 * pulse of pressure travels down the line - and the machine at its end coughs out a puff of green-grey exhaust now and
 * then. Particles only, nothing kept between ticks.
 */
public final class PressureFx {
    /** Ticks a pulse takes from the boiler to a machine. */
    private static final int TRAVEL = 24;
    private static final int PUFF_EVERY = 2;
    private static final int EXHAUST_EVERY = 16;

    private PressureFx() {
    }

    public static void tick(Level level, ChemBoilerBlockEntity boiler, long now) {
        List<BlockPos> driven = boiler.driven();
        if (driven.isEmpty() || now % PUFF_EVERY != 0) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 from = ChemBoilerBlock.valve(boiler.getBlockPos()).add(0, 0.1, 0);
        for (int i = 0; i < driven.size(); i++) {
            BlockPos machine = driven.get(i);
            Vec3 to = Vec3.atCenterOf(machine).add(0, 0.2, 0);
            double distance = from.distanceTo(to);
            double lift = 0.4 + distance * 0.18;
            // Each line has its own phase so the pulses do not march in step.
            float t = ((now + i * 7L) % TRAVEL) / (float) TRAVEL;
            Vec3 at = arc(from, to, lift, t);
            Vec3 ahead = arc(from, to, lift, Math.min(1, t + 0.05F)).subtract(at);
            level.addParticle(ModParticles.vapour(ChemBoilerBlock.STEAM), at.x, at.y, at.z, ahead.x * 0.4, ahead.y * 0.4 + 0.004,
                    ahead.z * 0.4);
            if ((now + i * 5L) % EXHAUST_EVERY == 0) {
                Vec3 top = Vec3.atLowerCornerOf(machine).add(0.5, 1.0, 0.5);
                level.addParticle(ModParticles.vapour(ChemBoilerBlock.EXHAUST), top.x + (random.nextDouble() - 0.5) * 0.3, top.y,
                        top.z + (random.nextDouble() - 0.5) * 0.3, 0, 0.035, 0);
            }
        }
    }

    /** A point on a quadratic arc from {@code from} to {@code to} that bows up by {@code lift} in the middle. */
    private static Vec3 arc(Vec3 from, Vec3 to, double lift, float t) {
        Vec3 line = from.lerp(to, t);
        return line.add(0, lift * 4 * t * (1 - t), 0);
    }
}
