package com.pockyl.hextale.rune;

import java.util.List;
import java.util.Locale;

/**
 * The nine simple runes, one per element. A simple rune is a small hieroglyph, its element's radical: two to four
 * brush strokes in a unit box ([0, 1] x [0, 1], y pointing down). The same radicals compose the complex runes.
 * Stroke geometry comes from the approved planning prototype and is shared by the recognizer and the renderers.
 */
public enum SimpleRune {
    FIRE("fire", 0xFF8A4C, strokes(
            line(0.5, 0.08, 0.5, 0.55),
            line(0.12, 0.95, 0.5, 0.55, 0.88, 0.95),
            line(0.18, 0.28, 0.3, 0.44),
            line(0.82, 0.28, 0.7, 0.44))),
    WATER("water", 0x4FB3FF, strokes(
            line(0.5, 0.04, 0.5, 0.9, 0.38, 0.8),
            line(0.12, 0.32, 0.38, 0.42, 0.1, 0.78),
            line(0.9, 0.22, 0.62, 0.5, 0.92, 0.9))),
    EARTH("earth", 0xC9A26B, strokes(
            line(0.18, 0.38, 0.82, 0.38),
            line(0.5, 0.08, 0.5, 0.86),
            line(0.04, 0.88, 0.96, 0.88))),
    AIR("air", 0xCFE9F2, strokes(
            line(0.12, 0.16, 0.88, 0.16),
            line(0.22, 0.44, 0.78, 0.44),
            line(0.08, 0.7, 0.8, 0.7, 0.94, 0.96))),
    LIGHT("light", 0xFFE08A, strokes(
            line(0.2, 0.08, 0.8, 0.08, 0.8, 0.92, 0.2, 0.92, 0.2, 0.08),
            line(0.2, 0.5, 0.8, 0.5))),
    SHADOW("shadow", 0xA48AD6, strokes(
            line(0.1, 0.28, 0.1, 0.1, 0.9, 0.1, 0.9, 0.28),
            line(0.62, 0.3, 0.28, 0.94),
            line(0.56, 0.6, 0.7, 0.74))),
    LIFE("life", 0x8FE07A, strokes(
            line(0.08, 0.38, 0.92, 0.38),
            line(0.5, 0.04, 0.5, 0.96),
            line(0.5, 0.44, 0.12, 0.86),
            line(0.5, 0.44, 0.88, 0.86))),
    METAL("metal", 0xB8C4CC, strokes(
            line(0.06, 0.42, 0.5, 0.04, 0.94, 0.42),
            line(0.24, 0.58, 0.76, 0.58),
            line(0.5, 0.42, 0.5, 0.9),
            line(0.12, 0.92, 0.88, 0.92))),
    SPARK("spark", 0x7EF0FF, strokes(
            line(0.14, 0.18, 0.74, 0.18, 0.74, 0.64, 0.14, 0.64, 0.14, 0.18),
            line(0.14, 0.41, 0.74, 0.41),
            line(0.44, 0.04, 0.44, 0.86, 0.92, 0.86, 0.92, 0.7)));

    private final String id;
    private final int color;
    private final List<float[]> radical;

    SimpleRune(String id, int color, List<float[]> radical) {
        this.id = id;
        this.color = color;
        this.radical = radical;
    }

    public String id() {
        return id;
    }

    /** Element colour, RGB. */
    public int color() {
        return color;
    }

    /** Radical strokes as polylines {@code [x0, y0, x1, y1, ...]} in the unit box; never modify them. */
    public List<float[]> radical() {
        return radical;
    }

    public int strokeCount() {
        return radical.size();
    }

    public String translationKey() {
        return "rune.hextale.element." + id;
    }

    public static SimpleRune byId(String id) {
        for (SimpleRune rune : values()) {
            if (rune.id.equals(id.toLowerCase(Locale.ROOT))) {
                return rune;
            }
        }
        return null;
    }

    private static List<float[]> strokes(float[]... strokes) {
        return List.of(strokes);
    }

    private static float[] line(double... xy) {
        float[] points = new float[xy.length];
        for (int i = 0; i < xy.length; i++) {
            points[i] = (float) xy[i];
        }
        return points;
    }
}
