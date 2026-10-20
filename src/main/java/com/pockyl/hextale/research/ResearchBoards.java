package com.pockyl.hextale.research;

import com.pockyl.hextale.Hextale;
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
 * Builds and caches the research boards (hexagonal parchments, six essences of the topic's theme around them).
 * Generation is deterministic (the topic's id and the researcher's attempt seed it) and identical on client and
 * server, so boards never travel over the network. Every burnt parchment makes way for a new board: attempt 0 is the
 * topic's first board, attempt n its n-th replacement. Every board is checked to be solvable without a guess (see
 * {@link #deducible}) and finishable with a plain drawing that never steps on a curse; its star thresholds come from that
 * drawing's length.
 */
public final class ResearchBoards {
    private static final int[] POINTS = {0, 3, 4, 6};
    /** Chains of stone dark runes: only chapter I has stone, deeper parchments are dark fields instead. */
    private static final int[] WALL_CHAINS = {0, 8, 0, 0};
    /** The share of the open parchment that lies in dark fields, per tier: chapter III is nearly all dark. */
    private static final double[] DARK_SHARE = {0, 0.3, 0.55, 0.8};
    /** The share of dark sections that are cursed, per tier. */
    private static final double[] CURSE_SHARE = {0, 0.16, 0.19, 0.21};
    /** The orb's surroundings up to this many steps stay open, so the first lines show the first counts. */
    private static final int CLEARING = 2;
    /** Curse layouts tried on one dark field before the round gives up on it. */
    private static final int CURSE_TRIES = 40;
    /** The plain thread of a board is this long at least ... */
    private static final int[] PLAIN_MIN = {0, 14, 22, 32};
    /** ... and at most: deeper topics are longer. */
    private static final int[] PLAIN_MAX = {0, 30, 46, 66};
    private static final Map<Key, CompletableFuture<ResearchBoard>> CACHE = new ConcurrentHashMap<>();
    /**
     * One low-priority daemon thread of our own: the game's background pool also loads chunks, and boards queued
     * there at world start would hold the world back.
     */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "HexTale research boards");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private ResearchBoards() {
    }

    private record Key(ResearchTopic topic, int attempt) {
    }

    /** The topic's first board. */
    public static ResearchBoard get(ResearchTopic topic) {
        return get(topic, 0);
    }

    /** The board of an attempt; generated right here if it is not ready yet, rather than waiting for the queue. */
    public static ResearchBoard get(ResearchTopic topic, int attempt) {
        CompletableFuture<ResearchBoard> future = request(topic, attempt);
        if (!future.isDone()) {
            future.complete(generate(topic, attempt));
        }
        return future.join();
    }

    public static CompletableFuture<ResearchBoard> request(ResearchTopic topic) {
        return request(topic, 0);
    }

    /** The board of an attempt, generated in the background if needed. */
    public static CompletableFuture<ResearchBoard> request(ResearchTopic topic, int attempt) {
        return CACHE.computeIfAbsent(new Key(topic, Math.max(0, attempt)),
                key -> CompletableFuture.supplyAsync(() -> generate(key.topic(), key.attempt()), EXECUTOR));
    }

    /** Generates every board in the background, so screens rarely wait. */
    public static void warmUp() {
        long start = System.nanoTime();
        CompletableFuture.allOf(ResearchTopics.table().stream().map(ResearchBoards::request).toArray(CompletableFuture[]::new))
                .thenRun(() -> Hextale.LOGGER.debug("Generated {} research boards in {} ms", ResearchTopics.table().size(),
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

    static ResearchBoard generate(ResearchTopic topic, int attempt) {
        int tier = topic.tier();
        // The first attempt keeps the seed boards always had; later ones are seeded apart.
        String key = attempt == 0 ? topic.id() : topic.id() + "@" + attempt;
        List<Essence> essences = theme(topic, new Random32(hash(key + "#theme")));
        ResearchBoard fallback = null;
        int fallbackMiss = Integer.MAX_VALUE;
        for (int round = 0; round < 300; round++) {
            Random32 random = new Random32(hash(key + "#thread#" + round));
            List<Essence> elements = new ArrayList<>(List.of(nearest(essences, topic.action()), nearest(essences, topic.target())));
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
            List<ResearchBoard.Point> points = new ArrayList<>();
            for (Essence element : elements) {
                List<Integer> ok = candidates.stream()
                        .filter(cell -> points.stream().allMatch(p -> HexGrid.distance(p.cell(), cell) > 0.32)).toList();
                if (ok.isEmpty()) {
                    break;
                }
                // The corners hold the ink wells: a point drawn there moves on to the next candidate.
                int at = random.pick(ok.size());
                for (int k = 0; k < ok.size() && HexGrid.isCorner(ok.get(at)); k++) {
                    at = (at + 1) % ok.size();
                }
                points.add(new ResearchBoard.Point(element, ok.get(at)));
            }
            for (int d = 0; d < 6; d++) {
                walls[HexGrid.cornerCell(d)] = false;
            }
            if (points.size() < POINTS[tier]) {
                continue;
            }
            boolean[] dark = darkFields(walls, points, tier, new Random32(hash(key + "#dark#" + round)));
            Random32 curseRandom = new Random32(hash(key + "#curse#" + round));
            boolean[] cursed = null;
            for (int t = 0; t < CURSE_TRIES && cursed == null; t++) {
                boolean[] tried = curses(dark, tier, curseRandom);
                if (deducible(new ResearchBoard(topic, essences, walls, dark, tried, List.copyOf(points), null))) {
                    cursed = tried;
                }
            }
            if (cursed == null) {
                continue;
            }
            ResearchBoard draft = new ResearchBoard(topic, essences, walls, dark, cursed, List.copyOf(points), null);
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
            throw new IllegalStateException("No board for topic " + topic.id());
        }
        return fallback;
    }

    /** How many essences of each tier (primal, of two primals, deeper) a tier's parchment holds. */
    private static final int[][] THEME_TIERS = {{}, {2, 4, 0}, {1, 3, 2}, {0, 2, 4}};

    /**
     * The six essences of a topic's parchment: all of its theme - essences made of the topic's two primals.
     * Tier 1 keeps both primals and adds compounds of two primals; deeper tiers trade primals for deeper compounds,
     * which take more distilling to gather. Picks favour the most fitting essences, alternating between the action's
     * and the target's side, so both halves of the theme are always present; their order around the hexagon is shuffled.
     */
    static List<Essence> theme(ResearchTopic topic, Random32 random) {
        Essence a = topic.action();
        Essence b = topic.target();
        int[] plan = THEME_TIERS[topic.tier()];
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
                    // Best fit first; a little randomness among equals so boards of related topics differ.
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
     * Dark fields: a few blobs of dark ink spread over the parchment until they cover the tier's share of it. The orb's
     * clearing, the points, the stone and the corners stay open.
     */
    private static boolean[] darkFields(boolean[] walls, List<ResearchBoard.Point> points, int tier, Random32 random) {
        int blobs = 3 + 2 * tier;
        double[][] centres = new double[blobs][3];
        for (double[] blob : centres) {
            int cell = random.pick(HexGrid.COUNT);
            blob[0] = HexGrid.cx(cell);
            blob[1] = HexGrid.cy(cell);
            blob[2] = 0.25 + random.next() * 0.35;
        }
        List<Integer> open = new ArrayList<>();
        double[] depth = new double[HexGrid.COUNT];
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            int at = cell;
            if (walls[cell] || HexGrid.isCorner(cell) || HexGrid.steps(HexGrid.CENTRE, cell) <= CLEARING
                    || points.stream().anyMatch(p -> p.cell() == at)) {
                continue;
            }
            double d = 0;
            for (double[] blob : centres) {
                d = Math.max(d, 1 - Math.hypot(HexGrid.cx(cell) - blob[0], HexGrid.cy(cell) - blob[1]) / blob[2]);
            }
            depth[cell] = d + random.next() * 0.3;
            open.add(cell);
        }
        open.sort((x, y) -> Double.compare(depth[y], depth[x]));
        boolean[] dark = new boolean[HexGrid.COUNT];
        int count = (int) Math.round(open.size() * DARK_SHARE[tier]);
        for (int i = 0; i < count; i++) {
            dark[open.get(i)] = true;
        }
        return dark;
    }

    /** Curses a share of the dark sections. */
    private static boolean[] curses(boolean[] dark, int tier, Random32 random) {
        boolean[] cursed = new boolean[HexGrid.COUNT];
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            cursed[cell] = dark[cell] && random.next() < CURSE_SHARE[tier];
        }
        return cursed;
    }

    /**
     * Whether a careful researcher reaches every point without a guess. Inking is free until the drawing is finished and
     * the counts stay known, so they may probe every section they know is safe: plain parchment, and dark sections the
     * counts prove clean. A count proves its unknown neighbours clean when its curses are all found, cursed when they are
     * exactly as many as its missing curses; a count whose unknowns lie within another's settles the difference too.
     */
    public static boolean deducible(ResearchBoard board) {
        boolean[] inked = new boolean[HexGrid.COUNT];
        boolean[] safe = new boolean[HexGrid.COUNT];
        boolean[] cursed = new boolean[HexGrid.COUNT];
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            safe[cell] = !board.wall(cell) && !board.dark(cell);
        }
        int[] queue = new int[HexGrid.COUNT];
        inked[HexGrid.CENTRE] = true;
        boolean progress = true;
        while (progress) {
            progress = false;
            // Ink every safe section the ink can reach.
            int head = 0;
            int tail = 0;
            for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                if (inked[cell]) {
                    queue[tail++] = cell;
                }
            }
            while (head < tail) {
                for (int next : HexGrid.neighbours(queue[head++])) {
                    if (!inked[next] && safe[next]) {
                        inked[next] = true;
                        queue[tail++] = next;
                        progress = true;
                    }
                }
            }
            // Read the counts.
            List<int[]> unknowns = new ArrayList<>();
            List<Integer> missing = new ArrayList<>();
            for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                // Goals carry their sigil, not a count.
                if (!inked[cell] || !board.nearDark(cell) || board.pointAt(cell) >= 0) {
                    continue;
                }
                int found = 0;
                List<Integer> open = new ArrayList<>();
                for (int next : HexGrid.neighbours(cell)) {
                    if (cursed[next]) {
                        found++;
                    } else if (board.dark(next) && !safe[next]) {
                        open.add(next);
                    }
                }
                if (open.isEmpty()) {
                    continue;
                }
                int left = board.danger(cell) - found;
                if (left == 0 || left == open.size()) {
                    for (int next : open) {
                        if (left == 0) {
                            safe[next] = true;
                        } else {
                            cursed[next] = true;
                        }
                    }
                    progress = true;
                } else {
                    unknowns.add(open.stream().mapToInt(Integer::intValue).sorted().toArray());
                    missing.add(left);
                }
            }
            for (int i = 0; i < unknowns.size() && !progress; i++) {
                for (int j = 0; j < unknowns.size() && !progress; j++) {
                    int[] a = unknowns.get(i);
                    int[] b = unknowns.get(j);
                    if (a.length >= b.length || !containsAll(b, a)) {
                        continue;
                    }
                    int need = missing.get(j) - missing.get(i);
                    if (need == 0 || need == b.length - a.length) {
                        for (int cell : b) {
                            if (Arrays.binarySearch(a, cell) < 0) {
                                if (need == 0) {
                                    safe[cell] = true;
                                } else {
                                    cursed[cell] = true;
                                }
                            }
                        }
                        progress = true;
                    }
                }
            }
        }
        for (ResearchBoard.Point point : board.points()) {
            if (!inked[point.cell()]) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsAll(int[] sorted, int[] items) {
        for (int item : items) {
            if (Arrays.binarySearch(sorted, item) < 0) {
                return false;
            }
        }
        return true;
    }

    private static ResearchBoard withPar(ResearchBoard draft, int plain) {
        return new ResearchBoard(draft.topic(), draft.essences(), draft.walls(), draft.dark(), draft.cursed(), draft.points(),
                new ResearchBoard.Par(plain, plain + Math.max(3, plain / 4), plain));
    }

    /**
     * A short drawing that reaches every point safely: the ink grows from the orb, each time out to the point nearest
     * to anything inked so far, along the shortest way around stone, cursed sections and the ink. Not always the very
     * shortest tree, but always a real one.
     *
     * @return the steps ({@link ResearchSheet} pairs), or null when a point cannot be reached
     */
    @Nullable
    public static int[] plainTree(ResearchBoard board) {
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
