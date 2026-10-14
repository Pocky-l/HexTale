package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.RuneGlyphs;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pixel-art drawing for screens: runes rasterised into crisp GUI pixels (no smoothing, like vanilla icons), hexagons
 * and lines. Masks are computed once per shape and size and drawn as horizontal runs.
 */
public final class PixelArt {
    private static final Map<String, int[][]> MASKS = new ConcurrentHashMap<>();

    private PixelArt() {
    }

    /**
     * A filled rectangle added to the GUI batch without flushing it. {@code GuiGraphics.fill} draws every rectangle at
     * once (outside managed drawing), which turns a parchment of thousands of pixel runs into thousands of draw calls;
     * these quads are drawn together with the next flush instead.
     */
    public static void fill(GuiGraphics graphics, int x0, int y0, int x1, int y1, int argb) {
        if (x0 >= x1 || y0 >= y1 || (argb >>> 24) == 0) {
            return;
        }
        Matrix4f pose = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        buffer.addVertex(pose, x0, y0, 0).setColor(argb);
        buffer.addVertex(pose, x0, y1, 0).setColor(argb);
        buffer.addVertex(pose, x1, y1, 0).setColor(argb);
        buffer.addVertex(pose, x1, y0, 0).setColor(argb);
    }

    /** A simple rune's radical in a {@code size} x {@code size} box at {@code x, y} (top left). */
    public static void radical(GuiGraphics graphics, SimpleRune rune, int x, int y, int size, int argb) {
        strokes(graphics, "r" + rune.ordinal(), rune.radical(), x, y, size, argb);
    }

    /** A complex rune's hieroglyph in a {@code size} x {@code size} box. */
    public static void glyph(GuiGraphics graphics, ComplexRune rune, int x, int y, int size, int argb) {
        strokes(graphics, "g" + rune.index(), RuneGlyphs.glyph(rune), x, y, size, argb);
    }

    /** Same with a one-pixel drop shadow down-right, like vanilla text. */
    public static void glyph(GuiGraphics graphics, ComplexRune rune, int x, int y, int size, int argb, int shadow) {
        glyph(graphics, rune, x + 1, y + 1, size, shadow);
        glyph(graphics, rune, x, y, size, argb);
    }

    public static void radical(GuiGraphics graphics, SimpleRune rune, int x, int y, int size, int argb, int shadow) {
        radical(graphics, rune, x + 1, y + 1, size, shadow);
        radical(graphics, rune, x, y, size, argb);
    }

    private static void strokes(GuiGraphics graphics, String key, List<float[]> strokes, int x, int y, int size, int argb) {
        int[][] runs = MASKS.computeIfAbsent(key + "@" + size, k -> rasterise(strokes, size));
        for (int[] run : runs) {
            fill(graphics, x + run[1], y + run[0], x + run[2], y + run[0] + 1, argb);
        }
    }

    /** Line thickness in pixels for a glyph of this size: one pixel up to 20, then thicker. */
    private static float thickness(int size) {
        return size <= 20 ? 1 : size / 20.0F;
    }

    /** Rows of {@code [y, x0, x1)} runs of the pixels whose centres lie on a stroke. */
    private static int[][] rasterise(List<float[]> strokes, int size) {
        float half = thickness(size) / 2;
        // The unit box maps onto pixel centres 0.5 .. size - 0.5, the roof of perception runes may stick out a little.
        float scale = size - 1;
        boolean[][] on = new boolean[size][size];
        for (float[] stroke : strokes) {
            for (int i = 0; i + 3 < stroke.length; i += 2) {
                float ax = 0.5F + Math.max(-0.02F, Math.min(1.02F, stroke[i])) * scale;
                float ay = 0.5F + Math.max(-0.04F, Math.min(1.02F, stroke[i + 1])) * scale;
                float bx = 0.5F + Math.max(-0.02F, Math.min(1.02F, stroke[i + 2])) * scale;
                float by = 0.5F + Math.max(-0.04F, Math.min(1.02F, stroke[i + 3])) * scale;
                for (int py = 0; py < size; py++) {
                    for (int px = 0; px < size; px++) {
                        if (!on[py][px] && distance(px + 0.5F, py + 0.5F, ax, ay, bx, by) <= half + 0.02F) {
                            on[py][px] = true;
                        }
                    }
                }
            }
        }
        return runs(on);
    }

    static int[][] runs(boolean[][] on) {
        List<int[]> runs = new ArrayList<>();
        for (int py = 0; py < on.length; py++) {
            int start = -1;
            for (int px = 0; px <= on[py].length; px++) {
                boolean set = px < on[py].length && on[py][px];
                if (set && start < 0) {
                    start = px;
                } else if (!set && start >= 0) {
                    runs.add(new int[]{py, start, px});
                    start = -1;
                }
            }
        }
        return runs.toArray(int[][]::new);
    }

