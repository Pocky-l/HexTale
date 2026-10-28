package com.pockyl.hextale.client.screen.book;

import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.quest.Region;

/**
 * The arrows that point at new steps, each in the look of the branch its step belongs to, so the reader knows where an
 * arrow leads before following it: a brass compass needle on its dial (the workshop), a falling drop with droplets behind
 * it (the essences), two chevrons of rune ink writing themselves (the runes), a hextech crystal with sparks circling it
 * (the circle), a pylon's claw holding a gem on a twisting tendril (the pylons), a red-hot spearhead trailing flame
 * (the forge), a leaf on a curling vine (chemtech) and a curved black thorn split by an angry crack (the Void).
 * <p>
 * Every arrow is drawn in pixels at any angle: its shape is worked out in its own frame (u along the arrow, v across),
 * then outlined in its branch's ink and lit from the top left like the medallions. A step written down by itself wears a
 * turning gold ring of dashes around its arrow.
 */
final class MarkArrows {
    /** Half the box an arrow's body is drawn in. */
    private static final int HALF = 12;
    private static final int GOLD = StarBook.GOLD;

    private MarkArrows() {
    }

    /** Where an arrow sits and which way it looks; turns arrow coordinates (u along, v across) into screen ones. */
    private record Frame(float cx, float cy, float ux, float uy) {
        float sx(float u, float v) {
            return cx + u * ux - v * uy;
        }

        float sy(float u, float v) {
            return cy + u * uy + v * ux;
        }

        /** Whether the arrow's +v side faces the light (top left of the screen). */
        boolean plusLit() {
            return uy - ux > 0;
        }

        void dot(GuiGraphicsExtractor graphics, float u, float v, int argb) {
            int x = Mth.floor(sx(u, v));
            int y = Mth.floor(sy(u, v));
            PixelArt.fill(graphics, x, y, x + 1, y + 1, argb);
        }

        /** A soft point: a bright pixel with a dimmer cross around it. */
        void spark(GuiGraphicsExtractor graphics, float u, float v, int rgb, float alpha) {
            int x = Mth.floor(sx(u, v));
            int y = Mth.floor(sy(u, v));
            int dim = PixelArt.argb(rgb, alpha * 0.4F);
            PixelArt.fill(graphics, x - 1, y, x, y + 1, dim);
            PixelArt.fill(graphics, x + 1, y, x + 2, y + 1, dim);
            PixelArt.fill(graphics, x, y - 1, x + 1, y, dim);
            PixelArt.fill(graphics, x, y + 1, x + 1, y + 2, dim);
            PixelArt.fill(graphics, x, y, x + 1, y + 1, PixelArt.argb(rgb, alpha));
        }
    }

    /** Colour of a pixel of the arrow at (u, v), or -1 where it is not. */
    private interface Shape {
        int at(float u, float v);
    }

    /** The colour an arrow of this branch glows in: its halo, the number of steps it stands for. */
    static int glow(Region region) {
        return switch (region) {
            case WORKSHOP -> 0xE8C070;
            case VOID -> Blight.ANGRY;
            default -> region.color();
        };
    }

    /** Draws one arrow pointing along {@code angle}, nodding towards its step; brighter when {@code lit}. */
    static void draw(GuiGraphicsExtractor graphics, Region region, boolean happened, int x, int y, float angle, boolean lit, long now, int seed) {
        float t = now / 1000.0F + seed * 0.37F;
        if (region == Region.CHEMTECH) {
            // The leaf sways on its vine.
            angle += Mth.sin(t * 2.1F) * 0.12F;
        }
        float nod = Mth.sin(now / 260.0F + seed) * 1.5F;
        float ux = Mth.cos(angle);
        float uy = Mth.sin(angle);
        Frame f = new Frame(x + ux * nod, y + uy * nod, ux, uy);
        float pulse = 0.5F + 0.5F * Mth.sin(now / 420.0F + seed);
        float anger = Blight.anger(now, seed);

        halo(graphics, region, f, lit, pulse, anger);
        if (happened) {
            seal(graphics, f, now, pulse);
        }
        behind(graphics, region, f, t, anger);
        body(graphics, region, f, lit, t, anger);
        front(graphics, region, f, t, anger);
    }

    // ---- Around the arrow -----------------------------------------------------------------------------------------

