package com.pockyl.hextale.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.PixelArt;

import java.util.ArrayList;
import java.util.List;

/**
 * The look shared by the mod's screens (all but the Research Table): a walnut panel in an aged brass frame with riveted
 * corners, an engraved brass title plate, wells sunk into the wood, brass-rimmed buttons, and text that never leaves its
 * box - one line is cut with an ellipsis, a paragraph wraps and is cut after its last allowed line. The materials are
 * GUI sprites drawn at two texels per GUI pixel ({@code textures/gui/sprites/plate}).
 */
public final class HexPlate {
    public static final int BRASS = 0xC29C50;
    public static final int BRASS_LIGHT = 0xF2DE9C;
    public static final int BRASS_DARK = 0x4A3312;
    /** The walnut's own colour, for anything that must blend into the panel. */
    public static final int PANEL = 0x432B1A;
    /** The floor of a well, for anything drawn inside one. */
    public static final int WELL_FLOOR = 0x2C1C10;
    /** Letters engraved into the brass title plate. */
    public static final int TITLE = 0xFF3A2410;
    public static final int TEXT = 0xFFF2E6CF;
    public static final int MUTED = 0xFFBCA88A;
    public static final int GOLD = 0xFFE9C66D;
    /** The brass frame's width: content starts this far in. */
    public static final int BORDER = 4;
    /** The riveted brass plates on the frame's corners. */
    public static final int CORNER = 6;
    public static final Identifier BUTTON = sprite("button");
    public static final Identifier BUTTON_HOVER = sprite("button_hover");
    public static final Identifier BUTTON_PRESSED = sprite("button_pressed");
    public static final Identifier BUTTON_DISABLED = sprite("button_disabled");
    public static final Identifier BUTTON_READY = sprite("button_ready");
    private static final Identifier WALNUT = sprite("walnut");
    private static final Identifier FRAME = sprite("frame");
    private static final Identifier CORNER_PLATE = sprite("corner");
    private static final Identifier WELL = sprite("well");
    private static final Identifier OUTPUT_RING = sprite("output_ring");
    private static final Identifier TITLE_PLATE = sprite("title");
    private static final int ENGRAVE_LIGHT = 0xFFE8CC84;
    private static final String ELLIPSIS = "…";

    private HexPlate() {
    }

    private static Identifier sprite(String name) {
        return Hextale.id("plate/" + name);
    }

