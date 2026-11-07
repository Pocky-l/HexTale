package com.pockyl.hextale.thevoid.boss;

import java.util.ArrayList;
import java.util.List;

/**
 * The hexagon grid the Prosector cuts the ground by: pure geometry round any point. A cell is a hexagon of {@link #CELL}
 * blocks radius, and the grid reaches {@link #REACH} blocks.
 */
public final class HexCells {
    public static final int REACH = 15;
    public static final int CELL = 3;
    private static final double SIN60 = 0.8660254;

    private HexCells() {
    }

    /** One hexagon of the grid, by its axial coordinates. */
    public record Cell(int q, int r) {
        public int ring() {
            return Math.max(Math.abs(q), Math.max(Math.abs(r), Math.abs(q + r)));
        }
    }

    /** Distance from the middle in hexagon rings: the same number all along one side. */
    public static double hex(double dx, double dz) {
        double ax = Math.abs(dx);
        double az = Math.abs(dz);
        return Math.max(az, ax * SIN60 + az * 0.5);
    }

    public static double cellX(Cell cell) {
        return 1.5 * CELL * cell.q();
    }

    public static double cellZ(Cell cell) {
        return Math.sqrt(3) * CELL * (cell.r() + cell.q() / 2.0);
    }

    /** The cell holding a point at (dx, dz) from the middle, by rounding its axial coordinates. */
    public static Cell cellOf(double dx, double dz) {
        double q = 2.0 / 3 * dx / CELL;
        double r = (-1.0 / 3 * dx + Math.sqrt(3) / 3 * dz) / CELL;
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
        return new Cell((int) rq, (int) rr);
    }

    /** Every cell of the grid. */
    public static List<Cell> cells() {
        List<Cell> cells = new ArrayList<>();
        for (int q = -6; q <= 6; q++) {
            for (int r = -6; r <= 6; r++) {
                Cell cell = new Cell(q, r);
                if (hex(cellX(cell), cellZ(cell)) <= REACH - 1) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }
}
