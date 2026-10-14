package com.pockyl.hextale.client.screen.diary;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.knowledge.RuneKnowledge;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/** What a section needs to draw one spread: where the pages are, the reader's knowledge and a few drawing helpers. */
final class DiaryContext {
    static final int INK = 0x3B2A1A;
    static final int HEADING = 0x6B3A12;
    static final int MUTED = 0x7A6648;
    static final int GOLD = 0x9A6B12;
    static final int RED = 0x8E2A1E;
    static final int PAGE_WIDTH = 138;
    static final int PAGE_TOP = 16;
    static final int PAGE_BOTTOM = 181;
    static final int LEFT_PAGE_X = 18;
    static final int RIGHT_PAGE_X = 174;

    final GuiGraphics graphics;
    final Font font;
    final int bookLeft;
    final int bookTop;
    final RuneKnowledge knowledge;
    final String owner;
    final long seed;
    private final DiaryBook screen;
    private final int mouseX;
    private final int mouseY;

    DiaryContext(DiaryBook screen, GuiGraphics graphics, Font font, int bookLeft, int bookTop, RuneKnowledge knowledge,
                 String owner, long seed, int mouseX, int mouseY) {
        this.screen = screen;
        this.graphics = graphics;
        this.font = font;
        this.bookLeft = bookLeft;
        this.bookTop = bookTop;
        this.knowledge = knowledge;
        this.owner = owner;
        this.seed = seed;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    Page left() {
        return new Page(this, bookLeft + LEFT_PAGE_X, bookTop + PAGE_TOP, PAGE_WIDTH, bookTop + PAGE_BOTTOM);
    }

    Page right() {
        return new Page(this, bookLeft + RIGHT_PAGE_X, bookTop + PAGE_TOP, PAGE_WIDTH, bookTop + PAGE_BOTTOM);
    }

    boolean hovered(double x, double y, double width, double height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    void tooltip(Component text) {
        screen.setPendingTooltip(font.split(text, 200));
    }

    void tooltip(List<FormattedCharSequence> lines) {
        screen.setPendingTooltip(lines);
    }

    /** An item icon; hovering it shows the item's name. */
    void item(ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
        if (hovered(x, y, 16, 16) && !stack.isEmpty()) {
            tooltip(stack.getHoverName());
        }
    }

    /** A recipe slot: a sunken square with the item in it. */
    void slot(@Nullable ItemStack stack, int x, int y) {
        graphics.blit(DiaryBook.TEXTURE, x - 1, y - 1, DiaryBook.SLOT_U, DiaryBook.SLOT_V, 18, 18,
                DiaryBook.SHEET_WIDTH, DiaryBook.SHEET_HEIGHT);
        if (stack != null) {
            item(stack, x, y);
        }
    }

    @Nullable
    DiaryBook.Host host() {
        return screen.host();
    }

    void turnTo(int spread) {
        screen.turnTo(spread);
    }

    void notesSeen() {
        screen.notesSeen();
    }
}
