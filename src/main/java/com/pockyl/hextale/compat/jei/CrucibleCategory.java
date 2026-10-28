package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
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
    public static final RecipeType<Recipe> TYPE = RecipeType.create(Hextale.MOD_ID, "crucible", Recipe.class);

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
        builder.addInputSlot(EssenceColumn.CATALYST_X, y).setStandardSlotBackground().addIngredientsUnsafe(com.pockyl.hextale.port.PortJei.stacks(recipe.recipe().catalyst()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.crucible.catalyst")));
        ResearchNote.add(builder.addOutputSlot(StepCategory.RESULT_X, y).setOutputSlotBackground().addIngredientsUnsafe(com.pockyl.hextale.port.PortJei.stacks(recipe.recipe().result())),
                recipe.recipe().result().item().value());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        EssenceColumn.addArrows(builder, rows);
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, net.minecraft.client.gui.GuiGraphics portGraphics, double mouseX, double mouseY) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        EssenceColumn.drawAmounts(graphics, recipe.essences(), rows);
    }

    @Override
    public ResourceLocation getRegistryName(Recipe recipe) {
        return recipe.id();
    }
}
