package com.pockyl.hextale.client.screen.book;

import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import com.pockyl.hextale.client.render.DiscStack;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.Region;

/**
 * The Void's branch of the guide is sick, not built: its medallions are dark and cracked, the cracks glowing an angry
 * magenta that flares up out of rhythm, thorns writhe around the rim, dark smoke seeps off them and the sky around them
 * is a little darker than elsewhere. Its steps hang from each other by veins with a slow throb running down them.
 */
final class Blight {
    /** The rim of a done step: a dark plum, a little brighter on the key ones. */
    static final int RIM = 0x6A2E58;
    static final int RIM_KEY = 0x9A3E78;
    /** The face of a done step: nearly black. */
    static final int FILL = 0x140812;
    /** What glows through the cracks: the Void's magenta. */
    static final int GLOW = 0xE0408C;
    /** The colour of an open step's breath and of its pulsing rim. */
    static final int ANGRY = 0xD0306A;
    private static final int ANGRY_HOT = 0xFF6AA8;
    private static final int THORN = 0x2A1024;
    private static final int THORN_LIGHT = 0x5A2048;
    private static final int SMOKE = 0x0A040A;
    /** The dark aura round the branch's steps: a deep plum that still reads against the night sky. */
    private static final int AURA = 0x2C0A26;
    private static final int AURA_DEEP = 0x14040F;
    private static final int SMOKE_PLUM = 0x3A1232;
    private static final int VEIN = 0x3A1630;
    private static final int VEIN_LIGHT = 0x7A2A5C;

    private Blight() {
    }

    static boolean of(Quest quest) {
        return quest.region() == Region.VOID;
    }

    /**
     * How angry the Void is at this moment, 0..1: a slow seething under it and sudden flares that come out of rhythm, each
     * step at its own time.
     */
    static float anger(long now, int seed) {
        float seethe = 0.35F + 0.25F * Mth.sin(now / 900.0F + seed) * Mth.sin(now / 370.0F + seed * 0.7F);
        long beat = now / 140;
        long h = hash(beat + seed * 7919L);
        // A flare now and then: sharp to rise, a little slower to sink.
        float flare = h % 23 == 0 ? 1 : hash(beat - 1 + seed * 7919L) % 23 == 0 ? 0.55F : hash(beat - 2 + seed * 7919L) % 23 == 0 ? 0.25F : 0;
        return Mth.clamp(seethe + flare * 0.7F, 0, 1);
    }

    /**
     * Behind the medallion: a dark plum aura swallows the sky around it, its edge smouldering magenta, an angry red glow
     * throbs around the rim (flaring when the Void does) and thick dark smoke seeps off it, rising and thinning.
     */
    static void behind(GuiGraphicsExtractor graphics, int x, int y, int size, boolean done, long now, int seed) {
        float anger = anger(now, seed);
        // The aura's edge first, so the darkness inside it has a lit rim against the sky.
        PixelArt.disc(graphics, x, y, size + 44, PixelArt.argb(GLOW, 0.05F + 0.06F * anger), 0);
        PixelArt.disc(graphics, x, y, size + 40, PixelArt.argb(AURA, 0.5F), 0);
        for (int ring = 0; ring < 3; ring++) {
            PixelArt.disc(graphics, x, y, size + 30 - ring * 9, PixelArt.argb(ring == 2 ? AURA_DEEP : AURA, 0.3F + ring * 0.1F), 0);
        }
        // The edge wavers: a ragged ring of darker and brighter pixels turning slowly.
        PixelArt.Circle edge = PixelArt.Circle.of(size + 40);
        float turn = now / 3000.0F + seed;
        for (int i = 0; i < edge.pixels.length; i++) {
            float wave = Mth.sin(edge.angles[i] * 5 + turn) * Mth.sin(edge.angles[i] * 3 - turn * 1.3F);
            if (wave > 0.25F) {
                int[] p = edge.pixels[i];
                PixelArt.fill(graphics, x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, PixelArt.argb(GLOW, (0.18F + 0.25F * anger) * wave));
            }
        }
        float glow = (done ? 0.12F : 0.2F) + (done ? 0.3F : 0.5F) * anger;
        for (int ring = 0; ring < 3; ring++) {
            PixelArt.disc(graphics, x, y, size + 4 + ring * 4, PixelArt.argb(ANGRY, glow * (0.5F - ring * 0.14F)), 0);
        }
        // A ragged rim of the glow: some pixels flare further out than others, changing with each flare.
        PixelArt.Circle circle = PixelArt.Circle.of(size + 8);
        long beat = now / 140;
        for (int i = 0; i < circle.pixels.length; i++) {
            long h = hash(i * 131L + beat * 7L + seed);
            if (h % 5 < 2) {
                int[] p = circle.pixels[i];
                PixelArt.fill(graphics, x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, PixelArt.argb(ANGRY_HOT, glow * 0.6F));
            }
        }
        for (int k = 0; k < 6; k++) {
            float t = ((now / 2800.0F) + k / 6.0F + seed * 0.17F) % 1;
            int px = x + Math.round(Mth.sin(t * 4 + k * 1.9F + seed) * size * 0.4F);
            int py = y - Math.round(size * 0.25F + t * size * 1.1F);
            PixelArt.disc(graphics, px, py, 6 + Math.round(t * 9), PixelArt.argb(SMOKE_PLUM, 0.75F * (1 - t)), 0);
            if (k % 2 == 0) {
                // A spark of the Void caught in the smoke.
                PixelArt.fill(graphics, px, py, px + 1, py + 1, PixelArt.argb(GLOW, 0.8F * (1 - t) * (1 - t)));
            }
        }
    }

