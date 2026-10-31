package com.pockyl.hextale.thevoid.realm;

/**
 * Fog banks of the Void: every few minutes a dense lavender bank rolls over a stretch of it for half a minute, hiding
 * the edges. Worked out from the time and the place alone, so the server and every client agree on it.
 */
public final class VoidFog {
    /** Size of the stretch one bank covers, in blocks. */
    public static final int CELL = 384;
    public static final int PERIOD = 6000;
    public static final int LENGTH = 700;
    public static final int RAMP = 120;

    private VoidFog() {
    }

    /** 0 clear .. 1 inside a bank at its thickest. */
    public static float bank(double x, double z, long gameTime) {
        int cx = Math.floorDiv((int) Math.floor(x), CELL), cz = Math.floorDiv((int) Math.floor(z), CELL);
        long phase = (long) (VoidShapes.hash(cx, 0, cz, 0xF06B) * PERIOD);
        long t = Math.floorMod(gameTime + phase, PERIOD);
        if (t >= LENGTH) {
            return 0;
        }
        float in = Math.min(1, t / (float) RAMP);
        float out = Math.min(1, (LENGTH - t) / (float) RAMP);
        return Math.min(in, out);
    }
}
