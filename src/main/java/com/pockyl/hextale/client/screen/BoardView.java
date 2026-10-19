package com.pockyl.hextale.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ItemStudy;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchBoard;
import com.pockyl.hextale.rune.Essence;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Draws the research parchment in pixel art, for both games: a research topic's parchment (fog, stone and dark fields with their
 * curse counts and flags, the goals - gold sigils - and the branching ink, the six essences as ink wells on the margin
 * beyond the hexagon's corners, a burning parchment) and an item's prism (the item in a misty field, the rim's ports, the
 * rays shone, the essences found).
 * Sections are laid out {@value #COL_STEP} pixels apart in a row and {@value #ROW_STEP} between rows (the background
 * texture uses the same numbers, see the texture generator).
 */
final class BoardView {
    /** Sections the fog lifts around the thread and the sections passed before. */
    private static final int SIGHT = 2;
    static final int CX = 126;
    static final int CY = 144;
    static final int COL_STEP = 14;
    static final int ROW_STEP = 12;
    static final int CELL = 13;
    /** The parchment on the desk, relative to the screen's top left (the desk texture draws it there). */
    static final int SHEET_X = 4;
    static final int SHEET_Y = 24;
    static final int SHEET_W = 244;
    static final int SHEET_H = 234;
    /** The paper itself inside that area (the burn chars only the paper). */
    private static final int PAPER_X = 8;
    private static final int PAPER_Y = 28;
    private static final int PAPER_W = 236;
    private static final int PAPER_H = 226;
    private static final Identifier DESK_TEXTURE = Hextale.id("textures/gui/research_desk.png");
    private static final double KX = COL_STEP / (HexGrid.HS * HexGrid.SQ3);
    private static final double KY = ROW_STEP / (HexGrid.HS * 1.5);
    private static final int WALL = 0x241A2E;
    private static final int WALL_RIM = 0x4A3A66;
    private static final int DARK = 0x1A1226;
    private static final int DARK_RIM = 0x6A4A9A;
    /** Colours of the curse counts 0..6, like a minesweeper's. */
    private static final int[] DANGER = {0x8C7A60, 0x2A5FC0, 0x2E8A3A, 0xC03028, 0x6A2A9A, 0x8A2A1A, 0x1A6A7A};
    private static final int GOLD = 0xE0A21C;
    private static final int GOLD_DARK = 0x8A5A12;
    private static final int SEPIA = 0x5E4526;
    /** The ink wells in the hexagon's corners: this wide, inside the corner plaques of the desk texture. */
    private static final int WELL = 13;
    private static final int FREE_INK = 0x8FB4C8;
    private static final long FADE_MILLIS = 450;
    private static final long LIGHT_MILLIS = 700;
    private static final long SHAKE_MILLIS = 320;
    private static final long BIND_MILLIS = 1200;

    private int left;
    private int top;
    private final boolean[] visible = new boolean[HexGrid.COUNT];
    /** When each section came out of the fog (for the fade), 0 = visible from the start. */
    private final long[] revealed = new long[HexGrid.COUNT];
    private boolean fogPrimed;
    /** When each point lit up, 0 = not lit (or lit before the screen opened). */
    private final long[] litAt = new long[8];
    private int lastLit;
    private long shakeStart;
    private int shakeCell = -1;
    private long bindStart;
    private boolean lastDone;

    void layout(int left, int top) {
        this.left = left;
        this.top = top;
    }

    static int px(int cell) {
        return CX + COL_STEP * HexGrid.q(cell) + COL_STEP / 2 * HexGrid.r(cell);
    }

    static int py(int cell) {
        return CY + ROW_STEP * HexGrid.r(cell);
    }

    private int sx(int cell) {
        return left + px(cell);
    }

    private int sy(int cell) {
        return top + py(cell);
    }

    /** The section under the mouse, or -1. */
    int cellAt(double mouseX, double mouseY) {
        double ux = (mouseX - left - CX - 0.5) / KX;
        double uy = (mouseY - top - CY - 0.5) / KY;
        return HexGrid.cellAt(ux, uy);
    }

    /** Screen position of a section's centre. */
    int[] screen(int cell) {
        return new int[]{sx(cell), sy(cell)};
    }

    /** A fresh parchment after a burn: everything on it fades in out of the blank sheet. */
    void freshSheet() {
        Arrays.fill(visible, false);
        fogPrimed = true;
    }

    /** A new topic or a fresh screen: nothing that is already there animates. */
    void reset() {
        fogPrimed = false;
        lastLit = -1;
        lastDone = false;
        lastRays = -1;
        Arrays.fill(litAt, 0);
        shakeCell = -1;
    }

    /** The thread could not go on here: the head shakes red. */
    void shake(int cell) {
        shakeCell = cell;
        shakeStart = Util.getMillis();
    }

    // ---- A topic's parchment ---------------------------------------------------------------------------------------

