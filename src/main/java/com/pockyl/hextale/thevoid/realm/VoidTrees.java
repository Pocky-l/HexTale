package com.pockyl.hextale.thevoid.realm;

import java.util.ArrayList;
import java.util.List;

/**
 * The Sinew Willow, the one tree of the Void: a trunk braided of two or three sinews twisting round each other, roots
 * gripping the plate, boughs that arch out and droop, a canopy of fronds over them with curtains of fronds hanging
 * from it and glowing ichor pods among the curtains. Pure Java like {@link VoidShapes}: the generator rasters it into
 * a chunk's kinds, a sprout into the level - the same tree both ways.
 */
public final class VoidTrees {
    private VoidTrees() {
    }

    /** A tapering tube along a curve, of wood. */
    record Tube(double[] x, double[] y, double[] z, double[] r) {
        int points() {
            return x.length;
        }
    }

    /** A cushion of fronds: a flattened, ragged ellipsoid. */
    record Crown(double x, double y, double z, double rx, double ry, double rz, int salt) {
    }

    public record Tree(List<Tube> tubes, List<Crown> crowns, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int salt) {
        boolean touches(int x0, int z0) {
            return maxX >= x0 && minX < x0 + 16 && maxZ >= z0 && minZ < z0 + 16;
        }
    }

    /** Where the tree reads and writes kinds ({@link VoidShapes} numbering); {@code best} is the deepest wood so far. */
    interface Grid {
        /** The kind at a block, or -1 out of reach. */
        int get(int x, int y, int z);

        void set(int x, int y, int z, byte kind);

        /** How deep inside other matter the block is (signed distance, the shape-raster's {@code best}). */
        float best(int x, int y, int z);

        void best(int x, int y, int z, float value);
    }