    /**
     * Over the medallion: cracks from the rim into the face, glowing with the step's anger, and thorns writhing out of
     * the rim. A done step's cracks smoulder; an open one's burn.
     */
    static void over(GuiGraphicsExtractor graphics, int x, int y, int size, boolean done, long now, int seed) {
        float anger = anger(now, seed);
        float heat = done ? 0.35F + 0.4F * anger : 0.55F + 0.45F * anger;
        int crack = PixelArt.mix(GLOW, ANGRY_HOT, anger * anger);
        float r = size / 2.0F;
        for (int c = 0; c < 3; c++) {
            long h = hash(seed * 31L + c * 977L);
            float angle = (h % 3600) / 3600.0F * Mth.TWO_PI;
            float px = x + Mth.cos(angle) * r;
            float py = y + Mth.sin(angle) * r;
            int segments = 2 + (int) (h >> 9 & 1);
            for (int s = 0; s < segments; s++) {
                // Inwards, zig-zagging: a fixed crack, so it reads as damage, not as lightning.
                float in = 2.5F + (h >> (11 + s) & 1);
                float jitter = ((h >> (14 + s * 2) & 3) - 1.5F) * 1.2F;
                float nx = px - Mth.cos(angle) * in - Mth.sin(angle) * jitter;
                float ny = py - Mth.sin(angle) * in + Mth.cos(angle) * jitter;
                PixelArt.line(graphics, Math.round(px), Math.round(py), Math.round(nx), Math.round(ny), PixelArt.argb(crack, heat * (1 - s * 0.25F)));
                px = nx;
                py = ny;
            }
        }
        int thorns = 8;
        for (int i = 0; i < thorns; i++) {
            float base = seed * 0.61F + i * Mth.TWO_PI / thorns;
            // Writhing: each thorn sways on its own and, when the Void flares, stretches out.
            float sway = Mth.sin(now / 700.0F + i * 1.7F + seed) * 0.35F;
            float reach = 4 + (i % 3) * 1.5F + anger * 2.5F;
            float bx = x + Mth.cos(base) * (r + 1);
            float by = y + Mth.sin(base) * (r + 1);
            float mx = bx + Mth.cos(base + sway * 0.5F) * reach * 0.55F;
            float my = by + Mth.sin(base + sway * 0.5F) * reach * 0.55F;
            float tx = bx + Mth.cos(base + sway) * reach;
            float ty = by + Mth.sin(base + sway) * reach;
            PixelArt.line(graphics, Math.round(bx), Math.round(by), Math.round(mx), Math.round(my), PixelArt.argb(THORN_LIGHT, 0.95F));
            // A thick base: the thorn grows out of the rim, not off it.
            PixelArt.line(graphics, Math.round(bx) + 1, Math.round(by), Math.round(mx) + 1, Math.round(my), PixelArt.argb(THORN, 0.95F));
            PixelArt.line(graphics, Math.round(mx), Math.round(my), Math.round(tx), Math.round(ty), PixelArt.argb(THORN, 0.95F));
            if (anger > 0.6F) {
                PixelArt.fill(graphics, Math.round(tx), Math.round(ty), Math.round(tx) + 1, Math.round(ty) + 1,
                        PixelArt.argb(ANGRY_HOT, (anger - 0.6F) / 0.4F));
            }
        }
    }

