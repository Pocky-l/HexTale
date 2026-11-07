package com.pockyl.hextale.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootTable;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loot tables: 26.3 reads {@code condition} and {@code modifier} and silently drops the old {@code conditions} and
 * {@code functions}, so a table in the old form still loads and simply ignores them.
 */
public final class LootTableGameTests {
    private static final Set<String> OLD_FIELDS = Set.of("conditions", "functions", "function");
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    private LootTableGameTests() {
    }

    /** Every table of the mod loads, and none holds a field the game no longer reads. */
    @HexTest
    public static void everyLootTableIsInTheCurrentFormat(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Map<Identifier, Resource> tables = server.getResourceManager().listResources("loot_table",
                id -> id.getNamespace().equals(Hextale.MOD_ID) && id.getPath().endsWith(".json"));
        helper.assertTrue(tables.size() > 200, "the tables are found, " + tables.size());
        tables.forEach((file, resource) -> {
            try (Reader reader = resource.openAsReader()) {
                String old = oldField(JsonParser.parseReader(reader));
                helper.assertTrue(old == null, file + " still has " + old);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            String path = file.getPath().substring("loot_table/".length(), file.getPath().length() - ".json".length());
            LootTable table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Hextale.id(path)));
            helper.assertTrue(table != LootTable.EMPTY, file + " does not load");
        });
        helper.succeed();
    }

    private static String oldField(JsonElement json) {
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                String old = oldField(element);
                if (old != null) {
                    return old;
                }
            }
        } else if (json.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                String old = OLD_FIELDS.contains(entry.getKey()) ? entry.getKey() : oldField(entry.getValue());
                if (old != null) {
                    return old;
                }
            }
        }
        return null;
    }

    private static int count(GameTestHelper helper, BlockState state, ItemStack tool, Item item) {
        List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(AT), null, null, tool);
        return drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    /** Conditions and functions that were ignored until the tables were rewritten: pods, ripe grain, shears. */
    @HexTest
    public static void dropsFollowTheirConditions(GameTestHelper helper) {
        BlockState cane = ModBlocks.CRYSTAL_CANE.get().defaultBlockState();
        BooleanProperty podded = (BooleanProperty) cane.getBlock().getStateDefinition().getProperty("podded");
        helper.assertTrue(count(helper, cane.setValue(podded, false), ItemStack.EMPTY, ModItems.CRYSTAL_POD.get()) == 0, "no pod on a bare cane");
        helper.assertTrue(count(helper, cane.setValue(podded, true), ItemStack.EMPTY, ModItems.CRYSTAL_POD.get()) == 1, "a podded cane gives its pod");

        BlockState wheat = ModBlocks.EMBER_WHEAT.get().defaultBlockState();
        IntegerProperty age = (IntegerProperty) wheat.getBlock().getStateDefinition().getProperty("age");
        BlockState ripe = wheat.setValue(age, 3);
        helper.assertTrue(count(helper, wheat.setValue(age, 0), ItemStack.EMPTY, ModItems.EMBER_WHEAT.get()) == 0, "unripe wheat gives no grain");
        helper.assertTrue(count(helper, wheat.setValue(age, 0), ItemStack.EMPTY, ModItems.EMBER_WHEAT_SEEDS.get()) == 1, "only its seed back");
        helper.assertTrue(count(helper, ripe, ItemStack.EMPTY, ModItems.EMBER_WHEAT.get()) == 1, "ripe wheat gives grain");

        BlockState curtain = ModBlocks.SINEW_CURTAIN.get().defaultBlockState();
        helper.assertTrue(count(helper, curtain, ItemStack.EMPTY, ModItems.SINEW_CURTAIN.get()) == 0, "by hand a curtain gives nothing");
        helper.assertTrue(count(helper, curtain, new ItemStack(Items.SHEARS), ModItems.SINEW_CURTAIN.get()) == 1, "shears take it");
        helper.succeed();
    }
}
