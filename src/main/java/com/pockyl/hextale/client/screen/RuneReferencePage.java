package com.pockyl.hextale.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * The researcher's diary lying open beside the Research Table: a reference page with all nine simple runes. Hovering a
 * rune describes it (strokes, how it is drawn, what it means as action and as target, how many complex runes with it
 * are known); clicking it shows the radical as an outline to trace on the parchment. Without the player's own diary on
 * the table the book lies closed.
 */
final class RuneReferencePage {
    static final int WIDTH = 108;
    static final int HEIGHT = 226;
    private static final int CONTENT_X = 13;
    private static final int CONTENT_WIDTH = 84;
    private static final int CELL = 26;
    private static final int GRID_Y = 26;
    private static final int DETAILS_Y = GRID_Y + CELL * 3 + 5;
    private static final int DETAILS_BOTTOM = HEIGHT - 16;
    private static final int INK = 0x3B2A1A;
    private static final int HEADING = 0x6B3A12;
    private static final int MUTED = 0x7A6648;
    private static final int GOLD = 0x9A6B12;
    private static final int COVER_TEXT = 0xE9D3A8;

    enum State {
        OPEN,
        NO_DIARY,
        FOREIGN_DIARY
    }

    private final Font font;
    private int x;
    private int y;

    RuneReferencePage(Font font) {
        this.font = font;
    }

