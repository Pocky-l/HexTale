package com.pockyl.hextale.world;

import net.minecraft.util.Mth;

/**
 * Where bloom caves are: a pure function of the world seed. The world is cut into square cells of {@value #CELL} blocks;
 * a cell is a bloom region with a chance of {@value #CHANCE}, and its region is a round patch around a seeded centre.
 * Strength runs from 1 at the centre to 0 at the edge, so a region's caves are thick with blooms in the middle and
 * thin out towards its rim - and everywhere else there is nothing at all.
 */
public final class BloomRegions {
    public static final int CELL = 96;
    public static final float CHANCE = 0.35F;
    public static final int MIN_RADIUS = 30;
    public static final int MAX_RADIUS = 50;
    /** Room kept between a centre and its cell's edge. */
    private static final int MARGIN = 16;

    private BloomRegions() {
    }

    /** 0 outside every bloom region, up to 1 at a region's heart. */
    public static float strength(long seed, int x, int z) {
        int cellX = Math.floorDiv(x, CELL);
        int cellZ = Math.floorDiv(z, CELL);
        float best = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                best = Math.max(best, strengthIn(seed, cellX + dx, cellZ + dz, x, z));
            }
        }
        return best;
    }

    private static float strengthIn(long seed, int cellX, int cellZ, int x, int z) {
        long hash = mix(seed ^ (cellX * 0x9E3779B97F4A7C15L) ^ (cellZ * 0xC2B2AE3D27D4EB4FL));
        if (unit(hash) >= CHANCE) {
            return 0;
        }
        hash = mix(hash);
        int centreX = cellX * CELL + MARGIN + (int) (unit(hash) * (CELL - 2 * MARGIN));
        hash = mix(hash);
        int centreZ = cellZ * CELL + MARGIN + (int) (unit(hash) * (CELL - 2 * MARGIN));
        hash = mix(hash);
        float radius = MIN_RADIUS + unit(hash) * (MAX_RADIUS - MIN_RADIUS);
        float distance = Mth.sqrt((float) ((x - centreX) * (x - centreX) + (z - centreZ) * (z - centreZ)));
        return Math.max(0, 1 - distance / radius);
    }

    /** SplitMix64's finaliser. */
    private static long mix(long value) {
        value += 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static float unit(long hash) {
        return (hash >>> 40) / (float) (1L << 24);
    }
}
