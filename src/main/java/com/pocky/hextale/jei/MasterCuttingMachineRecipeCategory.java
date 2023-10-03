package com.pocky.hextale.jei;

import com.pocky.hextale.common.recipe.MasterCuttingMachineRecipe;
import com.pocky.hextale.common.register.ModBlocks;
import com.pocky.hextale.common.register.ModRecipeTypes;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public class MasterCuttingMachineRecipeCategory implements IRecipeCategory<MasterCuttingMachineRecipe> {

    public static final int width = 82;
    public static final int height = 34;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component localizedName;

    public MasterCuttingMachineRecipeCategory(IGuiHelper guiHelper) {
        ResourceLocation location = new ResourceLocation(ModIds.JEI_ID, "textures/jei/gui/gui_vanilla.png");
        background = guiHelper.createDrawable(location, 0, 220, width, height);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.MASTER_CUTTING_MACHINE.get()));
        localizedName = Component.translatable("gui.jei.category.stoneCutter");
    }

    @Override
    public RecipeType<MasterCuttingMachineRecipe> getRecipeType() {
        return ModRecipeTypes.MASTER_CUTTING_MACHINE_JEI;
    }

    @Override
    public Component getTitle() {
        return localizedName;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MasterCuttingMachineRecipe recipeHolder, IFocusGroup focuses) {
        MasterCuttingMachineRecipe recipe = recipeHolder;

        builder.addSlot(RecipeIngredientRole.INPUT, 1, 9)
                .addIngredients(recipe.getIngredients().get(0));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 61,  9)
                .addItemStack(getResultItem(recipe));
    }

    @Override
    public boolean isHandled(MasterCuttingMachineRecipe recipeHolder) {
        MasterCuttingMachineRecipe recipe = recipeHolder;
        return !recipe.isSpecial();
    }

    public static ItemStack getResultItem(Recipe<?> recipe) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            throw new NullPointerException("level must not be null.");
        }
        RegistryAccess registryAccess = level.registryAccess();
        return recipe.getResultItem(registryAccess);
    }
}
