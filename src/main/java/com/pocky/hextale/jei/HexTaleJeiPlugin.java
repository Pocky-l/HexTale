package com.pocky.hextale.jei;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.recipe.AdvancedCuttingMachineRecipe;
import com.pocky.hextale.common.recipe.BaseCuttingMachineRecipe;
import com.pocky.hextale.common.recipe.EliteCuttingMachineRecipe;
import com.pocky.hextale.common.recipe.MasterCuttingMachineRecipe;
import com.pocky.hextale.common.register.ModBlocks;
import com.pocky.hextale.common.register.ModRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@JeiPlugin
public class HexTaleJeiPlugin implements IModPlugin {

    @Nullable
    private IRecipeCategory<BaseCuttingMachineRecipe> baseCuttingMachineRecipeCategory;
    @Nullable
    private IRecipeCategory<AdvancedCuttingMachineRecipe> advancedCuttingMachineRecipeCategory;
    @Nullable
    private IRecipeCategory<EliteCuttingMachineRecipe> eliteCuttingMachineRecipeCategory;
    @Nullable
    private IRecipeCategory<MasterCuttingMachineRecipe> masterCuttingMachineRecipeCategory;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(HexTaleMod.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(baseCuttingMachineRecipeCategory = new BaseCuttingMachineRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(advancedCuttingMachineRecipeCategory = new AdvancedCuttingMachineRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(eliteCuttingMachineRecipeCategory = new EliteCuttingMachineRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(masterCuttingMachineRecipeCategory = new MasterCuttingMachineRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ModRecipeTypes.BASE_CUTTING_MACHINE_JEI, getBaseCuttingMachineRecipes(baseCuttingMachineRecipeCategory, registration.getIngredientManager()));
        registration.addRecipes(ModRecipeTypes.ADVANCED_CUTTING_MACHINE_JEI, getAdvancedCuttingMachineRecipes(advancedCuttingMachineRecipeCategory, registration.getIngredientManager()));
        registration.addRecipes(ModRecipeTypes.ELITE_CUTTING_MACHINE_JEI, getEliteCuttingMachineRecipes(eliteCuttingMachineRecipeCategory, registration.getIngredientManager()));
        registration.addRecipes(ModRecipeTypes.MASTER_CUTTING_MACHINE_JEI, getMasterCuttingMachineRecipes(masterCuttingMachineRecipeCategory, registration.getIngredientManager()));

    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.BASE_CUTTING_MACHINE.get()), ModRecipeTypes.BASE_CUTTING_MACHINE_JEI);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ADVANCED_CUTTING_MACHINE.get()), ModRecipeTypes.ADVANCED_CUTTING_MACHINE_JEI);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELITE_CUTTING_MACHINE.get()), ModRecipeTypes.ELITE_CUTTING_MACHINE_JEI);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MASTER_CUTTING_MACHINE.get()), ModRecipeTypes.MASTER_CUTTING_MACHINE_JEI);
    }

    public List<BaseCuttingMachineRecipe> getBaseCuttingMachineRecipes(IRecipeCategory<BaseCuttingMachineRecipe> stonecuttingCategory, IIngredientManager ingredientManager) {
        var validator = new CategoryRecipeValidator<>(stonecuttingCategory, ingredientManager, 1);
        return getValidHandledRecipes(Minecraft.getInstance().level.getRecipeManager(), ModRecipeTypes.BASE_CUTTING_MACHINE.get(), validator);
    }

    public List<AdvancedCuttingMachineRecipe> getAdvancedCuttingMachineRecipes(IRecipeCategory<AdvancedCuttingMachineRecipe> stonecuttingCategory, IIngredientManager ingredientManager) {
        var validator = new CategoryRecipeValidator<>(stonecuttingCategory, ingredientManager, 1);
        return getValidHandledRecipes(Minecraft.getInstance().level.getRecipeManager(), ModRecipeTypes.ADVANCED_CUTTING_MACHINE.get(), validator);
    }

    public List<EliteCuttingMachineRecipe> getEliteCuttingMachineRecipes(IRecipeCategory<EliteCuttingMachineRecipe> stonecuttingCategory, IIngredientManager ingredientManager) {
        var validator = new CategoryRecipeValidator<>(stonecuttingCategory, ingredientManager, 1);
        return getValidHandledRecipes(Minecraft.getInstance().level.getRecipeManager(), ModRecipeTypes.ELITE_CUTTING_MACHINE.get(), validator);
    }

    public List<MasterCuttingMachineRecipe> getMasterCuttingMachineRecipes(IRecipeCategory<MasterCuttingMachineRecipe> stonecuttingCategory, IIngredientManager ingredientManager) {
        var validator = new CategoryRecipeValidator<>(stonecuttingCategory, ingredientManager, 1);
        return getValidHandledRecipes(Minecraft.getInstance().level.getRecipeManager(), ModRecipeTypes.MASTER_CUTTING_MACHINE.get(), validator);
    }

    private static <C extends Container, T extends Recipe<C>> List<T> getValidHandledRecipes(
            RecipeManager recipeManager,
            RecipeType<T> recipeType,
            CategoryRecipeValidator<T> validator
    ) {
        return recipeManager.getAllRecipesFor(recipeType)
                .stream()
                .filter(r -> validator.isRecipeValid(r) && validator.isRecipeHandled(r))
                .toList();
    }
}
