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

}
