package com.pockyl.hextale.client.screen.diary;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.knowledge.Discoveries;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * The codex: a 9 x 9 grid, rows are the action element, columns the target. Learned runes show their hieroglyph in
 * the category colour, unknown ones a faint "?". The right page describes the selected rune or hints at it.
 */
final class CodexSection implements DiarySection {
    private static final String KEY = "diary.hextale.codex.";
    private static final int CELL = 13;
    private static final int ELEMENTS = SimpleRune.values().length;

    @Nullable
    private ComplexRune selected;
    @Nullable
    private SimpleRune selectedSame;

    void select(ComplexRune rune) {
        selected = rune;
        selectedSame = null;
    }

    @Override
    public Component title() {
        return Component.translatable(KEY + "title");
    }

    @Override
    public void icon(InkRenderer ink, float x, float y, int color) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                ink.disc(x + i * 3.5F, y + j * 3.5F, 1.1F, RuneDrawing.argb(color, 1), RuneDrawing.argb(color, 1), false);
            }
        }
    }

    @Override
    public int spreads(DiaryContext context) {
        return 1;
    }

    @Override
    public void opened(DiaryContext context) {
        if (selected == null && selectedSame == null) {
            List<ComplexRune> runes = context.knowledge.runes();
            selected = runes.isEmpty() ? first(Discoveries.suggest(context.knowledge, context.seed, 1)) : runes.get(runes.size() - 1);
        }
    }

    @Nullable
    private static ComplexRune first(List<ComplexRune> runes) {
        return runes.isEmpty() ? null : runes.get(0);
    }

    private static int gridX(DiaryContext context) {
        return context.bookLeft + DiaryContext.LEFT_PAGE_X + (DiaryContext.PAGE_WIDTH - CELL * (ELEMENTS + 1)) / 2;
    }

    private static int gridY(DiaryContext context) {
        return context.bookTop + DiaryContext.PAGE_TOP + 14;
    }

    @Override
    public void render(DiaryContext context, int spread, int mouseX, int mouseY) {
        RuneKnowledge knowledge = context.knowledge;
        Page left = context.left();
        left.centered(Component.translatable(KEY + "count", knowledge.runeCount(), ComplexRunes.COUNT).withStyle(ChatFormatting.BOLD),
                DiaryContext.HEADING);
        int x0 = gridX(context);
        int y0 = gridY(context);
        SimpleRune[] elements = SimpleRune.values();
        InkRenderer ink = InkRenderer.begin(context.graphics);
        for (int i = 0; i < ELEMENTS; i++) {
            float header = CELL / 2.0F;
            RuneDrawing.radical(ink, elements[i], x0 + CELL * (i + 1) + header, y0 + header, 9.5F, RuneDrawing.Style.INK, 1);
            RuneDrawing.radical(ink, elements[i], x0 + header, y0 + CELL * (i + 1) + header, 9.5F, RuneDrawing.Style.INK, 1);
        }
        ink.stroke(new float[]{x0 + CELL - 0.5F, y0 + 2, x0 + CELL - 0.5F, y0 + CELL * (ELEMENTS + 1)}, 0.6F,
                RuneDrawing.argb(DiaryContext.HEADING, 0.45F));
        ink.stroke(new float[]{x0 + 2, y0 + CELL - 0.5F, x0 + CELL * (ELEMENTS + 1), y0 + CELL - 0.5F}, 0.6F,
                RuneDrawing.argb(DiaryContext.HEADING, 0.45F));
        for (int a = 0; a < ELEMENTS; a++) {
            for (int b = 0; b < ELEMENTS; b++) {
                float cx = x0 + CELL * (b + 1) + CELL / 2.0F;
                float cy = y0 + CELL * (a + 1) + CELL / 2.0F;
                if (a == b) {
                    ink.disc(cx, cy, 0.9F, RuneDrawing.argb(DiaryContext.MUTED, 0.6F), RuneDrawing.argb(DiaryContext.MUTED, 0.6F), false);
                    continue;
                }
                ComplexRune rune = ComplexRunes.of(elements[a], elements[b]);
                if (knowledge.knows(rune)) {
                    ink.rect(cx - CELL / 2.0F + 0.5F, cy - CELL / 2.0F + 0.5F, cx + CELL / 2.0F - 0.5F, cy + CELL / 2.0F - 0.5F,
                            RuneDrawing.argb(rune.color(), 0.22F));
                    RuneDrawing.glyph(ink, rune, cx, cy, 9.5F, RuneDrawing.Style.INK, 1);
                }
            }
        }
        ink.end();
        for (int a = 0; a < ELEMENTS; a++) {
            for (int b = 0; b < ELEMENTS; b++) {
                if (a == b) {
                    continue;
                }
                ComplexRune rune = ComplexRunes.of(elements[a], elements[b]);
                int cellX = x0 + CELL * (b + 1);
                int cellY = y0 + CELL * (a + 1);
                if (!knowledge.knows(rune)) {
                    context.graphics.drawString(context.font, "?", cellX + 4, cellY + 3, 0x55000000 | DiaryContext.MUTED, false);
                }
                boolean hovered = context.hovered(cellX, cellY, CELL, CELL);
                if (hovered || rune == selected) {
                    frame(context, cellX, cellY, rune == selected ? DiaryContext.GOLD : DiaryContext.HEADING, rune == selected ? 1 : 0.6F);
                }
                if (hovered) {
                    context.tooltip(knowledge.knows(rune) ? Component.translatable(rune.translationKey()).withColor(rune.color())
                            : Component.translatable(KEY + "unknown_tooltip", Component.translatable(rune.action().translationKey()),
                            Component.translatable(rune.target().translationKey())));
                }
            }
            int diagonalX = x0 + CELL * (a + 1);
            int diagonalY = y0 + CELL * (a + 1);
            if (selectedSame == elements[a]) {
                frame(context, diagonalX, diagonalY, DiaryContext.RED, 1);
            }
        }
        left.y = y0 + CELL * (ELEMENTS + 1) + 4;
        left.text(Component.translatable(KEY + "legend"), DiaryContext.MUTED);

        details(context, context.right());
    }

    private static void frame(DiaryContext context, int x, int y, int color, float alpha) {
        InkRenderer ink = InkRenderer.begin(context.graphics);
        ink.stroke(new float[]{x, y, x + CELL, y, x + CELL, y + CELL, x, y + CELL, x, y}, 0.9F, RuneDrawing.argb(color, alpha));
        ink.end();
    }

    private void details(DiaryContext context, Page page) {
        if (selectedSame != null) {
            page.heading(Component.translatable(KEY + "same.title"));
            formula(context, page, selectedSame, selectedSame, null);
            page.text(Component.translatable(KEY + "same.text"), DiaryContext.RED);
            return;
        }
        ComplexRune rune = selected;
        if (rune == null) {
            page.heading(Component.translatable(KEY + "empty.title"));
            page.text(Component.translatable(KEY + "empty.text"), DiaryContext.MUTED);
            return;
        }
        boolean known = context.knowledge.knows(rune);
        if (known) {
            Component name = Component.translatable(rune.translationKey()).withStyle(ChatFormatting.BOLD);
            page.centered(name, RuneDrawing.darken(rune.color(), 0.5F));
            page.space(1);
            float cx = page.x + page.width / 2.0F;
            float cy = page.y + 23;
            InkRenderer ink = InkRenderer.begin(context.graphics);
            ink.disc(cx, cy, 26, RuneDrawing.argb(rune.color(), 0.18F), RuneDrawing.argb(rune.color(), 0), false);
            RuneDrawing.glyph(ink, rune, cx, cy, 38, RuneDrawing.Style.INK, 1);
            ink.end();
            page.y += 48;
            formula(context, page, rune.action(), rune.target(), rune);
            page.centered(Component.translatable(rune.category().translationKey()), RuneDrawing.darken(rune.color(), 0.55F));
            page.space(3);
            page.text(Component.translatable(rune.translationKey() + ".effect"));
            page.text(Component.translatable(KEY + "example", Component.translatable(rune.translationKey() + ".use"))
                    .withStyle(ChatFormatting.ITALIC), DiaryContext.MUTED);
        } else {
            page.heading(Component.translatable(KEY + "unknown.title"));
            formula(context, page, rune.action(), rune.target(), null);
            page.space(2);
            page.text(RuneHints.hint(rune).copy().withStyle(ChatFormatting.ITALIC));
            page.text(RuneHints.elements(rune), DiaryContext.MUTED);
            page.text(Component.translatable(KEY + "unknown.how", Component.translatable(rune.action().translationKey()),
                    Component.translatable(rune.target().translationKey())), DiaryContext.MUTED);
        }
    }

    /** "radical -> radical = hieroglyph" centred on the page; an unknown result is a question mark. */
    private static void formula(DiaryContext context, Page page, SimpleRune action, SimpleRune target, @Nullable ComplexRune result) {
        float cx = page.x + page.width / 2.0F;
        float y = page.y + 9;
        int muted = RuneDrawing.argb(DiaryContext.MUTED, 1);
        InkRenderer ink = InkRenderer.begin(context.graphics);
        RuneDrawing.radical(ink, action, cx - 40, y, 15, RuneDrawing.Style.INK, 1);
        ink.stroke(new float[]{cx - 29, y, cx - 19, y}, 0.9F, muted);
        ink.stroke(new float[]{cx - 22, y - 2.5F, cx - 19, y, cx - 22, y + 2.5F}, 0.9F, muted);
        RuneDrawing.radical(ink, target, cx - 8, y, 15, RuneDrawing.Style.INK, 1);
        ink.stroke(new float[]{cx + 3, y - 1.5F, cx + 10, y - 1.5F}, 0.9F, muted);
        ink.stroke(new float[]{cx + 3, y + 1.5F, cx + 10, y + 1.5F}, 0.9F, muted);
        if (result != null) {
            RuneDrawing.glyph(ink, result, cx + 24, y, 17, RuneDrawing.Style.INK, 1);
        }
        ink.end();
        if (result == null) {
            context.graphics.drawString(context.font, action == target ? "✕" : "?", (int) cx + 21, (int) y - 4,
                    0xFF000000 | (action == target ? DiaryContext.RED : DiaryContext.MUTED), false);
        }
        page.y += 22;
    }

    @Override
    public boolean mouseClicked(DiaryContext context, int spread, double mouseX, double mouseY) {
        int x0 = gridX(context);
        int y0 = gridY(context);
        int column = (int) Math.floor((mouseX - x0) / CELL) - 1;
        int row = (int) Math.floor((mouseY - y0) / CELL) - 1;
        if (column < 0 || row < 0 || column >= ELEMENTS || row >= ELEMENTS) {
            return false;
        }
        SimpleRune[] elements = SimpleRune.values();
        if (row == column) {
            selected = null;
            selectedSame = elements[row];
        } else {
            select(ComplexRunes.of(elements[row], elements[column]));
        }
        return true;
    }
}
