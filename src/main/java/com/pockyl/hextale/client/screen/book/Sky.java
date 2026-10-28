package com.pockyl.hextale.client.screen.book;

import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import com.pockyl.hextale.port.client.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.rune.Essence;

/**
 * The night sky under the researcher's guide, drawn as the sky of a star chart kept in a diary. From the back: a deep
 * night going from blue at the top to a warm plum below; two veils of nebula drifting at their own depths (the far one
 * large and warm, so the pattern never shows its seams); a band of milky light across the sky; the researcher's chart
 * in faint gold ink - great circles with ticks turning slowly, meridians, a compass rose at the guide's heart and the
 * nine primal signs on the middle circle like a zodiac; small constellations joined by dotted lines; three layers of
 * twinkling stars, the brightest with crossed glints; a falling star now and then; warm motes of dust drifting up as in
 * lamplight; and a darker rim. The mouse leaves a short trail of star dust as it moves, gone a moment later, and the
 * stars close to it shine a little brighter.
 */
final class Sky {
    private static final ResourceLocation COSMOS = Hextale.id("textures/gui/cosmos.png");
    private static final int COSMOS_SIZE = 256;
    private static final int INK = 0xE0B85A;
    /** How far from the mouse the stars shine brighter. */
    private static final int NEAR = 40;
    /** How long a speck of the mouse's star dust lives, milliseconds. */
    private static final int DUST_LIFE = 650;
    /** The mouse's star dust: x, y, birth time and a seed per speck, oldest overwritten first. */
    private static final float[] DUST = new float[48 * 4];
    private static int dustNext;
    private static float lastMouseX = Float.NaN;
    private static float lastMouseY = Float.NaN;

    private final int x0;
    private final int y0;
    private final int w;
    private final int h;
    private final float cameraX;
    private final float cameraY;
    private final float zoom;
    private final long now;

    private Sky(int x0, int y0, int w, int h, float cameraX, float cameraY, float zoom, long now) {
        this.x0 = x0;
        this.y0 = y0;
        this.w = w;
        this.h = h;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.zoom = zoom;
        this.now = now;
    }

    /**
     * Draws the sky into the rectangle (the caller clips to it). {@code chartX/Y} is the map point the chart is centred
     * on; the mouse leaves its star dust when {@code trail} is set.
     */
    static void render(GuiGraphicsExtractor graphics, int x0, int y0, int w, int h, float cameraX, float cameraY, float zoom,
                       float chartX, float chartY, boolean trail, int mouseX, int mouseY, long now) {
        Sky sky = new Sky(x0, y0, w, h, cameraX, cameraY, zoom, now);
        sky.night(graphics);
        sky.nebula(graphics);
        sky.milkyWay(graphics);
        sky.chart(graphics, chartX, chartY);
        sky.constellations(graphics);
        sky.stars(graphics, trail, mouseX, mouseY);
        sky.fallingStar(graphics);
        sky.motes(graphics);
        sky.rim(graphics);
        sky.dust(graphics, trail, mouseX, mouseY);
    }

    /** A deep night: blue at the top, a warm plum towards the bottom. */
    private void night(GuiGraphicsExtractor graphics) {
        for (int y = 0; y < h; y += 2) {
            float t = (float) y / h;
            int color = PixelArt.mix(0x070916, 0x120914, t * t);
            PixelArt.fill(graphics, x0, y0 + y, x0 + w, y0 + Math.min(h, y + 2), 0xFF000000 | color);
        }
    }

    /**
     * Two veils of the nebula: the near one at its own size drifting with a quarter of the map's speed, the far one twice
     * as large, warmer and fainter, drifting slower, so the two never line up into a visible tile.
     */
    private void nebula(GuiGraphicsExtractor graphics) {
        float breath = 0.5F + 0.5F * Mth.sin(now / 5200.0F);
        veil(graphics, COSMOS_SIZE * 2, 0.1F, 97, PixelArt.argb(0xF0B8A0, 0.32F + 0.08F * breath));
        veil(graphics, COSMOS_SIZE, 0.25F, 0, PixelArt.argb(0xD8D0F0, 0.82F));
    }