    private static void halo(GuiGraphicsExtractor graphics, Region region, Frame f, boolean lit, float pulse, float anger) {
        int cx = Math.round(f.cx());
        int cy = Math.round(f.cy());
        if (region == Region.VOID) {
            // The Void swallows the light around its thorn and throbs red through the dark.
            PixelArt.disc(graphics, cx, cy, 32, PixelArt.argb(Blight.GLOW, 0.08F + 0.08F * anger), 0);
            PixelArt.disc(graphics, cx, cy, 30, PixelArt.argb(0x2C0A26, 0.7F), 0);
            PixelArt.disc(graphics, cx, cy, 24, PixelArt.argb(Blight.ANGRY, (lit ? 0.16F : 0.08F) + 0.16F * anger), 0);
            PixelArt.disc(graphics, cx, cy, 16, PixelArt.argb(Blight.ANGRY, 0.06F + 0.1F * anger), 0);
            return;
        }
        int color = glow(region);
        float alpha = (lit ? 0.26F : 0.11F) + 0.08F * pulse;
        PixelArt.disc(graphics, cx, cy, 28, PixelArt.argb(color, alpha * 0.45F), 0);
        PixelArt.disc(graphics, cx, cy, 20, PixelArt.argb(color, alpha), 0);
    }

    /** A step written down by itself: a ring of gold dashes turning slowly around its arrow, a glint running round it. */
    private static void seal(GuiGraphicsExtractor graphics, Frame f, long now, float pulse) {
        int cx = Math.round(f.cx());
        int cy = Math.round(f.cy());
        PixelArt.Circle circle = PixelArt.Circle.of(28);
        float turn = now / 1400.0F;
        float glint = (now / 900.0F) % Mth.TWO_PI;
        for (int i = 0; i < circle.pixels.length; i++) {
            float a = circle.angles[i];
            if (Mth.sin((a - turn) * 7) < 0.1F) {
                continue;
            }
            float near = Math.abs(Mth.wrapDegrees((a - glint) * Mth.RAD_TO_DEG)) / 180.0F;
            int color = near < 0.08F ? 0xFFF6D6 : GOLD;
            int[] p = circle.pixels[i];
            PixelArt.fill(graphics, cx + p[0], cy + p[1], cx + p[0] + 1, cy + p[1] + 1, PixelArt.argb(color, 0.55F + 0.3F * pulse));
        }
    }

    // ---- Behind and in front of the body --------------------------------------------------------------------------

    private static void behind(GuiGraphicsExtractor graphics, Region region, Frame f, float t, float anger) {
        switch (region) {
            case WORKSHOP -> dial(graphics, f, t);
            case ESSENCES -> {
                // Droplets falling after the drop, smaller and fainter, each bobbing a little.
                float[] at = {-9.2F, -12.4F, -15.0F};
                for (int k = 0; k < at.length; k++) {
                    float v = Mth.sin(t * 3 + k * 1.7F) * 0.8F;
                    float alpha = 0.85F - k * 0.22F;
                    f.dot(graphics, at[k], v, PixelArt.argb(0x3FE0A0, alpha));
                    if (k == 0) {
                        f.dot(graphics, at[k] + 1, v, PixelArt.argb(0x3FE0A0, alpha));
                        f.dot(graphics, at[k], v + 1, PixelArt.argb(0x1C7A62, alpha));
                        f.dot(graphics, at[k] + 1, v + 1, PixelArt.argb(0x1C7A62, alpha));
                    }
                    f.dot(graphics, at[k] + 0.5F, v - 0.5F, PixelArt.argb(0xE8FFF6, alpha));
                }
            }
            case RUNES -> {
                // The ink's glow along the strokes.
                paint(graphics, f, (u, v) -> runeDistance(u, v) <= 2.6F ? PixelArt.argb(0xB07AF0, 0.22F) : 0);
            }
            case CIRCLE -> orbit(graphics, f, t, false);
            case PYLONS -> tendril(graphics, f, t);
            case FORGE -> {
                // Flame licking back off the blade, flickering.
                paint(graphics, f, (u, v) -> {
                    if (u > -2 || u < -16) {
                        return 0;
                    }
                    float along = (-2 - u) / 14;
                    float width = 3.8F * (1 - along * along) * (0.7F + 0.3F * Mth.sin(t * 11 + u * 1.3F));
                    float middle = Mth.sin(t * 6 + u * 0.8F) * 1.1F * along;
                    if (Math.abs(v - middle) > width) {
                        return 0;
                    }
                    float core = 1 - Math.abs(v - middle) / Math.max(0.5F, width);
                    int color = PixelArt.mix(PixelArt.mix(0xD8401A, 0xFFD060, core), 0x7A1A10, along);
                    return PixelArt.argb(color, (1 - along) * (0.55F + 0.4F * core));
                });
            }
            case CHEMTECH -> vine(graphics, f, t);
            case VOID -> {
                // Dark smoke seeping off the thorn, drifting back and up.
                for (int k = 0; k < 3; k++) {
                    float p = (t * 0.55F + k / 3.0F) % 1;
                    float u = -5 - p * 9;
                    float v = Mth.sin(k * 2.1F + t) * 3;
                    int x = Mth.floor(f.sx(u, v));
                    int y = Mth.floor(f.sy(u, v) - p * 5);
                    int size = p < 0.5F ? 3 : 2;
                    PixelArt.fill(graphics, x, y, x + size, y + size, PixelArt.argb(0x3A1630, 0.75F * (1 - p)));
                }
            }
        }
    }

