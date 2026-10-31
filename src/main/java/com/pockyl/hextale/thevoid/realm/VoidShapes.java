package com.pockyl.hextale.thevoid.realm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The shapes the Void is built of, as plain numbers (no Minecraft here): region by region, huge curved plates of shell
 * lying in terraces over the abyss (the ground one walks on: soil and flesh on top, chitin with glowing seams beneath,
 * a curled lip at the edge), knots of flesh on them with fans of colossal chitin ribs arching out, bows of chitin from
 * plate to plate, skin stretched between neighbouring ribs, hollow husks sunk into the plates and digested masses hanging
 * in the gaps. Nothing of our world is in it. Everything bends round the nearest vortex ({@link VoidZones}).
 * <p>
 * A region is {@link #REGION} blocks square; its shapes are made once from the world seed and kept in a small cache,
 * and they may reach {@link #REACH} regions out. {@link #fill} writes the kind of every block of one chunk column into
 * an array: the solid shapes as signed distances (the deepest shape wins a block), the membranes from a raster made
 * once per region.
 */
public final class VoidShapes {
    public static final int REGION = 128;
    /** Regions round a chunk's own whose shapes may reach into it. */
    public static final int REACH = 2;
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 320;

    // Kinds of block the generator writes; the order of the membrane kinds is their precedence in the raster.
    public static final byte AIR = 0;
    public static final byte CHITIN = 1;
    public static final byte RIDGE = 2;
    public static final byte FLESH = 3;
    public static final byte MEMBRANE = 4;
    public static final byte WORN = 5;
    public static final byte VEINED = 6;
    public static final byte RIFT_ORE = 7;
    /** The Void's own soil on top of a plate. */
    public static final byte SOIL = 8;
    public static final byte GNAWED = 12;
    public static final byte CRYSTAL = 13;
    /** Void Ichor, pooled in the hollows of the plates. */
    public static final byte ICHOR = 14;

    private static final int CACHE_LIMIT = 512;

    private final long seed;
    private final Map<Long, Region> regions = new ConcurrentHashMap<>();

    public VoidShapes(long seed) {
        this.seed = seed;
    }

    public long seed() {
        return seed;
    }

    /** One rib: a tapering tube along a curve, as points and radii. */
    record Rib(double[] x, double[] y, double[] z, double[] r, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            int glowFrom) {
        int points() {
            return x.length;
        }

        /** The same tube, glowing from point {@code from} to its tip. */
        Rib glowingFrom(int from) {
            return new Rib(x, y, z, r, minX, minY, minZ, maxX, maxY, maxZ, from);
        }
    }

    /** A pool of ichor in a hollow of a plate: its middle, radius and the height its surface stands at. */
    record Pool(double x, double z, double radius, int level) {
    }

    /** A bladder of skin: a hollow sphere of membrane with a lump of flesh and crystals in its bottom. */
    record Bladder(double x, double y, double z, double radius, boolean crystals, int salt) {
    }

    /** A knot of flesh and plates the ribs of a fan spring from. */
    record Knot(double x, double y, double z, double radius) {
    }

    /** A hollow shell: centre, radii, yaw and tilt (radians), a salt for its openings. */
    record Husk(double x, double y, double z, double a, double b, double c, double yaw, double tilt, int salt, double ore) {
        double reach() {
            return Math.max(a, Math.max(b, c)) + 1;
        }
    }

    /** A mass the Void is digesting, hanging in a gap: its flat bottom at y, a gnawed cone rising out of it. */
    record Shoal(double x, double y, double z, double radius, double height, int salt) {
    }

    /**
     * A plate of shell: an ellipse (radii rx, rz, turned by yaw) round (x, z) at height y, sloping with the ground round
     * it, domed, thick in the middle and thin at the edge where its lip curls up.
     */
    record Plate(double x, double y, double z, double rx, double rz, double yaw, double slopeX, double slopeZ, double dome,
            double thick, double lip, int salt) {
        double reach() {
            return Math.max(rx, rz) * 1.12 + 1;
        }
    }

    /** Everything one region holds, and its membranes rastered into the chunks they cross. */
    static final class Region {
        final List<Rib> ribs = new ArrayList<>();
        final List<Knot> knots = new ArrayList<>();
        final List<Husk> husks = new ArrayList<>();
        final List<Shoal> shoals = new ArrayList<>();
        final List<Plate> plates = new ArrayList<>();
        final List<Bladder> bladders = new ArrayList<>();
        final Map<Plate, List<Pool>> pools = new java.util.HashMap<>();
        /** Packed membrane blocks by chunk ({@link #chunkKey}): {@code lx | lz << 4 | y << 8 | kind << 20}. */
        final Map<Long, int[]> raster = new java.util.HashMap<>();
    }

    public static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private Region region(int rx, int rz) {
        long key = chunkKey(rx, rz);
        Region region = regions.get(key);
        if (region != null) {
            return region;
        }
        if (regions.size() > CACHE_LIMIT) {
            regions.clear();
        }
        return regions.computeIfAbsent(key, k -> build(rx, rz));
    }

    // ------------------------------------------------------------------ building a region

    private Region build(int rx, int rz) {
        Region region = new Region();
        Random random = new Random(hash(seed, rx, rz, 0x5EED));
        double x0 = rx * (double) REGION;
        double z0 = rz * (double) REGION;
        double depth = VoidZones.depth(x0 + REGION / 2.0, z0 + REGION / 2.0);
        VoidZones.Zone zone = VoidZones.zone(x0 + REGION / 2.0, z0 + REGION / 2.0);

        // The ground: plates of shell, many and wide far out, few and small toward the vortex.
        int plates = switch (zone) {
            case THRESHOLD -> 7;
            case REACH -> 6;
            case SHOALS -> 5;
            case DRIFT -> 6;
            case VORTEX -> 4;
        };
        double size = switch (zone) {
            case THRESHOLD -> 1.15;
            case REACH -> 1.0;
            case SHOALS -> 0.85;
            case DRIFT -> 0.9;
            case VORTEX -> 0.65;
        };
        for (int i = 0; i < plates; i++) {
            double px = x0 + random.nextDouble() * REGION;
            double pz = z0 + random.nextDouble() * REGION;
            double r = (26 + random.nextDouble() * 32) * size;
            double gx = (ground(px + 4, pz) - ground(px - 4, pz)) / 8;
            double gz = (ground(px, pz + 4) - ground(px, pz - 4)) / 8;
            region.plates.add(new Plate(px, ground(px, pz) + (random.nextDouble() - 0.5) * 6, pz, r, r * (0.6 + random.nextDouble() * 0.5),
                    random.nextDouble() * Math.PI, gx * 0.85, gz * 0.85, 1.5 + random.nextDouble() * 4, 5 + random.nextDouble() * 7,
                    2 + random.nextDouble() * 2.5, random.nextInt()));
        }

        int thickets = switch (zone) {
            case THRESHOLD, SHOALS -> 1;
            case REACH, DRIFT -> 2;
            case VORTEX -> 3;
        };
        for (Plate plate : region.plates) {
            for (int t = 0; t < thickets; t++) {
                if (random.nextDouble() < 0.6) {
                    thicket(region, plate, random);
                }
            }
            int tendrils = 1 + random.nextInt(3);
            for (int t = 0; t < tendrils; t++) {
                tendril(region, plate, random);
            }
            if (random.nextDouble() < 0.45) {
                // A hollow full of ichor.
                double[] at = onPlate(plate, random, 0.5);
                if (!Double.isNaN(at[1])) {
                    region.pools.computeIfAbsent(plate, key -> new ArrayList<>())
                            .add(new Pool(at[0], at[2], 4 + random.nextDouble() * 7, floor(at[1])));
                }
            }
            if (random.nextDouble() < 0.35) {
                // A bladder half sunk into the plate.
                double[] at = onPlate(plate, random, 0.6);
                if (!Double.isNaN(at[1])) {
                    double r = 3 + random.nextDouble() * 3;
                    region.bladders.add(new Bladder(at[0], at[1] + r * 0.25, at[2], r, random.nextDouble() < 0.6, random.nextInt()));
                }
            }
        }
        // Bladders drifting in the gaps, glowing over the abyss.
        int drifting = 1 + random.nextInt(zone == VoidZones.Zone.VORTEX ? 4 : 3);
        for (int b = 0; b < drifting; b++) {
            double bx = x0 + random.nextDouble() * REGION;
            double bz = z0 + random.nextDouble() * REGION;
            double r = 2.5 + random.nextDouble() * 4;
            region.bladders.add(new Bladder(bx, ground(bx, bz) - 10 - random.nextDouble() * 30, bz, r, random.nextDouble() < 0.4, random.nextInt()));
        }

        for (Knot knot : knots(rx, rz)) {
            region.knots.add(knot);
            fan(region, knot, random, depth, zone);
        }
        // Long bows of chitin from this region's knots to the neighbours' to the east and south: the roads of the Void.
        List<Knot> mine = knots(rx, rz);
        for (int[] next : new int[][]{{1, 0}, {0, 1}, {1, 1}}) {
            if (random.nextDouble() > (next[0] + next[1] == 2 ? 0.35 : 0.8)) {
                continue;
            }
            List<Knot> theirs = knots(rx + next[0], rz + next[1]);
            Knot from = mine.get(random.nextInt(mine.size()));
            Knot to = theirs.get(random.nextInt(theirs.size()));
            region.ribs.add(bridge(from, to, random));
        }

        double huskChance = switch (zone) {
            case THRESHOLD -> 0.15;
            case REACH -> 0.3;
            case SHOALS -> 0.2;
            case DRIFT -> 1.4;
            case VORTEX -> 0.9;
        };
        for (double left = huskChance; left > 0; left--) {
            if (random.nextDouble() < left) {
                double a = 9 + random.nextDouble() * 12 * (1 + depth * 0.4);
                double hx = x0 + random.nextDouble() * REGION;
                double hz = z0 + random.nextDouble() * REGION;
                double b = a * (0.7 + random.nextDouble() * 0.5);
                // Half sunk into the ground, its openings at the level one walks on.
                region.husks.add(new Husk(hx, ground(hx, hz) + b * (0.15 + random.nextDouble() * 0.3), hz, a, b, a * (0.75 + random.nextDouble() * 0.6),
                        random.nextDouble() * Math.PI * 2, (random.nextDouble() - 0.5) * 0.9, random.nextInt(),
                        zone == VoidZones.Zone.VORTEX ? 0.05 : zone == VoidZones.Zone.DRIFT ? 0.035 : 0.012));
            }
        }

        double shoalChance = zone == VoidZones.Zone.SHOALS ? 1.6 : 0.3;
        for (double left = shoalChance; left > 0; left--) {
            if (random.nextDouble() < left) {
                double radius = 8 + random.nextDouble() * 12;
                double sx = x0 + 20 + random.nextDouble() * (REGION - 40);
                double sz = z0 + 20 + random.nextDouble() * (REGION - 40);
                // Hanging below the plates, in the gaps between them.
                region.shoals.add(new Shoal(sx, ground(sx, sz) - 18 - random.nextDouble() * 30, sz, radius,
                        radius * (0.8 + random.nextDouble() * 0.6), random.nextInt()));
            }
        }
        return region;
    }

    /** The knots of a region: where its fans spring from, at very different heights. */
    List<Knot> knots(int rx, int rz) {
        Random random = new Random(hash(seed, rx, rz, 0x4B07));
        double x0 = rx * (double) REGION;
        double z0 = rz * (double) REGION;
        double depth = VoidZones.depth(x0 + REGION / 2.0, z0 + REGION / 2.0);
        int count = 1 + (random.nextDouble() < 0.2 + 0.3 * depth ? 1 : 0);
        List<Knot> knots = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            double kx = x0 + 16 + random.nextDouble() * (REGION - 32);
            double kz = z0 + 16 + random.nextDouble() * (REGION - 32);
            // On the ground: the fans and the bows between plates spring from the plates themselves.
            double ky = ground(kx, kz) + 1;
            knots.add(new Knot(kx, ky, kz, 5 + random.nextDouble() * 5 + depth * 3));
        }
        return knots;
    }

    /**
     * The height of the ground at (x, z): a slow swell with a smaller one over it, so neighbouring plates lie close in
     * height and the plates form terraces one can climb and drop along.
     */
    public double ground(double x, double z) {
        int salt = (int) (seed ^ (seed >>> 32));
        return 118 + 38 * (2 * noise(x / 420, 0, z / 420, salt + 71) - 1) + 12 * (2 * noise(x / 140, 0, z / 140, salt + 73) - 1);
    }

    /** The height of a plate's top at (x, z), NaN off it (the same sums as {@link #plate}, without the pores). */
    private double plateTop(Plate plate, double x, double z) {
        double dx = x + 0.5 - plate.x(), dz = z + 0.5 - plate.z();
        double c = Math.cos(plate.yaw()), sn = Math.sin(plate.yaw());
        double qx = dx * c + dz * sn, qz = -dx * sn + dz * c;
        double e2 = (sq(qx / plate.rx()) + sq(qz / plate.rz())) * (1 + 0.22 * (noise(floor(x) / 9.0, 0, floor(z) / 9.0, plate.salt()) - 0.5));
        if (e2 >= 1) {
            return Double.NaN;
        }
        return plate.y() + plate.slopeX() * dx + plate.slopeZ() * dz + plate.dome() * (1 - e2)
                + (noise(floor(x) / 13.0, 1, floor(z) / 13.0, plate.salt()) - 0.5) * 3;
    }

    /** A point on a plate's top within {@code share} of its radius from the middle: {x, top, z} (top NaN if off it). */
    private double[] onPlate(Plate plate, Random random, double share) {
        double a = random.nextDouble() * Math.PI * 2;
        double d = Math.sqrt(random.nextDouble()) * share;
        double c = Math.cos(plate.yaw()), sn = Math.sin(plate.yaw());
        double qx = Math.cos(a) * d * plate.rx(), qz = Math.sin(a) * d * plate.rz();
        double x = plate.x() + qx * c - qz * sn, z = plate.z() + qx * sn + qz * c;
        return new double[]{x, plateTop(plate, x, z), z};
    }

    /** A thicket of thin curved spines rising out of a plate, leaning one way like grass in a wind. */
    private void thicket(Region region, Plate plate, Random random) {
        double[] centre = onPlate(plate, random, 0.65);
        if (Double.isNaN(centre[1])) {
            return;
        }
        double lean = random.nextDouble() * Math.PI * 2;
        int count = 4 + random.nextInt(8);
        for (int i = 0; i < count; i++) {
            double ox = centre[0] + (random.nextDouble() - 0.5) * 12;
            double oz = centre[2] + (random.nextDouble() - 0.5) * 12;
            double top = plateTop(plate, ox, oz);
            if (Double.isNaN(top)) {
                continue;
            }
            double height = 6 + random.nextDouble() * 18;
            double bend = height * (0.15 + random.nextDouble() * 0.35);
            double lx = Math.cos(lean + (random.nextDouble() - 0.5) * 0.8), lz = Math.sin(lean + (random.nextDouble() - 0.5) * 0.8);
            double[] p0 = {ox, top - 1, oz};
            double[] p1 = {ox, top + height * 0.45, oz};
            double[] p2 = {ox + lx * bend * 0.6, top + height * 0.85, oz + lz * bend * 0.6};
            double[] p3 = {ox + lx * bend * 1.4, top + height * 0.95, oz + lz * bend * 1.4};
            double root = 1.0 + random.nextDouble() * 0.8;
            Rib spine = curve(p0, p1, p2, p3, t -> root * (1 - 0.7 * t), 16);
            region.ribs.add(random.nextDouble() < 0.3 ? spine.glowingFrom(13) : spine);
        }
    }

    /** A tendril hanging from a plate's underside into the abyss, swaying, its tip glowing. */
    private void tendril(Region region, Plate plate, Random random) {
        double[] at = onPlate(plate, random, 0.75);
        if (Double.isNaN(at[1])) {
            return;
        }
        double bottom = at[1] - plate.thick() - 2;
        double length = 12 + random.nextDouble() * 26;
        double sway = length * 0.35;
        double a = random.nextDouble() * Math.PI * 2;
        double[] p0 = {at[0], bottom + 2, at[2]};
        double[] p1 = {at[0] + Math.cos(a) * sway * 0.3, bottom - length * 0.35, at[2] + Math.sin(a) * sway * 0.3};
        double[] p2 = {at[0] - Math.cos(a) * sway * 0.4, bottom - length * 0.7, at[2] - Math.sin(a) * sway * 0.4};
        double[] p3 = {at[0] + Math.cos(a + 1.2) * sway * 0.6, bottom - length, at[2] + Math.sin(a + 1.2) * sway * 0.6};
        double root = 1.4 + random.nextDouble() * 1.2;
        region.ribs.add(curve(p0, p1, p2, p3, t -> root * (1 - 0.65 * t), 24).glowingFrom(20));
    }

    /** A bow from one knot to another: it rises off both and sags or arches between, thick enough to walk. */
    private static Rib bridge(Knot from, Knot to, Random random) {
        double dx = to.x() - from.x(), dy = to.y() - from.y(), dz = to.z() - from.z();
        double length = Math.sqrt(dx * dx + dz * dz);
        double arch = (random.nextDouble() < 0.7 ? 1 : -0.4) * (12 + random.nextDouble() * 0.25 * length);
        double sideX = -dz / Math.max(1, length), sideZ = dx / Math.max(1, length);
        double swing = (random.nextDouble() - 0.5) * length * 0.5;
        double[] p0 = {from.x(), from.y(), from.z()};
        double[] p1 = {from.x() + dx * 0.3 + sideX * swing, from.y() + dy * 0.3 + arch, from.z() + dz * 0.3 + sideZ * swing};
        double[] p2 = {from.x() + dx * 0.7 + sideX * swing * 0.6, from.y() + dy * 0.7 + arch, from.z() + dz * 0.7 + sideZ * swing * 0.6};
        double[] p3 = {to.x(), to.y(), to.z()};
        double radius = 2.6 + random.nextDouble() * 1.8;
        return curve(p0, p1, p2, p3, t -> radius * (1 + 0.5 * Math.pow(Math.abs(t - 0.5) * 2, 3)), 56);
    }

    /** A fan of ribs out of a knot, curling round the vortex, with skin stretched between neighbours. */
    private void fan(Region region, Knot knot, Random random, double depth, VoidZones.Zone zone) {
        int count = 2 + random.nextInt(zone == VoidZones.Zone.VORTEX ? 3 : 2);
        double[] tangent = VoidZones.tangent(knot.x(), knot.z());
        double base = Math.atan2(tangent[1], tangent[0]) + (random.nextDouble() - 0.5) * 1.6;
        if (random.nextBoolean()) {
            base += Math.PI;
        }
        double spread = 0.4 + random.nextDouble() * 0.35;
        double curl = (random.nextBoolean() ? 1 : -1) * (0.15 + random.nextDouble() * 0.25);
        double scale = 1 + depth * 0.5;
        List<Rib> fan = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = base + (i - (count - 1) / 2.0) * spread + (random.nextDouble() - 0.5) * 0.15;
            double length = (40 + random.nextDouble() * 50) * scale;
            double height = (25 + random.nextDouble() * 40) * (1 + depth * 0.6);
            double drop = -45 + random.nextDouble() * 55;
            double root = (4 + random.nextDouble() * 4) * (1 + depth * 0.4);
            // The fan curls one way, round the vortex, each rib a little differently.
            double twist = (curl + (random.nextDouble() - 0.5) * 0.25) * length;
            Rib rib = rib(knot, angle, length, height, drop, root, twist, random);
            fan.add(rib);
            region.ribs.add(rib);
        }
        double skin = switch (zone) {
            case THRESHOLD -> 0.35;
            case REACH -> 0.75;
            case SHOALS -> 0.3;
            case DRIFT -> 0.45;
            case VORTEX -> 0.65;
        };
        double geodes = zone == VoidZones.Zone.REACH ? 0.75 : zone == VoidZones.Zone.THRESHOLD ? 0.15 : 0.35;
        for (int i = 0; i + 1 < fan.size(); i++) {
            if (random.nextDouble() < skin) {
                membrane(region, fan.get(i), fan.get(i + 1), random.nextDouble() < geodes, random);
            }
        }
    }

    private static Rib rib(Knot knot, double angle, double length, double height, double drop, double root, double twist, Random random) {
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        // Sideways, for a rib that curls instead of running straight.
        double sx = -dz;
        double sz = dx;
        double[] p0 = {knot.x() + dx * knot.radius() * 0.5, knot.y(), knot.z() + dz * knot.radius() * 0.5};
        // Curled in plan as well as arched: the bend runs the whole way, the tip hooks further round.
        double[] p1 = {p0[0] + dx * length * 0.25 + sx * twist * 0.5, p0[1] + height * 1.35, p0[2] + dz * length * 0.25 + sz * twist * 0.5};
        double[] p2 = {p0[0] + dx * length * 0.7 + sx * twist * 1.3, p0[1] + height * 1.15 + drop * 0.3, p0[2] + dz * length * 0.7 + sz * twist * 1.3};
        double[] p3 = {p0[0] + dx * length * 0.75 + sx * twist * 2.4, p0[1] + drop, p0[2] + dz * length * 0.75 + sz * twist * 2.4};
        double bulge = 0.8 + random.nextDouble() * 0.5;
        // Thick at the root, a little swelling past it, tapering to a point that is never quite nothing.
        return curve(p0, p1, p2, p3, t -> root * Math.pow(1 - t, 1.15) * (1 + bulge * 0.35 * Math.sin(Math.PI * Math.min(1, t * 2.2))), 44);
    }

    /** A tube along a cubic curve, its radius given along it (0-1). */
    private static Rib curve(double[] p0, double[] p1, double[] p2, double[] p3, java.util.function.DoubleUnaryOperator radius, int n) {
        double[] x = new double[n];
        double[] y = new double[n];
        double[] z = new double[n];
        double[] r = new double[n];
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            double u = 1 - t;
            double b0 = u * u * u;
            double b1 = 3 * u * u * t;
            double b2 = 3 * u * t * t;
            double b3 = t * t * t;
            x[i] = b0 * p0[0] + b1 * p1[0] + b2 * p2[0] + b3 * p3[0];
            y[i] = b0 * p0[1] + b1 * p1[1] + b2 * p2[1] + b3 * p3[1];
            z[i] = b0 * p0[2] + b1 * p1[2] + b2 * p2[2] + b3 * p3[2];
            r[i] = Math.max(0.9, radius.applyAsDouble(t));
            minX = Math.min(minX, x[i] - r[i]);
            minY = Math.min(minY, y[i] - r[i]);
            minZ = Math.min(minZ, z[i] - r[i]);
            maxX = Math.max(maxX, x[i] + r[i]);
            maxY = Math.max(maxY, y[i] + r[i]);
            maxZ = Math.max(maxZ, z[i] + r[i]);
        }
        return new Rib(x, y, z, r, minX, minY, minZ, maxX, maxY, maxZ, n);
    }

    /**
     * Skin between two ribs of a fan: points along both ribs joined straight across, sagging in the middle, one block
     * thick. Every block it crosses is noted once; its kind (plain, worn, veined, a pore) comes from noise at the block,
     * and a geode - a lump of flesh grown with Void crystals - may sit where it sags lowest.
     */
    private void membrane(Region region, Rib a, Rib b, boolean geode, Random random) {
        java.util.Map<Long, Byte> blocks = new java.util.HashMap<>();
        double from = 0.1;
        double to = 0.78 + random.nextDouble() * 0.12;
        double lengthA = 0;
        for (int i = 1; i < a.points(); i++) {
            lengthA += Math.sqrt(sq(a.x()[i] - a.x()[i - 1]) + sq(a.y()[i] - a.y()[i - 1]) + sq(a.z()[i] - a.z()[i - 1]));
        }
        int uSteps = (int) Math.ceil(lengthA * (to - from) / 0.45);
        double sagShare = 0.12 + random.nextDouble() * 0.1;
        double[] pa = new double[3];
        double[] pb = new double[3];
        double lowestY = Double.MAX_VALUE;
        double[] lowest = null;
        for (int iu = 0; iu <= uSteps; iu++) {
            double u = from + (to - from) * iu / uSteps;
            at(a, u, pa);
            at(b, u, pb);
            double width = Math.sqrt(sq(pb[0] - pa[0]) + sq(pb[1] - pa[1]) + sq(pb[2] - pa[2]));
            int vSteps = Math.max(2, (int) Math.ceil(width / 0.45));
            // Taut near the ends of the ribs, slack in the middle.
            double sag = width * sagShare * (0.4 + 0.6 * Math.sin(Math.PI * (u - from) / (to - from)));
            for (int iv = 0; iv <= vSteps; iv++) {
                double v = iv / (double) vSteps;
                double px = pa[0] + (pb[0] - pa[0]) * v;
                double py = pa[1] + (pb[1] - pa[1]) * v - sag * Math.sin(Math.PI * v);
                double pz = pa[2] + (pb[2] - pa[2]) * v;
                int bx = floor(px);
                int by = floor(py);
                int bz = floor(pz);
                if (by < MIN_Y + 1 || by >= MIN_Y + HEIGHT - 1) {
                    continue;
                }
                blocks.putIfAbsent(blockKey(bx, by, bz), MEMBRANE);
                if (Math.abs(v - 0.5) < 0.06 && Math.abs(iu - uSteps * 0.5) < uSteps * 0.08 && py < lowestY) {
                    lowestY = py;
                    lowest = new double[]{px, py, pz};
                }
            }
        }
        for (Map.Entry<Long, Byte> entry : blocks.entrySet()) {
            long key = entry.getKey();
            int bx = unpackX(key);
            int by = unpackY(key);
            int bz = unpackZ(key);
            if (noise(bx / 7.0, by / 7.0, bz / 7.0, 11) > 0.71) {
                continue;
            }
            byte kind = MEMBRANE;
            if (Math.abs(noise(bx / 11.0, by / 11.0, bz / 11.0, 23) - 0.5) < 0.035) {
                kind = VEINED;
            } else if (noise(bx / 21.0, by / 21.0, bz / 21.0, 37) > 0.64) {
                kind = WORN;
            }
            put(region, bx, by, bz, kind);
        }
        if (geode && lowest != null) {
            int cx = floor(lowest[0]);
            int cy = floor(lowest[1]);
            int cz = floor(lowest[2]);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 1; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (dx * dx + dy * dy * 1.6 + dz * dz <= 5.2) {
                            put(region, cx + dx, cy + dy, cz + dz, FLESH);
                        }
                    }
                }
            }
            // Crystals on the lump's crown, pointing up.
            for (int i = 0; i < 6; i++) {
                int dx = random.nextInt(5) - 2;
                int dz = random.nextInt(5) - 2;
                if (dx * dx + dz * dz <= 5) {
                    int top = dx * dx + dz * dz <= 2 ? 2 : 1;
                    put(region, cx + dx, cy + top, cz + dz, CRYSTAL);
                }
            }
        }
    }

    private static void put(Region region, int x, int y, int z, byte kind) {
        if (y < MIN_Y || y >= MIN_Y + HEIGHT) {
            return;
        }
        long chunk = chunkKey(x >> 4, z >> 4);
        int packed = (x & 15) | (z & 15) << 4 | (y - MIN_Y) << 8 | kind << 20;
        region.raster.merge(chunk, new int[]{packed}, (old, add) -> {
            int[] grown = java.util.Arrays.copyOf(old, old.length + 1);
            grown[old.length] = add[0];
            return grown;
        });
    }

    /** The point at share {@code u} (0-1) along a rib. */
    private static void at(Rib rib, double u, double[] out) {
        double f = u * (rib.points() - 1);
        int i = Math.min(rib.points() - 2, (int) f);
        double k = f - i;
        out[0] = rib.x()[i] + (rib.x()[i + 1] - rib.x()[i]) * k;
        out[1] = rib.y()[i] + (rib.y()[i + 1] - rib.y()[i]) * k;
        out[2] = rib.z()[i] + (rib.z()[i + 1] - rib.z()[i]) * k;
    }

    // ------------------------------------------------------------------ filling a chunk

    /**
     * Writes the kind of every block of the chunk column into {@code kinds} (index {@link #index}); {@code best} is
     * scratch of the same size. Ores and plants are left to the caller.
     */
    public void fill(int chunkX, int chunkZ, byte[] kinds, float[] best) {
        java.util.Arrays.fill(kinds, AIR);
        java.util.Arrays.fill(best, Float.MAX_VALUE);
        int x0 = chunkX << 4;
        int z0 = chunkZ << 4;
        int rx = Math.floorDiv(x0, REGION);
        int rz = Math.floorDiv(z0, REGION);
        List<Region> near = new ArrayList<>();
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                near.add(region(rx + dx, rz + dz));
            }
        }
        long key = chunkKey(chunkX, chunkZ);
        // Membranes first: the solid shapes take what they share with them.
        for (Region region : near) {
            int[] membranes = region.raster.get(key);
            if (membranes == null) {
                continue;
            }
            for (int packed : membranes) {
                int i = index(packed & 15, (packed >> 8) & 0xFFF, (packed >> 4) & 15);
                byte kind = (byte) (packed >>> 20);
                if (kinds[i] == AIR || kind > kinds[i] || kind == FLESH) {
                    kinds[i] = kind;
                }
            }
        }
        for (Region region : near) {
            for (Rib rib : region.ribs) {
                if (rib.maxX() >= x0 && rib.minX() < x0 + 16 && rib.maxZ() >= z0 && rib.minZ() < z0 + 16) {
                    rib(rib, x0, z0, kinds, best);
                }
            }
            for (Knot knot : region.knots) {
                knot(knot, x0, z0, kinds, best);
            }
            for (Husk husk : region.husks) {
                husk(husk, x0, z0, kinds, best);
            }
            for (Shoal shoal : region.shoals) {
                shoal(shoal, x0, z0, kinds, best);
            }
            for (Plate plate : region.plates) {
                plate(plate, region.pools.getOrDefault(plate, List.of()), x0, z0, kinds, best);
            }
            for (Bladder bladder : region.bladders) {
                bladder(bladder, x0, z0, kinds, best);
            }
        }
    }

    public static int index(int lx, int y, int lz) {
        return (y * 16 + lz) * 16 + lx;
    }

    private static void rib(Rib rib, int x0, int z0, byte[] kinds, float[] best) {
        for (int i = 0; i + 1 < rib.points(); i++) {
            double ax = rib.x()[i], ay = rib.y()[i], az = rib.z()[i], ar = rib.r()[i];
            double bx = rib.x()[i + 1], by = rib.y()[i + 1], bz = rib.z()[i + 1], br = rib.r()[i + 1];
            double reach = Math.max(ar, br) + 0.5;
            int minX = Math.max(x0, floor(Math.min(ax, bx) - reach));
            int maxX = Math.min(x0 + 15, floor(Math.max(ax, bx) + reach));
            int minZ = Math.max(z0, floor(Math.min(az, bz) - reach));
            int maxZ = Math.min(z0 + 15, floor(Math.max(az, bz) + reach));
            int minY = Math.max(MIN_Y, floor(Math.min(ay, by) - reach));
            int maxY = Math.min(MIN_Y + HEIGHT - 1, floor(Math.max(ay, by) + reach));
            if (minX > maxX || minZ > maxZ || minY > maxY) {
                continue;
            }
            double ex = bx - ax, ey = by - ay, ez = bz - az;
            double len2 = Math.max(1e-6, ex * ex + ey * ey + ez * ez);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        double px = x + 0.5 - ax, py = y + 0.5 - ay, pz = z + 0.5 - az;
                        double t = Math.max(0, Math.min(1, (px * ex + py * ey + pz * ez) / len2));
                        double qx = px - ex * t, qy = py - ey * t, qz = pz - ez * t;
                        double d = Math.sqrt(qx * qx + qy * qy + qz * qz);
                        double r = ar + (br - ar) * t;
                        double s = d - r;
                        int idx = index(x - x0, y - MIN_Y, z - z0);
                        if (s >= 0 || s >= best[idx]) {
                            continue;
                        }
                        best[idx] = (float) s;
                        byte kind;
                        // Joints: every third segment of a thick rib a ring of light runs round it, like the seams of
                        // a beetle's leg.
                        boolean joint = r > 2.2 && i % 3 == 0 && t < 0.3 && s > -1.3;
                        if (i >= rib.glowFrom() || joint || s > -1.05 && qy > 0.8 * d) {
                            kind = RIDGE;
                        } else if (s < -2.6 && r > 3.6) {
                            kind = FLESH;
                        } else {
                            kind = CHITIN;
                        }
                        kinds[idx] = kind;
                    }
                }
            }
        }
    }

    private void knot(Knot knot, int x0, int z0, byte[] kinds, float[] best) {
        double reach = knot.radius() * 1.3 + 1;
        int minX = Math.max(x0, floor(knot.x() - reach));
        int maxX = Math.min(x0 + 15, floor(knot.x() + reach));
        int minZ = Math.max(z0, floor(knot.z() - reach));
        int maxZ = Math.min(z0 + 15, floor(knot.z() + reach));
        if (minX > maxX || minZ > maxZ) {
            return;
        }
        int minY = Math.max(MIN_Y, floor(knot.y() - reach));
        int maxY = Math.min(MIN_Y + HEIGHT - 1, floor(knot.y() + reach));
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    double dx = x + 0.5 - knot.x(), dy = (y + 0.5 - knot.y()) * 1.35, dz = z + 0.5 - knot.z();
                    double wobble = (noise(x / 5.0, y / 5.0, z / 5.0, 51) - 0.5) * 0.55;
                    double s = Math.sqrt(dx * dx + dy * dy + dz * dz) - knot.radius() * (1 + wobble);
                    int idx = index(x - x0, y - MIN_Y, z - z0);
                    if (s >= 0 || s >= best[idx]) {
                        continue;
                    }
                    best[idx] = (float) s;
                    kinds[idx] = s < -1.6 || noise(x / 4.0, y / 4.0, z / 4.0, 67) > 0.5 ? FLESH : CHITIN;
                }
            }
        }
    }

    private void husk(Husk husk, int x0, int z0, byte[] kinds, float[] best) {
        double reach = husk.reach();
        int minX = Math.max(x0, floor(husk.x() - reach));
        int maxX = Math.min(x0 + 15, floor(husk.x() + reach));
        int minZ = Math.max(z0, floor(husk.z() - reach));
        int maxZ = Math.min(z0 + 15, floor(husk.z() + reach));
        if (minX > maxX || minZ > maxZ) {
            return;
        }
        int minY = Math.max(MIN_Y, floor(husk.y() - reach));
        int maxY = Math.min(MIN_Y + HEIGHT - 1, floor(husk.y() + reach));
        double cy = Math.cos(husk.yaw()), sy = Math.sin(husk.yaw());
        double ct = Math.cos(husk.tilt()), st = Math.sin(husk.tilt());
        double minR = Math.min(husk.a(), Math.min(husk.b(), husk.c()));
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    double px = x + 0.5 - husk.x(), py = y + 0.5 - husk.y(), pz = z + 0.5 - husk.z();
                    // Into the husk's own frame: turn by its yaw, then tip it over by its tilt.
                    double qx = px * cy + pz * sy;
                    double qz = -px * sy + pz * cy;
                    double qy = py * ct - qx * st;
                    qx = qx * ct + py * st;
                    double e = Math.sqrt(sq(qx / husk.a()) + sq(qy / husk.b()) + sq(qz / husk.c()));
                    double shell = Math.abs((e - 1) * minR) - 0.95;
                    int idx = index(x - x0, y - MIN_Y, z - z0);
                    if (shell < 0) {
                        if (noise(x / 6.5, y / 6.5, z / 6.5, husk.salt()) > 0.6) {
                            continue;
                        }
                        if (shell < best[idx]) {
                            best[idx] = (float) shell;
                            kinds[idx] = hash(x, y, z, husk.salt()) < husk.ore() ? RIFT_ORE : CHITIN;
                        }
                    } else if (e < 1 && qy < -0.5 * husk.b() && noise(x / 4.0, y / 4.0, z / 4.0, husk.salt() + 1) > 0.35) {
                        // The dark flesh pooled at the bottom inside.
                        if (-0.5 < best[idx]) {
                            best[idx] = -0.5F;
                            kinds[idx] = FLESH;
                        }
                    }
                }
            }
        }
    }

    private void plate(Plate plate, List<Pool> pools, int x0, int z0, byte[] kinds, float[] best) {
        double reach = plate.reach();
        int minX = Math.max(x0, floor(plate.x() - reach));
        int maxX = Math.min(x0 + 15, floor(plate.x() + reach));
        int minZ = Math.max(z0, floor(plate.z() - reach));
        int maxZ = Math.min(z0 + 15, floor(plate.z() + reach));
        if (minX > maxX || minZ > maxZ) {
            return;
        }
        double c = Math.cos(plate.yaw()), sn = Math.sin(plate.yaw());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double dx = x + 0.5 - plate.x(), dz = z + 0.5 - plate.z();
                double qx = dx * c + dz * sn, qz = -dx * sn + dz * c;
                // A ragged edge, as if bitten.
                double e2 = (sq(qx / plate.rx()) + sq(qz / plate.rz())) * (1 + 0.22 * (noise(x / 9.0, 0, z / 9.0, plate.salt()) - 0.5));
                if (e2 >= 1) {
                    continue;
                }
                double e = Math.sqrt(e2);
                // Pores: holes through the plate down to the abyss, ringed with a glowing rim.
                Pool pool = null;
                double inPool = 2;
                for (Pool each : pools) {
                    double d = Math.sqrt(sq(x + 0.5 - each.x()) + sq(z + 0.5 - each.z())) / each.radius();
                    if (d < inPool) {
                        inPool = d;
                        pool = each;
                    }
                }
                double pore = e < 0.78 && inPool > 1.3 ? noise(x / 7.0, 3, z / 7.0, plate.salt() + 5) : 0;
                if (pore > 0.82) {
                    continue;
                }
                double base = plate.y() + plate.slopeX() * dx + plate.slopeZ() * dz + plate.dome() * (1 - e2)
                        + (noise(x / 13.0, 1, z / 13.0, plate.salt()) - 0.5) * 3;
                double lip = e > 0.84 ? (e - 0.84) / 0.16 * plate.lip() : 0;
                int top = floor(base + lip);
                int bottom = floor(base - plate.thick() * Math.sqrt(1 - e2) - 1.5);
                if (pool != null && inPool < 1.2) {
                    // The hollow: ichor down to its depth; round it a rim of flesh up to the surface, so nothing spills.
                    int depth = inPool < 1 ? 1 + (int) (2.6 * (1 - inPool * inPool)) : 0;
                    int floorY = pool.level() - depth;
                    for (int y = Math.max(MIN_Y, Math.min(bottom, floorY)); y <= Math.min(MIN_Y + HEIGHT - 1, Math.max(top, pool.level())); y++) {
                        int idx = index(x - x0, y - MIN_Y, z - z0);
                        if (inPool < 1 && y > floorY && y <= pool.level()) {
                            best[idx] = -50;
                            kinds[idx] = ICHOR;
                        } else if (y > pool.level() && inPool < 1) {
                            continue;
                        } else if (y >= bottom && y <= top) {
                            if (-1 < best[idx]) {
                                best[idx] = -1;
                                kinds[idx] = y == top && inPool >= 1 ? FLESH : CHITIN;
                            }
                        } else if (y == floorY || y > top && y <= pool.level()) {
                            best[idx] = -2;
                            kinds[idx] = FLESH;
                        }
                    }
                    continue;
                }
                for (int y = Math.max(MIN_Y, bottom); y <= Math.min(MIN_Y + HEIGHT - 1, top); y++) {
                    float s = (float) -(Math.min(top - y, y - bottom) + 0.5);
                    int idx = index(x - x0, y - MIN_Y, z - z0);
                    if (s >= best[idx]) {
                        continue;
                    }
                    best[idx] = s;
                    byte kind;
                    if (y == top) {
                        // Veins of light run across the plates like rivers; flesh and soil lie in broad patches.
                        double vein = Math.abs(noise(x / 17.0, 4, z / 17.0, plate.salt() + 11) - 0.5)
                                + Math.abs(noise(x / 5.0, 5, z / 5.0, plate.salt() + 13) - 0.5) * 0.08;
                        if (pore > 0.76) {
                            kind = RIDGE;
                        } else if (lip > 1.2) {
                            kind = CHITIN;
                        } else if (vein < 0.035) {
                            kind = VEINED;
                        } else {
                            kind = noise(x / 11.0, 2, z / 11.0, plate.salt()) > 0.56 ? FLESH : SOIL;
                        }
                    } else if (top - y <= 2 && lip <= 1.2) {
                        kind = FLESH;
                    } else if (y == bottom && hash(x, y, z, plate.salt()) < 0.14) {
                        // Seams of light on the underside, seen from below over the abyss.
                        kind = RIDGE;
                    } else {
                        // Strata: bands of dark flesh through the chitin, showing on the plate's edges.
                        kind = noise(x / 23.0, y / 2.5, z / 23.0, plate.salt() + 17) > 0.62 ? FLESH : CHITIN;
                    }
                    kinds[idx] = kind;
                }
            }
        }
    }

    private void bladder(Bladder bladder, int x0, int z0, byte[] kinds, float[] best) {
        double r = bladder.radius();
        int minX = Math.max(x0, floor(bladder.x() - r - 1));
        int maxX = Math.min(x0 + 15, floor(bladder.x() + r + 1));
        int minZ = Math.max(z0, floor(bladder.z() - r - 1));
        int maxZ = Math.min(z0 + 15, floor(bladder.z() + r + 1));
        if (minX > maxX || minZ > maxZ) {
            return;
        }
        int minY = Math.max(MIN_Y, floor(bladder.y() - r - 1));
        int maxY = Math.min(MIN_Y + HEIGHT - 1, floor(bladder.y() + r + 1));
        double coreY = bladder.y() - r * 0.45;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    double dx = x + 0.5 - bladder.x(), dy = y + 0.5 - bladder.y(), dz = z + 0.5 - bladder.z();
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    int idx = index(x - x0, y - MIN_Y, z - z0);
                    double core = Math.sqrt(dx * dx + sq(y + 0.5 - coreY) * 1.6 + dz * dz) - r * 0.4;
                    if (core < 0) {
                        if (core < best[idx]) {
                            best[idx] = (float) core;
                            kinds[idx] = FLESH;
                        }
                        continue;
                    }
                    double shell = Math.abs(d - r) - 0.55;
                    if (shell < 0 && shell < best[idx] && kinds[idx] == AIR) {
                        best[idx] = (float) shell;
                        kinds[idx] = Math.abs(noise(x / 3.0, y / 3.0, z / 3.0, bladder.salt()) - 0.5) < 0.08 ? VEINED : MEMBRANE;
                    } else if (bladder.crystals() && core < 1.2 && dy > -r * 0.45 && kinds[idx] == AIR
                            && hash(x, y, z, bladder.salt()) < 0.5) {
                        kinds[idx] = CRYSTAL;
                    }
                }
            }
        }
    }

    private void shoal(Shoal shoal, int x0, int z0, byte[] kinds, float[] best) {
        double reach = shoal.radius() * 1.35 + 2;
        int minX = Math.max(x0, floor(shoal.x() - reach));
        int maxX = Math.min(x0 + 15, floor(shoal.x() + reach));
        int minZ = Math.max(z0, floor(shoal.z() - reach));
        int maxZ = Math.min(z0 + 15, floor(shoal.z() + reach));
        if (minX > maxX || minZ > maxZ) {
            return;
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double dx = x + 0.5 - shoal.x(), dz = z + 0.5 - shoal.z();
                double rr = Math.sqrt(dx * dx + dz * dz);
                double edge = shoal.radius() * (1 + 0.35 * (noise(x / 9.0, 0, z / 9.0, shoal.salt()) - 0.5));
                if (rr > edge * 1.05) {
                    continue;
                }
                double bottom = shoal.y() + (noise(x / 7.0, 3, z / 7.0, shoal.salt() + 3) - 0.5) * 3;
                int top = floor(bottom + shoal.height() * 1.1);
                for (int y = Math.max(MIN_Y, floor(bottom)); y <= Math.min(MIN_Y + HEIGHT - 1, top); y++) {
                    double h = (y + 0.5 - bottom) / shoal.height();
                    if (h < 0 || h > 1) {
                        continue;
                    }
                    // The cone of stone narrows as it rises, ragged where the Void has bitten it.
                    double allowed = edge * Math.pow(1 - h, 0.7) * (1 + 0.25 * (noise(x / 5.0, y / 5.0, z / 5.0, shoal.salt() + 5) - 0.5));
                    double s = rr - allowed;
                    int idx = index(x - x0, y - MIN_Y, z - z0);
                    if (s >= 0 || s >= best[idx]) {
                        continue;
                    }
                    best[idx] = (float) s;
                    // Gnawed stone through and through, flesh grown into it where the Void has eaten deepest.
                    double bite = noise(x / 4.0, y / 4.0, z / 4.0, shoal.salt() + 9);
                    kinds[idx] = s > -1.4 && bite > 0.58 ? FLESH : GNAWED;
                }
            }
        }
    }

    // ------------------------------------------------------------------ noise and hashes

    /** Value noise 0-1 with smooth steps, on the lattice of whole numbers. */
    public static double noise(double x, double y, double z, int salt) {
        int ix = floor(x), iy = floor(y), iz = floor(z);
        double fx = x - ix, fy = y - iy, fz = z - iz;
        double ux = fx * fx * (3 - 2 * fx), uy = fy * fy * (3 - 2 * fy), uz = fz * fz * (3 - 2 * fz);
        double c000 = hash(ix, iy, iz, salt), c100 = hash(ix + 1, iy, iz, salt);
        double c010 = hash(ix, iy + 1, iz, salt), c110 = hash(ix + 1, iy + 1, iz, salt);
        double c001 = hash(ix, iy, iz + 1, salt), c101 = hash(ix + 1, iy, iz + 1, salt);
        double c011 = hash(ix, iy + 1, iz + 1, salt), c111 = hash(ix + 1, iy + 1, iz + 1, salt);
        double x00 = c000 + (c100 - c000) * ux, x10 = c010 + (c110 - c010) * ux;
        double x01 = c001 + (c101 - c001) * ux, x11 = c011 + (c111 - c011) * ux;
        double y0 = x00 + (x10 - x00) * uy, y1 = x01 + (x11 - x01) * uy;
        return y0 + (y1 - y0) * uz;
    }

    /** A number 0-1 that only depends on the block and the salt. */
    public static double hash(int x, int y, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ salt * 0xD6E8FEB86659FD93L;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        h ^= h >>> 32;
        return (h >>> 11) * 0x1.0p-53;
    }

    static long hash(long seed, int x, int z, int salt) {
        long h = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 32);
    }

    private static long blockKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static int unpackX(long key) {
        return (int) (key >> 38) << 6 >> 6;
    }

    private static int unpackZ(long key) {
        return (int) ((key >> 12) & 0x3FFFFFF) << 6 >> 6;
    }

    private static int unpackY(long key) {
        return (int) (key & 0xFFF);
    }

    static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    private static double sq(double v) {
        return v * v;
    }

    /** A small seeded random (splitmix), the same everywhere for the same seed. */
    static final class Random {
        private long state;

        Random(long seed) {
            state = seed;
        }

        long nextLong() {
            long z = (state += 0x9E3779B97F4A7C15L);
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }

        double nextDouble() {
            return (nextLong() >>> 11) * 0x1.0p-53;
        }

        int nextInt(int bound) {
            return (int) ((nextLong() >>> 33) % bound);
        }

        int nextInt() {
            return (int) nextLong();
        }

        boolean nextBoolean() {
            return nextLong() < 0;
        }
    }
}
