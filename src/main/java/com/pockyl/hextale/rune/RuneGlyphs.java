package com.pockyl.hextale.rune;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composes complex runes in the arcane script (owner, 2026-10-29): the action's sign written first, the target's
 * sign joined to its right, and a swash under both that ends in a dot - one ligature, like a word of a flowing hand.
 * Polylines {@code [x0, y0, x1, y1, ...]} in the unit box, y down; curves are sampled into polylines here, a dot is a
 * very short stroke ({@link #isDot}). The renderers draw the strokes smooth and glowing (never on a pixel grid).
 */
public final class RuneGlyphs {
    /** Strokes shorter than this are dots: drawn as round blobs, not as lines. */
    public static final float DOT = 0.05F;
    private static final List<float[]> SWASH = List.of(
            curve(0.05, 0.9, 0.35, 1.02, 0.65, 0.8, 0.97, 0.94),
            dot(0.97, 0.94));
    private static final Map<ComplexRune, List<float[]>> CACHE = new ConcurrentHashMap<>();

    private RuneGlyphs() {
    }

    /** The simple rune's sign in the unit box. */
    public static List<float[]> radical(Essence rune) {
        return rune.radical();
    }

    /** The complex rune's ligature; cached, never modify the arrays. */
    public static List<float[]> glyph(ComplexRune rune) {
        return CACHE.computeIfAbsent(rune, RuneGlyphs::compose);
    }

    /** Whether a stroke is a dot rather than a line. */
    public static boolean isDot(float[] stroke) {
        float length = 0;
        for (int i = 0; i + 3 < stroke.length; i += 2) {
            length += (float) Math.hypot(stroke[i + 2] - stroke[i], stroke[i + 3] - stroke[i + 1]);
        }
        return length < DOT;
    }

    private static List<float[]> compose(ComplexRune rune) {
        List<float[]> out = new ArrayList<>();
        fit(out, rune.action().radical(), 0, 0.04F, 0.52F, 0.84F);
        fit(out, rune.target().radical(), 0.48F, 0.12F, 1, 0.86F);
        out.addAll(SWASH);
        return List.copyOf(out);
    }

    private static void fit(List<float[]> out, List<float[]> strokes, float x0, float y0, float x1, float y1) {
        for (float[] stroke : strokes) {
            float[] fitted = new float[stroke.length];
            for (int i = 0; i < stroke.length; i += 2) {
                fitted[i] = x0 + stroke[i] * (x1 - x0);
                fitted[i + 1] = y0 + stroke[i + 1] * (y1 - y0);
            }
            out.add(fitted);
        }
    }

    // ---- Stroke builders for the signs ----------------------------------------------------------------------------------

    /** A straight polyline through the points. */
    static float[] line(double... xy) {
        float[] points = new float[xy.length];
        for (int i = 0; i < xy.length; i++) {
            points[i] = (float) xy[i];
        }
        return points;
    }

    /** A cubic Bezier curve from (x0, y0) to (x3, y3), sampled into a polyline. */
    static float[] curve(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
        int steps = 14;
        float[] points = new float[(steps + 1) * 2];
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double a = (1 - t) * (1 - t) * (1 - t);
            double b = 3 * (1 - t) * (1 - t) * t;
            double c = 3 * (1 - t) * t * t;
            double d = t * t * t;
            points[i * 2] = (float) (a * x0 + b * x1 + c * x2 + d * x3);
            points[i * 2 + 1] = (float) (a * y0 + b * y1 + c * y2 + d * y3);
        }
        return points;
    }

    /** An arc of a circle from angle {@code from} to {@code to}, in degrees (0 = right, 90 = down). */
    static float[] arc(double cx, double cy, double r, double from, double to) {
        int steps = Math.max(6, (int) Math.abs((to - from) / 12));
        float[] points = new float[(steps + 1) * 2];
        for (int i = 0; i <= steps; i++) {
            double angle = Math.toRadians(from + (to - from) * i / steps);
            points[i * 2] = (float) (cx + r * Math.cos(angle));
            points[i * 2 + 1] = (float) (cy + r * Math.sin(angle));
        }
        return points;
    }

    /** A dot: a stroke too short to be a line ({@link #isDot}). */
    static float[] dot(double x, double y) {
        return line(x - 0.01, y, x + 0.01, y);
    }
}
