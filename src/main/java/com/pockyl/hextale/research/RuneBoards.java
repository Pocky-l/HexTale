package com.pockyl.hextale.research;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Builds and caches the 72 boards. Generation is deterministic (the rune's key seeds it) and identical on client and
 * server, so boards never travel over the network. Ported from the prototype: same random generator, same hash, same
 * order of picks, same solver.
 */
public final class RuneBoards {
    private static final int[] POINTS = {0, 2, 4, 6};
    private static final int[] WALL_CHAINS = {0, 7, 14, 20};
    /** Generation rejects boards a plain player cannot finish within this many symbols. */
    private static final int[] PLAIN_LIMIT = {0, 8, 14, 20};
    private static final int SEARCH_LIMIT = 60;
    /** Fork restarts a fully upgraded researcher has per sheet. */
    private static final int FULL_FORKS = 3;
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
            RuneBoard board = generate(rune);
            future.complete(board);
            return future.join();
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
        for (int attempt = 0; attempt < 150; attempt++) {
            Random32 random = new Random32(hash(key + "#" + attempt));
            List<SimpleRune> elements = new ArrayList<>(List.of(rune.action(), rune.target()));
            while (elements.size() < POINTS[tier]) {
                elements.add(HexGrid.RING[random.pick(HexGrid.RING.length)]);
            }
            SimpleRune[] walls = new SimpleRune[HexGrid.COUNT];
            for (int c = 0; c < WALL_CHAINS[tier]; c++) {
                int cell = random.pick(HexGrid.COUNT);
                SimpleRune element = random.next() < 0.65 ? elements.get(random.pick(elements.size()))
                        : HexGrid.RING[random.pick(HexGrid.RING.length)];
                for (int n = 2 + (int) Math.floor(random.next() * 4); n > 0; n--) {
                    if (HexGrid.radius(cell) > 0.22) {
                        walls[cell] = element;
                    }
                    int[] neighbours = HexGrid.neighbours(cell);
                    cell = neighbours[random.pick(neighbours.length)];
                }
            }
            List<Integer> candidates = new ArrayList<>();
            for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                if (walls[cell] == null && HexGrid.radius(cell) > 0.3) {
                    candidates.add(cell);
                }
            }
            List<RuneBoard.Point> points = new ArrayList<>();
            for (SimpleRune element : elements) {
                List<Integer> ok = candidates.stream()
                        .filter(cell -> points.stream().allMatch(p -> HexGrid.distance(p.cell(), cell) > 0.32)).toList();
                if (ok.isEmpty()) {
                    break;
                }
                points.add(new RuneBoard.Point(element, ok.get(random.pick(ok.size())), ResearchItems.resource(element, tier)));
            }
            if (points.size() < POINTS[tier]) {
                continue;
            }
            RuneBoard draft = new RuneBoard(rune, walls, List.copyOf(points), null);
            int plain = solve(draft, false, PLAIN_LIMIT[tier]);
            if (plain == Integer.MAX_VALUE) {
                continue;
            }
            boolean[] reach = reachablePlain(draft);
            boolean gated = points.stream().anyMatch(p -> !reach[p.cell()]);
            if (tier > 1 && !gated) {
                if (fallback == null) {
                    fallback = draft;
                }
                continue;
            }
            return withPar(draft);
        }
        if (fallback == null) {
            throw new IllegalStateException("No board for rune " + rune.key());
        }
        return withPar(fallback);
    }

    private static RuneBoard withPar(RuneBoard draft) {
        int plain = solve(draft, false, SEARCH_LIMIT);
        int tools = Math.min(plain, solve(draft, true, plain));
        int two = Math.max(plain + 1, tools + 2);
        return new RuneBoard(draft.rune(), draft.walls(), draft.points(),
                new RuneBoard.Par(tools, two, two + 1 + draft.tier(), plain));
    }

    /** Sections a plain sheet can reach from the orb without filling any point. */
    private static boolean[] reachablePlain(RuneBoard board) {
        boolean[] walls = board.activeWalls(0);
        boolean[] reach = new boolean[HexGrid.COUNT];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        reach[HexGrid.CENTRE] = true;
        queue.add(HexGrid.CENTRE);
        List<int[]> offsets = new ArrayList<>();
        for (ItemElements elements : ResearchItems.canonical().values()) {
            offsets.add(elements.offset(false));
        }
        while (!queue.isEmpty()) {
            int cell = queue.poll();
            for (int[] offset : offsets) {
                int end = HexGrid.walk(walls, cell, offset).end();
                if (!reach[end]) {
                    reach[end] = true;
                    queue.add(end);
                }
            }
        }
        return reach;
    }

    /**
     * Fewest symbols that fill every point. Each point is open, covered (a symbol lies on it) or filled (base-3 digit);
     * filling is free and may wait, and a filled point dissolves the dark runes of its element. With tools the mortar
     * adds half steps and the fork may restart from the orb or a filled point three times.
     *
     * @return the number of symbols, or {@link Integer#MAX_VALUE} when not possible within {@code limit}
     */
    static int solve(RuneBoard board, boolean tools, int limit) {
        int n = board.points().size();
        int[] pow = new int[n + 1];
        pow[0] = 1;
        for (int i = 1; i <= n; i++) {
            pow[i] = pow[i - 1] * 3;
        }
        int p3 = pow[n];
        int forks = tools ? FULL_FORKS : 0;
        int f1 = forks + 1;
        int goal = p3 - 1;
        int cells = HexGrid.COUNT;
        int[] pointIndex = new int[cells];
        Arrays.fill(pointIndex, -1);
        int[] pointCell = new int[n];
        for (int i = 0; i < n; i++) {
            pointCell[i] = board.points().get(i).cell();
            pointIndex[pointCell[i]] = i;
        }
        List<int[]> offsets = ResearchItems.canonicalOffsets(tools);
        int rays = offsets.size();
        int[][][] ray = new int[rays][cells][];
        for (int i = 0; i < rays; i++) {
            for (int c = 0; c < cells; c++) {
                ray[i][c] = HexGrid.ray(c, offsets.get(i));
            }
        }
        int[] dissolvedOf = new int[p3];
        for (int p = 0; p < p3; p++) {
            int filled = 0;
            for (int i = 0; i < n; i++) {
                if (digit(p, i, pow) == 2) {
                    filled |= 1 << i;
                }
            }
            dissolvedOf[p] = board.dissolved(filled);
        }
        Map<Integer, boolean[]> wallSets = new HashMap<>();
        Map<Integer, int[]> memos = new HashMap<>();
        boolean[] seen = new boolean[cells * p3 * f1];
        int[] current = new int[64];
        int size = 0;
        int start = (HexGrid.CENTRE * p3) * f1;
        seen[start] = true;
        current[size++] = start;
        for (int cost = 0; cost < limit && size > 0; cost++) {
            for (int x = 0; x < size; x++) {
                int st = current[x];
                int f = st % f1;
                int t = (st - f) / f1;
                int p = t % p3;
                int c = (t - p) / p3;
                if (p == goal) {
                    return cost;
                }
                for (int i = 0; i < n; i++) {
                    if (digit(p, i, pow) == 1) {
                        int k = ((c * p3) + p + pow[i]) * f1 + f;
                        if (!seen[k]) {
                            seen[k] = true;
                            if (size == current.length) {
                                current = Arrays.copyOf(current, size * 2);
                            }
                            current[size++] = k;
                        }
                    }
                }
                if (f < forks) {
                    int[] targets = new int[n + 1];
                    int count = 0;
                    targets[count++] = HexGrid.CENTRE;
                    for (int i = 0; i < n; i++) {
                        if (digit(p, i, pow) == 2) {
                            targets[count++] = pointCell[i];
                        }
                    }
                    for (int j = 0; j < count; j++) {
                        int k = ((targets[j] * p3) + p) * f1 + f + 1;
                        if (!seen[k]) {
                            seen[k] = true;
                            if (size == current.length) {
                                current = Arrays.copyOf(current, size * 2);
                            }
                            current[size++] = k;
                        }
                    }
                }
            }
            int[] next = new int[Math.max(64, size * 2)];
            int nextSize = 0;
            for (int x = 0; x < size; x++) {
                int st = current[x];
                int f = st % f1;
                int t = (st - f) / f1;
                int p = t % p3;
                int c = (t - p) / p3;
                int d = dissolvedOf[p];
                boolean[] wall = wallSets.computeIfAbsent(d, mask -> {
                    boolean[] w = new boolean[cells];
                    for (int i = 0; i < cells; i++) {
                        w[i] = board.walls()[i] != null && (mask & 1 << board.walls()[i].ordinal()) == 0;
                    }
                    return w;
                });
                int[] memo = memos.computeIfAbsent(d, mask -> {
                    int[] m = new int[cells * rays];
                    Arrays.fill(m, -1);
                    return m;
                });
                for (int i = 0; i < rays; i++) {
                    int e = memo[c * rays + i];
                    if (e < 0) {
                        int[] line = ray[i][c];
                        e = c;
                        for (int j = 0; j < line.length && !wall[line[j]]; j++) {
                            e = line[j];
                        }
                        memo[c * rays + i] = e;
                    }
                    int pi = pointIndex[e];
                    int np = pi >= 0 && digit(p, pi, pow) == 0 ? p + pow[pi] : p;
                    int k = ((e * p3) + np) * f1 + f;
                    if (!seen[k]) {
                        seen[k] = true;
                        if (nextSize == next.length) {
                            next = Arrays.copyOf(next, nextSize * 2);
                        }
                        next[nextSize++] = k;
                    }
                }
            }
            current = next;
            size = nextSize;
        }
        return Integer.MAX_VALUE;
    }

    private static int digit(int p, int i, int[] pow) {
        return p / pow[i] % 3;
    }
}
