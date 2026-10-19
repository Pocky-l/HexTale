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
    private static final List<String> RECIPES = List.of("brass_ingot_from_nuggets", "brass_nugget", "researchers_diary");
    /** Hex Workbench and Hex Furnace recipes, loaded by the mod itself. */
    private static final List<String> HEX_CRAFTING = List.of("research_table", "alembic", "hextech_altar", "hex_lapidary");
    private static final List<String> ALLOYS = List.of("brass_ingot", "hexsteel_ingot");
    private static final List<String> ADVANCEMENTS = List.of("root", "born_of_storm", "researchers_table", "first_rune");

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
        for (String recipe : HEX_CRAFTING) {
            helper.assertTrue(com.pockyl.hextale.crafting.HexCraftingRecipe.ALL.get(false).containsKey(Hextale.id(recipe)),
                    "hex crafting " + recipe + " is loaded");
        }
        for (String recipe : ALLOYS) {
            helper.assertTrue(com.pockyl.hextale.crafting.AlloyRecipe.ALL.get(false).containsKey(Hextale.id(recipe)), "alloy " + recipe + " is loaded");
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