    private void veil(GuiGraphicsExtractor graphics, int size, float speed, int shift, int color) {
        int offsetX = Math.floorMod((int) (cameraX * speed) + shift, size);
        int offsetY = Math.floorMod((int) (cameraY * speed) + shift * 2, size);
        for (int ty = -offsetY; ty < h; ty += size) {
            for (int tx = -offsetX; tx < w; tx += size) {
                PixelArt.flush(graphics);
                graphics.blit(RenderPipelines.GUI_TEXTURED, COSMOS, x0 + tx, y0 + ty, 0, 0, size, size, COSMOS_SIZE, COSMOS_SIZE,
                        COSMOS_SIZE, COSMOS_SIZE, color);
            }
        }
        PixelArt.flush(graphics);
    }

    /** A soft band of milky light falling across the sky from the top right, with its own crowd of faint stars. */
    private void milkyWay(GuiGraphicsExtractor graphics) {
        float speed = 0.15F;
        float shiftX = cameraX * speed * zoom;
        float shiftY = cameraY * speed * zoom;
        float slope = -0.55F;
        // The band's middle at each row, in layer coordinates that repeat every 900 px along x.
        for (int y = 0; y < h; y += 2) {
            float ly = y + shiftY;
            float middle = (float) (Math.floorMod((long) (ly * slope + 420 - shiftX), 900L)) - 160;
            float wave = 18 * Mth.sin(ly / 140.0F);
            for (int layer = 0; layer < 4; layer++) {
                float half = 70 - layer * 16;
                int from = Math.round(middle + wave - half);
                int to = Math.round(middle + wave + half);
                PixelArt.fill(graphics, x0 + Math.max(0, from), y0 + y, x0 + Math.min(w, to), y0 + Math.min(h, y + 2),
                        PixelArt.argb(layer == 3 ? 0xF0E0C8 : 0xC8B8E8, 0.016F));
            }
        }
        for (int i = 0; i < 140; i++) {
            long seed = i * 7919L + 31;
            float along = hash(seed) % 1400;
            float across = (float) ((hash(seed + 1) % 1000) / 1000.0 - 0.5) * (hash(seed + 2) % 3 == 0 ? 120 : 60);
            float ly = along;
            float lx = ly * slope + 420 + across + 18 * Mth.sin(ly / 140.0F);
            int sx = (int) Math.floorMod((long) (lx - shiftX), 900L) - 160;
            int sy = (int) Math.floorMod((long) (ly - shiftY), 1400L);
            if (sx < 0 || sy < 0 || sx >= w || sy >= h) {
                continue;
            }
            float twinkle = 0.6F + 0.4F * Mth.sin(now / (600.0F + hash(seed + 3) % 700) + i);
            PixelArt.fill(graphics, x0 + sx, y0 + sy, x0 + sx + 1, y0 + sy + 1, PixelArt.argb(0xF0E8FF, 0.35F * twinkle));
        }
    }

