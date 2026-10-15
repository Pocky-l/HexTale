package com.pockyl.hextale.research;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

import net.minecraft.world.item.Item;

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
 * server, so boards never travel over the network. Points and dark-rune chains are placed like in the prototype; every
 * board is checked to be finishable under the table's rules, and its star thresholds are the shortest routes.
 */
public final class RuneBoards {
    private static final int[] POINTS = {0, 2, 4, 6};
    private static final int[] WALL_CHAINS = {0, 7, 14, 20};
    /** Share of dark-rune chains that are mirrors. */
    private static final double[] MIRRORS = {0, 0.2, 0.3, 0.35};
    /** A plain player must be able to finish within this many symbols ... */
    private static final int[] PLAIN_LIMIT = {0, 8, 14, 20};
    /** ... and must need at least this many: harder runes are longer. */
    private static final int[] PLAIN_MIN = {0, 3, 6, 9};
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
        int fallbackPlain = Integer.MAX_VALUE;
        for (int attempt = 0; attempt < 200; attempt++) {
            Random32 random = new Random32(hash(key + "#walls#" + attempt));
            List<SimpleRune> elements = new ArrayList<>(List.of(rune.action(), rune.target()));
            while (elements.size() < POINTS[tier]) {
                elements.add(HexGrid.RING[random.pick(HexGrid.RING.length)]);
            }
            WallKind[] walls = new WallKind[HexGrid.COUNT];
            for (int c = 0; c < WALL_CHAINS[tier]; c++) {
                int cell = random.pick(HexGrid.COUNT);
                WallKind kind = random.next() < MIRRORS[tier] ? WallKind.MIRROR : WallKind.STONE;
                for (int n = 2 + (int) Math.floor(random.next() * 4); n > 0; n--) {
                    if (HexGrid.radius(cell) > 0.22) {
                        walls[cell] = kind;
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
            List<Item> route = route(draft, PLAIN_LIMIT[tier]);
            if (route == null) {
                continue;
            }
            int plain = route.size();
            if (plain >= PLAIN_MIN[tier]) {
                return withPar(draft, plain);
            }
            if (fallback == null || plain > fallbackPlain) {
                fallback = draft;
                fallbackPlain = plain;
            }
        }
        if (fallback == null) {
            throw new IllegalStateException("No board for rune " + rune.key());
        }
        return withPar(fallback, fallbackPlain);
    }

    private static RuneBoard withPar(RuneBoard draft, int plain) {
        int tools = Math.min(plain, solve(draft, true, plain));
        int two = Math.max(plain + 1, tools + 2);
        return new RuneBoard(draft.rune(), draft.walls(), draft.points(), new RuneBoard.Par(tools, two, two + 1 + draft.tier(), plain));
    }

    /**
     * Fewest symbols that cover every point under the table's rules: two symbols of the same element never follow each
     * other, and a symbol never lands where it started or on the orb. (Leaving resources is free and changes nothing on
     * the parchment, so covering is what counts; landing on an older symbol is not tracked - the shortest routes hardly
     * ever cross themselves.) With tools the mortar adds half steps and the fork may restart from the orb or a covered
     * point three times.
     *
     * @return the number of symbols, or {@link Integer#MAX_VALUE} when not possible within {@code limit}
     */
    static int solve(RuneBoard board, boolean tools, int limit) {
        int n = board.points().size();
        int masks = 1 << n;
        int goal = masks - 1;
        int forks = tools ? FULL_FORKS : 0;
        int f1 = forks + 1;
        int lasts = SimpleRune.values().length + 1;
        int none = lasts - 1;
        int cells = HexGrid.COUNT;
        int[] pointIndex = new int[cells];
        Arrays.fill(pointIndex, -1);
        int[] pointCell = new int[n];
        for (int i = 0; i < n; i++) {
            pointCell[i] = board.points().get(i).cell();
            pointIndex[pointCell[i]] = i;
        }
        List<ResearchItems.Step> steps = ResearchItems.canonicalSteps(tools);
        int rays = steps.size();
        int[] end = new int[cells * rays];
        for (int c = 0; c < cells; c++) {
            for (int i = 0; i < rays; i++) {
                end[c * rays + i] = HexGrid.walk(board.walls(), c, steps.get(i).offset()).end();
            }
        }
        boolean[] seen = new boolean[cells * masks * f1 * lasts];
        int[] current = new int[64];
        int size = 0;
        int start = encode(HexGrid.CENTRE, 0, 0, none, masks, f1, lasts);
        seen[start] = true;
        current[size++] = start;
        for (int cost = 0; cost <= limit && size > 0; cost++) {
            // Free moves first: a fork restart.
            for (int x = 0; x < size; x++) {
                int st = current[x];
                int last = st % lasts;
                int f = st / lasts % f1;
                int mask = st / lasts / f1 % masks;
                int c = st / lasts / f1 / masks;
                if (mask == goal) {
                    return cost;
                }
                if (f < forks) {
                    for (int j = -1; j < n; j++) {
                        if (j >= 0 && (mask & 1 << j) == 0) {
                            continue;
                        }
                        int target = j < 0 ? HexGrid.CENTRE : pointCell[j];
                        int k = encode(target, mask, f + 1, last, masks, f1, lasts);
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
            if (cost == limit) {
                break;
            }
            int[] next = new int[Math.max(64, size * 2)];
            int nextSize = 0;
            for (int x = 0; x < size; x++) {
                int st = current[x];
                int last = st % lasts;
                int f = st / lasts % f1;
                int mask = st / lasts / f1 % masks;
                int c = st / lasts / f1 / masks;
                for (int i = 0; i < rays; i++) {
                    int element = steps.get(i).element();
                    int e = end[c * rays + i];
                    if (element == last || e == c || e == HexGrid.CENTRE) {
                        continue;
                    }
                    int pi = pointIndex[e];
                    int nm = pi >= 0 ? mask | 1 << pi : mask;
                    int k = encode(e, nm, f, element, masks, f1, lasts);
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

    /**
     * A plain route that really finishes the board: canonical items whose symbols cover every point within
     * {@code limit} symbols under all the table's rules, including that a symbol never lands on an older one (which
     * {@link #solve} does not track). Iterative deepening depth-first search, pruned by the distances under solve's
     * rules (never longer than the real ones). Leaving resources is free and left out.
     *
     * @return the items in writing order, a shortest such route; null when none is found within {@code limit}
     */
    @Nullable
    public static List<Item> route(RuneBoard board, int limit) {
        List<Item> items = new ArrayList<>();
        List<int[]> offsets = new ArrayList<>();
        for (Map.Entry<Item, ItemElements> entry : ResearchItems.canonical().entrySet()) {
            int[] offset = entry.getValue().offset(false);
            if (offset[0] != 0 || offset[1] != 0) {
                items.add(entry.getKey());
                offsets.add(offset);
            }
        }
        Route search = new Route(board, items, offsets);
        int start = search.state(HexGrid.CENTRE, 0, search.none);
        if (search.distance[start] == Integer.MAX_VALUE) {
            return null;
        }
        for (int budget = search.distance[start]; budget <= limit; budget++) {
            Arrays.fill(search.taken, false);
            search.taken[HexGrid.CENTRE] = true;
            if (search.deepen(HexGrid.CENTRE, 0, search.none, 0, budget)) {
                List<Item> out = new ArrayList<>();
                for (int i = 0; i < budget; i++) {
                    out.add(items.get(search.path[i]));
                }
                return out;
            }
            if (search.nodes > Route.NODE_LIMIT) {
                return null;
            }
        }
        return null;
    }

    /** The search behind {@link #route}. */
    private static final class Route {
        static final int NODE_LIMIT = 2_000_000;
        final int cells = HexGrid.COUNT;
        final int k;
        final int masks;
        final int goal;
        final int lasts = SimpleRune.values().length + 1;
        final int none = lasts - 1;
        final int[] element;
        final int[] end;
        final int[] pointIndex = new int[HexGrid.COUNT];
        /** Fewest symbols to cover every point from (cell, covered, last element) under solve's rules. */
        final int[] distance;
        final boolean[] taken = new boolean[HexGrid.COUNT];
        final int[] path = new int[SEARCH_LIMIT + 1];
        long nodes;

        Route(RuneBoard board, List<Item> items, List<int[]> offsets) {
            k = items.size();
            int n = board.points().size();
            masks = 1 << n;
            goal = masks - 1;
            element = new int[k];
            end = new int[cells * k];
            Arrays.fill(pointIndex, -1);
            for (int i = 0; i < n; i++) {
                pointIndex[board.points().get(i).cell()] = i;
            }
            List<List<int[]>> into = new ArrayList<>();
            for (int c = 0; c < cells; c++) {
                into.add(new ArrayList<>());
            }
            for (int i = 0; i < k; i++) {
                element[i] = ResearchItems.of(items.get(i)).main().ordinal();
                for (int c = 0; c < cells; c++) {
                    int e = HexGrid.walk(board.walls(), c, offsets.get(i)).end();
                    end[c * k + i] = e;
                    if (e != c && e != HexGrid.CENTRE) {
                        into.get(e).add(new int[]{c, i});
                    }
                }
            }
            // Breadth-first backwards from every state with all points covered.
            distance = new int[cells * masks * lasts];
            Arrays.fill(distance, Integer.MAX_VALUE);
            int[] queue = new int[distance.length];
            int head = 0;
            int tail = 0;
            for (int c = 0; c < cells; c++) {
                for (int last = 0; last < lasts; last++) {
                    int st = state(c, goal, last);
                    distance[st] = 0;
                    queue[tail++] = st;
                }
            }
            while (head < tail) {
                int st = queue[head++];
                int last = st % lasts;
                int mask = st / lasts % masks;
                int e = st / lasts / masks;
                int point = pointIndex[e];
                if (last == none || point >= 0 && (mask & 1 << point) == 0) {
                    continue;
                }
                int[] befores = point >= 0 ? new int[]{mask, mask & ~(1 << point)} : new int[]{mask};
                for (int[] move : into.get(e)) {
                    if (element[move[1]] != last) {
                        continue;
                    }
                    for (int before : befores) {
                        for (int l = 0; l < lasts; l++) {
                            int from = state(move[0], before, l);
                            if (l != last && distance[from] == Integer.MAX_VALUE) {
                                distance[from] = distance[st] + 1;
                                queue[tail++] = from;
                            }
                        }
                    }
                }
            }
        }

        int state(int cell, int mask, int last) {
            return (cell * masks + mask) * lasts + last;
        }

        boolean deepen(int cell, int mask, int last, int depth, int budget) {
            if (mask == goal) {
                return true;
            }
            int left = distance[state(cell, mask, last)];
            if (left == Integer.MAX_VALUE || depth + left > budget || ++nodes > NODE_LIMIT) {
                return false;
            }
            for (int i = 0; i < k; i++) {
                int e = end[cell * k + i];
                if (element[i] == last || e == cell || taken[e]) {
                    continue;
                }
                int point = pointIndex[e];
                taken[e] = true;
                path[depth] = i;
                boolean found = deepen(e, point >= 0 ? mask | 1 << point : mask, element[i], depth + 1, budget);
                taken[e] = false;
                if (found) {
                    return true;
                }
            }
            return false;
        }
    }

    private static int encode(int cell, int mask, int forks, int last, int masks, int f1, int lasts) {
        return ((cell * masks + mask) * f1 + forks) * lasts + last;
    }
}
