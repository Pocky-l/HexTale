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
 * Crucible brews, drawn as the pot ({@link CruciblePot}): the essences the boiling water must hold, the catalyst thrown
 * in from above, the vials of brew it gives (one per dose, scooped with the glass bottle over the arrow), and under the
 * pot the heat it needs.
 */
public class BrewingCategory extends AbstractRecipeCategory<BrewingCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "brewing", Recipe.class);

    public record Recipe(Identifier id, BrewRecipe recipe) {
        List<Map.Entry<Essence, Integer>> essences() {
            return CruciblePot.entries(recipe.essences().asMap());
        }
    }

    private final int rows;

    public BrewingCategory(IGuiHelper guiHelper) {
        this(guiHelper, CruciblePot.rows(BrewRecipe.ALL.get(true).values().stream().map(recipe -> recipe.essences().asMap()).toList()));
    }

    private BrewingCategory(IGuiHelper guiHelper, int rows) {
        super(TYPE, Component.translatable("jei.hextale.brewing"), guiHelper.createDrawableItemLike(ModItems.CHEM_VIAL.get()),
                StepCategory.WIDTH, CruciblePot.height(rows));
        this.rows = rows;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        CruciblePot.addEssences(builder, recipe.essences());
        int y = CruciblePot.middle(rows) - 8;
        builder.addInputSlot(CruciblePot.catalystX(), CruciblePot.CATALYST_Y).setStandardSlotBackground().add(recipe.recipe().catalyst())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.crucible.catalyst")));
        builder.addInputSlot(CruciblePot.ARROW_X + 4, y - 22).setStandardSlotBackground()
                .add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.brewing.bottled")));
        builder.addOutputSlot(StepCategory.RESULT_X, y).setOutputSlotBackground()
                .add(ChemVialItem.of(recipe.recipe().brew(), recipe.recipe().doses()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.brewing.bottled")));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(CruciblePot.ARROW_X, CruciblePot.middle(rows) - 8);
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        int level = recipe.recipe().heat();
        CruciblePot.draw(graphics, recipe.essences(), rows, level);
        // The heater's icon says which heat; words only when a hotter one will do too.
        CruciblePot.drawHeat(graphics, rows, level, level > 1 && level < BrewRecipe.BURNER_HEAT
                ? Component.translatable("book.hextale.quest.crucible.or_hotter") : null);
    }

    @Override
    public Identifier getIdentifier(Recipe recipe) {
        return recipe.id();
    }
}