    /**
     * The researcher's chart in faint gold ink, centred on the heart of the guide and drifting at half the map's pace:
     * four great circles (ticks on the inner ones, turning slowly one way and the other), twelve meridians, a compass rose
     * and the nine primal signs set round the second circle.
     */
    private void chart(GuiGraphicsExtractor graphics, float chartX, float chartY) {
        float scale = 0.55F + 0.45F * zoom;
        float cx = x0 + w / 2.0F + (chartX - cameraX) * 0.5F * zoom;
        float cy = y0 + h / 2.0F + (chartY - cameraY) * 0.5F * zoom;
        float[] radii = {70, 150, 245, 360};
        float turn = now / 90000.0F;
        for (int ring = 0; ring < radii.length; ring++) {
            float r = radii[ring] * scale;
            float spin = ring % 2 == 0 ? turn : -turn * 0.7F;
            circle(graphics, cx, cy, r, ring == 3 ? 3 : 0, PixelArt.argb(INK, ring == 1 ? 0.1F : 0.07F));
            if (ring == 3) {
                continue;
            }
            int ticks = ring == 0 ? 24 : 72;
            for (int i = 0; i < ticks; i++) {
                float a = spin + i * Mth.TWO_PI / ticks;
                boolean major = i % (ticks / 12) == 0;
                float length = major ? 5 : 2;
                float ca = Mth.cos(a);
                float sa = Mth.sin(a);
                for (float d = 0; d < length; d += 1) {
                    dot(graphics, cx + ca * (r + d), cy + sa * (r + d), PixelArt.argb(INK, major ? 0.16F : 0.09F));
                }
            }
        }
        // Meridians from the inner circle out to the last.
        for (int i = 0; i < 12; i++) {
            float a = turn + i * Mth.TWO_PI / 12 + Mth.PI / 12;
            float ca = Mth.cos(a);
            float sa = Mth.sin(a);
            for (float d = radii[0] * scale; d < radii[3] * scale; d += 3) {
                dot(graphics, cx + ca * d, cy + sa * d, PixelArt.argb(INK, 0.05F));
            }
        }
        rose(graphics, cx, cy, radii[0] * scale);
        // The nine primal signs round the second circle, turning with it.
        float r = (radii[1] - 22) * scale;
        for (int i = 0; i < Essence.PRIMALS.size(); i++) {
            float a = turn + i * Mth.TWO_PI / Essence.PRIMALS.size() - Mth.HALF_PI;
            int ex = Math.round(cx + Mth.cos(a) * r) - PixelArt.ICON / 2;
            int ey = Math.round(cy + Mth.sin(a) * r) - PixelArt.ICON / 2;
            if (ex < x0 - 10 || ey < y0 - 10 || ex > x0 + w || ey > y0 + h) {
                continue;
            }
            float glow = 0.5F + 0.5F * Mth.sin(now / 2400.0F + i * 0.7F);
            PixelArt.essence(graphics, Essence.PRIMALS.get(i), ex, ey, PixelArt.ICON, PixelArt.argb(0xF0D898, 0.16F + 0.08F * glow));
        }
        PixelArt.flush(graphics);
    }

    /** An eight-pointed compass rose: long points to the four quarters, short ones between, a small ring in the middle. */
    private void rose(GuiGraphicsExtractor graphics, float cx, float cy, float reach) {
        for (int i = 0; i < 8; i++) {
            float a = i * Mth.PI / 4 - Mth.HALF_PI;
            float length = i % 2 == 0 ? reach * 0.9F : reach * 0.5F;
            float width = i % 2 == 0 ? 6 : 4;
            float ca = Mth.cos(a);
            float sa = Mth.sin(a);
            // Each point: two edges from either side of its foot to its tip.
            for (int side = -1; side <= 1; side += 2) {
                float fa = a + side * Mth.HALF_PI;
                float fx = cx + Mth.cos(fa) * width;
                float fy = cy + Mth.sin(fa) * width;
                float tx = cx + ca * length;
                float ty = cy + sa * length;
                int steps = Math.round(length);
                for (int s = 0; s <= steps; s++) {
                    float k = (float) s / steps;
                    dot(graphics, fx + (tx - fx) * k, fy + (ty - fy) * k, PixelArt.argb(INK, 0.12F));
                }
            }
        }
        circle(graphics, cx, cy, 9, 0, PixelArt.argb(INK, 0.14F));
        circle(graphics, cx, cy, 3, 0, PixelArt.argb(0xFFF0B8, 0.25F));
    }

