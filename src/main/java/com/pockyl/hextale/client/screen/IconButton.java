package com.pockyl.hextale.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.pockyl.hextale.client.render.InkRenderer;

import java.util.function.BiConsumer;

/** A small brass button from a GUI sheet with a vector icon on it; the label is its tooltip. */
public class IconButton extends Button {
    private final ResourceLocation texture;
    private final int u;
    private final int v;
    private final int sheetWidth;
    private final int sheetHeight;
    private final BiConsumer<InkRenderer, IconButton> icon;

    /**
     * @param u, v  the normal sprite; the hovered one is right of it, the pressed-down one right of that
     * @param icon  draws the icon, centred on the button
     */
    public IconButton(int x, int y, int width, int height, Component label, ResourceLocation texture, int u, int v, int sheetWidth,
                      int sheetHeight, BiConsumer<InkRenderer, IconButton> icon, OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.sheetWidth = sheetWidth;
        this.sheetHeight = sheetHeight;
        this.icon = icon;
        setTooltip(Tooltip.create(label));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int sprite = !active ? 2 : isHoveredOrFocused() ? 1 : 0;
        graphics.blit(texture, getX(), getY(), u + sprite * width, v, width, height, sheetWidth, sheetHeight);
        InkRenderer ink = InkRenderer.begin(graphics);
        icon.accept(ink, this);
        ink.end();
    }

    public float centerX() {
        return getX() + width / 2.0F;
    }

    public float centerY() {
        return getY() + height / 2.0F - (isHoveredOrFocused() ? 0 : 0.5F);
    }
}
