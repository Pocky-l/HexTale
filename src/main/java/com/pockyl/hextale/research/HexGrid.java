package com.pockyl.hextale.research;

import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The research parchment: a nonagon with one corner per element (fire on top, then clockwise lightning, air, light,
 * life, water, shadow, earth, metal), cut into hexagonal sections. Coordinates are axial {@code (q, r)} of pointy-top
 * hexagons; positions are in units of the nonagon's circumradius.
 */
public final class HexGrid {
    /** Corner order around the rim, starting at the top. */
    public static final Essence[] RING = {Essence.FIRE, Essence.SPARK, Essence.AIR, Essence.LIGHT,
            Essence.LIFE, Essence.WATER, Essence.SHADOW, Essence.EARTH, Essence.METAL};
    /** Circumradius of one section. */
    public static final double HS = 0.072;
    public static final double SQ3 = Math.sqrt(3);
    /** Distance between the centres of two neighbouring sections. */
    public static final double CELL = HS * SQ3;
    private static final int SPAN = 16;
    private static final int[][] DIRECTIONS = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};
    private static final double[][] UNIT = new double[Essence.values().length][];
    private static final double[][] CORNERS = new double[RING.length][];

    private static final int[] Q;
    private static final int[] R;
    private static final int[][] INDEX = new int[SPAN * 2 + 1][SPAN * 2 + 1];
    private static final int[][] NEIGHBOURS;
    public static final int COUNT;
    public static final int CENTRE;

    static {
        for (int i = 0; i < RING.length; i++) {
            // StrictMath is fdlibm, like the prototype's JavaScript engine: identical corners, identical boards.
            double angle = -Math.PI / 2 + i * 2 * Math.PI / 9;
            double[] u = {StrictMath.cos(angle), StrictMath.sin(angle)};
            UNIT[RING[i].ordinal()] = u;
            CORNERS[i] = u;
        }
        List<int[]> cells = new ArrayList<>();
        for (int[] row : INDEX) {
            Arrays.fill(row, -1);
        }
        for (int q = -SPAN; q <= SPAN; q++) {
            for (int r = -SPAN; r <= SPAN; r++) {
                if (inside(x(q, r), y(r), HS * 0.55)) {
                    INDEX[q + SPAN][r + SPAN] = cells.size();
                    cells.add(new int[]{q, r});
                }
            }
        }
        COUNT = cells.size();
        Q = new int[COUNT];
        R = new int[COUNT];
        for (int i = 0; i < COUNT; i++) {
            Q[i] = cells.get(i)[0];
            R[i] = cells.get(i)[1];
        }
        NEIGHBOURS = new int[COUNT][];
        for (int i = 0; i < COUNT; i++) {
            List<Integer> list = new ArrayList<>();
            for (int[] d : DIRECTIONS) {
                int n = index(Q[i] + d[0], R[i] + d[1]);
                if (n >= 0) {
                    list.add(n);
                }
            }
            NEIGHBOURS[i] = list.stream().mapToInt(Integer::intValue).toArray();
        }
        CENTRE = index(0, 0);
    }

    private HexGrid() {
    }

    private static double x(double q, double r) {
        return HS * SQ3 * (q + r / 2);
    }

    private static double y(double r) {
        return HS * 1.5 * r;
    }

    /** Whether a point lies inside the nonagon, at least {@code margin} away from its edges. */
    public static boolean inside(double px, double py, double margin) {
        for (int i = 0; i < 9; i++) {
            double[] a = CORNERS[i];
            double[] b = CORNERS[(i + 1) % 9];
            double ex = b[0] - a[0];
            double ey = b[1] - a[1];
            if ((ex * (py - a[1]) - ey * (px - a[0])) / Math.hypot(ex, ey) < margin) {
                return false;
            }
        }
        return true;
    }

    /** Unit vector from the centre towards the element's corner. */
    public static double[] unit(Essence element) {
        return UNIT[element.ordinal()];
    }

    public static int q(int cell) {
        return Q[cell];
    }

    public static int r(int cell) {
        return R[cell];
    }

    public static double cx(int cell) {
        return x(Q[cell], R[cell]);
    }

    public static double cy(int cell) {
        return y(R[cell]);
    }

    /** The section at axial coordinates, or -1 outside the parchment. */
    public static int index(int q, int r) {
        if (q < -SPAN || q > SPAN || r < -SPAN || r > SPAN) {
            return -1;
        }
        return INDEX[q + SPAN][r + SPAN];
    }

    public static int[] neighbours(int cell) {
        return NEIGHBOURS[cell];
    }

    public static double distance(int a, int b) {
        return Math.hypot(cx(a) - cx(b), cy(a) - cy(b));
    }

    public static double radius(int cell) {
        return Math.hypot(cx(cell), cy(cell));
    }

    /** Axial coordinates {@code [q, r]} of the hexagon containing a point (may be outside the parchment). */
    public static int[] axialAt(double px, double py) {
        double q = (SQ3 / 3 * px - py / 3) / HS;
        double r = 2.0 / 3 * py / HS;
        return round(q, r);
    }

    /** The section under a point, or -1. */
    public static int cellAt(double px, double py) {
        int[] a = axialAt(px, py);
        return index(a[0], a[1]);
    }

    private static int[] round(double q, double r) {
        double s = -q - r;
        long rq = Math.round(q);
        long rr = Math.round(r);
        long rs = Math.round(s);
        double dq = Math.abs(rq - q);
        double dr = Math.abs(rr - r);
        double ds = Math.abs(rs - s);
        if (dq > dr && dq > ds) {
            rq = -rr - rs;
        } else if (dr > ds) {
            rr = -rq - rs;
        }
        return new int[]{(int) rq, (int) rr};
    }

    public static int length(int[] offset) {
        return (Math.abs(offset[0]) + Math.abs(offset[1]) + Math.abs(offset[0] + offset[1])) / 2;
    }

    /**
     * The straight line of sections from {@code from} along an axial offset, section by section, as axial coordinates
     * (the line may leave the parchment). A fixed nudge breaks ties the same way everywhere.
     */
    public static int[][] line(int from, int[] offset) {
        int n = length(offset);
        int[][] out = new int[n][];
        for (int i = 1; i <= n; i++) {
            double q = Q[from] + (double) offset[0] * i / n + 1e-6;
            double r = R[from] + (double) offset[1] * i / n + 2e-6;
            out[i - 1] = round(q, r);
        }
        return out;
    }

    /** The sections of {@link #line} up to the edge of the parchment. */
    public static int[] ray(int from, int[] offset) {
        int[][] line = line(from, offset);
        int[] out = new int[line.length];
        int n = 0;
        for (int[] axial : line) {
            int cell = index(axial[0], axial[1]);
            if (cell < 0) {
                break;
            }
            out[n++] = cell;
        }
        return Arrays.copyOf(out, n);
    }

    /** Hex distance in sections (walls ignored). */
    public static int steps(int a, int b) {
        return length(new int[]{Q[b] - Q[a], R[b] - R[a]});
    }

    /** Whether two sections touch. */
    public static boolean adjacent(int a, int b) {
        return steps(a, b) == 1;
    }
}
