package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.item.EssenceItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The Crucible's recipes in JEI drawn like the diary draws them: the cast-iron pot over its fire, the essences its water
 * must hold as rows in the water (the essence as an ingredient, its name and mB beside it), the catalyst over the pot
 * falling in, an arrow to the result, and under the pot the heat it boils over. Shared by transmutations and brews; the
 * pot is as deep as the recipe with the most essences, so every page of a category lines up.
 */
final class CruciblePot {
    static final int ROW = 18;
    /** Rows the pot always has room for; more only when a recipe needs them. */
    static final int MIN_ROWS = 2;
    static final int POT_X = 2;
    static final int POT_WIDTH = 100;
    private static final int WALL = 3;
    /** The catalyst's slot, then its fall into the water. */
    static final int POT_TOP = 30;
    static final int ARROW_X = POT_X + POT_WIDTH + 4;
    private static final int IRON_DARK = 0x1F1C24;
    private static final int IRON = 0x3E3946;
    private static final int IRON_LIGHT = 0x67606F;
    private static final int FALL = 0x8A6420;

    private CruciblePot() {
    }

    static int rows(List<? extends Map<Essence, Integer>> recipes) {
        int rows = MIN_ROWS;
        for (Map<Essence, Integer> essences : recipes) {
            rows = Math.max(rows, essences.size());
        }
        return rows;
    }

    static List<Map.Entry<Essence, Integer>> entries(Map<Essence, Integer> essences) {
        return new ArrayList<>(essences.entrySet());
    }

    private static int waterTop() {
        return POT_TOP + WALL;
    }

    private static int waterBottom(int rows) {
        return waterTop() + rows * ROW + 2;
    }

    private static int potBottom(int rows) {
        return waterBottom(rows) + WALL;
    }

    /** The y of the heat line under the fire. */
    static int heatY(int rows) {
        return potBottom(rows) + 11;
    }

    static int height(int rows) {
        return heatY(rows) + 11;
    }

    /** The middle of the water, where the arrow to the result points from. */
    static int middle(int rows) {
        return waterTop() + (rows * ROW + 2) / 2;
    }

    static int catalystX() {
        return POT_X + POT_WIDTH / 2 - 8;
    }

    static final int CATALYST_Y = 2;

    /** The essences as ingredient slots, one row each from the top of the water. */
    static void addEssences(IRecipeLayoutBuilder builder, List<Map.Entry<Essence, Integer>> essences) {
        for (int i = 0; i < essences.size(); i++) {
            Map.Entry<Essence, Integer> entry = essences.get(i);
            builder.addInputSlot(POT_X + WALL + 3, rowY(i)).add(EssenceItem.of(entry.getKey()))
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.crucible.essence", entry.getValue())));
        }
    }

    private static int rowY(int row) {
        return waterTop() + 2 + row * ROW;
    }

    /** The pot, its water, fire and the catalyst's fall, with the essences' names and mB on their rows. */
    static void draw(GuiGraphicsExtractor graphics, List<Map.Entry<Essence, Integer>> essences, int rows, int heat) {
        long now = Util.getMillis();
        int x0 = POT_X + WALL;
        int x1 = POT_X + POT_WIDTH - WALL;
        water(graphics, essences, x0, waterTop(), x1, waterBottom(rows), now);
        for (int i = 0; i < essences.size(); i++) {
            PixelArt.fill(graphics, x0 + 2, rowY(i) - 1, x1 - 2, rowY(i) + 17, PixelArt.argb(0x08070E, 0.45F));
        }
        fire(graphics, potBottom(rows), heat, now);
        ironPot(graphics, POT_TOP, potBottom(rows));
        fall(graphics, POT_X + POT_WIDTH / 2, CATALYST_Y + 18, waterTop() - 1, now);
        PixelArt.flush(graphics);
        Font font = Minecraft.getInstance().font;
        int textX = POT_X + WALL + 23;
        int room = x1 - 2 - textX;
        for (int i = 0; i < essences.size(); i++) {
            Essence essence = essences.get(i).getKey();
            int amount = essences.get(i).getValue();
            Component full = Component.translatable("book.hextale.quest.crucible.amount", Component.translatable(essence.translationKey()), amount);
            Component text = font.width(full) <= room ? full : Component.translatable("book.hextale.quest.crucible.amount_short", amount);
            graphics.text(font, text, textX, rowY(i) + 4, 0xFF000000 | essence.color(), true);
        }
    }

    /** The heat line: the plainest thing that heats the pot enough and the words (none: the icon alone), centred under the pot. */
    static void drawHeat(GuiGraphicsExtractor graphics, int rows, int heat, @Nullable Component words) {
        Font font = Minecraft.getInstance().font;
        int width = words == null ? 16 : 18 + font.width(words);
        int x = POT_X + POT_WIDTH / 2 - width / 2;
        int y = heatY(rows);
        graphics.item(heater(heat), x, y - 4);
        if (words != null) {
            graphics.text(font, words, x + 18, y, heatColor(heat), false);
        }
    }

    /** Darker than the diary's: the JEI page is light grey. */
    private static int heatColor(int heat) {
        return switch (heat) {
            case 3 -> 0xFF1E7F8C;
            case 4 -> 0xFFB0400E;
            case 5 -> 0xFF3F7A1E;
            default -> 0xFF8A4A1C;
        };
    }

