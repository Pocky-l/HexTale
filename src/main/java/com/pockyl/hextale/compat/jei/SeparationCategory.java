package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.Essence;

import java.util.List;

/**
 * Splitting in the Separator: a mixture with one essence chosen (and blaze powder burned) gives that essence's concentrate (the wanted
 * product) and the rest of the mixture as an unknown essence (the by-product).
 */
public class SeparationCategory extends AbstractRecipeCategory<SeparationCategory.Recipe> {
    public static final RecipeType<Recipe> TYPE = RecipeType.create(Hextale.MOD_ID, "separation", Recipe.class);

    /** The fuel burned to draw {@code essence} out (blaze powder). */
    public record Recipe(Essence essence, List<ItemStack> reagents) {
    }

    public SeparationCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.hextale.separation"), guiHelper.createDrawableItemLike(ModItems.SEPARATOR.get()), 120, 40);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStack(new ItemStack(ModItems.ESSENCE_FLASK.get()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.separation.mixture",
                        ResearchText.essence(recipe.essence()))));
        builder.addInputSlot(1, 21).setStandardSlotBackground().addItemStacks(recipe.reagents())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("gui.hextale.separator.binds",
                        ResearchText.essence(recipe.essence()))));
        builder.addOutputSlot(96, 1).setOutputSlotBackground().addItemStack(HextaleJeiPlugin.concentrate(recipe.essence()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("gui.hextale.port.product.hint")));
        builder.addOutputSlot(96, 21).setOutputSlotBackground()
                .addItemStack(EssenceFlaskItem.filled(EssenceMix.pure(recipe.essence() == Essence.FIRE ? Essence.WATER : Essence.FIRE, 12)
                        .plus(EssenceMix.unknown(2))))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("gui.hextale.port.waste.hint")));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(SeparatorBlockEntity.STEP_TICKS * 4).setPosition(48, 11);
    }
}
