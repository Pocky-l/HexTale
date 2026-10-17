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
 * Builds and caches the 72 boards (hexagonal parchments, six essences of the rune's theme around them). Generation is deterministic (the rune's key seeds it) and identical on client and
 * server, so boards never travel over the network. Every board is checked to be finishable with a plain drawing that
 * never steps on a curse, and its star thresholds come from that drawing's length.
 */
public final class RuneBoards {
    private static final int[] POINTS = {0, 3, 4, 6};
    private static final int[] WALL_CHAINS = {0, 10, 18, 26};
    /** Points ringed by dark sections, and the share of dark sections that are cursed, per chapter. */
    private static final int[] RINGS = {0, 0, 1, 2};
    private static final double[] CURSE_SHARE = {0, 0, 0.3, 0.4};
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
        List<Essence> essences = theme(rune, new Random32(hash(key + "#theme")));
        RuneBoard fallback = null;
        int fallbackMiss = Integer.MAX_VALUE;
        for (int attempt = 0; attempt < 300; attempt++) {
            Random32 random = new Random32(hash(key + "#thread#" + attempt));
            List<Essence> elements = new ArrayList<>(List.of(nearest(essences, rune.action()), nearest(essences, rune.target())));
            if (elements.get(1) == elements.get(0)) {
                elements.set(1, essences.get((essences.indexOf(elements.get(0)) + 3) % 6));
            }
            while (elements.size() < POINTS[tier]) {
                Essence next = essences.get(random.pick(essences.size()));
                if (!elements.contains(next) || elements.size() >= essences.size()) {
                    elements.add(next);
                }
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
            boolean[] dark = new boolean[HexGrid.COUNT];
            boolean[] cursed = new boolean[HexGrid.COUNT];
            if (tier >= 2) {
                // Later chapters: the dark runes become fields to cross, some points sit inside a ring of them.
                dark = walls;
                walls = new boolean[HexGrid.COUNT];
                darkFields(dark, cursed, points, tier, new Random32(hash(key + "#curse#" + attempt)));
            }
            RuneBoard draft = new RuneBoard(rune, essences, walls, dark, cursed, List.copyOf(points), null);
            int[] tree = plainTree(draft);
            if (tree == null) {
                continue;
            }
            int length = tree.length / 2;
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

    /** How many essences of each tier (primal, of two primals, deeper) a chapter's parchment holds. */
    private static final int[][] THEME_TIERS = {{}, {2, 4, 0}, {1, 3, 2}, {0, 2, 4}};

    /**
     * The six essences of a rune's parchment: all of its theme - essences made of the rune's action and target primals.
     * Chapter I keeps both primals and adds compounds of two primals; later chapters trade primals for deeper compounds,
     * which take more distilling to gather. Picks favour the most fitting essences, alternating between the action's
     * and the target's side, so both halves of the rune are always present; their order around the hexagon is shuffled.
     */
    static List<Essence> theme(ComplexRune rune, Random32 random) {
        Essence a = rune.action();
        Essence b = rune.target();
        int[] plan = THEME_TIERS[ResearchBook.chapter(rune)];
        List<Essence> picked = new ArrayList<>();
        if (plan[0] >= 1) {
            picked.add(a);
        }
        if (plan[0] >= 2) {
            picked.add(b);
        }
        for (int tier = 1; tier <= 2; tier++) {
            int want = plan[tier];
            List<double[]> scored = new ArrayList<>();
            for (Essence e : Essence.values()) {
                boolean fits = tier == 2 ? e.tier() >= 2 : e.tier() == 1;
                double fit = e.share(a) + e.share(b);
                if (fits && !picked.contains(e) && fit > 0) {
                    // Best fit first; a little randomness among equals so boards of related runes differ.
                    scored.add(new double[]{e.ordinal(), fit + (e.share(a) > 0 && e.share(b) > 0 ? 0.5 : 0) + random.next() * 0.2});
                }
            }
            scored.sort((x, y) -> Double.compare(y[1], x[1]));
            boolean actionSide = true;
            while (want > 0 && !scored.isEmpty()) {
                Essence side = actionSide ? a : b;
                int at = 0;
                for (int i = 0; i < scored.size(); i++) {
                    if (Essence.values()[(int) scored.get(i)[0]].share(side) > 0) {
                        at = i;
                        break;
                    }
                }
                picked.add(Essence.values()[(int) scored.remove(at)[0]]);
                actionSide = !actionSide;
                want--;
            }
        }
        // Top up from any essence of the theme if a tier ran short.
        for (Essence e : Essence.values()) {
            if (picked.size() >= 6) {
                break;
            }
            if (!picked.contains(e) && e.share(a) + e.share(b) > 0) {
                picked.add(e);
            }
        }
        for (int i = picked.size() - 1; i > 0; i--) {
            int j = random.pick(i + 1);
            Essence t = picked.get(i);
            picked.set(i, picked.get(j));
            picked.set(j, t);
        }
        return List.copyOf(picked.subList(0, 6));
    }

    /** The essence of the theme with the most of this primal in it. */
    private static Essence nearest(List<Essence> essences, Essence primal) {
        Essence best = essences.getFirst();
        for (Essence e : essences) {
            if (e.share(primal) > best.share(primal)) {
                best = e;
            }
        }
        return best;
    }

    /**
     * Rings the last points with dark sections, keeps the orb's neighbourhood clear and curses a share of the dark
     * sections. Every ring keeps two safe sections, so a point is never sealed off.
     */
    private static void darkFields(boolean[] dark, boolean[] cursed, List<RuneBoard.Point> points, int tier, Random32 random) {
        int rings = Math.min(RINGS[tier], points.size());
        for (int r = 0; r < rings; r++) {
            int centre = points.get(points.size() - 1 - r).cell();
            for (int next : HexGrid.neighbours(centre)) {
                dark[next] = true;
            }
        }
        for (RuneBoard.Point point : points) {
            dark[point.cell()] = false;
        }
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (HexGrid.steps(HexGrid.CENTRE, cell) <= 1) {
                dark[cell] = false;
            }
            cursed[cell] = dark[cell] && random.next() < CURSE_SHARE[tier];
        }
        for (int r = 0; r < rings; r++) {
            int[] ring = HexGrid.neighbours(points.get(points.size() - 1 - r).cell());
            int safe = 0;
            for (int cell : ring) {
                if (!cursed[cell]) {
                    safe++;
                }
            }
            for (int k = 0; safe < 2 && k < ring.length * 3; k++) {
                int cell = ring[random.pick(ring.length)];
                if (cursed[cell]) {
                    cursed[cell] = false;
                    safe++;
                }
            }
        }
    }

    private static RuneBoard withPar(RuneBoard draft, int plain) {
        return new RuneBoard(draft.rune(), draft.essences(), draft.walls(), draft.dark(), draft.cursed(), draft.points(),
                new RuneBoard.Par(plain, plain + Math.max(3, plain / 4), plain));
    }

    /**
     * A short drawing that reaches every point safely: the ink grows from the orb, each time out to the point nearest
     * to anything inked so far, along the shortest way around stone, cursed sections and the ink. Not always the very
     * shortest tree, but always a real one.
     *
     * @return the steps ({@link ResearchSheet} pairs), or null when a point cannot be reached
     */
    @Nullable
    public static int[] plainTree(RuneBoard board) {
        boolean[] inked = new boolean[HexGrid.COUNT];
        inked[HexGrid.CENTRE] = true;
        List<Integer> steps = new ArrayList<>();
        int[] previous = new int[HexGrid.COUNT];
        int[] queue = new int[HexGrid.COUNT];
        boolean[] reached = new boolean[board.points().size()];
        int left = reached.length;
        while (left > 0) {
            Arrays.fill(previous, -2);
            int head = 0;
            int tail = 0;
            for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                if (inked[cell]) {
                    previous[cell] = -1;
                    queue[tail++] = cell;
                }
            }
            int found = -1;
            while (head < tail && found < 0) {
                int cell = queue[head++];
                for (int next : HexGrid.neighbours(cell)) {
                    if (previous[next] == -2 && !board.wall(next) && !board.cursed(next)) {
                        previous[next] = cell;
                        queue[tail++] = next;
                        int point = board.pointAt(next);
                        if (point >= 0 && !reached[point]) {
                            found = next;
                            break;
                        }
                    }
                }
            }
            if (found < 0) {
                return null;
            }
            List<Integer> leg = new ArrayList<>();
            for (int cell = found; previous[cell] != -1; cell = previous[cell]) {
                leg.add(cell);
            }
            for (int i = leg.size() - 1; i >= 0; i--) {
                int cell = leg.get(i);
                steps.add(previous[cell]);
                steps.add(cell);
                inked[cell] = true;
                int point = board.pointAt(cell);
                if (point >= 0 && !reached[point]) {
                    reached[point] = true;
                    left--;
                }
            }
        }
        return steps.stream().mapToInt(Integer::intValue).toArray();
    }
}
