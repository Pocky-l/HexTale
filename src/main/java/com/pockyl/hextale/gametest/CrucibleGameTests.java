package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.CrucibleBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.event.BlockAwakening;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.rune.Essence;

/**
 * The Crucible: dust wakes a cauldron into it (keeping its water), heat below brings the water to a boil, and only
 * boiling water takes essence and items in; it evaporates while it boils.
 */
public final class CrucibleGameTests {
    private CrucibleGameTests() {
    }

    private static final BlockPos AT = new BlockPos(0, 2, 0);

    @HexTest(maxTicks = 80)
    public static void aWaterCauldronWakesIntoAFullCrucible(GameTestHelper helper) {
        helper.setBlock(AT, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        BlockAwakening.start(helper.getLevel(), helper.absolutePos(AT), Direction.SOUTH, null, false);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(ModBlocks.CRUCIBLE.get(), AT);
            helper.assertTrue(helper.getBlockEntity(AT, CrucibleBlockEntity.class).water() == CrucibleBlockEntity.WATER_CAPACITY,
                    "the cauldron's water stays in the crucible");
        });
    }

    @HexTest
    public static void coldWaterTakesNothingIn(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        helper.assertTrue(crucible.dissolve(new ItemStack(Items.STICK), 1) == 0, "cold water dissolves nothing");
        helper.assertTrue(crucible.pour(EssenceMix.pure(Essence.FIRE, 16)).isEmpty(), "cold water takes no essence");
        helper.assertTrue(crucible.fillWater(CrucibleBlockEntity.WATER_BOTTLE, true) == 0, "a full crucible takes no more water");
        helper.succeed();
    }

    /** Over a lit soul campfire the water boils; then essence pours in, dropped sticks dissolve and the water evaporates. */
    @HexTest(maxTicks = 700)
    public static void heatBoilsTheWaterAndItTakesThingsIn(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        helper.assertTrue(crucible.heat() == 3, "a soul campfire heats by three");
        boolean[] dropped = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (!dropped[0]) {
                dropped[0] = true;
                helper.assertTrue(crucible.pour(EssenceMix.pure(Essence.FIRE, 16)).total() == 16, "boiling water takes essence");
                Vec3 at = Vec3.atLowerCornerOf(helper.absolutePos(AT)).add(0.5, crucible.surface(), 0.5);
                ItemEntity sticks = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.STICK, 3));
                sticks.setDeltaMovement(Vec3.ZERO);
                helper.getLevel().addFreshEntity(sticks);
            }
            helper.assertTrue(crucible.dissolvedCount() == 3, "the dropped sticks dissolve");
            helper.assertTrue(crucible.essence().get(Essence.FIRE) == 16, "the essence stays in the water");
            helper.assertTrue(crucible.water() < CrucibleBlockEntity.WATER_CAPACITY, "boiling water evaporates");
        });
    }
}
