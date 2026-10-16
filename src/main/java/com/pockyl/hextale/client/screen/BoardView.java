package com.pockyl.hextale.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ItemStudy;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchTool;
import com.pockyl.hextale.research.RuneBoard;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Draws the research parchment in pixel art, for both games: a rune's parchment (fog, dark runes, points and the ink
 * thread) and an item's echoes (the item in the centre, the nine element strings, the ripples of the strikes).
 * Sections are laid out {@value #COL_STEP} pixels apart in a row and {@value #ROW_STEP} between rows (the background
 * texture uses the same numbers, see the texture generator).
 */
final class BoardView {
    static final int CX = 126;
    static final int CY = 144;
    static final int COL_STEP = 14;
    static final int ROW_STEP = 12;
    static final int CELL = 13;
    private static final double KX = COL_STEP / (HexGrid.HS * HexGrid.SQ3);
    private static final double KY = ROW_STEP / (HexGrid.HS * 1.5);
    private static final int WALL = 0x241A2E;
    private static final int WALL_RIM = 0x4A3A66;
    private static final int GOLD = 0xE0A21C;
    private static final int FREE_INK = 0x8FB4C8;
    private static final long FADE_MILLIS = 450;
    private static final long LIGHT_MILLIS = 700;
    private static final long SHAKE_MILLIS = 320;
    private static final long BIND_MILLIS = 1200;
    private static final long RIPPLE_STEP_MILLIS = 70;

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
    private int lastEchoes = -1;
    private long echoStart;

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

    /** A new rune or a fresh screen: nothing that is already there animates. */
    void reset() {
        fogPrimed = false;
        lastLit = -1;
        lastDone = false;
        lastEchoes = -1;
        Arrays.fill(litAt, 0);
        shakeCell = -1;
    }

    /** The thread could not go on here: the head shakes red. */
    void shake(int cell) {
        shakeCell = cell;
        shakeStart = Util.getMillis();
    }

    // ---- The rune's parchment -------------------------------------------------------------------------------------

