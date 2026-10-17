package com.pockyl.hextale.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

/**
 * Studying an item: "the prism". The item's essences hide as bodies of light inside a small hexagonal field in the
 * middle of the parchment - a strong essence ({@value #HEAVY} or more) as a big body of seven sections, a weak one as a
 * single section. Around the field runs the rim; from its ports the researcher shines rays inward.
 * <p>
 * A ray runs straight. Running into a body it is absorbed (a hit). A body beside the section ahead bends it away by 60
 * degrees; bodies on both sides turn it back. A ray bent at the very rim comes straight back out (a reflection). A ray
 * that gets through leaves at another port. What the researcher sees is only where each ray went in and came out (or
 * that it was absorbed or reflected), its path, tinted with the colours of the essences it touched, and the mist lifting
 * from the sections it proved empty.
 * <p>
 * From that the researcher works out where the bodies lie and checks a section: a body there is revealed, an empty
 * section costs a ray. Rays are limited: {@value #BASE_RAYS} plus {@value #RAYS_PER_ESSENCE} per essence. All bodies
 * revealed = the item is studied.
 * <p>
 * Pure rules shared by the table (server) and its screen (client copy): the bodies follow from the item and a seed
 * chosen when the study begins.
 */
public final class ItemStudy {
    /** Sections from the centre the bodies can lie within. */
    public static final int FIELD = 4;
    /** Ring of the rim, where the ports are. */
    public static final int RIM = FIELD + 1;
    /** An essence this strong is a big body (the section and its six neighbours). */
    public static final int HEAVY = 10;
    public static final int MAX_BODIES = 6;
    public static final int BASE_RAYS = 6;
    public static final int RAYS_PER_ESSENCE = 2;
    /** Ports in rim order (clockwise around the field), each {@code rimCell * 6 + direction}. */
    private static final int[] PORTS;
    private static final int[] PORT_INDEX = new int[HexGrid.COUNT * 6];

