package com.pockyl.hextale.thevoid;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.Arrays;

/**
 * The Void's outline: a grid of square cells in the breach's plane, built from the seed alone (both sides build the
 * same one). The body is rectangles of nothing laid over each other - each keeps its own region, so their overlaps
 * show as seams - with bites taken out of the edge, dark dead cells inside, teeth standing out of the edge and loose
 * cells hanging just off it. Cell (i, j) spans local x {@code (i + originX) * cell} .. one cell further, y likewise.
 */
public final class VoidLayout {
    public static final byte EMPTY = -1;
    /** Region of the dead cells inside the body. */
    public static final byte DEAD = 15;
    /** Region of the loose cells beside it. */
    public static final byte LOOSE = 14;

    public static final byte BODY = 0;
    public static final byte TOOTH = 1;
    public static final byte FRAGMENT = 2;

    /** Free cells around the body: room for teeth, loose cells and the glow around them. */
    private static final int MARGIN = 5;

    public final VoidShape shape;
    public final float size;
    public final int seed;
    /** Edge of a cell in blocks. */
    public final float cell;
    public final int width;
    public final int height;
    public final int originX;
    public final int originY;
    private final byte[] region;
    private final byte[] kind;
    /** 0-1: when the cell appears as the Void opens (and the reverse as it closes). */
    private final float[] delay;
    private final int[] body;
    private final int[] rim;

    private VoidLayout(VoidShape shape, float size, int seed) {
        this.shape = shape;
        this.size = size;
        this.seed = seed;
        cell = Mth.clamp(size / 13, 0.16F, 0.42F);
        int cellsLong = Math.max(4, Math.round(size / cell));
        int cellsWide = Math.max(2, Math.round(size * shape.aspect() / cell));
        width = cellsWide + MARGIN * 2;
        height = cellsLong + MARGIN * 2;
        originX = -width / 2;
        originY = -height / 2;
        region = new byte[width * height];
        kind = new byte[width * height];
        delay = new float[width * height];
        Arrays.fill(region, EMPTY);

        RandomSource random = RandomSource.create(seed * 0x9E3779B97F4A7C15L + shape.ordinal());
        int x0 = MARGIN;
        int y0 = MARGIN;
        switch (shape) {
            case EYE -> eye(random, x0, y0, cellsWide, cellsLong);
            case SLIT -> slit(random, x0, y0, cellsWide, cellsLong);
            case SCATTER -> scatter(random, x0, y0, cellsWide, cellsLong);
            default -> gate(random, x0, y0, cellsWide, cellsLong);
        }
        bites(random, 2 + random.nextInt(3) + Math.round(size * 0.4F));
        deadCells(random, 1 + random.nextInt(3));
        timeline(random);
        teeth(random, Math.max(3, Math.round((cellsWide + cellsLong) * 0.3F)));
        fragments(random, 3 + random.nextInt(3) + Math.round(size));

        int bodyCount = 0;
        int rimCount = 0;
        for (int at = 0; at < region.length; at++) {
            if (region[at] != EMPTY && kind[at] == BODY) {
                bodyCount++;
                if (edge(at % width, at / width)) {
                    rimCount++;
                }
            }
        }
        body = new int[bodyCount];
        rim = new int[rimCount];
        for (int at = 0, b = 0, r = 0; at < region.length; at++) {
            if (region[at] != EMPTY && kind[at] == BODY) {
                body[b++] = at;
                if (edge(at % width, at / width)) {
                    rim[r++] = at;
                }
            }
        }
    }

