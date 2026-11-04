package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.garden.ChemPlanterBlockEntity;
import com.pockyl.hextale.garden.Dose;
import com.pockyl.hextale.garden.EssenceSprinklerBlockEntity;
import com.pockyl.hextale.garden.GardenEvents;
import com.pockyl.hextale.garden.GardenPouring;
import com.pockyl.hextale.garden.SapTapBlockEntity;
import com.pockyl.hextale.garden.SteepedSoilBlock;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

/**
 * The chemtech garden: a vial poured on farmland steeps it, a dosed bed makes its plant grow a stage more and spends a
 * dose, a Chem Planter takes vials and piped sap (pure sap only), an Essence Sprinkler wets and doses the beds around it,
 * and a Sap Tap on a lone log gives nothing.
 */
public final class GardenGameTests {
    private GardenGameTests() {
    }

    private static final BlockPos AT = new BlockPos(2, 1, 2);

    /** A vial of growth tonic poured on farmland: steeped soil with four doses, the vial comes back as a glass bottle. */
    @HexTest
    public static void aVialSteepsFarmland(GameTestHelper helper) {
        helper.setBlock(AT, Blocks.FARMLAND);
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, ChemVialItem.of(Brew.GROWTH));
        helper.assertTrue(GardenPouring.pourOnSoil(helper.getLevel(), helper.absolutePos(AT), player.getMainHandItem(), player, InteractionHand.MAIN_HAND),
                "the vial pours on the farmland");
        BlockState soil = helper.getBlockState(AT);
        helper.assertTrue(soil.is(ModBlocks.STEEPED_SOIL.get()) && soil.getValue(SteepedSoilBlock.DOSE) == Dose.GROWTH
                && soil.getValue(SteepedSoilBlock.CHARGES) == Dose.PER_VIAL, "steeped with four doses of tonic");
        helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "the bottle comes back");
        helper.succeed();
    }

    /** Wheat on shimmer-steeped soil: every stage it grows may bring another, and each extra stage spends a dose. */
    @HexTest
    public static void aDosedBedGrowsAStageMore(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.STEEPED_SOIL.get().defaultBlockState().setValue(SteepedSoilBlock.DOSE, Dose.SHIMMER)
                .setValue(SteepedSoilBlock.CHARGES, SteepedSoilBlock.MAX_CHARGES));
        helper.setBlock(AT.above(), Blocks.WHEAT);
        helper.assertTrue(helper.getBlockState(AT.above()).canSurvive(helper.getLevel(), helper.absolutePos(AT.above())), "wheat takes root in it");
        for (int i = 0; i < 6; i++) {
            GardenEvents.grew(helper.getLevel(), helper.absolutePos(AT.above()));
        }
        int age = helper.getBlockState(AT.above()).getValue(CropBlock.AGE);
        int left = helper.getBlockState(AT).getValue(SteepedSoilBlock.CHARGES);
        helper.assertTrue(age > 0, "the wheat grew on the dose");
        helper.assertTrue(left == SteepedSoilBlock.MAX_CHARGES - age, "each extra stage spent one dose (" + age + " stages, " + left + " left)");
        helper.succeed();
    }

    /** A Chem Planter: vials of one kind only, pure sap through a pipe in whole doses, nothing else. */
    @HexTest
    public static void aPlanterTakesOneKindAndPureSap(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.CHEM_PLANTER.get());
        ChemPlanterBlockEntity planter = helper.getBlockEntity(AT, ChemPlanterBlockEntity.class);
        helper.assertTrue(planter.receive(Direction.NORTH, EssenceMix.pure(Essence.SAP, 12).plus(EssenceMix.pure(Essence.FIRE, 1)), false) == 0,
                "a mixture is refused");
        helper.assertTrue(planter.receive(Direction.NORTH, EssenceMix.pure(Essence.SAP, 20), false) == 20, "pure sap is taken");
        helper.assertTrue(planter.dose() == Dose.SAP && planter.charges() == 20 / Dose.SAP_MB, "two whole doses of sap");
        helper.assertTrue(planter.pour(Dose.GROWTH, 4) == 0, "another kind waits until the tank is dry");
        helper.assertTrue(planter.receive(Direction.UP, EssenceMix.pure(Essence.SAP, 8), false) == 0, "nothing comes in from the top");
        helper.succeed();
    }

    /** An Essence Sprinkler with tonic in it wets dry farmland and steeps it, and fills a planter in reach. */
    @HexTest
    public static void aSprinklerWetsAndDosesTheBeds(GameTestHelper helper) {
        BlockPos sprinkler = AT.above();
        BlockPos field = AT.east(2);
        BlockPos planterAt = AT.west(2);
        helper.setBlock(field, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 0));
        helper.setBlock(planterAt, ModBlocks.CHEM_PLANTER.get());
        helper.setBlock(sprinkler, ModBlocks.ESSENCE_SPRINKLER.get());
        EssenceSprinklerBlockEntity head = helper.getBlockEntity(sprinkler, EssenceSprinklerBlockEntity.class);
        helper.assertTrue(head.pour(Dose.GROWTH, 8) == 8, "eight doses of tonic poured in");
        helper.assertTrue(head.receiveWater(Direction.DOWN, 500, false) == 500, "water piped in");
        for (int i = 0; i < 3; i++) {
            head.pulse(helper.getLevel());
        }
        BlockState soil = helper.getBlockState(field);
        helper.assertTrue(soil.is(ModBlocks.STEEPED_SOIL.get()) && soil.getValue(SteepedSoilBlock.DOSE) == Dose.GROWTH, "the farmland is steeped in tonic");
        ChemPlanterBlockEntity planter = helper.getBlockEntity(planterAt, ChemPlanterBlockEntity.class);
        helper.assertTrue(planter.dose() == Dose.GROWTH && planter.charges() > 0, "the planter's tank got tonic");
        helper.assertTrue(head.charges() < 8, "the sprinkler spent its doses");
        helper.succeed();
    }

    /** A tap on a lone log (no tree) stays dry. */
    @HexTest(maxTicks = 400)
    public static void aTapOnALoneLogStaysDry(GameTestHelper helper) {
        helper.setBlock(AT, Blocks.OAK_LOG);
        helper.setBlock(AT.south(), ModBlocks.SAP_TAP.get().defaultBlockState().setValue(com.pockyl.hextale.garden.SapTapBlock.FACING, Direction.NORTH));
        SapTapBlockEntity tap = helper.getBlockEntity(AT.south(), SapTapBlockEntity.class);
        helper.runAfterDelay(SapTapBlockEntity.DRIP_TICKS + 20, () -> {
            helper.assertTrue(tap.held() == 0 && !tap.dripping(), "a lone log gives no sap");
            helper.succeed();
        });
    }

    /** A Chem Planter grows the wheat on it by itself, a stage a dose, without waiting for the weather. */
    @HexTest(maxTicks = 600)
    public static void aPlanterGrowsItsPlant(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.CHEM_PLANTER.get());
        helper.setBlock(AT.above(), Blocks.WHEAT);
        ChemPlanterBlockEntity planter = helper.getBlockEntity(AT, ChemPlanterBlockEntity.class);
        planter.pour(Dose.GROWTH, 3);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(AT.above()).getValue(CropBlock.AGE) >= 3, "the wheat grew three stages");
            helper.assertTrue(planter.charges() == 0, "for its three doses");
        });
    }

    /** A vial poured on farmland soaks the 3x3 round it. */
    @HexTest
    public static void aVialSoaksTheThreeByThree(GameTestHelper helper) {
        for (BlockPos at : BlockPos.betweenClosed(AT.offset(-1, 0, -1), AT.offset(1, 0, 1))) {
            helper.setBlock(at, Blocks.FARMLAND);
        }
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, ChemVialItem.of(Brew.GROWTH));
        GardenPouring.pourOnSoil(helper.getLevel(), helper.absolutePos(AT), player.getMainHandItem(), player, InteractionHand.MAIN_HAND);
        for (BlockPos at : BlockPos.betweenClosed(AT.offset(-1, 0, -1), AT.offset(1, 0, 1))) {
            BlockState soil = helper.getBlockState(at);
            helper.assertTrue(soil.is(ModBlocks.STEEPED_SOIL.get()) && soil.getValue(SteepedSoilBlock.CHARGES) == Dose.PER_VIAL, "every bed soaked: " + at);
        }
        helper.succeed();
    }

    /** The sprinkler only tops up: soil still holding enough sap takes none, so the sprinkler keeps its sap. */
    @HexTest
    public static void aSprinklerOnlyTopsUp(GameTestHelper helper) {
        helper.setBlock(AT.east(2), ModBlocks.STEEPED_SOIL.get().defaultBlockState().setValue(SteepedSoilBlock.DOSE, Dose.SAP)
                .setValue(SteepedSoilBlock.CHARGES, 3));
        helper.setBlock(AT.above(), ModBlocks.ESSENCE_SPRINKLER.get());
        EssenceSprinklerBlockEntity head = helper.getBlockEntity(AT.above(), EssenceSprinklerBlockEntity.class);
        head.receive(Direction.DOWN, EssenceMix.pure(Essence.SAP, 64), false);
        head.pulse(helper.getLevel());
        helper.assertTrue(head.sap() == 64, "the bed holds enough: no sap spent");
        helper.succeed();
    }

    /** Crystal Cane takes root on grass, dirt, sand, farmland and the garden's beds. */
    @HexTest
    public static void crystalCaneTakesRoot(GameTestHelper helper) {
        BlockState cane = ModBlocks.CRYSTAL_CANE.get().defaultBlockState();
        for (var soil : java.util.List.of(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.SAND.defaultBlockState(),
                Blocks.FARMLAND.defaultBlockState(), ModBlocks.STEEPED_SOIL.get().defaultBlockState(), ModBlocks.CHEM_PLANTER.get().defaultBlockState())) {
            helper.setBlock(AT, soil);
            helper.assertTrue(cane.canSurvive(helper.getLevel(), helper.absolutePos(AT.above())), "crystal cane stands on " + soil);
        }
        helper.succeed();
    }
}
