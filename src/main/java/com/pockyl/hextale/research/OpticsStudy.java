package com.pockyl.hextale.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

/**
 * Studying an item on the optics bench. The item lies in the middle of a round bench; its strongest essences hide in it
 * as spots of light - a strong essence a big spot, a weak one a small one. The researcher sets lamps and mirrors on the
 * bench and sweeps light through the item.
 * <p>
 * White light finds a spot: it glows warm, tinted towards its essence's colour. A lamp of a colour (any primal) wakes a
 * spot made of that primal - a compound wakes under either of its two. A woken spot stays awake and names its essence;
 * all of them awake = the item is studied. There are no limits: the bench is free to rearrange as often as the
 * researcher likes.
 * <p>
 * Pure rules shared by the table (server) and its screen (client copy): the spots follow from the item and a seed
 * chosen when the study begins, the light from the tools.
 */
public final class OpticsStudy {
    /** The bench's radius, in bench units (the item's sprite is {@value #ITEM_SIZE} across). */
    public static final float BENCH = 100;
    public static final float ITEM_SIZE = 80;
    /** Spots lie within this distance of the middle. */
    private static final float SPOT_FIELD = 26;
    public static final int MAX_SPOTS = 4;
    public static final int MAX_TOOLS = 24;
    /** A lamp's beam: this wide. */
    public static final float BEAM = 8;
    public static final float MIN_WIDTH = 1.2F;
    /** Light a spot needs to wake: a lamp's beam is 1, every mirror keeps 95%. */
    public static final float NEED = 0.5F;
    /** Tools keep this far inside the bench's rim. */
    public static final float RIM_GAP = 6;
    private static final int INTS_PER_TOOL = 5;

    public enum Type {
        LAMP(10),
        MIRROR(19);

        /** The length of the tool's glass across the beam (the lamp: its body). */
        public final float length;

        Type(float length) {
            this.length = length;
        }
    }

    /**
     * A tool on the bench. Lamps shine along {@code angle}; a mirror's glass lies across it (along angle + 90 degrees),
     * so a mirror at angle 0 faces a beam coming from the left.
     *
     * @param color the primal a lamp shines (null: white light; always null for mirrors)
     */
    public record Tool(Type type, float x, float y, float angle, @Nullable Essence color) {
        /** The glass as a segment {@code x0, y0, x1, y1, ux, uy} (unit vector along it). */
        float[] segment() {
            float ux = Mth.cos(angle + Mth.HALF_PI);
            float uy = Mth.sin(angle + Mth.HALF_PI);
            float half = type.length / 2;
            return new float[]{x - ux * half, y - uy * half, x + ux * half, y + uy * half, ux, uy};
        }
    }

    /** A hidden essence: where it lies and how big it is. */
    public record Spot(Essence essence, int amount, float x, float y, float radius) {
        /** The primals whose light wakes it. */
        public List<Essence> colours() {
            List<Essence> out = new ArrayList<>();
            for (Essence primal : Essence.PRIMALS) {
                if (essence.share(primal) > 0) {
                    out.add(primal);
                }
            }
            return out;
        }
    }

    /**
     * A straight piece of a beam: from {@code x0, y0} along its length; its width is {@code |w + k * s|} at distance
     * {@code s} (a lens makes {@code k} negative, so the beam narrows to its focus and widens beyond).
     *
     * @param color the primal it carries, or null for white light
     */
    public record Beam(float x0, float y0, float x1, float y1, float w, float k, float power, @Nullable Essence color) {
        public float length() {
            return Mth.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
        }

        public float width(float s) {
            return Math.max(MIN_WIDTH, Math.abs(w + k * s));
        }

        /** Light at distance {@code s}: a narrower beam is brighter. */
        public float intensity(float s) {
            return power * BEAM / width(s);
        }
    }

