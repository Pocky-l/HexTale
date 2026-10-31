package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * The researcher's notes on an item, as a tab of its own after the tabs that show how it is made. JEI's information tab
 * always comes before every mod's tabs, so R on a device opened its notes instead of its recipe. Long notes run over
 * several pages, one recipe each.
 */
public class NotesCategory extends AbstractRecipeCategory<NotesCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "notes", Recipe.class);
    private static final int WIDTH = 160;
    private static final int HEIGHT = 125;
    private static final int TEXT_Y = 22;
    private static final int LINE = 10;
    private static final int LINES_PER_PAGE = (HEIGHT - TEXT_Y) / LINE;

    /** The items the notes are about and one page of their lines. */
    public record Recipe(List<ItemStack> items, List<FormattedCharSequence> lines) {
    }

    public NotesCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.hextale.notes"), guiHelper.createDrawableItemLike(ModItems.RESEARCHERS_DIARY.get()), WIDTH, HEIGHT);
    }

    /** The notes on {@code items}, wrapped to the tab and cut into pages. */
    public static List<Recipe> pages(List<ItemStack> items, Component text) {
        List<FormattedCharSequence> lines = Minecraft.getInstance().font.split(text, WIDTH);
        List<Recipe> out = new ArrayList<>();
        for (int from = 0; from < lines.size(); from += LINES_PER_PAGE) {
            out.add(new Recipe(items, List.copyOf(lines.subList(from, Math.min(lines.size(), from + LINES_PER_PAGE)))));
        }
        return out;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        // An output, so R (how it is made) finds the notes; the invisible input lets U (what it is for) find them too.
        builder.addOutputSlot(WIDTH / 2 - 8, 2).setStandardSlotBackground().addItemStacks(recipe.items());
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(recipe.items());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < recipe.lines().size(); i++) {
            graphics.text(font, recipe.lines().get(i), 0, TEXT_Y + i * LINE, StepCategory.NOTE_COLOR, false);
        }
    }
}