    private static void front(GuiGraphicsExtractor graphics, Region region, Frame f, float t, float anger) {
        switch (region) {
            case RUNES -> {
                // Small shards of rune ink drifting off behind.
                for (int k = 0; k < 3; k++) {
                    float p = (t * 0.7F + k / 3.0F) % 1;
                    f.dot(graphics, -8 - p * 7, Mth.sin(t * 2 + k * 2.3F) * 4, PixelArt.argb(0xD8C0FF, 0.8F * (1 - p)));
                }
            }
            case CIRCLE -> orbit(graphics, f, t, true);
            case FORGE -> {
                // Embers rising off the hot metal.
                for (int k = 0; k < 3; k++) {
                    float p = (t * 0.8F + k / 3.0F) % 1;
                    int x = Mth.floor(f.sx(-1 - k * 2.5F, (k - 1) * 2.5F) + Mth.sin(t * 3 + k) * 2);
                    int y = Mth.floor(f.sy(-1 - k * 2.5F, (k - 1) * 2.5F) - p * 12);
                    PixelArt.fill(graphics, x, y, x + 1, y + 1, PixelArt.argb(p < 0.4F ? 0xFFE08A : 0xFF8A3A, 0.95F * (1 - p)));
                }
            }
            case CHEMTECH -> {
                // A bubble or two of something not quite healthy rising off the leaf.
                for (int k = 0; k < 2; k++) {
                    float p = (t * 0.45F + k * 0.5F) % 1;
                    int x = Mth.floor(f.sx(1 + k * 3, (k * 2 - 1) * 3) + Mth.sin(t * 4 + k * 3) * 1.5F);
                    int y = Mth.floor(f.sy(1 + k * 3, (k * 2 - 1) * 3) - 4 - p * 10);
                    int color = PixelArt.argb(0x9FE070, 0.8F * (1 - p));
                    if (p < 0.8F) {
                        PixelArt.fill(graphics, x - 1, y, x, y + 1, color);
                        PixelArt.fill(graphics, x + 1, y, x + 2, y + 1, color);
                        PixelArt.fill(graphics, x, y - 1, x + 1, y, color);
                        PixelArt.fill(graphics, x, y + 1, x + 1, y + 2, color);
                    } else {
                        PixelArt.fill(graphics, x, y, x + 1, y + 1, color);
                    }
                }
            }
            case VOID -> {
                if (anger > 0.55F) {
                    // When the Void flares, magenta tears fall off the thorn.
                    for (int k = 0; k < 2; k++) {
                        float p = (t * 1.3F + k * 0.5F) % 1;
                        f.spark(graphics, -2 + k * 4, (k * 2 - 1) * (3 + p * 4), 0xFF6AA8, (anger - 0.4F) * (1 - p));
                    }
                }
            }
            default -> {
            }
        }
    }