    /** Lifts the fog around the sections the researcher's threads have passed (and the thread being drawn). */
    void updateFog(BitSet seen, int[] thread) {
        long now = Util.getMillis();
        boolean[] next = new boolean[HexGrid.COUNT];
        BitSet sources = (BitSet) seen.clone();
        sources.set(HexGrid.CENTRE);
        for (int cell : thread) {
            sources.set(cell);
        }
        for (int s = sources.nextSetBit(0); s >= 0; s = sources.nextSetBit(s + 1)) {
            for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                if (!next[cell] && HexGrid.steps(s, cell) <= SIGHT) {
                    next[cell] = true;
                }
            }
        }
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (next[cell] && !visible[cell]) {
                revealed[cell] = fogPrimed ? now : 0;
            }
            visible[cell] = next[cell];
        }
        fogPrimed = true;
    }

    /**
     * @param tree        the drawing, steps as {@code from, to} pairs
     * @param trace       the drawing checked against the rules (null only for an empty board)
     * @param hovered     the section under the mouse, or -1
     * @param preview     a section a click would ink, or -1
     * @param previewFrom the inked section that step would start from
     * @param head        where the line being drawn ends
     * @param known       sections inked in earlier sheets of this attempt (their curse counts stay shown)
     * @param flags       the researcher's flags on suspected sections
     */
    void renderTopic(GuiGraphicsExtractor graphics, Font font, ResearchBoard board, int[] tree, ResearchSheet.@Nullable Trace trace,
                    boolean done, boolean drawing, int hovered, int preview, int previewFrom, boolean ready, int head,
                    BitSet known, BitSet flags) {
        long now = Util.getMillis();
        float pulse = 0.5F + 0.5F * Mth.sin(now / 320.0F);
        int lit = trace == null ? 0 : trace.lit();
        noticeLit(board, lit, now);
        if (done && !lastDone) {
            bindStart = now;
        }
        lastDone = done;
        int[] cells = ResearchSheet.cells(tree);
        boolean[] inked = new boolean[HexGrid.COUNT];
        for (int cell : cells) {
            inked[cell] = true;
        }

        // Sections: fog, open parchment, stone, dark fields. The corners hold the ink wells, drawn last.
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (HexGrid.isCorner(cell)) {
                continue;
            }
            int x = sx(cell);
            int y = sy(cell);
            float fade = revealed[cell] == 0 ? 1 : Math.min(1, (now - revealed[cell]) / (float) FADE_MILLIS);
            boolean field = board.dark(cell);
            if (!visible[cell] && !field) {
                fog(graphics, x, y, cell, now);
                continue;
            }
            if (fade < 1 && !field) {
                fog(graphics, x, y, cell, now);
            }
            if (board.stone(cell)) {
                stone(graphics, x, y, cell, fade);
            } else if (field) {
                if (inked[cell] && board.cursed(cell)) {
                    curse(graphics, x, y, now);
                } else if (inked[cell] || known.get(cell)) {
                    PixelArt.hex(graphics, x, y, CELL, argb(0x6A5A86, 0.55F), argb(DARK_RIM, 0.9F));
                } else {
                    darkField(graphics, x, y, cell, now);
                    if (flags.get(cell)) {
                        flag(graphics, x, y);
                    }
                }
            } else if (inked[cell] && board.cursed(cell)) {
                curse(graphics, x, y, now);
            } else {
                Essence near = nearestCorner(board, cell);
                int tint = near == null ? 0xFFF8E8 : PixelArt.mix(0xFFF8E8, near.color(), 0.22F);
                PixelArt.hex(graphics, x, y, CELL, argb(tint, 0.22F * fade), argb(0x8C7047, 0.28F * fade));
                if (flags.get(cell) && !inked[cell]) {
                    flag(graphics, x, y);
                }
            }
        }

        // The ink: each step in the colour of the ink it costs, a glow running along the branches.
        int steps = tree.length / 2;
        int bound = done ? (int) Math.min(steps, (now - bindStart) / (float) BIND_MILLIS * steps * 1.5F) : 0;
        for (int i = 0; i < steps; i++) {
            Essence paying = trace == null || i >= trace.paying().length ? null : trace.paying()[i];
            int rgb = paying == null ? FREE_INK : PixelArt.onParchment(paying.color());
            if (done && i < bound) {
                rgb = PixelArt.mix(rgb, GOLD, 0.65F);
            }
            threadSegment(graphics, tree[2 * i], tree[2 * i + 1], rgb, board.dark(tree[2 * i + 1]));
        }
        if (steps > 0) {
            int every = Math.max(4, steps / 5);
            float flow = now / 140.0F;
            for (int i = 0; i < steps; i += every) {
                float t = (flow + i * 0.37F) % 1;
                int from = tree[2 * i];
                int to = tree[2 * i + 1];
                int x = Math.round(sx(from) + (sx(to) - sx(from)) * t);
                int y = Math.round(sy(from) + (sy(to) - sy(from)) * t);
                PixelArt.fill(graphics, x, y, x + 2, y + 2, argb(done ? 0xFFF3C0 : 0xFFFFFF, 0.75F));
            }
        }

        // Goals: gold sigils. Waiting ones send out rays that run around them, reached ones fill with their essence.
        for (int i = 0; i < board.points().size(); i++) {
            ResearchBoard.Point p = board.points().get(i);
            int x = sx(p.cell());
            int y = sy(p.cell());
            boolean reached = (lit & 1 << i) != 0;
            goal(graphics, x, y, p.element(), reached, now + i * 517L);
            float t = reached && litAt[i] != 0 ? (now - litAt[i]) / (float) LIGHT_MILLIS : 1;
            if (t < 1) {
                int width = CELL + 2 * (int) (t * 12) | 1;
                PixelArt.hexRing(graphics, x, y, width, argb(p.element().color(), 1 - t));
                PixelArt.hexRing(graphics, x, y, width + 4, argb(0xFFF3C0, (1 - t) * 0.8F));
            }
        }

        // The orb, ringed with the ink of the topic's first essence.
        int ox = sx(HexGrid.CENTRE);
        int oy = sy(HexGrid.CENTRE);
        int action = board.topic().action().color();
        PixelArt.hex(graphics, ox, oy, CELL + 4, argb(0x5CC8F2, 0.25F + 0.2F * pulse), argb(action, 1));
        PixelArt.hexRing(graphics, ox, oy, CELL + 6, argb(action, 0.35F + 0.35F * pulse));
        if (ready) {
            PixelArt.flush(graphics);
            graphics.item(new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), ox - 8, oy - 8);
        }

        // Curse counts: every inked (or once inked) section next to a dark field tells how many curses touch it.
        PixelArt.flush(graphics);
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if ((inked[cell] || known.get(cell)) && cell != HexGrid.CENTRE && board.pointAt(cell) < 0 && board.counted(cell)
                    && !board.cursed(cell)) {
                int danger = board.danger(cell);
                String text = String.valueOf(danger);
                int x = sx(cell) - font.width(text) / 2 + 1;
                int y = sy(cell) - 3;
                graphics.text(font, text, x, y, 0xFF000000 | DANGER[Math.min(danger, DANGER.length - 1)], true);
            }
        }

        // The head of the line being drawn, where it can go on, and what is under the mouse.
        if (!done && ready) {
            inkCompass(graphics, board, inked, head, now);
        }
        if (!done && tree.length > 0 && head != HexGrid.CENTRE) {
            Essence last = null;
            for (int i = 0; i < steps && trace != null && i < trace.paying().length; i++) {
                if (tree[2 * i + 1] == head) {
                    last = trace.paying()[i];
                }
            }
            head(graphics, head, last, now, drawing);
        } else if (!done && ready && !drawing) {
            PixelArt.hexRing(graphics, ox, oy, CELL + 8 + 2 * Math.round(pulse * 2), argb(0xFFFFFF, 0.3F + 0.4F * pulse));
        }
        if (preview >= 0 && previewFrom >= 0) {
            int[] h = screen(previewFrom);
            int[] p = screen(preview);
            PixelArt.dotted(graphics, h[0], h[1], p[0], p[1], 2, 2, (int) (now / 90 % 4), argb(0x3B2A1A, 0.8F));
            PixelArt.hexRing(graphics, p[0], p[1], CELL, argb(board.dark(preview) ? 0xE04A3A : 0xFFFFFF, 0.7F));
        } else if (hovered >= 0 && !drawing && !HexGrid.isCorner(hovered) && (visible[hovered] || board.dark(hovered))) {
            PixelArt.hexRing(graphics, sx(hovered), sy(hovered), CELL, argb(0xFFFFFF, 0.35F));
        }
        if (shakeCell >= 0) {
            float t = (now - shakeStart) / (float) SHAKE_MILLIS;
            if (t < 1) {
                int dx = Math.round(Mth.sin(t * 30) * 2 * (1 - t));
                PixelArt.hexRing(graphics, sx(shakeCell) + dx, sy(shakeCell), CELL + 2, argb(0xE04A3A, 1 - t));
                PixelArt.hexRing(graphics, sx(shakeCell) + dx, sy(shakeCell), CELL, argb(0xE04A3A, (1 - t) * 0.7F));
            } else {
                shakeCell = -1;
            }
        }
        if (done) {
            float t = (now - bindStart) / (float) BIND_MILLIS;
            if (t < 1.4F) {
                int width = CELL + 2 * (int) (Math.min(1, t) * 40) | 1;
                PixelArt.hexRing(graphics, ox, oy, width, argb(board.topic().color(), Math.max(0, 1 - t)));
            }
        }
        renderWells(graphics, board.essences());
    }

    /**
     * A goal: a gold-framed sigil with the essence it asks for. While it waits, six rays leave its corners and a bright
     * one runs around it; reached, it fills with its essence and the rays go out.
     */
    private static void goal(GuiGraphicsExtractor graphics, int x, int y, Essence essence, boolean reached, long now) {
        float pulse = 0.5F + 0.5F * Mth.sin(now / 300.0F);
        int color = essence.color();
        if (!reached) {
            int lead = (int) (now / 160 % 6);
            for (int k = 0; k < 6; k++) {
                double angle = Math.PI / 6 + k * Math.PI / 3;
                float bright = k == lead ? 1 : k == (lead + 5) % 6 ? 0.6F : 0.3F;
                int r0 = CELL / 2 + 3;
                int r1 = r0 + 2 + Math.round((k == lead ? 2 : 1) * pulse);
                PixelArt.line(graphics, x + (int) Math.round(Math.cos(angle) * r0), y + (int) Math.round(Math.sin(angle) * r0),
                        x + (int) Math.round(Math.cos(angle) * r1), y + (int) Math.round(Math.sin(angle) * r1),
                        argb(PixelArt.mix(GOLD, 0xFFF3C0, bright * 0.6F), 0.35F + 0.6F * bright));
            }
            PixelArt.hex(graphics, x, y, CELL + 4, argb(0xFFF8E6, 1), argb(PixelArt.mix(GOLD_DARK, GOLD, pulse), 1));
            PixelArt.hexRing(graphics, x, y, CELL + 2, argb(GOLD, 0.55F + 0.3F * pulse));
            PixelArt.essence(graphics, essence, x - 4, y - 4, 9, argb(PixelArt.onParchment(color), 1));
        } else {
            PixelArt.hex(graphics, x, y, CELL + 4, argb(color, 1), argb(GOLD_DARK, 1));
            PixelArt.hexRing(graphics, x, y, CELL + 2, argb(GOLD, 0.9F));
            PixelArt.essence(graphics, essence, x - 4, y - 4, 9, 0xFF000000 | PixelArt.mix(PixelArt.onParchment(color), 0x000000, 0.35F));
        }
    }

    /** The goals still to reach, counted on the parchment's top left: a small gold sigil and "Goals n/m". */
    void renderGoals(GuiGraphicsExtractor graphics, Font font, ResearchBoard board, int lit, Component label) {
        int width = 12 + font.width(label);
        int x = left + CX - width / 2 - 10;
        int y = top + PAPER_Y + 8;
        boolean all = Integer.bitCount(lit) == board.points().size();
        PixelArt.hex(graphics, x, y, 9, argb(all ? GOLD : 0xFFF8E6, 1), argb(GOLD_DARK, 1));
        PixelArt.flush(graphics);
        graphics.text(font, label, x + 8, y - 4, 0xFF000000 | PixelArt.mix(GOLD_DARK, 0x2A1A0A, 0.4F), false);
    }

    /** An unexplored dark field section: a dark ink blot whose sheen drifts. */
    private static void darkField(GuiGraphicsExtractor graphics, int x, int y, int cell, long now) {
        float drift = 0.5F + 0.5F * Mth.sin(now / 900.0F + cell * 0.61F);
        PixelArt.hex(graphics, x, y, CELL, argb(DARK, 0.92F), argb(DARK_RIM, 1));
        PixelArt.fill(graphics, x - 2, y - 3, x + 1, y - 2, argb(0x8C7AB8, 0.35F + 0.35F * drift));
        PixelArt.fill(graphics, x + 1, y + 1, x + 3, y + 2, argb(0x8C7AB8, 0.2F + 0.2F * (1 - drift)));
    }

    /** A cursed section the orb burst on: scorched, with a red cross. */
    private static void curse(GuiGraphicsExtractor graphics, int x, int y, long now) {
        float glow = 0.6F + 0.4F * Mth.sin(now / 260.0F);
        PixelArt.hex(graphics, x, y, CELL, argb(0x2A0A0A, 0.95F), argb(0xB0302A, glow));
        int red = argb(0xE04A3A, 1);
        PixelArt.line(graphics, x - 3, y - 3, x + 3, y + 3, red);
        PixelArt.line(graphics, x - 3, y + 3, x + 3, y - 3, red);
    }

    /** The researcher's flag: a little red pennant on a pole. */
    private static void flag(GuiGraphicsExtractor graphics, int x, int y) {
        PixelArt.fill(graphics, x - 1, y - 4, x, y + 4, argb(0xE8DCC0, 1));
        PixelArt.fill(graphics, x, y - 4, x + 4, y - 2, argb(0xE04A3A, 1));
        PixelArt.fill(graphics, x, y - 2, x + 2, y - 1, argb(0xE04A3A, 1));
        PixelArt.fill(graphics, x - 3, y + 4, x + 2, y + 5, argb(0xE8DCC0, 1));
    }

    private void noticeLit(ResearchBoard board, int lit, long now) {
        if (lastLit >= 0) {
            int added = lit & ~lastLit;
            for (int i = 0; i < board.points().size() && i < litAt.length; i++) {
                if ((added & 1 << i) != 0) {
                    litAt[i] = now;
                }
            }
        }
        lastLit = lit;
    }

    /**
     * Around the end of the line: a drop of colour towards every open neighbour, the ink a step that way would cost. They
     * breathe one after another, like a compass needle settling.
     */
    private void inkCompass(GuiGraphicsExtractor graphics, ResearchBoard board, boolean[] inked, int head, long now) {
        int hx = sx(head);
        int hy = sy(head);
        int[] neighbours = HexGrid.neighbours(head);
        for (int k = 0; k < neighbours.length; k++) {
            int next = neighbours[k];
            if (inked[next] || board.wall(next) && (visible[next] || HexGrid.isCorner(next))) {
                continue;
            }
            Essence ink = ResearchSheet.paying(board, head, next);
            float breath = 0.6F + 0.4F * Mth.sin(now / 220.0F - k * 1.05F);
            int x = hx + Math.round((sx(next) - hx) * 0.55F);
            int y = hy + Math.round((sy(next) - hy) * 0.55F);
            PixelArt.fill(graphics, x - 1, y - 2, x + 2, y + 3, argb(PixelArt.mix(ink.color(), 0x000000, 0.45F), breath));
            PixelArt.fill(graphics, x - 2, y - 1, x + 3, y + 2, argb(PixelArt.mix(ink.color(), 0x000000, 0.45F), breath));
            PixelArt.fill(graphics, x - 1, y - 1, x + 2, y + 2, argb(ink.color(), breath));
        }
    }

    private static boolean contains(int[] cells, int cell) {
        for (int c : cells) {
            if (c == cell) {
                return true;
            }
        }
        return false;
    }

    private void head(GuiGraphicsExtractor graphics, int cell, @Nullable Essence colour, long now, boolean drawing) {
        int x = sx(cell);
        int y = sy(cell);
        int rgb = colour == null ? 0xFFFFFF : colour.color();
        float pulse = 0.5F + 0.5F * Mth.sin(now / (drawing ? 90.0F : 200.0F));
        PixelArt.hex(graphics, x, y, 7, argb(0xFFFFFF, 0.9F), argb(rgb, 1));
        PixelArt.hexRing(graphics, x, y, CELL + 2 + 2 * Math.round(pulse * 2), argb(rgb, 0.4F + 0.5F * pulse));
        for (int k = 0; k < 4; k++) {
            float t = ((now + k * 250L) % 1000) / 1000.0F;
            double angle = k * 1.7 + now / 700.0;
            int px = x + (int) Math.round(Math.cos(angle) * (4 + t * 6));
            int py = y + (int) Math.round(Math.sin(angle) * (4 + t * 6)) - (int) (t * 3);
            PixelArt.fill(graphics, px, py, px + 1, py + 1, argb(PixelArt.mix(rgb, 0xFFFFFF, 0.5F), 1 - t));
        }
    }

    private void threadSegment(GuiGraphicsExtractor graphics, int from, int to, int rgb, boolean throughWall) {
        int x0 = sx(from);
        int y0 = sy(from);
        int x1 = sx(to);
        int y1 = sy(to);
        int dark = argb(PixelArt.mix(rgb, 0x000000, 0.35F), 0.9F);
        PixelArt.line(graphics, x0, y0 + 1, x1, y1 + 1, dark);
        PixelArt.line(graphics, x0, y0, x1, y1, argb(rgb, 1));
        PixelArt.line(graphics, x0 + 1, y0, x1 + 1, y1, argb(rgb, 1));
        PixelArt.fill(graphics, x1 - 1, y1 - 1, x1 + 2, y1 + 2, argb(rgb, 1));
        if (throughWall) {
            PixelArt.hexRing(graphics, x1, y1, CELL, argb(0xC9D3E3, 0.8F));
        }
    }

    /** Fog: a smudge of darker parchment that drifts slowly. */
    private static void fog(GuiGraphicsExtractor graphics, int x, int y, int cell, long now) {
        float drift = 0.5F + 0.5F * Mth.sin(now / 1500.0F + cell * 0.37F);
        PixelArt.hex(graphics, x, y, CELL, argb(0x4A3520, 0.3F + 0.08F * drift), argb(0x4A3520, 0.18F));
    }

    private static void stone(GuiGraphicsExtractor graphics, int x, int y, int cell, float alpha) {
        PixelArt.hex(graphics, x, y, CELL, argb(WALL, alpha), argb(WALL_RIM, alpha));
        int crack = argb(0x0C0812, alpha);
        if (cell % 2 == 0) {
            PixelArt.line(graphics, x - 3, y - 2, x, y + 1, crack);
            PixelArt.line(graphics, x, y + 1, x + 3, y - 1, crack);
        } else {
            PixelArt.line(graphics, x - 2, y + 2, x + 1, y - 2, crack);
            PixelArt.line(graphics, x + 1, y - 2, x + 3, y, crack);
        }
        PixelArt.fill(graphics, x - 2, y - 4, x, y - 3, argb(0x6A5890, alpha));
    }

    // ---- An item through the prism ------------------------------------------------------------------------------

    /** The prism's field is small: it is drawn this much larger than a topic's parchment. */
    private static final float PRISM_SCALE = 1.5F;
    private static final int PRISM_CELL = 19;
    private static final long RAY_STEP_MILLIS = 45;
    private static final int PALE_LIGHT = 0xFFF3C0;
    private int lastRays = -1;
    private long rayStart;

    private int psx(int cell) {
        return left + CX + Math.round((px(cell) - CX) * PRISM_SCALE);
    }

    private int psy(int cell) {
        return top + CY + Math.round((py(cell) - CY) * PRISM_SCALE);
    }

    /** Screen position of a port: on the edge between its rim section and the field section it shines into. */
    private int[] portScreen(int port) {
        int rim = ItemStudy.portCell(port);
        int target = ItemStudy.portTarget(port);
        return new int[]{Math.round((psx(rim) * 0.58F + psx(target) * 0.42F)), Math.round((psy(rim) * 0.58F + psy(target) * 0.42F))};
    }

    /** The field section under the mouse on the prism, or -1. */
    int prismCellAt(double mouseX, double mouseY) {
        double ux = ((mouseX - left - CX) / PRISM_SCALE - 0.5) / KX;
        double uy = ((mouseY - top - CY) / PRISM_SCALE - 0.5) / KY;
        int cell = HexGrid.cellAt(ux, uy);
        return ItemStudy.inField(cell) ? cell : -1;
    }

    /** The port nearest the mouse (within a few pixels), or -1. */
    int portAt(double mouseX, double mouseY) {
        int best = -1;
        double bestDistance = 6.5;
        for (int i = 0; i < ItemStudy.portCount(); i++) {
            int port = ItemStudy.port(i);
            int[] at = portScreen(port);
            double d = Math.hypot(mouseX - at[0], mouseY - at[1]);
            if (d < bestDistance) {
                bestDistance = d;
                best = port;
            }
        }
        return best;
    }

    /** The colour of a ray: the essences it touched mixed, pale light when it touched none. */
    private static int rayColor(ItemStudy study, ItemStudy.Ray ray) {
        int color = -1;
        int count = 0;
        for (int b = 0; b < study.bodies().size(); b++) {
            if ((ray.touched() & 1 << b) != 0) {
                int c = study.bodies().get(b).essence().color();
                color = count == 0 ? c : PixelArt.mix(color, c, 1F / (count + 1));
                count++;
            }
        }
        return count == 0 ? PALE_LIGHT : color;
    }

    /**
     * The prism: a mist-filled hexagonal field in a brass rim with ports; the rays shone so far marked at the ports they
     * went in and came out (numbered, in the colours of the essences they touched) and drawn along their paths, the mist
     * lifted from every section a ray proved empty, revealed essences, empty checks and the researcher's pencil marks.
     *
     * @param hoveredCell a field section under the mouse, or -1
     * @param hoveredPort a port under the mouse, or -1
     * @param pencil      the researcher's own marks (client only)
     */
    void renderStudy(GuiGraphicsExtractor graphics, Font font, Item item, ItemStudy study, int hoveredCell, int hoveredPort, BitSet pencil) {
        long now = Util.getMillis();
        float pulse = 0.5F + 0.5F * Mth.sin(now / 320.0F);
        boolean active = study.isActive();
        boolean lost = study.lost();
        List<ItemStudy.Ray> rays = study.rays();
        if (rays.size() != lastRays) {
            rayStart = lastRays < 0 ? 0 : now;
            lastRays = rays.size();
        }
        ItemStudy.Ray newest = rays.isEmpty() ? null : rays.get(rays.size() - 1);
        boolean[] empty = new boolean[HexGrid.COUNT];
        for (ItemStudy.Ray ray : rays) {
            if (ray == newest && rayStart != 0 && (now - rayStart) / (float) RAY_STEP_MILLIS < ray.path().length) {
                continue;
            }
            for (int cell : ray.empty()) {
                empty[cell] = true;
            }
        }
        float run = newest == null || rayStart == 0 ? Float.MAX_VALUE : (now - rayStart) / (float) RAY_STEP_MILLIS;
        boolean running = newest != null && run < newest.path().length + 2;

        // The rim: brass sections around the field.
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            int ring = HexGrid.steps(cell, HexGrid.CENTRE);
            if (ring == ItemStudy.RIM) {
                PixelArt.hex(graphics, psx(cell), psy(cell), PRISM_CELL, argb(0x6E4A16, 0.55F), argb(0x3B2A10, 0.8F));
            }
        }
        // The field: mist drifting over the parchment.
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (!ItemStudy.inField(cell)) {
                continue;
            }
            int x = psx(cell);
            int y = psy(cell);
            if (empty[cell]) {
                // Proved empty by a ray: the mist has lifted, clean parchment.
                PixelArt.hex(graphics, x, y, PRISM_CELL, argb(0xFFF8E8, 0.12F), argb(0x8C7047, 0.18F));
                PixelArt.fill(graphics, x, y, x + 1, y + 1, argb(0x8C7047, 0.5F));
                continue;
            }
            float drift = 0.5F + 0.5F * Mth.sin(now / 1300.0F + cell * 0.53F);
            PixelArt.hex(graphics, x, y, PRISM_CELL, argb(0xBFAE90, 0.55F + 0.1F * drift), argb(0x6E5634, 0.55F));
        }

        // The rays' paths: each in its colour, the newest drawing itself section by section, the hovered one bright.
        for (ItemStudy.Ray ray : rays) {
            boolean fresh = ray == newest && running;
            boolean hot = ray.port() == hoveredPort || ray.exit() == hoveredPort;
            int color = PixelArt.onParchment(rayColor(study, ray));
            int[] path = ray.path();
            int segments = fresh ? Math.min(path.length - 1, (int) run) : path.length - 1;
            float alpha = hot || fresh ? 1 : 0.55F;
            int[] from = portScreen(ray.port());
            for (int k = 1; k <= segments; k++) {
                int cell = path[k];
                int[] to = k == path.length - 1 && ray.outcome() != ItemStudy.Outcome.HIT
                        ? (ray.exit() >= 0 ? portScreen(ray.exit()) : portScreen(ray.port()))
                        : new int[]{psx(cell), psy(cell)};
                PixelArt.line(graphics, from[0], from[1] + 1, to[0], to[1] + 1, argb(PixelArt.mix(color, 0x000000, 0.4F), alpha * 0.7F));
                PixelArt.line(graphics, from[0], from[1], to[0], to[1], argb(color, alpha));
                if (hot) {
                    PixelArt.line(graphics, from[0] + 1, from[1], to[0] + 1, to[1], argb(color, alpha));
                }
                from = to;
            }
            if (ray.outcome() == ItemStudy.Outcome.HIT && segments == path.length - 1) {
                int end = path[path.length - 1];
                PixelArt.hexRing(graphics, psx(end), psy(end), PRISM_CELL - 4, argb(color, alpha));
            }
        }

        // Revealed essences, empty checks and pencil marks.
        for (int b = 0; b < study.bodies().size(); b++) {
            if (!study.isRevealed(b)) {
                continue;
            }
            ItemStudy.Body body = study.bodies().get(b);
            Essence essence = body.essence();
            for (int cell : body.cells()) {
                PixelArt.hex(graphics, psx(cell), psy(cell), PRISM_CELL, argb(essence.color(), cell == body.centre() ? 0.95F : 0.55F),
                        argb(PixelArt.mix(essence.color(), 0x000000, 0.35F), 1));
            }
            int x = psx(body.centre());
            int y = psy(body.centre());
            PixelArt.hex(graphics, x, y, PRISM_CELL + 6, argb(essence.color(), 0.2F + 0.15F * pulse), 0);
            PixelArt.essence(graphics, essence, x - 4, y - 4, 9, 0xFF000000 | PixelArt.mix(essence.color(), 0x000000, 0.45F));
            sparkle(graphics, x, y, now, body.centre());
        }
        for (int cell : study.misses()) {
            int x = psx(cell);
            int y = psy(cell);
            int red = argb(0x9A3A2A, 0.8F);
            PixelArt.line(graphics, x - 3, y - 3, x + 3, y + 3, red);
            PixelArt.line(graphics, x - 3, y + 3, x + 3, y - 3, red);
        }
        for (int cell = pencil.nextSetBit(0); cell >= 0; cell = pencil.nextSetBit(cell + 1)) {
            if (ItemStudy.inField(cell) && !study.checked(cell)) {
                PixelArt.hexRing(graphics, psx(cell), psy(cell), PRISM_CELL - 6, argb(0x3B2A1A, 0.85F));
                PixelArt.fill(graphics, psx(cell) - 1, psy(cell) - 1, psx(cell) + 1, psy(cell) + 1, argb(0x3B2A1A, 0.85F));
            }
        }

        // Ports: free ones as small brass studs, used ones as badges of the ray's colour.
        ItemStudy.Ray highlighted = null;
        for (ItemStudy.Ray ray : rays) {
            if (ray.port() == hoveredPort || ray.exit() == hoveredPort) {
                highlighted = ray;
            }
        }
        for (int i = 0; i < ItemStudy.portCount(); i++) {
            int port = ItemStudy.port(i);
            int[] at = portScreen(port);
            if (!study.used(port)) {
                boolean over = port == hoveredPort && active;
                PixelArt.hex(graphics, at[0], at[1], over ? 7 : 5, argb(over ? 0xFFF3C0 : 0xD6AB52, 1), argb(0x6E4A16, 1));
                if (over) {
                    // Where it would shine: a short dotted line into the field.
                    int target = ItemStudy.portTarget(port);
                    PixelArt.dotted(graphics, at[0], at[1], psx(target), psy(target), 1, 2, (int) (now / 90 % 3), argb(0xFFF3C0, 0.9F));
                    PixelArt.hexRing(graphics, at[0], at[1], 11 + 2 * Math.round(pulse * 2), argb(0xFFFFFF, 0.7F));
                }
            }
        }
        PixelArt.flush(graphics);
        for (int r = 0; r < rays.size(); r++) {
            ItemStudy.Ray ray = rays.get(r);
            boolean fresh = ray == newest && running;
            if (fresh && run < ray.path().length) {
                // The badges appear when the light has made its way.
                badge(graphics, font, portScreen(ray.port()), r, rayColor(study, ray), ItemStudy.Outcome.EXIT, ray == highlighted, true);
                continue;
            }
            int color = rayColor(study, ray);
            badge(graphics, font, portScreen(ray.port()), r, color, ray.outcome(), ray == highlighted, false);
            if (ray.outcome() == ItemStudy.Outcome.EXIT && ray.exit() >= 0) {
                badge(graphics, font, portScreen(ray.exit()), r, color, ItemStudy.Outcome.EXIT, ray == highlighted, false);
            }
            if (fresh) {
                float t = (run - ray.path().length) / 2;
                int[] at = portScreen(ray.outcome() == ItemStudy.Outcome.EXIT && ray.exit() >= 0 ? ray.exit() : ray.port());
                PixelArt.hexRing(graphics, at[0], at[1], 9 + Math.round(t * 10), argb(color, Math.max(0, 1 - t)));
            }
        }
        if (highlighted != null && highlighted.outcome() == ItemStudy.Outcome.EXIT && highlighted.exit() >= 0) {
            int[] a = portScreen(highlighted.port());
            int[] b = portScreen(highlighted.exit());
            PixelArt.dotted(graphics, a[0], a[1], b[0], b[1], 1, 3, (int) (now / 120 % 4), argb(rayColor(study, highlighted), 0.6F));
        }

        if (hoveredCell >= 0 && active && study.canCheck(hoveredCell)) {
            PixelArt.hexRing(graphics, psx(hoveredCell), psy(hoveredCell), PRISM_CELL + 2 + 2 * Math.round(pulse * 2), argb(0xFFFFFF, 0.7F));
        }

        // The item in the middle of the field.
        int ox = psx(HexGrid.CENTRE);
        int oy = psy(HexGrid.CENTRE);
        PixelArt.hex(graphics, ox, oy, PRISM_CELL + 4, argb(0xFFF8E8, 0.6F), argb(lost ? 0x6A6058 : GOLD, 0.6F + 0.4F * pulse));
        if (study.won()) {
            float t = Math.min(1, (now - rayStart) / 900.0F);
            PixelArt.hexRing(graphics, ox, oy, PRISM_CELL + 2 * (int) (t * 40) | 1, argb(GOLD, 1 - t));
        }
        PixelArt.flush(graphics);
        graphics.item(new ItemStack(item), ox - 8, oy - 8);
        if (lost) {
            PixelArt.line(graphics, ox - 6, oy - 6, ox + 6, oy + 6, argb(0x3B2A1A, 0.8F));
            PixelArt.line(graphics, ox + 6, oy - 6, ox - 6, oy + 6, argb(0x3B2A1A, 0.8F));
        }
        renderSigns(graphics, study.signs());
    }

    /** A used port's badge: numbered for a ray that came out, a cross for one absorbed, a ring for one reflected. */
    private static void badge(GuiGraphicsExtractor graphics, Font font, int[] at, int index, int color, ItemStudy.Outcome outcome,
                              boolean highlight, boolean pending) {
        int x = at[0];
        int y = at[1];
        if (pending) {
            PixelArt.hex(graphics, x, y, 7, argb(0xFFF3C0, 0.9F), argb(0x6E4A16, 1));
            return;
        }
        PixelArt.hex(graphics, x, y, 11, argb(PixelArt.mix(color, 0x000000, outcome == ItemStudy.Outcome.HIT ? 0.55F : 0.15F), 1),
                argb(PixelArt.mix(color, 0x000000, 0.5F), 1));
        if (highlight) {
            PixelArt.hexRing(graphics, x, y, 15, argb(0xFFFFFF, 0.9F));
        }
        switch (outcome) {
            case HIT -> {
                int c = argb(PixelArt.mix(color, 0xFFFFFF, 0.3F), 1);
                PixelArt.line(graphics, x - 2, y - 2, x + 2, y + 2, c);
                PixelArt.line(graphics, x - 2, y + 2, x + 2, y - 2, c);
            }
            case REFLECT -> PixelArt.hexRing(graphics, x, y, 7, argb(0xFFFFFF, 0.9F));
            case EXIT -> {
                PixelArt.flush(graphics);
                String text = String.valueOf(index + 1);
                graphics.text(font, text, x - font.width(text) / 2 + 1, y - 3, 0xFF1A1208, false);
            }
        }
    }

    private static void sparkle(GuiGraphicsExtractor graphics, int x, int y, long now, int seed) {
        for (int k = 0; k < 3; k++) {
            float t = ((now + seed * 137L + k * 333L) % 1000) / 1000.0F;
            double angle = (seed * 1.7 + k * 2.1) % (Math.PI * 2);
            int sx = x + (int) Math.round(Math.cos(angle) * (5 + t * 5));
            int sy = y + (int) Math.round(Math.sin(angle) * (5 + t * 5)) - (int) (t * 3);
            PixelArt.fill(graphics, sx, sy, sx + 1, sy + 1, argb(0xFFF6D6, 1 - t));
        }
    }

    // ---- Shared -----------------------------------------------------------------------------------------------------

    /** The parchment with nothing on it: the element signs, and a book inviting to choose a topic. */
    void renderEmpty(GuiGraphicsExtractor graphics, boolean hovered) {
        long now = Util.getMillis();
        float pulse = 0.5F + 0.5F * Mth.sin(now / 320.0F);
        int ox = sx(HexGrid.CENTRE);
        int oy = sy(HexGrid.CENTRE);
        PixelArt.hex(graphics, ox, oy, 27, argb(0x5E4526, 0.12F), argb(0x5E4526, 0.3F));
        PixelArt.hexRing(graphics, ox, oy, 31 + 2 * Math.round(pulse * 2), argb(hovered ? 0xFFFFFF : GOLD, 0.35F + 0.4F * pulse));
        PixelArt.flush(graphics);
        graphics.pose().pushMatrix();
        graphics.pose().translate(ox - 12, oy - 13 - Math.round(pulse * 2));
        graphics.pose().scale(1.5F, 1.5F);
        graphics.item(new ItemStack(ModItems.RESEARCHERS_DIARY.get()), 0, 0);
        graphics.pose().popMatrix();
        renderSigns(graphics, java.util.Collections.nCopies(6, null));
    }

    /** Where the ink well of a corner's essence sits: in the corner section itself, on the parchment's corner plaque. */
    private int[] well(int d) {
        int cell = HexGrid.cornerCell(d);
        return new int[]{sx(cell), sy(cell)};
    }

    /** The corner whose ink well is under the mouse, or -1. */
    int cornerAt(double mouseX, double mouseY) {
        for (int d = 0; d < 6; d++) {
            int[] at = well(d);
            if (Math.hypot(mouseX - at[0], mouseY - at[1]) <= WELL / 2.0 + 1) {
                return d;
            }
        }
        return -1;
    }

    /**
     * The six essences of the parchment: an ink well in each corner of the hexagon, a step towards that corner is paid
     * with its ink. Muted towards the parchment, so they never read as goals.
     */
    private void renderWells(GuiGraphicsExtractor graphics, List<Essence> essences) {
        for (int d = 0; d < 6 && d < essences.size(); d++) {
            Essence essence = essences.get(d);
            int[] at = well(d);
            int muted = PixelArt.mix(essence.color(), 0xE8D6B0, 0.45F);
            PixelArt.disc(graphics, at[0], at[1], WELL, argb(muted, 1), argb(SEPIA, 0.9F));
            PixelArt.essence(graphics, essence, at[0] - 4, at[1] - 4, 9, argb(PixelArt.mix(PixelArt.onParchment(essence.color()), 0x1E140A, 0.3F), 1));
        }
    }

    /** Signs at the hexagon's corners (the prism and a blank sheet): an essence, or a question mark for a hidden one. */
    private void renderSigns(GuiGraphicsExtractor graphics, List<@Nullable Essence> essences) {
        for (int d = 0; d < 6; d++) {
            double[] corner = HexGrid.corner(d);
            int x = left + CX + (int) Math.round(corner[0] * KX);
            int y = top + CY + (int) Math.round(corner[1] * KY);
            Essence essence = d < essences.size() ? essences.get(d) : null;
            if (essence != null) {
                PixelArt.hex(graphics, x, y, 15, argb(0x1E1608, 0.85F), argb(PixelArt.mix(essence.color(), 0x000000, 0.2F), 1));
                PixelArt.essence(graphics, essence, x - 4, y - 4, 9, argb(essence.color(), 1));
            } else {
                PixelArt.hex(graphics, x, y, 15, argb(SEPIA, 0.12F), argb(SEPIA, 0.35F));
                PixelArt.fill(graphics, x - 1, y - 3, x + 2, y - 2, argb(SEPIA, 0.6F));
                PixelArt.fill(graphics, x + 1, y - 2, x + 2, y, argb(SEPIA, 0.6F));
                PixelArt.fill(graphics, x, y, x + 1, y + 1, argb(SEPIA, 0.6F));
                PixelArt.fill(graphics, x, y + 2, x + 1, y + 3, argb(SEPIA, 0.6F));
            }
        }
    }

    // ---- A burning parchment ---------------------------------------------------------------------------------------

    /** The fire eats the sheet in this time, the char lies a moment, then a fresh sheet slides in. */
    private static final long BURN_MILLIS = 1500;
    private static final long CHAR_MILLIS = 450;
    private static final long SLIDE_MILLIS = 550;
    /** The char is drawn in blocks of this many pixels. */
    private static final int BURN_BLOCK = 2;
    private static final int EDGE = 7;
    private static final int SCORCH = 10;
    private static final int EMBERS = 36;
    private static final long EMBER_MILLIS = 650;

    /** A parchment burning: the board and drawing that set it alight, the section where the orb burst and when. */
    record Burn(ResearchBoard board, int[] tree, ResearchSheet.@Nullable Trace trace, int cell, long start) {
    }

    /**
     * Plays a burn: the fire spreads from the burst over the old board with a ragged glowing edge and embers rising off
     * it, the char lies a moment, then a fresh sheet slides in from above.
     *
     * @param next the new board (null while it is still generated)
     * @return false once it is over
     */
    boolean renderBurn(GuiGraphicsExtractor graphics, Font font, Burn burn, @Nullable ResearchBoard next) {
        long now = Util.getMillis();
        long age = now - burn.start();
        if (age >= BURN_MILLIS + CHAR_MILLIS + SLIDE_MILLIS) {
            return false;
        }
        int x0 = left + PAPER_X;
        int y0 = top + PAPER_Y;
        int ox = sx(burn.cell());
        int oy = sy(burn.cell());
        double reach = 0;
        for (int corner = 0; corner < 4; corner++) {
            reach = Math.max(reach, Math.hypot(x0 + (corner & 1) * PAPER_W - ox, y0 + (corner >> 1) * PAPER_H - oy));
        }
        reach += EDGE + 14;
        if (age < BURN_MILLIS + CHAR_MILLIS) {
            renderTopic(graphics, font, burn.board(), burn.tree(), burn.trace(), false, false, -1, -1, -1, true, burn.cell(),
                    new BitSet(), new BitSet());
        }
        double radius = reach * Math.pow(Math.min(1, age / (float) BURN_MILLIS), 1.35);
        int seed = burn.cell() * 31 + (int) burn.start();
        for (int by = y0; by < y0 + PAPER_H; by += BURN_BLOCK) {
            int runColor = 0;
            int runStart = x0;
            for (int bx = x0; bx < x0 + PAPER_W; bx += BURN_BLOCK) {
                double d = Math.hypot(bx + 1 - ox, by + 1 - oy) + noise(bx - x0, by - y0, seed) * 9;
                int color = burnColor(bx - x0, by - y0, radius - d, age, seed);
                if (color != runColor) {
                    PixelArt.fill(graphics, runStart, by, bx, by + BURN_BLOCK, runColor);
                    runColor = color;
                    runStart = bx;
                }
            }
            PixelArt.fill(graphics, runStart, by, x0 + PAPER_W, by + BURN_BLOCK, runColor);
        }
        // The first flash of the burst, then embers rising off the burning edge.
        if (age < 300) {
            float f = age / 300.0F;
            PixelArt.disc(graphics, ox, oy, 6 + 2 * Math.round(f * 14), argb(0xFFE9A0, 0.9F * (1 - f)), argb(0xFF8A1E, 1 - f));
        }
        for (int k = 0; k < EMBERS; k++) {
            long shifted = age + k * 97L;
            long cycle = shifted / EMBER_MILLIS;
            float phase = (shifted % EMBER_MILLIS) / (float) EMBER_MILLIS;
            float born = age - phase * EMBER_MILLIS;
            if (born < 0 || born > BURN_MILLIS) {
                continue;
            }
            double at = reach * Math.pow(born / BURN_MILLIS, 1.35);
            double angle = (rnd(k, (int) cycle, seed) + 1) * Math.PI;
            int px = ox + (int) Math.round(Math.cos(angle) * at + Math.sin(phase * 9 + k) * 2);
            int py = oy + (int) Math.round(Math.sin(angle) * at - phase * 18);
            if (px < x0 || px >= x0 + PAPER_W - 1 || py < y0 || py >= y0 + PAPER_H - 1) {
                continue;
            }
            int size = phase < 0.35F ? 2 : 1;
            PixelArt.fill(graphics, px, py, px + size, py + size, argb(PixelArt.mix(0xFFE070, 0xC0400A, phase), 1 - phase));
        }
        // A fresh sheet slides in from above over the char, with a soft shadow under its edge.
        if (age > BURN_MILLIS + CHAR_MILLIS) {
            float t = (age - BURN_MILLIS - CHAR_MILLIS) / (float) SLIDE_MILLIS;
            float eased = 1 - (1 - t) * (1 - t) * (1 - t);
            int dy = -Math.round(PAPER_H * (1 - eased));
            PixelArt.flush(graphics);
            graphics.enableScissor(x0, y0, x0 + PAPER_W, y0 + PAPER_H);
            PixelArt.fill(graphics, x0, y0 + PAPER_H + dy, x0 + PAPER_W, y0 + PAPER_H + dy + 4, argb(0x000000, 0.35F));
            PixelArt.flush(graphics);
            graphics.blit(RenderPipelines.GUI_TEXTURED, DESK_TEXTURE, x0, y0 + dy, PAPER_X, PAPER_Y, PAPER_W, PAPER_H,
                    ResearchTableMenu.IMAGE_WIDTH, ResearchTableMenu.IMAGE_HEIGHT);
            if (next != null) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(0, dy);
                renderWells(graphics, next.essences());
                PixelArt.flush(graphics);
                graphics.pose().popMatrix();
            }
            PixelArt.flush(graphics);
            graphics.disableScissor();
        }
        return true;
    }

    /**
     * The colour of a block of the burning sheet {@code depth} pixels behind the fire's edge (negative: still ahead of
     * it): char with dying embers, the glowing edge, a brown scorch running ahead, or nothing.
     */
    private static int burnColor(int x, int y, double depth, long age, int seed) {
        if (depth > EDGE) {
            float ember = rnd(x / BURN_BLOCK, y / BURN_BLOCK, seed + 7);
            if (ember > 0.9F && depth < EDGE + 40) {
                float cool = (float) ((depth - EDGE) / 40);
                float flicker = 0.6F + 0.4F * Mth.sin(age / 70.0F + ember * 40);
                return argb(PixelArt.mix(0xFF7A1A, 0x1C1008, Math.min(1, cool + 0.2F * (1 - flicker))), 1);
            }
            return argb(ember < -0.6F ? 0x24160C : 0x1A0F07, 0.96F);
        }
        if (depth > 0) {
            float e = (float) (depth / EDGE);
            float flicker = 0.85F + 0.15F * Mth.sin(age / 55.0F + x * 0.7F + y * 0.3F);
            int rgb = e > 0.6F ? PixelArt.mix(0xFF8A1E, 0x7A2408, (e - 0.6F) / 0.4F)
                    : PixelArt.mix(0xFFE070, 0xFF8A1E, e / 0.6F);
            return argb(rgb, flicker);
        }
        if (depth > -SCORCH) {
            return argb(0x6A3A10, 0.55F * (float) (1 + depth / SCORCH));
        }
        return 0;
    }

    /** Smooth value noise in -1..1 on an 8 pixel lattice, for the fire's ragged edge. */
    private static float noise(int x, int y, int seed) {
        int gx = Math.floorDiv(x, 8);
        int gy = Math.floorDiv(y, 8);
        float fx = (x - gx * 8) / 8.0F;
        float fy = (y - gy * 8) / 8.0F;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        float top = Mth.lerp(fx, rnd(gx, gy, seed), rnd(gx + 1, gy, seed));
        float bottom = Mth.lerp(fx, rnd(gx, gy + 1, seed), rnd(gx + 1, gy + 1, seed));
        return Mth.lerp(fy, top, bottom);
    }

    /** A hashed number in -1..1. */
    private static float rnd(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ h >>> 13) * 1274126177;
        return ((h ^ h >>> 16) & 0xFFFF) / 65535.0F * 2 - 1;
    }

    private static @Nullable Essence nearestCorner(ResearchBoard board, int cell) {
        Essence best = null;
        double distance = 0.55;
        for (int d = 0; d < 6; d++) {
            double[] corner = HexGrid.corner(d);
            double dist = Math.hypot(HexGrid.cx(cell) - corner[0], HexGrid.cy(cell) - corner[1]);
            if (dist < distance) {
                distance = dist;
                best = board.essence(d);
            }
        }
        return best;
    }

    boolean visible(int cell) {
        return visible[cell];
    }

    private static int argb(int rgb, float alpha) {
        return PixelArt.argb(rgb, alpha);
    }
}
