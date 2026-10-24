package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.crafting.CrucibleRecipe;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.List;
import java.util.Map;

/**
 * Crucible transmutations: the essences the water must hold (a column of essence items with their mB), the catalyst
 * thrown in, and what rises out.
 */
public class CrucibleCategory extends AbstractRecipeCategory<CrucibleCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "crucible", Recipe.class);

    public record Recipe(ResourceLocation id, CrucibleRecipe recipe) {
        List<Map.Entry<Essence, Integer>> essences() {
            return EssenceColumn.entries(recipe.essences().asMap());
        }
    }

    private final int rows;

    public CrucibleCategory(IGuiHelper guiHelper) {
        this(guiHelper, EssenceColumn.rows(CrucibleRecipe.ALL.get(true).values().stream().map(recipe -> recipe.essences().asMap()).toList()));
    }

    private CrucibleCategory(IGuiHelper guiHelper, int rows) {
        super(TYPE, Component.translatable("block.hextale.crucible"), guiHelper.createDrawableItemLike(ModItems.CRUCIBLE.get()),
                StepCategory.WIDTH, EssenceColumn.height(rows) + 2);
        this.rows = rows;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        EssenceColumn.addEssences(builder, recipe.essences(), rows);
        int y = EssenceColumn.middle(rows);
        builder.addInputSlot(EssenceColumn.CATALYST_X, y).setStandardSlotBackground().add(recipe.recipe().catalyst())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.crucible.catalyst")));
        builder.addOutputSlot(StepCategory.RESULT_X, y).setOutputSlotBackground().add(recipe.recipe().result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        EssenceColumn.addArrows(builder, rows);
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        EssenceColumn.drawAmounts(graphics, recipe.essences(), rows);
    }

    @Override
    public ResourceLocation getIdentifier(Recipe recipe) {
        return recipe.id();
    }
}
