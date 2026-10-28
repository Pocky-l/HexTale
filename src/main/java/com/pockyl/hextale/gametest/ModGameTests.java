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

    /**
     * A device whose model spreads past its cell gets stand-ins in the empty cells it reaches: they outline the device,
     * take a click and the mining to it and go together with it.
     */
    @HexTest(padding = 3)
    public static void spreadingDevicesGetStandIns(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        net.minecraft.core.BlockPos altar = helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        for (net.minecraft.core.BlockPos cell : net.minecraft.core.BlockPos.betweenClosed(altar.offset(-1, 0, -1), altar.offset(1, 1, 1))) {
            level.setBlock(cell, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(altar, com.pockyl.hextale.registry.ModBlocks.HEXTECH_ALTAR.get().defaultBlockState(), 3);
        com.pockyl.hextale.block.ShapeParts.refresh(level, altar, null);
        net.minecraft.core.BlockPos east = altar.east();
        net.minecraft.world.level.block.state.BlockState part = level.getBlockState(east);
        helper.assertTrue(part.getBlock() instanceof com.pockyl.hextale.block.ShapePartBlock, "the dais spreads into the cell east");
        helper.assertTrue(level.getBlockState(altar.above()).getBlock() instanceof com.pockyl.hextale.block.ShapePartBlock,
                "the prongs reach into the cell above");
        helper.assertTrue(level.getBlockState(altar.north().west()).getBlock() instanceof com.pockyl.hextale.block.ShapePartBlock,
                "and the dais into the corner cells");
        helper.assertTrue(com.pockyl.hextale.block.ShapePartBlock.device(part, east).equals(altar), "a part knows its device");
        helper.assertTrue(part.getShape(level, east).bounds().equals(level.getBlockState(altar).getShape(level, altar).bounds().move(-1, 0, 0)),
                "a part outlines the whole device");
        net.minecraft.server.level.ServerPlayer player = GameTests.watcher(helper, altar);
        player.gameMode.destroyBlock(east);
        helper.assertTrue(level.getBlockState(altar).isAir(), "mining a part mines the device");
        // The parts follow a tick at a time, quietly.
        helper.succeedWhen(() -> helper.assertTrue(level.getBlockState(east).isAir() && level.getBlockState(altar.above()).isAir()
                && level.getBlockState(altar.north().west()).isAir() && level.getBlockState(altar.above().south().east()).isAir(),
                "the parts go with the device"));
    }

    /** Every block of ours that is meant to drop something has its loot table: a missing one deletes the block when mined. */
    @HexTest
    public static void everyBlockHasItsLoot(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        List<String> missing = new java.util.ArrayList<>();
        for (var entry : com.pockyl.hextale.registry.ModBlocks.BLOCKS.getEntries()) {
            var key = entry.get().getLootTable();
            if (key.isPresent() && server.reloadableRegistries().getLootTable(key.get()) == LootTable.EMPTY) {
                missing.add(entry.getId().getPath());
            }
        }
        helper.assertTrue(missing.isEmpty(), "blocks without a loot table (add one, or noLootTable() if on purpose): " + missing);
        helper.succeed();
    }

    /**
     * Since 26.x a block stands in the world's heightmaps (spawning, the blight's and the breach's search for the ground)
     * only through the tag {@code minecraft:blocks_motion_no_leaves}: every block of ours that has a body must be in it.
     */
    @HexTest
    public static void solidBlocksStandInTheHeightmap(GameTestHelper helper) {
        List<String> missing = new java.util.ArrayList<>();
        for (net.minecraft.world.level.block.Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            net.minecraft.resources.Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            net.minecraft.world.level.block.state.BlockState state = block.defaultBlockState();
            // Pots and carpets stand outside the tag in vanilla too.
            if (id.getNamespace().equals(Hextale.MOD_ID) && !id.getPath().startsWith("potted_") && !id.getPath().endsWith("_carpet")
                    && !state.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,
                    net.minecraft.core.BlockPos.ZERO).isEmpty() && !state.is(net.minecraft.tags.BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP)) {
                missing.add(id.toString());
            }
        }
        java.util.Collections.sort(missing);
        helper.assertTrue(missing.isEmpty(), "not in minecraft:blocks_motion_no_leaves: " + String.join(" ", missing));
        helper.succeed();
    }

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
            helper.assertTrue((com.pockyl.hextale.crafting.HexCraftingRecipe.ALL.named(Hextale.id(recipe), false) != null),
                    "hex crafting " + recipe + " is loaded");
        }
        for (String recipe : ALLOYS) {
            helper.assertTrue((com.pockyl.hextale.crafting.AlloyRecipe.ALL.named(Hextale.id(recipe), false) != null), "alloy " + recipe + " is loaded");
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

    /**
     * Every item and block id the mod has released still resolves (registered, or an alias of its new name), so packs,
     * quests and worlds never lose one. The list is docs/released-ids.txt (path from the run's system property).
     */
    @HexTest
    public static void releasedIdsStillResolve(GameTestHelper helper) throws java.io.IOException {
        String file = System.getProperty("hextale.releasedIds");
        if (file == null) {
            helper.succeed();
            return;
        }
        for (String line : java.nio.file.Files.readAllLines(java.nio.file.Path.of(file))) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.trim().split(" ");
            net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.parse(parts[1]);
            boolean found = switch (parts[0]) {
                case "item" -> net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id).isPresent();
                case "block" -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).isPresent();
                default -> false;
            };
            helper.assertTrue(found, line + " still resolves (or has an alias in ModAliases)");
        }
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
