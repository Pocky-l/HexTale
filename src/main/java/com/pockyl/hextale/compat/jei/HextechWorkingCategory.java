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
import com.pockyl.hextale.registry.ModItems;

import java.util.List;

/**
 * How hextech is worked in the world: lightning shatters amethyst into orbs, a thrown orb shatters into dust, dust wakes
 * a crafting table and a furnace, and the lapidary cuts orbs into crystals and crystals into gemstones.
 */
public class HextechWorkingCategory extends AbstractRecipeCategory<HextechWorkingCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "hextech_working", Recipe.class);

    /** Inputs side by side, the result, and a short note under them ({@code jei.hextale.working.<note>}). */
    public record Recipe(List<ItemStack> inputs, ItemStack result, String note) {
    }

    public HextechWorkingCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.hextale.working"), guiHelper.createDrawableItemLike(ModItems.HEXTECH_DUST.get()), 120, 44);
    }

    public static List<Recipe> all() {
        return List.of(
                new Recipe(List.of(new ItemStack(Items.LIGHTNING_ROD.asList().getFirst()), new ItemStack(Items.AMETHYST_BLOCK)),
                        new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), "lightning"),
                new Recipe(List.of(new ItemStack(ModItems.UNSTABLE_HEXTECH.get())), new ItemStack(ModItems.HEXTECH_DUST.get(), 2), "throw"),
                new Recipe(List.of(new ItemStack(ModItems.HEXTECH_DUST.get()), new ItemStack(Items.CRAFTING_TABLE)),
                        new ItemStack(ModItems.HEX_WORKBENCH.get()), "awaken"),
                new Recipe(List.of(new ItemStack(ModItems.HEXTECH_DUST.get()), new ItemStack(Items.FURNACE)),
                        new ItemStack(ModItems.HEX_FURNACE.get()), "awaken"),
                new Recipe(List.of(new ItemStack(ModItems.HEXTECH_DUST.get()), new ItemStack(Items.CAULDRON)),
                        new ItemStack(ModItems.CRUCIBLE.get()), "awaken"),
                new Recipe(List.of(new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), new ItemStack(ModItems.CUTTING_DISC.get())),
                        new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), "cut"),
                new Recipe(List.of(new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), new ItemStack(ModItems.MASTER_CUTTING_DISC.get())),
                        new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), "finish"));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.inputs().size(); i++) {
            builder.addInputSlot(1 + i * 22, 1).setStandardSlotBackground().addItemStack(recipe.inputs().get(i));
        }
        builder.addOutputSlot(99, 1).setOutputSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        if (recipe.inputs().size() > 1) {
            builder.addRecipePlusSignWidget().setPosition(19, 4);
        }
        builder.addRecipeArrowWidget().setPosition(66, 1);
        builder.addText(Component.translatable("jei.hextale.working." + recipe.note()), 118, 20).setPosition(0, 22).setColor(0xFF404040);
    }
}
