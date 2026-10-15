package com.pockyl.hextale.gametest;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.loot.LootTable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.RuneGlyphs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * In-game tests, run headless by {@code gradlew runGameTestServer}.
 * Tests use the 1x1x1 {@code empty} structure unless they need a prepared one.
 */
public final class ModGameTests {
    private static final List<String> RECIPES = List.of("brass_ingot", "brass_ingot_from_nuggets", "brass_nugget", "brass_scraper",
            "fork_quill", "mortar", "parchment_stretcher", "research_table", "researchers_diary", "researchers_lens");
    private static final List<String> ADVANCEMENTS = List.of("root", "born_of_storm", "researchers_table", "first_rune", "milestone_3",
            "milestone_12", "milestone_24", "milestone_36", "milestone_54", "milestone_72");

    private ModGameTests() {
    }

    @HexTest
    public static void modLoads(GameTestHelper helper) {
        helper.succeed();
    }

    /** Every recipe, advancement and loot table of the mod parses in the current data formats. */
    @HexTest
    public static void dataFilesLoad(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (String recipe : RECIPES) {
            helper.assertTrue(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Hextale.id(recipe))).isPresent(),
                    "recipe " + recipe + " is loaded");
            helper.assertTrue(server.getAdvancements().get(Hextale.id("recipes/misc/" + recipe)) != null,
                    "the advancement unlocking " + recipe + " is loaded");
        }
        for (String advancement : ADVANCEMENTS) {
            helper.assertTrue(server.getAdvancements().get(Hextale.id("research/" + advancement)) != null,
                    "advancement " + advancement + " is loaded");
        }
        LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                Hextale.id("blocks/research_table")));
        helper.assertTrue(table != LootTable.EMPTY, "the research table drops itself");
        helper.succeed();
    }

    /** A tool recipe takes the scroll of its own rune only (a NeoForge components ingredient). */
    @HexTest
    public static void toolRecipesWantTheirRune(GameTestHelper helper) {
        RecipeHolder<?> holder = helper.getLevel().getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, Hextale.id("mortar"))).orElseThrow();
        @SuppressWarnings("unchecked")
        Recipe<CraftingInput> mortar = (Recipe<CraftingInput>) holder.value();
        ItemStack right = RuneScrollItem.of(ComplexRunes.byKey("water_earth"), 1, ModItems.RUNE_SCROLL.get());
        ItemStack wrong = RuneScrollItem.of(ComplexRunes.byKey("fire_earth"), 3, ModItems.RUNE_SCROLL.get());
        CraftingInput good = CraftingInput.of(3, 1, List.of(right, new ItemStack(Items.BOWL), new ItemStack(Items.FLINT)));
        CraftingInput bad = CraftingInput.of(3, 1, List.of(wrong, new ItemStack(Items.BOWL), new ItemStack(Items.FLINT)));
        helper.assertTrue(mortar.matches(good, helper.getLevel()), "the mortar is made from the scroll of its rune");
        helper.assertFalse(mortar.matches(bad, helper.getLevel()), "another rune's scroll does not make a mortar");
        helper.assertTrue(mortar.assemble(good).is(ModItems.MORTAR.get()), "the recipe makes a mortar");
        helper.succeed();
    }

    @HexTest
    public static void seventyTwoDistinctRunes(GameTestHelper helper) {
        helper.assertTrue(ComplexRunes.all().size() == ComplexRunes.COUNT, "72 complex runes");
        Set<String> keys = new HashSet<>();
        for (ComplexRune rune : ComplexRunes.all()) {
            helper.assertTrue(rune.action() != rune.target(), rune.key() + " uses two different elements");
            helper.assertTrue(keys.add(rune.key()), rune.key() + " is unique");
            helper.assertTrue(ComplexRunes.byKey(rune.key()) == rune, rune.key() + " is found by key");
            helper.assertTrue(rune.reversed() != rune && rune.reversed().reversed() == rune, rune.key() + " reversed is another rune");
            helper.assertFalse(RuneGlyphs.glyph(rune).isEmpty(), rune.key() + " has a hieroglyph");
        }
        helper.succeed();
    }
}
