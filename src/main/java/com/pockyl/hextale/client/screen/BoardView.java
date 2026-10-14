package com.pockyl.hextale.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.research.RuneBoard;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Draws the research parchment in pixel art: hexagonal sections on the nonagon of the background texture, dark runes,
 * points, the symbols written so far with their ink lines, the orb in the centre and a preview of the next symbol.
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
    /** Fog radius (parchment units) around sections the ink has passed, by lens strength 0-2; 3 sees everything. */
    private static final double[] SIGHT = {0.26, 0.4, 0.6};
    private static final int INK = 0x3B2A1A;
    private static final int WALL = 0x1E1628;
    private static final int WALL_RIM = 0x4A3A66;
    private static final long INK_MILLIS = 380;
    private static final long BURST_MILLIS = 800;

    private int left;
    private int top;
    private final boolean[] visible = new boolean[HexGrid.COUNT];
    /** Animation of the newest symbol's ink line and of a filled point. */
    private long inkStart;
    private int inkMarks;
    private long burstStart;
    private int burstCell = -1;
    private int burstColor;
    private long fadeStart;
    private int fadeElement = -1;
    private int lastFilled;
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

    boolean visible(int cell) {
        return visible[cell];
    }

    /** Notices new symbols and filled points to animate them. */
    void observe(RuneBoard board, ResearchSheet sheet) {
        long now = Util.getMillis();
        if (!primed) {
            primed = true;
            inkMarks = sheet.used();
            lastFilled = sheet.filledMask();
            return;
        }
        if (sheet.used() > inkMarks) {
            inkStart = now;
        }
        inkMarks = sheet.used();
        int filled = sheet.filledMask();
        int added = filled & ~lastFilled;
        if (added != 0 && sheet.isOpen()) {
            int point = Integer.numberOfTrailingZeros(added);
            RuneBoard.Point p = board.points().get(point);
            burstCell = p.cell();
            burstColor = p.element().color();
            burstStart = now;
            fadeElement = p.element().ordinal();
            fadeStart = now;
        }
        lastFilled = filled;
    }

    /** The preview of the next symbol: where a known item lands, or only the direction of an untried one. */
    record Preview(ItemElements elements, boolean known, boolean ground) {
    }

    void render(GuiGraphics graphics, Font font, RuneBoard board, ResearchSheet sheet, @Nullable Preview preview,
                int hoveredCell, int forkLevel) {
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
                if (sheet.isFilled(point)) {
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

        if (preview != null && sheet.canWrite()) {
            renderPreview(graphics, font, board, sheet, preview, now);
        }
        if (sheet.isActive() && sheet.from() != HexGrid.CENTRE) {
            PixelArt.hexRing(graphics, left + px(sheet.from()), top + py(sheet.from()), CELL + 2, argb(0x2B8FB8, 0.6F + 0.4F * pulse));
        }
        // The orb lies in the centre.
        int ox = left + px(HexGrid.CENTRE);
        int oy = top + py(HexGrid.CENTRE);
        PixelArt.hex(graphics, ox, oy, CELL + 2, argb(0x5CC8F2, 0.25F + 0.2F * pulse), argb(0x2B8FB8, 1));
        graphics.renderItem(new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), ox - 8, oy - 8);
        if (forkLevel > 0 && sheet.isActive() && sheet.canJump(board, HexGrid.CENTRE, forkLevel) && hoveredCell == HexGrid.CENTRE) {
            PixelArt.hexRing(graphics, ox, oy, CELL + 4, argb(0xE0A21C, 1));
        }

        for (SimpleRune element : HexGrid.RING) {
            double[] u = HexGrid.unit(element);
            int sx = left + CX + (int) Math.round(u[0] * KX * 1.03);
            int sy = top + CY + (int) Math.round(u[1] * KY * 1.03);
            PixelArt.radical(graphics, element, sx - 4, sy - 4, 9, argb(element.color(), 1));
        }

        if (burstCell >= 0) {
            float t = (now - burstStart) / (float) BURST_MILLIS;
            if (t < 1) {
                int width = CELL + 2 * (int) (t * 14) | 1;
                PixelArt.hexRing(graphics, left + px(burstCell), top + py(burstCell), width, argb(burstColor, 1 - t));
            } else {
                burstCell = -1;
            }
        }
        if (hoveredCell >= 0) {
            PixelArt.hexRing(graphics, left + px(hoveredCell), top + py(hoveredCell), CELL, argb(0xFFFFFF, 0.6F));
        }
    }

    private void renderPreview(GuiGraphics graphics, Font font, RuneBoard board, ResearchSheet sheet, Preview preview, long now) {
        int phase = (int) (now / 90 % 6);
        int fx = left + px(sheet.from());
        int fy = top + py(sheet.from());
        if (preview.known()) {
            HexGrid.Walk walk = sheet.preview(board, preview.elements(), preview.ground());
            int point = board.pointAt(walk.end());
            boolean good = point >= 0 && !sheet.isFilled(point);
            int color = good ? 0xFFC88A12 : 0xCC3B2A1A;
            int x0 = fx;
            int y0 = fy;
            for (int cell : walk.cells()) {
                int x1 = left + px(cell);
                int y1 = top + py(cell);
                PixelArt.dotted(graphics, x0, y0, x1, y1, 3, 3, phase, color);
                x0 = x1;
                y0 = y1;
            }
            symbol(graphics, walk.end(), preview.elements().main(), 0.45F);
            PixelArt.hexRing(graphics, left + px(walk.end()), top + py(walk.end()), CELL + 2, color);
        } else {
            double[] v = preview.elements().vector(preview.ground());
            double length = Math.hypot(v[0] * KX, v[1] * KY);
            if (length < 0.01) {
                return;
            }
            double ux = v[0] * KX / length;
            double uy = v[1] * KY / length;
            int ex = fx + (int) Math.round(ux * 30);
            int ey = fy + (int) Math.round(uy * 30);
            PixelArt.dotted(graphics, fx, fy, ex, ey, 2, 3, phase, 0xCC3B2A1A);
            int ax = (int) Math.round(-uy * 4);
            int ay = (int) Math.round(ux * 4);
            int bx = ex - (int) Math.round(ux * 5);
            int by = ey - (int) Math.round(uy * 5);
            PixelArt.line(graphics, ex, ey, bx + ax, by + ay, 0xCC3B2A1A);
            PixelArt.line(graphics, ex, ey, bx - ax, by - ay, 0xCC3B2A1A);
            graphics.drawString(font, "?", ex + (int) Math.round(ux * 6) - 2, ey + (int) Math.round(uy * 6) - 4, 0xFF3B2A1A, false);
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

    /** What the hovered section is, for a tooltip. */
    List<Component> describe(RuneBoard board, ResearchSheet sheet, int cell, int forkLevel) {
        List<Component> lines = new ArrayList<>();
        int point = board.pointAt(cell);
        SimpleRune wall = board.walls()[cell];
        int dissolved = board.dissolved(sheet.isOpen() ? sheet.filledMask() : 0);
        String key = "gui.hextale.research_table.cell.";
        if (point >= 0) {
            RuneBoard.Point p = board.points().get(point);
            lines.add(Component.translatable(key + "point", ResearchText.element(p.element())));
            if (sheet.isOpen() && sheet.isFilled(point)) {
                lines.add(Component.translatable(key + "point.filled").withStyle(ChatFormatting.GREEN));
                if (forkLevel >= 2 && sheet.canJump(board, cell, forkLevel)) {
                    lines.add(Component.translatable(key + "fork").withStyle(ChatFormatting.GOLD));
                }
            } else {
                lines.add(Component.translatable(key + "point.needs", p.resource().getDescription()).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable(sheet.isOpen() && sheet.markAt(cell) != null ? key + "point.ready" : key + "point.cover")
                        .withStyle(ChatFormatting.DARK_GRAY));
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
            if (forkLevel > 0 && sheet.canJump(board, cell, forkLevel)) {
                lines.add(Component.translatable(key + "fork").withStyle(ChatFormatting.GOLD));
            }
        } else {
            ResearchSheet.Mark mark = sheet.isOpen() ? sheet.markAt(cell) : null;
            if (mark != null) {
                lines.add(Component.translatable(key + "symbol", ResearchText.element(mark.element())));
            }
        }
        return lines;
    }

    private static int argb(int rgb, float alpha) {
        return PixelArt.argb(rgb, alpha);
    }
}
