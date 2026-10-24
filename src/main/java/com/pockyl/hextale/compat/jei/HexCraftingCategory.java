package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.crafting.HexCraftingRecipe;
import com.pockyl.hextale.registry.ModItems;

import java.util.List;
import java.util.Optional;

/** Hex Workbench recipes: the 3 x 3 grid, the result and the charge the crystal spends on it. */
public class HexCraftingCategory extends AbstractRecipeCategory<HexCraftingCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "hex_crafting", Recipe.class);

    public record Recipe(ResourceLocation id, HexCraftingRecipe recipe) {
    }

    public HexCraftingCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("block.hextale.hex_workbench"), guiHelper.createDrawableItemLike(ModItems.HEX_WORKBENCH.get()), 116, 64);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        List<Optional<Ingredient>> cells = recipe.recipe().grid();
        for (int i = 0; i < 9; i++) {
            var slot = builder.addSlot(RecipeIngredientRole.INPUT, 1 + i % 3 * 18, 1 + i / 3 * 18).setStandardSlotBackground();
            cells.get(i).ifPresent(slot::add);
        }
        builder.addOutputSlot(95, 19).setOutputSlotBackground().add(recipe.recipe().result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(61, 19);
        builder.addText(Component.translatable("jei.hextale.hex_crafting.charge", recipe.recipe().charge()), 56, 10)
                .setPosition(60, 42).setColor(0xFF2A7FB8)
                .setTooltip(Component.translatable("jei.hextale.hex_crafting.charge.tooltip", recipe.recipe().charge()));
    }

    @Override
    public ResourceLocation getIdentifier(Recipe recipe) {
        return recipe.id();
    }
}