    /** The workshop's compass dial: a thin brass ring with ticks, turning slowly under the needle. */
    private static void dial(GuiGraphicsExtractor graphics, Frame f, float t) {
        int cx = Math.round(f.cx());
        int cy = Math.round(f.cy());
        PixelArt.disc(graphics, cx, cy, 20, PixelArt.argb(0x1A140C, 0.55F), PixelArt.argb(0xB08A40, 0.7F));
        float turn = t * 0.25F;
        for (int i = 0; i < 12; i++) {
            float a = turn + i * Mth.PI / 6;
            boolean major = i % 3 == 0;
            float r = major ? 7.5F : 8.0F;
            int x = Mth.floor(cx + Mth.cos(a) * r);
            int y = Mth.floor(cy + Mth.sin(a) * r);
            PixelArt.fill(graphics, x, y, x + 1, y + 1, PixelArt.argb(major ? 0xFFF0B8 : 0xC09040, major ? 0.9F : 0.6F));
        }
    }

    /** Two hextech sparks circling the crystal on a tilted orbit: drawn behind it on the far half, in front on the near. */
    private static void orbit(GuiGraphicsExtractor graphics, Frame f, float t, boolean near) {
        for (int k = 0; k < 2; k++) {
            float a = t * 3 + k * Mth.PI;
            float depth = Mth.sin(a);
            if (depth > 0 != near) {
                continue;
            }
            f.spark(graphics, 1 + 8.5F * Mth.cos(a), 4.2F * depth, near ? 0xBFF0FF : 0x5CA8E8, near ? 0.95F : 0.55F);
        }
    }

    /** The pylon's tendril: two strands twisting round each other behind the claw, pulses of light running to it. */
    private static void tendril(GuiGraphicsExtractor graphics, Frame f, float t) {
        for (int side = -1; side <= 1; side += 2) {
            for (float u = -3; u > -15; u -= 0.4F) {
                float fade = 1 - (-3 - u) / 12;
                float v = side * 1.4F * Mth.sin(u * 0.9F + t * 7) * (0.6F + 0.4F * fade);
                boolean pulse = Mth.sin(u * 0.6F + t * 9) > 0.85F;
                f.dot(graphics, u, v, PixelArt.argb(pulse ? 0xFFD8F0 : side < 0 ? 0xF07AC0 : 0xC04A98, (pulse ? 0.95F : 0.75F) * fade));
            }
        }
    }

    /** The leaf's vine: a stem curving away behind it, curling up at its end round a small bud. */
    private static void vine(GuiGraphicsExtractor graphics, Frame f, float t) {
        float sway = Mth.sin(t * 1.7F) * 0.3F;
        for (float u = -3; u > -11; u -= 0.35F) {
            float v = 1.5F * Mth.sin((u + 3) * 0.7F + 0.4F + sway);
            f.dot(graphics, u, v, PixelArt.argb(0x0E1E0A, 0.8F));
            f.dot(graphics, u, v - 1, PixelArt.argb((int) (u * 3) % 2 == 0 ? 0x6FA83E : 0x4E8A32, 1));
        }
        // The curl at its end.
        float endV = 1.5F * Mth.sin(-8 * 0.7F + 0.4F + sway) - 1;
        for (int i = 0; i < 9; i++) {
            float a = i * 0.7F;
            float r = 2.4F - i * 0.22F;
            f.dot(graphics, -11.5F - Mth.cos(a) * r, endV + Mth.sin(a) * r, PixelArt.argb(0x6FA83E, 0.9F));
        }
        f.dot(graphics, -9, endV - 1.5F, PixelArt.argb(0xD8F070, 1));
    }

    // ---- The body -------------------------------------------------------------------------------------------------

