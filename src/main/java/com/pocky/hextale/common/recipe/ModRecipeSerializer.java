package com.pocky.hextale.common.recipe;

import com.pocky.hextale.HexTaleMod;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeSerializer {

    public static final DeferredRegister<RecipeSerializer<?>> REGISTRY_RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, HexTaleMod.MODID);

    public static final RegistryObject<RecipeSerializer<BaseCuttingMachineRecipe>> BASE_CUTTING_MACHINE = REGISTRY_RECIPE_SERIALIZERS
            .register("base_cutting_machine_recipe", () -> new BaseCuttingMachineRecipe.Serializer<>(BaseCuttingMachineRecipe::new));
    public static final RegistryObject<RecipeSerializer<AdvancedCuttingMachineRecipe>> ADVANCED_CUTTING_MACHINE = REGISTRY_RECIPE_SERIALIZERS
            .register("advanced_cutting_machine_recipe", () -> new AdvancedCuttingMachineRecipe.Serializer<>(AdvancedCuttingMachineRecipe::new));
    public static final RegistryObject<RecipeSerializer<EliteCuttingMachineRecipe>> ELITE_CUTTING_MACHINE = REGISTRY_RECIPE_SERIALIZERS
            .register("elite_cutting_machine_recipe", () -> new EliteCuttingMachineRecipe.Serializer<>(EliteCuttingMachineRecipe::new));
    public static final RegistryObject<RecipeSerializer<MasterCuttingMachineRecipe>> MASTER_CUTTING_MACHINE = REGISTRY_RECIPE_SERIALIZERS
            .register("master_cutting_machine_recipe", () -> new MasterCuttingMachineRecipe.Serializer<>(MasterCuttingMachineRecipe::new));
    public static final RegistryObject<RecipeSerializer<HexcoreRecipe>> HEXCORE = REGISTRY_RECIPE_SERIALIZERS
            .register("hexcore_recipe", () -> new HexcoreRecipe.Serializer<>(HexcoreRecipe::new));

}