    /** Lifts the fog around the sections the researcher's threads have passed (and the thread being drawn). */
    void updateFog(BitSet seen, int[] thread, int lens) {
        long now = Util.getMillis();
        boolean[] next = new boolean[HexGrid.COUNT];
        if (lens >= 3) {
            Arrays.fill(next, true);
        } else {
            int sight = ResearchTool.SIGHT[lens];
            BitSet sources = (BitSet) seen.clone();
            sources.set(HexGrid.CENTRE);
            for (int cell : thread) {
                sources.set(cell);
            }
            for (int s = sources.nextSetBit(0); s >= 0; s = sources.nextSetBit(s + 1)) {
                for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                    if (!next[cell] && HexGrid.steps(s, cell) <= sight) {
                        next[cell] = true;
                    }
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
     * @param trace   the thread checked against the rules (null only for an empty board)
     * @param hovered the section under the mouse, or -1
     * @param preview a section the thread would step into if clicked, or -1
     */
    void renderRune(GuiGraphicsExtractor graphics, Font font, RuneBoard board, int[] thread, ResearchSheet.@Nullable Trace trace,
                    boolean done, boolean drawing, int hovered, int preview, boolean ready) {
        long now = Util.getMillis();
        float pulse = 0.5F + 0.5F * Mth.sin(now / 320.0F);
        int lit = trace == null ? 0 : trace.lit();
        noticeLit(board, lit, now);
        if (done && !lastDone) {
            bindStart = now;
        }
        lastDone = done;

        // Sections: fog, open parchment, dark runes.
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            int x = sx(cell);
            int y = sy(cell);
            float fade = revealed[cell] == 0 ? 1 : Math.min(1, (now - revealed[cell]) / (float) FADE_MILLIS);
            if (!visible[cell]) {
                fog(graphics, x, y, cell, now);
            } else {
                if (fade < 1) {
                    fog(graphics, x, y, cell, now);
                }
                if (board.wall(cell)) {
                    stone(graphics, x, y, cell, fade);
                } else {
                    SimpleRune near = nearestCorner(cell);
                    int tint = near == null ? 0xFFF8E8 : PixelArt.mix(0xFFF8E8, near.color(), 0.22F);
                    PixelArt.hex(graphics, x, y, CELL, argb(tint, 0.22F * fade), argb(0x8C7047, 0.28F * fade));
                }
            }
        }

        // The thread: each section in the colour of the ink it costs, a glow running along it.
        int bound = done ? (int) Math.min(thread.length, (now - bindStart) / (float) BIND_MILLIS * thread.length * 1.5F) : 0;
        for (int i = 1; i < thread.length; i++) {
            SimpleRune paying = trace == null ? null : trace.paying()[i];
            int rgb = paying == null ? FREE_INK : PixelArt.onParchment(paying.color());
            if (done && i <= bound) {
                rgb = PixelArt.mix(rgb, GOLD, 0.65F);
            }
            threadSegment(graphics, thread[i - 1], thread[i], rgb, board.wall(thread[i]));
        }
        if (thread.length > 1) {
            int step = Math.max(6, thread.length / 4);
            float flow = now / 140.0F;
            for (int k = 0; k < thread.length; k += step) {
                float at = (k + flow) % (thread.length - 1);
                int i = (int) at;
                float t = at - i;
                int x = Math.round(sx(thread[i]) + (sx(thread[i + 1]) - sx(thread[i])) * t);
                int y = Math.round(sy(thread[i]) + (sy(thread[i + 1]) - sy(thread[i])) * t);
                PixelArt.fill(graphics, x, y, x + 2, y + 2, argb(done ? 0xFFF3C0 : 0xFFFFFF, 0.75F));
            }
        }

        // Points: waiting ones breathe, passed ones shine in their element.
        for (int i = 0; i < board.points().size(); i++) {
            RuneBoard.Point p = board.points().get(i);
            int x = sx(p.cell());
            int y = sy(p.cell());
            int color = PixelArt.onParchment(p.element().color());
            if ((lit & 1 << i) != 0) {
                PixelArt.hex(graphics, x, y, CELL + 2, argb(p.element().color(), 1), argb(PixelArt.mix(color, 0x000000, 0.3F), 1));
                PixelArt.radical(graphics, p.element(), x - 4, y - 4, 9, 0xFF000000 | PixelArt.mix(color, 0x000000, 0.6F));
                float t = litAt[i] == 0 ? 1 : (now - litAt[i]) / (float) LIGHT_MILLIS;
                if (t < 1) {
                    int width = CELL + 2 * (int) (t * 12) | 1;
                    PixelArt.hexRing(graphics, x, y, width, argb(p.element().color(), 1 - t));
                    PixelArt.hexRing(graphics, x, y, width + 4, argb(0xFFFFFF, (1 - t) * 0.6F));
                }
            } else {
                PixelArt.hex(graphics, x, y, CELL + 2, argb(0xFBF2DA, 1), argb(PixelArt.mix(color, 0xFFFFFF, 0.3F * pulse), 1));
                PixelArt.radical(graphics, p.element(), x - 4, y - 4, 9, argb(color, 0.85F));
                PixelArt.hexRing(graphics, x, y, CELL + 4, argb(p.element().color(), 0.25F + 0.35F * pulse));
            }
        }

        // The orb, ringed with the ink the thread starts on.
        int ox = sx(HexGrid.CENTRE);
        int oy = sy(HexGrid.CENTRE);
        int action = board.rune().action().color();
        PixelArt.hex(graphics, ox, oy, CELL + 4, argb(0x5CC8F2, 0.25F + 0.2F * pulse), argb(action, 1));
        PixelArt.hexRing(graphics, ox, oy, CELL + 6, argb(action, 0.35F + 0.35F * pulse));
        if (ready) {
            PixelArt.flush(graphics);
            graphics.item(new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), ox - 8, oy - 8);
        }

        // The head of the thread, where it can go on, and what is under the mouse.
        int head = thread[thread.length - 1];
        if (!done && thread.length > 1) {
            head(graphics, head, trace == null ? null : headColour(board, thread), now, drawing);
        } else if (!done && ready && !drawing) {
            PixelArt.hexRing(graphics, ox, oy, CELL + 8 + 2 * Math.round(pulse * 2), argb(0xFFFFFF, 0.3F + 0.4F * pulse));
        }
        if (preview >= 0) {
            int[] h = screen(head);
            int[] p = screen(preview);
            PixelArt.dotted(graphics, h[0], h[1], p[0], p[1], 2, 2, (int) (now / 90 % 4), argb(0x3B2A1A, 0.8F));
            PixelArt.hexRing(graphics, p[0], p[1], CELL, argb(0xFFFFFF, 0.7F));
        } else if (hovered >= 0 && !drawing && visible[hovered]) {
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
                PixelArt.hexRing(graphics, ox, oy, width, argb(board.rune().color(), Math.max(0, 1 - t)));
            }
        }
        renderSigns(graphics);
    }

