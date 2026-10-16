package com.pockyl.hextale.rune;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composes complex runes into hieroglyphs: the action radical and the target radical fitted into the boxes of the
 * category's structure (left-right, top-bottom, inside a frame, under a roof). Polylines {@code [x0, y0, x1, y1, ...]}
 * in the unit box, y down; the roof may reach slightly outside it. Ported from the planning prototype's
 * {@code glyphStrokes}, so the in-game runes look exactly like the approved ones.
 */
public final class RuneGlyphs {
    private static final float[] FRAME = {0, 0, 1, 0, 1, 1, 0, 1, 0, 0};
    private static final List<float[]> ROOF = List.of(
            new float[]{0.5F, -0.04F, 0.54F, 0.06F},
            new float[]{0.04F, 0.14F, 0.98F, 0.14F},
            new float[]{0.1F, 0.14F, 0.1F, 0.66F, -0.02F, 0.98F});
    private static final Map<ComplexRune, List<float[]>> CACHE = new ConcurrentHashMap<>();

    private RuneGlyphs() {
    }

    /** The simple rune's radical in the unit box. */
    public static List<float[]> radical(Essence rune) {
        return rune.radical();
    }

    /** The complex rune's hieroglyph; cached, never modify the arrays. */
    public static List<float[]> glyph(ComplexRune rune) {
        return CACHE.computeIfAbsent(rune, RuneGlyphs::compose);
    }

    private static List<float[]> compose(ComplexRune rune) {
        List<float[]> a = rune.action().radical();
        List<float[]> b = rune.target().radical();
        List<float[]> out = new ArrayList<>();
        switch (rune.category().layout()) {
            case LEFT_RIGHT -> {
                fit(out, a, 0, 0.02F, 0.4F, 0.98F);
                fit(out, b, 0.47F, 0, 1, 1);
            }
            case TOP_BOTTOM -> {
                fit(out, a, 0.1F, 0, 0.9F, 0.4F);
                fit(out, b, 0, 0.47F, 1, 1);
            }
            case BOX -> {
                out.add(FRAME);
                fit(out, a, 0.2F, 0.12F, 0.8F, 0.44F);
                fit(out, b, 0.16F, 0.52F, 0.84F, 0.9F);
            }
            case ROOF -> {
                out.addAll(ROOF);
                fit(out, a, 0.24F, 0.26F, 0.58F, 0.96F);
                fit(out, b, 0.62F, 0.26F, 1, 0.96F);
            }
        }
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
}
