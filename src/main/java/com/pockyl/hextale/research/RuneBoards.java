package com.pockyl.hextale.research;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.Nullable;

/**
 * Builds and caches the 72 boards. Generation is deterministic (the rune's key seeds it) and identical on client and
 * server, so boards never travel over the network. Every board is checked to be finishable with a plain thread, and its
 * star thresholds come from that thread's length.
 */
public final class RuneBoards {
    private static final int[] POINTS = {0, 3, 4, 6};
    private static final int[] WALL_CHAINS = {0, 10, 18, 26};
    /** The plain thread of a board is this long at least ... */
    private static final int[] PLAIN_MIN = {0, 14, 22, 32};
    /** ... and at most: harder runes are longer. */
    private static final int[] PLAIN_MAX = {0, 30, 46, 66};
    private static final Map<ComplexRune, CompletableFuture<RuneBoard>> CACHE = new ConcurrentHashMap<>();
    /**
     * One low-priority daemon thread of our own: the game's background pool also loads chunks, and 72 boards queued
     * there at world start would hold the world back.
     */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "HexTale research boards");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private RuneBoards() {
    }

    /** The board; generated right here if it is not ready yet, rather than waiting for the queue. */
    public static RuneBoard get(ComplexRune rune) {
        CompletableFuture<RuneBoard> future = request(rune);
        if (!future.isDone()) {
            future.complete(generate(rune));
        }
        return future.join();
    }

    /** The board, generated in the background if needed. */
    public static CompletableFuture<RuneBoard> request(ComplexRune rune) {
        return CACHE.computeIfAbsent(rune, r -> CompletableFuture.supplyAsync(() -> generate(r), EXECUTOR));
    }

    /** Generates every board in the background, so screens rarely wait. */
    public static void warmUp() {
        long start = System.nanoTime();
        CompletableFuture.allOf(ComplexRunes.all().stream().map(RuneBoards::request).toArray(CompletableFuture[]::new))
                .thenRun(() -> Hextale.LOGGER.debug("Generated {} research boards in {} ms", ComplexRunes.COUNT,
                        (System.nanoTime() - start) / 1_000_000));
    }

    /** The prototype's mulberry32. */
    private static final class Random32 {
        private int a;

        Random32(int seed) {
            a = seed;
        }

        double next() {
            a = a + 0x6D2B79F5;
            int t = (a ^ a >>> 15) * (1 | a);
            t = t + (t ^ t >>> 7) * (61 | t) ^ t;
            return ((t ^ t >>> 14) & 0xFFFFFFFFL) / 4294967296.0;
        }

        int pick(int size) {
            return (int) Math.floor(next() * size);
        }
    }

    /** FNV-1a over UTF-16 units, like the prototype's {@code strHash}. */
    private static int hash(String text) {
        int h = (int) 2166136261L;
        for (int i = 0; i < text.length(); i++) {
            h = (h ^ text.charAt(i)) * 16777619;
        }
        return h;
    }

    static RuneBoard generate(ComplexRune rune) {
        int tier = ResearchBook.chapter(rune);
        String key = rune.action().id() + ">" + rune.target().id();
        RuneBoard fallback = null;
        int fallbackMiss = Integer.MAX_VALUE;
        for (int attempt = 0; attempt < 300; attempt++) {
            Random32 random = new Random32(hash(key + "#thread#" + attempt));
            List<Essence> elements = new ArrayList<>(List.of(rune.action(), rune.target()));
            while (elements.size() < POINTS[tier]) {
                elements.add(HexGrid.RING[random.pick(HexGrid.RING.length)]);
            }
            boolean[] walls = new boolean[HexGrid.COUNT];
            for (int c = 0; c < WALL_CHAINS[tier]; c++) {
                int cell = random.pick(HexGrid.COUNT);
                for (int n = 2 + (int) Math.floor(random.next() * 4); n > 0; n--) {
                    if (HexGrid.radius(cell) > 0.22) {
                        walls[cell] = true;
                    }
                    int[] neighbours = HexGrid.neighbours(cell);
                    cell = neighbours[random.pick(neighbours.length)];
                }
            }
            List<Integer> candidates = new ArrayList<>();
            for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                if (!walls[cell] && HexGrid.radius(cell) > 0.3) {
                    candidates.add(cell);
                }
            }
            List<RuneBoard.Point> points = new ArrayList<>();
            for (Essence element : elements) {
                List<Integer> ok = candidates.stream()
                        .filter(cell -> points.stream().allMatch(p -> HexGrid.distance(p.cell(), cell) > 0.32)).toList();
                if (ok.isEmpty()) {
                    break;
                }
                points.add(new RuneBoard.Point(element, ok.get(random.pick(ok.size()))));
            }
            if (points.size() < POINTS[tier]) {
                continue;
            }
            RuneBoard draft = new RuneBoard(rune, walls, List.copyOf(points), null);
            int[] thread = plainThread(draft);
            if (thread == null) {
                continue;
            }
            int length = thread.length - 1;
            int miss = Math.max(0, PLAIN_MIN[tier] - length) + Math.max(0, length - PLAIN_MAX[tier]);
            if (miss == 0) {
                return withPar(draft, length);
            }
            if (miss < fallbackMiss) {
                fallback = withPar(draft, length);
                fallbackMiss = miss;
            }
        }
        if (fallback == null) {
            throw new IllegalStateException("No board for rune " + rune.key());
        }
        return fallback;
    }

    private static RuneBoard withPar(RuneBoard draft, int plain) {
        return new RuneBoard(draft.rune(), draft.walls(), draft.points(), new RuneBoard.Par(plain, plain + Math.max(3, plain / 4), plain));
    }

    /**
     * A short thread through every point without tools: every order of the points is tried, each leg the shortest way
     * around dark runes and the thread laid so far. Not always the very shortest thread (a leg never bends to make room
     * for a later one), but always a real one.
     *
     * @return the thread from the orb, or null when no order works
     */
    @Nullable
    public static int[] plainThread(RuneBoard board) {
        int n = board.points().size();
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        int[] best = null;
        do {
            int[] thread = lay(board, order, best == null ? Integer.MAX_VALUE : best.length);
            if (thread != null && (best == null || thread.length < best.length)) {
                best = thread;
            }
        } while (nextPermutation(order));
        return best;
    }

    /** Lays the thread leg by leg in this order of points; null when blocked or not shorter than {@code limit}. */
    @Nullable
    private static int[] lay(RuneBoard board, int[] order, int limit) {
        boolean[] used = new boolean[HexGrid.COUNT];
        used[HexGrid.CENTRE] = true;
        List<Integer> thread = new ArrayList<>(List.of(HexGrid.CENTRE));
        int[] previous = new int[HexGrid.COUNT];
        int[] queue = new int[HexGrid.COUNT];
        for (int point : order) {
            int target = board.points().get(point).cell();
            if (used[target]) {
                continue;
            }
            int from = thread.get(thread.size() - 1);
            Arrays.fill(previous, -2);
            previous[from] = -1;
            int head = 0;
            int tail = 0;
            queue[tail++] = from;
            while (head < tail && previous[target] == -2) {
                int cell = queue[head++];
                for (int next : HexGrid.neighbours(cell)) {
                    if (previous[next] == -2 && !used[next] && !board.wall(next)) {
                        previous[next] = cell;
                        queue[tail++] = next;
                    }
                }
            }
            if (previous[target] == -2) {
                return null;
            }
            List<Integer> leg = new ArrayList<>();
            for (int cell = target; cell != from; cell = previous[cell]) {
                leg.add(cell);
            }
            for (int i = leg.size() - 1; i >= 0; i--) {
                used[leg.get(i)] = true;
                thread.add(leg.get(i));
            }
            if (thread.size() >= limit) {
                return null;
            }
        }
        return thread.stream().mapToInt(Integer::intValue).toArray();
    }

    private static boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) {
            i--;
        }
        if (i < 0) {
            return false;
        }
        int j = a.length - 1;
        while (a[j] <= a[i]) {
            j--;
        }
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
        for (int l = i + 1, r = a.length - 1; l < r; l++, r--) {
            t = a[l];
            a[l] = a[r];
            a[r] = t;
        }
        return true;
    }
}