    /** Fills the pixels a shader-like function gives a colour (argb, 0 = nothing) in the arrow's box. */
    private static void paint(GuiGraphicsExtractor graphics, Frame f, java.util.function.BiFunction<Float, Float, Integer> colour) {
        int x0 = Mth.floor(f.cx()) - HALF - 4;
        int y0 = Mth.floor(f.cy()) - HALF - 4;
        int n = HALF * 2 + 9;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float dx = x0 + i + 0.5F - f.cx();
                float dy = y0 + j + 0.5F - f.cy();
                int argb = colour.apply(dx * f.ux() + dy * f.uy(), -dx * f.uy() + dy * f.ux());
                if (argb != 0) {
                    PixelArt.fill(graphics, x0 + i, y0 + j, x0 + i + 1, y0 + j + 1, argb);
                }
            }
        }
    }

    /** The arrow's solid shape: filled, outlined in its branch's ink, edges lit on the top left and shaded below. */
    private static void body(GuiGraphicsExtractor graphics, Region region, Frame f, boolean lit, float t, float anger) {
        Shape shape = shape(region, f, t, anger);
        int ink = ink(region);
        // The Void's thorn catches no light: its edges glow with its own anger instead.
        int edgeLight = region == Region.VOID ? PixelArt.mix(Blight.GLOW, 0xFFB0D0, anger * 0.6F) : 0xFFFFFF;
        int n = HALF * 2 + 3;
        int x0 = Mth.floor(f.cx()) - HALF - 1;
        int y0 = Mth.floor(f.cy()) - HALF - 1;
        int[] grid = new int[n * n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float dx = x0 + i + 0.5F - f.cx();
                float dy = y0 + j + 0.5F - f.cy();
                grid[j * n + i] = shape.at(dx * f.ux() + dy * f.uy(), -dx * f.uy() + dy * f.ux());
            }
        }
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int color = grid[j * n + i];
                boolean left = filled(grid, n, i - 1, j);
                boolean right = filled(grid, n, i + 1, j);
                boolean up = filled(grid, n, i, j - 1);
                boolean down = filled(grid, n, i, j + 1);
                int argb;
                if (color < 0) {
                    if (!(left || right || up || down)) {
                        continue;
                    }
                    argb = PixelArt.argb(ink, 0.9F);
                } else {
                    if (!up || !left) {
                        color = PixelArt.mix(color, edgeLight, region == Region.VOID ? 0.55F : 0.3F);
                    } else if (!down || !right) {
                        color = PixelArt.mix(color, ink, 0.3F);
                    }
                    if (lit) {
                        color = PixelArt.mix(color, 0xFFFFFF, 0.25F);
                    }
                    argb = 0xFF000000 | color;
                }
                PixelArt.fill(graphics, x0 + i, y0 + j, x0 + i + 1, y0 + j + 1, argb);
            }
        }
    }

    private static boolean filled(int[] grid, int n, int i, int j) {
        return i >= 0 && j >= 0 && i < n && j < n && grid[j * n + i] >= 0;
    }

    private static int ink(Region region) {
        return switch (region) {
            case WORKSHOP -> 0x1E140A;
            case ESSENCES -> 0x062A22;
            case RUNES -> 0x140A24;
            case CIRCLE -> 0x061226;
            case PYLONS -> 0x1E0A18;
            case FORGE -> 0x200806;
            case CHEMTECH -> 0x0E1E0A;
            case VOID -> 0x050008;
        };
    }

    private static Shape shape(Region region, Frame f, float t, float anger) {
        boolean plusLit = f.plusLit();
        return switch (region) {
            case WORKSHOP -> (u, v) -> needle(u, v, plusLit);
            case ESSENCES -> (u, v) -> drop(u, v, f, t);
            case RUNES -> (u, v) -> rune(u, v, t);
            case CIRCLE -> (u, v) -> crystal(u, v, t, plusLit);
            case PYLONS -> (u, v) -> claw(u, v, plusLit);
            case FORGE -> (u, v) -> spearhead(u, v, t);
            case CHEMTECH -> (u, v) -> leaf(u, v, plusLit);
            case VOID -> (u, v) -> thorn(u, v, anger, plusLit);
        };
    }

    /** A compass needle: brass towards its step, blued steel behind, split along its ridge; a rivet on its pivot. */
    private static int needle(float u, float v, boolean plusLit) {
        float pivot = com.pockyl.hextale.port.PortMath.length(u + 0.5F, v);
        if (pivot <= 1.0F) {
            return 0xFFF0B8;
        }
        if (pivot <= 2.1F) {
            return 0x7A5620;
        }
        float half = u >= 0 ? 3.6F * (1 - u / 8.8F) : 3.6F * (1 + u / 6.8F);
        if (half <= 0 || Math.abs(v) > half) {
            return -1;
        }
        boolean light = v > 0 == plusLit;
        if (u >= 0) {
            return light ? 0xF4D78A : 0xC09040;
        }
        return light ? 0x7A8AAE : 0x46526E;
    }

    /** A drop of essence falling point first: round glassy body, a highlight, a swirl turning inside. */
    private static int drop(float u, float v, Frame f, float t) {
        float bu = u + 1.5F;
        float r = 4.6F;
        boolean inside = com.pockyl.hextale.port.PortMath.length(bu, v) <= r || bu >= 0 && u <= 8.2F && Math.abs(v) <= r * (float) Math.pow((8.2F - u) / 9.7F, 1.15F);
        if (!inside) {
            return -1;
        }
        // Lit from the top left of the screen, whichever way the drop falls.
        float ox = bu * f.ux() - v * f.uy();
        float oy = bu * f.uy() + v * f.ux();
        if (com.pockyl.hextale.port.PortMath.length(ox + 2, oy + 2) <= 1.1F) {
            return 0xF0FFF8;
        }
        float light = Mth.clamp(-(ox + oy) / 6.5F, -1, 1);
        int color = PixelArt.mix(0x1C7A62, 0x7FF0C8, 0.5F + 0.5F * light);
        if (Mth.sin(bu * 1.4F + v * 0.8F - t * 4) > 0.86F) {
            color = PixelArt.mix(color, 0xC8FFEA, 0.4F);
        }
        return color;
    }

    /** How far (u, v) lies from the rune arrow's strokes: two chevrons and a dot behind them. */
    private static float runeDistance(float u, float v) {
        float best = com.pockyl.hextale.port.PortMath.length(u + 7.6F, v) - 0.3F;
        for (int k = 0; k < 2; k++) {
            float tip = 7.5F - k * 5.6F;
            float arm = 4.6F - k * 0.8F;
            float d = Math.min(segment(u, v, tip - 4.6F, -arm, tip, 0), segment(u, v, tip - 4.6F, arm, tip, 0));
            best = Math.min(best, d + k * 0.25F);
        }
        return best;
    }

    /** Two chevrons of rune ink and a dot: a light runs through the strokes from the dot to the tip, again and again. */
    private static int rune(float u, float v, float t) {
        if (runeDistance(u, v) > 1.2F) {
            return -1;
        }
        float wave = Mth.sin(u * 0.9F - t * 5);
        if (wave > 0.7F) {
            return 0xF0E4FF;
        }
        return u > 2 ? 0xB98CFF : 0x9A6AE0;
    }

    /** A cut hextech crystal: a bright ridge, a lit and a shaded facet, a glint running from its foot to its point. */
    private static int crystal(float u, float v, float t, boolean plusLit) {
        float half = Math.min(3.7F, Math.min((8.4F - u) * 0.62F, (u + 6.4F) * 0.95F));
        if (half <= 0 || Math.abs(v) > half) {
            return -1;
        }
        float glint = (t * 0.7F) % 1.6F * 15 - 7;
        if (Math.abs(v) <= 0.55F) {
            return Math.abs(u - glint) < 1.2F ? 0xFFFFFF : 0xD8F4FF;
        }
        if (Math.abs(u - glint) < 0.7F) {
            return 0xE8FCFF;
        }
        boolean back = u < -2.5F;
        return v > 0 == plusLit ? (back ? 0x6AAEE8 : 0x9FD8FF) : (back ? 0x1E4E9A : 0x2E6FC0);
    }

    /** A pylon's claw: two curved brass prongs with glowing tips closing round a pink gem, a collar behind. */
    private static int claw(float u, float v, boolean plusLit) {
        float gem = com.pockyl.hextale.port.PortMath.length(u - 1.6F, v);
        if (gem <= 2.1F) {
            if (com.pockyl.hextale.port.PortMath.length(u - 1.1F, v + (plusLit ? 0.6F : -0.6F)) <= 0.7F) {
                return 0xFFE8F6;
            }
            return v > 0 == plusLit ? 0xF8A0D8 : 0xC04A98;
        }
        if (u >= -3.6F && u <= -1.2F && Math.abs(v) <= 1.7F) {
            return 0xA07A38;
        }
        for (int side = -1; side <= 1; side += 2) {
            // A quadratic curve from the collar out and round to the tip.
            float best = Float.MAX_VALUE;
            float along = 0;
            for (int i = 0; i <= 14; i++) {
                float s = i / 14.0F;
                float cu = (1 - s) * (1 - s) * -1.6F + 2 * (1 - s) * s * 4.4F + s * s * 8.2F;
                float cv = side * ((1 - s) * (1 - s) * 4.6F + 2 * (1 - s) * s * 5.0F + s * s * 0.9F);
                float d = com.pockyl.hextale.port.PortMath.length(u - cu, v - cv);
                if (d < best) {
                    best = d;
                    along = s;
                }
            }
            if (best <= 1.3F - 0.55F * along) {
                if (along > 0.78F) {
                    return 0xFFB0E0;
                }
                return v > 0 == plusLit ? 0xF0D090 : 0xB89048;
            }
        }
        return -1;
    }

    /** A spearhead fresh from the forge: white-hot along its fuller, orange to dark red at its edges, on a dark socket. */
    private static int spearhead(float u, float v, float t) {
        float av = Math.abs(v);
        if (u >= -1.5F && u <= 8.8F) {
            float half = u >= 1 ? 3.5F * (8.8F - u) / 7.8F : 1.5F + 2.0F * (u + 1.5F) / 2.5F;
            if (av <= half) {
                float edge = av / Math.max(0.5F, half);
                int hot = PixelArt.mix(0xFFF0B0, 0xFF8A3A, Mth.clamp(edge * 1.3F, 0, 1));
                if (edge > 0.7F) {
                    hot = PixelArt.mix(hot, 0xA0281A, Math.min(1, (edge - 0.7F) * 3));
                }
                float flicker = 0.5F + 0.5F * Mth.sin(t * 9 + u * 0.7F) * Mth.sin(t * 4.3F);
                return PixelArt.mix(hot, 0xFFFFFF, 0.25F * flicker * (1 - edge));
            }
        }
        if (u >= -2.6F && u < -1.5F && av <= 2.2F) {
            return 0x8A3A1E;
        }
        if (u >= -6.5F && u < -2.6F && av <= 1.3F) {
            float glow = Mth.clamp((u + 6.5F) / 4, 0, 1);
            return PixelArt.mix(0x3A2420, 0xB8401A, glow * glow);
        }
        return -1;
    }

    /** A leaf pointing the way: a light and a shaded half, a pale midrib, side veins. */
    private static int leaf(float u, float v, boolean plusLit) {
        float length = 11.6F;
        float s = (u + 3.2F) / length;
        if (s < 0 || s > 1) {
            return -1;
        }
        float half = 4.4F * Mth.sin(Mth.PI * (float) Math.pow(s, 0.8F));
        float av = Math.abs(v);
        if (av > half) {
            return -1;
        }
        if (av <= 0.5F && s < 0.9F) {
            return 0xD8F070;
        }
        boolean light = v > 0 == plusLit;
        float vein = (s * length - av * 1.1F + 20) % 2.6F;
        if (vein < 0.6F && av < half - 0.7F) {
            return light ? 0xC8F49A : 0x7FB04A;
        }
        return light ? 0x9FE070 : 0x4E8A32;
    }

    /**
     * A thorn of the Void: a near-black claw curving to its point, two small hooks on its back, ragged, an angry crack
     * glowing down its length and its edges lit magenta.
     */
    private static int thorn(float u, float v, float anger, boolean plusLit) {
        float s = (8.8F - u) / 15.0F;
        if (s < 0 || s > 1) {
            return -1;
        }
        // The spine bows to one side as it nears the root, so the thorn curves like a claw.
        float spine = 2.0F * s * s;
        float w = v - spine;
        float half = 3.1F * (float) Math.pow(s, 0.9F) + 0.3F * Mth.sin(u * 3.7F) * Mth.sin(u * 1.9F + 1);
        boolean inside = Math.abs(w) <= half;
        for (float at : new float[]{0.42F, 0.72F}) {
            // A hook on the convex back, swept towards the root.
            float root = 8.8F - at * 15.0F;
            float back = root - 2.6F;
            if (u >= back && u <= root && w < 0) {
                float out = 3.1F * (float) Math.pow(at, 0.9F) + 2.2F * (u - back) / 2.6F;
                if (-w <= out) {
                    inside = true;
                }
            }
        }
        if (!inside) {
            return -1;
        }
        float crack = spine + 0.8F * Mth.sin(u * 1.7F + 1.3F) * Math.min(1, s * 3);
        if (u > -5.2F && u < 7 && Math.abs(v - crack) < 0.6F) {
            return PixelArt.mix(Blight.GLOW, 0xFFB0D0, anger * 0.8F);
        }
        if (s < 0.18F) {
            return 0x1A0A16;
        }
        return w > 0 == plusLit ? 0x5A2048 : 0x2A1024;
    }

    private static float segment(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float k = Mth.clamp(((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy), 0, 1);
        return com.pockyl.hextale.port.PortMath.length(px - ax - dx * k, py - ay - dy * k);
    }
}
