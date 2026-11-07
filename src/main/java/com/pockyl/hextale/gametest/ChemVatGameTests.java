package com.pockyl.hextale.gametest;

import com.pockyl.hextale.block.ChemVatBlock;
import com.pockyl.hextale.block.ChemVatBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Chem Vat: rusted plating over a Chem Burner, glass in three sides, a grate on top and the valve in front, sealed by
 * a vial of acid; it tempers iron and netherite scrap into chem-steel in a bath of acid and refined shimmer over a fire of
 * fuel vials, and swallows what is thrown on its grate.
 */
public final class ChemVatGameTests {
    /** The vat's north-west bottom corner (relative); its valve is in the middle of the south face. */
    private static final BlockPos CORNER = new BlockPos(1, 1, 1);
    private static final BlockPos VALVE = CORNER.offset(1, 1, 2);

    private ChemVatGameTests() {
    }

    private static void build(GameTestHelper helper) {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    helper.setBlock(CORNER.offset(x, y, z), (x + z) % 2 == 0 ? ModBlocks.RIVETED_RUSTED_PLATING.get() : ModBlocks.RUSTED_PLATING.get());
                }
            }
        }
        helper.setBlock(CORNER.offset(1, 0, 1), ModBlocks.CHEM_BURNER.get());
        helper.setBlock(CORNER.offset(1, 1, 1), net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(CORNER.offset(0, 1, 1), ModBlocks.CHEM_GLASS.get());
        helper.setBlock(CORNER.offset(2, 1, 1), ModBlocks.CHEM_GLASS.get());
        helper.setBlock(CORNER.offset(1, 1, 0), ModBlocks.CHEM_GLASS.get());
        helper.setBlock(CORNER.offset(1, 2, 1), ModBlocks.RUSTED_GRATE.get());
        helper.setBlock(VALVE, ModBlocks.CHEM_VAT.get().defaultBlockState().setValue(ChemVatBlock.FACING, Direction.SOUTH));
    }

    /** With the carboys around holding acid, refined shimmer and fuel, the vat needs only the metal. */
    @HexTest(maxTicks = 800)
    public static void theVatDrawsItsBathAndFireFromCarboys(GameTestHelper helper) {
        build(helper);
        var found = Multiblocks.find(helper.getLevel(), helper.absolutePos(CORNER), ChemVialItem.of(Brew.ACID));
        Multiblocks.form(helper.getLevel(), found.get().getKey(), found.get().getValue(), null);
        ChemVatBlockEntity vat = helper.<ChemVatBlockEntity>getBlockEntity(VALVE);
        com.pockyl.hextale.block.ChemCarboyBlockEntity[] carboys = {carboy(helper, CORNER.offset(4, 0, 0), Brew.ACID),
                carboy(helper, CORNER.offset(4, 0, 1), Brew.REFINED_SHIMMER), carboy(helper, CORNER.offset(4, 0, 2), Brew.EMBER_OIL)};
        helper.assertTrue(vat.insert(new ItemStack(Items.IRON_INGOT, 2)) == 2, "iron goes in");
        helper.spawnItem(Items.NETHERITE_SCRAP, CORNER.offset(1, 3, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(vat.getItem(ChemVatBlockEntity.OUTPUT).is(ModItems.CHEM_STEEL_INGOT.get()), "a chem-steel ingot came out");
            for (var carboy : carboys) {
                helper.assertTrue(carboy.contents().doses() < 10, "every carboy gave a dose");
            }
        });
    }

    private static com.pockyl.hextale.block.ChemCarboyBlockEntity carboy(GameTestHelper helper, BlockPos pos, Brew brew) {
        helper.setBlock(pos, ModBlocks.CHEM_CARBOY.get());
        var carboy = helper.<com.pockyl.hextale.block.ChemCarboyBlockEntity>getBlockEntity(pos);
        carboy.setContents(new com.pockyl.hextale.chem.CarboyContents(brew, 10));
        return carboy;
    }

    @HexTest(maxTicks = 800)
    public static void theVatTempersChemSteel(GameTestHelper helper) {
        build(helper);
        helper.assertTrue(Multiblocks.find(helper.getLevel(), helper.absolutePos(CORNER), new ItemStack(ModItems.HEXTECH_DUST.get())).isEmpty(),
                "hextech dust does not seal a chemtech vat");
        var found = Multiblocks.find(helper.getLevel(), helper.absolutePos(CORNER), ChemVialItem.of(Brew.ACID));
        helper.assertTrue(found.isPresent() && found.get().getKey() == Multiblocks.CHEM_VAT, "a vial of acid finds the vat");
        Multiblocks.form(helper.getLevel(), found.get().getKey(), found.get().getValue(), null);
        helper.assertTrue(helper.getBlockState(VALVE).getValue(ChemVatBlock.FORMED), "the valve is the vat's heart");
        ChemVatBlockEntity vat = helper.<ChemVatBlockEntity>getBlockEntity(VALVE);

        helper.assertTrue(vat.insert(new ItemStack(Items.IRON_INGOT, 2)) == 2, "iron goes in");
        helper.assertTrue(vat.insert(new ItemStack(Items.DIRT)) == 0, "dirt is not tempered");
        helper.assertTrue(vat.insert(ChemVialItem.of(Brew.ACID, 1)) == 1 && vat.insert(ChemVialItem.of(Brew.REFINED_SHIMMER, 1)) == 1,
                "the bath takes acid and refined shimmer");
        helper.assertTrue(vat.insert(ChemVialItem.of(Brew.EMBER_OIL, 2)) == 2, "ember oil goes under the bath");
        // The scrap is thrown on the grate.
        helper.spawnItem(Items.NETHERITE_SCRAP, CORNER.offset(1, 3, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(vat.getItem(ChemVatBlockEntity.OUTPUT).is(ModItems.CHEM_STEEL_INGOT.get()), "a chem-steel ingot came out");
            helper.assertTrue(vat.acid() == 0 && vat.shimmer() == 0, "the bath was spent");
            helper.assertTrue(vat.getItem(0).isEmpty() && vat.getItem(1).isEmpty() && vat.getItem(2).isEmpty(), "the metal was used");
        });
    }
}
