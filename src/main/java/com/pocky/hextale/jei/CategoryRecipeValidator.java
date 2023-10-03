package com.pocky.hextale.jei;

import com.pocky.hextale.HexTaleMod;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public final class CategoryRecipeValidator<T extends Recipe<?>> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int INVALID_COUNT = -1;
    private final IRecipeCategory<T> recipeCategory;
    private final IIngredientManager ingredientManager;
    private final int maxInputs;

    public CategoryRecipeValidator(IRecipeCategory<T> recipeCategory, IIngredientManager ingredientManager, int maxInputs) {
        this.recipeCategory = recipeCategory;
        this.ingredientManager = ingredientManager;
        this.maxInputs = maxInputs;
    }

    public boolean isRecipeValid(T recipeHolder) {
        return hasValidInputsAndOutputs(recipeHolder);
    }

    public boolean isRecipeHandled(T recipeHolder) {
        return this.recipeCategory.isHandled(recipeHolder);
    }

    @SuppressWarnings("ConstantConditions")
    private boolean hasValidInputsAndOutputs(T recipeHolder) {
        T recipe = recipeHolder;
        if (recipe.isSpecial()) {
            return true;
        }
        ItemStack recipeOutput = getResultItem(recipe);
        if (recipeOutput == null || recipeOutput.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                //String recipeInfo = RecipeErrorUtil.getInfoFromRecipe(recipeHolder, recipeCategory, ingredientManager);
                LOGGER.debug("Skipping Recipe because it has no output. {}", HexTaleMod.MODID);
            }
            return false;
        }
        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients == null) {
            if (LOGGER.isDebugEnabled()) {
                //String recipeInfo = RecipeErrorUtil.getInfoFromRecipe(recipeHolder, recipeCategory, ingredientManager);
                LOGGER.debug("Skipping Recipe because it has no input Ingredients. {}", HexTaleMod.MODID);
            }
            return false;
        }
        int inputCount = getInputCount(ingredients);
        if (inputCount == INVALID_COUNT) {
            if (LOGGER.isDebugEnabled()) {
                //String recipeInfo = RecipeErrorUtil.getInfoFromRecipe(recipeHolder, recipeCategory, ingredientManager);
                LOGGER.debug("Skipping Recipe because it contains invalid inputs. {}", HexTaleMod.MODID);
            }
            return false;
        } else if (inputCount > maxInputs) {
            if (LOGGER.isDebugEnabled()) {
                //String recipeInfo = RecipeErrorUtil.getInfoFromRecipe(recipeHolder, recipeCategory, ingredientManager);
                LOGGER.debug("Skipping Recipe because it has too many inputs. {}", HexTaleMod.MODID);
            }
            return false;
        } else if (inputCount == 0 && maxInputs > 0) {
            if (LOGGER.isDebugEnabled()) {
                //String recipeInfo = RecipeErrorUtil.getInfoFromRecipe(recipeHolder, recipeCategory, ingredientManager);
                LOGGER.debug("Skipping Recipe because it has no inputs. {}", HexTaleMod.MODID);
            }
            return false;
        }
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    private static int getInputCount(List<Ingredient> ingredientList) {
        int inputCount = 0;
        for (Ingredient ingredient : ingredientList) {
            ItemStack[] input = ingredient.getItems();
            if (input == null) {
                return INVALID_COUNT;
            } else {
                inputCount++;
            }
        }
        return inputCount;
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