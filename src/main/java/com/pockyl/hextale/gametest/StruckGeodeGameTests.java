package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.world.StruckGeodeFeature;

import java.util.List;

/** Struck geodes: the feature's geode, channel and scar, and what dormant hextech breaks into. */
public final class StruckGeodeGameTests {
    private static final int HALF = 12;
    private static final int HEIGHT = 26;

    private StruckGeodeGameTests() {
    }

    @HexTest(maxTicks = 20, padding = 14)
    public static void aStruckGeodeLiesUnderAScarOfFulgurite(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos min = base.offset(-HALF, 0, -HALF);
        BlockPos max = base.offset(HALF, HEIGHT - 1, HALF);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            level.setBlock(pos, pos.getY() == max.getY() ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        Feature feature = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValueOrThrow(StruckGeodeFeature.KEY);
        BlockPos origin = new BlockPos(base.getX(), max.getY() + 1, base.getZ());
        boolean placed = false;
        for (int tries = 0; tries < 8 && !placed; tries++) {
            placed = feature.place(level, level.getChunkSource().getGenerator(), level.getRandom(), origin);
        }
        int fulgurite = 0;
        int dormant = 0;
        int scorched = 0;
        int scar = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max.above())) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.FULGURITE.get())) {
                fulgurite++;
                if (pos.getY() >= max.getY() - 1) {
                    scar++;
                }
            } else if (state.is(ModBlocks.DORMANT_HEXTECH.get())) {
                dormant++;
            } else if (state.is(ModBlocks.SCORCHED_CALCITE.get())) {
                scorched++;
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(min, max.above())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        helper.assertTrue(placed, "the geode fits in a hill of stone");
        helper.assertTrue(scar >= 12, "a scar branches over it (" + scar + " fulgurite on the surface)");
        helper.assertTrue(fulgurite - scar >= 4, "a channel goes down (" + (fulgurite - scar) + " fulgurite below)");
        helper.assertTrue(dormant >= 3, "the amethyst partly woke into dormant hextech (" + dormant + ")");
        helper.assertTrue(scorched >= 20, "the shell is scorched calcite (" + scorched + ")");
        helper.succeed();
    }

    @HexTest
    public static void dormantHextechBreaksIntoOrbs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockState state = ModBlocks.DORMANT_HEXTECH.get().defaultBlockState();
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(ModItems.UNSTABLE_HEXTECH.get()) && drops.getFirst().getCount() >= 1,
                "a pickaxe breaks it into unstable hextech: " + drops);
        helper.assertTrue(state.is(com.pockyl.hextale.registry.ModTags.AWAKENS_INTO_HEXTECH), "a rod's strike wakes it like amethyst");
        helper.succeed();
    }
}
