package com.pockyl.hextale.client;

import net.minecraft.client.gui.Font;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.research.EssencesTooltip;
import com.pockyl.hextale.rune.Essence;

import java.util.Map;

/** Draws a studied item's elements in its tooltip: each element's sign in its colour, then its strength. */
public final class ClientEssencesTooltip implements ClientTooltipComponent {
    private static final int SIGN = 9;
    private static final int GAP = 6;

    private final EssencesTooltip data;

    public ClientEssencesTooltip(EssencesTooltip data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return SIGN + 3;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (Map.Entry<Essence, Integer> entry : data.essences().essences().entrySet()) {
            width += SIGN + 2 + font.width(String.valueOf(entry.getValue())) + GAP;
        }
        return Math.max(0, width - GAP);
    }

    @Override
    public void renderImage(Font font, int x, int y, net.minecraft.client.gui.GuiGraphics portGraphics) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        int width = getWidth(font);
        int height = getHeight();
        int cx = x;
        for (Map.Entry<Essence, Integer> entry : data.essences().essences().entrySet()) {
            Essence element = entry.getKey();
            PixelArt.essence(graphics, element, cx, y, SIGN, PixelArt.argb(element.color(), 1));
            String strength = String.valueOf(entry.getValue());
            PixelArt.flush(graphics);
            graphics.text(font, strength, cx + SIGN + 2, y + 1, 0xFF000000 | element.color(), true);
            cx += SIGN + 2 + font.width(strength) + GAP;
        }
        PixelArt.flush(graphics);
    }
}
