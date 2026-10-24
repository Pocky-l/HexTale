package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.block.CrucibleBlockEntity;
import com.pockyl.hextale.block.MagicSaplingBlock;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;

/**
 * The magic tree: a crucible holding magic, plant and sap turns a sapling into a Magic Tree Sprout, but only for a
 * researcher who has researched the tree; the sprout grows into a tree of magic logs and leaves none of which decay.
 */
public final class MagicTreeGameTests {
    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private MagicTreeGameTests() {
    }

    @HexTest(maxTicks = 700)
    public static void aSaplingBecomesASproutOnlyForItsResearcher(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.LAVA);
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        boolean[] done = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (done[0]) {
                return;
            }
            crucible.pour(EssenceMix.pure(Essence.MAGIC, 8).plus(EssenceMix.pure(Essence.PLANT, 6)).plus(EssenceMix.pure(Essence.SAP, 4)));
            helper.assertTrue(crucible.take(new ItemStack(Items.OAK_SAPLING), 1, true, player) == 1, "the sapling goes in");
            helper.assertTrue(crucible.rising().isEmpty() && crucible.essence().get(Essence.MAGIC) == 8,
                    "without the research it only dissolves");
            Research.research(player, ResearchTopics.MAGIC_TREE);
            helper.assertTrue(crucible.take(new ItemStack(Items.BIRCH_SAPLING), 1, true, player) == 1, "the second sapling goes in");
            helper.assertTrue(crucible.essence().get(Essence.MAGIC) == 0 && crucible.essence().get(Essence.SAP) == 0,
                    "it drinks the magic and the sap");
            helper.assertTrue(crucible.rising().size() == 1 && crucible.rising().getFirst().is(ModItems.MAGIC_SAPLING.get()),
                    "a Magic Tree Sprout rises");
            done[0] = true;
        });
    }

    @HexTest(maxTicks = 40)
    public static void theSproutGrowsATreeThatKeepsItsLeaves(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.GRASS_BLOCK);
        BlockState sprout = ModBlocks.MAGIC_SAPLING.get().defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        helper.setBlock(AT, sprout);
        BlockPos at = helper.absolutePos(AT);
        ((MagicSaplingBlock) sprout.getBlock()).advanceTree(helper.getLevel(), at, sprout, helper.getLevel().getRandom());
        helper.assertBlockPresent(ModBlocks.MAGIC_LOG.get(), AT);
        int leaves = 0;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-9, 0, -9), at.offset(9, 16, 9))) {
            BlockState state = helper.getLevel().getBlockState(pos);
            if (state.is(ModBlocks.MAGIC_LEAVES.get())) {
                leaves++;
                helper.assertTrue(state.getValue(LeavesBlock.DISTANCE) < LeavesBlock.DECAY_DISTANCE, "no leaf of the tree decays");
            }
        }
        helper.assertTrue(leaves >= 40, "the tree has a crown (" + leaves + " leaves)");
        helper.succeed();
    }
}
