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
import net.minecraft.resources.Identifier;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.crafting.CrucibleRecipe;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.List;
import java.util.Map;

/**
 * Crucible transmutations, drawn as the pot ({@link CruciblePot}): the essences the water must hold, the catalyst thrown
 * in from above, what rises out, and the heat under it.
 */
public class CrucibleCategory extends AbstractRecipeCategory<CrucibleCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "crucible", Recipe.class);

    public record Recipe(Identifier id, CrucibleRecipe recipe) {
        List<Map.Entry<Essence, Integer>> essences() {
            return CruciblePot.entries(recipe.essences().asMap());
        }
    }

    private final int rows;

    public CrucibleCategory(IGuiHelper guiHelper) {
        this(guiHelper, CruciblePot.rows(CrucibleRecipe.ALL.get(true).values().stream().map(recipe -> recipe.essences().asMap()).toList()));
    }

    private CrucibleCategory(IGuiHelper guiHelper, int rows) {
        super(TYPE, Component.translatable("block.hextale.crucible"), guiHelper.createDrawableItemLike(ModItems.CRUCIBLE.get()),
                StepCategory.WIDTH, CruciblePot.height(rows));
        this.rows = rows;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        CruciblePot.addEssences(builder, recipe.essences());
        builder.addInputSlot(CruciblePot.catalystX(), CruciblePot.CATALYST_Y).setStandardSlotBackground().add(recipe.recipe().catalyst())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.crucible.catalyst")));
        ResearchNote.add(builder.addOutputSlot(StepCategory.RESULT_X, CruciblePot.middle(rows) - 8).setOutputSlotBackground().add(recipe.recipe().result()),
                recipe.recipe().result().item().value());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(CruciblePot.ARROW_X, CruciblePot.middle(rows) - 8);
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        CruciblePot.draw(graphics, recipe.essences(), rows, 1);
        // Any fire will do: the campfire's icon says it.
        CruciblePot.drawHeat(graphics, rows, 1, null);
    }

    @Override
    public Identifier getIdentifier(Recipe recipe) {
        return recipe.id();
    }
}
