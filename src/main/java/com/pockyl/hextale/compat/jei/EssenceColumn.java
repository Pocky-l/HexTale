package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import net.minecraft.client.Minecraft;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import com.pockyl.hextale.item.EssenceItem;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The essences the crucible's water must hold, one row each: the essence as an item, its mB to the right of it. Then a plus
 * sign, the catalyst, an arrow and the result, level with the middle of the column. The column is centred in a box of
 * {@code rows} rows, so recipes with fewer essences stay level with the rest. Shared by transmutations and brews.
 */
final class EssenceColumn {
    static final int ROW = 18;
    static final int CATALYST_X = 72;
    /** Rows the box always has room for; more only when a recipe needs them. */
    static final int MIN_ROWS = 3;

    private EssenceColumn() {
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

    static int height(int rows) {
        return rows * ROW;
    }

    private static int top(int count, int rows) {
        return 1 + (rows - count) * ROW / 2;
    }

    /** The y of the catalyst and the result slots. */
    static int middle(int rows) {
        return height(rows) / 2 - 8;
    }

    static void addEssences(IRecipeLayoutBuilder builder, List<Map.Entry<Essence, Integer>> essences, int rows) {
        int top = top(essences.size(), rows);
        for (int i = 0; i < essences.size(); i++) {
            Map.Entry<Essence, Integer> entry = essences.get(i);
            builder.addInputSlot(1, top + i * ROW).setStandardSlotBackground().add(EssenceItem.of(entry.getKey()))
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.crucible.essence", entry.getValue())));
        }
    }

    static void addArrows(IRecipeExtrasBuilder builder, int rows) {
        int y = middle(rows);
        builder.addRecipePlusSignWidget().setPosition(CATALYST_X - 16, y + 2);
        builder.addRecipeArrowWidget().setPosition(StepCategory.ARROW_X, y);
    }

    static void drawAmounts(GuiGraphicsExtractor graphics, List<Map.Entry<Essence, Integer>> essences, int rows) {
        var font = Minecraft.getInstance().font;
        int top = top(essences.size(), rows);
        for (int i = 0; i < essences.size(); i++) {
            Map.Entry<Essence, Integer> entry = essences.get(i);
            graphics.text(font, entry.getValue() + " mB", 21, top + i * ROW + 5, 0xFF000000 | entry.getKey().color(), true);
        }
    }
}
