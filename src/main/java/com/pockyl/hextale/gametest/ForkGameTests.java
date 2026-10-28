package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import com.pockyl.hextale.item.TuningForkItem;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;

import java.util.List;

/** The Tuning Fork hears the nearest hextech, or the block it was struck against, and ores only within its ore reach. */
public final class ForkGameTests {
    private ForkGameTests() {
    }

    @HexTest(padding = 4)
    public static void theForkHearsTheNearestHextech(GameTestHelper helper) {
        BlockPos ear = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos near = helper.absolutePos(new BlockPos(3, 2, 1));
        BlockPos far = helper.absolutePos(new BlockPos(1, 2, 4));
        BlockPos ore = helper.absolutePos(new BlockPos(2, 2, 1));
        helper.getLevel().setBlockAndUpdate(near, ModBlocks.DORMANT_HEXTECH.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(far, ModBlocks.HEXTECH_CLUSTER.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(ore, Blocks.IRON_ORE.defaultBlockState());
        ItemStack fork = new ItemStack(ModItems.TUNING_FORK.get());

        List<BlockPos> heard = TuningForkItem.hear(helper.getLevel(), ear, 16, TuningForkItem.listensFor(fork), 1, 0);
        helper.assertTrue(heard.equals(List.of(near)), "untuned it hears the nearest hextech and not the ore: " + heard);
        heard = TuningForkItem.hear(helper.getLevel(), ear, 16, TuningForkItem.listensFor(fork), 2, 0);
        helper.assertTrue(heard.equals(List.of(near, far)), "with two voices it hears both, nearest first: " + heard);
        heard = TuningForkItem.hear(helper.getLevel(), ear, 16, TuningForkItem.listensFor(fork), 1, 8);
        helper.assertTrue(heard.equals(List.of(ore)), "within its ore reach it hears the ore too: " + heard);

        fork.set(ModDataComponents.TUNED_BLOCK.get(), Blocks.IRON_ORE);
        heard = TuningForkItem.hear(helper.getLevel(), ear, 16, TuningForkItem.listensFor(fork), 3, 0);
        helper.assertTrue(heard.equals(List.of(ore)), "tuned to iron ore it hears only that: " + heard);
        heard = TuningForkItem.hear(helper.getLevel(), ear, 0, TuningForkItem.listensFor(fork), 3, 0);
        helper.assertTrue(heard.isEmpty(), "nothing beyond its reach");
        for (BlockPos pos : List.of(near, far, ore)) {
            helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }
}