    private static void blit(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int w, int h) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
    }

    /** The plate: walnut boards in an aged brass frame with a riveted plate on every corner. */
    public static void plate(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        PixelArt.flush(graphics);
        blit(graphics, WALNUT, x + 2, y + 2, w - 4, h - 4);
        blit(graphics, FRAME, x, y, w, h);
        blit(graphics, CORNER_PLATE, x, y, CORNER, CORNER);
        blit(graphics, CORNER_PLATE, x + w - CORNER, y, CORNER, CORNER);
        blit(graphics, CORNER_PLATE, x, y + h - CORNER, CORNER, CORNER);
        blit(graphics, CORNER_PLATE, x + w - CORNER, y + h - CORNER, CORNER, CORNER);
    }

    /** A brass strip inlaid across the plate at {@code y} (two pixels: the groove, then the brass), between its frame sides. */
    public static void divider(GuiGraphicsExtractor graphics, int x, int y, int w) {
        PixelArt.fill(graphics, x + BORDER, y, x + w - BORDER, y + 1, PixelArt.argb(0x1E1209, 0.8F));
        PixelArt.fill(graphics, x + BORDER, y + 1, x + w - BORDER, y + 2, PixelArt.argb(0xA88442, 1));
    }

    /**
     * The engraved brass plate a title sits on, {@code width} pixels of text wide with its first letter at {@code x, y}
     * (where the text itself is drawn by {@link #engraved}).
     */
    public static void titlePlate(GuiGraphicsExtractor graphics, int x, int y, int width) {
        PixelArt.flush(graphics);
        blit(graphics, TITLE_PLATE, x - 2, y - 2, width + 4, 12);
    }

    /** {@code text} cut to {@code width}, engraved: dark letters with the brass catching light under them. */
    public static void engraved(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int x, int y) {
        graphics.text(font, text, x, y + 1, ENGRAVE_LIGHT, false);
        graphics.text(font, text, x, y, TITLE, false);
    }

    /** A screen's title on its brass plate, cut to {@code width}; the plate is as wide as the text. */
    public static void title(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width) {
        FormattedCharSequence line = fit(font, text, width);
        titlePlate(graphics, x, y, font.width(line));
        engraved(graphics, font, line, x, y);
    }

    /** {@link #title} centred on {@code cx}. */
    public static void centeredTitle(GuiGraphicsExtractor graphics, Font font, Component text, int cx, int y, int width) {
        FormattedCharSequence line = fit(font, text, width);
        int x = cx - font.width(line) / 2;
        titlePlate(graphics, x, y, font.width(line));
        engraved(graphics, font, line, x, y);
    }

    /** Wrapped title lines centred on {@code cx}, one line every 10 pixels from {@code y}, on one engraved plate. */
    public static void centeredTitle(GuiGraphicsExtractor graphics, Font font, List<FormattedCharSequence> lines, int cx, int y) {
        int width = 0;
        for (FormattedCharSequence line : lines) {
            width = Math.max(width, font.width(line));
        }
        PixelArt.flush(graphics);
        blit(graphics, TITLE_PLATE, cx - width / 2 - 2, y - 2, width + 4, lines.size() * 10 + 2);
        for (FormattedCharSequence line : lines) {
            engraved(graphics, font, line, cx - font.width(line) / 2, y);
            y += 10;
        }
    }

    /** A well sunk into the wood: its walls in shadow at the top left, its lip lit at the bottom right. */
    public static void well(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        PixelArt.flush(graphics);
        blit(graphics, WELL, x, y, w, h);
    }

    /** The well of a slot whose item sits at {@code x, y} (the slot's own coordinates). */
    public static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        well(graphics, x - 1, y - 1, 18, 18);
    }

    /** A riveted brass ring two pixels round the slot at {@code x, y}: the slot where the product comes out. */
    public static void outputSlot(GuiGraphicsExtractor graphics, int x, int y) {
        outputRing(graphics, x, y);
        slot(graphics, x, y);
    }

    /** Only the brass ring of {@link #outputSlot}; wells drawn after it cover the rings of neighbouring product slots. */
    public static void outputRing(GuiGraphicsExtractor graphics, int x, int y) {
        PixelArt.flush(graphics);
        blit(graphics, OUTPUT_RING, x - 3, y - 3, 22, 22);
    }

    /** A thin rim round a small square (a cell in a grid of choices): gold when chosen, brass when hovered. */
    public static void cell(GuiGraphicsExtractor graphics, int x, int y, int size, boolean chosen, boolean hovered) {
        if (!chosen && !hovered) {
            return;
        }
        PixelArt.fill(graphics, x, y, x + size, y + size, chosen ? GOLD : PixelArt.argb(BRASS, 0.7F));
        PixelArt.fill(graphics, x + 1, y + 1, x + size - 1, y + size - 1, PixelArt.argb(chosen ? 0x3A2616 : WELL_FLOOR, 1));
    }

    /** The sprite of a brass-rimmed button in its state. */
    public static Identifier buttonSprite(boolean hovered, boolean active) {
        return !active ? BUTTON_DISABLED : hovered ? BUTTON_HOVER : BUTTON;
    }

    /** A brass-rimmed button with its label centred and cut to fit; a disabled one is dim. */
    public static void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, Component text, boolean hovered,
                              boolean active) {
        PixelArt.flush(graphics);
        blit(graphics, buttonSprite(hovered, active), x, y, w, h);
        int colour = !active ? 0xFF8A7A66 : hovered ? 0xFFFFF4D8 : TEXT;
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
