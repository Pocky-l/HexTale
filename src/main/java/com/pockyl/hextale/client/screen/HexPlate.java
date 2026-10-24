package com.pockyl.hextale.client.screen;

import net.minecraft.client.gui.Font;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import com.pockyl.hextale.client.render.PixelArt;

import java.util.ArrayList;
import java.util.List;

/**
 * The look shared by the mod's screens (all but the Research Table): a dark panel in a brass frame lit at the top left,
 * slot wells shaded like holes, brass-rimmed buttons, and text that never leaves its box - one line is cut with an
 * ellipsis, a paragraph wraps and is cut after its last allowed line.
 */
public final class HexPlate {
    public static final int BRASS = 0xC29445;
    public static final int BRASS_LIGHT = 0xF8E3A0;
    public static final int BRASS_DARK = 0x4A3312;
    public static final int PANEL = 0x16121F;
    public static final int TITLE = 0xFF000000 | BRASS_LIGHT;
    public static final int TEXT = 0xFFE8E0F0;
    public static final int MUTED = 0xFF9A90B8;
    public static final int GOLD = 0xFFE9C66D;
    /** The brass frame's width: content starts this far in. */
    public static final int BORDER = 4;
    private static final String ELLIPSIS = "…";

    private HexPlate() {
    }

    /** The plate: a dark panel in a brass frame lit at the top left. */
    public static void plate(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        PixelArt.fill(graphics, x, y, x + w, y + h, PixelArt.argb(BRASS_DARK, 1));
        PixelArt.fill(graphics, x + 1, y + 1, x + w - 1, y + h - 1, PixelArt.argb(BRASS, 1));
        PixelArt.fill(graphics, x + 1, y + 1, x + w - 1, y + 2, PixelArt.argb(BRASS_LIGHT, 1));
        PixelArt.fill(graphics, x + 1, y + 1, x + 2, y + h - 1, PixelArt.argb(BRASS_LIGHT, 1));
        PixelArt.fill(graphics, x + 3, y + 3, x + w - 3, y + h - 3, PixelArt.argb(BRASS_DARK, 1));
        PixelArt.fill(graphics, x + 4, y + 4, x + w - 4, y + h - 4, PixelArt.argb(PANEL, 1));
    }

    /** A faint brass line across the plate at {@code y}, between its frame sides. */
    public static void divider(GuiGraphicsExtractor graphics, int x, int y, int w) {
        PixelArt.fill(graphics, x + BORDER, y, x + w - BORDER, y + 1, PixelArt.argb(BRASS, 0.5F));
    }

    /** A well: shaded at the top left, lit at the bottom right. Slots, tanks and recesses use it. */
    public static void well(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        PixelArt.fill(graphics, x, y, x + w, y + h, PixelArt.argb(0x0A0810, 1));
        PixelArt.fill(graphics, x + w - 1, y + 1, x + w, y + h, PixelArt.argb(0x4A4060, 1));
        PixelArt.fill(graphics, x + 1, y + h - 1, x + w, y + h, PixelArt.argb(0x4A4060, 1));
        PixelArt.fill(graphics, x + 1, y + 1, x + w - 1, y + h - 1, PixelArt.argb(0x1E1A2A, 1));
    }

