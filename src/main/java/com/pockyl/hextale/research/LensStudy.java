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
 * Studying an item under the Hextech Lens. The item floats in the middle; its strongest essences leave it as wisps, one
 * on each orbit around it - the strongest close and big, the weakest far, small and quick, neighbours turning opposite
 * ways. The lens above shines a wedge of light down onto the item; a flash of the lens catches every wisp inside the
 * wedge. A flash that catches nothing clouds the lens for a moment. All wisps caught = the item is studied.
 * <p>
 * Pure rules shared by the table (server) and its screen (client): the wisps follow from the item, a seed and the game
 * tick the study began on, so both sides see them in the same place. The client says WHEN it flashed (ticks since the
 * start); the server checks that time is plausible and decides what the flash caught.
 */
public final class LensStudy {
    public static final int MAX_WISPS = 4;
    /** The innermost and outermost orbit's radius, in screen pixels (the screen draws the lens at this scale). */
    public static final float INNER = 44;
    public static final float OUTER = 90;
    /** The lens's wedge of light: its direction (straight up) and half its width. */
    public static final float LENS_ANGLE = -Mth.HALF_PI;
    public static final float LENS_HALF = 0.3F;
    /** Ticks for one turn on each orbit: the outer, weaker wisps are quicker. */
    private static final float[] PERIODS = {74, 66, 60, 54};
    /** Ticks the lens needs after a flash that caught something, and after one that caught nothing. */
    public static final int RECHARGE = 6;
    public static final int CLOUDED = 24;
    /** How far a claimed flash time may lag behind the server's clock (latency), or run ahead of it. */
    private static final float LAG = 60;
    private static final float AHEAD = 10;

    /**
     * A hidden essence circling the item.
     *
     * @param distance its orbit's radius
     * @param radius   its own size
     */
    public record Wisp(Essence essence, int amount, int orbit, float distance, float radius, float start, float speed) {
        /** Where it is {@code ticks} after the study began, in radians. */
        public float angle(float ticks) {
            return start + speed * ticks;
        }

        /** Whether the lens's wedge covers it at that time. */
        public boolean inLens(float ticks) {
            float off = Mth.wrapDegrees((angle(ticks) - LENS_ANGLE) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            return Math.abs(off) <= LENS_HALF + radius / distance;
        }
    }

    @Nullable
    private Item item;
    private int seed;
    private long began;
    private List<Wisp> wisps = List.of();
    private int caught;
    /** The last flash, in ticks since the start, and whether it caught nothing (the lens is clouded after it). */
    private float lastFlash = -1000;
    private boolean lastMissed;
    private int revision;

    // ---- The wisps -------------------------------------------------------------------------------------------------

    /** The item's strongest essences as wisps: the strongest on the inner orbit; none starts inside the lens. */
    public static List<Wisp> place(ItemEssences essences, int seed) {
        Random random = new Random(seed);
        List<Map.Entry<Essence, Integer>> sorted = new ArrayList<>(essences.essences().entrySet());
        sorted.sort(Comparator.comparing((Map.Entry<Essence, Integer> e) -> -e.getValue()).thenComparing(e -> e.getKey().ordinal()));
        List<Wisp> out = new ArrayList<>();
        int count = Math.min(MAX_WISPS, sorted.size());
        for (Map.Entry<Essence, Integer> entry : sorted.subList(0, count)) {
            int orbit = out.size();
            int amount = entry.getValue();
            float radius = 3.5F + Math.min(amount, 12) * 0.25F;
            float direction = orbit % 2 == 0 ? 1 : -1;
            // Starts somewhere on the lower three quarters, away from the lens.
            float start = LENS_ANGLE + Mth.HALF_PI * 0.6F + random.nextFloat() * Mth.PI * 1.4F;
            out.add(new Wisp(entry.getKey(), amount, orbit, orbit(orbit, count), radius, start, direction * Mth.TWO_PI / PERIODS[orbit]));
        }
        return List.copyOf(out);
    }

