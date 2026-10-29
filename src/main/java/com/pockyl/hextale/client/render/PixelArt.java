package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pixel-art drawing for screens: runes rasterised into crisp GUI pixels (no smoothing, like vanilla icons), hexagons
 * and lines. Masks are computed once per shape and size and drawn as horizontal runs.
 *
 * <p>A parchment is thousands of one-pixel runs. Each {@code GuiGraphicsExtractor.fill} becomes its own GUI element that
 * the GUI renderer sorts into layers by overlap, so the runs are collected into one batch element instead. A batch is
 * submitted by {@link #flush}, which callers do before anything that must appear above it (text, items, sprites) and at
 * the end of a frame; a change of pose or scissor flushes it on its own.
 */
public final class PixelArt {
    private static final Identifier ESSENCE_ICONS = Hextale.id("textures/gui/essence_icons.png");
    /** Essence pictograms take 9 x 9 GUI pixels; the sheet holds them at 18 x 18, one every 20 pixels. */
    public static final int ICON = 9;
    private static final int ICON_TEXELS = 18;
    private static final int ICON_STEP = 20;
    private static final int ICONS_PER_ROW = 16;
    private static final int ICONS_WIDTH = 512;
    private static final int ICONS_HEIGHT = 128;
    private static final Batch BATCH = new Batch();

    private PixelArt() {
    }

    /** A filled rectangle, added to the current batch. */
    public static void fill(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int argb) {
        if (x0 >= x1 || y0 >= y1 || (argb >>> 24) == 0) {
            return;
        }
        BATCH.add(graphics, x0, y0, x1, y1, argb);
    }

    /** Submits the collected pixels, so whatever is drawn next lies above them. */
    public static void flush(GuiGraphicsExtractor graphics) {
        BATCH.submit(graphics);
    }

    /**
     * The essence's pictogram ({@code gui/essence_icons.png}, greys) tinted with {@code argb}, scaled by whole pixels
     * and centred in a {@code size} x {@code size} box at {@code x, y} (top left).
     */
    public static void essence(GuiGraphicsExtractor graphics, Essence essence, int x, int y, int size, int argb) {
        flush(graphics);
        int scale = Math.max(1, size / ICON);
        int drawn = ICON * scale;
        int ox = x + (size - drawn) / 2;
        int oy = y + (size - drawn) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, ESSENCE_ICONS, ox, oy, essence.ordinal() % ICONS_PER_ROW * ICON_STEP,
                essence.ordinal() / ICONS_PER_ROW * ICON_STEP, drawn, drawn, ICON_TEXELS, ICON_TEXELS,
                ICONS_WIDTH, ICONS_HEIGHT, argb);
    }

    /** A complex rune's sign in a {@code size} x {@code size} box, drawn smooth and glowing ({@link RuneIcons}). */
    public static void glyph(GuiGraphicsExtractor graphics, ComplexRune rune, int x, int y, int size, int argb) {
        RuneIcons.draw(graphics, rune, x, y, size, argb);
    }

    /** The same; the old drop shadow is no longer drawn - the rune's own glow sets it off the page. */
    public static void glyph(GuiGraphicsExtractor graphics, ComplexRune rune, int x, int y, int size, int argb, int shadow) {
        glyph(graphics, rune, x, y, size, argb);
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

    /** A pixel line between two points (inclusive), one pixel wide. */
    public static void line(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int argb) {
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
    public static void dotted(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int on, int off, int phase, int argb) {
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
    public static void hex(GuiGraphicsExtractor graphics, int cx, int cy, int width, int fill, int outline) {
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
    public static void hexRing(GuiGraphicsExtractor graphics, int cx, int cy, int width, int argb) {
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

    /**
     * A pixel disc {@code diameter} wide (even) centred on the pixel corner {@code cx, cy}, like an item icon at
     * {@code cx - 8, cy - 8}: filled, then a one-pixel outline. Either colour may be 0 to skip it.
     */
    public static void disc(GuiGraphicsExtractor graphics, int cx, int cy, int diameter, int fill, int outline) {
        Circle circle = Circle.of(diameter);
        if (outline != 0) {
            for (int[] run : circle.ring) {
                fill(graphics, cx + run[1], cy + run[0], cx + run[2], cy + run[0] + 1, outline);
            }
        }
        if (fill != 0) {
            for (int[] run : outline != 0 ? circle.inner : circle.outer) {
                fill(graphics, cx + run[1], cy + run[0], cx + run[2], cy + run[0] + 1, fill);
            }
        }
    }

    /** Only the one-pixel rim of a disc. */
    public static void discRing(GuiGraphicsExtractor graphics, int cx, int cy, int diameter, int argb) {
        for (int[] run : Circle.of(diameter).ring) {
            fill(graphics, cx + run[1], cy + run[0], cx + run[2], cy + run[0] + 1, argb);
        }
    }

    /** Pixel masks of a disc centred on a pixel corner; {@code pixels} = the rim's pixels with their angle. */
    public static final class Circle {
        private static final Map<Integer, Circle> CACHE = new ConcurrentHashMap<>();
        final int[][] outer;
        final int[][] inner;
        final int[][] ring;
        /** {@code [dx, dy]} of every rim pixel, relative to the centre corner. */
        public final int[][] pixels;
        /** The direction of each rim pixel from the centre, radians, 0 = right, growing clockwise on screen. */
        public final float[] angles;

        private Circle(int diameter) {
            int r = diameter / 2;
            boolean[][] outer = new boolean[r * 2][r * 2];
            boolean[][] inner = new boolean[r * 2][r * 2];
            boolean[][] rim = new boolean[r * 2][r * 2];
            List<int[]> pixels = new ArrayList<>();
            for (int row = 0; row < r * 2; row++) {
                for (int col = 0; col < r * 2; col++) {
                    float x = col - r + 0.5F;
                    float y = row - r + 0.5F;
                    float d = x * x + y * y;
                    outer[row][col] = d <= r * r;
                    inner[row][col] = d <= (r - 1) * (r - 1);
                    rim[row][col] = outer[row][col] && !inner[row][col];
                    if (rim[row][col]) {
                        pixels.add(new int[]{col - r, row - r});
                    }
                }
            }
            this.outer = Hex.shift(runs(outer), r, r);
            this.inner = Hex.shift(runs(inner), r, r);
            this.ring = Hex.shift(runs(rim), r, r);
            this.pixels = pixels.toArray(int[][]::new);
            this.angles = new float[this.pixels.length];
            for (int i = 0; i < this.pixels.length; i++) {
                angles[i] = (float) Math.atan2(this.pixels[i][1] + 0.5, this.pixels[i][0] + 0.5);
            }
        }

        public static Circle of(int diameter) {
            return CACHE.computeIfAbsent(diameter, Circle::new);
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

    /** The pixels collected for one GUI element: quads in screen space under one pose and scissor. */
    private static final class Batch {
        private @Nullable GuiGraphicsExtractor owner;
        private final Matrix3x2f pose = new Matrix3x2f();
        private @Nullable ScreenRectangle scissor;
        private int[] quads = new int[5 * 256];
        private int size;
        private int minX;
        private int minY;
        private int maxX;
        private int maxY;

        void add(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int argb) {
            Matrix3x2fc current = graphics.pose();
            ScreenRectangle currentScissor = graphics.peekScissorStack();
            if (owner != graphics || !pose.equals(current, 0) || !Objects.equals(scissor, currentScissor)) {
                if (owner == graphics) {
                    submit(graphics);
                } else {
                    size = 0;
                }
                owner = graphics;
                pose.set(current);
                scissor = currentScissor;
            }
            if (size == 0) {
                minX = x0;
                minY = y0;
                maxX = x1;
                maxY = y1;
            } else {
                minX = Math.min(minX, x0);
                minY = Math.min(minY, y0);
                maxX = Math.max(maxX, x1);
                maxY = Math.max(maxY, y1);
            }
            if (size + 5 > quads.length) {
                quads = Arrays.copyOf(quads, quads.length * 2);
            }
            quads[size++] = x0;
            quads[size++] = y0;
            quads[size++] = x1;
            quads[size++] = y1;
            quads[size++] = argb;
        }

        void submit(GuiGraphicsExtractor graphics) {
            if (owner != graphics || size == 0) {
                size = 0;
                return;
            }
            ScreenRectangle bounds = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY).transformMaxBounds(pose);
            if (scissor != null) {
                bounds = scissor.intersection(bounds);
            }
            if (bounds != null) {
                graphics.submitGuiElementRenderState(new Element(new Matrix3x2f(pose), Arrays.copyOf(quads, size), scissor, bounds));
            }
            size = 0;
        }
    }

    private record Element(Matrix3x2fc pose, int[] quads, @Nullable ScreenRectangle scissorArea, ScreenRectangle bounds)
            implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (int i = 0; i < quads.length; i += 5) {
                int x0 = quads[i];
                int y0 = quads[i + 1];
                int x1 = quads[i + 2];
                int y1 = quads[i + 3];
                int color = quads[i + 4];
                consumer.addVertexWith2DPose(pose, x0, y0).setColor(color);
                consumer.addVertexWith2DPose(pose, x0, y1).setColor(color);
                consumer.addVertexWith2DPose(pose, x1, y1).setColor(color);
                consumer.addVertexWith2DPose(pose, x1, y0).setColor(color);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }
}
