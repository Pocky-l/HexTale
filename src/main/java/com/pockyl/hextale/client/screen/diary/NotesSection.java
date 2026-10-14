package com.pockyl.hextale.client.screen.diary;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.knowledge.DiaryNote;
import com.pockyl.hextale.knowledge.RuneKnowledge;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The notes the diary wrote by itself, newest first, over as many pages as they need. Notes unread when the diary was
 * opened keep a "new" marker for this visit.
 */
final class NotesSection implements DiarySection {
    private static final String KEY = "diary.hextale.notes.";
    private static final int NOTE_GAP = 6;

    private final Set<DiaryNote> unreadAtOpen = EnumSet.noneOf(DiaryNote.class);

    NotesSection(RuneKnowledge knowledge) {
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

    /** Notes laid out over pages; the first page also carries the heading. */
    private List<List<DiaryNote>> notePages(DiaryContext context) {
        List<List<DiaryNote>> pages = new ArrayList<>();
        List<DiaryNote> page = new ArrayList<>();
        int full = DiaryContext.PAGE_BOTTOM - DiaryContext.PAGE_TOP;
        int capacity = full - 16;
        int used = 0;
        Page probe = context.right();
        for (DiaryNote note : context.knowledge.notesNewestFirst()) {
            int height = Page.LINE + probe.measure(Component.translatable(note.translationKey() + ".text"), 0) + NOTE_GAP;
            if (!page.isEmpty() && used + height > capacity) {
                pages.add(page);
                page = new ArrayList<>();
                used = 0;
                capacity = full;
            }
            page.add(note);
            used += height;
        }
        if (!page.isEmpty() || pages.isEmpty()) {
            pages.add(page);
        }
        return pages;
    }

    @Override
    public int spreads(DiaryContext context) {
        return (notePages(context).size() + 1) / 2;
    }

    @Override
    public void render(DiaryContext context, int spread, int mouseX, int mouseY) {
        List<List<DiaryNote>> pages = notePages(context);
        int leftIndex = spread * 2;
        if (leftIndex < pages.size()) {
            renderNotes(context, context.left(), pages.get(leftIndex), leftIndex == 0);
        }
        if (leftIndex + 1 < pages.size()) {
            renderNotes(context, context.right(), pages.get(leftIndex + 1), false);
        }
    }

    private void renderNotes(DiaryContext context, Page page, List<DiaryNote> notes, boolean first) {
        if (first) {
            page.heading(Component.translatable(KEY + "title"));
        }
        if (notes.isEmpty()) {
            page.text(Component.translatable(KEY + "none"), DiaryContext.MUTED);
            return;
        }
        for (DiaryNote note : notes) {
            Component title = Component.translatable(note.translationKey()).withStyle(ChatFormatting.BOLD);
            context.graphics.drawString(context.font, title, page.x, page.y, 0xFF000000 | DiaryContext.HEADING, false);
            if (unreadAtOpen.contains(note)) {
                Component badge = Component.translatable(KEY + "new");
                int bx = page.x + page.width - context.font.width(badge) - 2;
                context.graphics.fill(bx - 2, page.y - 1, bx + context.font.width(badge) + 2, page.y + 9, 0xD98E2A1E);
                context.graphics.drawString(context.font, badge, bx, page.y, 0xFFF3E3B8, false);
            }
            page.y += Page.LINE;
            page.text(Component.translatable(note.translationKey() + ".text"));
            page.space(NOTE_GAP - 3);
        }
    }
}
