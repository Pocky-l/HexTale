package com.pockyl.hextale.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.client.RecipeLookup;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiKeyMappings;
import mezz.jei.api.runtime.IJeiRuntime;

/**
 * JEI behind {@link RecipeLookup}: an item under the mouse in the diary's pictures opens JEI's recipes or uses with the
 * player's own JEI bindings, whatever keys they are set to.
 */
final class JeiLookup implements RecipeLookup.Viewer {
    private final IJeiRuntime runtime;

    private JeiLookup(IJeiRuntime runtime) {
        this.runtime = runtime;
    }

    static void start(IJeiRuntime runtime) {
        RecipeLookup.set(new JeiLookup(runtime));
    }

    static void stop() {
        RecipeLookup.set(null);
    }

    @Override
    public boolean show(InputConstants.Key key, ItemStack stack) {
        IJeiKeyMappings keys = runtime.getKeyMappings();
        RecipeIngredientRole role;
        if (keys.getShowRecipe().isActiveAndMatches(key)) {
            role = RecipeIngredientRole.OUTPUT;
        } else if (keys.getShowUses().isActiveAndMatches(key)) {
            role = RecipeIngredientRole.INPUT;
        } else {
            return false;
        }
        runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack.copyWithCount(1)));
        return true;
    }
}
