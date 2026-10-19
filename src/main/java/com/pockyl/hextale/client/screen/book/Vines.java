package com.pockyl.hextale.client.screen.book;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.Region;

/**
 * The chemtech half of the guide grows instead of being built: its medallions are wreathed in a vine with leaves,
 * small flowers and a drop of dew, and its steps hang from their parents by vines with leaves and buds instead of brass
 * lines. The deeper a step lies in the chemtech branch, the more the greenery wilts: leaves brown and fall, flowers go,
 * rust creeps over the bronze and smog rises from it. A step where hextech and chemtech meet wears half a wreath.
 */
final class Vines {
    /** The bronze of a chemtech medallion, gone a little green. */
    static final int BRONZE = 0x8A8A4A;
    private static final int VINE_LIGHT = 0x6FA83E;
    private static final int VINE_DARK = 0x3E6A2A;
    private static final int LEAF = 0x7FD05A;
    private static final int LEAF_LIGHT = 0xB8EE88;
    private static final int WILTED = 0x7A6A3A;
    private static final int WILTED_DARK = 0x4A3E22;
    private static final int RUST = 0x9A4A26;
    private static final int SMOG = 0x5A5650;
    private static final int[] PETALS = {0xF4A0C0, 0xF4F0E0, 0xC8A8F0};
    private static final int POLLEN = 0xFFD54A;
    private static final int DEW = 0xD8F8FF;
    private static final int BUD = 0xD8F070;
    /** Chemtech steps this deep in their branch are fully wilted. */
    private static final int DEEPEST = 6;

    enum Frame {
        NONE,
        FULL,
        HALF
    }

    private Vines() {
    }

    /** A full wreath on chemtech steps, half a wreath where a step needs both a hextech and a chemtech parent. */
    static Frame frame(Quest quest) {
        boolean chemtech = quest.region() == Region.CHEMTECH;
        boolean fromChemtech = false;
        boolean fromHextech = false;
        for (Quest parent : quest.parents()) {
            if (parent.region() == Region.CHEMTECH) {
                fromChemtech = true;
            } else {
                fromHextech = true;
            }
        }
        if (fromChemtech && fromHextech) {
            return Frame.HALF;
        }
        return chemtech ? Frame.FULL : Frame.NONE;
    }

    /** How wilted a chemtech step's greenery is: 0 at the branch's root, 1 this many steps further. */
    static float decay(Quest quest) {
        int depth = 0;
        for (Quest at = quest.parent(); at != null && at.region() == Region.CHEMTECH && depth < DEEPEST; at = at.parent()) {
            depth++;
        }
        return depth / (float) DEEPEST;
    }

