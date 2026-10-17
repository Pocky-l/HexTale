package com.pockyl.hextale.research;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The research parchment: a hexagon cut into hexagonal sections. A section has six neighbours, one per direction, and
 * the parchment's six corners lie in those six directions - every study gives each direction its own essence, and a
 * step of ink pays with the essence of its direction. Coordinates are axial {@code (q, r)} of pointy-top hexagons;
 * positions are in units of the parchment's circumradius (its corners lie at distance ~1).
 */
public final class HexGrid {
    /** Sections from the centre to a corner. */
    public static final int RADIUS = 8;
    public static final double SQ3 = Math.sqrt(3);
    /** Circumradius of one section. */
    public static final double HS = 1.0 / (RADIUS * SQ3);
    /** Distance between the centres of two neighbouring sections. */
    public static final double CELL = HS * SQ3;
    /**
     * The six directions in axial steps, counter-clockwise on screen from the east: east, north-east, north-west,
     * west, south-west, south-east.
     */
    public static final int[][] DIRECTIONS = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};
    private static final int SPAN = RADIUS + 1;

    private static final int[] Q;
    private static final int[] R;
    private static final int[][] INDEX = new int[SPAN * 2 + 1][SPAN * 2 + 1];
    private static final int[][] NEIGHBOURS;
    public static final int COUNT;
    public static final int CENTRE;

    static {
        List<int[]> cells = new ArrayList<>();
        for (int[] row : INDEX) {
            Arrays.fill(row, -1);
        }
        for (int q = -RADIUS; q <= RADIUS; q++) {
            for (int r = -RADIUS; r <= RADIUS; r++) {
                if (length(new int[]{q, r}) <= RADIUS) {
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

    /** The corner of the parchment in a direction, in parchment units. */
    public static double[] corner(int direction) {
        int[] d = DIRECTIONS[direction];
        return new double[]{x(d[0] * RADIUS, d[1] * RADIUS), y(d[1] * RADIUS)};
    }

    /** Whether a point lies inside the parchment's hexagon, at least {@code margin} away from its edges. */
    public static boolean inside(double px, double py, double margin) {
        for (int i = 0; i < 6; i++) {
            double[] a = corner(i);
            double[] b = corner((i + 1) % 6);
            double ex = b[0] - a[0];
            double ey = b[1] - a[1];
            // Corners run counter-clockwise on screen (y down): the inside is to the right of every edge.
            if ((ey * (px - a[0]) - ex * (py - a[1])) / Math.hypot(ex, ey) < margin) {
                return false;
            }
        }
        return true;
    }

    /** The direction (0-5) of a step between two neighbouring sections, or -1 when they do not touch. */
    public static int direction(int from, int to) {
        int dq = Q[to] - Q[from];
        int dr = R[to] - R[from];
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (DIRECTIONS[i][0] == dq && DIRECTIONS[i][1] == dr) {
                return i;
            }
        }
        return -1;
    }

    /** The section one step from {@code cell} in a direction, or -1 off the parchment. */
    public static int step(int cell, int direction) {
        return index(Q[cell] + DIRECTIONS[direction][0], R[cell] + DIRECTIONS[direction][1]);
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

    /** Hex distance in sections (walls ignored). */
    public static int steps(int a, int b) {
        return length(new int[]{Q[b] - Q[a], R[b] - R[a]});
    }

    /** Whether two sections touch. */
    public static boolean adjacent(int a, int b) {
        return steps(a, b) == 1;
    }
}
