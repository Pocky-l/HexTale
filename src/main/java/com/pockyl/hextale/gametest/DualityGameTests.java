package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.chunk.LevelChunk;

import com.pockyl.hextale.block.ChemBoilerBlock;
import com.pockyl.hextale.block.ChemBoilerBlockEntity;
import com.pockyl.hextale.block.ChemBurnerBlockEntity;
import com.pockyl.hextale.block.ChemStillBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.chem.Overdrive;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.thevoid.Thinning;

/**
 * Stage 5, hextech against chemtech: a Chem Boiler on a burner drives the machines around it faster and pays in fumes,
 * takes on no more than its share of machines, and lets its burner rest when no machine asks for steam.
 */
public final class DualityGameTests {
    private DualityGameTests() {
    }

    /** A boiler on a lit burner with water, at {@code at}; returns it. */
    private static ChemBoilerBlockEntity boiler(GameTestHelper helper, BlockPos at, int fuelVials) {
        helper.setBlock(at.below(), ModBlocks.CHEM_BURNER.get());
        helper.setBlock(at, ModBlocks.CHEM_BOILER.get().defaultBlockState().setValue(ChemBoilerBlock.FACING, Direction.NORTH));
        helper.getBlockEntity(at.below(), ChemBurnerBlockEntity.class).feed(ChemVialItem.of(Brew.FUEL, fuelVials));
        ChemBoilerBlockEntity boiler = helper.getBlockEntity(at, ChemBoilerBlockEntity.class);
        boiler.fillWater(ChemBoilerBlockEntity.WATER_CAPACITY, false);
        return boiler;
    }

    /** A still with raw shimmer over its own lit burner, at {@code at}. */
    private static ChemStillBlockEntity still(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at.below(), ModBlocks.CHEM_BURNER.get());
        helper.setBlock(at, ModBlocks.CHEM_STILL.get());
        helper.getBlockEntity(at.below(), ChemBurnerBlockEntity.class).feed(ChemVialItem.of(Brew.FUEL, 4));
        ChemStillBlockEntity still = helper.getBlockEntity(at, ChemStillBlockEntity.class);
        still.feed(ChemVialItem.of(Brew.RAW_SHIMMER, 16));
        return still;
    }

    @HexTest(maxTicks = 400, padding = 4)
    public static void aBoilerDrivesAStillTwiceAsFastAndItsFumesWearTheLand(GameTestHelper helper) {
        // The lone still stands out of the boiler's reach, the other one beside it.
        BlockPos lone = new BlockPos(0, 2, 1);
        BlockPos driven = new BlockPos(6, 2, 1);
        BlockPos at = new BlockPos(8, 2, 1);
        ChemStillBlockEntity alone = still(helper, lone);
        ChemStillBlockEntity fast = still(helper, driven);
        ChemBoilerBlockEntity boiler = boiler(helper, at, 2);
        helper.assertFalse(boiler.reaches(helper.absolutePos(lone)), "the lone still is out of reach");
        LevelChunk chunk = helper.getLevel().getChunkAt(helper.absolutePos(at));
        float sourcesBefore = chunk.getData(ModAttachments.THINNING_SOURCES).getOrDefault(Thinning.Source.FUMES.name(), 0F);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(at).getValue(ChemBoilerBlock.DRIVING), "the boiler drives");
            helper.assertTrue(boiler.drives(helper.absolutePos(driven)), "the still is under its steam");
            helper.assertTrue(fast.progress() > alone.progress() * 1.6F + 0.05F || !fast.output().isEmpty() && alone.output().isEmpty(),
                    "the driven still is far ahead (" + fast.progress() + " vs " + alone.progress() + ")");
            float fumes = chunk.getData(ModAttachments.THINNING_SOURCES).getOrDefault(Thinning.Source.FUMES.name(), 0F);
            helper.assertTrue(fumes > sourcesBefore, "its fumes wore the land");
        });
    }

    @HexTest(maxTicks = 200, padding = 4)
    public static void aBoilerTakesOnlyItsShareOfMachines(GameTestHelper helper) {
        ChemBoilerBlockEntity boiler = boiler(helper, new BlockPos(3, 2, 3), 1);
        // A boiler joins the index when its block entity loads, a tick after it is placed.
        helper.runAfterDelay(2, () -> shareOfMachines(helper, boiler));
    }

    private static void shareOfMachines(GameTestHelper helper, ChemBoilerBlockEntity boiler) {
        boiler.setPressure(ChemBoilerBlockEntity.MAX_PRESSURE);
        int driven = 0;
        for (int i = 0; i < ChemBoilerBlockEntity.MAX_DRIVEN + 2; i++) {
            if (Overdrive.factor(helper.getLevel(), helper.absolutePos(new BlockPos(i, 2, 0))) > 1) {
                driven++;
            }
        }
        helper.assertTrue(driven == ChemBoilerBlockEntity.MAX_DRIVEN, "it drives " + driven + " machines, at most "
                + ChemBoilerBlockEntity.MAX_DRIVEN);
        helper.assertTrue(Overdrive.factor(helper.getLevel(), helper.absolutePos(new BlockPos(0, 2, 0))) >= ChemBoilerBlockEntity.PUSH,
                "a machine it took on keeps its steam");
        helper.assertTrue(Overdrive.factor(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 3)).offset(30, 0, 0)) == 1,
                "nothing out of reach is driven");
        helper.succeed();
    }

    @HexTest(maxTicks = 200, padding = 4)
    public static void anIdleBoilerLetsItsBurnerRest(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 2, 2);
        ChemBoilerBlockEntity boiler = boiler(helper, at, 1);
        helper.runAfterDelay(60, () -> {
            helper.assertFalse(boiler.wantsHeat(), "no machine asks for steam");
            helper.assertBlockProperty(at.below(), com.pockyl.hextale.block.ChemBurnerBlock.LIT, false);
            helper.assertTrue(boiler.pressure() == 0, "and no pressure is raised");
            helper.succeed();
        });
    }
}
