package com.pockyl.hextale.client.screen.diary;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.UUID;

/** The Researcher's Diary read in hand. */
public class DiaryScreen extends Screen {
    private final DiaryBook book;

    public DiaryScreen(String owner, UUID player) {
        super(Component.translatable("item.hextale.researchers_diary.owned", owner));
        this.book = new DiaryBook(owner, player.getMostSignificantBits() ^ player.getLeastSignificantBits(), null);
    }

    @Override
    protected void init() {
        book.layout((width - DiaryBook.FULL_WIDTH) / 2, (height - DiaryBook.BOOK_HEIGHT) / 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        List<FormattedCharSequence> tooltip = book.render(graphics, mouseX, mouseY);
        if (tooltip != null) {
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return book.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return book.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }
}
