package com.pockyl.hextale.research;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

/**
 * Learning a rune: ink drawn out from the orb, section by section, until it reaches every point of the rune's parchment.
 * The ink branches freely: a new line may start at the orb or at any section already inked, so the drawing is a tree
 * rooted at the orb. It is kept as a flat list of steps, {@code [from, to, from, to, ...]}, in the order they were drawn;
 * {@code from} is the orb or an earlier step's {@code to}.
 * <p>
 * Every step costs one section of ink of the essence the parchment gives its direction (see {@link #toward});
 * a step onto a point costs {@value #POINT_COST} sections of that point's own essence instead (see {@link #paying}). Stone dark runes block the ink and no section is inked twice. In
 * chapters II and III the dark runes are fields the ink may cross, but a cursed section bursts the orb: the check stops
 * there ({@link Trace#burst}). Every inked section next to a dark field shows how many cursed sections touch it.
 * Removing steps is free: ink is only spent when the last point is reached (or the orb bursts). Stars count the sections.
 * A paper is spent when an attempt starts (its first section).
 * <p>
 * Pure rules shared by the table (server) and its screen (client copy).
 */
public final class ResearchSheet {
    /** A step onto a point costs this many sections of the point's essence. */
    public static final int POINT_COST = 10;

    @Nullable
    private ComplexRune rune;
    /** The drawing, steps as {@code from, to} pairs. */
    private int[] tree = new int[0];
    /** The drawing bound its rune: it stays shown until a new one is started. */
    private boolean done;
    /** A paper went into this attempt when its first section was drawn. */
    private boolean paid;
    /** Bumped on every change the server makes, so screens know to take the server's drawing. */
    private int revision;

    /**
     * A drawing checked against the rules.
     *
     * @param ink      sections of ink needed per element ({@link Essence#ordinal()})
     * @param lit      points reached, bit per point
     * @param paying   per step: the element whose ink it costs
     * @param sections steps checked (all of them, or up to and including the burst)
     * @param burst    index of the step into a cursed section, or -1
     */
    public record Trace(int[] ink, int lit, Essence[] paying, int sections, int burst) {
        public boolean complete(RuneBoard board) {
            return burst < 0 && lit == (1 << board.points().size()) - 1;
        }

        /** Whether the supply covers the ink this drawing needs. */
        public boolean affordable(int[] supply) {
            for (int i = 0; i < ink.length; i++) {
                if (ink[i] > supply[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /** The steps of a single line drawn along {@code path} (which starts at the orb). */
    public static int[] fromPath(int[] path) {
        int[] tree = new int[Math.max(0, path.length - 1) * 2];
        for (int i = 1; i < path.length; i++) {
            tree[2 * i - 2] = path[i - 1];
            tree[2 * i - 1] = path[i];
        }
        return tree;
    }

    /** Every inked section of a drawing, the orb first. */
    public static int[] cells(int[] tree) {
        int[] out = new int[tree.length / 2 + 1];
        out[0] = HexGrid.CENTRE;
        for (int i = 1; i < out.length; i++) {
            out[i] = tree[2 * i - 1];
        }
        return out;
    }

    /**
     * Checks a drawing: every step starts at the orb or an inked section, goes to a neighbouring section that is not inked
     * yet, not the orb and not a stone dark rune. A step into a cursed section ends the check (the orb bursts there).
     * Null when a rule is broken.
     */
    @Nullable
    public static Trace trace(RuneBoard board, int[] tree) {
        if (tree.length % 2 != 0) {
            return null;
        }
        int steps = tree.length / 2;
        int[] ink = new int[Essence.values().length];
        Essence[] paying = new Essence[steps];
        boolean[] inked = new boolean[HexGrid.COUNT];
        inked[HexGrid.CENTRE] = true;
        int lit = 0;
        for (int i = 0; i < steps; i++) {
            int from = tree[2 * i];
            int cell = tree[2 * i + 1];
            if (from < 0 || from >= HexGrid.COUNT || cell < 0 || cell >= HexGrid.COUNT || !inked[from] || inked[cell]
                    || !HexGrid.adjacent(from, cell) || board.wall(cell)) {
                return null;
            }
            inked[cell] = true;
            Essence colour = paying(board, from, cell);
            int point = board.pointAt(cell);
            ink[colour.ordinal()] += point >= 0 ? POINT_COST : 1;
            paying[i] = colour;
            if (board.cursed(cell)) {
                return new Trace(ink, lit, Arrays.copyOf(paying, i + 1), i + 1, i);
            }
            if (point >= 0) {
                lit |= 1 << point;
            }
        }
        return new Trace(ink, lit, paying, steps, -1);
    }

    /** The essence a step pays with: a point's own essence when it steps onto a point, else that of its direction. */
    public static Essence paying(RuneBoard board, int from, int to) {
        int point = board.pointAt(to);
        return point >= 0 ? board.points().get(point).element() : toward(board, from, to);
    }

    /** The essence of a plain step: the one the parchment gives the step's direction. */
    public static Essence toward(RuneBoard board, int from, int to) {
        return board.essence(Math.max(0, HexGrid.direction(from, to)));
    }

    @Nullable
    public ComplexRune rune() {
        return rune;
    }

    /** Chooses the rune to study: a fresh drawing. */
    public void select(ComplexRune rune) {
        this.rune = rune;
        reset();
    }

    public void clear() {
        this.rune = null;
        reset();
    }

    private void reset() {
        tree = new int[0];
        done = false;
        paid = false;
        revision++;
    }

    public int[] tree() {
        return tree;
    }

    /** Sections of ink (the orb does not count). */
    public int length() {
        return tree.length / 2;
    }

    public boolean isDone() {
        return done;
    }

    /** Whether this attempt's paper is spent already. */
    public boolean paid() {
        return paid;
    }

    public void pay() {
        paid = true;
    }

    public int revision() {
        return revision;
    }

    /** Takes a checked drawing; a bound drawing is replaced by the new one. */
    public void setTree(int[] tree) {
        this.tree = tree.clone();
        done = false;
        revision++;
    }

    /** The drawing bound its rune; the next attempt takes a new paper. */
    public void finish() {
        done = true;
        paid = false;
        revision++;
    }

    /** The orb burst: the sheet is burnt, the next attempt starts from a bare orb on a new paper. */
    public void burn() {
        tree = new int[0];
        done = false;
        paid = false;
        revision++;
    }

    /** Nothing changed, but screens should take the server's drawing again (a refused update). */
    public void touch() {
        revision++;
    }

    public void save(ValueOutput tag) {
        if (rune != null) {
            tag.putString("rune", rune.key());
        }
        tag.putIntArray("tree", tree);
        tag.putBoolean("done", done);
        tag.putBoolean("paid", paid);
        tag.putInt("revision", revision);
    }

    public void load(ValueInput tag) {
        rune = tag.getString("rune").map(ComplexRunes::byKey).orElse(null);
        int[] saved = tag.getIntArray("tree").orElse(new int[0]);
        tree = saved.length % 2 != 0 || Arrays.stream(saved).anyMatch(c -> c < 0 || c >= HexGrid.COUNT) ? new int[0] : saved;
        done = tag.getBooleanOr("done", false);
        paid = tag.getBooleanOr("paid", false);
        revision = tag.getIntOr("revision", 0);
    }
}
