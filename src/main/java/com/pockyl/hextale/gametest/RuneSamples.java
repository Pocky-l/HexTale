package com.pockyl.hextale.gametest;

import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Synthetic drawings for recognizer tests: radicals drawn by a shaky hand (any size, slight tilt and stretch, wobbly
 * lines, overshooting ends, random stroke order and direction) and random scribbles. Coordinates are in units of the
 * drawing circle's radius, like the strokes the Research Table screen sends.
 */
public final class RuneSamples {
    /** The radical's unit box spans this many circle radii when drawn at its intended size. */
    public static final double RADICAL_SIZE = 1.25;

    private RuneSamples() {
    }

    /** The clean radical, centred in the circle at its intended size. */
    public static List<float[]> clean(SimpleRune rune) {
        List<float[]> strokes = new ArrayList<>();
        for (float[] stroke : rune.radical()) {
            float[] out = new float[stroke.length];
            for (int i = 0; i < stroke.length; i += 2) {
                out[i] = (float) ((stroke[i] - 0.5) * RADICAL_SIZE);
                out[i + 1] = (float) ((stroke[i + 1] - 0.5) * RADICAL_SIZE);
            }
            strokes.add(out);
        }
        return strokes;
    }

    /**
     * @param wobble 1 = careful hand, 2 = quick sketch, 3 = sloppy
     */
    public static List<float[]> wobbly(SimpleRune rune, Random random, double wobble) {
        double size = RADICAL_SIZE * (0.7 + random.nextDouble() * 0.6);
        double aspect = 1 + (random.nextDouble() - 0.5) * 0.12 * wobble;
        double angle = Math.toRadians((random.nextDouble() - 0.5) * 6 * wobble);
        double offsetX = (random.nextDouble() - 0.5) * 0.4;
        double offsetY = (random.nextDouble() - 0.5) * 0.4;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        List<float[]> strokes = new ArrayList<>();
        for (float[] stroke : rune.radical()) {
            List<double[]> points = densify(stroke, 0.02);
            overshoot(points, random, 0.03 * wobble);
            double phaseA = random.nextDouble() * Math.PI * 2;
            double phaseB = random.nextDouble() * Math.PI * 2;
            double amplitude = 0.012 * wobble;
            float[] out = new float[points.size() * 2];
            for (int i = 0; i < points.size(); i++) {
                double[] p = points.get(i);
                double t = i / (double) Math.max(1, points.size() - 1);
                double wave = Math.sin(t * 5.3 + phaseA) * amplitude + Math.sin(t * 11.9 + phaseB) * amplitude * 0.5;
                double x = p[0] + wave + random.nextGaussian() * 0.003 * wobble;
                double y = p[1] - wave * 0.7 + random.nextGaussian() * 0.003 * wobble;
                double lx = (x - 0.5) * size * aspect;
                double ly = (y - 0.5) * size;
                out[i * 2] = (float) (lx * cos - ly * sin + offsetX);
                out[i * 2 + 1] = (float) (lx * sin + ly * cos + offsetY);
            }
            if (random.nextBoolean()) {
                out = reversed(out);
            }
            strokes.add(out);
        }
        Collections.shuffle(strokes, random);
        return strokes;
    }

    /** One to four strokes of random zigzags and loops inside the circle. */
    public static List<float[]> scribble(Random random) {
        int count = 1 + random.nextInt(4);
        List<float[]> strokes = new ArrayList<>();
        for (int s = 0; s < count; s++) {
            int vertices = 2 + random.nextInt(5);
            float[] polyline = new float[vertices * 2];
            for (int i = 0; i < vertices * 2; i++) {
                polyline[i] = (float) ((random.nextDouble() - 0.5) * 1.6);
            }
            List<double[]> points = densify(polyline, 0.03);
            float[] out = new float[points.size() * 2];
            for (int i = 0; i < points.size(); i++) {
                out[i * 2] = (float) (points.get(i)[0] + random.nextGaussian() * 0.01);
                out[i * 2 + 1] = (float) (points.get(i)[1] + random.nextGaussian() * 0.01);
            }
            strokes.add(out);
        }
        return strokes;
    }

    private static List<double[]> densify(float[] polyline, double step) {
        List<double[]> points = new ArrayList<>();
        points.add(new double[]{polyline[0], polyline[1]});
        for (int i = 2; i + 1 < polyline.length; i += 2) {
            double x0 = polyline[i - 2];
            double y0 = polyline[i - 1];
            double x1 = polyline[i];
            double y1 = polyline[i + 1];
            int steps = Math.max(1, (int) Math.ceil(Math.hypot(x1 - x0, y1 - y0) / step));
            for (int k = 1; k <= steps; k++) {
                double t = k / (double) steps;
                points.add(new double[]{x0 + (x1 - x0) * t, y0 + (y1 - y0) * t});
            }
        }
        return points;
    }

    /** Moves both ends along the stroke direction: the brush stops a little short or runs on. */
    private static void overshoot(List<double[]> points, Random random, double amount) {
        if (points.size() < 2) {
            return;
        }
        extend(points.get(0), points.get(1), (random.nextDouble() - 0.4) * amount);
        extend(points.get(points.size() - 1), points.get(points.size() - 2), (random.nextDouble() - 0.4) * amount);
    }

    private static void extend(double[] end, double[] neighbour, double by) {
        double dx = end[0] - neighbour[0];
        double dy = end[1] - neighbour[1];
        double length = Math.hypot(dx, dy);
        if (length > 0) {
            end[0] += dx / length * by;
            end[1] += dy / length * by;
        }
    }

    private static float[] reversed(float[] polyline) {
        float[] out = new float[polyline.length];
        for (int i = 0; i < polyline.length; i += 2) {
            out[polyline.length - 2 - i] = polyline[i];
            out[polyline.length - 1 - i] = polyline[i + 1];
        }
        return out;
    }
}
