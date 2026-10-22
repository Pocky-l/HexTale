package com.pockyl.hextale.client.render;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import com.pockyl.hextale.abyss.RiftShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The jagged opening of one rift, built once from its shape, size and seed (the same numbers always give the same
 * rift). Flat, on the rift's plane: every vertex sits on a SPINE point plus an OFFSET that the opening scales - 0 is a
 * closed crack, 1 the full tear - so the renderer can let it crack, part with an overshoot, breathe and shut without
 * rebuilding. Each vertex also knows how deep it lies in the opening (0 on the torn edge, 1 on the spine) and when the
 * crack reaches it ({@link #delay}, 0 at the point it started from, ~1 at the far ends).
 */
final class RiftMesh {
    float[] cx = new float[64];
    float[] cy = new float[64];
    float[] ox = new float[64];
    float[] oy = new float[64];
    /** Outward direction of an edge vertex (unit), for the halo. */
    float[] nx = new float[64];
    float[] ny = new float[64];
    float[] edge = new float[64];
    float[] delay = new float[64];
    int count;
    int[] quads = new int[256];
    int quadCount;
    /** Runs of edge vertices along the torn edge, for the halo. */
    final List<int[]> rims = new ArrayList<>();
    final List<Shard> shards = new ArrayList<>();

    /** A splinter of torn space floating near the edge: an irregular tetrahedron (12 floats) at a rest position. */
    record Shard(float x, float y, float z, float[] corners, float axisX, float axisY, float axisZ, float spin, float phase) {
    }

    private RiftMesh() {
    }

    static RiftMesh build(RiftShape shape, float size, int seed) {
        RiftMesh mesh = new RiftMesh();
        RandomSource random = RandomSource.create(seed * 341873128712L + 132897987541L);
        float half = size / 2;
        float width = shape.halfWidth(size);
        switch (shape) {
            case TEAR -> {
                float[][] spine = jagged(0, -half, 0, half, segments(size), 0.035F, 0.12F, random);
                float origin = 0.35F + random.nextFloat() * 0.3F;
                mesh.strip(spine, width, true, false, origin, 0, half, random);
                if (random.nextFloat() < 0.4F) {
                    mesh.branches(spine, 1, width * 0.45F, size * 0.22F, origin, half, random);
                }
            }
            case FISSURE -> {
                float[][] spine = jagged(0, -half, 0, half, segments(size), 0.05F, 0.1F, random);
                float origin = 0.3F + random.nextFloat() * 0.4F;
                mesh.strip(spine, width, true, false, origin, 0, half, random);
                mesh.branches(spine, 1 + random.nextInt(2), width * 0.5F, size * 0.22F, origin, half, random);
            }
            case CLEFT -> {
                float[][] spine = jagged(0, -half, 0, half, segments(size), 0.06F, 0.16F, random);
                float origin = 0.25F + random.nextFloat() * 0.5F;
                mesh.strip(spine, width, true, false, origin, 0, half, random);
                mesh.branches(spine, 2 + random.nextInt(2), width * 0.6F, size * 0.36F, origin, half, random);
            }
            case SHATTER -> mesh.shatter(size, random);
            case MAW -> mesh.maw(size, width, random);
        }
        mesh.shards(shape, size, random);
        return mesh;
    }

    private static int segments(float size) {
        return Mth.clamp(Math.round(size * 7), 12, 44);
    }

    /**
     * A wandering line from a to b: a gentle bend, an S-curve and a slow random drift, all smooth (the torn look comes
     * from the edges, not the spine - a spine that zigzags folds the strip over itself). {@code jag} and {@code bend}
     * are shares of the length.
     */
    private static float[][] jagged(float ax, float ay, float bx, float by, int segments, float jag, float bend, RandomSource random) {
        float dx = bx - ax;
        float dy = by - ay;
        float length = Mth.sqrt(dx * dx + dy * dy);
        float px = -dy / length;
        float py = dx / length;
        float bow = (random.nextFloat() * 2 - 1) * bend;
        float wave = (random.nextFloat() * 2 - 1) * bend * 0.5F;
        float[] walk = new float[segments + 1];
        float drift = 0;
        for (int i = 0; i <= segments; i++) {
            drift = drift * 0.8F + (float) random.nextGaussian() * jag;
            walk[i] = drift;
        }
        float[][] points = new float[segments + 1][];
        for (int i = 0; i <= segments; i++) {
            float smooth = 0;
            int taken = 0;
            for (int k = Math.max(0, i - 2); k <= Math.min(segments, i + 2); k++) {
                smooth += walk[k];
                taken++;
            }
            float s = i / (float) segments;
            float pin = Mth.sin(s * Mth.PI);
            float off = (smooth / taken * pin + bow * pin + wave * Mth.sin(s * Mth.TWO_PI)) * length;
            points[i] = new float[]{ax + dx * s + px * off, ay + dy * s + py * off};
        }
        return points;
    }

    /** Radius of the spine's turn at point i (huge on straight stretches), times a safety share. */
    private static float bendRoom(float[][] spine, int i) {
        int n = spine.length;
        if (i == 0 || i == n - 1) {
            return Float.MAX_VALUE;
        }
        float ax = spine[i][0] - spine[i - 1][0];
        float ay = spine[i][1] - spine[i - 1][1];
        float bx = spine[i + 1][0] - spine[i][0];
        float by = spine[i + 1][1] - spine[i][1];
        float turn = Math.abs((float) Mth.atan2(ax * by - ay * bx, ax * bx + ay * by));
        float step = (Mth.sqrt(ax * ax + ay * ay) + Mth.sqrt(bx * bx + by * by)) / 2;
        return turn < 1.0E-4F ? Float.MAX_VALUE : 0.8F * step / turn;
    }

    /**
     * A strip along the spine: an edge vertex left and right of every spine point and the spine point itself.
     * {@code bothEnds}: tapers to a point at both ends, else full at the root and a point at the tip; a soft root melts
     * into what it grows out of (no torn edge there). Delays run from {@code origin} (share of the spine) outwards,
     * starting at {@code delayAtRoot}, in units of {@code half} blocks.
     */
    private void strip(float[][] spine, float width, boolean bothEnds, boolean softRoot, float origin, float delayAtRoot, float half,
                       RandomSource random) {
        int n = spine.length;
        float[] along = new float[n];
        for (int i = 1; i < n; i++) {
            along[i] = along[i - 1] + Mth.sqrt(Mth.square(spine[i][0] - spine[i - 1][0]) + Mth.square(spine[i][1] - spine[i - 1][1]));
        }
        float total = along[n - 1];
        float originAt = bothEnds ? origin * total : 0;
        int[] left = new int[n];
        int[] middle = new int[n];
        int[] right = new int[n];
        for (int i = 0; i < n; i++) {
            float s = i / (float) (n - 1);
            float[] a = spine[Math.max(0, i - 2)];
            float[] b = spine[Math.min(n - 1, i + 2)];
            float tx = b[0] - a[0];
            float ty = b[1] - a[1];
            float tl = Mth.sqrt(tx * tx + ty * ty);
            float normalX = tl < 1.0E-5F ? 1 : -ty / tl;
            float normalY = tl < 1.0E-5F ? 0 : tx / tl;
            // A fork leaves its parent narrow, swells a little and runs out to a point.
            float profile = bothEnds ? (float) Math.pow(Mth.sin(s * Mth.PI), 0.62)
                    : (float) Math.pow(1 - s, 0.8) * (0.55F + 0.45F * Math.min(1, s / 0.25F));
            float w = width * profile * (0.85F + 0.3F * random.nextFloat());
            // Never wider than the bend can take: wider would fold the strip over itself on the inside of the turn.
            float room = Math.min(bendRoom(spine, Math.max(1, i - 1)), Math.min(bendRoom(spine, i), bendRoom(spine, Math.min(n - 2, i + 1))));
            float wl = Math.min(w * tooth(random), room);
            float wr = Math.min(w * tooth(random), room);
            float d = delayAtRoot + Math.abs(along[i] - originAt) / half;
            // A soft root has no torn edge where it leaves its parent: the edge depth fades in over the first stretch.
            float rimEdge = softRoot && s < 0.3F ? 1 - s / 0.3F : 0;
            middle[i] = vertex(spine[i][0], spine[i][1], 0, 0, normalX, normalY, 1, d);
            left[i] = vertex(spine[i][0], spine[i][1], normalX * wl, normalY * wl, normalX, normalY, rimEdge, d);
            right[i] = vertex(spine[i][0], spine[i][1], -normalX * wr, -normalY * wr, -normalX, -normalY, rimEdge, d);
        }
        for (int i = 0; i + 1 < n; i++) {
            quad(left[i], left[i + 1], middle[i + 1], middle[i]);
            quad(middle[i], middle[i + 1], right[i + 1], right[i]);
        }
        int from = softRoot ? Math.min(n - 1, Math.max(2, n * 3 / 10)) : 0;
        rims.add(Arrays.copyOfRange(left, from, n));
        rims.add(Arrays.copyOfRange(right, from, n));
    }

    /** A random lip of the edge: mostly near the line, now and then torn far out or bitten in. */
    private static float tooth(RandomSource random) {
        float r = random.nextFloat();
        if (r < 0.12F) {
            return 1.25F + random.nextFloat() * 0.25F;
        }
        if (r < 0.24F) {
            return 0.7F + random.nextFloat() * 0.1F;
        }
        return 0.85F + random.nextFloat() * 0.25F;
    }

    /** Forks growing out of the spine at random points, turned away from it, each maybe forking once more. */
    private void branches(float[][] spine, int count, float width, float length, float origin, float half, RandomSource random) {
        int n = spine.length;
        float total = 0;
        float[] along = new float[n];
        for (int i = 1; i < n; i++) {
            total += Mth.sqrt(Mth.square(spine[i][0] - spine[i - 1][0]) + Mth.square(spine[i][1] - spine[i - 1][1]));
            along[i] = total;
        }
        for (int b = 0; b < count; b++) {
            int at = Mth.clamp((int) ((0.2F + random.nextFloat() * 0.6F) * (n - 1)), 1, n - 2);
            float tx = spine[at + 1][0] - spine[at - 1][0];
            float ty = spine[at + 1][1] - spine[at - 1][1];
            float heading = (float) Mth.atan2(ty, tx);
            // Forks lean away from where the crack started, like real cracks running on.
            if (at / (float) (n - 1) < origin) {
                heading += Mth.PI;
            }
            float turn = (0.45F + random.nextFloat() * 0.5F) * (random.nextBoolean() ? 1 : -1);
            fork(spine[at][0], spine[at][1], heading + turn, length * (0.7F + random.nextFloat() * 0.5F), width,
                    Math.abs(along[at] - origin * total) / half, half, random, 1);
        }
    }

    private void fork(float x, float y, float heading, float length, float width, float delay, float half, RandomSource random, int depth) {
        float ex = x + Mth.cos(heading) * length;
        float ey = y + Mth.sin(heading) * length;
        float[][] spine = jagged(x, y, ex, ey, Mth.clamp(Math.round(length * 7), 5, 24), 0.05F, 0.14F, random);
        strip(spine, width, false, true, 0, delay, half, random);
        if (depth > 0 && length > 1.2F && random.nextFloat() < 0.5F) {
            int at = spine.length / 2;
            fork(spine[at][0], spine[at][1], heading + (0.5F + random.nextFloat() * 0.4F) * (random.nextBoolean() ? 1 : -1),
                    length * 0.45F, width * 0.6F, delay + length * 0.5F / half, half, random, depth - 1);
        }
    }

    /** Space broken like glass: a torn hub and arms of cracks running out of it. */
    private void shatter(float size, RandomSource random) {
        float half = size / 2;
        float hub = size * 0.13F;
        ring(0, 0, hub, hub, new float[]{1, 0.55F, 0}, 12, 0.25F, random, 0, half, false);
        int arms = 4 + random.nextInt(4);
        float start = random.nextFloat() * Mth.TWO_PI;
        for (int a = 0; a < arms; a++) {
            float heading = start + a * Mth.TWO_PI / arms + (random.nextFloat() - 0.5F) * 0.6F;
            float length = half * (0.45F + random.nextFloat() * 0.55F);
            float rx = Mth.cos(heading) * hub * 0.35F;
            float ry = Mth.sin(heading) * hub * 0.35F;
            float[][] spine = jagged(rx, ry, rx + Mth.cos(heading) * length, ry + Mth.sin(heading) * length,
                    Mth.clamp(Math.round(length * 7), 6, 28), 0.05F, 0.1F, random);
            strip(spine, hub * 0.85F, false, true, 0, 0, half, random);
            if (length > half * 0.7F && random.nextFloat() < 0.6F) {
                int at = spine.length * 2 / 3;
                fork(spine[at][0], spine[at][1], heading + (0.5F + random.nextFloat() * 0.4F) * (random.nextBoolean() ? 1 : -1),
                        length * 0.35F, hub * 0.4F, length * 0.66F / half, half, random, 0);
            }
        }
    }

    /** A gaping wound opening like an eye: rings of vertices around the centre, parting across the long axis. */
    private void maw(float size, float width, RandomSource random) {
        ring(0, 0, width, size / 2, new float[]{1, 0.68F, 0.36F, 0}, 28, 0.2F, random, 0, size / 2, true);
    }

    /**
     * Rings around (x, y) with radii rx/ry, one per entry of {@code depths} (edge depth 1 at the centre ... 0 on the
     * outer ring), the outer ring torn by {@code tear}. {@code eye}: it opens across x like an eyelid (spine on the
     * long axis), else it grows out of the centre.
     */
    private void ring(float x, float y, float rx, float ry, float[] depths, int points, float tear, RandomSource random, float delayAtRoot,
                      float half, boolean eye) {
        float[] jag = new float[points];
        for (int i = 0; i < points; i++) {
            jag[i] = 1 + (random.nextFloat() * 2 - 1) * tear * (i % 2 == 0 ? 1 : 0.5F);
        }
        int centre = vertex(x, y, 0, 0, 0, 1, depths[0], delayAtRoot);
        int[] previous = null;
        int[] outer = null;
        for (int r = 1; r < depths.length; r++) {
            float share = 1 - depths[r] * 0.85F;
            if (r == depths.length - 1) {
                share = 1;
            }
            int[] current = new int[points + 1];
            for (int i = 0; i <= points; i++) {
                int k = i % points;
                float angle = k * Mth.TWO_PI / points;
                float j = r == depths.length - 1 ? jag[k] : 1 + (jag[k] - 1) * 0.4F;
                float px = Mth.cos(angle) * rx * share * j;
                float py = Mth.sin(angle) * ry * share * j;
                float len = Mth.sqrt(px * px + py * py);
                float outX = len < 1.0E-5F ? 0 : px / len;
                float outY = len < 1.0E-5F ? 1 : py / len;
                float d = delayAtRoot + Math.abs(eye ? py : len) / half;
                current[i] = eye
                        ? vertex(x, y + py, px, 0, outX, outY, depths[r], d)
                        : vertex(x, y, px, py, outX, outY, depths[r], d);
            }
            for (int i = 0; i < points; i++) {
                if (previous == null) {
                    quad(centre, current[i], current[i + 1], current[i + 1]);
                } else {
                    quad(previous[i], current[i], current[i + 1], previous[i + 1]);
                }
            }
            previous = current;
            outer = current;
        }
        rims.add(outer);
    }

    /** Splinters of torn space hanging around the opening. */
    private void shards(RiftShape shape, float size, RandomSource random) {
        int count = Math.min(10, 3 + Math.round(size * 1.1F));
        for (int i = 0; i < count; i++) {
            Vector3f at = shape.samplePoint(random, size, true).mul(1.1F + random.nextFloat() * 0.5F);
            at.z = (random.nextFloat() * 2 - 1) * (0.15F + size * 0.06F);
            float h = (0.07F + random.nextFloat() * 0.14F) * (0.7F + size * 0.08F);
            float w = h * (0.35F + random.nextFloat() * 0.25F);
            float[] corners = {
                    jitter(random, w), h, jitter(random, w),
                    -w + jitter(random, w), -h * 0.45F, w * 0.6F,
                    w * 0.9F, -h * (0.4F + random.nextFloat() * 0.3F), jitter(random, w),
                    jitter(random, w), -h * 0.3F, -w * 0.8F,
            };
            Vector3f axis = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F).normalize();
            float spin = (0.01F + random.nextFloat() * 0.025F) * (random.nextBoolean() ? 1 : -1);
            shards.add(new Shard(at.x, at.y, at.z, corners, axis.x, axis.y, axis.z, spin, random.nextFloat() * Mth.TWO_PI));
        }
    }

    private static float jitter(RandomSource random, float amount) {
        return (random.nextFloat() - 0.5F) * amount * 0.6F;
    }

    private int vertex(float x, float y, float offsetX, float offsetY, float outX, float outY, float depth, float when) {
        if (count == cx.length) {
            int grow = count * 2;
            cx = Arrays.copyOf(cx, grow);
            cy = Arrays.copyOf(cy, grow);
            ox = Arrays.copyOf(ox, grow);
            oy = Arrays.copyOf(oy, grow);
            nx = Arrays.copyOf(nx, grow);
            ny = Arrays.copyOf(ny, grow);
            edge = Arrays.copyOf(edge, grow);
            delay = Arrays.copyOf(delay, grow);
        }
        cx[count] = x;
        cy[count] = y;
        ox[count] = offsetX;
        oy[count] = offsetY;
        nx[count] = outX;
        ny[count] = outY;
        edge[count] = depth;
        delay[count] = when;
        return count++;
    }

    private void quad(int a, int b, int c, int d) {
        if (quadCount * 4 + 4 > quads.length) {
            quads = Arrays.copyOf(quads, quads.length * 2);
        }
        int at = quadCount * 4;
        quads[at] = a;
        quads[at + 1] = b;
        quads[at + 2] = c;
        quads[at + 3] = d;
        quadCount++;
    }
}