    private void noticeLit(RuneBoard board, int lit, long now) {
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

    /** The element whose ink the thread runs on after its last section. */
    static SimpleRune headColour(RuneBoard board, int[] thread) {
        for (int i = thread.length - 1; i >= 1; i--) {
            int point = board.pointAt(thread[i]);
            if (point >= 0) {
                return board.points().get(point).element();
            }
        }
        return board.rune().action();
    }

    private void head(GuiGraphicsExtractor graphics, int cell, @Nullable SimpleRune colour, long now, boolean drawing) {
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

    // ---- An item's echoes -----------------------------------------------------------------------------------------

    void renderStudy(GuiGraphicsExtractor graphics, Font font, Item item, ItemElements elements, ItemStudy study, int hovered) {
        long now = Util.getMillis();
        float pulse = 0.5F + 0.5F * Mth.sin(now / 320.0F);
        boolean active = study.isActive(elements);
        boolean lost = study.lost(elements);
        List<ItemStudy.Echo> echoes = study.echoes();
        if (echoes.size() != lastEchoes) {
            echoStart = lastEchoes < 0 ? 0 : now;
            lastEchoes = echoes.size();
        }
        ItemStudy.Echo newest = echoes.isEmpty() ? null : echoes.get(echoes.size() - 1);
        // How far the newest ripple has run, in sections (it is still on its way).
        float wave = newest == null || echoStart == 0 ? Float.MAX_VALUE : (now - echoStart) / (float) RIPPLE_STEP_MILLIS;

        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            int x = sx(cell);
            int y = sy(cell);
            boolean cleared = false;
            for (ItemStudy.Echo echo : echoes) {
                if (echo == newest && wave < echo.radius()) {
                    continue;
                }
                if (HexGrid.steps(echo.cell(), cell) < echo.radius()) {
                    cleared = true;
                    break;
                }
            }
            int string = ItemStudy.stringOf(cell);
            if (cleared) {
                PixelArt.hex(graphics, x, y, CELL, argb(0x3B4A5A, 0.16F), argb(0x3B4A5A, 0.12F));
            } else if (string >= 0 && study.possible(cell)) {
                SimpleRune element = HexGrid.RING[string];
                int heat = wave >= (newest == null ? 0 : newest.radius()) ? study.heat(cell, elements) : 0;
                if (heat > 0) {
                    // On the rim of an open echo: an element may well sing here.
                    float glow = 0.5F + 0.5F * Mth.sin(now / 160.0F + cell);
                    PixelArt.hex(graphics, x, y, CELL, argb(PixelArt.mix(element.color(), 0xFFF8E8, 0.35F), 0.9F),
                            argb(element.color(), 1));
                    PixelArt.hexRing(graphics, x, y, CELL + 2 + (heat > 1 ? 2 : 0), argb(element.color(), 0.4F + 0.5F * glow));
                } else {
                    PixelArt.hex(graphics, x, y, CELL, argb(PixelArt.mix(element.color(), 0xFFF8E8, 0.75F), 0.55F),
                            argb(PixelArt.onParchment(element.color()), 0.35F));
                }
                PixelArt.fill(graphics, x, y, x + 1, y + 1, argb(PixelArt.onParchment(element.color()), 0.8F));
            } else {
                PixelArt.hex(graphics, x, y, CELL, argb(0x5E4526, 0.06F), argb(0x5E4526, 0.1F));
            }
        }

        // Strings, faint, from the item to the corners.
        for (int string = 0; string < HexGrid.RING.length; string++) {
            SimpleRune element = HexGrid.RING[string];
            int[] cells = ItemStudy.string(string);
            boolean found = study.isRevealed(string);
            int x0 = sx(HexGrid.CENTRE);
            int y0 = sy(HexGrid.CENTRE);
            for (int cell : cells) {
                PixelArt.dotted(graphics, x0, y0, sx(cell), sy(cell), 1, 3, 0, argb(PixelArt.onParchment(element.color()), found ? 0.5F : 0.3F));
                x0 = sx(cell);
                y0 = sy(cell);
            }
        }

        // Ripples: the older ones as faint rims, the newest one running out and ringing.
        for (ItemStudy.Echo echo : echoes) {
            if (echo.radius() == 0) {
                continue;
            }
            boolean running = echo == newest && wave < echo.radius() + 4;
            float reach = echo == newest ? Math.min(wave, echo.radius()) : echo.radius();
            float alpha = echo == newest ? 0.85F : 0.3F;
            for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                int d = HexGrid.steps(echo.cell(), cell);
                if (running && d == (int) reach && d < echo.radius()) {
                    PixelArt.hexRing(graphics, sx(cell), sy(cell), CELL, argb(0x9FE3FF, 0.8F));
                } else if (d == echo.radius() && reach >= echo.radius()) {
                    float ring = echo == newest ? Math.max(0, 1 - (wave - echo.radius()) / 6) : 0;
                    PixelArt.hexRing(graphics, sx(cell), sy(cell), CELL - 2, argb(0x2B8FB8, alpha * (0.6F + 0.4F * pulse) + ring * 0.3F));
                }
            }
        }

        // Strike marks with their distance, and the elements found.
        for (ItemStudy.Echo echo : echoes) {
            int x = sx(echo.cell());
            int y = sy(echo.cell());
            if (echo.radius() == 0) {
                continue;
            }
            PixelArt.hex(graphics, x, y, 11, argb(0xD6AB52, 1), argb(0x6E4A16, 1));
            PixelArt.flush(graphics);
            String text = echo.radius() > 9 ? "+" : String.valueOf(echo.radius());
            graphics.text(font, text, x - font.width(text) / 2 + 1, y - 3, 0xFF2A1B0C, false);
        }
        for (Map.Entry<SimpleRune, Integer> entry : elements.elements().entrySet()) {
            SimpleRune element = entry.getKey();
            if (!study.isRevealed(element)) {
                continue;
            }
            int cell = ItemStudy.hiding(element, entry.getValue());
            int x = sx(cell);
            int y = sy(cell);
            PixelArt.hex(graphics, x, y, CELL + 6, argb(element.color(), 0.25F + 0.15F * pulse), 0);
            PixelArt.hex(graphics, x, y, CELL + 2, argb(element.color(), 1), argb(GOLD, 1));
            PixelArt.radical(graphics, element, x - 4, y - 4, 9, 0xFF000000 | PixelArt.mix(element.color(), 0x000000, 0.6F));
            sparkle(graphics, x, y, now, cell);
            PixelArt.flush(graphics);
            String strength = String.valueOf(entry.getValue());
            graphics.text(font, strength, x + 7, y - 11, 0xFFFFF3C0, true);
        }

        if (hovered >= 0 && active && study.canStrike(hovered, elements)) {
            int x = sx(hovered);
            int y = sy(hovered);
            PixelArt.hex(graphics, x, y, 11, argb(0xD6AB52, 0.5F), argb(0x6E4A16, 0.7F));
            PixelArt.hexRing(graphics, x, y, CELL + 2 + 2 * Math.round(pulse * 2), argb(0xFFFFFF, 0.7F));
        }

        int ox = sx(HexGrid.CENTRE);
        int oy = sy(HexGrid.CENTRE);
        PixelArt.hex(graphics, ox, oy, CELL + 6, argb(0xFFF8E8, 0.7F), argb(lost ? 0x6A6058 : GOLD, 0.6F + 0.4F * pulse));
        if (study.won(elements)) {
            float t = Math.min(1, (now - echoStart) / 900.0F);
            PixelArt.hexRing(graphics, ox, oy, CELL + 2 * (int) (t * 30) | 1, argb(GOLD, 1 - t));
        }
        PixelArt.flush(graphics);
        graphics.item(new ItemStack(item), ox - 8, oy - 8);
        if (lost) {
            PixelArt.line(graphics, ox - 6, oy - 6, ox + 6, oy + 6, argb(0x3B2A1A, 0.8F));
            PixelArt.line(graphics, ox + 6, oy - 6, ox - 6, oy + 6, argb(0x3B2A1A, 0.8F));
        }
        renderSigns(graphics);
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

    /** The parchment with nothing on it: the element signs, and a book inviting to choose a rune. */
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
        renderSigns(graphics);
    }

    private void renderSigns(GuiGraphicsExtractor graphics) {
        for (SimpleRune element : HexGrid.RING) {
            double[] u = HexGrid.unit(element);
            int sx = left + CX + (int) Math.round(u[0] * KX * 1.03);
            int sy = top + CY + (int) Math.round(u[1] * KY * 1.03);
            PixelArt.radical(graphics, element, sx - 4, sy - 4, 9, argb(element.color(), 1));
        }
    }

    private static @Nullable SimpleRune nearestCorner(int cell) {
        SimpleRune best = null;
        double distance = 0.55;
        for (SimpleRune element : HexGrid.RING) {
            double[] u = HexGrid.unit(element);
            double d = Math.hypot(HexGrid.cx(cell) - u[0], HexGrid.cy(cell) - u[1]);
            if (d < distance) {
                distance = d;
                best = element;
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
