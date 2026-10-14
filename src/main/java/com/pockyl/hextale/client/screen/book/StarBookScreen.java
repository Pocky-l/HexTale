package com.pockyl.hextale.client.screen.book;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** The Researcher's Diary read in hand: the star book over most of the screen. */
public class StarBookScreen extends Screen {
    private static final int MAX_WIDTH = 520;
    private static final int MAX_HEIGHT = 320;
    private static final int MARGIN = 12;

    private final StarBook book = new StarBook(null);

    public StarBookScreen(String owner) {
        super(Component.translatable("item.hextale.researchers_diary.owned", owner));
    }

    @Override
    protected void init() {
        int w = Math.min(MAX_WIDTH, width - MARGIN * 2);
        int h = Math.min(MAX_HEIGHT, height - MARGIN * 2);
        book.layout((width - w) / 2, (height - h) / 2, w, h);
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return book.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return book.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return book.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
