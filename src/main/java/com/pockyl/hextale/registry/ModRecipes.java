package com.pockyl.hextale.registry;

import com.pockyl.hextale.crafting.AlloyRecipe;
import com.pockyl.hextale.crafting.AwakeningRecipe;
import com.pockyl.hextale.crafting.BrewRecipe;
import com.pockyl.hextale.crafting.CrucibleRecipe;
import com.pockyl.hextale.crafting.DataRecipes;
import com.pockyl.hextale.crafting.HexCraftingRecipe;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.research.ExperimentPlan;
import net.neoforged.bus.api.IEventBus;

import java.util.List;

/** The mod's recipe types and serializers: one per {@link DataRecipes} kind. */
public final class ModRecipes {
    /** Every kind; naming them here loads their classes, which registers their types before the registry event. */
    public static final List<DataRecipes<?>> KINDS = List.of(HexCraftingRecipe.ALL, InfusionRecipes.ALL, AlloyRecipe.ALL,
            CrucibleRecipe.ALL, BrewRecipe.ALL, ExperimentPlan.ALL, AwakeningRecipe.ALL);

    private ModRecipes() {
    }

    public static void register(IEventBus modBus) {
        DataRecipes.register(modBus);
    }
}
