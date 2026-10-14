package com.pockyl.hextale.rune;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * $P point-cloud recognizer for simple runes, ported 1:1 from the tuned planning prototype. Stroke order, direction
 * and how strokes are joined do not matter: the strokes are resampled into one cloud of {@link #POINTS} points,
 * normalised by centroid and the larger side of the bounding box, and greedily matched against every radical.
 * Used by the client for instant feedback and by the server, which re-runs it on the submitted strokes.
 *
 * <p>Coordinates are in units of the drawing circle's radius (the canvas maps the circle to radius 1).
 */
public final class RuneRecognizer {
    public static final int POINTS = 48;
    /** Greedy matching starts from every {@code STEP}-th point, in both directions. */
    private static final int STEP = 6;
    /** A match closer than this is accepted (prototype: ~98% of wobbly radicals, 0-2% of scribbles). */
    public static final double ACCEPT_DISTANCE = 2.1;
    public static final int STROKE_TOLERANCE = 1;
    private static final double STROKE_PENALTY = 3;
    /** Ink shorter than this is a stray dot: 30 px on the prototype's circle of radius 187.2 px. */
    public static final double MIN_LENGTH = 30 / 187.2;
    /** Pause after the last stroke before the circle is read: reading after every stroke cut longer radicals short. */
    public static final long READ_DELAY_MS = 800;

    private static final Map<SimpleRune, double[][]> TEMPLATES = new EnumMap<>(SimpleRune.class);

    static {
        for (SimpleRune rune : SimpleRune.values()) {
            Cloud cloud = cloud(rune.radical());
            TEMPLATES.put(rune, cloud == null ? new double[0][] : cloud.points);
        }
    }

    private RuneRecognizer() {
    }

    /** The closest radical and its distance; {@link #accepted()} tells whether the orb recognises it. */
    public record Match(SimpleRune rune, double distance, int strokes) {
        public boolean accepted() {
            return distance < ACCEPT_DISTANCE && Math.abs(strokes - rune.strokeCount()) <= STROKE_TOLERANCE;
        }
    }

    /** Best match for the strokes of one circle, or null when there is too little ink to judge. */
    @Nullable
    public static Match match(List<float[]> strokes) {
        if (strokes.isEmpty()) {
            return null;
        }
        Cloud cloud = cloud(strokes);
        if (cloud == null || cloud.length < MIN_LENGTH) {
            return null;
        }
        Match best = null;
        for (SimpleRune rune : SimpleRune.values()) {
            double distance = greedy(cloud.points, TEMPLATES.get(rune))
                    + (Math.abs(strokes.size() - rune.strokeCount()) > STROKE_TOLERANCE ? STROKE_PENALTY : 0);
            if (best == null || distance < best.distance) {
                best = new Match(rune, distance, strokes.size());
            }
        }
        return best;
    }

    /** The recognised radical, or null when the orb does not know the drawing. */
    @Nullable
    public static SimpleRune recognise(List<float[]> strokes) {
        Match match = match(strokes);
        return match != null && match.accepted() ? match.rune : null;
    }

    private record Cloud(double[][] points, double length) {
    }

    /**
     * Resamples all strokes into {@link #POINTS} equidistant points, as the prototype does: the walked distance
     * carries over between strokes and a new stroke contributes its first point only when a sample falls exactly on it.
     */
    @Nullable
    private static Cloud cloud(List<float[]> strokes) {
        double length = 0;
        for (float[] stroke : strokes) {
            for (int i = 2; i + 1 < stroke.length; i += 2) {
                length += Math.hypot(stroke[i] - stroke[i - 2], stroke[i + 1] - stroke[i - 1]);
            }
        }
        if (length == 0) {
            return null;
        }
        double gap = length / (POINTS - 1);
        double[][] out = new double[POINTS][];
        int count = 0;
        double walked = 0;
        for (float[] stroke : strokes) {
            if (stroke.length < 2) {
                continue;
            }
            if ((count == 0 || walked == 0) && count < POINTS) {
                out[count++] = new double[]{stroke[0], stroke[1]};
            }
            double px = stroke[0];
            double py = stroke[1];
            for (int i = 2; i + 1 < stroke.length; i += 2) {
                double cx = stroke[i];
                double cy = stroke[i + 1];
                double d = Math.hypot(cx - px, cy - py);
                while (d > 0 && walked + d >= gap && count < POINTS) {
                    double t = (gap - walked) / d;
                    double qx = px + (cx - px) * t;
                    double qy = py + (cy - py) * t;
                    out[count++] = new double[]{qx, qy};
                    px = qx;
                    py = qy;
                    d = Math.hypot(cx - px, cy - py);
                    walked = 0;
                }
                walked += d;
                px = cx;
                py = cy;
            }
        }
        float[] last = strokes.get(strokes.size() - 1);
        while (count < POINTS) {
            out[count++] = new double[]{last[last.length - 2], last[last.length - 1]};
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double centreX = 0;
        double centreY = 0;
        for (double[] p : out) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
            centreX += p[0] / POINTS;
            centreY += p[1] / POINTS;
        }
        double scale = Math.max(maxX - minX, maxY - minY);
        if (scale == 0) {
            scale = 1;
        }
        double[][] points = new double[POINTS][];
        for (int i = 0; i < POINTS; i++) {
            points[i] = new double[]{(out[i][0] - centreX) / scale, (out[i][1] - centreY) / scale};
        }
        return new Cloud(points, length);
    }

    private static double greedy(double[][] drawn, double[][] template) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < POINTS; i += STEP) {
            min = Math.min(min, Math.min(cloudDistance(drawn, template, i), cloudDistance(template, drawn, i)));
        }
        return min;
    }

    /** Greedy one-to-one matching from {@code start}; earlier matches weigh more, as in $P. */
    private static double cloudDistance(double[][] a, double[][] b, int start) {
        boolean[] used = new boolean[POINTS];
        double sum = 0;
        int i = start;
        do {
            int best = -1;
            double min = Double.POSITIVE_INFINITY;
            for (int j = 0; j < POINTS; j++) {
                if (!used[j]) {
                    double d = Math.hypot(a[i][0] - b[j][0], a[i][1] - b[j][1]);
                    if (d < min) {
                        min = d;
                        best = j;
                    }
                }
            }
            used[best] = true;
            sum += (1 - ((i - start + POINTS) % POINTS) / (double) POINTS) * min;
            i = (i + 1) % POINTS;
        } while (i != start);
        return sum;
    }
}
