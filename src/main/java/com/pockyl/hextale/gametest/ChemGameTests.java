package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.ChemBurnerBlock;
import com.pockyl.hextale.block.ChemBurnerBlockEntity;
import com.pockyl.hextale.block.CrucibleBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.BrewEffects;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;

/**
 * Chemtech's first chemistry: a catalyst turns boiling water holding its essences into doses of a brew (for a
 * researcher only), glass bottles take the doses as vials, the acid needs a Chem Burner's heat, the burner burns only
 * under work, and the splashes change the world.
 */
public final class ChemGameTests {
    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private ChemGameTests() {
    }

    @HexTest(maxTicks = 700)
    public static void aCatalystTurnsTheWaterIntoABrew(GameTestHelper helper) {
        helper.setBlock(AT.below(), Blocks.LAVA);
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        boolean[] done = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(crucible.boiling(), "the water boils");
            if (done[0]) {
                return;
            }
            crucible.pour(EssenceMix.pure(Essence.FIRE, 8).plus(EssenceMix.pure(Essence.ASH, 4)));
            helper.assertTrue(crucible.take(new ItemStack(Items.COAL), 1, true, player) == 1, "the coal goes in");
            helper.assertTrue(crucible.brew() == null, "without the research it only dissolves");
            Research.research(player, ResearchTopics.COMBUSTIBLE_MIX);
            helper.assertTrue(crucible.take(new ItemStack(Items.COAL), 1, true, player) == 1, "the second coal goes in");
            helper.assertTrue(crucible.brew() == Brew.FUEL && crucible.doses() == 4, "the water is four doses of combustible mix");
            ItemStack vial = crucible.bottle();
            helper.assertTrue(ChemVialItem.brew(vial) == Brew.FUEL && crucible.doses() == 3, "a bottle takes one dose");
            // Lava is too weak for the acid: the eye only dissolves.
            crucible.pour(EssenceMix.pure(Essence.VENOM, 6).plus(EssenceMix.pure(Essence.DECAY, 6)));
            crucible.take(new ItemStack(Items.FERMENTED_SPIDER_EYE), 1, true, player);
            helper.assertTrue(crucible.brew() == Brew.FUEL, "no acid over lava");
            done[0] = true;
        });
    }

    @HexTest(maxTicks = 900)
    public static void theBurnerHeatsTheCrucibleHotEnoughForAcid(GameTestHelper helper) {
        helper.setBlock(AT.below(), ModBlocks.CHEM_BURNER.get());
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        ChemBurnerBlockEntity burner = helper.getBlockEntity(AT.below(), ChemBurnerBlockEntity.class);
        burner.feed(ChemVialItem.of(Brew.FUEL, 2));
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Research.research(player, ResearchTopics.ACID);
        boolean[] done = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(ChemBurnerBlock.isLit(helper.getBlockState(AT.below())), "the burner burns under the crucible");
            helper.assertTrue(crucible.heat() == 5 && crucible.boiling(), "the water boils over the green flame");
            helper.assertTrue(burner.fuel().getCount() == 1, "it burned one vial");
            if (done[0]) {
                return;
            }
            crucible.pour(EssenceMix.pure(Essence.VENOM, 6).plus(EssenceMix.pure(Essence.DECAY, 6)));
            helper.assertTrue(crucible.take(new ItemStack(Items.FERMENTED_SPIDER_EYE), 1, true, player) == 1, "the eye goes in");
            helper.assertTrue(crucible.brew() == Brew.ACID, "the water turns into acid");
            done[0] = true;
        });
    }

    @HexTest(maxTicks = 60)
    public static void theBurnerBurnsOnlyUnderWork(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.CHEM_BURNER.get());
        ChemBurnerBlockEntity burner = helper.getBlockEntity(AT, ChemBurnerBlockEntity.class);
        burner.pour(EssenceMix.unknown(10));
        helper.runAfterDelay(40, () -> {
            helper.assertFalse(ChemBurnerBlock.isLit(helper.getBlockState(AT)), "nothing above: it stays out");
            helper.assertTrue(burner.tank().unknown() == 10, "and spends nothing");
            helper.succeed();
        });
    }

    @HexTest
    public static void splashesChangeTheWorld(GameTestHelper helper) {
        // Acid: stone hisses away, iron rusts, the ore stays.
        BlockPos stone = new BlockPos(1, 2, 1);
        helper.setBlock(stone, Blocks.STONE);
        helper.setBlock(stone.east(), Blocks.IRON_BLOCK);
        helper.setBlock(stone.south(), Blocks.IRON_ORE);
        BrewEffects.splash(helper.getLevel(), Vec3.atCenterOf(helper.absolutePos(stone)), Brew.ACID, null);
        helper.assertBlockPresent(Blocks.AIR, stone);
        helper.assertBlockPresent(ModBlocks.RUSTED_PLATING.get(), stone.east());
        helper.assertBlockPresent(Blocks.IRON_ORE, stone.south());
        // Coolant: still lava freezes into obsidian.
        BlockPos lava = new BlockPos(5, 2, 5);
        helper.setBlock(lava, Blocks.LAVA);
        BrewEffects.splash(helper.getLevel(), Vec3.atCenterOf(helper.absolutePos(lava.above())), Brew.COOLANT, null);
        helper.assertBlockPresent(Blocks.OBSIDIAN, lava);
        // Growth: a young crop shoots up.
        BlockPos crop = new BlockPos(1, 3, 6);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, Blocks.WHEAT);
        BrewEffects.splash(helper.getLevel(), Vec3.atCenterOf(helper.absolutePos(crop)), Brew.GROWTH, null);
        BlockState wheat = helper.getBlockState(crop);
        helper.assertTrue(wheat.getValue(CropBlock.AGE) > 0, "the wheat grew");
        helper.succeed();
    }
}