    /**
     * Plans a tree standing on the ground at (x, y, z) (y = the top of the ground block). {@code large}: a grown old
     * willow of 15-26 blocks; otherwise a young one of 8-12.
     */
    static Tree plan(VoidShapes.Random random, double x, double y, double z, boolean large) {
        List<Tube> tubes = new ArrayList<>();
        List<Crown> crowns = new ArrayList<>();
        double height = large ? 16 + random.nextDouble() * 10 : 9 + random.nextDouble() * 4;
        double lean = random.nextDouble() * Math.PI * 2;
        double leanBy = height * (0.08 + random.nextDouble() * 0.2);
        double lx = Math.cos(lean) * leanBy, lz = Math.sin(lean) * leanBy;
        // The trunk's axis: straight off the ground, leaning more the higher it goes.
        double[][] axis = {
                {x, y - 1.5, z},
                {x, y + height * 0.4, z},
                {x + lx * 0.45, y + height * 0.75, z + lz * 0.45},
                {x + lx, y + height, z + lz}};
        // The sinews: braided round the axis, apart at the foot, closing in toward the top.
        int strands = large ? 3 : 2;
        double twist = (random.nextBoolean() ? 1 : -1) * (Math.PI * (1.1 + random.nextDouble() * 0.9));
        double phase = random.nextDouble() * Math.PI * 2;
        double spread = large ? 1.25 + random.nextDouble() * 0.4 : 0.85 + random.nextDouble() * 0.25;
        double thick = large ? 1.35 + random.nextDouble() * 0.35 : 1.0 + random.nextDouble() * 0.2;
        int n = (int) (height * 1.5) + 8;
        for (int s = 0; s < strands; s++) {
            double[] px = new double[n], py = new double[n], pz = new double[n], pr = new double[n];
            double start = phase + s * Math.PI * 2 / strands;
            for (int i = 0; i < n; i++) {
                double t = i / (double) (n - 1);
                double[] a = bezier(axis, t);
                double angle = start + twist * t;
                double off = spread * (1 - 0.6 * t) + (t < 0.12 ? (0.12 - t) * 6 : 0);
                px[i] = a[0] + Math.cos(angle) * off;
                py[i] = a[1];
                pz[i] = a[2] + Math.sin(angle) * off;
                pr[i] = thick * (1 - 0.4 * t);
            }
            tubes.add(new Tube(px, py, pz, pr));
        }
        // Roots: out from the foot, arching over the plate and gripping down into it.
        int roots = (large ? 4 : 3) + random.nextInt(2);
        for (int k = 0; k < roots; k++) {
            double a = phase + k * Math.PI * 2 / roots + (random.nextDouble() - 0.5) * 0.7;
            double reach = (large ? 4.5 : 3) + random.nextDouble() * 2.5;
            double ox = Math.cos(a), oz = Math.sin(a);
            double rise = large ? 2.2 : 1.4;
            double[][] p = {
                    {x + ox * 0.8, y + rise + 0.8, z + oz * 0.8},
                    {x + ox * reach * 0.4, y + rise, z + oz * reach * 0.4},
                    {x + ox * reach * 0.8, y + 0.6, z + oz * reach * 0.8},
                    {x + ox * reach, y - 2, z + oz * reach}};
            double r0 = large ? 1.15 : 0.9;
            tubes.add(tube(p, t -> r0 * (1 - 0.45 * t), 14));
        }
        // Boughs: from the upper trunk, out and up, then drooping under their own fronds.
        double[] top = axis[3];
        int boughs = large ? 5 + random.nextInt(3) : 3 + random.nextInt(2);
        double turn = random.nextDouble() * Math.PI * 2;
        for (int b = 0; b < boughs; b++) {
            double t0 = 0.62 + random.nextDouble() * 0.33;
            double[] from = bezier(axis, t0);
            double a = turn + b * Math.PI * 2 / boughs + (random.nextDouble() - 0.5) * 0.6;
            double length = height * (0.42 + random.nextDouble() * 0.24) + (large ? 2 : 0);
            double ox = Math.cos(a), oz = Math.sin(a);
            double up = length * (0.45 + random.nextDouble() * 0.3);
            double[] end = {from[0] + ox * length, from[1] + up * 0.35 - random.nextDouble() * 1.5, from[2] + oz * length};
            double[][] p = {
                    from,
                    {from[0] + ox * length * 0.3, from[1] + up, from[2] + oz * length * 0.3},
                    {from[0] + ox * length * 0.7, from[1] + up * 1.05, from[2] + oz * length * 0.7},
                    end};
            double r0 = large ? 1.0 : 0.8;
            tubes.add(tube(p, t -> r0 * (1 - 0.3 * t), 18));
            // The bough's fronds sit over its arch, spilling past its drooping end.
            double[] arch = bezier(p, 0.62);
            double spreadR = (large ? 2.4 : 1.7) + random.nextDouble() * 0.9;
            crowns.add(new Crown(arch[0] * 0.3 + end[0] * 0.7, Math.max(arch[1], end[1]) + 0.4, arch[2] * 0.3 + end[2] * 0.7,
                    spreadR, (large ? 1.3 : 1.1) + random.nextDouble() * 0.4, spreadR * (0.8 + random.nextDouble() * 0.3), random.nextInt()));
            // A smaller cushion over the top of the arch knits the crowns into one weeping dome.
            double[] crest = bezier(p, 0.4);
            double crestR = spreadR * 0.75;
            crowns.add(new Crown(crest[0], crest[1] + 0.6, crest[2], crestR, 1.0 + random.nextDouble() * 0.3, crestR, random.nextInt()));
        }
        // A cap of fronds over the crown of the trunk.
        double cap = large ? 2.8 + random.nextDouble() * 1.4 : 2.0 + random.nextDouble() * 0.8;
        crowns.add(new Crown(top[0], top[1] + 1.0, top[2], cap, large ? 1.6 : 1.3, cap * (0.85 + random.nextDouble() * 0.25), random.nextInt()));

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Tube tube : tubes) {
            for (int i = 0; i < tube.points(); i++) {
                minX = Math.min(minX, tube.x()[i] - tube.r()[i]);
                minY = Math.min(minY, tube.y()[i] - tube.r()[i]);
                minZ = Math.min(minZ, tube.z()[i] - tube.r()[i]);
                maxX = Math.max(maxX, tube.x()[i] + tube.r()[i]);
                maxY = Math.max(maxY, tube.y()[i] + tube.r()[i]);
                maxZ = Math.max(maxZ, tube.z()[i] + tube.r()[i]);
            }
        }
        for (Crown crown : crowns) {
            minX = Math.min(minX, crown.x() - crown.rx() * 1.3);
            maxX = Math.max(maxX, crown.x() + crown.rx() * 1.3);
            minZ = Math.min(minZ, crown.z() - crown.rz() * 1.3);
            maxZ = Math.max(maxZ, crown.z() + crown.rz() * 1.3);
            minY = Math.min(minY, crown.y() - crown.ry() * 1.3 - HANG);
            maxY = Math.max(maxY, crown.y() + crown.ry() * 1.3);
        }
        return new Tree(tubes, crowns, VoidShapes.floor(minX) - 1, VoidShapes.floor(minY) - 1, VoidShapes.floor(minZ) - 1,
                VoidShapes.floor(maxX) + 1, VoidShapes.floor(maxY) + 1, VoidShapes.floor(maxZ) + 1, random.nextInt());
    }

    /** How far the curtains of fronds hang below a crown at most. */
    static final int HANG = 9;

    private static Tube tube(double[][] p, java.util.function.DoubleUnaryOperator radius, int n) {
        double[] x = new double[n], y = new double[n], z = new double[n], r = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            double[] a = bezier(p, t);
            x[i] = a[0];
            y[i] = a[1];
            z[i] = a[2];
            r[i] = radius.applyAsDouble(t);
        }
        return new Tube(x, y, z, r);
    }

    private static double[] bezier(double[][] p, double t) {
        double u = 1 - t;
        double b0 = u * u * u, b1 = 3 * u * u * t, b2 = 3 * u * t * t, b3 = t * t * t;
        return new double[]{
                b0 * p[0][0] + b1 * p[1][0] + b2 * p[2][0] + b3 * p[3][0],
                b0 * p[0][1] + b1 * p[1][1] + b2 * p[2][1] + b3 * p[3][1],
                b0 * p[0][2] + b1 * p[1][2] + b2 * p[2][2] + b3 * p[3][2]};
    }

    /**
     * Writes the tree into the blocks {@code grid} can reach within x0..x1, z0..z1: wood where it lies deeper than what
     * is there (roots sink into the plate), fronds only into air, then the hanging curtains and pods.
     */
    static void raster(Tree tree, int x0, int z0, int x1, int z1, Grid grid) {
        int minX = Math.max(x0, tree.minX()), maxX = Math.min(x1, tree.maxX());
        int minZ = Math.max(z0, tree.minZ()), maxZ = Math.min(z1, tree.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return;
        }
        for (Tube tube : tree.tubes()) {
            wood(tube, minX, maxX, minZ, maxZ, grid);
        }
        for (Crown crown : tree.crowns()) {
            fronds(crown, minX, maxX, minZ, maxZ, grid);
        }
        // Curtains: strands of fronds down from the crowns' undersides, a pod or a tendril at the end of some.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = tree.maxY(); y >= tree.minY(); y--) {
                    if (grid.get(x, y, z) != VoidShapes.FRONDS || grid.get(x, y - 1, z) != VoidShapes.AIR) {
                        continue;
                    }
                    double roll = VoidShapes.hash(x, y, z, tree.salt());
                    if (roll < 0.55) {
                        // Long near the crowns' rims, where the willow weeps, short under their middles.
                        double h = VoidShapes.hash(x, y, z, tree.salt() + 1);
                        int length = 2 + (int) (h * h * (HANG - 1));
                        int k = 1;
                        while (k <= length && grid.get(x, y - k, z) == VoidShapes.AIR) {
                            grid.set(x, y - k, z, VoidShapes.FRONDS);
                            k++;
                        }
                        double tip = VoidShapes.hash(x, y, z, tree.salt() + 2);
                        if (k > 1 && grid.get(x, y - k, z) == VoidShapes.AIR && tip < 0.28) {
                            grid.set(x, y - k, z, VoidShapes.POD);
                        }
                        // On below the strand, so it does not start another from its own end.
                        y -= k;
                    } else if (roll < 0.62) {
                        grid.set(x, y - 1, z, VoidShapes.POD);
                    }
                }
            }
        }
    }

    private static void wood(Tube tube, int minX, int maxX, int minZ, int maxZ, Grid grid) {
        for (int i = 0; i + 1 < tube.points(); i++) {
            double ax = tube.x()[i], ay = tube.y()[i], az = tube.z()[i], ar = tube.r()[i];
            double bx = tube.x()[i + 1], by = tube.y()[i + 1], bz = tube.z()[i + 1], br = tube.r()[i + 1];
            double reach = Math.max(ar, br) + 0.5;
            int sx = Math.max(minX, VoidShapes.floor(Math.min(ax, bx) - reach));
            int ex = Math.min(maxX, VoidShapes.floor(Math.max(ax, bx) + reach));
            int sz = Math.max(minZ, VoidShapes.floor(Math.min(az, bz) - reach));
            int ez = Math.min(maxZ, VoidShapes.floor(Math.max(az, bz) + reach));
            int sy = VoidShapes.floor(Math.min(ay, by) - reach);
            int ey = VoidShapes.floor(Math.max(ay, by) + reach);
            double dx = bx - ax, dy = by - ay, dz = bz - az;
            double len2 = Math.max(1e-6, dx * dx + dy * dy + dz * dz);
            // The grain runs along the segment.
            byte kind = Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= Math.abs(dz) ? VoidShapes.LOG_Y
                    : Math.abs(dx) >= Math.abs(dz) ? VoidShapes.LOG_X : VoidShapes.LOG_Z;
            for (int x = sx; x <= ex; x++) {
                for (int z = sz; z <= ez; z++) {
                    for (int y = sy; y <= ey; y++) {
                        int here = grid.get(x, y, z);
                        if (here < 0) {
                            continue;
                        }
                        double px = x + 0.5 - ax, py = y + 0.5 - ay, pz = z + 0.5 - az;
                        double t = Math.max(0, Math.min(1, (px * dx + py * dy + pz * dz) / len2));
                        double qx = px - dx * t, qy = py - dy * t, qz = pz - dz * t;
                        double s = Math.sqrt(qx * qx + qy * qy + qz * qz) - Math.max(0.62, ar + (br - ar) * t);
                        // Wood takes air and fronds; ground only where it lies deeper in the wood than in the ground
                        // (so roots show at the plate's skin); where sinews cross, the first one keeps its grain.
                        boolean free = here == VoidShapes.AIR || here == VoidShapes.FRONDS || here == VoidShapes.POD;
                        boolean wooden = here == VoidShapes.LOG_X || here == VoidShapes.LOG_Y || here == VoidShapes.LOG_Z;
                        if (s >= 0 || wooden || !free && s >= grid.best(x, y, z)) {
                            continue;
                        }
                        grid.best(x, y, z, (float) s);
                        grid.set(x, y, z, kind);
                    }
                }
            }
        }
    }

    private static void fronds(Crown crown, int minX, int maxX, int minZ, int maxZ, Grid grid) {
        int sx = Math.max(minX, VoidShapes.floor(crown.x() - crown.rx() * 1.3));
        int ex = Math.min(maxX, VoidShapes.floor(crown.x() + crown.rx() * 1.3));
        int sz = Math.max(minZ, VoidShapes.floor(crown.z() - crown.rz() * 1.3));
        int ez = Math.min(maxZ, VoidShapes.floor(crown.z() + crown.rz() * 1.3));
        int sy = VoidShapes.floor(crown.y() - crown.ry() * 1.3);
        int ey = VoidShapes.floor(crown.y() + crown.ry() * 1.3);
        for (int x = sx; x <= ex; x++) {
            for (int z = sz; z <= ez; z++) {
                for (int y = sy; y <= ey; y++) {
                    if (grid.get(x, y, z) != VoidShapes.AIR) {
                        continue;
                    }
                    double dx = (x + 0.5 - crown.x()) / crown.rx();
                    double dy = (y + 0.5 - crown.y()) / crown.ry();
                    double dz = (z + 0.5 - crown.z()) / crown.rz();
                    // Domed: the top rounds off, the underside is flatter so the curtains hang from a level edge.
                    double e = dx * dx + (dy > 0 ? dy * dy : dy * dy * 1.8) + dz * dz;
                    double ragged = (VoidShapes.noise(x / 3.0, y / 3.0, z / 3.0, crown.salt()) - 0.5) * 0.7;
                    if (e < 1 + ragged && VoidShapes.hash(x, y, z, crown.salt()) > 0.14) {
                        grid.set(x, y, z, VoidShapes.FRONDS);
                    }
                }
            }
        }
    }

    static boolean isTree(int kind) {
        return kind == VoidShapes.LOG_X || kind == VoidShapes.LOG_Y || kind == VoidShapes.LOG_Z || kind == VoidShapes.FRONDS
                || kind == VoidShapes.POD;
    }
}
