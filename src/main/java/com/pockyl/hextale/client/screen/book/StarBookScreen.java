package com.pockyl.hextale.client.screen.book;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        List<FormattedCharSequence> tooltip = book.render(graphics, mouseX, mouseY);
        if (tooltip != null) {
            graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return book.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return book.mouseDragged(event.x(), event.y(), event.button()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return book.mouseReleased(event.x(), event.y(), event.button()) || super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return book.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return book.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