    private static final int MAX_CACHED = 64;
    private static final java.util.Map<Long, VoidLayout> CACHE = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<>(16, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Long, VoidLayout> eldest) {
                    return size() > MAX_CACHED;
                }
            });

    /** The layout of a Void with these numbers (cached: the client asks every frame). */
    public static VoidLayout of(VoidShape shape, float size, int seed) {
        long key = (long) seed << 32 ^ (long) shape.ordinal() << 24 ^ Float.floatToIntBits(size);
        return CACHE.computeIfAbsent(key, k -> new VoidLayout(shape, size, seed));
    }

    // --- Shapes -------------------------------------------------------------------------------------------------------

    private void gate(RandomSource random, int x0, int y0, int w, int h) {
        int inset = Math.max(1, w / 8);
        fill(x0 + random.nextInt(inset + 1), y0 + random.nextInt(2), x0 + w - 1 - random.nextInt(inset + 1),
                y0 + h - 1 - random.nextInt(2), 0);
        overlays(random, x0, y0, w, h, 2 + random.nextInt(3));
        inner(random, x0, y0, w, h, 2 + random.nextInt(2));
    }

    private void eye(RandomSource random, int x0, int y0, int w, int h) {
        float cx = x0 + w / 2F;
        float cy = y0 + h / 2F;
        int steps = 10;
        float[] reach = new float[steps];
        for (int i = 0; i < steps; i++) {
            reach[i] = 0.88F + random.nextFloat() * 0.24F;
        }
        for (int j = y0 - 2; j < y0 + h + 2; j++) {
            for (int i = x0 - 2; i < x0 + w + 2; i++) {
                float dx = (i + 0.5F - cx) / (w / 2F);
                float dy = (j + 0.5F - cy) / (h / 2F);
                float angle = (float) Math.atan2(dy, dx) / Mth.TWO_PI + 0.5F;
                // Stepped edge: the reach changes in whole sectors, so the outline breaks into ledges.
                if (dx * dx + dy * dy <= reach[Math.min(steps - 1, (int) (angle * steps))]) {
                    set(i, j, 0);
                }
            }
        }
        overlays(random, x0 + 1, y0 + 1, w - 2, h - 2, 1 + random.nextInt(2));
        inner(random, x0, y0, w, h, 2 + random.nextInt(3));
    }

    private void slit(RandomSource random, int x0, int y0, int w, int h) {
        int offset = 0;
        int region = 0;
        for (int y = y0; y < y0 + h; ) {
            int run = Math.min(y0 + h - y, 2 + random.nextInt(3));
            int span = Math.max(2, w + random.nextInt(3) - 1);
            int left = x0 + (w - span) / 2 + offset;
            fill(left, y, left + span - 1, y + run - 1, region);
            region = region % 3 + 1;
            offset = Mth.clamp(offset + random.nextInt(3) - 1, -1, 1);
            y += run;
        }
    }

    private void scatter(RandomSource random, int x0, int y0, int w, int h) {
        int mw = Math.max(3, Math.round(w * (0.5F + random.nextFloat() * 0.2F)));
        int mh = Math.max(3, Math.round(h * (0.5F + random.nextFloat() * 0.2F)));
        int mx = x0 + (w - mw) / 2;
        int my = y0 + (h - mh) / 2;
        fill(mx, my, mx + mw - 1, my + mh - 1, 0);
        int pieces = 3 + random.nextInt(3);
        for (int k = 1; k <= pieces; k++) {
            int pw = Math.max(2, Math.round(w * (0.2F + random.nextFloat() * 0.25F)));
            int ph = Math.max(2, Math.round(h * (0.2F + random.nextFloat() * 0.25F)));
            int px = x0 + random.nextInt(Math.max(1, w - pw + 1));
            int py = y0 + random.nextInt(Math.max(1, h - ph + 1));
            fill(px, py, px + pw - 1, py + ph - 1, k);
        }
    }

    /** Rectangles laid over an edge of the body, half in, half out. */
    private void overlays(RandomSource random, int x0, int y0, int w, int h, int count) {
        for (int k = 0; k < count; k++) {
            int side = random.nextInt(4);
            boolean across = side < 2;
            int span = Math.max(2, Math.round((across ? w : h) * (0.3F + random.nextFloat() * 0.3F)));
            int out = 1 + random.nextInt(2);
            int in = 2 + random.nextInt(3);
            int along = random.nextInt(Math.max(1, (across ? w : h) - span + 1));
            int region = 1 + k;
            switch (side) {
                case 0 -> fill(x0 + along, y0 + h - in, x0 + along + span - 1, y0 + h - 1 + out, region);
                case 1 -> fill(x0 + along, y0 - out, x0 + along + span - 1, y0 + in - 1, region);
                case 2 -> fill(x0 + w - in, y0 + along, x0 + w - 1 + out, y0 + along + span - 1, region);
                default -> fill(x0 - out, y0 + along, x0 + in - 1, y0 + along + span - 1, region);
            }
        }
    }

    /** Rectangles inside the body with their own shade: seams cross the Void like cracks in glass. */
    private void inner(RandomSource random, int x0, int y0, int w, int h, int count) {
        for (int k = 0; k < count; k++) {
            int iw = Math.max(2, Math.round(w * (0.25F + random.nextFloat() * 0.25F)));
            int ih = Math.max(2, Math.round(h * (0.2F + random.nextFloat() * 0.25F)));
            int ix = x0 + 1 + random.nextInt(Math.max(1, w - iw - 1));
            int iy = y0 + 1 + random.nextInt(Math.max(1, h - ih - 1));
            for (int j = iy; j < iy + ih; j++) {
                for (int i = ix; i < ix + iw; i++) {
                    if (on(i, j)) {
                        region[i + j * width] = (byte) (5 + k);
                    }
                }
            }
        }
    }

    /** Small pieces bitten out of the edge. */
    private void bites(RandomSource random, int count) {
        for (int k = 0; k < count * 4 && count > 0; k++) {
            int i = random.nextInt(width);
            int j = random.nextInt(height);
            if (!on(i, j) || !edge(i, j)) {
                continue;
            }
            int bw = 1 + random.nextInt(2);
            int bh = 1 + random.nextInt(3);
            for (int y = j; y < j + bh; y++) {
                for (int x = i; x < i + bw; x++) {
                    if (inside(x, y)) {
                        region[x + y * width] = EMPTY;
                    }
                }
            }
            count--;
        }
    }

    private void deadCells(RandomSource random, int count) {
        for (int k = 0; k < count * 20 && count > 0; k++) {
            int i = random.nextInt(width);
            int j = random.nextInt(height);
            if (on(i, j) && !edge(i, j) && on(i - 1, j - 1) && on(i + 1, j + 1) && on(i - 1, j + 1) && on(i + 1, j - 1)) {
                region[i + j * width] = DEAD;
                count--;
            }
        }
    }

    private void timeline(RandomSource random) {
        float cx = width / 2F;
        float cy = height / 2F;
        float far = 0;
        for (int at = 0; at < region.length; at++) {
            if (region[at] != EMPTY) {
                far = Math.max(far, Mth.length(at % width + 0.5F - cx, at / width + 0.5F - cy));
            }
        }
        for (int at = 0; at < region.length; at++) {
            if (region[at] != EMPTY) {
                float d = Mth.length(at % width + 0.5F - cx, at / width + 0.5F - cy) / Math.max(1, far);
                delay[at] = Math.min(0.8F, d * 0.7F + random.nextFloat() * 0.18F);
            }
        }
    }

    private static final int[][] SIDES = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    /** Teeth: one cell wide, up to three long, standing out of the edge; they break through last. */
    private void teeth(RandomSource random, int count) {
        for (int k = 0; k < count * 8 && count > 0; k++) {
            int i = random.nextInt(width);
            int j = random.nextInt(height);
            if (!on(i, j) || kind[i + j * width] != BODY) {
                continue;
            }
            int[] side = SIDES[random.nextInt(4)];
            if (on(i + side[0], j + side[1])) {
                continue;
            }
            int length = 1 + random.nextInt(3);
            byte from = region[i + j * width];
            for (int step = 1; step <= length; step++) {
                int x = i + side[0] * step;
                int y = j + side[1] * step;
                if (!inside(x, y) || x < 1 || y < 1 || x >= width - 1 || y >= height - 1 || on(x, y)) {
                    break;
                }
                int at = x + y * width;
                region[at] = from == DEAD ? 0 : from;
                kind[at] = TOOTH;
                delay[at] = 0.8F + 0.05F * step;
            }
            count--;
        }
    }

    /** Loose cells hanging a cell or two off the edge. */
    private void fragments(RandomSource random, int count) {
        for (int k = 0; k < count * 10 && count > 0; k++) {
            int i = random.nextInt(width);
            int j = random.nextInt(height);
            if (!on(i, j)) {
                continue;
            }
            int[] side = SIDES[random.nextInt(4)];
            int gap = 2 + random.nextInt(2);
            int x = i + side[0] * gap;
            int y = j + side[1] * gap;
            if (x < 1 || y < 1 || x >= width - 1 || y >= height - 1 || on(x, y) || touches(x, y)) {
                continue;
            }
            float when = random.nextFloat();
            place(x, y, when);
            // Now and then a piece of two.
            int[] grow = SIDES[random.nextInt(4)];
            int x2 = x + grow[0];
            int y2 = y + grow[1];
            if (random.nextInt(3) == 0 && x2 >= 1 && y2 >= 1 && x2 < width - 1 && y2 < height - 1 && !on(x2, y2)
                    && !touchesOther(x2, y2, x, y)) {
                place(x2, y2, when);
            }
            count--;
        }
    }

    private void place(int x, int y, float when) {
        int at = x + y * width;
        region[at] = LOOSE;
        kind[at] = FRAGMENT;
        delay[at] = 0.85F + when * 0.15F;
    }

    private boolean touches(int x, int y) {
        return touchesOther(x, y, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private boolean touchesOther(int x, int y, int skipX, int skipY) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dy != 0) && !(x + dx == skipX && y + dy == skipY) && on(x + dx, y + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void fill(int ax, int ay, int bx, int by, int regionId) {
        for (int y = Math.min(ay, by); y <= Math.max(ay, by); y++) {
            for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++) {
                set(x, y, regionId);
            }
        }
    }

    private void set(int x, int y, int regionId) {
        if (x >= 1 && y >= 1 && x < width - 1 && y < height - 1) {
            region[x + y * width] = (byte) regionId;
        }
    }

    // --- Queries ------------------------------------------------------------------------------------------------------

    public boolean inside(int i, int j) {
        return i >= 0 && j >= 0 && i < width && j < height;
    }

    /** Part of the Void (whatever the timeline says). */
    public boolean on(int i, int j) {
        return inside(i, j) && region[i + j * width] != EMPTY;
    }

    private boolean edge(int i, int j) {
        return !on(i + 1, j) || !on(i - 1, j) || !on(i, j + 1) || !on(i, j - 1);
    }

    public byte region(int i, int j) {
        return region[i + j * width];
    }

    public byte kind(int i, int j) {
        return kind[i + j * width];
    }

    public float delay(int i, int j) {
        return delay[i + j * width];
    }

    /** Whether a cell shows while the Void is {@code reveal} (0-1) open: it spreads from the centre and shrinks back. */
    public boolean shows(int i, int j, float reveal) {
        int at = i + j * width;
        return region[at] != EMPTY && delay[at] <= reveal * 1.02F;
    }

    /** 1 for a cell that has only just broken through, fading to 0 as the opening runs on. */
    public float fresh(int i, int j, float reveal) {
        float since = reveal - delay[i + j * width];
        return reveal >= 1 || since < 0 ? 0 : Mth.clamp(1 - since / 0.2F, 0, 1);
    }

    /** Local x of the cell's left edge (blocks). */
    public float x(int i) {
        return (i + originX) * cell;
    }

    /** Local y of the cell's lower edge (blocks). */
    public float y(int j) {
        return (j + originY) * cell;
    }

    /** Half the slab's thickness (blocks). */
    public float halfThickness() {
        return cell * 0.22F;
    }

    /** Roughly how far the Void reaches from its centre (blocks). */
    public float reach() {
        return Math.max(width, height) * cell * 0.5F;
    }

    /** A random point in the body (local, blocks, plane z = 0); {@code rim} picks one on its edge. */
    public Vector3f samplePoint(RandomSource random, boolean rim) {
        int[] from = rim && this.rim.length > 0 ? this.rim : body;
        if (from.length == 0) {
            return new Vector3f();
        }
        int at = from[random.nextInt(from.length)];
        return new Vector3f(x(at % width) + random.nextFloat() * cell, y(at / width) + random.nextFloat() * cell, 0);
    }

    public int bodyCells() {
        return body.length;
    }

    /** A stable 0-1 hash of two ints. */
    public static float hash(int a, int b) {
        int h = a * 0x27D4EB2D ^ b * 0x165667B1;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h >>> 8) / (float) (1 << 24);
    }
}
