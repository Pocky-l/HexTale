package com.pockyl.hextale.client.screen.book;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.quest.Quest;

/**
 * The effects around important steps of the guide, drawn in pixels behind the frames: a golden halo for key steps, arcs
 * of lightning jumping off storm steps, blue sparks circling refined hextech, dark smoke creeping out of cursed ones,
 * embers over heat and green motes over life. Everything runs on the wall clock and each step's index, so neighbours
 * never move in step.
 */
final class Auras {
    private static final int GOLD = 0xE0B85A;
    private static final int ARC = 0x9FEFFF;
    private static final int HEXTECH = 0x5CC8F2;
    private static final int SMOKE = 0x1A0A26;
    private static final int CURSE = 0x7A3AB0;
    private static final int EMBER = 0xFFB02E;
    private static final int LIFE = 0x8FE07A;

    private Auras() {
    }

    static void draw(GuiGraphicsExtractor graphics, Quest quest, int size, long now) {
        int x = quest.x();
        int y = quest.y();
        int seed = quest.index() * 7919;
        switch (quest.aura()) {
            case NONE -> {
            }
            case GOLD -> gold(graphics, x, y, size, now, seed);
            case STORM -> storm(graphics, x, y, size, now, seed);
            case HEXTECH -> hextech(graphics, x, y, size, now, seed);
            case CURSED -> cursed(graphics, x, y, size, now, seed);
            case FLAME -> flame(graphics, x, y, size, now, seed);
            case LIFE -> life(graphics, x, y, size, now, seed);
        }
    }

    /** Slow golden halo rings and six motes circling: a key step of the path. */
    private static void gold(GuiGraphicsExtractor graphics, int x, int y, int size, long now, int seed) {
        float pulse = 0.5F + 0.5F * Mth.sin(now / 700.0F + seed);
        for (int ring = 0; ring < 4; ring++) {
            PixelArt.hex(graphics, x, y, size + 6 + ring * 5, PixelArt.argb(GOLD, (0.13F - ring * 0.028F) * (0.5F + 0.5F * pulse)), 0);
        }
        float radius = size * 0.85F;
        for (int i = 0; i < 6; i++) {
            float angle = now / 1400.0F + i * Mth.TWO_PI / 6;
            int px = x + Math.round(Mth.cos(angle) * radius);
            int py = y + Math.round(Mth.sin(angle) * radius * 0.8F);
            float twinkle = 0.5F + 0.5F * Mth.sin(now / 250.0F + i * 1.3F + seed);
            PixelArt.fill(graphics, px, py, px + 1, py + 1, PixelArt.argb(0xFFF0C0, 0.4F + 0.6F * twinkle));
            if (twinkle > 0.8F) {
                PixelArt.fill(graphics, px - 1, py, px + 2, py + 1, PixelArt.argb(GOLD, 0.4F));
                PixelArt.fill(graphics, px, py - 1, px + 1, py + 2, PixelArt.argb(GOLD, 0.4F));
            }
        }
    }

    /** Arcs of lightning: a few jagged bolts leave the frame, change every few frames and sometimes skip a beat. */
    private static void storm(GuiGraphicsExtractor graphics, int x, int y, int size, long now, int seed) {
        long bucket = now / 90;
        float glow = 0.5F + 0.5F * Mth.sin(now / 160.0F + seed);
        PixelArt.hex(graphics, x, y, size + 6, PixelArt.argb(HEXTECH, 0.06F + 0.06F * glow), 0);
        for (int k = 0; k < 3; k++) {
            long h = hash(bucket * 31 + k * 977 + seed);
            if (h % 4 == 0) {
                continue;
            }
            float angle = (h % 3600) / 3600.0F * Mth.TWO_PI;
            float r = size / 2.0F;
            float px = x + Mth.cos(angle) * r;
            float py = y + Mth.sin(angle) * r;
            int segments = 3 + (int) (h >> 8 & 1);
            for (int s = 0; s < segments; s++) {
                float out = 3 + (h >> (10 + s) & 3);
                float jitter = ((h >> (14 + s * 2) & 7) - 3.5F) * 0.8F;
                float nx = px + Mth.cos(angle) * out - Mth.sin(angle) * jitter;
                float ny = py + Mth.sin(angle) * out + Mth.cos(angle) * jitter;
                int a = PixelArt.argb(ARC, 0.9F - s * 0.18F);
                PixelArt.line(graphics, Math.round(px), Math.round(py), Math.round(nx), Math.round(ny), a);
                PixelArt.line(graphics, Math.round(px) + 1, Math.round(py), Math.round(nx) + 1, Math.round(ny), PixelArt.argb(HEXTECH, 0.25F));
                px = nx;
                py = ny;
            }
            PixelArt.fill(graphics, Math.round(px), Math.round(py), Math.round(px) + 1, Math.round(py) + 1, PixelArt.argb(0xFFFFFF, 0.9F));
        }
    }

