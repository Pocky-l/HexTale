package com.pockyl.hextale.infusion;

import com.pockyl.hextale.crafting.DataRecipes;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Every altar recipe: recipe type {@code hextale:infusion} ({@code data/<namespace>/recipe/**.json}). The server reads its
 * recipe manager; clients keep what the server sent them (quests and JEI show them).
 */
public final class InfusionRecipes {
    public static final DataRecipes<InfusionRecipe> ALL = new DataRecipes<>("infusion", InfusionRecipe.MAP_CODEC, InfusionRecipe.STREAM_CODEC,
            recipe -> recipe.result().item().value());

    private InfusionRecipes() {
    }

    public static Map<Identifier, InfusionRecipe> get(boolean clientSide) {
        return ALL.get(clientSide);
    }

    public static void setClient(Map<Identifier, InfusionRecipe> recipes) {
        ALL.setClient(recipes);
    }
}