    /** Small constellations in the far sky: a few brighter stars joined by dotted threads. */
    private void constellations(GuiGraphicsExtractor graphics) {
        float speed = 0.12F;
        float shiftX = cameraX * speed * zoom;
        float shiftY = cameraY * speed * zoom;
        int cell = 300;
        int firstX = Mth.floor(shiftX / cell);
        int firstY = Mth.floor(shiftY / cell);
        for (int gy = firstY; gy <= Mth.floor((shiftY + h) / cell); gy++) {
            for (int gx = firstX; gx <= Mth.floor((shiftX + w) / cell); gx++) {
                long seed = gx * 73856093L ^ gy * 19349663L;
                if (hash(seed) % 3 == 0) {
                    continue;
                }
                int count = 4 + (int) (hash(seed + 1) % 3);
                float px = gx * cell + 40 + hash(seed + 2) % (cell - 80) - shiftX;
                float py = gy * cell + 40 + hash(seed + 3) % (cell - 80) - shiftY;
                float heading = hash(seed + 4) % 628 / 100.0F;
                float twinkle = 0.6F + 0.4F * Mth.sin(now / 1800.0F + gx * 1.3F + gy);
                for (int i = 0; i < count; i++) {
                    heading += (hash(seed + 10 + i) % 200 - 100) / 70.0F;
                    float step = 14 + hash(seed + 20 + i) % 18;
                    float nx = px + Mth.cos(heading) * step;
                    float ny = py + Mth.sin(heading) * step;
                    if (i < count - 1) {
                        PixelArt.dotted(graphics, x0 + Math.round(px), y0 + Math.round(py), x0 + Math.round(nx), y0 + Math.round(ny), 1, 2, 0,
                                PixelArt.argb(0xB8C4F0, 0.13F * twinkle));
                    }
                    star(graphics, x0 + Math.round(px), y0 + Math.round(py), 0xE8ECFF, 0.55F * twinkle, i == 0);
                    px = nx;
                    py = ny;
                }
            }
        }
    }

    /** Three layers of small stars at their own depths, twinkling; near the mouse they shine a little brighter. */
    private void stars(GuiGraphicsExtractor graphics, boolean near, int mouseX, int mouseY) {
        float[] speeds = {0.12F, 0.3F, 0.55F};
        int[] counts = {90, 60, 34};
        for (int layer = 0; layer < speeds.length; layer++) {
            float shiftX = cameraX * speeds[layer] * zoom;
            float shiftY = cameraY * speeds[layer] * zoom;
            int cell = 512;
            int count = counts[layer] * Math.max(1, w * h / (300 * 180));
            for (int i = 0; i < count; i++) {
                long seed = i * 2654435761L + layer * 40503L;
                int sx = (int) Math.floorMod((long) (hash(seed) % cell) - (long) shiftX, cell);
                int sy = (int) Math.floorMod((long) (hash(seed + 7) % cell) - (long) shiftY, cell);
                if (sx >= w || sy >= h) {
                    continue;
                }
                float twinkle = 0.55F + 0.45F * Mth.sin(now / (700.0F + hash(seed + 3) % 900) + i);
                long kind = hash(seed + 5) % 7;
                int tint = layer == 2 ? 0xFFF6E0 : kind == 0 ? 0xB8C8FF : kind == 1 ? 0xFFD8B0 : 0xE0D8F0;
                float alpha = (0.25F + 0.25F * layer) * twinkle;
                if (near) {
                    float d = com.pockyl.hextale.port.PortMath.length(x0 + sx - mouseX, y0 + sy - mouseY);
                    alpha = Math.min(1, alpha * (1 + 0.8F * Math.max(0, 1 - d / NEAR)));
                }
                boolean great = layer == 2 && hash(seed + 11) % 4 == 0;
                star(graphics, x0 + sx, y0 + sy, tint, alpha, great);
                if (great && hash(seed + 13) % 3 == 0) {
                    // The brightest flash long crossed glints now and then.
                    float flare = Math.max(0, Mth.sin(now / 1300.0F + i * 2.1F) - 0.6F) * 2.5F;
                    for (int d = 2; d <= 4; d++) {
                        int glint = PixelArt.argb(tint, alpha * flare * (1 - d / 5.0F));
                        PixelArt.fill(graphics, x0 + sx - d, y0 + sy, x0 + sx - d + 1, y0 + sy + 1, glint);
                        PixelArt.fill(graphics, x0 + sx + d, y0 + sy, x0 + sx + d + 1, y0 + sy + 1, glint);
                        PixelArt.fill(graphics, x0 + sx, y0 + sy - d, x0 + sx + 1, y0 + sy - d + 1, glint);
                        PixelArt.fill(graphics, x0 + sx, y0 + sy + d, x0 + sx + 1, y0 + sy + d + 1, glint);
                    }
                }
            }
        }
    }

