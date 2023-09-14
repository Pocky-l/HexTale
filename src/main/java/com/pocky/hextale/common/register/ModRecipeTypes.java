package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.recipe.BaseCuttingMachineRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> REGISTRY_RECIPE = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, HexTaleMod.MODID);

    public static final RegistryObject<RecipeType<BaseCuttingMachineRecipe>> BASE_CUTTING_MACHINE = REGISTRY_RECIPE
            .register("base_cutting_machine_recipe", () -> new RecipeType<>() {});

}
