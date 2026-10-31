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
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation UID = Hextale.id("jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    /** Bottles of different essences are different ingredients. */
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        // The diary's step pictures are no items anyone can have.
        runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                java.util.List.of(new net.minecraft.world.item.ItemStack(ModItems.GUIDE_MARK.get())));
        // The essence fluid without a kind is no essence at all: only the 22 pure ones are shown.
        runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK,
                java.util.List.of(new net.neoforged.neoforge.fluids.FluidStack(com.pockyl.hextale.registry.ModFluids.ESSENCE.get(),
                        net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME)));
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        com.pockyl.hextale.port.PortJei.registerFromDataComponentTypes(registration, ModItems.ESSENCE_FLASK.get(), ModDataComponents.ESSENCE.get());
        com.pockyl.hextale.port.PortJei.registerFromDataComponentTypes(registration, ModItems.ESSENCE.get(), ModDataComponents.ESSENCE_KIND.get());
        com.pockyl.hextale.port.PortJei.registerFromDataComponentTypes(registration, ModItems.CHEM_VIAL.get(), ModDataComponents.BREW.get());
    }

    /** The essence fluid of each essence is its own ingredient. */
    @Override
    public <T> void registerFluidSubtypes(ISubtypeRegistration registration, mezz.jei.api.helpers.IPlatformFluidHelper<T> platformFluidHelper) {
        registration.registerSubtypeInterpreter(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, com.pockyl.hextale.registry.ModFluids.ESSENCE.get(),
                new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<net.neoforged.neoforge.fluids.FluidStack>() {
                    @Override
                    public Object getSubtypeData(net.neoforged.neoforge.fluids.FluidStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                        return com.pockyl.hextale.essence.EssenceFluid.kind(stack);
                    }

                    @Override
                    public String getLegacyStringSubtypeInfo(net.neoforged.neoforge.fluids.FluidStack stack,
                                                             mezz.jei.api.ingredients.subtypes.UidContext context) {
                        com.pockyl.hextale.rune.Essence kind = com.pockyl.hextale.essence.EssenceFluid.kind(stack);
                        return kind == null ? "" : kind.name();
                    }
                });
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
        // Pure essence as a fluid, as other mods' tanks hold it.
        List<net.neoforged.neoforge.fluids.FluidStack> fluids = new ArrayList<>();
        for (Essence essence : Essence.values()) {
            fluids.add(com.pockyl.hextale.essence.EssenceFluid.stack(essence, net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME));
        }
        registration.addExtraIngredients(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, fluids);
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
                new ChemFuelCategory(registration.getJeiHelpers().getGuiHelper()),
                new MultiblockCategory(registration.getJeiHelpers().getGuiHelper()));
        for (StepCategory category : DeviceSteps.categories(registration.getJeiHelpers().getGuiHelper())) {
            registration.addRecipeCategories(category);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // The stacks JEI already shows, one per item: a bare default stack of another mod's item may break that mod's
        // subtype interpreter, and JEI then drops the whole recipe (PackagedAuto's package, GitHub issue #4).
        List<ItemStack> boilable = new ArrayList<>();
        java.util.Set<net.minecraft.world.item.Item> seen = new java.util.HashSet<>();
        for (ItemStack stack : registration.getIngredientManager().getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK)) {
            if (ResearchItems.of(stack) != null && !EssenceFlaskItem.isFlask(stack) && seen.add(stack.getItem())) {
                boilable.add(stack);
            }
        }
        registration.addRecipes(BoilingCategory.TYPE, List.of(new BoilingCategory.Recipe(boilable)));
        List<SeparationCategory.Recipe> separations = new ArrayList<>();
        for (Essence essence : Essence.values()) {
            separations.add(new SeparationCategory.Recipe(essence, com.pockyl.hextale.registry.ModTags.stacks(com.pockyl.hextale.registry.ModTags.SEPARATOR_FUELS)));
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
        info(registration, ModItems.DORMANT_HEXTECH.get(), "dormant_hextech");
        info(registration, ModItems.FULGURITE.get(), "fulgurite");
        info(registration, ModItems.TUNING_FORK.get(), "tuning_fork");
        info(registration, ModItems.DUST_CHARGE.get(), "dust_charge");
        info(registration, ModItems.LEVITATION_PLATE.get(), "levitation_plate");
        info(registration, ModItems.FLOATING_STONE.get(), "floating_stone");
        info(registration, ModItems.NULL_LANTERN.get(), "null_lantern");
        info(registration, ModItems.IMPULSE_PLATE.get(), "impulse_plate");
        info(registration, ModItems.RIFT_GATE.get(), "rift_gate");
        info(registration, ModItems.RIFT_LINK_CRYSTAL.get(), "rift_link_crystal");
        info(registration, ModItems.WORLD_ANCHOR.get(), "world_anchor");
        info(registration, ModItems.DISINTEGRATOR.get(), "disintegrator");
        info(registration, ModItems.RECALL_CHARM.get(), "recall_charm");
        info(registration, ModItems.GRAPPLE_GAUNTLET.get(), "grapple_gauntlet");
        info(registration, ModItems.SOUL_FLASK.get(), "soul_flask");
        info(registration, ModItems.SPAWNER_CAGE.get(), "spawner_cage");
        info(registration, ModItems.WARD_GLASS.get(), "ward_glass");
        info(registration, ModItems.VEIL_GLASS.get(), "veil_glass");
        info(registration, ModItems.HEX_FURNACE.get(), "hex_furnace");
        info(registration, ModItems.HEX_FOUNDRY.get(), "hex_foundry");
        registration.addRecipes(BrewingCategory.TYPE, com.pockyl.hextale.crafting.BrewRecipe.ALL.get(true).entrySet().stream()
                .map(entry -> new BrewingCategory.Recipe(entry.getKey(), entry.getValue())).toList());
        registration.addRecipes(LabExperimentCategory.TYPE, LabExperimentCategory.all());
        registration.addRecipes(ChemFuelCategory.TYPE, ChemFuelCategory.all());
        registration.addRecipes(MultiblockCategory.TYPE, MultiblockCategory.all());
        registration.addRecipes(DeviceSteps.STILL, DeviceSteps.still());
        registration.addRecipes(DeviceSteps.TANK, DeviceSteps.tank());
        registration.addRecipes(DeviceSteps.SOAKING, DeviceSteps.soaking());
        registration.addRecipes(DeviceSteps.WRITING, DeviceSteps.writing());
        info(registration, ModItems.CRUCIBLE.get(), "crucible");
        info(registration, ModItems.CHEM_BURNER.get(), "chem_burner");
        info(registration, ModItems.CHEM_VIAL.get(), "chem_vial");
        info(registration, ModItems.RUSTED_PLATING.get(), "rusted_plating");
        info(registration, ModItems.SHIMMERBLOOM.get(), "shimmerbloom");
        info(registration, ModItems.HOLLOWBLOOM.get(), "hollowbloom");
        info(registration, ModItems.VOID_CHITIN.get(), "void_chitin");
        info(registration, ModItems.CLOUDED_CRYSTAL.get(), "clouded_crystal");
        info(registration, ModItems.BREACH_HEART.get(), "breach_heart");
        info(registration, ModItems.HEXTECH_PURIFIER.get(), "hextech_purifier");
        info(registration, ModItems.VOID_ANCHOR.get(), "void_anchor");
        info(registration, ModItems.AXOLOTL_TANK.get(), "axolotl_tank");
        info(registration, ModItems.REGENERATIVE_SLIME.get(), "regenerative_slime");
        info(registration, ModItems.CHEM_STILL.get(), "chem_still");
        info(registration, ModItems.CHEM_BOILER.get(), "chem_boiler");
        info(registration, ModItems.HEXTECH_LAMPPOST.get(), "hextech_lamppost");
        info(registration, ModItems.LAB_BENCH.get(), "lab_bench");
        info(registration, ModItems.PINK_BRAZIER.get(), "pink_brazier");
        info(registration, ModItems.SYNTHETIC_CRYSTAL.get(), "synthetic_crystal");
        info(registration, ModItems.GATHERING_PYLON.get(), "gathering_pylon");
        info(registration, ModItems.SENDING_PYLON.get(), "sending_pylon");
        info(registration, ModItems.RELAY_PYLON.get(), "relay_pylon");
        info(registration, ModItems.SIPHON_PYLON.get(), "siphon_pylon");
        info(registration, ModItems.HEXTECH_SKYPACK.get(), "hextech_skypack");
        info(registration, ModItems.SORTING_MATRIX.get(), "sorting_matrix");
        info(registration, ModItems.HEXCORE.get(), "hexcore");
        info(registration, ModItems.WHIRL_REAPER.get(), "whirl_reaper");
        info(registration, ModItems.HEX_SAWMILL.get(), "hex_sawmill");
        info(registration, ModItems.GREEDY_FUNNEL.get(), "greedy_funnel");
        info(registration, ModItems.ARC_SPIRE.get(), "arc_spire");
        info(registration, ModItems.BINDING_CHALK.get(), "binding_chalk");
        info(registration, ModItems.XP_CONDENSER.get(), "xp_condenser");
        info(registration, ModItems.MIASMA_VENT.get(), "miasma_vent");
        info(registration, ModItems.HEXTECH_ALTAR.get(), "hextech_altar");
        info(registration, ModItems.PEDESTAL.get(), "pedestal");
        info(registration, ModItems.RUNE_STELE.get(), "rune_stele");
        info(registration, ModItems.RUNE_CIRCLE.get(), "rune_circle");
        info(registration, ModItems.HEXTECH_LAMP.get(), "hextech_lamp");
        info(registration, ModItems.RUNE_CHALK.get(), "rune_chalk");
        info(registration, ModItems.NULLSTONE.get(), "nullstone");
        info(registration, ModItems.CRYSTAL_BATTERY.get(), "crystal_battery");
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
        registration.addRecipeClickArea(com.pockyl.hextale.client.screen.machine.HexFoundryScreen.class,
                com.pockyl.hextale.client.screen.machine.HexFoundryScreen.ARROW_X, com.pockyl.hextale.client.screen.machine.HexFoundryScreen.ARROW_Y,
                28, 10, mezz.jei.api.constants.RecipeTypes.SMELTING);
    }

    @Override
    public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        // Hex Workbench: grid slots 0-8, then the crystal and the result, then the player's 36 slots.
        registration.addRecipeTransferHandler(com.pockyl.hextale.menu.HexWorkbenchMenu.class, com.pockyl.hextale.registry.ModMenus.HEX_WORKBENCH.get(),
                HexCraftingCategory.TYPE, 0, 9, 11, 36);
        registration.addRecipeTransferHandler(com.pockyl.hextale.menu.HexWorkbenchMenu.class, com.pockyl.hextale.registry.ModMenus.HEX_WORKBENCH.get(),
                mezz.jei.api.constants.RecipeTypes.CRAFTING, 0, 9, 11, 36);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(BoilingCategory.TYPE, ModItems.ALEMBIC.get());
        registration.addRecipeCatalysts(SeparationCategory.TYPE, ModItems.SEPARATOR.get());
        registration.addRecipeCatalysts(InfusionCategory.TYPE, ModItems.HEXTECH_ALTAR.get(), ModItems.HEXCORE.get(), ModItems.PEDESTAL.get());
        registration.addRecipeCatalysts(RuneBurningCategory.TYPE, ModItems.RUNE_CIRCLE.get(), ModItems.RUNE_STELE.get());
        registration.addRecipeCatalysts(HexCraftingCategory.TYPE, ModItems.HEX_WORKBENCH.get());
        // Without a hex recipe the workbench crafts like a table and the furnace smelts like a furnace.
        registration.addRecipeCatalysts(mezz.jei.api.constants.RecipeTypes.CRAFTING, ModItems.HEX_WORKBENCH.get());
        registration.addRecipeCatalysts(AlloyingCategory.TYPE, ModItems.HEX_FURNACE.get());
        registration.addRecipeCatalysts(AlloyingCategory.TYPE, ModItems.HEX_FOUNDRY.get());
        registration.addRecipeCatalysts(mezz.jei.api.constants.RecipeTypes.SMELTING, ModItems.HEX_FURNACE.get());
        registration.addRecipeCatalysts(mezz.jei.api.constants.RecipeTypes.SMELTING, ModItems.HEX_FOUNDRY.get());
        registration.addRecipeCatalysts(HextechWorkingCategory.TYPE, ModItems.HEX_LAPIDARY.get());
        registration.addRecipeCatalysts(CrucibleCategory.TYPE, ModItems.CRUCIBLE.get());
        registration.addRecipeCatalysts(BrewingCategory.TYPE, ModItems.CRUCIBLE.get(), ModItems.CHEM_BURNER.get());
        registration.addRecipeCatalysts(LabExperimentCategory.TYPE, ModItems.LAB_BENCH.get());
        registration.addRecipeCatalysts(MultiblockCategory.TYPE, ModItems.HEXTECH_DUST.get());
        registration.addRecipeCatalysts(ChemFuelCategory.TYPE, ModItems.CHEM_BURNER.get(), ModItems.PINK_BRAZIER.get());
        registration.addRecipeCatalysts(DeviceSteps.STILL, ModItems.CHEM_STILL.get(), ModItems.CHEM_BURNER.get());
        registration.addRecipeCatalysts(DeviceSteps.TANK, ModItems.AXOLOTL_TANK.get());
        registration.addRecipeCatalysts(DeviceSteps.SOAKING, ModItems.DRAIN_VALVE.get());
        registration.addRecipeCatalysts(DeviceSteps.WRITING, ModItems.RUNE_CIRCLE.get(), ModItems.RUNE_STELE.get());
    }

    static ItemStack concentrate(Essence essence) {
        return EssenceFlaskItem.filled(EssenceMix.pure(essence, EssenceFlaskItem.CAPACITY));
    }
}
