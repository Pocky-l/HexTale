package com.pockyl.hextale.client.screen.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.runework.PickaxeRune;
import com.pockyl.hextale.runework.PylonRune;
import com.pockyl.hextale.runework.SmelterRune;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The research book's rune page: every rune the researcher has found, in the order they were found, as a plain list -
 * no paths, no dependencies. Each entry shows the rune's sign, name, stars and chapter, what it does and what it is used
 * for now (a pickaxe, the smelter, the pylons, an altar ritual). Runes come from scrolls found in treasure chests; a
 * page with none tells where to look.
 */
final class RunesPage {
    private static final int SIGN = 22;
    private static final int PAD = 8;
    private static final int GAP = 6;
    private static final int TEXT = 0xFFD8D0E8;
    private static final int MUTED = 0xFF9A90B8;
    private static final int USE = 0xFFC8C0DC;

    private final StarBook book;
    private final Font font;
    private int scroll;
    /** Height of everything on the page at the last draw, for the scroll limit. */
    private int contentHeight;

    RunesPage(StarBook book) {
        this.book = book;
        this.font = Minecraft.getInstance().font;
    }

    /** The lines of one entry under its name: what the rune does, then what it is used for now. */
    private List<FormattedCharSequence> lines(ComplexRune rune, int width) {
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(Component.translatable(rune.translationKey() + ".effect"), width));
        for (Component use : uses(rune)) {
            lines.addAll(font.split(Component.literal("• ").append(use).withColor(USE), width));
        }
        return lines;
    }

    /** What a rune is used for now: the holders that take it and the altar rituals that ask for it. */
    static List<Component> uses(ComplexRune rune) {
        List<Component> uses = new ArrayList<>();
        PickaxeRune pickaxe = PickaxeRune.of(rune);
        if (pickaxe != null) {
            uses.add(Component.translatable("book.hextale.guide.use.pickaxe", Component.translatable(pickaxe.descriptionKey())));
        }
        SmelterRune smelter = SmelterRune.of(rune);
        if (smelter != null) {
            uses.add(Component.translatable("book.hextale.guide.use.smelter", Component.translatable(smelter.descriptionKey())));
        }
        PylonRune pylon = PylonRune.of(rune);
        if (pylon != null) {
            uses.add(Component.translatable("book.hextale.guide.use.pylon", Component.translatable(pylon.descriptionKey())));
        }
        for (var recipe : InfusionRecipes.get(true).values()) {
            if (recipe.runes().contains(rune)) {
                ItemStack result = recipe.result().create();
                uses.add(Component.translatable("book.hextale.guide.use.recipe", result.getHoverName()));
            }
        }
        return uses;
    }

    /** Draws the page inside the sky; returns the tooltip under the mouse, or null. */
    @Nullable
    List<FormattedCharSequence> render(GuiGraphicsExtractor graphics, RuneKnowledge knowledge, int mouseX, int mouseY, long now) {
        int left = book.skyLeft() + PAD;
        int top = book.skyTop() + StarBook.TAB_ROW + PAD;
        int width = book.skyWidth() - PAD * 2;
        int bottom = book.skyTop() + book.skyHeight() - PAD;
        List<ComplexRune> runes = knowledge.runes();
        Component header = Component.translatable("book.hextale.runes.title", runes.size());
        PixelArt.flush(graphics);
        graphics.text(font, header, left, top, 0xFF000000 | StarBook.GOLD, true);
        PixelArt.fill(graphics, left, top + 11, left + width, top + 12, PixelArt.argb(StarBook.GOLD, 0.3F));
        int listTop = top + 16;
        if (runes.isEmpty()) {
            PixelArt.flush(graphics);
            int y = listTop + 6;
            for (FormattedCharSequence line : font.split(Component.translatable("book.hextale.runes.none"), width)) {
                graphics.text(font, line, left, y, MUTED, false);
                y += 10;
            }
            contentHeight = 0;
            return null;
        }
        int textWidth = width - SIGN - GAP - 4;
        int viewHeight = bottom - listTop;
        int total = 0;
        for (ComplexRune rune : runes) {
            total += entryHeight(rune, textWidth) + GAP;
        }
        contentHeight = total;
        scroll = Mth.clamp(scroll, 0, Math.max(0, total - viewHeight));
        PixelArt.flush(graphics);
        graphics.enableScissor(left - 2, listTop, left + width + 2, bottom);
        List<FormattedCharSequence> tooltip = null;
        int y = listTop - scroll;
        for (ComplexRune rune : runes) {
            int height = entryHeight(rune, textWidth);
            if (y + height >= listTop && y < bottom) {
                boolean hovered = mouseX >= left && mouseX < left + width && mouseY >= Math.max(y, listTop) && mouseY < Math.min(y + height, bottom);
                entry(graphics, rune, knowledge.stars(rune), left, y, width, textWidth, height, hovered, now);
                if (hovered && mouseX < left + SIGN + 2) {
                    tooltip = List.of(Component.translatable("rune.hextale.formula", Component.translatable(rune.action().translationKey()),
                            Component.translatable(rune.target().translationKey())).withColor(0x9A90B8).getVisualOrderText());
                }
            }
            y += height + GAP;
        }
        PixelArt.flush(graphics);
        graphics.disableScissor();
        if (total > viewHeight) {
            int bar = Math.max(12, viewHeight * viewHeight / total);
            int by = listTop + (viewHeight - bar) * scroll / Math.max(1, total - viewHeight);
            PixelArt.fill(graphics, left + width + 1, by, left + width + 3, by + bar, PixelArt.argb(StarBook.GOLD, 0.6F));
        }
        return tooltip;
    }

    private int entryHeight(ComplexRune rune, int textWidth) {
        return Math.max(SIGN + 6, 14 + lines(rune, textWidth).size() * 10 + 4);
    }

    /** One rune: its sign in a hexagon, name with stars and chapter, then what it does and is used for. */
    private void entry(GuiGraphicsExtractor graphics, ComplexRune rune, int stars, int x, int y, int width, int textWidth, int height,
                       boolean hovered, long now) {
        int color = rune.color();
        PixelArt.fill(graphics, x - 2, y - 2, x + width + 2, y + height, PixelArt.argb(hovered ? 0x1A1630 : 0x0E0C1E, 0.8F));
        PixelArt.fill(graphics, x - 2, y - 2, x - 1, y + height, PixelArt.argb(color, 0.6F));
        float pulse = 0.5F + 0.5F * Mth.sin(now / 600.0F + rune.index());
        PixelArt.hex(graphics, x + SIGN / 2 + 2, y + SIGN / 2 + 2, SIGN, PixelArt.argb(PixelArt.mix(color, 0x06050D, 0.75F), 1),
                PixelArt.argb(PixelArt.mix(color, 0xFFFFFF, 0.2F * pulse), 1));
        PixelArt.glyph(graphics, rune, x + 6, y + 6, SIGN - 8, PixelArt.argb(PixelArt.mix(color, 0xFFFFFF, 0.15F), 1), 0xFF000000);
        PixelArt.flush(graphics);
        int tx = x + SIGN + GAP + 2;
        Component name = Component.translatable(rune.translationKey()).withColor(color).append(" ").append(StarBook.stars(stars));
        graphics.text(font, name, tx, y + 1, 0xFFFFFFFF, true);
        Component chapter = Component.translatable("book.hextale.quest.pickaxe_rune.chapter", "I".repeat(ResearchBook.chapter(rune)));
        graphics.text(font, chapter, x + width - font.width(chapter), y + 1, MUTED, false);
        int ly = y + 14;
        for (FormattedCharSequence line : lines(rune, textWidth)) {
            graphics.text(font, line, tx, ly, TEXT, false);
            ly += 10;
        }
    }

    void scroll(double amount) {
        scroll = Math.max(0, Math.min(Math.max(0, contentHeight), scroll - (int) Math.round(amount * 14)));
    }
}