    void layout(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** The rune under the mouse, or null. */
    @Nullable
    SimpleRune runeAt(double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - x - CONTENT_X) / CELL);
        int row = (int) Math.floor((mouseY - y - GRID_Y) / CELL);
        if (column < 0 || column > 2 || row < 0 || row > 2 || mouseX < x + CONTENT_X || mouseY < y + GRID_Y) {
            return null;
        }
        return SimpleRune.values()[row * 3 + column];
    }

    void render(GuiGraphics graphics, State state, @Nullable SimpleRune guide, int mouseX, int mouseY) {
        if (state != State.OPEN) {
            Component text = Component.translatable(state == State.NO_DIARY
                    ? "gui.hextale.research_table.reference.no_diary" : "gui.hextale.research_table.reference.foreign");
            int ty = y + HEIGHT / 2 + 20;
            for (FormattedCharSequence line : font.split(text, WIDTH - 30)) {
                graphics.drawString(font, line, x + (WIDTH - font.width(line)) / 2, ty, 0xFF000000 | COVER_TEXT, true);
                ty += 10;
            }
            return;
        }
        Component title = Component.translatable("gui.hextale.research_table.reference.title").withStyle(ChatFormatting.BOLD);
        graphics.drawString(font, title, x + CONTENT_X + (CONTENT_WIDTH - font.width(title)) / 2, y + 11, 0xFF000000 | HEADING, false);

        SimpleRune hovered = runeAt(mouseX, mouseY);
        RuneKnowledge knowledge = ClientKnowledge.get();
        InkRenderer ink = InkRenderer.begin(graphics);
        float rule = y + 22.5F;
        float mid = x + CONTENT_X + CONTENT_WIDTH / 2.0F;
        ink.stroke(new float[]{mid - 30, rule, mid - 4, rule}, 0.8F, RuneDrawing.argb(HEADING, 0.55F));
        ink.disc(mid, rule, 1.3F, RuneDrawing.argb(GOLD, 0.9F), RuneDrawing.argb(GOLD, 0.9F), false);
        ink.stroke(new float[]{mid + 4, rule, mid + 30, rule}, 0.8F, RuneDrawing.argb(HEADING, 0.55F));
        SimpleRune[] runes = SimpleRune.values();
        for (int i = 0; i < runes.length; i++) {
            SimpleRune rune = runes[i];
            float cx = x + CONTENT_X + (i % 3) * CELL + CELL / 2.0F;
            float cy = y + GRID_Y + (i / 3) * CELL + CELL / 2.0F;
            if (rune == hovered || rune == guide) {
                ink.rect(cx - CELL / 2.0F + 1, cy - CELL / 2.0F + 1, cx + CELL / 2.0F - 1, cy + CELL / 2.0F - 1,
                        RuneDrawing.argb(rune == guide ? GOLD : rune.color(), rune == guide ? 0.18F : 0.14F));
            }
            if (rune == guide) {
                float h = CELL / 2.0F - 1;
                ink.stroke(new float[]{cx - h, cy - h, cx + h, cy - h, cx + h, cy + h, cx - h, cy + h, cx - h, cy - h}, 0.9F,
                        RuneDrawing.argb(GOLD, 0.9F));
            }
            boolean used = knowledge.usedElements().contains(rune);
            RuneDrawing.radical(ink, rune, cx, cy, rune == hovered ? 20 : 18, RuneDrawing.Style.INK, used || rune == hovered ? 1 : 0.8F);
        }
        ink.end();

        SimpleRune shown = hovered != null ? hovered : guide;
        if (shown == null) {
            paragraph(graphics, List.of(Component.translatable("gui.hextale.research_table.reference.prompt")), MUTED, DETAILS_Y);
        } else {
            details(graphics, shown, knowledge);
        }
    }

    private void details(GuiGraphics graphics, SimpleRune rune, RuneKnowledge knowledge) {
        int known = 0;
        for (ComplexRune complex : knowledge.runes()) {
            if (complex.action() == rune || complex.target() == rune) {
                known++;
            }
        }
        // Name on the left, stroke count on the right of the same line.
        Component name = Component.translatable(rune.translationKey()).withStyle(ChatFormatting.BOLD);
        graphics.drawString(font, name, x + CONTENT_X, y + DETAILS_Y, 0xFF000000 | RuneDrawing.darken(rune.color(), 0.5F), false);
        Component strokes = Component.translatable("diary.hextale.strokes", rune.strokeCount());
        graphics.drawString(font, strokes, x + CONTENT_X + CONTENT_WIDTH - font.width(strokes), y + DETAILS_Y, 0xFF000000 | MUTED, false);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(rune.translationKey() + ".hint").withStyle(ChatFormatting.ITALIC).withColor(MUTED));
        lines.add(label("diary.hextale.simple.as_action", rune.translationKey() + ".action"));
        lines.add(label("diary.hextale.simple.as_target", rune.translationKey() + ".target"));
        lines.add(Component.translatable("gui.hextale.research_table.reference.known", known, 16).withColor(GOLD));
        paragraph(graphics, lines, INK, DETAILS_Y + 12);
    }

    private static MutableComponent label(String label, String text) {
        return Component.translatable(label).withStyle(ChatFormatting.BOLD)
                .append(Component.literal(" ").append(Component.translatable(text)).withStyle(style -> style.withBold(false)));
    }

    /** Wrapped paragraphs in the details area; a smaller type when a translation runs long. */
    private void paragraph(GuiGraphics graphics, List<Component> paragraphs, int color, int top) {
        int available = DETAILS_BOTTOM - top;
        float scale = 1;
        List<List<FormattedCharSequence>> wrapped = wrap(paragraphs, CONTENT_WIDTH);
        if (height(wrapped) > available) {
            scale = 0.8F;
            wrapped = wrap(paragraphs, (int) (CONTENT_WIDTH / scale));
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x + CONTENT_X, y + top, 0);
        graphics.pose().scale(scale, scale, 1);
        int ty = 0;
        for (List<FormattedCharSequence> paragraph : wrapped) {
            for (FormattedCharSequence line : paragraph) {
                graphics.drawString(font, line, 0, ty, 0xFF000000 | color, false);
                ty += 9;
            }
            ty += 3;
        }
        graphics.pose().popPose();
    }

    private List<List<FormattedCharSequence>> wrap(List<Component> paragraphs, int width) {
        List<List<FormattedCharSequence>> out = new ArrayList<>();
        for (Component paragraph : paragraphs) {
            out.add(font.split(paragraph, width));
        }
        return out;
    }

    private static int height(List<List<FormattedCharSequence>> wrapped) {
        int lines = 0;
        for (List<FormattedCharSequence> paragraph : wrapped) {
            lines += paragraph.size();
        }
        return lines * 9 + wrapped.size() * 3;
    }
}