    /** One star: a pixel, and a dim cross around it for a bright one. */
    private static void star(GuiGraphicsExtractor graphics, int x, int y, int tint, float alpha, boolean bright) {
        PixelArt.fill(graphics, x, y, x + 1, y + 1, PixelArt.argb(tint, alpha));
        if (bright) {
            int dim = PixelArt.argb(tint, alpha * 0.45F);
            PixelArt.fill(graphics, x - 1, y, x, y + 1, dim);
            PixelArt.fill(graphics, x + 1, y, x + 2, y + 1, dim);
            PixelArt.fill(graphics, x, y - 1, x + 1, y, dim);
            PixelArt.fill(graphics, x, y + 1, x + 1, y + 2, dim);
        }
    }

    /**
     * Star dust off the mouse: while it moves, small specks are left along its way, twinkle and drift down a little as they
     * fade, gone in well under a second. A mouse at rest leaves nothing.
     */
    private void dust(GuiGraphicsExtractor graphics, boolean trail, int mouseX, int mouseY) {
        if (!trail) {
            lastMouseX = Float.NaN;
        } else {
            if (!Float.isNaN(lastMouseX)) {
                float dx = mouseX - lastMouseX;
                float dy = mouseY - lastMouseY;
                int specks = Math.min(6, (int) (com.pockyl.hextale.port.PortMath.length(dx, dy) / 5));
                for (int i = 1; i <= specks; i++) {
                    float k = (float) i / specks;
                    int at = dustNext * 4;
                    DUST[at] = lastMouseX + dx * k;
                    DUST[at + 1] = lastMouseY + dy * k;
                    DUST[at + 2] = now % 1_000_000;
                    DUST[at + 3] = hash(now + i * 7919L) % 1000;
                    dustNext = (dustNext + 1) % (DUST.length / 4);
                }
                if (specks > 0 || Float.isNaN(lastMouseX)) {
                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                }
            } else {
                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }
        }
        for (int i = 0; i < DUST.length; i += 4) {
            float age = (now % 1_000_000 - DUST[i + 2]) / DUST_LIFE;
            if (age < 0 || age >= 1) {
                continue;
            }
            float seed = DUST[i + 3];
            float fade = (1 - age) * (1 - age);
            float x = DUST[i] + Mth.sin(seed + age * 3) * 2 * age;
            float y = DUST[i + 1] + age * 5;
            int tint = seed % 4 < 1 ? 0xBFE6FF : 0xFFE4A8;
            float twinkle = 0.7F + 0.3F * Mth.sin(age * 20 + seed);
            int px = Mth.floor(x);
            int py = Mth.floor(y);
            if (age < 0.35F && seed % 3 < 1) {
                star(graphics, px, py, tint, 0.75F * fade * twinkle, true);
            } else {
                dot(graphics, x, y, PixelArt.argb(tint, 0.7F * fade * twinkle));
            }
        }
    }

    /** Now and then a star falls across the sky, a fading tail behind it. */
    private void fallingStar(GuiGraphicsExtractor graphics) {
        long slot = now / 3700;
        if (hash(slot * 31 + 7) % 3 != 0) {
            return;
        }
        float life = (now % 3700) / 700.0F;
        if (life >= 1) {
            return;
        }
        float startX = hash(slot + 1) % Math.max(1, w);
        float startY = hash(slot + 2) % Math.max(1, h / 2);
        float heading = (hash(slot + 3) % 2 == 0 ? 0.55F : Mth.PI - 0.55F) + (hash(slot + 4) % 40 - 20) / 100.0F;
        float travel = 90 + hash(slot + 5) % 80;
        float hx = startX + Mth.cos(heading) * travel * life;
        float hy = startY + Mth.sin(heading) * travel * life;
        float fade = life < 0.15F ? life / 0.15F : 1 - (life - 0.15F) / 0.85F;
        for (int i = 0; i < 22; i++) {
            float k = i / 22.0F;
            float px = hx - Mth.cos(heading) * i;
            float py = hy - Mth.sin(heading) * i;
            dot(graphics, x0 + px, y0 + py, PixelArt.argb(i < 3 ? 0xFFFFFF : 0xFFE8B8, fade * (1 - k) * 0.85F));
        }
        star(graphics, Math.round(x0 + hx), Math.round(y0 + hy), 0xFFFFFF, fade, true);
    }