    /** Blue sparks on a tilted orbit, each with a short fading tail. */
    private static void hextech(GuiGraphicsExtractor graphics, int x, int y, int size, long now, int seed) {
        float rx = size * 0.82F;
        float ry = size * 0.45F;
        for (int i = 0; i < 5; i++) {
            float base = now / 900.0F + i * Mth.TWO_PI / 5 + seed;
            for (int t = 0; t < 4; t++) {
                float angle = base - t * 0.12F;
                int px = x + Math.round(Mth.cos(angle) * rx);
                int py = y + Math.round(Mth.sin(angle) * ry);
                int color = t == 0 ? PixelArt.mix(HEXTECH, 0xFFFFFF, 0.5F) : HEXTECH;
                PixelArt.fill(graphics, px, py, px + 1, py + 1, PixelArt.argb(color, 0.9F - t * 0.22F));
            }
        }
        PixelArt.hex(graphics, x, y, size + 4, PixelArt.argb(HEXTECH, 0.08F), 0);
    }

    /** Dark smoke puffs creeping out and up, with a sickly violet flicker at the rim. */
    private static void cursed(GuiGraphicsExtractor graphics, int x, int y, int size, long now, int seed) {
        for (int i = 0; i < 8; i++) {
            float period = 2400 + (hash(seed + i) % 900);
            float t = ((now + i * 377L) % (long) period) / period;
            float angle = (hash(seed * 3 + i) % 628) / 100.0F;
            float r = size / 2.0F + t * 7;
            int px = x + Math.round(Mth.cos(angle) * r);
            int py = y + Math.round(Mth.sin(angle) * r - t * 6);
            int radius = 2 + Math.round(t * 3);
            disc(graphics, px, py, radius, PixelArt.argb(PixelArt.mix(SMOKE, CURSE, 0.25F * (1 - t)), 0.55F * (1 - t)));
        }
        float flicker = 0.5F + 0.5F * Mth.sin(now / 130.0F + seed) * Mth.sin(now / 370.0F);
        PixelArt.hexRing(graphics, x, y, size + 3, PixelArt.argb(CURSE, 0.15F + 0.3F * flicker));
        PixelArt.hex(graphics, x, y, size + 10, PixelArt.argb(SMOKE, 0.25F), 0);
    }

    /** Embers rising from below the frame, swaying and dying out. */
    private static void flame(GuiGraphicsExtractor graphics, int x, int y, int size, long now, int seed) {
        for (int i = 0; i < 7; i++) {
            float period = 1300 + (hash(seed + i * 13) % 700);
            float t = ((now + i * 211L) % (long) period) / period;
            int spread = size / 2 + 2;
            int px = x - spread + (int) (hash(seed + i * 5) % (spread * 2)) + Math.round(Mth.sin(t * 6 + i) * 2);
            int py = y + size / 2 - Math.round(t * (size + 6));
            int color = PixelArt.mix(EMBER, 0xFF4A1C, t);
            PixelArt.fill(graphics, px, py, px + 1, py + 1, PixelArt.argb(color, 1 - t));
        }
        float glow = 0.5F + 0.5F * Mth.sin(now / 300.0F + seed);
        PixelArt.hex(graphics, x, y + 2, size + 6, PixelArt.argb(0xFF7A1C, 0.06F + 0.05F * glow), 0);
    }

    /** Green motes drifting up with a gentle sway. */
    private static void life(GuiGraphicsExtractor graphics, int x, int y, int size, long now, int seed) {
        for (int i = 0; i < 5; i++) {
            float period = 2600 + (hash(seed + i * 17) % 900);
            float t = ((now + i * 523L) % (long) period) / period;
            int px = x - size / 2 + (int) (hash(seed + i * 7) % size) + Math.round(Mth.sin(t * 5 + i) * 3);
            int py = y + size / 3 - Math.round(t * (size + 4));
            PixelArt.fill(graphics, px, py, px + 1, py + 1, PixelArt.argb(LIFE, 0.9F * Mth.sin(t * Mth.PI)));
        }
    }

    private static void disc(GuiGraphicsExtractor graphics, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.floor(Math.sqrt(r * r - dy * dy + 0.5));
            PixelArt.fill(graphics, cx - half, cy + dy, cx + half + 1, cy + dy + 1, argb);
        }
    }

    private static long hash(long value) {
        long x = value * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        return Math.abs(x);
    }
}
