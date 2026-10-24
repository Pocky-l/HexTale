package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.crafting.AlloyRecipe;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.List;

/** Hex Furnace alloys: the metals with their counts melt on ordinary fuel into the result; researched alloys say so. */
public class AlloyingCategory extends AbstractRecipeCategory<AlloyingCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "alloying", Recipe.class);

    public record Recipe(ResourceLocation id, AlloyRecipe recipe) {
    }

    public AlloyingCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("block.hextale.hex_furnace"), guiHelper.createDrawableItemLike(ModItems.HEX_FURNACE.get()),
                StepCategory.WIDTH, 48);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.recipe().inputs().size(); i++) {
            var input = recipe.recipe().inputs().get(i);
            List<ItemStack> stacks = input.ingredient().items().map(item -> new ItemStack(item, input.count())).toList();
            builder.addInputSlot(1 + i * 18, StepCategory.ROW_Y).setStandardSlotBackground().addItemStacks(stacks);
        }
        builder.addOutputSlot(StepCategory.RESULT_X, StepCategory.ROW_Y).setOutputSlotBackground().add(recipe.recipe().result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(recipe.recipe().time()).setPosition(StepCategory.ARROW_X, StepCategory.ROW_Y);
        builder.addAnimatedRecipeFlame(300).setPosition(1, StepCategory.ROW_Y + 22);
        if (ResearchTopics.of(recipe.recipe().result().item().value()) != null) {
            builder.addText(Component.translatable("gui.hextale.hex_furnace.locked"), StepCategory.WIDTH - 20, 20)
                    .setPosition(20, StepCategory.ROW_Y + 22).setColor(0xFF8A4A1C);
        }
    }

    @Override
    public ResourceLocation getIdentifier(Recipe recipe) {
        return recipe.id();
    }
}