    static {
        Arrays.fill(PORT_INDEX, -1);
        List<int[]> ports = new ArrayList<>();
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (HexGrid.steps(cell, HexGrid.CENTRE) != RIM) {
                continue;
            }
            for (int d = 0; d < 6; d++) {
                int next = HexGrid.step(cell, d);
                if (next >= 0 && inField(next)) {
                    ports.add(new int[]{cell, d});
                }
            }
        }
        // Clockwise from the top: by the angle of the point between the rim section and the section it shines into.
        ports.sort((a, b) -> Double.compare(angle(a), angle(b)));
        PORTS = new int[ports.size()];
        for (int i = 0; i < PORTS.length; i++) {
            PORTS[i] = ports.get(i)[0] * 6 + ports.get(i)[1];
            PORT_INDEX[PORTS[i]] = i;
        }
    }

    private static double angle(int[] port) {
        int next = HexGrid.step(port[0], port[1]);
        double x = (HexGrid.cx(port[0]) + HexGrid.cx(next)) / 2;
        double y = (HexGrid.cy(port[0]) + HexGrid.cy(next)) / 2;
        double a = Math.atan2(x, -y);
        return a < 0 ? a + Math.PI * 2 : a;
    }

    /** An essence's body of light. */
    public record Body(Essence essence, int amount, int centre, boolean heavy) {
        public boolean covers(int cell) {
            return cell == centre || heavy && HexGrid.adjacent(cell, centre);
        }

        public int[] cells() {
            if (!heavy) {
                return new int[]{centre};
            }
            int[] out = new int[7];
            out[0] = centre;
            int[] around = HexGrid.neighbours(centre);
            System.arraycopy(around, 0, out, 1, Math.min(6, around.length));
            return out;
        }
    }

    public enum Outcome {
        /** Absorbed by a body. */
        HIT,
        /** Came back out of its own port. */
        REFLECT,
        /** Left at another port. */
        EXIT
    }

    /**
     * A ray and what became of it.
     *
     * @param port    the port it was shone from
     * @param exit    the port it left by (EXIT only), else -1
     * @param path    the sections it ran through, from the rim section it started in
     * @param touched bit per body that bent, turned or absorbed it
     * @param empty   field sections the ray proves empty: those it ran through or looked into, and those beside a
     *                section it entered without bending
     */
    public record Ray(int port, Outcome outcome, int exit, int[] path, int touched, int[] empty) {
    }

    @Nullable
    private Item item;
    private int seed;
    private List<Body> bodies = List.of();
    private int revealed;
    private final List<Ray> rays = new ArrayList<>();
    /** Sections the researcher checked and found empty. */
    private final List<Integer> misses = new ArrayList<>();
    private int maxRays;

    public ItemStudy() {
        clear();
    }

    // ---- Geometry -----------------------------------------------------------------------------------------------------

    public static boolean inField(int cell) {
        return cell >= 0 && HexGrid.steps(cell, HexGrid.CENTRE) <= FIELD;
    }

    public static int portCount() {
        return PORTS.length;
    }

    /** The {@code index}-th port in rim order. */
    public static int port(int index) {
        return PORTS[index];
    }

    /** A port's place in rim order, or -1 for no port. */
    public static int portIndex(int port) {
        return port >= 0 && port < PORT_INDEX.length ? PORT_INDEX[port] : -1;
    }

    public static int portCell(int port) {
        return port / 6;
    }

    public static int portDirection(int port) {
        return port % 6;
    }

    /** The field section a port shines into. */
    public static int portTarget(int port) {
        return HexGrid.step(portCell(port), portDirection(port));
    }

    // ---- Bodies -------------------------------------------------------------------------------------------------------

    /** Where the item's essences (strongest first, at most six) lie for this seed. */
    public static List<Body> place(ItemEssences essences, int seed) {
        Random random = new Random(seed * 31L + 7);
        List<Body> out = new ArrayList<>();
        for (Map.Entry<Essence, Integer> entry : essences.essences().entrySet()) {
            if (out.size() >= MAX_BODIES) {
                break;
            }
            boolean heavy = entry.getValue() >= HEAVY;
            Body placed = null;
            // First apart from the others by a free section, then just not touching, then anywhere free.
            for (int gap = 2; gap >= 0 && placed == null; gap--) {
                for (int attempt = 0; attempt < 400 && placed == null; attempt++) {
                    int centre = random.nextInt(HexGrid.COUNT);
                    int distance = HexGrid.steps(centre, HexGrid.CENTRE);
                    if (distance < 1 || distance > (heavy ? FIELD - 1 : FIELD)) {
                        continue;
                    }
                    Body body = new Body(entry.getKey(), entry.getValue(), centre, heavy);
                    if (fits(body, out, gap)) {
                        placed = body;
                    }
                }
            }
            if (placed == null) {
                break;
            }
            out.add(placed);
        }
        return List.copyOf(out);
    }

    private static boolean fits(Body body, List<Body> others, int gap) {
        for (int cell : body.cells()) {
            if (cell == HexGrid.CENTRE && gap > 0) {
                return false;
            }
            for (Body other : others) {
                for (int taken : other.cells()) {
                    if (HexGrid.steps(cell, taken) < Math.max(1, gap)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** The body covering a section, or -1. */
    public static int bodyAt(List<Body> bodies, int cell) {
        if (cell < 0) {
            return -1;
        }
        for (int i = 0; i < bodies.size(); i++) {
            if (bodies.get(i).covers(cell)) {
                return i;
            }
        }
        return -1;
    }

    // ---- Rays ---------------------------------------------------------------------------------------------------------

    /** Where a ray shone from {@code port} goes among these bodies. */
    public static Ray trace(List<Body> bodies, int port) {
        int cell = portCell(port);
        int d = portDirection(port);
        List<Integer> path = new ArrayList<>();
        java.util.Set<Integer> empty = new java.util.LinkedHashSet<>();
        path.add(cell);
        int touched = 0;
        boolean atRim = true;
        int turns = 0;
        for (int guard = 0; guard < 400; guard++) {
            int next = HexGrid.step(cell, d);
            if (next < 0 || !inField(next)) {
                // Out of the field: through the rim section ahead.
                int exitCell = next < 0 ? cell : next;
                int exit = exitCell * 6 + (d + 3) % 6;
                path.add(exitCell);
                if (exit == port) {
                    return new Ray(port, Outcome.REFLECT, -1, toArray(path), touched, toArray(empty));
                }
                return new Ray(port, Outcome.EXIT, portIndex(exit) >= 0 ? exit : -1, toArray(path), touched, toArray(empty));
            }
            int ahead = bodyAt(bodies, next);
            if (ahead >= 0) {
                path.add(next);
                return new Ray(port, Outcome.HIT, -1, toArray(path), touched | 1 << ahead, toArray(empty));
            }
            // Nothing in the section ahead, or the ray would have been absorbed.
            empty.add(next);
            int leftCell = HexGrid.step(next, (d + 1) % 6);
            int rightCell = HexGrid.step(next, (d + 5) % 6);
            int left = bodyAt(bodies, leftCell);
            int right = bodyAt(bodies, rightCell);
            if (left >= 0 || right >= 0) {
                touched |= (left >= 0 ? 1 << left : 0) | (right >= 0 ? 1 << right : 0);
                if (atRim) {
                    return new Ray(port, Outcome.REFLECT, -1, toArray(path), touched, toArray(empty));
                }
                if (++turns > 3) {
                    // Caught between bodies: the light dies there.
                    return new Ray(port, Outcome.HIT, -1, toArray(path), touched, toArray(empty));
                }
                d = left >= 0 && right >= 0 ? (d + 3) % 6 : left >= 0 ? (d + 5) % 6 : (d + 1) % 6;
                continue;
            }
            // It went on unbent: nothing beside the section either.
            for (int side : new int[]{leftCell, rightCell}) {
                if (inField(side)) {
                    empty.add(side);
                }
            }
            cell = next;
            path.add(cell);
            atRim = false;
            turns = 0;
        }
        return new Ray(port, Outcome.HIT, -1, toArray(path), touched, toArray(empty));
    }

    private static int[] toArray(java.util.Collection<Integer> cells) {
        return cells.stream().mapToInt(Integer::intValue).toArray();
    }

    // ---- The study ----------------------------------------------------------------------------------------------------

    public static int maxRays(List<Body> bodies) {
        return BASE_RAYS + RAYS_PER_ESSENCE * bodies.size();
    }

    @Nullable
    public Item item() {
        return item;
    }

    /** A fresh study of this item. */
    public void begin(Item item, ItemEssences essences, int seed) {
        clear();
        this.item = item;
        this.seed = seed;
        bodies = place(essences, seed);
        maxRays = maxRays(bodies);
    }

    public void clear() {
        item = null;
        seed = 0;
        bodies = List.of();
        revealed = 0;
        rays.clear();
        misses.clear();
        maxRays = 0;
    }

    public List<Body> bodies() {
        return bodies;
    }

    /** The first ray (or check) uses up one of the item: the study has begun. */
    public boolean started() {
        return !rays.isEmpty() || !misses.isEmpty() || revealed != 0;
    }

    public List<Ray> rays() {
        return rays;
    }

    public List<Integer> misses() {
        return misses;
    }

    /** Rays shone plus empty checks: both spend the study's light. */
    public int spent() {
        return rays.size() + misses.size();
    }

    public int maxRays() {
        return maxRays;
    }

    public int left() {
        return maxRays - spent();
    }

    public boolean isRevealed(int body) {
        return (revealed & 1 << body) != 0;
    }

    /** The revealed essences in body order (null where still hidden), for the hexagon's corner signs. */
    public List<@Nullable Essence> signs() {
        List<@Nullable Essence> out = new ArrayList<>();
        for (int i = 0; i < bodies.size(); i++) {
            out.add(isRevealed(i) ? bodies.get(i).essence() : null);
        }
        return out;
    }

    /** Whether a port was used already, as the start or the end of a ray. */
    public boolean used(int port) {
        for (Ray ray : rays) {
            if (ray.port() == port || ray.exit() == port) {
                return true;
            }
        }
        return false;
    }

    public boolean canFire(int port) {
        return isActive() && portIndex(port) >= 0 && !used(port);
    }

    /** Whether a section was checked already (found empty, or holds a revealed body). */
    public boolean checked(int cell) {
        int body = bodyAt(bodies, cell);
        return misses.contains(cell) || body >= 0 && isRevealed(body);
    }

    /** Whether some ray already proved this section empty. */
    public boolean provenEmpty(int cell) {
        for (Ray ray : rays) {
            for (int e : ray.empty()) {
                if (e == cell) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A section can be checked unless it was checked already or a ray proved it empty (no wasted light). */
    public boolean canCheck(int cell) {
        return isActive() && inField(cell) && !checked(cell) && !provenEmpty(cell);
    }

    public boolean won() {
        return item != null && !bodies.isEmpty() && revealed == (1 << bodies.size()) - 1;
    }

    public boolean lost() {
        return item != null && spent() >= maxRays && !won();
    }

    public boolean isActive() {
        return item != null && !won() && !lost();
    }

    /** Shines a ray from a port. */
    @Nullable
    public Ray fire(int port) {
        if (!canFire(port)) {
            return null;
        }
        Ray ray = trace(bodies, port);
        rays.add(ray);
        return ray;
    }

    /**
     * Checks a section: a hidden body there is revealed (returned), an empty section is a miss that costs a ray (null).
     */
    @Nullable
    public Body check(int cell) {
        if (!canCheck(cell)) {
            return null;
        }
        int body = bodyAt(bodies, cell);
        if (body < 0) {
            misses.add(cell);
            return null;
        }
        revealed |= 1 << body;
        return bodies.get(body);
    }

    public void save(ValueOutput tag) {
        if (item != null) {
            tag.putString("item", BuiltInRegistries.ITEM.getKey(item).toString());
        }
        tag.putInt("seed", seed);
        tag.putInt("revealed", revealed);
        tag.putIntArray("rays", rays.stream().mapToInt(Ray::port).toArray());
        tag.putIntArray("misses", misses.stream().mapToInt(Integer::intValue).toArray());
    }

    public void load(ValueInput tag) {
        clear();
        Identifier id = tag.getString("item").map(Identifier::tryParse).orElse(null);
        Item loaded = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        ItemEssences essences = loaded == null ? null : ResearchItems.of(loaded);
        if (loaded == null || essences == null) {
            return;
        }
        begin(loaded, essences, tag.getIntOr("seed", 0));
        revealed = tag.getIntOr("revealed", 0) & (1 << bodies.size()) - 1;
        for (int port : tag.getIntArray("rays").orElse(new int[0])) {
            if (portIndex(port) >= 0) {
                rays.add(trace(bodies, port));
            }
        }
        for (int cell : tag.getIntArray("misses").orElse(new int[0])) {
            if (inField(cell)) {
                misses.add(cell);
            }
        }
    }
}