    /** The wreath around a medallion; {@code half} keeps only its left half. */
    static void ring(GuiGraphicsExtractor graphics, int x, int y, int size, boolean half, float decay, long now, int seed) {
        int light = PixelArt.mix(VINE_LIGHT, WILTED, decay);
        int dark = PixelArt.mix(VINE_DARK, WILTED_DARK, decay);
        // The stem winds around the rim, its two strands twisting over each other.
        PixelArt.Circle circle = PixelArt.Circle.of(size + 3);
        for (int i = 0; i < circle.pixels.length; i++) {
            float a = circle.angles[i];
            if (half && Mth.cos(a) > 0) {
                continue;
            }
            int[] p = circle.pixels[i];
            int color = Mth.sin(a * 9 + seed) > 0 ? light : dark;
            PixelArt.fill(graphics, x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, PixelArt.argb(color, 1));
        }
        float radius = (size + 3) / 2.0F;
        int leaves = half ? 4 : 7;
        float spread = half ? Mth.PI : Mth.TWO_PI;
        float from = half ? Mth.HALF_PI : seed * 0.37F;
        for (int i = 0; i < leaves; i++) {
            // Wilting: the leaves brown first, then fall one by one.
            if (i < Math.round(leaves * Math.max(0, decay - 0.4F) / 0.6F)) {
                continue;
            }
            float a = from + (i + 0.5F) * spread / leaves + Mth.sin(now / 900.0F + i * 1.7F + seed) * 0.05F;
            leaf(graphics, x + Mth.cos(a) * radius, y + Mth.sin(a) * radius, a + (i % 2 == 0 ? 0.6F : -0.6F),
                    PixelArt.mix(LEAF, WILTED, decay), PixelArt.mix(LEAF_LIGHT, WILTED, decay));
        }
        if (decay < 0.5F) {
            for (int f = 0; f < (half ? 1 : 2); f++) {
                float a = from + (f * 0.5F + 0.3F) * spread;
                flower(graphics, Math.round(x + Mth.cos(a) * (radius + 1)), Math.round(y + Mth.sin(a) * (radius + 1)),
                        PETALS[Math.floorMod(seed + f, PETALS.length)], 1 - decay * 2);
            }
        }
        if (decay < 0.4F) {
            // A drop of dew on the stem, catching the light now and then.
            float a = from + 0.62F * spread;
            int dx = Math.round(x + Mth.cos(a) * radius);
            int dy = Math.round(y + Mth.sin(a) * radius) + 1;
            float glint = Mth.clamp(Mth.sin(now / 1100.0F + seed) * 3 - 2, 0, 1);
            PixelArt.fill(graphics, dx, dy, dx + 1, dy + 1, PixelArt.argb(DEW, 0.75F + 0.25F * glint));
            if (glint > 0) {
                PixelArt.fill(graphics, dx - 1, dy, dx + 2, dy + 1, PixelArt.argb(0xFFFFFF, 0.5F * glint));
                PixelArt.fill(graphics, dx, dy - 1, dx + 1, dy + 2, PixelArt.argb(0xFFFFFF, 0.5F * glint));
            }
        }
        if (decay > 0.3F) {
            // Rust creeping over the bronze.
            PixelArt.Circle rim = PixelArt.Circle.of(size);
            for (int i = 0; i < rim.pixels.length; i++) {
                float n = Mth.sin(i * 12.9898F + seed) * 43758.547F;
                if (n - Mth.floor(n) < (decay - 0.3F) * 0.6F) {
                    int[] p = rim.pixels[i];
                    PixelArt.fill(graphics, x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, PixelArt.argb(RUST, 0.85F));
                }
            }
        }
        if (decay > 0.6F) {
            // Smog: slow grey puffs rising from the medallion and thinning out.
            for (int k = 0; k < 3; k++) {
                float t = ((now / 2600.0F) + k / 3.0F + seed * 0.13F) % 1;
                int px = x + Math.round(Mth.sin(t * 5 + k * 2.1F) * size * 0.3F);
                int py = y - Math.round(size * 0.4F + t * size * 0.8F);
                PixelArt.disc(graphics, px, py, 5 + Math.round(t * 6), PixelArt.argb(SMOG, 0.3F * (decay - 0.6F) / 0.4F * (1 - t)), 0);
            }
        } else if (decay < 0.3F) {
            // Pollen drifting off the young greenery.
            for (int k = 0; k < 3; k++) {
                float t = ((now / 3400.0F) + k / 3.0F + seed * 0.29F) % 1;
                float a = from + k * 2.1F + t * 0.8F;
                int px = x + Math.round(Mth.cos(a) * (radius + 2 + t * 10));
                int py = y + Math.round(Mth.sin(a) * (radius + 2 + t * 10) - t * 6);
                PixelArt.fill(graphics, px, py, px + 1, py + 1, PixelArt.argb(POLLEN, 0.8F * (1 - t)));
            }
        }
    }

