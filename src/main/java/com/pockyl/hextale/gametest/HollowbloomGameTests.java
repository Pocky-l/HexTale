package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootTable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.event.BlockAwakening;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.thevoid.HollowbloomBlock;
import com.pockyl.hextale.thevoid.Thinning;

/** The Hollowbloom wakes from a dead bush and follows the Thinning; the creatures of the Void leave chitin. */
public final class HollowbloomGameTests {
    private static final BlockPos AT = new BlockPos(0, 1, 0);

    private HollowbloomGameTests() {
    }

    @HexTest(maxTicks = 80)
    public static void dustWakesADeadBush(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SAND);
        helper.setBlock(AT, Blocks.DEAD_BUSH);
        BlockAwakening.start(helper.getLevel(), helper.absolutePos(AT), Direction.SOUTH, null, false);
        helper.succeedWhen(() -> helper.assertBlockPresent(ModBlocks.HOLLOWBLOOM.get(), AT));
    }

    @HexTest
    public static void itLivesAsTheWorldThins(GameTestHelper helper) {
        helper.assertTrue(HollowbloomBlock.vigorFor(0) == 0 && HollowbloomBlock.vigorFor(149) == 0, "clean land leaves it withered");
        helper.assertTrue(HollowbloomBlock.vigorFor(150) == 1, "it stirs long before the land is felt");
        helper.assertTrue(HollowbloomBlock.vigorFor(Thinning.SAFE - 1) == 2, "and leafs out before the haze");
        helper.assertTrue(HollowbloomBlock.vigorFor(Thinning.Stage.HAZE.from) == 3, "it blooms with the haze");
        helper.assertTrue(HollowbloomBlock.vigorFor(Thinning.MAX) == 4, "and glows with the blight");
        helper.assertTrue(HollowbloomBlock.signal(0) == 0 && HollowbloomBlock.signal(1) == 1 && HollowbloomBlock.signal(Thinning.MAX) == 15,
                "a comparator reads 0 to 15");

        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        LevelChunk chunk = level.getChunkAt(pos);
        helper.setBlock(AT.below(), Blocks.SAND);
        helper.setBlock(AT, ModBlocks.HOLLOWBLOOM.get());
        HollowbloomBlock bloom = ModBlocks.HOLLOWBLOOM.get();
        Thinning.set(level, chunk, Thinning.Stage.BLIGHT.from);
        BlockState state = level.getBlockState(pos);
        for (int step = 1; step <= 4; step++) {
            state = bloom.sense(state, level, pos);
            helper.assertTrue(state.getValue(HollowbloomBlock.VIGOR) == step, "it grows one step at a time, step " + step);
        }
        helper.assertTrue(bloom.sense(state, level, pos) == state, "and stays at full bloom");
        Thinning.set(level, chunk, 0);
        state = bloom.sense(state, level, pos);
        helper.assertTrue(state.getValue(HollowbloomBlock.VIGOR) == 3, "on clean land it withers one step at a time");
        helper.assertTrue(level.getBlockState(pos) == state, "the world holds what it sensed");
        helper.succeed();
    }

    @HexTest
    public static void voidCreaturesLeaveChitin(GameTestHelper helper) {
        for (String entity : new String[] {"voidling", "void_mite"}) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, Hextale.id("entities/" + entity));
            LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
            helper.assertTrue(table != LootTable.EMPTY, "the " + entity + " has its loot");
        }
        helper.assertTrue(ModEntities.VOIDLING.get().getDefaultLootTable().equals(
                ResourceKey.create(Registries.LOOT_TABLE, Hextale.id("entities/voidling"))), "the voidling uses it");
        helper.succeed();
    }
}
