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
 * boiling water takes essence and items in; the water never boils off by itself. Stone soaks up its magic essence.
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
            helper.assertTrue(helper.getBlockEntity(AT, CrucibleBlockEntity.class).water() == CrucibleBlockEntity.BUCKET,
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
        crucible.setWater(CrucibleBlockEntity.CAPACITY);
        helper.assertTrue(crucible.fillWater(CrucibleBlockEntity.WATER_BOTTLE, true) == 0, "a crucible brim-full of water takes no more");
        helper.succeed();
    }

    /** Past the brim the liquid spills over in proportion; dissolved items stay and take their room. */
    @HexTest(maxTicks = 700)
    public static void itBrimsOver(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.LAVA);
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        boolean[] done = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (!done[0]) {
                crucible.setWater(1400);
                int water = crucible.water();
                crucible.pour(EssenceMix.pure(Essence.FIRE, 300));
                helper.assertTrue(crucible.volume() == CrucibleBlockEntity.CAPACITY, "it holds no more than its capacity");
                helper.assertTrue(crucible.water() < water && crucible.essence().get(Essence.FIRE) < 300, "water and essence spill in proportion");
                crucible.dissolve(new ItemStack(Items.STICK), 2);
                helper.assertTrue(crucible.dissolvedCount() == 2 && crucible.volume() == CrucibleBlockEntity.CAPACITY,
                        "an item takes its room and pushes liquid over the rim");
                helper.assertTrue(crucible.dissolve(new ItemStack(Items.STONE), 64) < 64, "items never crowd out all the liquid");
                // Set only once every check passed: a failed check is tried again until the test times out.
                done[0] = true;
            }
        });
    }

    /** Sneak-use boils the water away in a moment, essence and all. */
    @HexTest(maxTicks = 80)
    public static void theWaterBoilsAway(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        crucible.boilAway();
        helper.assertTrue(crucible.drying() && crucible.fillWater(CrucibleBlockEntity.WATER_BOTTLE, true) == 0, "it boils dry and takes no water meanwhile");
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.water() == 0 && !crucible.drying(), "the water is gone");
            helper.assertTrue(crucible.fillWater(CrucibleBlockEntity.WATER_BOTTLE, true) > 0, "then it can be filled again");
        });
    }

    /** Over a lit soul campfire the water boils; then essence pours in, dropped sticks dissolve and the water stays. */
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
                // Thrown over the pot, well above the water: boiling water draws it in from there.
                Vec3 at = Vec3.atLowerCornerOf(helper.absolutePos(AT)).add(0.5, 2.2, 0.5);
                ItemEntity sticks = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.STICK, 3));
                sticks.setDeltaMovement(Vec3.ZERO);
                sticks.setNoGravity(true);
                helper.getLevel().addFreshEntity(sticks);
            }
            helper.assertTrue(crucible.dissolvedCount() == 3, "the dropped sticks dissolve");
            helper.assertTrue(crucible.essence().get(Essence.FIRE) == 16, "the essence stays in the water");
            helper.assertTrue(crucible.water() == CrucibleBlockEntity.BUCKET, "boiling water does not boil off by itself");
        });
    }

    /** Stone thrown into a crucible with magic essence sinks, takes five of the magic each and rises as magic stone. */
    @HexTest(maxTicks = 700)
    public static void stoneSoaksUpMagicIntoMagicStone(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        boolean[] thrown = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (!thrown[0]) {
                thrown[0] = true;
                crucible.pour(EssenceMix.pure(Essence.MAGIC, CrucibleBlockEntity.MAGIC_PER_STONE * 2 + 3));
                Vec3 at = Vec3.atLowerCornerOf(helper.absolutePos(AT)).add(0.5, 1.5, 0.5);
                ItemEntity stone = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.STONE, 3));
                stone.setDeltaMovement(Vec3.ZERO);
                stone.setNoGravity(true);
                helper.getLevel().addFreshEntity(stone);
            }
            helper.assertTrue(crucible.essence().get(Essence.MAGIC) == 3, "two stones took five magic each, the third found too little");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(AT)).inflate(3),
                    item -> item.getItem().is(com.pockyl.hextale.registry.ModItems.MAGIC_STONE.get()) && item.getItem().getCount() == 2).isEmpty(),
                    "two magic stones rose out");
        });
    }
}
