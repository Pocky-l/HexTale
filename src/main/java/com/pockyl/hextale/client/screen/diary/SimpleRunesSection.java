package com.pockyl.hextale.client.screen.diary;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.RuneCategory;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.Locale;

/**
 * The nine simple runes, one to a page and drawn large on a calligraphy practice grid, then a page on how they
 * combine into complex runes.
 */
final class SimpleRunesSection implements DiarySection {
    private static final String KEY = "diary.hextale.simple.";
    private static final int BOX = 58;

    @Override
    public Component title() {
        return Component.translatable(KEY + "title");
    }

    @Override
    public void icon(InkRenderer ink, float x, float y, int color) {
        RuneDrawing.strokes(ink, SimpleRune.FIRE.radical(), x, y, 12, color, RuneDrawing.Style.FAINT, 3.3F);
    }

    @Override
    public int spreads(DiaryContext context) {
        return (SimpleRune.values().length + 2) / 2;
    }

    @Override
    public void render(DiaryContext context, int spread, int mouseX, int mouseY) {
        renderPage(context, context.left(), spread * 2);
        renderPage(context, context.right(), spread * 2 + 1);
    }

    private static void renderPage(DiaryContext context, Page page, int index) {
        SimpleRune[] runes = SimpleRune.values();
        if (index < runes.length) {
            entry(context, page, runes[index]);
        } else if (index == runes.length) {
            combining(context, page);
        }
    }

    private static void entry(DiaryContext context, Page page, SimpleRune rune) {
        Component name = Component.translatable(rune.translationKey()).withStyle(ChatFormatting.BOLD);
        context.graphics.drawString(context.font, name, page.x + (page.width - context.font.width(name)) / 2, page.y,
                0xFF000000 | RuneDrawing.darken(rune.color(), 0.5F), false);
        page.y += 12;
        float boxX = page.x + (page.width - BOX) / 2.0F;
        float boxY = page.y;
        InkRenderer ink = InkRenderer.begin(context.graphics);
        practiceGrid(ink, boxX, boxY, BOX);
        RuneDrawing.radical(ink, rune, boxX + BOX / 2.0F, boxY + BOX / 2.0F, BOX - 12, RuneDrawing.Style.INK, 1);
        ink.end();
        page.y += BOX + 3;
        page.centered(Component.translatable("diary.hextale.strokes", rune.strokeCount()), DiaryContext.MUTED);
        page.space(2);
        page.text(Component.translatable(rune.translationKey() + ".hint").withStyle(ChatFormatting.ITALIC), DiaryContext.MUTED);
        page.text(Component.translatable(KEY + "as_action").withStyle(ChatFormatting.BOLD)
                .append(" ").append(Component.translatable(rune.translationKey() + ".action").withStyle(style -> style.withBold(false))));
        page.y -= 2;
        page.text(Component.translatable(KEY + "as_target").withStyle(ChatFormatting.BOLD)
                .append(" ").append(Component.translatable(rune.translationKey() + ".target").withStyle(style -> style.withBold(false))));
    }

    /** A square with its centre lines and diagonals, as on calligraphy practice paper. */
    private static void practiceGrid(InkRenderer ink, float x, float y, float size) {
        int frame = RuneDrawing.argb(0x8E2A1E, 0.45F);
        int guide = RuneDrawing.argb(0x8E2A1E, 0.16F);
        ink.rect(x, y, x + size, y + size, RuneDrawing.argb(0xFFF6E0, 0.35F));
        ink.stroke(new float[]{x, y, x + size, y, x + size, y + size, x, y + size, x, y}, 0.9F, frame);
        dashed(ink, x + size / 2, y, x + size / 2, y + size, guide);
        dashed(ink, x, y + size / 2, x + size, y + size / 2, guide);
        dashed(ink, x, y, x + size, y + size, guide);
        dashed(ink, x + size, y, x, y + size, guide);
    }

    private static void dashed(InkRenderer ink, float x0, float y0, float x1, float y1, int color) {
        int dashes = 9;
        for (int i = 0; i < dashes; i++) {
            float a = i / (float) dashes;
            float b = (i + 0.55F) / dashes;
            ink.stroke(new float[]{x0 + (x1 - x0) * a, y0 + (y1 - y0) * a, x0 + (x1 - x0) * b, y0 + (y1 - y0) * b}, 0.6F, color);
        }
    }

    /** How simple runes become complex ones, with one example hieroglyph per structure. */
    private static void combining(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "combining.title"));
        page.text(Component.translatable(KEY + "combining.text"));
        RuneCategory.Layout[] layouts = RuneCategory.Layout.values();
        SimpleRune[][] examples = {{SimpleRune.FIRE, SimpleRune.EARTH}, {SimpleRune.LIFE, SimpleRune.METAL},
                {SimpleRune.WATER, SimpleRune.FIRE}, {SimpleRune.LIGHT, SimpleRune.EARTH}};
        int cell = page.width / layouts.length;
        int y = page.y + 11;
        InkRenderer ink = InkRenderer.begin(context.graphics);
        for (int i = 0; i < layouts.length; i++) {
            RuneDrawing.glyph(ink, ComplexRunes.of(examples[i][0], examples[i][1]), page.x + cell * i + cell / 2.0F, y, 20,
                    RuneDrawing.Style.INK, 1);
        }
        ink.end();
        for (int i = 0; i < layouts.length; i++) {
            Component label = Component.translatable(KEY + "layout." + layouts[i].name().toLowerCase(Locale.ROOT));
            context.graphics.pose().pushPose();
            context.graphics.pose().translate(page.x + cell * i + cell / 2.0F, y + 14, 0);
            context.graphics.pose().scale(0.75F, 0.75F, 1);
            context.graphics.drawString(context.font, label, -context.font.width(label) / 2, 0, 0xFF000000 | DiaryContext.MUTED, false);
            context.graphics.pose().popPose();
        }
        page.y = y + 25;
        page.text(Component.translatable(KEY + "combining.categories"), DiaryContext.MUTED);
    }
}