    @Nullable
    private Item item;
    private int seed;
    private List<Spot> spots = List.of();
    private List<Tool> tools = List.of();
    private int awake;
    /** Bumped on every change, so screens know to take the server's bench. */
    private int revision;

    // ---- The spots -------------------------------------------------------------------------------------------------

    /** The item's strongest essences as spots, scattered in its middle by the seed; the strongest is the biggest. */
    public static List<Spot> place(ItemEssences essences, int seed) {
        Random random = new Random(seed);
        List<Map.Entry<Essence, Integer>> sorted = new ArrayList<>(essences.essences().entrySet());
        sorted.sort(Comparator.comparing((Map.Entry<Essence, Integer> e) -> -e.getValue()).thenComparing(e -> e.getKey().ordinal()));
        List<Spot> out = new ArrayList<>();
        for (Map.Entry<Essence, Integer> entry : sorted.subList(0, Math.min(MAX_SPOTS, sorted.size()))) {
            int amount = entry.getValue();
            float radius = 4 + Math.min(amount, 12) * 0.55F;
            float bestX = 0;
            float bestY = 0;
            float bestGap = -Float.MAX_VALUE;
            for (int tries = 0; tries < 200; tries++) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(random.nextDouble()) * (SPOT_FIELD - radius * 0.5);
                float x = (float) (Math.cos(a) * r);
                float y = (float) (Math.sin(a) * r);
                float gap = Float.MAX_VALUE;
                for (Spot other : out) {
                    gap = Math.min(gap, Mth.sqrt((other.x - x) * (other.x - x) + (other.y - y) * (other.y - y)) - other.radius - radius);
                }
                if (gap > bestGap) {
                    bestGap = gap;
                    bestX = x;
                    bestY = y;
                }
                if (gap > 4) {
                    break;
                }
            }
            out.add(new Spot(entry.getKey(), amount, bestX, bestY, radius));
        }
        return List.copyOf(out);
    }

    // ---- The light -------------------------------------------------------------------------------------------------

    /** Every beam on the bench: from each lamp through the glass, until it leaves the bench or is stopped. */
    public static List<Beam> trace(List<Tool> tools) {
        List<Beam> beams = new ArrayList<>();
        for (Tool lamp : tools) {
            if (lamp.type() == Type.LAMP) {
                float dx = Mth.cos(lamp.angle());
                float dy = Mth.sin(lamp.angle());
                trace(tools, lamp, lamp.x() + dx * 6, lamp.y() + dy * 6, dx, dy, 1, lamp.color(), beams);
            }
        }
        return beams;
    }

    private static void trace(List<Tool> tools, @Nullable Tool from, float x, float y, float dx, float dy, float power,
                              @Nullable Essence color, List<Beam> beams) {
        for (int bounce = 0; bounce < 24 && power > 0.03F && beams.size() < 256; bounce++) {
            Tool hit = null;
            float best = Float.MAX_VALUE;
            for (Tool tool : tools) {
                if (tool == from) {
                    continue;
                }
                float t = tool.type() == Type.LAMP ? hitLamp(x, y, dx, dy, tool) : hitGlass(x, y, dx, dy, tool.segment());
                if (t > 0 && t < best) {
                    best = t;
                    hit = tool;
                }
            }
            // The bench's rim stops the light.
            float b = x * dx + y * dy;
            float c = x * x + y * y - BENCH * BENCH;
            float exit = -b + Mth.sqrt(Math.max(0, b * b - c));
            float t = Math.min(best, exit);
            float x1 = x + dx * t;
            float y1 = y + dy * t;
            beams.add(new Beam(x, y, x1, y1, BEAM, 0, power, color));
            if (hit == null || best > exit || hit.type() == Type.LAMP) {
                return;
            }
            // A mirror turns the beam.
            float[] s = hit.segment();
            float nx = -s[5];
            float ny = s[4];
            float dot = dx * nx + dy * ny;
            dx -= 2 * dot * nx;
            dy -= 2 * dot * ny;
            power *= 0.95F;
            x = x1;
            y = y1;
            from = hit;
        }
    }

    private static float hitGlass(float px, float py, float dx, float dy, float[] s) {
        float ex = s[2] - s[0];
        float ey = s[3] - s[1];
        float den = dx * ey - dy * ex;
        if (Math.abs(den) < 1e-6F) {
            return -1;
        }
        float t = ((s[0] - px) * ey - (s[1] - py) * ex) / den;
        float u = ((s[0] - px) * dy - (s[1] - py) * dx) / den;
        return t > 0.01F && u >= 0 && u <= 1 ? t : -1;
    }

    private static float hitLamp(float px, float py, float dx, float dy, Tool lamp) {
        float ox = lamp.x() - px;
        float oy = lamp.y() - py;
        float t = ox * dx + oy * dy;
        if (t <= 0.01F) {
            return -1;
        }
        float ax = ox - dx * t;
        float ay = oy - dy * t;
        return ax * ax + ay * ay < 4.5F * 4.5F ? Math.max(0.02F, t - 3.5F) : -1;
    }

    /**
     * The light each spot receives: {@code [spot][0]} white, {@code [spot][1 + primal index]} each colour. Every beam
     * that crosses the spot adds its light where it passes.
     */
    public static float[][] exposure(List<Spot> spots, List<Beam> beams) {
        float[][] out = new float[spots.size()][1 + Essence.PRIMALS.size()];
        for (Beam beam : beams) {
            float ex = beam.x1() - beam.x0();
            float ey = beam.y1() - beam.y0();
            float length = beam.length();
            float len2 = Math.max(1e-6F, ex * ex + ey * ey);
            for (int i = 0; i < spots.size(); i++) {
                Spot spot = spots.get(i);
                float u = Mth.clamp(((spot.x() - beam.x0()) * ex + (spot.y() - beam.y0()) * ey) / len2, 0, 1);
                float cx = beam.x0() + ex * u;
                float cy = beam.y0() + ey * u;
                float d = Mth.sqrt((spot.x() - cx) * (spot.x() - cx) + (spot.y() - cy) * (spot.y() - cy));
                float s = u * length;
                if (d > beam.width(s) / 2 + spot.radius() * 0.55F) {
                    continue;
                }
                out[i][beam.color() == null ? 0 : 1 + Essence.PRIMALS.indexOf(beam.color())] += beam.intensity(s);
            }
        }
        return out;
    }

    /** Whether the light wakes this spot: any primal it is made of, strongly enough. */
    public static boolean wakes(Spot spot, float[] exposure) {
        for (Essence primal : spot.colours()) {
            if (exposure[1 + Essence.PRIMALS.indexOf(primal)] >= NEED) {
                return true;
            }
        }
        return false;
    }

    // ---- The study ---------------------------------------------------------------------------------------------------

    @Nullable
    public Item item() {
        return item;
    }

    public void begin(Item item, ItemEssences essences, int seed) {
        this.item = item;
        this.seed = seed;
        spots = place(essences, seed);
        tools = List.of();
        awake = 0;
        revision++;
    }

    public void clear() {
        item = null;
        spots = List.of();
        tools = List.of();
        awake = 0;
        revision++;
    }

    public List<Spot> spots() {
        return spots;
    }

    public List<Tool> tools() {
        return tools;
    }

    public boolean isAwake(int spot) {
        return (awake & 1 << spot) != 0;
    }

    public int awakeCount() {
        return Integer.bitCount(awake);
    }

    public boolean studied() {
        return item != null && !spots.isEmpty() && awakeCount() == spots.size();
    }

    public int revision() {
        return revision;
    }

    /** An item studied before: its essences show awake from the start. */
    public void wakeAll() {
        awake = (1 << spots.size()) - 1;
        revision++;
    }

    /** Nothing changed, but screens should take the server's bench again (a refused update). */
    public void touch() {
        revision++;
    }

    /**
     * The researcher's bench: the tools are checked (on the bench, within the limit) and the light traced; spots it
     * wakes stay awake.
     *
     * @return the spots woken by this bench, bit per spot
     */
    public int setTools(List<Tool> next) {
        List<Tool> kept = new ArrayList<>();
        for (Tool tool : next) {
            float reach = BENCH - RIM_GAP + 0.5F;
            if (kept.size() < MAX_TOOLS && tool.x() * tool.x() + tool.y() * tool.y() <= reach * reach && Float.isFinite(tool.angle())
                    && (tool.type() == Type.LAMP || tool.color() == null)) {
                kept.add(tool);
            }
        }
        tools = List.copyOf(kept);
        float[][] light = exposure(spots, trace(tools));
        int woken = 0;
        for (int i = 0; i < spots.size(); i++) {
            if (!isAwake(i) && wakes(spots.get(i), light[i])) {
                woken |= 1 << i;
            }
        }
        awake |= woken;
        revision++;
        return woken;
    }

    // ---- Network and saving ---------------------------------------------------------------------------------------

    /** The tools as ints for a packet: type, x and y in sixteenths, angle in milliradians, lamp primal (or -1). */
    public static int[] encode(List<Tool> tools) {
        int[] out = new int[tools.size() * INTS_PER_TOOL];
        for (int i = 0; i < tools.size(); i++) {
            Tool tool = tools.get(i);
            out[i * INTS_PER_TOOL] = tool.type().ordinal();
            out[i * INTS_PER_TOOL + 1] = Math.round(tool.x() * 16);
            out[i * INTS_PER_TOOL + 2] = Math.round(tool.y() * 16);
            out[i * INTS_PER_TOOL + 3] = Math.round(Mth.wrapDegrees(tool.angle() * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * 1000);
            out[i * INTS_PER_TOOL + 4] = tool.color() == null ? -1 : Essence.PRIMALS.indexOf(tool.color());
        }
        return out;
    }

    /** The tools back from {@link #encode}; malformed entries are dropped. */
    public static List<Tool> decode(int[] ints) {
        List<Tool> out = new ArrayList<>();
        for (int i = 0; i + INTS_PER_TOOL <= ints.length && out.size() < MAX_TOOLS; i += INTS_PER_TOOL) {
            int type = ints[i];
            int color = ints[i + 4];
            if (type < 0 || type >= Type.values().length || color < -1 || color >= Essence.PRIMALS.size()) {
                continue;
            }
            out.add(new Tool(Type.values()[type], ints[i + 1] / 16F, ints[i + 2] / 16F, ints[i + 3] / 1000F,
                    color < 0 ? null : Essence.PRIMALS.get(color)));
        }
        return out;
    }

    public void save(ValueOutput tag) {
        if (item == null) {
            return;
        }
        tag.putString("item", BuiltInRegistries.ITEM.getKey(item).toString());
        tag.putInt("seed", seed);
        tag.putIntArray("tools", encode(tools));
        tag.putInt("awake", awake);
        tag.putInt("revision", revision);
    }

    /** Loads the bench; the spots are placed again from the item's essences, which only the caller knows. */
    public void load(ValueInput tag, java.util.function.Function<Item, @Nullable ItemEssences> essences) {
        Item loaded = tag.getString("item").map(Identifier::tryParse).flatMap(BuiltInRegistries.ITEM::getOptional).orElse(null);
        ItemEssences known = loaded == null ? null : essences.apply(loaded);
        if (loaded == null || known == null) {
            clear();
            return;
        }
        item = loaded;
        seed = tag.getIntOr("seed", 0);
        spots = place(known, seed);
        tools = decode(tag.getIntArray("tools").orElse(new int[0]));
        awake = tag.getIntOr("awake", 0) & (1 << spots.size()) - 1;
        revision = tag.getIntOr("revision", 0);
    }

}
