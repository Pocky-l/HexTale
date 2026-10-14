package com.pockyl.hextale.client.screen.diary;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.knowledge.Milestone;
import com.pockyl.hextale.rune.ComplexRunes;

/** Progress through the codex and the six milestones; what they unlock arrives in later updates ("soon"). */
final class MilestonesSection implements DiarySection {
    private static final String KEY = "diary.hextale.milestones.";

    @Override
    public Component title() {
        return Component.translatable(KEY + "title");
    }

    @Override
    public void icon(InkRenderer ink, float x, float y, int color) {
        int c = RuneDrawing.argb(color, 1);
        ink.stroke(new float[]{x - 3, y + 6, x - 3, y - 6}, 1.2F, c);
        ink.stroke(new float[]{x - 3, y - 6, x + 4, y - 4, x - 3, y - 1}, 1.2F, c);
    }

    @Override
    public int spreads(DiaryContext context) {
        return 1;
    }

    @Override
    public void render(DiaryContext context, int spread, int mouseX, int mouseY) {
        int learned = context.knowledge.runeCount();
        Page left = context.left();
        left.heading(Component.translatable(KEY + "title"));
        progressBar(context, left, learned);
        Milestone next = Milestone.next(learned);
        left.centered(next == null ? Component.translatable(KEY + "all_done")
                : Component.translatable(KEY + "next", next.count() - learned), next == null ? DiaryContext.GOLD : DiaryContext.MUTED);
        left.space(3);
        Milestone[] milestones = Milestone.values();
        Page right = context.right();
        for (int i = 0; i < milestones.length; i++) {
            entry(context, i < 3 ? left : right, milestones[i], learned);
        }
    }

    private static void progressBar(DiaryContext context, Page page, int learned) {
        Component count = Component.translatable(KEY + "progress", learned, ComplexRunes.COUNT).withStyle(ChatFormatting.BOLD);
        float x0 = page.x + 2;
        float x1 = page.x + page.width - context.font.width(count) - 6;
        float y = page.y + 4;
        float fill = learned / (float) ComplexRunes.COUNT;
        InkRenderer ink = InkRenderer.begin(context.graphics);
        ink.rect(x0, y - 3, x1, y + 3, RuneDrawing.argb(0x6B4A26, 0.25F));
        if (fill > 0) {
            ink.rect(x0, y - 3, x0 + (x1 - x0) * fill, y + 3, RuneDrawing.argb(0xD6AB52, 1));
            ink.rect(x0, y - 3, x0 + (x1 - x0) * fill, y - 1, RuneDrawing.argb(0xF3D98C, 1));
        }
        for (Milestone milestone : Milestone.values()) {
            float mx = x0 + (x1 - x0) * milestone.count() / ComplexRunes.COUNT;
            ink.stroke(new float[]{mx, y - 4.5F, mx, y + 4.5F}, 0.8F,
                    RuneDrawing.argb(learned >= milestone.count() ? DiaryContext.GOLD : DiaryContext.HEADING, 0.8F));
        }
        ink.stroke(new float[]{x0, y - 3, x1, y - 3, x1, y + 3, x0, y + 3, x0, y - 3}, 0.8F, RuneDrawing.argb(DiaryContext.HEADING, 0.7F));
        ink.end();
        context.graphics.drawString(context.font, count, (int) x1 + 5, (int) y - 4, 0xFF000000 | DiaryContext.HEADING, false);
        page.y += 11;
    }

    private static void entry(DiaryContext context, Page page, Milestone milestone, int learned) {
        boolean reached = learned >= milestone.count();
        int badgeX = page.x + 9;
        int badgeY = page.y + 6;
        InkRenderer ink = InkRenderer.begin(context.graphics);
        int color = reached ? 0xD6AB52 : 0xB9A27A;
        ink.disc(badgeX, badgeY, 8.5F, RuneDrawing.argb(color, reached ? 1 : 0.45F), RuneDrawing.argb(color, reached ? 1 : 0.45F), false);
        ink.ring(badgeX, badgeY, 8.5F, 9.4F, RuneDrawing.argb(DiaryContext.HEADING, 0.7F), RuneDrawing.argb(DiaryContext.HEADING, 0.7F), false);
        ink.end();
        String count = String.valueOf(milestone.count());
        context.graphics.drawString(context.font, count, badgeX - context.font.width(count) / 2 + 1, badgeY - 3,
                reached ? 0xFF3B2A1A : 0xFF6B5A3E, false);
        Component title = Component.translatable(milestone.translationKey()).withStyle(ChatFormatting.BOLD);
        context.graphics.drawString(context.font, title, page.x + 22, page.y + 2, 0xFF000000 | (reached ? DiaryContext.GOLD : DiaryContext.HEADING),
                false);
        page.y += 12;
        Page body = new Page(context, page.x + 22, page.y, page.width - 22, page.bottom);
        body.text(Component.translatable(milestone.translationKey() + ".unlocks").append(" ")
                .append(Component.translatable(KEY + "soon").withStyle(ChatFormatting.ITALIC).withColor(DiaryContext.GOLD)), DiaryContext.INK);
        page.y = body.y + 1;
    }
}
