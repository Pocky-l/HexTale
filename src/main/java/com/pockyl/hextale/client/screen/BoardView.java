package com.pockyl.hextale.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ItemStudy;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.research.RuneBoard;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Draws the research parchment in pixel art, for both games: a rune's parchment (dark runes, points, the symbols written
 * so far, the sections the next symbol can reach) and an item's resonance (the item in the centre, the nine element
 * strings, tuning marks and their echoes). Sections are laid out {@value #COL_STEP} pixels apart in a row and
 * {@value #ROW_STEP} between rows (the background texture uses the same numbers, see the texture generator).
 */
final class BoardView {
    static final int CX = 126;
    static final int CY = 144;
    static final int COL_STEP = 14;
    static final int ROW_STEP = 12;
    static final int CELL = 13;
    private static final double KX = COL_STEP / (HexGrid.HS * HexGrid.SQ3);
    private static final double KY = ROW_STEP / (HexGrid.HS * 1.5);
    /** Fog radius (parchment units) around sections the ink has passed, by lens strength 0-2; 3 sees everything. */
    private static final double[] SIGHT = {0.26, 0.4, 0.6};
    private static final int INK = 0x3B2A1A;
    private static final int WALL = 0x1E1628;
    private static final int WALL_RIM = 0x4A3A66;
    private static final int GOLD = 0xC88A12;
    private static final long INK_MILLIS = 380;
    private static final long BURST_MILLIS = 800;

    /** A symbol the researcher can write now: which studied item, ground or not, and where its ink goes. */
    record Move(Item item, ItemElements elements, boolean ground, HexGrid.Walk walk) {
    }

    private int left;
    private int top;
    private final boolean[] visible = new boolean[HexGrid.COUNT];
    private long inkStart;
    private int inkMarks;
    private long burstStart;
    private int burstCell = -1;
    private int burstColor;
    private long fadeStart;
    private int fadeElement = -1;
    private int lastFilled;
    private int lastEchoes;
    /** False until the first look at the sheet: what was already there when the screen opened is not animated. */
    private boolean primed;

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

    /** The section under the mouse, or -1. */
    int cellAt(double mouseX, double mouseY) {
        double ux = (mouseX - left - CX - 0.5) / KX;
        double uy = (mouseY - top - CY - 0.5) / KY;
        return HexGrid.cellAt(ux, uy);
    }

    /** Recomputes the fog: sections near the ink's past paths are visible, all of them with a full lens. */
    void updateFog(BitSet seen, int lens) {
        if (lens >= 3) {
            Arrays.fill(visible, true);
            return;
        }
        double sight = SIGHT[lens];
        Arrays.fill(visible, false);
        for (int s = seen.nextSetBit(0); s >= 0; s = seen.nextSetBit(s + 1)) {
            for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                if (!visible[cell] && HexGrid.distance(s, cell) < sight) {
                    visible[cell] = true;
                }
            }
        }
        visible[HexGrid.CENTRE] = true;
    }

    /** Notices new symbols, filled points and echoes to animate them. */
    void observe(@Nullable RuneBoard board, @Nullable ResearchSheet sheet, @Nullable ItemStudy study) {
        long now = Util.getMillis();
        int used = sheet == null ? 0 : sheet.used();
        int filled = sheet == null ? 0 : sheet.filledMask();
        int echoes = study == null ? 0 : study.echoes().size() + study.listens() * 100;
        if (!primed) {
            primed = true;
            inkMarks = used;
            lastFilled = filled;
            lastEchoes = echoes;
            return;
        }
        if (used > inkMarks) {
            inkStart = now;
        }
        inkMarks = used;
        int added = filled & ~lastFilled;
        if (added != 0 && board != null) {
            RuneBoard.Point p = board.points().get(Integer.numberOfTrailingZeros(added));
            burstCell = p.cell();
            burstColor = p.element().color();
            burstStart = now;
            fadeElement = p.element().ordinal();
            fadeStart = now;
        }
        lastFilled = filled;
        if (echoes != lastEchoes && study != null) {
            burstCell = HexGrid.CENTRE;
            burstColor = 0x5CC8F2;
            burstStart = now;
        }
        lastEchoes = echoes;
    }

    // ---- A rune's parchment ---------------------------------------------------------------------------------------

    /**
     * @param moves   where the next symbol can go, by landing section (empty when nothing can be written)
     * @param hovered the section under the mouse, or -1
     * @param chosen  the move shown for the hovered section, or null
     */
    void renderRune(GuiGraphics graphics, Font font, RuneBoard board, ResearchSheet sheet, Map<Integer, List<Move>> moves,
                    int hovered, @Nullable Move chosen, int choice, int forkLevel) {
        long now = Util.getMillis();
        float pulse = 0.5F + 0.5F * Mth.sin(now / 320.0F);
        int filled = sheet.isOpen() ? sheet.filledMask() : 0;
        int dissolved = board.dissolved(filled);
        float fade = fadeElement >= 0 ? Math.min(1, (now - fadeStart) / 900.0F) : 1;

        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            int x = left + px(cell);
            int y = top + py(cell);
            SimpleRune wall = board.walls()[cell];
            int point = board.pointAt(cell);
            if (point >= 0) {
                RuneBoard.Point p = board.points().get(point);
                int color = PixelArt.onParchment(p.element().color());
                if (sheet.isOpen() && sheet.isFilled(point)) {
                    PixelArt.hex(graphics, x, y, CELL, argb(PixelArt.mix(p.element().color(), 0xFFFFFF, 0.1F), 1), argb(color, 1));
                    PixelArt.radical(graphics, p.element(), x - 4, y - 4, 9, 0xFF000000 | PixelArt.mix(color, 0x000000, 0.55F));
                } else {
                    PixelArt.hex(graphics, x, y, CELL, argb(0xFBF2DA, 1), argb(PixelArt.mix(color, 0xFFFFFF, 0.35F * pulse), 1));
                    PixelArt.radical(graphics, p.element(), x - 4, y - 4, 9, argb(color, 0.75F));
                }
            } else if (wall != null) {
                boolean gone = (dissolved & 1 << wall.ordinal()) != 0;
                if (gone) {
                    float alpha = fadeElement == wall.ordinal() ? 1 - fade * 0.8F : 0.2F;
                    PixelArt.hex(graphics, x, y, CELL, argb(WALL, alpha * 0.6F), argb(wall.color(), alpha));
                    PixelArt.radical(graphics, wall, x - 3, y - 3, 7, argb(PixelArt.onParchment(wall.color()), Math.max(0.35F, alpha)));
                } else {
                    PixelArt.hex(graphics, x, y, CELL, argb(WALL, 1), argb(WALL_RIM, 1));
                    if (visible[cell]) {
                        PixelArt.radical(graphics, wall, x - 3, y - 3, 7, argb(0x9A86C8, 1));
                        graphics.fill(x + 3, y + 3, x + 5, y + 5, argb(wall.color(), 1));
                    } else {
                        graphics.drawString(font, "?", x - 2, y - 4, 0xFF6A5890, false);
                    }
                }
            } else if (visible[cell]) {
                SimpleRune near = nearestCorner(cell);
                int tint = near == null ? 0xFFF8E8 : PixelArt.mix(0xFFF8E8, near.color(), 0.25F);
                PixelArt.hex(graphics, x, y, CELL, argb(tint, 0.28F), argb(0x8C7047, 0.32F));
            } else {
                PixelArt.hex(graphics, x, y, CELL, argb(0x5E4526, 0.2F), argb(0x5E4526, 0.12F));
            }
        }

        List<ResearchSheet.Mark> marks = sheet.isOpen() ? sheet.marks() : List.of();
        float inkProgress = Math.min(1, (now - inkStart) / (float) INK_MILLIS);
        for (int i = 0; i < marks.size(); i++) {
            ResearchSheet.Mark mark = marks.get(i);
            boolean newest = i == marks.size() - 1 && inkProgress < 1;
            inkLine(graphics, mark.from(), mark.cells(), newest ? inkProgress : 1, argb(INK, 0.75F));
        }
        for (int i = 0; i < marks.size(); i++) {
            ResearchSheet.Mark mark = marks.get(i);
            if (i == marks.size() - 1 && inkProgress < 1) {
                continue;
            }
            symbol(graphics, mark.cell(), mark.element(), 1);
        }
        for (int i = 0; i < board.points().size(); i++) {
            RuneBoard.Point p = board.points().get(i);
            if (sheet.isOpen() && sheet.markAt(p.cell()) != null) {
                int color = sheet.isFilled(i) ? p.element().color() : PixelArt.mix(p.element().color(), 0xFFFFFF, pulse * 0.6F);
                PixelArt.hexRing(graphics, left + px(p.cell()), top + py(p.cell()), CELL, argb(color, 1));
                PixelArt.hexRing(graphics, left + px(p.cell()), top + py(p.cell()), CELL + 2, argb(color, 0.5F + 0.5F * pulse));
            }
        }

        // Where the next symbol can go: a gold spark on every reachable section.
        for (Map.Entry<Integer, List<Move>> entry : moves.entrySet()) {
            int cell = entry.getKey();
            int x = left + px(cell);
            int y = top + py(cell);
            int color = board.pointAt(cell) >= 0 ? 0xE0A21C : GOLD;
            PixelArt.hexRing(graphics, x, y, CELL - 4, argb(color, 0.45F + 0.4F * pulse));
            graphics.fill(x, y, x + 1, y + 1, argb(color, 0.9F));
        }
        if (chosen != null) {
            renderMove(graphics, font, board, sheet, chosen, now, moves.getOrDefault(hovered, List.of()).size(), choice);
        }
        if (sheet.isActive() && sheet.from() != HexGrid.CENTRE) {
            PixelArt.hexRing(graphics, left + px(sheet.from()), top + py(sheet.from()), CELL + 2, argb(0x2B8FB8, 0.6F + 0.4F * pulse));
        }
        int ox = left + px(HexGrid.CENTRE);
        int oy = top + py(HexGrid.CENTRE);
        PixelArt.hex(graphics, ox, oy, CELL + 2, argb(0x5CC8F2, 0.25F + 0.2F * pulse), argb(0x2B8FB8, 1));
        graphics.renderItem(new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), ox - 8, oy - 8);
        if (forkLevel > 0 && sheet.canJump(board, HexGrid.CENTRE, forkLevel) && hovered == HexGrid.CENTRE) {
            PixelArt.hexRing(graphics, ox, oy, CELL + 4, argb(0xE0A21C, 1));
        }
        renderSigns(graphics);
        renderBurst(graphics, now);
        if (hovered >= 0 && chosen == null) {
            PixelArt.hexRing(graphics, left + px(hovered), top + py(hovered), CELL, argb(0xFFFFFF, 0.6F));
        }
    }

    /** The chosen move: its ink path, a ghost symbol and the item that writes it, hovering above the section. */
    private void renderMove(GuiGraphics graphics, Font font, RuneBoard board, ResearchSheet sheet, Move move, long now, int options,
                            int choice) {
        int phase = (int) (now / 90 % 6);
        int from = sheet.isActive() ? sheet.from() : HexGrid.CENTRE;
        int x0 = left + px(from);
        int y0 = top + py(from);
        int point = board.pointAt(move.walk().end());
        boolean good = point >= 0 && !(sheet.isOpen() && sheet.isFilled(point));
        int color = good ? 0xFFC88A12 : 0xCC3B2A1A;
        for (int cell : move.walk().cells()) {
            int x1 = left + px(cell);
            int y1 = top + py(cell);
            PixelArt.dotted(graphics, x0, y0, x1, y1, 3, 3, phase, color);
            x0 = x1;
            y0 = y1;
        }
        int end = move.walk().end();
        symbol(graphics, end, move.elements().main(), 0.55F);
        PixelArt.hexRing(graphics, left + px(end), top + py(end), CELL + 2, color);
        int ix = left + px(end) + 6;
        int iy = top + py(end) - 20;
        graphics.fill(ix - 1, iy - 1, ix + 17, iy + 17, 0xD0140C06);
        graphics.renderItem(new ItemStack(move.item()), ix, iy);
        if (move.ground()) {
            graphics.drawString(font, "½", ix, iy, 0xFF7EF0FF, true);
        }
        if (options > 1) {
            graphics.drawString(font, (choice % options + 1) + "/" + options, ix + 18, iy + 5, 0xFFF0E3C2, true);
        }
    }

    // ---- An item's resonance --------------------------------------------------------------------------------------

    void renderStudy(GuiGraphics graphics, Item item, ItemElements elements, ItemStudy study, int hovered) {
        long now = Util.getMillis();
        float pulse = 0.5F + 0.5F * Mth.sin(now / 320.0F);
        boolean active = study.isActive(elements);
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            PixelArt.hex(graphics, left + px(cell), top + py(cell), CELL, argb(0x5E4526, 0.08F), argb(0x5E4526, 0.12F));
        }
        for (int string = 0; string < HexGrid.RING.length; string++) {
            SimpleRune element = HexGrid.RING[string];
            boolean revealed = study.isRevealed(element);
            int ink = PixelArt.onParchment(element.color());
            int[] cells = ItemStudy.string(string);
            int x0 = left + px(HexGrid.CENTRE);
            int y0 = top + py(HexGrid.CENTRE);
            for (int i = 0; i < cells.length; i++) {
                int cell = cells[i];
                int x = left + px(cell);
                int y = top + py(cell);
                PixelArt.line(graphics, x0, y0, x, y, argb(ink, revealed ? 0.6F : 0.22F));
                x0 = x;
                y0 = y;
                ItemStudy.Echo echo = study.echoes().get(cell);
                int fill = PixelArt.mix(element.color(), 0xFFF8E8, revealed ? 0.75F : 0.88F);
                PixelArt.hex(graphics, x, y, CELL, argb(fill, revealed ? 0.8F : 0.55F), argb(ink, revealed ? 0.7F : 0.35F));
                if (echo == ItemStudy.Echo.EXACT) {
                    PixelArt.hex(graphics, x, y, CELL, argb(element.color(), 1), argb(GOLD, 1));
                    PixelArt.hexRing(graphics, x, y, CELL + 2, argb(0xFFD36E, 0.6F + 0.4F * pulse));
                    PixelArt.radical(graphics, element, x - 4, y - 4, 9, 0xFF000000 | PixelArt.mix(element.color(), 0x000000, 0.6F));
                } else if (echo == ItemStudy.Echo.SILENT) {
                    PixelArt.line(graphics, x - 3, y - 3, x + 3, y + 3, 0xC0605048);
                    PixelArt.line(graphics, x - 3, y + 3, x + 3, y - 3, 0xC0605048);
                } else if (echo != null) {
                    // It hums: the element is on this string, nearer to or farther from the item.
                    PixelArt.hexRing(graphics, x, y, CELL, argb(ink, 1));
                    arrow(graphics, x, y, element, echo == ItemStudy.Echo.FARTHER, ink);
                }
                if (study.mark(string) == i) {
                    PixelArt.hex(graphics, x, y, CELL - 4, argb(0x5CC8F2, 0.55F + 0.35F * pulse), argb(0x2B8FB8, 1));
                    PixelArt.hexRing(graphics, x, y, CELL + 2, argb(0x5CC8F2, 0.5F + 0.5F * pulse));
                }
                if (cell == hovered && active && !revealed) {
                    PixelArt.hexRing(graphics, x, y, CELL, argb(0xFFFFFF, 0.8F));
                }
            }
        }
        int ox = left + px(HexGrid.CENTRE);
        int oy = top + py(HexGrid.CENTRE);
        PixelArt.hex(graphics, ox, oy, CELL + 4, argb(0xFFF8E8, 0.6F), argb(GOLD, 0.6F + 0.4F * pulse));
        graphics.renderItem(new ItemStack(item), ox - 8, oy - 8);
        renderSigns(graphics);
        renderBurst(graphics, now);
    }

    /** A small pixel arrow along the string: outwards (farther) or towards the item (nearer). */
    private void arrow(GuiGraphics graphics, int x, int y, SimpleRune element, boolean outwards, int color) {
        double[] u = HexGrid.unit(element);
        double dx = u[0] * (outwards ? 1 : -1);
        double dy = u[1] * (outwards ? 1 : -1);
        int tipX = x + (int) Math.round(dx * 4);
        int tipY = y + (int) Math.round(dy * 4);
        int argb = argb(color, 1);
        PixelArt.line(graphics, x - (int) Math.round(dx * 3), y - (int) Math.round(dy * 3), tipX, tipY, argb);
        int sideX = (int) Math.round(-dy * 3);
        int sideY = (int) Math.round(dx * 3);
        int baseX = tipX - (int) Math.round(dx * 3);
        int baseY = tipY - (int) Math.round(dy * 3);
        PixelArt.line(graphics, tipX, tipY, baseX + sideX, baseY + sideY, argb);
        PixelArt.line(graphics, tipX, tipY, baseX - sideX, baseY - sideY, argb);
    }

    // ---- Shared -----------------------------------------------------------------------------------------------------

    private void renderSigns(GuiGraphics graphics) {
        for (SimpleRune element : HexGrid.RING) {
            double[] u = HexGrid.unit(element);
            int sx = left + CX + (int) Math.round(u[0] * KX * 1.03);
            int sy = top + CY + (int) Math.round(u[1] * KY * 1.03);
            PixelArt.radical(graphics, element, sx - 4, sy - 4, 9, argb(element.color(), 1));
        }
    }

    private void renderBurst(GuiGraphics graphics, long now) {
        if (burstCell >= 0) {
            float t = (now - burstStart) / (float) BURST_MILLIS;
            if (t < 1) {
                int width = CELL + 2 * (int) (t * 14) | 1;
                PixelArt.hexRing(graphics, left + px(burstCell), top + py(burstCell), width, argb(burstColor, 1 - t));
            } else {
                burstCell = -1;
            }
        }
    }

    /** A written symbol: a section washed with ink and the element's radical in pale parchment. */
    private void symbol(GuiGraphics graphics, int cell, SimpleRune element, float alpha) {
        int x = left + px(cell);
        int y = top + py(cell);
        PixelArt.hex(graphics, x, y, CELL, argb(INK, 0.9F * alpha), argb(0x1E140A, alpha));
        PixelArt.radical(graphics, element, x - 4, y - 4, 9, argb(PixelArt.mix(0xF4E6C4, element.color(), 0.35F), alpha));
    }

    private void inkLine(GuiGraphics graphics, int from, int[] cells, float progress, int color) {
        int shown = Math.round(cells.length * progress);
        int x0 = left + px(from);
        int y0 = top + py(from);
        for (int i = 0; i < shown; i++) {
            int x1 = left + px(cells[i]);
            int y1 = top + py(cells[i]);
            PixelArt.line(graphics, x0, y0, x1, y1, color);
            PixelArt.line(graphics, x0 + 1, y0, x1 + 1, y1, color);
            x0 = x1;
            y0 = y1;
        }
    }

    @Nullable
    private static SimpleRune nearestCorner(int cell) {
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

    /** What the hovered section of a rune's parchment is, for a tooltip. */
    List<Component> describe(RuneBoard board, ResearchSheet sheet, int cell, int forkLevel, @Nullable Move move) {
        List<Component> lines = new ArrayList<>();
        String key = "gui.hextale.research_table.cell.";
        if (move != null) {
            lines.add(Component.translatable(key + "write", move.item().getDescription()));
            lines.add(ResearchText.elements(move.elements()));
        }
        int point = board.pointAt(cell);
        SimpleRune wall = board.walls()[cell];
        int dissolved = board.dissolved(sheet.isOpen() ? sheet.filledMask() : 0);
        if (point >= 0) {
            RuneBoard.Point p = board.points().get(point);
            lines.add(Component.translatable(key + "point", ResearchText.element(p.element())));
            if (sheet.isOpen() && sheet.isFilled(point)) {
                lines.add(Component.translatable(key + "point.filled").withStyle(ChatFormatting.GREEN));
            } else if (sheet.isActive() && sheet.ready(board, point)) {
                lines.add(Component.translatable(key + "point.leave", p.resource().getDescription()).withStyle(ChatFormatting.GOLD));
            } else {
                lines.add(Component.translatable(key + "point.needs", p.resource().getDescription()).withStyle(ChatFormatting.GRAY));
            }
        } else if (wall != null) {
            boolean gone = (dissolved & 1 << wall.ordinal()) != 0;
            if (gone) {
                lines.add(Component.translatable(key + "wall.gone", ResearchText.element(wall)));
            } else if (visible[cell]) {
                lines.add(Component.translatable(key + "wall", ResearchText.element(wall)));
                lines.add(Component.translatable(key + "wall.dissolve", ResearchText.element(wall)).withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable(key + "wall.unknown"));
                lines.add(Component.translatable(key + "wall.fog").withStyle(ChatFormatting.GRAY));
            }
        } else if (cell == HexGrid.CENTRE) {
            lines.add(Component.translatable(key + "orb"));
        }
        if (forkLevel > 0 && sheet.canJump(board, cell, forkLevel)) {
            lines.add(Component.translatable(move != null ? key + "fork.shift" : key + "fork").withStyle(ChatFormatting.AQUA));
        }
        return lines;
    }

    /** What a section of an item's resonance is, for a tooltip. */
    List<Component> describeStudy(ItemStudy study, int cell) {
        List<Component> lines = new ArrayList<>();
        String key = "gui.hextale.research_table.string.";
        int string = ItemStudy.stringOf(cell);
        if (string < 0) {
            return lines;
        }
        SimpleRune element = HexGrid.RING[string];
        int index = -1;
        int[] cells = ItemStudy.string(string);
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == cell) {
                index = i;
            }
        }
        lines.add(Component.translatable(key + "name", ResearchText.element(element), index + 1));
        ItemStudy.Echo echo = study.echoes().get(cell);
        if (echo != null) {
            ChatFormatting style = echo == ItemStudy.Echo.EXACT ? ChatFormatting.GOLD
                    : echo == ItemStudy.Echo.SILENT ? ChatFormatting.GRAY : ChatFormatting.AQUA;
            lines.add(Component.translatable(key + "echo." + echo.name().toLowerCase(Locale.ROOT)).withStyle(style));
        } else if (!study.isRevealed(element)) {
            lines.add(Component.translatable(study.mark(string) == index ? key + "marked" : key + "mark").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    private static int argb(int rgb, float alpha) {
        return PixelArt.argb(rgb, alpha);
    }
}
