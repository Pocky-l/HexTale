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

    /** Insight: listening again and again keeps one glow over the block heard, never a pile of markers. */
    @HexTest(padding = 4)
    public static void forkInsightKeepsOneGlowPerBlock(GameTestHelper helper) {
        BlockPos ear = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos near = helper.absolutePos(new BlockPos(2, 1, 1));
        helper.getLevel().setBlockAndUpdate(near, ModBlocks.DORMANT_HEXTECH.get().defaultBlockState());
        ItemStack fork = new ItemStack(ModItems.TUNING_FORK.get());
        com.pockyl.hextale.runework.RuneHolder.FORK.set(fork,
                com.pockyl.hextale.runework.PickaxeRunes.EMPTY.with(com.pockyl.hextale.runework.ForkRune.INSIGHT.rune(), 1));
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.moveTo(ear.getX() + 0.5, ear.getY() - 1.0, ear.getZ() + 0.5, 0, 0);
        for (int i = 0; i < 3; i++) {
            TuningForkItem.listen(helper.getLevel(), player, fork);
        }
        List<net.minecraft.world.entity.Entity> markers = helper.getLevel().getEntities((net.minecraft.world.entity.Entity) null,
                new net.minecraft.world.phys.AABB(near).inflate(0.5), com.pockyl.hextale.runework.PickaxeEffects::isGlowMarker);
        helper.assertTrue(markers.size() == 1, "one glow over the heard block, got " + markers.size());
        markers.forEach(net.minecraft.world.entity.Entity::discard);
        helper.getLevel().setBlockAndUpdate(near, Blocks.AIR.defaultBlockState());
        helper.succeed();
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
