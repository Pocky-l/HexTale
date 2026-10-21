package com.pockyl.hextale.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
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
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI: every way the mod makes something - boiling and separating essence, the altar's rituals, rune burning and rune
 * writing, the hex workbench and furnace, hextech working, the crucible's transmutations and brews, Lab Bench experiments,
 * what the chem burner and the brazier burn, the still, the axolotl tank, the drain valve's soaking - and an info page for
 * every device. What an item boils down to stays a secret of the research: the boiling page shows no item's essences.
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
        registration.registerFromDataComponentTypes(ModItems.ESSENCE.get(), ModDataComponents.ESSENCE_KIND.get());
        registration.registerFromDataComponentTypes(ModItems.CHEM_VIAL.get(), ModDataComponents.BREW.get());
    }

    /** Typing an essence's name also finds the studied items that carry it. */
    @Override
    public void registerAdvancedSearch(mezz.jei.api.registration.IAdvancedSearchRegistration registration) {
        registration.replaceSearchStorage(EssenceSearch.wrap(registration.getDefaultSearchStorageBuilderFactory()));
    }

    /** A bottle of each concentrate and one of unknown essence, so they can be looked up. */
    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        List<ItemStack> bottles = new ArrayList<>();
        for (Essence essence : Essence.values()) {
            bottles.add(concentrate(essence));
        }
        bottles.add(EssenceFlaskItem.filled(EssenceMix.unknown(EssenceFlaskItem.CAPACITY)));
        registration.addExtraItemStacks(bottles);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new BoilingCategory(registration.getJeiHelpers().getGuiHelper()),
                new SeparationCategory(registration.getJeiHelpers().getGuiHelper()),
                new InfusionCategory(registration.getJeiHelpers().getGuiHelper()),
                new RuneBurningCategory(registration.getJeiHelpers().getGuiHelper()),
                new HexCraftingCategory(registration.getJeiHelpers().getGuiHelper()),
                new AlloyingCategory(registration.getJeiHelpers().getGuiHelper()),
                HextechWorkingCategory.create(registration.getJeiHelpers().getGuiHelper()),
                new CrucibleCategory(registration.getJeiHelpers().getGuiHelper()),
                new BrewingCategory(registration.getJeiHelpers().getGuiHelper()),
                new LabExperimentCategory(registration.getJeiHelpers().getGuiHelper()),
                new ChemFuelCategory(registration.getJeiHelpers().getGuiHelper()));
        for (StepCategory category : DeviceSteps.categories(registration.getJeiHelpers().getGuiHelper())) {
            registration.addRecipeCategories(category);
        }
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
            separations.add(new SeparationCategory.Recipe(essence, List.of(new ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER))));
        }
        registration.addRecipes(SeparationCategory.TYPE, separations);
        registration.addRecipes(InfusionCategory.TYPE, InfusionRecipes.get(true).entrySet().stream()
                .map(entry -> new InfusionCategory.Recipe(entry.getKey(), entry.getValue())).toList());
        registration.addRecipes(RuneBurningCategory.TYPE, RuneBurningCategory.Recipe.all());
        registration.addRecipes(HexCraftingCategory.TYPE, com.pockyl.hextale.crafting.HexCraftingRecipe.ALL.get(true).entrySet().stream()
                .map(entry -> new HexCraftingCategory.Recipe(entry.getKey(), entry.getValue())).toList());
        registration.addRecipes(AlloyingCategory.TYPE, com.pockyl.hextale.crafting.AlloyRecipe.ALL.get(true).entrySet().stream()
                .map(entry -> new AlloyingCategory.Recipe(entry.getKey(), entry.getValue())).toList());
        registration.addRecipes(HextechWorkingCategory.TYPE, HextechWorkingCategory.all());
        registration.addRecipes(CrucibleCategory.TYPE, com.pockyl.hextale.crafting.CrucibleRecipe.ALL.get(true).entrySet().stream()
                .map(entry -> new CrucibleCategory.Recipe(entry.getKey(), entry.getValue())).toList());
        info(registration, ModItems.ESSENCE.get(), "essence");
        info(registration, ModItems.HEX_WORKBENCH.get(), "hex_workbench");
        info(registration, ModItems.HEX_LAPIDARY.get(), "hex_lapidary");
        info(registration, ModItems.HEXTECH_DUST.get(), "hextech_dust");
        info(registration, ModItems.HEX_FURNACE.get(), "hex_furnace");
        registration.addRecipes(BrewingCategory.TYPE, com.pockyl.hextale.crafting.BrewRecipe.ALL.get(true).entrySet().stream()
                .map(entry -> new BrewingCategory.Recipe(entry.getKey(), entry.getValue())).toList());
        registration.addRecipes(LabExperimentCategory.TYPE, LabExperimentCategory.all());
        registration.addRecipes(ChemFuelCategory.TYPE, ChemFuelCategory.all());
        registration.addRecipes(DeviceSteps.STILL, DeviceSteps.still());
        registration.addRecipes(DeviceSteps.TANK, DeviceSteps.tank());
        registration.addRecipes(DeviceSteps.SOAKING, DeviceSteps.soaking());
        registration.addRecipes(DeviceSteps.WRITING, DeviceSteps.writing());
        info(registration, ModItems.CRUCIBLE.get(), "crucible");
        info(registration, ModItems.CHEM_BURNER.get(), "chem_burner");
        info(registration, ModItems.CHEM_VIAL.get(), "chem_vial");
        info(registration, ModItems.RUSTED_PLATING.get(), "rusted_plating");
        info(registration, ModItems.SHIMMERBLOOM.get(), "shimmerbloom");
        info(registration, ModItems.AXOLOTL_TANK.get(), "axolotl_tank");
        info(registration, ModItems.REGENERATIVE_SLIME.get(), "regenerative_slime");
        info(registration, ModItems.CHEM_STILL.get(), "chem_still");
        info(registration, ModItems.LAB_BENCH.get(), "lab_bench");
        info(registration, ModItems.PINK_BRAZIER.get(), "pink_brazier");
        info(registration, ModItems.SYNTHETIC_CRYSTAL.get(), "synthetic_crystal");
        info(registration, ModItems.GATHERING_PYLON.get(), "gathering_pylon");
        info(registration, ModItems.SENDING_PYLON.get(), "sending_pylon");
        info(registration, ModItems.RELAY_PYLON.get(), "relay_pylon");
        info(registration, ModItems.SIPHON_PYLON.get(), "siphon_pylon");
        info(registration, ModItems.HEXTECH_SKYPACK.get(), "hextech_skypack");
        info(registration, ModItems.SORTING_MATRIX.get(), "sorting_matrix");
        info(registration, ModItems.WHIRL_REAPER.get(), "whirl_reaper");
        info(registration, ModItems.HEX_SAWMILL.get(), "hex_sawmill");
        info(registration, ModItems.HEXTECH_ALTAR.get(), "hextech_altar");
        info(registration, ModItems.PEDESTAL.get(), "pedestal");
        info(registration, ModItems.RUNE_STELE.get(), "rune_stele");
        info(registration, ModItems.HEXTECH_LAMP.get(), "hextech_lamp");
        info(registration, ModItems.MAGIC_STONE.get(), "magic_stone");
        info(registration, ModItems.MAGIC_SAPLING.get(), "magic_sapling");
        info(registration, ModItems.ALEMBIC.get(), "alembic");
        info(registration, ModItems.SEPARATOR.get(), "separator");
        info(registration, ModItems.ESSENCE_TANK.get(), "essence_tank");
        info(registration, ModItems.BRASS_PIPE.get(), "brass_pipe");
        info(registration, ModItems.PUMP.get(), "pump");
        info(registration, ModItems.DRAIN_VALVE.get(), "drain_valve");
        info(registration, ModItems.RESEARCH_TABLE.get(), "research_table");
        info(registration, ModItems.BLANK_SCROLL.get(), "blank_scroll");
        info(registration, ModItems.RUNE_SCROLL.get(), "rune_scroll");
        info(registration, ModItems.RESEARCH_NOTES.get(), "research_notes");
        registration.addItemStackInfo(new ItemStack(ModItems.ESSENCE_FLASK.get()), Component.translatable("jei.hextale.info.essence_flask"));
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addIngredientInfo(item, Component.translatable("jei.hextale.info." + key));
    }

    /** A click on the arrow of the workbench or the furnace opens their recipes. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(com.pockyl.hextale.client.screen.machine.HexWorkbenchScreen.class,
                com.pockyl.hextale.client.screen.machine.HexWorkbenchScreen.ARROW_X, com.pockyl.hextale.client.screen.machine.HexWorkbenchScreen.ARROW_Y,
                22, 10, HexCraftingCategory.TYPE);
        registration.addRecipeClickArea(com.pockyl.hextale.client.screen.machine.HexFurnaceScreen.class,
                com.pockyl.hextale.client.screen.machine.HexFurnaceScreen.ARROW_X, com.pockyl.hextale.client.screen.machine.HexFurnaceScreen.ARROW_Y,
                30, 10, AlloyingCategory.TYPE);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(BoilingCategory.TYPE, ModItems.ALEMBIC.get());
        registration.addCraftingStation(SeparationCategory.TYPE, ModItems.SEPARATOR.get());
        registration.addCraftingStation(InfusionCategory.TYPE, ModItems.HEXTECH_ALTAR.get(), ModItems.PEDESTAL.get(), ModItems.RUNE_STELE.get());
        registration.addCraftingStation(RuneBurningCategory.TYPE, ModItems.HEXTECH_ALTAR.get(), ModItems.RUNE_STELE.get());
        registration.addCraftingStation(HexCraftingCategory.TYPE, ModItems.HEX_WORKBENCH.get());
        registration.addCraftingStation(AlloyingCategory.TYPE, ModItems.HEX_FURNACE.get());
        registration.addCraftingStation(HextechWorkingCategory.TYPE, ModItems.HEX_LAPIDARY.get());
        registration.addCraftingStation(CrucibleCategory.TYPE, ModItems.CRUCIBLE.get());
        registration.addCraftingStation(BrewingCategory.TYPE, ModItems.CRUCIBLE.get(), ModItems.CHEM_BURNER.get());
        registration.addCraftingStation(LabExperimentCategory.TYPE, ModItems.LAB_BENCH.get());
        registration.addCraftingStation(ChemFuelCategory.TYPE, ModItems.CHEM_BURNER.get(), ModItems.PINK_BRAZIER.get());
        registration.addCraftingStation(DeviceSteps.STILL, ModItems.CHEM_STILL.get(), ModItems.CHEM_BURNER.get());
        registration.addCraftingStation(DeviceSteps.TANK, ModItems.AXOLOTL_TANK.get());
        registration.addCraftingStation(DeviceSteps.SOAKING, ModItems.DRAIN_VALVE.get());
        registration.addCraftingStation(DeviceSteps.WRITING, ModItems.HEXTECH_ALTAR.get(), ModItems.RUNE_STELE.get());
    }

    static ItemStack concentrate(Essence essence) {
        return EssenceFlaskItem.filled(EssenceMix.pure(essence, EssenceFlaskItem.CAPACITY));
    }
}
