package com.pockyl.hextale.client.screen.book;

import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import com.pockyl.hextale.port.client.KeyEvent;
import com.pockyl.hextale.port.client.MouseButtonEvent;
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
    public void render(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.render(graphics.vanilla(), mouseX, mouseY, partialTick);
        List<FormattedCharSequence> tooltip = book.render(graphics, mouseX, mouseY);
        if (tooltip != null) {
            graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double portX, double portY, int portButton) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        boolean doubleClick = false;
        return book.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(double portX, double portY, int portButton, double dragX, double dragY) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        return book.mouseDragged(event.x(), event.y(), event.button()) || super.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double portX, double portY, int portButton) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        return book.mouseReleased(event.x(), event.y(), event.button()) || super.mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean keyPressed(int portKey, int portScan, int portMods) {
        KeyEvent event = new KeyEvent(portKey, portScan, portMods);
        return book.keyPressed(event) || super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return book.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
