package com.pockyl.hextale.thevoid.realm;

/**
 * Where in the Void a place lies: every {@link #CELL} blocks square holds one vortex, and the realm is laid out in rings
 * round the nearest one - the Threshold far out, the Ribcage Reach, the Husk Drift, the Vortex itself - with patches of
 * Swallowed Shoals wherever low noise says so. The layout does not depend on the world seed, so the zones (biomes) and
 * the shapes always agree.
 */
public final class VoidZones {
    /** One vortex per cell of this many blocks. */
    public static final int CELL = 1536;
    /** Blocks from a vortex at which its pull is no longer felt. */
    public static final double REACH = 1000;
    private static final int SALT = 0x701D;

    public enum Zone {
        THRESHOLD("threshold"),
        REACH("ribcage_reach"),
        SHOALS("swallowed_shoals"),
        DRIFT("husk_drift"),
        VORTEX("vortex");

        private final String id;

        Zone(String id) {
            this.id = id;
        }

        /** The biome's path under the mod's namespace. */
        public String id() {
            return id;
        }
    }

    private VoidZones() {
    }

    /** The centre of the vortex nearest to (x, z). */
    public static double[] vortex(double x, double z) {
        int cx = Math.floorDiv(VoidShapes.floor(x), CELL);
        int cz = Math.floorDiv(VoidShapes.floor(z), CELL);
        double best = Double.MAX_VALUE;
        double[] found = new double[2];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                double vx = (cx + dx) * (double) CELL + CELL * 0.2 + VoidShapes.hash(cx + dx, 0, cz + dz, SALT) * CELL * 0.6;
                double vz = (cz + dz) * (double) CELL + CELL * 0.2 + VoidShapes.hash(cx + dx, 1, cz + dz, SALT) * CELL * 0.6;
                double d = (vx - x) * (vx - x) + (vz - z) * (vz - z);
                if (d < best) {
                    best = d;
                    found[0] = vx;
                    found[1] = vz;
                }
            }
        }
        return found;
    }

    /** 0 far out .. 1 at the heart of a vortex. */
    public static double depth(double x, double z) {
        double[] v = vortex(x, z);
        double d = Math.sqrt((v[0] - x) * (v[0] - x) + (v[1] - z) * (v[1] - z));
        return Math.max(0, Math.min(1, 1 - d / REACH));
    }

    /** The way round the nearest vortex at (x, z), a unit vector (x, z). */
    public static double[] tangent(double x, double z) {
        double[] v = vortex(x, z);
        double dx = x - v[0];
        double dz = z - v[1];
        double length = Math.max(1e-6, Math.sqrt(dx * dx + dz * dz));
        return new double[]{-dz / length, dx / length};
    }

    public static Zone zone(double x, double z) {
        double depth = depth(x, z);
        if (depth > 0.72) {
            return Zone.VORTEX;
        }
        if (depth < 0.7 && VoidShapes.noise(x / 420.0, 0, z / 420.0, SALT + 1) > 0.66) {
            return Zone.SHOALS;
        }
        if (depth > 0.42) {
            return Zone.DRIFT;
        }
        if (depth > 0.16) {
            return Zone.REACH;
        }
        return Zone.THRESHOLD;
    }
}
