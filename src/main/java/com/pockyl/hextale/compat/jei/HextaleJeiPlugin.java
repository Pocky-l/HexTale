package com.pockyl.hextale.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.Reagents;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI: how essence is made - boiling in the Alembic and splitting in the Separator - one bottle per concentrate, and an
 * info page for every block of the essence works. What an item boils down to stays a secret of the research: the boiling
 * page shows no item's essences.
 */
@JeiPlugin
public class HextaleJeiPlugin implements IModPlugin {
    private static final Identifier UID = Hextale.id("jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    /** Bottles of different essences are different ingredients. */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerFromDataComponentTypes(ModItems.ESSENCE_FLASK.get(), ModDataComponents.ESSENCE.get());
    }

    /** A bottle of each concentrate and one of unknown essence, so they can be looked up. */
    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        List<ItemStack> bottles = new ArrayList<>();
        for (Essence essence : Essence.values()) {
            bottles.add(concentrate(essence));
        }
        bottles.add(EssenceFlaskItem.filled(EssenceMix.pure(Essence.FIRE, EssenceFlaskItem.CAPACITY).asUnknown()));
        registration.addExtraItemStacks(bottles);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new BoilingCategory(registration.getJeiHelpers().getGuiHelper()),
                new SeparationCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ItemStack> boilable = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            if (ResearchItems.of(stack) != null && !EssenceFlaskItem.isFlask(stack)) {
                boilable.add(stack);
            }
        }
        registration.addRecipes(BoilingCategory.TYPE, List.of(new BoilingCategory.Recipe(boilable)));
        List<SeparationCategory.Recipe> separations = new ArrayList<>();
        for (Essence essence : Essence.values()) {
            List<ItemStack> reagents = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(Reagents.tag(essence)).forEach(holder -> reagents.add(new ItemStack(holder)));
            if (!reagents.isEmpty()) {
                separations.add(new SeparationCategory.Recipe(essence, reagents));
            }
        }
        registration.addRecipes(SeparationCategory.TYPE, separations);
        info(registration, ModItems.ALEMBIC.get(), "alembic");
        info(registration, ModItems.SEPARATOR.get(), "separator");
        info(registration, ModItems.ESSENCE_TANK.get(), "essence_tank");
        info(registration, ModItems.BRASS_PIPE.get(), "brass_pipe");
        info(registration, ModItems.PUMP.get(), "pump");
        info(registration, ModItems.DRAIN_VALVE.get(), "drain_valve");
        info(registration, ModItems.RESEARCH_TABLE.get(), "research_table");
        registration.addItemStackInfo(new ItemStack(ModItems.ESSENCE_FLASK.get()), Component.translatable("jei.hextale.info.essence_flask"));
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addIngredientInfo(item, Component.translatable("jei.hextale.info." + key));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(BoilingCategory.TYPE, ModItems.ALEMBIC.get());
        registration.addCraftingStation(SeparationCategory.TYPE, ModItems.SEPARATOR.get());
    }

    static ItemStack concentrate(Essence essence) {
        return EssenceFlaskItem.filled(EssenceMix.pure(essence, EssenceFlaskItem.CAPACITY));
    }
}