    /** A small leaf of four pixels pointing along {@code angle}, lit along its upper edge. */
    private static void leaf(GuiGraphicsExtractor graphics, float bx, float by, float angle, int color, int light) {
        float ux = Mth.cos(angle);
        float uy = Mth.sin(angle);
        int x0 = Math.round(bx + ux);
        int y0 = Math.round(by + uy);
        int x1 = Math.round(bx + ux * 2.5F);
        int y1 = Math.round(by + uy * 2.5F);
        int x2 = Math.round(bx + ux * 2 - uy);
        int y2 = Math.round(by + uy * 2 + ux);
        int x3 = Math.round(bx + ux * 3.5F);
        int y3 = Math.round(by + uy * 3.5F);
        PixelArt.fill(graphics, x0, y0, x0 + 1, y0 + 1, PixelArt.argb(color, 1));
        PixelArt.fill(graphics, x2, y2, x2 + 1, y2 + 1, PixelArt.argb(color, 1));
        PixelArt.fill(graphics, x1, y1, x1 + 1, y1 + 1, PixelArt.argb(light, 1));
        PixelArt.fill(graphics, x3, y3, x3 + 1, y3 + 1, PixelArt.argb(color, 1));
    }

    /** A tiny flower: four petals around a pollen heart. */
    private static void flower(GuiGraphicsExtractor graphics, int x, int y, int petals, float alpha) {
        int petal = PixelArt.argb(petals, alpha);
        PixelArt.fill(graphics, x - 1, y, x, y + 1, petal);
        PixelArt.fill(graphics, x + 1, y, x + 2, y + 1, petal);
        PixelArt.fill(graphics, x, y - 1, x + 1, y, petal);
        PixelArt.fill(graphics, x, y + 1, x + 1, y + 2, petal);
        PixelArt.fill(graphics, x, y, x + 1, y + 1, PixelArt.argb(POLLEN, alpha));
    }

    /**
     * A vine from a parent to a chemtech step along the link's pixels: two strands twisting, a leaf every few pixels on
     * alternating sides; an open link grows a bud that creeps along it.
     */
    static void link(GuiGraphicsExtractor graphics, int[][] path, boolean lit, float decay, long now, int seed) {
        int light = PixelArt.mix(VINE_LIGHT, WILTED, decay);
        int dark = PixelArt.mix(VINE_DARK, WILTED_DARK, decay);
        float alpha = lit ? 0.9F : 0.6F;
        for (int i = 0; i < path.length; i++) {
            int[] p = path[i];
            int color = (i / 3) % 2 == 0 ? light : dark;
            PixelArt.fill(graphics, p[0], p[1], p[0] + 1, p[1] + 1, PixelArt.argb(color, alpha));
        }
        int leafColor = PixelArt.mix(LEAF, WILTED, decay);
        int leafLight = PixelArt.mix(LEAF_LIGHT, WILTED, decay);
        for (int i = 6; i + 2 < path.length; i += 9) {
            // Wilted vines keep fewer leaves.
            if (decay > 0.4F && (i / 9 + seed) % 3 < Math.round((decay - 0.4F) * 5)) {
                continue;
            }
            int[] a = path[i - 2];
            int[] b = path[i + 2];
            float dx = b[0] - a[0];
            float dy = b[1] - a[1];
            float length = Math.max(1, Mth.sqrt(dx * dx + dy * dy));
            float side = (i / 9) % 2 == 0 ? 1 : -1;
            float sway = Mth.sin(now / 1000.0F + i * 0.7F + seed) * 0.1F;
            // Out to the side and a little forward, the way the vine grows.
            float ox = -dy / length * side + dx / length * 0.6F;
            float oy = dx / length * side + dy / length * 0.6F;
            leaf(graphics, path[i][0], path[i][1], (float) Math.atan2(oy, ox) + sway, leafColor, leafLight);
        }
        if (!lit) {
            // A bud creeping along the open vine, glowing faintly.
            float t = (now % 2400) / 2400.0F;
            int[] p = path[Math.min(path.length - 1, (int) (t * path.length))];
            float fade = 1 - t * 0.6F;
            PixelArt.fill(graphics, p[0] - 1, p[1], p[0] + 2, p[1] + 1, PixelArt.argb(BUD, 0.4F * fade));
            PixelArt.fill(graphics, p[0], p[1] - 1, p[0] + 1, p[1] + 2, PixelArt.argb(BUD, 0.4F * fade));
            PixelArt.fill(graphics, p[0], p[1], p[0] + 1, p[1] + 1, PixelArt.argb(0xF8FFD0, 0.9F * fade));
        }
    }
}
