package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import com.pockyl.hextale.block.ChalkMarkBlock;
import com.pockyl.hextale.block.CrystalBatteryBlock;
import com.pockyl.hextale.block.CrystalBatteryBlockEntity;
import com.pockyl.hextale.block.HexWorkbenchBlockEntity;
import com.pockyl.hextale.block.NullstoneBlock;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;

/**
 * The steps between the benches: chalk runes quicken the altar, the Crystal Battery feeds
 * workbenches, nullstone drinks the Hexcore's anger.
 */
public final class GateGameTests {
    private GateGameTests() {
    }

    @HexTest
    public static void chalkRunesQuickenTheAltar(GameTestHelper helper) {
        BlockPos altar = new BlockPos(3, 1, 3);
        for (int x = 0; x < 3; x++) {
            helper.setBlock(new BlockPos(1 + x * 2, 1, 1), ModBlocks.CHALK_MARK.get());
        }
        BlockPos at = helper.absolutePos(altar);
        helper.assertTrue(ChalkMarkBlock.around(helper.getLevel(), at).size() == 3, "three marks around the altar");
        helper.assertTrue(Math.abs(ChalkMarkBlock.speed(helper.getLevel(), at) - 3 * ChalkMarkBlock.SPEED_PER_MARK) < 1e-4, "each mark adds speed");
        ChalkMarkBlock.light(helper.getLevel(), at, true);
        helper.assertBlockProperty(new BlockPos(1, 1, 1), ChalkMarkBlock.LIT, true);
        helper.succeed();
    }

    @HexTest(maxTicks = 100)
    public static void theBatteryFeedsAWorkbench(GameTestHelper helper) {
        BlockPos batteryAt = new BlockPos(1, 1, 1);
        BlockPos benchAt = new BlockPos(3, 1, 1);
        helper.setBlock(batteryAt, ModBlocks.CRYSTAL_BATTERY.get());
        helper.setBlock(benchAt, ModBlocks.HEX_WORKBENCH.get());
        CrystalBatteryBlockEntity battery = helper.getBlockEntity(batteryAt, CrystalBatteryBlockEntity.class);
        helper.assertTrue(battery.soak(new ItemStack(ModItems.HEXTECH_CRYSTAL.get())), "a crystal soaks in");
        helper.assertTrue(battery.charge() > 0, "the battery holds charge");
        HexWorkbenchBlockEntity bench = helper.getBlockEntity(benchAt, HexWorkbenchBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(bench.charge() > 0, "the workbench got charge");
            helper.assertBlockProperty(batteryAt, CrystalBatteryBlock.LEVEL, helper.getBlockState(batteryAt).getValue(CrystalBatteryBlock.LEVEL));
        });
    }

    @HexTest
    public static void nullstoneDrinksTheAnger(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.NULLSTONE.get());
        helper.setBlock(new BlockPos(5, 1, 5), ModBlocks.NULLSTONE.get().defaultBlockState().setValue(NullstoneBlock.SATURATION, NullstoneBlock.FULL));
        helper.setBlock(new BlockPos(5, 1, 1), Blocks.STONE);
        BlockPos at = helper.absolutePos(core);
        helper.assertTrue(NullstoneBlock.thirsty(helper.getLevel(), at).size() == 1, "only the stone that is not full drinks");
        helper.assertTrue(NullstoneBlock.calm(1) < 1 && NullstoneBlock.calm(9) == NullstoneBlock.calm(NullstoneBlock.MAX_STONES), "calm grows with stones up to a cap");
        NullstoneBlock.fill(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)));
        helper.assertBlockProperty(new BlockPos(1, 1, 1), NullstoneBlock.SATURATION, 1);
        helper.succeed();
    }

}
