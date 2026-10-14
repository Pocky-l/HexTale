package com.pockyl.hextale.client.screen.diary;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.knowledge.DiaryNote;
import com.pockyl.hextale.knowledge.Discoveries;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Hints: three suggested next discoveries (click one to see it in the codex) and the notes the diary wrote by itself,
 * newest first. Notes unread when the diary was opened keep a "new" marker for this visit.
 */
final class HintsSection implements DiarySection {
    private static final String KEY = "diary.hextale.hints.";
    private static final int SUGGESTIONS = 3;
    private static final int NOTE_GAP = 6;

    private final Set<DiaryNote> unreadAtOpen = EnumSet.noneOf(DiaryNote.class);
    private final List<int[]> suggestionAreas = new ArrayList<>();
    private List<ComplexRune> suggestions = List.of();

    HintsSection(RuneKnowledge knowledge) {
        for (DiaryNote note : knowledge.notesNewestFirst()) {
            if (!knowledge.isRead(note)) {
                unreadAtOpen.add(note);
            }
        }
    }

    @Override
    public Component title() {
        return Component.translatable(KEY + "title");
    }

    @Override
    public void icon(InkRenderer ink, float x, float y, int color) {
        int c = RuneDrawing.argb(color, 1);
        ink.ring(x, y - 1.5F, 3.2F, 4.4F, c, c, false);
        ink.stroke(new float[]{x - 1.5F, y + 4, x + 1.5F, y + 4}, 1.1F, c);
        ink.stroke(new float[]{x - 1, y + 6, x + 1, y + 6}, 1.1F, c);
    }

    @Override
    public void opened(DiaryContext context) {
        context.notesSeen();
    }

    /** Notes laid out over pages: the first spread's right page, then both pages of each following spread. */
    private List<List<DiaryNote>> notePages(DiaryContext context) {
        List<List<DiaryNote>> pages = new ArrayList<>();
        List<DiaryNote> page = new ArrayList<>();
        int capacity = DiaryContext.PAGE_BOTTOM - DiaryContext.PAGE_TOP - 16;
        int used = 0;
        Page probe = context.right();
        for (DiaryNote note : context.knowledge.notesNewestFirst()) {
            int height = noteHeight(probe, note);
            if (!page.isEmpty() && used + height > capacity) {
                pages.add(page);
                page = new ArrayList<>();
                used = 0;
            }
            page.add(note);
            used += height;
        }
        if (!page.isEmpty() || pages.isEmpty()) {
            pages.add(page);
        }
        return pages;
    }

    private static int noteHeight(Page probe, DiaryNote note) {
        return Page.LINE + probe.measure(Component.translatable(note.translationKey() + ".text"), 0) + NOTE_GAP;
    }

    @Override
    public int spreads(DiaryContext context) {
        return 1 + notePages(context).size() / 2;
    }

    @Override
    public void render(DiaryContext context, int spread, int mouseX, int mouseY) {
        List<List<DiaryNote>> pages = notePages(context);
        if (spread == 0) {
            renderSuggestions(context, context.left());
            renderNotes(context, context.right(), pages.get(0), true);
            return;
        }
        int leftIndex = spread * 2 - 1;
        if (leftIndex < pages.size()) {
            renderNotes(context, context.left(), pages.get(leftIndex), false);
        }
        if (leftIndex + 1 < pages.size()) {
            renderNotes(context, context.right(), pages.get(leftIndex + 1), false);
        }
    }

    private void renderSuggestions(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "next"));
        suggestions = Discoveries.suggest(context.knowledge, context.seed, SUGGESTIONS);
        suggestionAreas.clear();
        if (suggestions.isEmpty()) {
            page.text(Component.translatable(KEY + "complete"), DiaryContext.GOLD);
            return;
        }
        for (ComplexRune rune : suggestions) {
            int top = page.y;
            float y = page.y + 6;
            InkRenderer ink = InkRenderer.begin(context.graphics);
            RuneDrawing.radical(ink, rune.action(), page.x + 7, y, 11, RuneDrawing.Style.INK, 1);
            ink.stroke(new float[]{page.x + 15, y, page.x + 21, y, page.x + 19, y - 2, page.x + 21, y, page.x + 19, y + 2}, 0.8F,
                    RuneDrawing.argb(DiaryContext.MUTED, 1));
            RuneDrawing.radical(ink, rune.target(), page.x + 29, y, 11, RuneDrawing.Style.INK, 1);
            ink.end();
            context.graphics.drawString(context.font, Component.translatable(KEY + "unknown_rune").withStyle(ChatFormatting.BOLD),
                    page.x + 39, page.y + 2, 0xFF000000 | DiaryContext.HEADING, false);
            page.y += 14;
            page.text(RuneHints.hint(rune).copy().withStyle(ChatFormatting.ITALIC), DiaryContext.INK, 4);
            int height = page.y - top;
            suggestionAreas.add(new int[]{page.x, top, page.width, height});
            if (context.hovered(page.x, top, page.width, height)) {
                InkRenderer frame = InkRenderer.begin(context.graphics);
                frame.rect(page.x - 2, top - 1, page.x + page.width + 2, top + height - 2, RuneDrawing.argb(DiaryContext.GOLD, 0.1F));
                frame.end();
                context.tooltip(Component.translatable(KEY + "open_codex"));
            }
            page.space(4);
        }
    }

    private void renderNotes(DiaryContext context, Page page, List<DiaryNote> notes, boolean first) {
        if (first) {
            page.heading(Component.translatable(KEY + "notes"));
        }
        if (notes.isEmpty()) {
            page.text(Component.translatable(KEY + "no_notes"), DiaryContext.MUTED);
            return;
        }
        for (DiaryNote note : notes) {
            Component title = Component.translatable(note.translationKey()).withStyle(ChatFormatting.BOLD);
            context.graphics.drawString(context.font, title, page.x, page.y, 0xFF000000 | DiaryContext.HEADING, false);
            if (unreadAtOpen.contains(note)) {
                Component badge = Component.translatable(KEY + "new");
                int bx = page.x + page.width - context.font.width(badge) - 2;
                InkRenderer ink = InkRenderer.begin(context.graphics);
                ink.rect(bx - 2, page.y - 1, bx + context.font.width(badge) + 2, page.y + 9, RuneDrawing.argb(0x8E2A1E, 0.85F));
                ink.end();
                context.graphics.drawString(context.font, badge, bx, page.y, 0xFFF3E3B8, false);
            }
            page.y += Page.LINE;
            page.text(Component.translatable(note.translationKey() + ".text"));
            page.space(NOTE_GAP - 3);
        }
    }

    @Override
    public boolean mouseClicked(DiaryContext context, int spread, double mouseX, double mouseY) {
        if (spread != 0) {
            return false;
        }
        for (int i = 0; i < suggestionAreas.size() && i < suggestions.size(); i++) {
            int[] area = suggestionAreas.get(i);
            if (mouseX >= area[0] && mouseX < area[0] + area[2] && mouseY >= area[1] && mouseY < area[1] + area[3]) {
                context.openCodex(suggestions.get(i));
                return true;
            }
        }
        return false;
    }
}