    private static int flameColor(int heat) {
        return switch (heat) {
            case 3 -> 0x5CE0F0;
            case 4 -> 0xFF7A2A;
            case 5 -> 0x9CF06A;
            default -> 0xFFA040;
        };
    }

    static ItemStack heater(int heat) {
        return switch (heat) {
            case 3 -> new ItemStack(Items.SOUL_CAMPFIRE);
            case 4 -> new ItemStack(Items.LAVA_BUCKET);
            case 5 -> new ItemStack(ModItems.CHEM_BURNER.get());
            default -> new ItemStack(Items.CAMPFIRE);
        };
    }

    private static void water(GuiGraphicsExtractor graphics, List<Map.Entry<Essence, Integer>> essences, int x0, int y0, int x1, int y1, long now) {
        int r = 0;
        int g = 0;
        int b = 0;
        int weight = 0;
        for (Map.Entry<Essence, Integer> entry : essences) {
            int color = entry.getKey().color();
            r += (color >> 16 & 0xFF) * entry.getValue();
            g += (color >> 8 & 0xFF) * entry.getValue();
            b += (color & 0xFF) * entry.getValue();
            weight += entry.getValue();
        }
        int tint = weight == 0 ? 0x3A78B0 : (r / weight) << 16 | (g / weight) << 8 | b / weight;
        PixelArt.fill(graphics, x0, y0, x1, y1, PixelArt.argb(PixelArt.mix(tint, 0x10142A, 0.72F), 1));
        PixelArt.fill(graphics, x0, y0, x1, y0 + 1, PixelArt.argb(PixelArt.mix(tint, 0xFFFFFF, 0.25F), 0.9F));
        int height = y1 - y0 - 3;
        int bubble = PixelArt.argb(PixelArt.mix(tint, 0xFFFFFF, 0.5F), 0.55F);
        for (int i = 0; i < 7; i++) {
            int period = 1300 + i * 170;
            float t = (float) ((now + i * 431L) % period) / period;
            int bx = x0 + 4 + (i * 37) % Math.max(1, x1 - x0 - 8);
            int by = y1 - 2 - (int) (t * height);
            int size = i % 3 == 0 ? 2 : 1;
            PixelArt.fill(graphics, bx, by, bx + size, by + size, bubble);
        }
    }

    private static void ironPot(GuiGraphicsExtractor graphics, int top, int bottom) {
        int x = POT_X;
        int right = x + POT_WIDTH;
        int dark = PixelArt.argb(IRON_DARK, 1);
        int iron = PixelArt.argb(IRON, 1);
        int light = PixelArt.argb(IRON_LIGHT, 1);
        PixelArt.fill(graphics, x, top + 2, x + WALL, bottom - 2, iron);
        PixelArt.fill(graphics, right - WALL, top + 2, right, bottom - 2, iron);
        PixelArt.fill(graphics, x, top + 2, x + 1, bottom - 2, light);
        PixelArt.fill(graphics, right - 1, top + 2, right, bottom - 2, dark);
        PixelArt.fill(graphics, x + 2, bottom - WALL, right - 2, bottom, iron);
        PixelArt.fill(graphics, x + 1, bottom - 2, x + 2, bottom - 1, iron);
        PixelArt.fill(graphics, right - 2, bottom - 2, right - 1, bottom - 1, iron);
        PixelArt.fill(graphics, x + 2, bottom - 1, right - 2, bottom, dark);
        PixelArt.fill(graphics, x - 2, top, right + 2, top + 3, iron);
        PixelArt.fill(graphics, x - 2, top, right + 2, top + 1, light);
        PixelArt.fill(graphics, x - 2, top + 2, right + 2, top + 3, dark);
        PixelArt.fill(graphics, x + 4, bottom, x + 8, bottom + 3, iron);
        PixelArt.fill(graphics, right - 8, bottom, right - 4, bottom + 3, iron);
    }

    private static void fire(GuiGraphicsExtractor graphics, int potBottom, int heat, long now) {
        int outer = flameColor(heat);
        int inner = PixelArt.mix(outer, 0xFFFFE0, 0.55F);
        int base = potBottom + 9;
        for (int fx = POT_X + 11; fx < POT_X + POT_WIDTH - 11; fx += 4) {
            int flicker = (int) ((now / 110 + fx * 7L) % 4);
            int tall = 4 + (fx * 13 % 3) + (flicker == 0 ? 1 : 0) - (flicker == 3 ? 1 : 0);
            PixelArt.fill(graphics, fx, base - tall, fx + 3, base, PixelArt.argb(outer, 0.9F));
            PixelArt.fill(graphics, fx + 1, base - tall + 2, fx + 2, base, PixelArt.argb(inner, 0.95F));
        }
    }

    private static void fall(GuiGraphicsExtractor graphics, int cx, int y0, int y1, long now) {
        int color = PixelArt.argb(FALL, 1);
        int phase = (int) (6 - now / 90 % 6);
        PixelArt.dotted(graphics, cx - 1, y0, cx - 1, y1 - 4, 3, 3, phase, color);
        PixelArt.dotted(graphics, cx, y0, cx, y1 - 4, 3, 3, phase, color);
        for (int i = 0; i < 3; i++) {
            PixelArt.fill(graphics, cx - 3 + i, y1 - 3 + i, cx + 3 - i, y1 - 2 + i, color);
        }
    }
}
