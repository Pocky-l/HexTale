package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.crafting.TemperingRecipe;
import com.pockyl.hextale.registry.ModItems;

import java.util.List;

/** Chem Vat temperings: the metals soak over the fire in a bath that must hold the shown vials of acid and shimmer. */
public class TemperingCategory extends AbstractRecipeCategory<TemperingCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "tempering", Recipe.class);
    private static final int BATH_X = 59;

    public record Recipe(Identifier id, TemperingRecipe recipe) {
    }

    public TemperingCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.hextale.tempering"), guiHelper.createDrawableItemLike(ModItems.CHEM_VAT.get()),
                StepCategory.WIDTH, 48);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        TemperingRecipe tempering = recipe.recipe();
        for (int i = 0; i < tempering.inputs().size(); i++) {
            var input = tempering.inputs().get(i);
            List<ItemStack> stacks = input.ingredient().items().map(item -> new ItemStack(item, input.count())).toList();
            builder.addInputSlot(1 + i * 18, StepCategory.ROW_Y).setStandardSlotBackground().addItemStacks(stacks);
        }
        // The bath: vials poured into the vat beforehand, one dose each; the tempering spends them.
        int x = BATH_X;
        if (tempering.acid() > 0) {
            builder.addInputSlot(x, StepCategory.ROW_Y + 22).setStandardSlotBackground()
                    .add(ChemVialItem.of(Brew.ACID, tempering.acid()));
            x += 18;
        }
        if (tempering.shimmer() > 0) {
            builder.addInputSlot(x, StepCategory.ROW_Y + 22).setStandardSlotBackground()
                    .add(ChemVialItem.of(Brew.REFINED_SHIMMER, tempering.shimmer()));
        }
        ResearchNote.add(builder.addOutputSlot(StepCategory.RESULT_X, StepCategory.ROW_Y).setOutputSlotBackground().add(tempering.result()),
                tempering.result().item().value());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(recipe.recipe().time()).setPosition(StepCategory.ARROW_X, StepCategory.ROW_Y);
        builder.addAnimatedRecipeFlameWidget(300).setPosition(1, StepCategory.ROW_Y + 22);
    }

    @Override
    public Identifier getIdentifier(Recipe recipe) {
        return recipe.id();
    }
}