    private static float distance(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float length = dx * dx + dy * dy;
        float t = length < 1.0E-6F ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / length));
        float cx = ax + dx * t - px;
        float cy = ay + dy * t - py;
        return (float) Math.sqrt(cx * cx + cy * cy);
    }

    /** A pixel line between two points (inclusive), one pixel wide. */
    public static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int argb) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            fill(graphics, x0, y0, x0 + 1, y0 + 1, argb);
            if (x0 == x1 && y0 == y1) {
                return;
            }
            int e2 = 2 * error;
            if (e2 >= dy) {
                error += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                error += dx;
                y0 += sy;
            }
        }
    }

    /** A dotted pixel line: {@code on} pixels drawn, {@code off} skipped, starting {@code phase} pixels in. */
    public static void dotted(GuiGraphics graphics, int x0, int y0, int x1, int y1, int on, int off, int phase, int argb) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            if ((i + phase) % (on + off) < on) {
                int x = Math.round(x0 + (x1 - x0) * (steps == 0 ? 0 : (float) i / steps));
                int y = Math.round(y0 + (y1 - y0) * (steps == 0 ? 0 : (float) i / steps));
                fill(graphics, x, y, x + 1, y + 1, argb);
            }
        }
    }

    /**
     * A pointy-top pixel hexagon {@code width} wide (odd) centred on {@code cx, cy}: filled, then an outline.
     * Either colour may be 0 to skip it.
     */
    public static void hex(GuiGraphics graphics, int cx, int cy, int width, int fill, int outline) {
        Hex hex = Hex.of(width);
        if (outline != 0) {
            for (int[] run : hex.outer) {
                fill(graphics, cx + run[1], cy + run[0], cx + run[2], cy + run[0] + 1, outline);
            }
        }
        if (fill != 0) {
            for (int[] run : outline != 0 ? hex.inner : hex.outer) {
                fill(graphics, cx + run[1], cy + run[0], cx + run[2], cy + run[0] + 1, fill);
            }
        }
    }

    /** Only the one-pixel rim of a hexagon. */
    public static void hexRing(GuiGraphics graphics, int cx, int cy, int width, int argb) {
        for (int[] run : Hex.of(width).ring) {
            fill(graphics, cx + run[1], cy + run[0], cx + run[2], cy + run[0] + 1, argb);
        }
    }

    /** Pixel masks of a hexagon and of its inside one pixel in, as runs relative to the centre pixel. */
    public static final class Hex {
        private static final Map<Integer, Hex> CACHE = new ConcurrentHashMap<>();
        final int[][] outer;
        final int[][] inner;
        final int[][] ring;
        public final int height;

        private Hex(int width) {
            int half = width / 2;
            double circumradius = (half + 0.5) * 2 / Math.sqrt(3) + 0.15;
            int rows = (int) Math.ceil(circumradius) * 2 + 1;
            boolean[][] outer = mask(half, circumradius, rows, 0);
            boolean[][] inner = mask(half, circumradius, rows, 1);
            this.outer = shift(runs(outer), rows / 2, half);
            this.inner = shift(runs(inner), rows / 2, half);
            boolean[][] rim = new boolean[rows][half * 2 + 1];
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col <= half * 2; col++) {
                    rim[row][col] = outer[row][col] && !inner[row][col];
                }
            }
            this.ring = shift(runs(rim), rows / 2, half);
            int top = rows;
            int bottom = 0;
            for (int[] run : this.outer) {
                top = Math.min(top, run[0]);
                bottom = Math.max(bottom, run[0]);
            }
            this.height = bottom - top + 1;
        }

        public static Hex of(int width) {
            return CACHE.computeIfAbsent(width, Hex::new);
        }

        private static boolean[][] mask(int half, double circumradius, int rows, int inset) {
            boolean[][] on = new boolean[rows][half * 2 + 1];
            int mid = rows / 2;
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col <= half * 2; col++) {
                    double x = Math.abs(col - half);
                    double y = Math.abs(row - mid);
                    boolean inside = x <= half - inset + 0.01 && y + x / Math.sqrt(3) <= circumradius - 0.5 - inset * 1.15;
                    on[row][col] = inside;
                }
            }
            return on;
        }

        private static int[][] shift(int[][] runs, int dy, int dx) {
            for (int[] run : runs) {
                run[0] -= dy;
                run[1] -= dx;
                run[2] -= dx;
            }
            return runs;
        }
    }

    public static int argb(int rgb, float alpha) {
        return (Math.max(0, Math.min(255, (int) (alpha * 255))) << 24) | (rgb & 0xFFFFFF);
    }

    /** Blends {@code a} towards {@code b} by {@code t} (0 = a, 1 = b). */
    public static int mix(int a, int b, float t) {
        int r = (int) ((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int g = (int) ((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | g << 8 | bl;
    }

    /** An element or category colour made dark enough to read on parchment. */
    public static int onParchment(int rgb) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        float luma = (0.3F * r + 0.59F * g + 0.11F * b) / 255;
        return mix(rgb, 0x1E140A, Math.max(0.15F, luma - 0.35F));
    }
}
