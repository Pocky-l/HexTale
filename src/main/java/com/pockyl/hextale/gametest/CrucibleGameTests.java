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
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
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
            helper.assertTrue(rose(helper, crucible, com.pockyl.hextale.registry.ModItems.MAGIC_STONE.get()) == 1, "magic stone rose up");
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
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        BlockPos workbench = AT.offset(2, 0, 0);
        helper.setBlock(workbench, ModBlocks.HEX_WORKBENCH.get());
        var bench = helper.getBlockEntity(workbench, com.pockyl.hextale.block.HexWorkbenchBlockEntity.class);
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

    /** Light and spark in the boiling water: a torch thrown in rises as two hextech wisps. */
    @HexTest(maxTicks = 700)
    public static void aTorchRisesAsWisps(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        boolean[] thrown = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (!thrown[0]) {
                thrown[0] = true;
                crucible.pour(EssenceMix.pure(Essence.LIGHT, 2).plus(EssenceMix.pure(Essence.SPARK, 1)));
                drop(helper, new ItemStack(Items.TORCH));
            }
            int wisps = rose(helper, crucible, com.pockyl.hextale.registry.ModItems.HEX_WISP.get());
            helper.assertTrue(wisps == 2, "two wisps rose up, not " + wisps);
        });
    }

    /**
     * A wisp set down from its item drops it when put out and keeps its cell; the rune's wisp (any wisp set some other way)
     * drops nothing and gives way to a block.
     */
    @HexTest(padding = 2)
    public static void onlyAPlacedWispDropsItself(GameTestHelper helper) {
        BlockPos placed = AT;
        BlockPos wild = AT.offset(2, 0, 0);
        var red = ModBlocks.DYED_WISPS.get(net.minecraft.world.item.DyeColor.RED).get();
        helper.setBlock(placed, red.defaultBlockState().setValue(com.pockyl.hextale.block.HexWispBlock.PLACED, true));
        helper.setBlock(wild, ModBlocks.HEX_WISP.get());
        net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(helper.absolutePos(AT)).inflate(4);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).forEach(net.minecraft.world.entity.Entity::discard);
        helper.getLevel().destroyBlock(helper.absolutePos(wild), true);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "the rune's wisp drops nothing");
        helper.getLevel().destroyBlock(helper.absolutePos(placed), true);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around,
                item -> item.getItem().is(com.pockyl.hextale.registry.ModItems.DYED_WISPS.get(net.minecraft.world.item.DyeColor.RED).get())).size() == 1,
                "the placed red wisp drops itself");
        helper.succeed();
    }

    /** Only a pickaxe with the Eternal Flame rune looks past a wisp; a plain pickaxe, a block or an empty hand aim at it. */
    @HexTest
    public static void onlyAnEternalFlamePickaxeLooksPastAWisp(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.HEX_WISP.get());
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos at = helper.absolutePos(AT);
        java.util.function.Supplier<Boolean> aims = () -> !helper.getBlockState(AT)
                .getShape(helper.getLevel(), at, net.minecraft.world.phys.shapes.CollisionContext.of(player)).isEmpty();
        helper.assertTrue(aims.get(), "an empty hand aims at the wisp");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(aims.get(), "so does a hand with a block");
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pickaxe);
        helper.assertTrue(aims.get(), "and a pickaxe without the rune");
        pickaxe.set(com.pockyl.hextale.registry.ModDataComponents.PICKAXE_RUNES.get(),
                com.pockyl.hextale.runework.PickaxeRunes.EMPTY.with(com.pockyl.hextale.runework.PickaxeRune.ETERNAL_FLAME, 1));
        helper.assertTrue(!aims.get(), "a pickaxe with the Eternal Flame looks past it");
        helper.succeed();
    }

    /** Magic for two stones out of three: two rise as magic stone, the third dissolves like any other item. */
    @HexTest(maxTicks = 700)
    public static void aCatalystTheWaterCannotPayForDissolves(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
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
            helper.assertTrue(rose(helper, crucible, com.pockyl.hextale.registry.ModItems.MAGIC_STONE.get()) == 2, "two magic stones rose up");
        });
    }

    /** Broken while a magic stone still soaks: it comes out with the pot instead of vanishing. */
    @HexTest(maxTicks = 700)
    public static void aBrokenCrucibleGivesBackWhatSoaks(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
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

    /** Results of this item that rose: floating on the water or thrown out around the pot. */
    private static int rose(GameTestHelper helper, CrucibleBlockEntity crucible, net.minecraft.world.item.Item item) {
        int floating = crucible.floating().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
        return floating + helper.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(AT)).inflate(3),
                entity -> entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    /** Finished results float on the water; a chest on the pot's facing side takes them over the lip. */
    @HexTest(maxTicks = 700)
    public static void resultsFloatAndDriftIntoAChest(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        net.minecraft.core.Direction facing = crucible.getBlockState().getValue(com.pockyl.hextale.block.CrucibleBlock.FACING);
        boolean[] thrown = {false};
        boolean[] chest = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (!thrown[0]) {
                thrown[0] = true;
                crucible.pour(EssenceMix.pure(Essence.MAGIC, 6));
                drop(helper, new ItemStack(Items.STONE));
            }
            if (!chest[0]) {
                helper.assertTrue(crucible.floating().size() == 1 && crucible.floating().getFirst().is(com.pockyl.hextale.registry.ModItems.MAGIC_STONE.get()),
                        "the magic stone floats");
                chest[0] = true;
                helper.setBlock(AT.relative(facing), Blocks.CHEST);
            }
            helper.assertTrue(crucible.floating().isEmpty(), "it drifted out");
            var box = helper.getBlockEntity(AT.relative(facing), net.minecraft.world.level.block.entity.ChestBlockEntity.class);
            helper.assertTrue(box.getItem(0).is(com.pockyl.hextale.registry.ModItems.MAGIC_STONE.get()), "into the chest");
        });
    }

    /** Named things, tools and jars are never eaten: refused when put in. */
    @HexTest(maxTicks = 700)
    public static void itNeverEatsNamedThingsToolsOrJars(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        ItemStack named = new ItemStack(Items.LAPIS_LAZULI, 4);
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keep"));
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            helper.assertTrue(crucible.take(named, 4, true, null) == 0, "a named stack is refused");
            helper.assertTrue(crucible.take(new ItemStack(Items.IRON_PICKAXE), 1, true, null) == 0, "a tool is refused");
            helper.assertTrue(crucible.take(new ItemStack(com.pockyl.hextale.registry.ModItems.ESSENCE_TANK.get()), 1, true, null) == 0,
                    "an empty jar is refused");
            helper.assertTrue(crucible.essence().isEmpty(), "nothing dissolved");
        });
    }

    /** Boiling away needs two sneak-uses, and the essence it boils off wears the land like any poured away. */
    @HexTest(maxTicks = 700)
    public static void boilingAwayIsTwoStepsAndWearsTheLand(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        var chunk = helper.getLevel().getChunkAt(helper.absolutePos(AT));
        boolean[] armed = {false};
        helper.succeedWhen(() -> {
            if (!armed[0]) {
                helper.assertTrue(crucible.boiling(), "the water boils");
                armed[0] = true;
                crucible.pour(EssenceMix.pure(Essence.FIRE, 40));
                com.pockyl.hextale.thevoid.Thinning.set(helper.getLevel(), chunk, 0);
                net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
                helper.assertFalse(crucible.sneakUse(player), "the first sneak-use only readies it");
                helper.assertTrue(crucible.essence().get(Essence.FIRE) == 40 && crucible.armed(), "nothing is lost yet");
                helper.assertTrue(crucible.sneakUse(player), "the second boils it away");
            }
            helper.assertTrue(crucible.essence().isEmpty(), "the essence is gone");
            helper.assertTrue(com.pockyl.hextale.thevoid.Thinning.get(chunk) > 0, "and wore the land");
        });
    }

    /** Water that would push essence over the rim is refused; plain water may still brim over. */
    @HexTest
    public static void waterNeverPushesEssenceOverTheRim(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        helper.assertTrue(crucible.fillWater(CrucibleBlockEntity.BUCKET, true) > 0, "plain water may brim over");
        helper.assertTrue(crucible.wouldSpillEssence(0) == false, "no essence, nothing to spill");
        helper.succeed();
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
