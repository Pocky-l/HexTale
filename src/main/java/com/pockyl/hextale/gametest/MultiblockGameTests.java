package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import com.pockyl.hextale.block.BigEssenceJarBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.multiblock.MultiblockPartBlockEntity;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

/** Multiblocks: the Big Essence Jar is found, wakes with a hextech crystal, holds sixteen jars and falls apart when broken. */
public final class MultiblockGameTests {
    /** The jar's south-west bottom corner (relative). */
    private static final BlockPos CORNER = new BlockPos(2, 1, 2);

    private MultiblockGameTests() {
    }

    private static void build(GameTestHelper helper, boolean hollow) {
        for (int y = 0; y < 5; y++) {
            for (int x = -1; x < 4; x++) {
                for (int z = -1; z < 4; z++) {
                    helper.setBlock(CORNER.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    boolean middle = x == 1 && z == 1 && (y == 1 || y == 2);
                    if (middle && hollow) {
                        continue;
                    }
                    helper.setBlock(CORNER.offset(x, y, z), y == 3 ? Blocks.OAK_SLAB : Blocks.GLASS);
                }
            }
        }
    }

    @HexTest
    public static void theBigJarWakesUpAndFallsApart(GameTestHelper helper) {
        build(helper, false);
        ItemStack crystal = new ItemStack(ModItems.HEXTECH_DUST.get());
        helper.assertTrue(Multiblocks.find(helper.getLevel(), helper.absolutePos(CORNER), crystal).isEmpty(), "a jar with a full middle is no jar");
        build(helper, true);
        helper.assertTrue(Multiblocks.find(helper.getLevel(), helper.absolutePos(CORNER), new ItemStack(ModItems.BRASS_INGOT.get())).isEmpty(),
                "brass does not wake it");
        var found = Multiblocks.find(helper.getLevel(), helper.absolutePos(CORNER.offset(2, 3, 0)), crystal);
        helper.assertTrue(found.isPresent(), "clicking a slab of the lid finds the jar");
        Multiblocks.form(helper.getLevel(), found.get().getKey(), found.get().getValue(), null);

        BlockPos controller = CORNER.offset(1, 0, 1);
        helper.assertBlockPresent(ModBlocks.BIG_ESSENCE_JAR.get(), controller);
        helper.assertBlockPresent(ModBlocks.MULTIBLOCK_PART.get(), CORNER.offset(1, 1, 1));
        helper.assertBlockPresent(ModBlocks.MULTIBLOCK_PART.get(), CORNER.offset(0, 3, 2));
        BigEssenceJarBlockEntity jar = helper.getBlockEntity(controller, BigEssenceJarBlockEntity.class);
        helper.assertTrue(jar.tank().capacity() == BigEssenceJarBlockEntity.CAPACITY, "it holds sixteen jars");

        MultiblockPartBlockEntity side = helper.getBlockEntity(CORNER.offset(0, 2, 1), MultiblockPartBlockEntity.class);
        int taken = side.receive(Direction.WEST, EssenceMix.pure(Essence.FIRE, 300), false);
        helper.assertTrue(taken == 300 && jar.tank().amount() == 300, "a pipe on its side fills it, got " + taken);

        helper.setBlock(CORNER.offset(2, 3, 2), Blocks.AIR);
        helper.assertBlockPresent(Blocks.GLASS, controller);
        helper.assertBlockPresent(Blocks.GLASS, CORNER.offset(0, 2, 1));
        helper.assertBlockPresent(Blocks.OAK_SLAB, CORNER.offset(0, 3, 0));
        helper.assertBlockPresent(Blocks.AIR, CORNER.offset(1, 1, 1));
        helper.assertBlockPresent(Blocks.AIR, CORNER.offset(2, 3, 2));
        helper.succeed();
    }
}
