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
 * boiling water takes essence and items in, dissolving items into their essences; the water never boils off by itself.
 * A catalyst the water can pay for transmutes: stone takes magic and rises as magic stone.
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
            helper.assertTrue(helper.<CrucibleBlockEntity>getBlockEntity(AT).water() == CrucibleBlockEntity.BUCKET,
                    "the cauldron's water stays in the crucible");
        });
    }

    @HexTest
    public static void coldWaterTakesNothingIn(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
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
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
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
                crucible.dissolve(new ItemStack(Items.LAPIS_LAZULI), 2);
                helper.assertTrue(crucible.volume() == CrucibleBlockEntity.CAPACITY, "dissolved essence pushes liquid over the rim too");
                // Set only once every check passed: a failed check is tried again until the test times out.
                done[0] = true;
            }
        });
    }

    /** Sneak-use boils the water away in a moment, essence and all. */
    @HexTest(maxTicks = 80)
    public static void theWaterBoilsAway(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
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
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
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
                ItemEntity sticks = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.LAPIS_LAZULI, 3));
                sticks.setDeltaMovement(Vec3.ZERO);
                sticks.setNoGravity(true);
                helper.getLevel().addFreshEntity(sticks);
            }
            int magic = CrucibleBlockEntity.essencesOf(new ItemStack(Items.LAPIS_LAZULI)).get(Essence.MAGIC);
            helper.assertTrue(magic > 0 && crucible.essence().get(Essence.MAGIC) == 3 * magic, "the dropped lapis dissolves into its essences");
            helper.assertTrue(crucible.essence().get(Essence.FIRE) == 16, "the essence stays in the water");
            helper.assertTrue(crucible.water() == CrucibleBlockEntity.BUCKET, "boiling water does not boil off by itself");
        });
    }

    /** Two lapis lazuli dissolve into magic; a stone thrown in after them takes six of it and rises as magic stone. */
    @HexTest(maxTicks = 700)
    public static void lapisAndStoneMakeMagicStone(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        int[] step = {2};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (step[0] == 2) {
                step[0] = 3;
                drop(helper, new ItemStack(Items.LAPIS_LAZULI, 2));
            }
            if (step[0] == 3) {
                helper.assertTrue(crucible.essence().get(Essence.MAGIC) == 6, "two lapis give six magic");
                step[0] = 4;
                drop(helper, new ItemStack(Items.STONE));
            }
            helper.assertTrue(crucible.essence().get(Essence.MAGIC) == 0, "the stone took the magic");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(AT)).inflate(3),
                    item -> item.getItem().is(com.pockyl.hextale.registry.ModItems.MAGIC_STONE.get())).isEmpty(), "magic stone rose out");
        });
    }

    /**
     * Brewing wears the land: a stone taking six magic in a transmutation adds six mB of brewing and one catalyst to the
     * chunk's Thinning, counted as brewing; a Hex Workbench spending its charge adds nothing.
     */
    @HexTest(maxTicks = 700)
    public static void brewingWearsTheLandThin(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        BlockPos workbench = AT.offset(2, 0, 0);
        helper.setBlock(workbench, ModBlocks.HEX_WORKBENCH.get());
        var bench = helper.<com.pockyl.hextale.block.HexWorkbenchBlockEntity>getBlockEntity(workbench);
        net.minecraft.world.level.chunk.LevelChunk chunk = helper.getLevel().getChunkAt(helper.absolutePos(AT));
        com.pockyl.hextale.thevoid.Thinning.set(helper.getLevel(), chunk, 0);
        bench.setItem(com.pockyl.hextale.block.HexWorkbenchBlockEntity.CRYSTAL, new ItemStack(com.pockyl.hextale.registry.ModItems.UNSTABLE_HEXTECH.get(), 2));
        helper.assertTrue(bench.spend(8) && com.pockyl.hextale.thevoid.Thinning.get(chunk) == 0, "the workbench's charge wears nothing");
        float expected = (6 * com.pockyl.hextale.thevoid.Thinning.BREWED + com.pockyl.hextale.thevoid.Thinning.BREW)
                * com.pockyl.hextale.Config.THINNING_RATE.get().floatValue();
        boolean[] thrown = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (!thrown[0]) {
                thrown[0] = true;
                crucible.pour(EssenceMix.pure(Essence.MAGIC, 6));
                drop(helper, new ItemStack(Items.STONE));
            }
            helper.assertTrue(crucible.essence().get(Essence.MAGIC) == 0, "the stone took the magic");
            float worn = com.pockyl.hextale.thevoid.Thinning.get(chunk);
            helper.assertTrue(worn >= expected - 0.01F, "the land wore " + worn + ", at least " + expected);
            helper.assertTrue(com.pockyl.hextale.thevoid.Thinning.sources(helper.getLevel(), helper.absolutePos(AT), 0).stream()
                    .anyMatch(entry -> entry.getKey() == com.pockyl.hextale.thevoid.Thinning.Source.BREWED), "counted as brewing");
        });
    }

    /** Magic for two stones out of three: two rise as magic stone, the third dissolves like any other item. */
    @HexTest(maxTicks = 700)
    public static void aCatalystTheWaterCannotPayForDissolves(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        boolean[] thrown = {false};
        EssenceMix stone = CrucibleBlockEntity.essencesOf(new ItemStack(Items.STONE));
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (!thrown[0]) {
                thrown[0] = true;
                crucible.pour(EssenceMix.pure(Essence.MAGIC, 6 * 2 + 3));
                drop(helper, new ItemStack(Items.STONE, 3));
            }
            helper.assertTrue(crucible.essence().get(Essence.MAGIC) == 3 + stone.get(Essence.MAGIC), "two stones took six magic each, the third dissolved");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(AT)).inflate(3),
                    item -> item.getItem().is(com.pockyl.hextale.registry.ModItems.MAGIC_STONE.get()) && item.getItem().getCount() == 2).isEmpty(),
                    "two magic stones rose out");
        });
    }

    /** Broken while a magic stone still soaks: it comes out with the pot instead of vanishing. */
    @HexTest(maxTicks = 700)
    public static void aBrokenCrucibleGivesBackWhatSoaks(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.<CrucibleBlockEntity>getBlockEntity(AT);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        boolean[] thrown = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (!thrown[0]) {
                thrown[0] = true;
                crucible.pour(EssenceMix.pure(Essence.MAGIC, 6));
                drop(helper, new ItemStack(Items.STONE));
            }
            helper.assertFalse(crucible.rising().isEmpty(), "the stone soaks");
            helper.getLevel().destroyBlock(helper.absolutePos(AT), false);
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(AT)).inflate(3),
                    item -> item.getItem().is(com.pockyl.hextale.registry.ModItems.MAGIC_STONE.get())).isEmpty(), "the magic stone came out");
        });
    }

    /** Thrown over the pot, well above the water: boiling water draws it in from there. */
    private static void drop(GameTestHelper helper, ItemStack stack) {
        Vec3 at = Vec3.atLowerCornerOf(helper.absolutePos(AT)).add(0.5, 1.5, 0.5);
        ItemEntity entity = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNoGravity(true);
        helper.getLevel().addFreshEntity(entity);
    }
}