    /** The well of a slot whose item sits at {@code x, y} (the slot's own coordinates). */
    public static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        well(graphics, x - 1, y - 1, 18, 18);
    }

    /** A brass ring two pixels round the slot at {@code x, y}: the slot where the product comes out. */
    public static void outputSlot(GuiGraphicsExtractor graphics, int x, int y) {
        PixelArt.fill(graphics, x - 3, y - 3, x + 19, y + 19, PixelArt.argb(BRASS_DARK, 1));
        PixelArt.fill(graphics, x - 2, y - 2, x + 18, y + 18, PixelArt.argb(BRASS, 1));
        PixelArt.fill(graphics, x - 2, y - 2, x + 18, y - 1, PixelArt.argb(BRASS_LIGHT, 1));
        PixelArt.fill(graphics, x - 2, y - 2, x - 1, y + 18, PixelArt.argb(BRASS_LIGHT, 1));
        slot(graphics, x, y);
    }

    /** A thin rim round a small square (a cell in a grid of choices): gold when chosen, brass when hovered. */
    public static void cell(GuiGraphicsExtractor graphics, int x, int y, int size, boolean chosen, boolean hovered) {
        if (!chosen && !hovered) {
            return;
        }
        PixelArt.fill(graphics, x, y, x + size, y + size, chosen ? GOLD : PixelArt.argb(BRASS, 0.7F));
        PixelArt.fill(graphics, x + 1, y + 1, x + size - 1, y + size - 1, PixelArt.argb(chosen ? 0x2E2440 : 0x221B30, 1));
    }

    /** A brass-rimmed button with its label centred and cut to fit; a disabled one is dim. */
    public static void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, Component text, boolean hovered,
                              boolean active) {
        int rim = !active ? 0x5A4A3A : hovered ? BRASS_LIGHT : BRASS;
        PixelArt.fill(graphics, x + 1, y, x + w - 1, y + h, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x, y + 1, x + w, y + h - 1, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x + 1, y + 1, x + w - 1, y + h - 1, PixelArt.argb(hovered && active ? 0x2E2440 : 0x221B30, 1));
        PixelArt.flush(graphics);
        int colour = !active ? 0xFF7A7090 : hovered ? 0xFFFFF6D6 : TEXT;
        graphics.centeredText(font, fit(font, text, w - 6), x + w / 2, y + (h - 8) / 2, colour);
    }

    /** {@code text} as one line no wider than {@code width}, cut with an ellipsis when it is longer. */
    public static FormattedCharSequence fit(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        FormattedText cut = font.substrByWidth(text, Math.max(0, width - font.width(ELLIPSIS)));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of(ELLIPSIS, text.getStyle())));
    }

    /** Whether {@link #fit} would cut {@code text} (a tooltip can then show it whole). */
    public static boolean cut(Font font, Component text, int width) {
        return font.width(text) > width;
    }

    /** One line at {@code x, y}, cut to {@code width}. */
    public static void line(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int colour, boolean shadow) {
        graphics.text(font, fit(font, text, width), x, y, colour, shadow);
    }

    /** One line centred on {@code cx}, cut to {@code width}. */
    public static void centered(GuiGraphicsExtractor graphics, Font font, Component text, int cx, int y, int width, int colour) {
        graphics.centeredText(font, fit(font, text, width), cx, y, colour);
    }

    /** {@code text} wrapped to {@code width}, at most {@code maxLines} lines, the last one cut with an ellipsis. */
    public static List<FormattedCharSequence> wrap(Font font, Component text, int width, int maxLines) {
        List<FormattedText> parts = font.getSplitter().splitLines(text, width, text.getStyle());
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (int i = 0; i < parts.size() && i < maxLines; i++) {
            FormattedText part = parts.get(i);
            if (i == maxLines - 1 && parts.size() > maxLines) {
                FormattedText cut = font.substrByWidth(part, Math.max(0, width - font.width(ELLIPSIS)));
                part = FormattedText.composite(cut, FormattedText.of(ELLIPSIS, text.getStyle()));
            }
            lines.add(Language.getInstance().getVisualOrder(part));
        }
        return lines;
    }

    /** Draws {@link #wrap} from {@code x, y} down, one line every 10 pixels; returns the y under the last line. */
    public static int paragraph(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int maxLines, int colour) {
        for (FormattedCharSequence line : wrap(font, text, width, maxLines)) {
            graphics.text(font, line, x, y, colour, false);
            y += 10;
        }
        return y;
    }

    /** Draws {@link #wrap} centred on {@code cx}; returns the y under the last line. */
    public static int centeredParagraph(GuiGraphicsExtractor graphics, Font font, Component text, int cx, int y, int width, int maxLines,
                                        int colour) {
        for (FormattedCharSequence line : wrap(font, text, width, maxLines)) {
            graphics.centeredText(font, line, cx, y, colour);
            y += 10;
        }
        return y;
    }
}
