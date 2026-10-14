package com.pockyl.hextale.client.render;

import net.minecraft.util.Mth;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.RuneGlyphs;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.List;

/** Draws radicals and complex-rune hieroglyphs with {@link InkRenderer} in a few looks. */
public final class RuneDrawing {
    private RuneDrawing() {
    }

    public enum Style {
        /** Coloured ink with a dark outline, readable on light parchment. */
        INK,
        /** Coloured core in a soft additive halo with a hot white centre, for dark backgrounds. */
        GLOW,
        /** A pale trace of a rune not known yet. */
        FAINT
    }

    public static void radical(InkRenderer ink, SimpleRune rune, float cx, float cy, float size, Style style, float alpha) {
        strokes(ink, RuneGlyphs.radical(rune), cx, cy, size, rune.color(), style, alpha);
    }

    public static void glyph(InkRenderer ink, ComplexRune rune, float cx, float cy, float size, Style style, float alpha) {
        strokes(ink, RuneGlyphs.glyph(rune), cx, cy, size, rune.color(), style, alpha);
    }

    /** Line width that keeps a rune of this size legible without clogging its small parts. */
    public static float lineWidth(float size) {
        return Math.max(1.0F, size * 0.075F);
    }

    /**
     * @param strokes polylines in the unit box
     * @param size    side of the unit box in GUI pixels, centred on {@code cx, cy}
     */
    public static void strokes(InkRenderer ink, List<float[]> strokes, float cx, float cy, float size, int rgb, Style style,
                               float alpha) {
        float width = lineWidth(size);
        for (float[] stroke : strokes) {
            float[] placed = place(stroke, cx, cy, size);
            switch (style) {
                case INK -> {
                    ink.stroke(placed, width * 1.8F, argb(darken(rgb, 0.36F), alpha));
                    ink.stroke(placed, width, argb(rgb, alpha));
                }
                case GLOW -> {
                    ink.glow(placed, width * 5.0F, argb(rgb, 0.42F * alpha));
                    ink.stroke(placed, width, argb(rgb, alpha));
                    ink.stroke(placed, width * 0.38F, argb(lighten(rgb, 0.65F), 0.75F * alpha));
                }
                case FAINT -> ink.stroke(placed, width * 0.8F, argb(rgb, 0.3F * alpha));
            }
        }
    }

    public static float[] place(float[] stroke, float cx, float cy, float size) {
        float[] out = new float[stroke.length];
        for (int i = 0; i < stroke.length; i += 2) {
            out[i] = cx + (stroke[i] - 0.5F) * size;
            out[i + 1] = cy + (stroke[i + 1] - 0.5F) * size;
        }
        return out;
    }

    public static int argb(int rgb, float alpha) {
        return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    public static int darken(int rgb, float factor) {
        return mix(rgb, 0x1A120A, 1 - factor);
    }

    public static int lighten(int rgb, float amount) {
        return mix(rgb, 0xFFFFFF, amount);
    }

    /** Blends {@code a} towards {@code b} by {@code t} (0 = a, 1 = b). */
    public static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, a >> 16 & 0xFF, b >> 16 & 0xFF);
        int g = (int) Mth.lerp(t, a >> 8 & 0xFF, b >> 8 & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return r << 16 | g << 8 | bl;
    }
}
