package com.pockyl.hextale.block;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Hexcore's rune matrix as a puzzle cube: 18 square and 8 triangular tiles on a rhombicuboctahedron. A twist turns
 * one layer of tiles (those on one side of a plane across an axis, or the band in the middle) a quarter turn about that
 * axis, which maps the shape onto itself, so the tiles really travel like the faces of a Rubik's cube. Client side only:
 * every Hexcore keeps its own arrangement while it is loaded.
 */
public final class HexcoreMatrix {
    /** Tile models in {@code block/hexcore/}, in the order of {@link #NORMALS}. */
    public static final String[] TILES = {
            "tile_px", "tile_nx", "tile_py", "tile_ny", "tile_pz", "tile_nz",
            "tile_pypz", "tile_pynz", "tile_nypz", "tile_nynz", "tile_pxpz", "tile_pxnz", "tile_nxpz", "tile_nxnz",
            "tile_pxpy", "tile_pxny", "tile_nxpy", "tile_nxny",
            "tri_ppp", "tri_ppn", "tri_pnp", "tri_pnn", "tri_npp", "tri_npn", "tri_nnp", "tri_nnn"};
    /** Unit outward normal of every tile as built. */
    public static final Vector3f[] NORMALS = new Vector3f[TILES.length];

    static {
        for (int i = 0; i < TILES.length; i++) {
            NORMALS[i] = normal(TILES[i]);
        }
    }

    private final Quaternionf[] orientation = new Quaternionf[TILES.length];
    private final boolean[] moving = new boolean[TILES.length];
    private int axis;
    private int direction = 1;
    private float start = Float.NaN;
    private float length;
    private float nextAt = Float.NaN;

    public HexcoreMatrix() {
        for (int i = 0; i < TILES.length; i++) {
            orientation[i] = new Quaternionf();
        }
    }

    /**
     * Finishes the twist that is over and starts the next one when it is due.
     *
     * @param time     smooth client time (ticks)
     * @param interval ticks between the end of a twist and the start of the next
     * @param duration ticks a twist takes
     */
    public void update(float time, float interval, float duration, RandomSource random) {
        if (!Float.isNaN(start) && (time >= start + length || time < start)) {
            commit();
        }
        if (Float.isNaN(nextAt) || time < nextAt - interval * 4) {
            nextAt = time + interval * random.nextFloat();
        }
        if (Float.isNaN(start) && time >= nextAt) {
            axis = random.nextInt(3);
            direction = random.nextBoolean() ? 1 : -1;
            int layer = random.nextInt(3) - 1;
            Vector3f current = new Vector3f();
            boolean any = false;
            for (int i = 0; i < TILES.length; i++) {
                orientation[i].transform(NORMALS[i], current);
                float along = current.get(axis);
                moving[i] = layer > 0 ? along > 0.3F : layer < 0 ? along < -0.3F : Math.abs(along) <= 0.3F;
                any |= moving[i];
            }
            if (any) {
                start = time;
                length = duration;
            }
            nextAt = time + duration + interval * (0.6F + 0.8F * random.nextFloat());
        }
    }

    /** Tile {@code index}'s orientation at {@code time}, a running twist included. */
    public Quaternionf pose(int index, float time) {
        Quaternionf pose = new Quaternionf(orientation[index]);
        if (!Float.isNaN(start) && moving[index]) {
            float t = Mth.clamp((time - start) / length, 0, 1);
            float eased = t * t * (3 - 2 * t);
            twist(axis, direction * Mth.HALF_PI * eased).mul(orientation[index], pose);
        }
        return pose;
    }

    /** Whether a twist is turning now (for the click it makes as it settles). */
    public boolean twisting() {
        return !Float.isNaN(start);
    }

    private void commit() {
        Quaternionf quarter = twist(axis, direction * Mth.HALF_PI);
        for (int i = 0; i < TILES.length; i++) {
            if (moving[i]) {
                quarter.mul(orientation[i], orientation[i]);
                orientation[i].normalize();
                moving[i] = false;
            }
        }
        start = Float.NaN;
    }

    private static Quaternionf twist(int axis, float angle) {
        return switch (axis) {
            case 0 -> new Quaternionf().rotationX(angle);
            case 1 -> new Quaternionf().rotationY(angle);
            default -> new Quaternionf().rotationZ(angle);
        };
    }

    /** "tile_pxny" -> (1, -1, 0) normalised; "tri_pnp" -> (1, -1, 1) normalised. */
    private static Vector3f normal(String name) {
        Vector3f n = new Vector3f();
        String code = name.substring(name.indexOf('_') + 1);
        if (name.startsWith("tri_")) {
            for (int a = 0; a < 3; a++) {
                n.setComponent(a, code.charAt(a) == 'p' ? 1 : -1);
            }
        } else {
            for (int k = 0; k + 1 < code.length(); k += 2) {
                n.setComponent(code.charAt(k + 1) - 'x', code.charAt(k) == 'p' ? 1 : -1);
            }
        }
        return n.normalize();
    }
}
