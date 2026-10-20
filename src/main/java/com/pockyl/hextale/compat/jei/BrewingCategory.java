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
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.crafting.BrewRecipe;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.List;
import java.util.Map;

/**
 * Crucible brews: the essences the boiling water must hold, the catalyst thrown in, the vials of brew it gives (one per
 * dose, bottled with glass bottles), and under them the heat it needs.
 */
public class BrewingCategory extends AbstractRecipeCategory<BrewingCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "brewing", Recipe.class);

    public record Recipe(Identifier id, BrewRecipe recipe) {
        List<Map.Entry<Essence, Integer>> essences() {
            return EssenceColumn.entries(recipe.essences().asMap());
        }
    }

    private final int rows;

    public BrewingCategory(IGuiHelper guiHelper) {
        this(guiHelper, EssenceColumn.rows(BrewRecipe.ALL.get(true).values().stream().map(recipe -> recipe.essences().asMap()).toList()));
    }

    private BrewingCategory(IGuiHelper guiHelper, int rows) {
        super(TYPE, Component.translatable("jei.hextale.brewing"), guiHelper.createDrawableItemLike(ModItems.CHEM_VIAL.get()),
                StepCategory.WIDTH, EssenceColumn.height(rows) + 14);
        this.rows = rows;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        EssenceColumn.addEssences(builder, recipe.essences(), rows);
        int y = EssenceColumn.middle(rows);
        builder.addInputSlot(EssenceColumn.CATALYST_X, y).setStandardSlotBackground().add(recipe.recipe().catalyst())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.crucible.catalyst")));
        builder.addOutputSlot(StepCategory.RESULT_X, y).setOutputSlotBackground()
                .add(ChemVialItem.of(recipe.recipe().brew(), recipe.recipe().doses()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.brewing.bottled")));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        EssenceColumn.addArrows(builder, rows);
        int level = recipe.recipe().heat();
        Component heat = Component.translatable(level >= 5 ? "jei.hextale.brewing.heat_only" : "jei.hextale.brewing.heat",
                Component.translatable("block.hextale.crucible.heat." + level));
        builder.addText(heat, StepCategory.WIDTH, 10).setPosition(0, EssenceColumn.height(rows) + 4).setColor(0xFF8A4A1C);
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        EssenceColumn.drawAmounts(graphics, recipe.essences(), rows);
    }

    @Override
    public Identifier getIdentifier(Recipe recipe) {
        return recipe.id();
    }
}
