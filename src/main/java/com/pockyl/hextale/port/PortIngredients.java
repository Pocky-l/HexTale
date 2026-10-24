package com.pockyl.hextale.port;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/** Ingredient and recipe views of Minecraft 26.x (holders, placement info, displays) over the 1.21.1 recipe API. */
public final class PortIngredients {
    private PortIngredients() {
    }

    /** {@code ingredient.items()} of 26.x: the items it accepts. */
    public static Stream<Holder<Item>> items(Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems()).map(ItemStack::getItemHolder).distinct();
    }

    /** {@code recipe.placementInfo().ingredients()} without the empty cells. */
    public static List<Ingredient> ingredients(Recipe<?> recipe) {
        return recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList();
    }

    /** What the recipe book shows as the result ({@code recipe.display().getFirst().result()} of 26.x). */
    public static ItemStack result(Recipe<?> recipe, HolderLookup.Provider registries) {
        return recipe.getResultItem(registries);
    }
}
