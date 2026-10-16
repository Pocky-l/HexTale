package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.registry.ModItems;

import java.util.List;

/**
 * Boiling in the Alembic: a studied item in water over heat, and a glass bottle, give a bottle of essence - a mixture of
 * the item's essences, which only the research reveals.
 */
public class BoilingCategory extends AbstractRecipeCategory<BoilingCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "boiling", Recipe.class);

    /** The items that boil into essence (any of them, studied first). */
    public record Recipe(List<ItemStack> items) {
    }

    public BoilingCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.hextale.boiling"), guiHelper.createDrawableItemLike(ModItems.ALEMBIC.get()), 120, 54);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        List<ItemStack> items = recipe.items().isEmpty()
                ? List.of(new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.COAL), new ItemStack(Items.KELP)) : recipe.items();
        builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(items);
        builder.addInputSlot(1, 19).setStandardSlotBackground().addItemStack(new ItemStack(Items.WATER_BUCKET))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.boiling.water",
                        AlembicBlockEntity.WATER_PER_ITEM)));
        builder.addInputSlot(1, 37).setStandardSlotBackground()
                .addItemStacks(List.of(new ItemStack(Items.CAMPFIRE), new ItemStack(Items.MAGMA_BLOCK), new ItemStack(Items.LAVA_BUCKET),
                        new ItemStack(Items.FLINT_AND_STEEL)))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.boiling.heat")));
        builder.addInputSlot(30, 19).setStandardSlotBackground().addItemStack(new ItemStack(Items.GLASS_BOTTLE));
        builder.addOutputSlot(96, 19).setOutputSlotBackground().addItemStack(new ItemStack(ModItems.ESSENCE_FLASK.get()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.hextale.boiling.result")));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(AlembicBlockEntity.BOIL_TICKS).setPosition(58, 19);
    }
}