    /**
     * A vein from a parent to a step of the Void along the link's pixels: a dark core with a lighter edge and a small
     * thorn every so often; a throb of magenta runs down it towards the step, brighter into an open one.
     */
    static void link(GuiGraphicsExtractor graphics, int[][] path, boolean lit, long now, int seed) {
        float alpha = lit ? 0.85F : 0.7F;
        for (int i = 0; i < path.length; i++) {
            int[] p = path[i];
            int color = (i / 4 + seed) % 3 == 0 ? VEIN_LIGHT : VEIN;
            PixelArt.fill(graphics, p[0], p[1], p[0] + 1, p[1] + 1, PixelArt.argb(color, alpha));
        }
        for (int i = 8; i + 2 < path.length; i += 11) {
            int[] a = path[i - 2];
            int[] b = path[i + 2];
            float dx = b[0] - a[0];
            float dy = b[1] - a[1];
            float length = Math.max(1, Mth.sqrt(dx * dx + dy * dy));
            float side = (i / 11 + seed) % 2 == 0 ? 1 : -1;
            // Back along the vein and out to the side, like a thorn on a stem.
            int tx = Math.round(path[i][0] - dy / length * side * 2 - dx / length);
            int ty = Math.round(path[i][1] + dx / length * side * 2 - dy / length);
            PixelArt.line(graphics, path[i][0], path[i][1], tx, ty, PixelArt.argb(THORN_LIGHT, alpha));
        }
        // The throb: a swelling of light that runs down the vein, slow, then gone for a moment.
        float t = (now % 2200) / 1600.0F;
        if (t < 1) {
            int at = Math.min(path.length - 1, (int) (t * path.length));
            float strength = (lit ? 0.45F : 0.9F) * Mth.sin(t * Mth.PI);
            for (int k = -3; k <= 3; k++) {
                int index = at + k;
                if (index < 0 || index >= path.length) {
                    continue;
                }
                int[] p = path[index];
                float fall = 1 - Math.abs(k) / 4.0F;
                PixelArt.fill(graphics, p[0], p[1], p[0] + 1, p[1] + 1, PixelArt.argb(k == 0 ? ANGRY_HOT : GLOW, strength * fall));
            }
        }
    }

    /** The sky around the branch's steps darkens into plum, with a breath of magenta in it, instead of a coloured haze. */
    static void haze(GuiGraphicsExtractor graphics, Quest quest) {
        DiscStack.draw(graphics, quest.x(), quest.y(), HAZE_WIDTHS, HAZE_COLOURS);
    }

    /** The haze's discs, widest first: six rings of aura and smoke, then a faint glow. */
    private static final int[] HAZE_WIDTHS = new int[7];
    private static final int[] HAZE_COLOURS = new int[7];

    static {
        for (int ring = 0; ring < 6; ring++) {
            HAZE_WIDTHS[ring] = 210 - ring * 28;
            HAZE_COLOURS[ring] = PixelArt.argb(ring < 2 ? AURA : SMOKE, 0.08F);
        }
        HAZE_WIDTHS[6] = 130;
        HAZE_COLOURS[6] = PixelArt.argb(GLOW, 0.022F);
    }

    private static long hash(long value) {
        long x = value * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        return Math.abs(x);
    }
}
