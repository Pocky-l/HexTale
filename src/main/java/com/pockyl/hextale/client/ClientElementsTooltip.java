package com.pockyl.hextale.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.research.ElementsTooltip;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.Map;

/** Draws a studied item's elements in its tooltip: each element's sign in its colour, then its strength. */
public final class ClientElementsTooltip implements ClientTooltipComponent {
    private static final int SIGN = 9;
    private static final int GAP = 6;

    private final ElementsTooltip data;

    public ClientElementsTooltip(ElementsTooltip data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return SIGN + 3;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (Map.Entry<SimpleRune, Integer> entry : data.elements().elements().entrySet()) {
            width += SIGN + 2 + font.width(String.valueOf(entry.getValue())) + GAP;
        }
        return Math.max(0, width - GAP);
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int cx = x;
        for (Map.Entry<SimpleRune, Integer> entry : data.elements().elements().entrySet()) {
            SimpleRune element = entry.getKey();
            PixelArt.radical(graphics, element, cx, y, SIGN, PixelArt.argb(element.color(), 1), 0xFF000000);
            String strength = String.valueOf(entry.getValue());
            graphics.drawString(font, strength, cx + SIGN + 2, y + 1, 0xFF000000 | element.color(), true);
            cx += SIGN + 2 + font.width(strength) + GAP;
        }
    }
}