    /** The radius of an orbit when there are {@code count}: spread evenly over the field (one wisp circles mid-way). */
    public static float orbit(int orbit, int count) {
        return count <= 1 ? (INNER + OUTER) / 2 : INNER + (OUTER - INNER) * orbit / (count - 1);
    }

    // ---- The study ---------------------------------------------------------------------------------------------------

    @Nullable
    public Item item() {
        return item;
    }

    public void begin(Item item, ItemEssences essences, int seed, long gameTime) {
        this.item = item;
        this.seed = seed;
        this.began = gameTime;
        wisps = place(essences, seed);
        caught = 0;
        lastFlash = -1000;
        lastMissed = false;
        revision++;
    }

    public void clear() {
        item = null;
        wisps = List.of();
        caught = 0;
        revision++;
    }

    public List<Wisp> wisps() {
        return wisps;
    }

    /** The game tick the study began on: wisps turn from there. */
    public long began() {
        return began;
    }

    public boolean isCaught(int wisp) {
        return (caught & 1 << wisp) != 0;
    }

    public int caughtCount() {
        return Integer.bitCount(caught);
    }

    public boolean studied() {
        return item != null && !wisps.isEmpty() && caughtCount() == wisps.size();
    }

    public int revision() {
        return revision;
    }

    /** An item studied before: its wisps show caught from the start. */
    public void catchAll() {
        caught = (1 << wisps.size()) - 1;
        revision++;
    }

    /** The wisps not caught yet that a flash at this time would catch, bit per wisp. */
    public int inLens(float ticks) {
        int out = 0;
        for (int i = 0; i < wisps.size(); i++) {
            if (!isCaught(i) && wisps.get(i).inLens(ticks)) {
                out |= 1 << i;
            }
        }
        return out;
    }

    /** Ticks the lens needs after the last flash before it can flash again. */
    public float readyAt() {
        return lastFlash + (lastMissed ? CLOUDED : RECHARGE);
    }

    /**
     * A flash of the lens at {@code ticks} after the start, the server's clock being {@code now} ticks after it.
     *
     * @return the wisps it caught, bit per wisp; -1 if the flash is refused (too early, or an implausible time)
     */
    public int flash(float ticks, float now) {
        if (item == null || !Float.isFinite(ticks) || ticks < 0 || ticks > now + AHEAD || ticks < now - LAG || ticks < readyAt() - 1) {
            return -1;
        }
        int got = inLens(ticks);
        caught |= got;
        lastFlash = ticks;
        lastMissed = got == 0;
        revision++;
        return got;
    }

    // ---- Saving ------------------------------------------------------------------------------------------------------

    public void save(ValueOutput tag) {
        if (item == null) {
            return;
        }
        tag.putString("item", BuiltInRegistries.ITEM.getKey(item).toString());
        tag.putInt("seed", seed);
        tag.putLong("began", began);
        tag.putInt("caught", caught);
        tag.putFloat("last_flash", lastFlash);
        tag.putBoolean("last_missed", lastMissed);
        tag.putInt("revision", revision);
    }

    /** Loads the study; the wisps are placed again from the item's essences, which only the caller knows. */
    public void load(ValueInput tag, java.util.function.Function<Item, @Nullable ItemEssences> essences) {
        Item loaded = tag.getString("item").map(Identifier::tryParse).flatMap(BuiltInRegistries.ITEM::getOptional).orElse(null);
        ItemEssences known = loaded == null ? null : essences.apply(loaded);
        if (loaded == null || known == null) {
            clear();
            return;
        }
        item = loaded;
        seed = tag.getIntOr("seed", 0);
        began = tag.getLongOr("began", 0L);
        wisps = place(known, seed);
        caught = tag.getIntOr("caught", 0) & (1 << wisps.size()) - 1;
        lastFlash = tag.getFloatOr("last_flash", -1000F);
        lastMissed = tag.getBooleanOr("last_missed", false);
        revision = tag.getIntOr("revision", 0);
    }
}