    /** Warm motes of dust drifting slowly up, as in the light of a desk lamp; a few cold hextech sparks among them. */
    private void motes(GuiGraphicsExtractor graphics) {
        float speed = 0.8F;
        float shiftX = cameraX * speed * zoom;
        float shiftY = cameraY * speed * zoom;
        int count = 18 * Math.max(1, w * h / (300 * 180));
        for (int i = 0; i < count; i++) {
            long seed = i * 40503L + 17;
            float rise = now / (90.0F + hash(seed) % 70);
            float sway = 6 * Mth.sin(now / (1500.0F + hash(seed + 1) % 900) + i);
            int sx = (int) Math.floorMod((long) (hash(seed + 2) % 600 + sway - shiftX), 600L);
            int sy = (int) Math.floorMod((long) (hash(seed + 3) % 400 - rise - shiftY), 400L);
            if (sx >= w || sy >= h) {
                continue;
            }
            boolean spark = hash(seed + 4) % 5 == 0;
            float glow = 0.5F + 0.5F * Mth.sin(now / (500.0F + hash(seed + 5) % 400) + i * 1.7F);
            int tint = spark ? 0x9FE6FF : 0xFFD890;
            PixelArt.disc(graphics, x0 + sx, y0 + sy, 4, PixelArt.argb(tint, 0.06F + 0.05F * glow), 0);
            PixelArt.fill(graphics, x0 + sx, y0 + sy, x0 + sx + 1, y0 + sy + 1, PixelArt.argb(tint, 0.3F + 0.45F * glow));
        }
    }

    /** The sky darkens towards its rim, so the guide sits in a pool of light. */
    private void rim(GuiGraphicsExtractor graphics) {
        int bands = 14;
        for (int k = 0; k < bands; k++) {
            float t = 1 - (float) k / bands;
            int color = PixelArt.argb(0x020106, 0.09F * t * t);
            int in = k * 2;
            PixelArt.fill(graphics, x0 + in, y0 + in, x0 + w - in, y0 + in + 2, color);
            PixelArt.fill(graphics, x0 + in, y0 + h - in - 2, x0 + w - in, y0 + h - in, color);
            PixelArt.fill(graphics, x0 + in, y0 + in + 2, x0 + in + 2, y0 + h - in - 2, color);
            PixelArt.fill(graphics, x0 + w - in - 2, y0 + in + 2, x0 + w - in, y0 + h - in - 2, color);
        }
    }

    /** A circle of pixels, each drawn once; {@code dash} > 0 leaves gaps of that many pixels after as many drawn. */
    private void circle(GuiGraphicsExtractor graphics, float cx, float cy, float r, int dash, int argb) {
        int steps = Math.max(8, Math.round(Mth.TWO_PI * r));
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        for (int i = 0; i < steps; i++) {
            if (dash > 0 && i % (dash * 2) >= dash) {
                continue;
            }
            float a = i * Mth.TWO_PI / steps;
            int x = Mth.floor(cx + Mth.cos(a) * r);
            int y = Mth.floor(cy + Mth.sin(a) * r);
            if (x == lastX && y == lastY) {
                continue;
            }
            lastX = x;
            lastY = y;
            if (x >= x0 && y >= y0 && x < x0 + w && y < y0 + h) {
                PixelArt.fill(graphics, x, y, x + 1, y + 1, argb);
            }
        }
    }

    private void dot(GuiGraphicsExtractor graphics, float x, float y, int argb) {
        int px = Mth.floor(x);
        int py = Mth.floor(y);
        if (px >= x0 && py >= y0 && px < x0 + w && py < y0 + h) {
            PixelArt.fill(graphics, px, py, px + 1, py + 1, argb);
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
