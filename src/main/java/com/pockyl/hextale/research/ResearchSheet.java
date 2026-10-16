package com.pockyl.hextale.research;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

/**
 * Learning a rune: one ink thread drawn from the orb, section by section, through every point of the rune's parchment.
 * <p>
 * The thread starts on the ink of the rune's action element; every point it passes switches it to that point's element,
 * and each section costs one drop of the ink the thread runs on there. Dark runes block it and it never crosses itself
 * (research tools allow a few exceptions, see {@link Allowance}). Winding the thread back is free: ink is only spent
 * when the last point is reached and the rune binds. Stars count the sections of the finished thread. A paper is spent
 * when an attempt starts (its first section); winding back does not return it.
 * <p>
 * Pure rules shared by the table (server) and its screen (client copy).
 */
public final class ResearchSheet {
    @Nullable
    private ComplexRune rune;
    /** The thread, starting at the orb. */
    private int[] thread = {HexGrid.CENTRE};
    /** The thread bound its rune: it stays shown until a new one is started. */
    private boolean done;
    /** A paper went into this attempt when its thread was started. */
    private boolean paid;
    /** Bumped on every change the server makes, so screens know to take the server's thread. */
    private int revision;

    /**
     * What the researcher's tools allow on one thread.
     *
     * @param crossings sections where the thread may cross itself (fork quill)
     * @param passes    dark runes it may pass through (brass scraper)
     * @param free      first sections that cost no ink (parchment stretcher)
     */
    public record Allowance(int crossings, int passes, int free) {
        public static final Allowance NONE = new Allowance(0, 0, 0);
    }

    /**
     * A thread checked against the rules.
     *
     * @param ink    drops needed per element ({@link SimpleRune#ordinal()})
     * @param lit    points passed, bit per point
     * @param paying per section: the element whose ink it costs, null for free sections and the orb
     */
    public record Trace(int[] ink, int lit, int crossings, int passes, SimpleRune[] paying) {
        public boolean complete(RuneBoard board) {
            return lit == (1 << board.points().size()) - 1;
        }

        /** Whether the supply covers the ink this thread needs. */
        public boolean affordable(int[] supply) {
            for (int i = 0; i < ink.length; i++) {
                if (ink[i] > supply[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Checks a thread: it starts at the orb, steps to neighbouring sections only, never returns to the orb, and crosses
     * itself and dark runes no more than allowed. Null when it breaks a rule.
     */
    @Nullable
    public static Trace trace(RuneBoard board, int[] thread, Allowance allowance) {
        if (thread.length == 0 || thread[0] != HexGrid.CENTRE) {
            return null;
        }
        int[] ink = new int[SimpleRune.values().length];
        SimpleRune[] paying = new SimpleRune[thread.length];
        boolean[] used = new boolean[HexGrid.COUNT];
        used[HexGrid.CENTRE] = true;
        SimpleRune colour = board.rune().action();
        int lit = 0;
        int crossings = 0;
        int passes = 0;
        for (int i = 1; i < thread.length; i++) {
            int cell = thread[i];
            if (cell < 0 || cell >= HexGrid.COUNT || cell == HexGrid.CENTRE || !HexGrid.adjacent(thread[i - 1], cell)
                    || i >= 2 && thread[i - 2] == cell) {
                return null;
            }
            if (used[cell] && ++crossings > allowance.crossings()) {
                return null;
            }
            if (board.wall(cell) && ++passes > allowance.passes()) {
                return null;
            }
            used[cell] = true;
            if (i > allowance.free()) {
                ink[colour.ordinal()]++;
                paying[i] = colour;
            }
            int point = board.pointAt(cell);
            if (point >= 0) {
                lit |= 1 << point;
                colour = board.points().get(point).element();
            }
        }
        return new Trace(ink, lit, crossings, passes, paying);
    }

    @Nullable
    public ComplexRune rune() {
        return rune;
    }

    /** Chooses the rune to study: a fresh thread. */
    public void select(ComplexRune rune) {
        this.rune = rune;
        reset();
    }

    public void clear() {
        this.rune = null;
        reset();
    }

    private void reset() {
        thread = new int[]{HexGrid.CENTRE};
        done = false;
        paid = false;
        revision++;
    }

    public int[] thread() {
        return thread;
    }

    /** Sections of thread (the orb does not count). */
    public int length() {
        return thread.length - 1;
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

    /** Takes a checked thread; a bound thread is replaced by the new one. */
    public void setThread(int[] thread) {
        this.thread = thread.clone();
        done = false;
        revision++;
    }

    /** The thread bound its rune; the next attempt takes a new paper. */
    public void finish() {
        done = true;
        paid = false;
        revision++;
    }

    /** Nothing changed, but screens should take the server's thread again (a refused update). */
    public void touch() {
        revision++;
    }

    public void save(ValueOutput tag) {
        if (rune != null) {
            tag.putString("rune", rune.key());
        }
        tag.putIntArray("thread", thread);
        tag.putBoolean("done", done);
        tag.putBoolean("paid", paid);
        tag.putInt("revision", revision);
    }

    public void load(ValueInput tag) {
        rune = tag.getString("rune").map(ComplexRunes::byKey).orElse(null);
        int[] saved = tag.getIntArray("thread").orElse(new int[0]);
        thread = saved.length == 0 || Arrays.stream(saved).anyMatch(c -> c < 0 || c >= HexGrid.COUNT) ? new int[]{HexGrid.CENTRE} : saved;
        done = tag.getBooleanOr("done", false);
        paid = tag.getBooleanOr("paid", false);
        revision = tag.getIntOr("revision", 0);
    }
}
