package com.pockyl.hextale.client.screen.diary;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;

import java.util.List;

/** A writing cursor on one page: headings and wrapped paragraphs flow down from the top. */
final class Page {
    static final int LINE = 10;

    final DiaryContext context;
    final int x;
    final int top;
    final int width;
    final int bottom;
    int y;

    Page(DiaryContext context, int x, int top, int width, int bottom) {
        this.context = context;
        this.x = x;
        this.top = top;
        this.width = width;
        this.bottom = bottom;
        this.y = top;
    }

    /** A bold title with a thin ink rule under it. */
    Page heading(Component text) {
        Component bold = text.copy().withStyle(ChatFormatting.BOLD);
        context.graphics.drawString(context.font, bold, x + (width - context.font.width(bold)) / 2, y, 0xFF000000 | DiaryContext.HEADING, false);
        y += LINE + 1;
        InkRenderer ink = InkRenderer.begin(context.graphics);
        float mid = x + width / 2.0F;
        ink.stroke(new float[]{mid - 34, y, mid - 4, y}, 0.8F, RuneDrawing.argb(DiaryContext.HEADING, 0.55F));
        ink.disc(mid, y, 1.4F, RuneDrawing.argb(DiaryContext.GOLD, 0.9F), RuneDrawing.argb(DiaryContext.GOLD, 0.9F), false);
        ink.stroke(new float[]{mid + 4, y, mid + 34, y}, 0.8F, RuneDrawing.argb(DiaryContext.HEADING, 0.55F));
        ink.end();
        y += 5;
        return this;
    }

    Page text(Component text) {
        return text(text, DiaryContext.INK);
    }

    Page text(Component text, int color) {
        return text(text, color, 0);
    }

    /** A wrapped paragraph, indented by {@code indent} pixels. */
    Page text(Component text, int color, int indent) {
        List<FormattedCharSequence> lines = context.font.split(text, width - indent);
        for (FormattedCharSequence line : lines) {
            context.graphics.drawString(context.font, line, x + indent, y, 0xFF000000 | color, false);
            y += LINE;
        }
        y += 3;
        return this;
    }

    Page centered(Component text, int color) {
        context.graphics.drawString(context.font, text, x + (width - context.font.width(text)) / 2, y, 0xFF000000 | color, false);
        y += LINE;
        return this;
    }

    Page space(int pixels) {
        y += pixels;
        return this;
    }

    /** Height a paragraph would take. */
    int measure(Component text, int indent) {
        return context.font.split(text, width - indent).size() * LINE + 3;
    }

    boolean fits(int height) {
        return y + height <= bottom;
    }
}
