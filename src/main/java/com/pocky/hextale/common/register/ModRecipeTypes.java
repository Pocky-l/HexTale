package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.recipe.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> REGISTRY_RECIPE = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, HexTaleMod.MODID);

    public static final RegistryObject<RecipeType<BaseCuttingMachineRecipe>> BASE_CUTTING_MACHINE = REGISTRY_RECIPE
            .register("base_cutting_machine_recipe", () -> new RecipeType<>() {});

    public static final RegistryObject<RecipeType<AdvancedCuttingMachineRecipe>> ADVANCED_CUTTING_MACHINE = REGISTRY_RECIPE
            .register("advanced_cutting_machine_recipe", () -> new RecipeType<>() {});
    public static final RegistryObject<RecipeType<EliteCuttingMachineRecipe>> ELITE_CUTTING_MACHINE = REGISTRY_RECIPE
            .register("elite_cutting_machine_recipe", () -> new RecipeType<>() {});
    public static final RegistryObject<RecipeType<MasterCuttingMachineRecipe>> MASTER_CUTTING_MACHINE = REGISTRY_RECIPE
            .register("master_cutting_machine_recipe", () -> new RecipeType<>() {});
    public static final RegistryObject<RecipeType<HexcoreRecipe>> HEXCORE = REGISTRY_RECIPE
            .register("hexcore_recipe", () -> new RecipeType<>() {});


    public static final mezz.jei.api.recipe.RecipeType<BaseCuttingMachineRecipe> BASE_CUTTING_MACHINE_JEI = register("base_cutting_machine", BaseCuttingMachineRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<AdvancedCuttingMachineRecipe> ADVANCED_CUTTING_MACHINE_JEI = register("advanced_cutting_machine", AdvancedCuttingMachineRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<EliteCuttingMachineRecipe> ELITE_CUTTING_MACHINE_JEI = register("elite_cutting_machine", EliteCuttingMachineRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<MasterCuttingMachineRecipe> MASTER_CUTTING_MACHINE_JEI = register("master_cutting_machine", MasterCuttingMachineRecipe.class);

    private static <T> mezz.jei.api.recipe.RecipeType<T> register(String name, Class<? extends T> recipeClass) {
        return new mezz.jei.api.recipe.RecipeType<>(new ResourceLocation(HexTaleMod.MODID, name), recipeClass);
    }
}
