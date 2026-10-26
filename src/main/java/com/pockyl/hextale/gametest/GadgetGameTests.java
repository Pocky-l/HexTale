package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.entity.DustCharge;
import com.pockyl.hextale.registry.ModEntities;

import java.util.List;

/** The early hextech gadgets: the Dust Charge (and the Levitation Plate). */
public final class GadgetGameTests {
    private GadgetGameTests() {
    }

    @HexTest(padding = 4)
    public static void aDustChargeCracksRockButNotBuilds(GameTestHelper helper) {
        BlockPos centre = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos stone = centre.east();
        BlockPos ore = centre.west();
        BlockPos planks = centre.north();
        BlockPos obsidian = centre.south();
        helper.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(ore, Blocks.IRON_ORE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(planks, Blocks.OAK_PLANKS.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(obsidian, Blocks.OBSIDIAN.defaultBlockState());
        DustCharge charge = new DustCharge(ModEntities.DUST_CHARGE.get(), helper.getLevel());
        List<BlockPos> cracked = DustCharge.burst(helper.getLevel(), Vec3.atCenterOf(centre), charge, null);
        helper.assertTrue(cracked.contains(stone) && cracked.contains(ore), "stone and ore crack: " + cracked);
        helper.assertTrue(helper.getLevel().getBlockState(planks).is(Blocks.OAK_PLANKS), "planks stay");
        helper.assertTrue(helper.getLevel().getBlockState(obsidian).is(Blocks.OBSIDIAN), "obsidian stays");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre).inflate(4));
        helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Items.COBBLESTONE)), "stone drops cobblestone");
        helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Items.RAW_IRON)), "ore drops raw iron");
        drops.forEach(ItemEntity::discard);
        helper.getLevel().setBlockAndUpdate(planks, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(obsidian, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }
}
