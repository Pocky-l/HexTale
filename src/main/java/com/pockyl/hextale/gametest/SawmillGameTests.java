package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.HexSawmillBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.farm.Felling;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

/** The Hex Sawmill: grown trees are felled whole and planted again, log walls are left alone. */
public final class SawmillGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private SawmillGameTests() {
    }

    private static void clear(GameTestHelper helper) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                helper.setBlock(BASE.offset(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 7; y++) {
                    helper.setBlock(BASE.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        AABB area = new AABB(helper.absolutePos(BASE)).inflate(8, 8, 8);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
    }

    /** A small oak: four logs on dirt, two rings of leaves and a top; grown leaves unless {@code placed}. */
    private static void oak(GameTestHelper helper, BlockPos foot, boolean placed) {
        helper.setBlock(foot.below(), Blocks.DIRT);
        for (int dy = 0; dy < 4; dy++) {
            helper.setBlock(foot.above(dy), Blocks.OAK_LOG);
        }
        for (int dy = 2; dy <= 3; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        helper.setBlock(foot.offset(dx, dy, dz), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, placed));
                    }
                }
            }
        }
        helper.setBlock(foot.above(4), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, placed));
    }

    private static int lying(GameTestHelper helper, Item item, BlockPos around) {
        AABB box = new AABB(helper.absolutePos(around)).inflate(2);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).stream().filter(e -> e.getItem().is(item))
                .mapToInt(e -> e.getItem().getCount()).sum();
    }

    /**
     * One grove (neighbouring tests stand a few blocks away): the grown oak is felled whole - no log or leaf left, its
     * four logs lie at its foot - while an oak of logs and placed leaves (a build) stands untouched; wind is spent.
     */
    @HexTest(maxTicks = 300)
    public static void theSawmillFellsGrownTreesOnly(GameTestHelper helper) {
        clear(helper);
        BlockPos tree = BASE.offset(3, 0, 1);
        BlockPos build = BASE.offset(-3, 0, 1);
        oak(helper, tree, false);
        oak(helper, build, true);
        BlockPos jarPos = BASE.offset(0, 0, -3);
        helper.setBlock(jarPos, ModBlocks.ESSENCE_TANK.get());
        helper.<EssenceTankBlockEntity>getBlockEntity(jarPos).tank().set(EssenceMix.pure(Essence.AIR, 4));
        helper.setBlock(BASE, ModBlocks.HEX_SAWMILL.get());
        helper.succeedWhen(() -> {
            for (int dy = 0; dy < 4; dy++) {
                helper.assertBlockNotPresent(Blocks.OAK_LOG, tree.above(dy));
            }
            helper.assertBlockNotPresent(Blocks.OAK_LEAVES, tree.above(4));
            helper.assertTrue(lying(helper, Items.OAK_LOG, tree) >= 4, "the logs lie at the foot");
            helper.assertBlockPresent(Blocks.OAK_LOG, build.above(3));
            helper.assertBlockPresent(Blocks.OAK_LEAVES, build.above(4));
            helper.assertTrue(helper.<HexSawmillBlockEntity>getBlockEntity(BASE).pending().isEmpty(), "the tree has landed");
            helper.assertTrue(helper.<EssenceTankBlockEntity>getBlockEntity(jarPos).tank().amount() < 4, "the saw drank wind");
        });
    }

    /** A felled tree's sapling is planted back on its foot and taken out of the drops; four on a big tree's four feet. */
    @HexTest
    public static void felledTreesArePlantedBack(GameTestHelper helper) {
        clear(helper);
        BlockPos foot = BASE.offset(2, 0, 2);
        helper.setBlock(foot.below(), Blocks.GRASS_BLOCK);
        List<ItemStack> drops = new ArrayList<>(List.of(new ItemStack(Items.OAK_LOG, 5), new ItemStack(Items.OAK_SAPLING, 2)));
        int planted = Felling.replant(helper.getLevel(), helper.absolutePos(foot), List.of(helper.absolutePos(foot)), drops);
        helper.assertTrue(planted == 1, "one sapling planted");
        helper.assertBlockPresent(Blocks.OAK_SAPLING, foot);
        helper.assertTrue(drops.stream().filter(s -> s.is(Items.OAK_SAPLING)).mapToInt(ItemStack::getCount).sum() == 1, "one sapling left");

        BlockPos big = BASE.offset(-3, 0, -3);
        List<BlockPos> feet = new ArrayList<>();
        for (BlockPos at : List.of(big, big.east(), big.south(), big.south().east())) {
            helper.setBlock(at.below(), Blocks.DIRT);
            feet.add(helper.absolutePos(at));
        }
        List<ItemStack> darkDrops = new ArrayList<>(List.of(new ItemStack(Items.DARK_OAK_SAPLING, 4)));
        helper.assertTrue(Felling.replant(helper.getLevel(), feet.getFirst(), feet, darkDrops) == 4, "four saplings for a big tree");
        helper.assertBlockPresent(Blocks.DARK_OAK_SAPLING, big.south().east());
        helper.succeed();
    }
}
