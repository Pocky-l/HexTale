package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.HexFoundryBlock;
import com.pockyl.hextale.block.HexFoundryBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.multiblock.MultiblockPartBlockEntity;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.PickaxeRunes;

/**
 * The Hex Foundry: built of fire bricks round its hatch, woken with hextech dust, melts nine pieces at once, burns fire
 * essence from a jar nearby, swallows what is dropped on its top and falls apart keeping the hatch's runes.
 */
public final class FoundryGameTests {
    /** The kiln's north-west bottom corner (relative); its hatch is in the middle of the south face. */
    private static final BlockPos CORNER = new BlockPos(1, 1, 1);
    private static final BlockPos HATCH = CORNER.offset(1, 1, 2);

    private FoundryGameTests() {
    }

    private static HexFoundryBlockEntity build(GameTestHelper helper) {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    helper.setBlock(CORNER.offset(x, y, z), ModBlocks.FIRE_BRICKS.get());
                }
            }
        }
        helper.setBlock(CORNER.offset(1, 1, 1), Blocks.AIR);
        helper.setBlock(CORNER.offset(1, 2, 1), Blocks.IRON_BARS);
        helper.setBlock(HATCH, ModBlocks.HEX_FOUNDRY.get().defaultBlockState().setValue(HexFoundryBlock.FACING, Direction.SOUTH));
        return helper.<HexFoundryBlockEntity>getBlockEntity(HATCH);
    }

    private static void light(GameTestHelper helper) {
        helper.assertTrue(Multiblocks.find(helper.getLevel(), helper.absolutePos(CORNER), new ItemStack(Items.FLINT_AND_STEEL)).isEmpty(),
                "flint and steel no longer wakes the kiln");
        var found = Multiblocks.find(helper.getLevel(), helper.absolutePos(CORNER), new ItemStack(ModItems.HEXTECH_DUST.get()));
        helper.assertTrue(found.isPresent() && found.get().getKey() == Multiblocks.HEX_FOUNDRY, "hextech dust on a brick finds the kiln");
        Multiblocks.form(helper.getLevel(), found.get().getKey(), found.get().getValue(), null);
    }

    @HexTest
    public static void theFoundryIsLitAndFallsApart(GameTestHelper helper) {
        HexFoundryBlockEntity hatch = build(helper);
        PickaxeRunes runes = PickaxeRunes.EMPTY.with(ComplexRunes.byKey("metal_fire"), 2);
        hatch.setRunes(runes);
        hatch.setItem(0, new ItemStack(Items.RAW_IRON, 5));
        helper.setBlock(CORNER.offset(1, 2, 1), Blocks.GLASS);
        helper.assertTrue(Multiblocks.find(helper.getLevel(), helper.absolutePos(CORNER), new ItemStack(ModItems.HEXTECH_DUST.get())).isEmpty(),
                "no grate on top, no kiln");
        helper.setBlock(CORNER.offset(1, 2, 1), Blocks.IRON_BARS);
        light(helper);
        helper.assertTrue(helper.getBlockState(HATCH).getValue(HexFoundryBlock.FORMED), "the hatch is the kiln's heart");
        helper.assertBlockPresent(ModBlocks.MULTIBLOCK_PART.get(), CORNER);
        helper.assertBlockPresent(ModBlocks.MULTIBLOCK_PART.get(), CORNER.offset(1, 1, 1));
        HexFoundryBlockEntity foundry = helper.<HexFoundryBlockEntity>getBlockEntity(HATCH);
        helper.assertTrue(foundry.isFormed(), "it stands as a kiln");
        MultiblockPartBlockEntity brick = helper.<MultiblockPartBlockEntity>getBlockEntity(CORNER.offset(0, 1, 1));
        helper.assertTrue(brick.sharedContainer() == foundry, "its bricks share the hatch's contents with hoppers");
        helper.assertTrue(brick.receive(Direction.WEST, EssenceMix.pure(Essence.FIRE, 10), false) == 10, "a pipe of fire feeds it");
        helper.assertTrue(brick.receive(Direction.WEST, EssenceMix.pure(Essence.WATER, 10), false) == 0, "water does not");

        helper.setBlock(CORNER.offset(2, 0, 0), Blocks.AIR);
        helper.assertBlockPresent(ModBlocks.FIRE_BRICKS.get(), CORNER);
        helper.assertBlockPresent(Blocks.IRON_BARS, CORNER.offset(1, 2, 1));
        helper.assertBlockPresent(Blocks.AIR, CORNER.offset(1, 1, 1));
        helper.assertFalse(helper.getBlockState(HATCH).getValue(HexFoundryBlock.FORMED), "the hatch is a plain hatch again");
        HexFoundryBlockEntity after = helper.<HexFoundryBlockEntity>getBlockEntity(HATCH);
        helper.assertTrue(!after.isFormed() && after.runes().equals(runes), "it keeps its runes");
        helper.assertTrue(after.getItem(0).is(Items.RAW_IRON) && after.getItem(0).getCount() == 5, "and what was in it");
        helper.succeed();
    }

    @HexTest(maxTicks = 300)
    public static void theFoundryMeltsNineAtOnce(GameTestHelper helper) {
        build(helper);
        light(helper);
        HexFoundryBlockEntity foundry = helper.<HexFoundryBlockEntity>getBlockEntity(HATCH);
        for (int slot = 0; slot < HexFoundryBlockEntity.INPUTS; slot++) {
            foundry.setItem(slot, new ItemStack(Items.RAW_IRON));
        }
        foundry.setItem(HexFoundryBlockEntity.FUEL, new ItemStack(Items.COAL));
        foundry.setHeat(HexFoundryBlockEntity.MAX_HEAT);
        helper.succeedWhen(() -> {
            int ingots = 0;
            for (int slot = HexFoundryBlockEntity.OUTPUT_START; slot < HexFoundryBlockEntity.FUEL; slot++) {
                ItemStack out = foundry.getItem(slot);
                ingots += out.is(Items.IRON_INGOT) ? out.getCount() : 0;
            }
            helper.assertTrue(ingots == 9, "all nine melt together, got " + ingots);
            helper.assertTrue(helper.getTick() <= 140, "hot, it melts twice as fast as a furnace");
        });
    }

    /** At full heat the foundry melts gold, charged hexsteel and gemstone dust into Radiant Alloy; ore melts beside it. */
    @HexTest(maxTicks = 400)
    public static void theFoundryMeltsRadiantAlloy(GameTestHelper helper) {
        build(helper);
        light(helper);
        HexFoundryBlockEntity foundry = helper.<HexFoundryBlockEntity>getBlockEntity(HATCH);
        foundry.setItem(0, new ItemStack(Items.GOLD_INGOT, 2));
        foundry.setItem(3, new ItemStack(ModItems.CHARGED_HEXSTEEL_INGOT.get()));
        foundry.setItem(5, new ItemStack(ModItems.GEMSTONE_DUST.get(), 2));
        foundry.setItem(8, new ItemStack(Items.RAW_IRON));
        foundry.setItem(HexFoundryBlockEntity.FUEL, new ItemStack(Items.COAL, 4));
        foundry.setHeat(HexFoundryBlockEntity.MAX_HEAT);
        helper.succeedWhen(() -> {
            int alloy = 0;
            int iron = 0;
            for (int slot = HexFoundryBlockEntity.OUTPUT_START; slot < HexFoundryBlockEntity.FUEL; slot++) {
                ItemStack out = foundry.getItem(slot);
                alloy += out.is(ModItems.RADIANT_ALLOY_INGOT.get()) ? out.getCount() : 0;
                iron += out.is(Items.IRON_INGOT) ? out.getCount() : 0;
            }
            helper.assertTrue(alloy == 2 && iron == 1, "two ingots of Radiant Alloy and the iron beside them, got " + alloy + " / " + iron);
            helper.assertTrue(foundry.getItem(0).isEmpty() && foundry.getItem(3).isEmpty() && foundry.getItem(5).isEmpty(), "the inputs are used up");
        });
    }

    @HexTest(maxTicks = 100)
    public static void theFoundryBurnsFireFromAJar(GameTestHelper helper) {
        build(helper);
        BlockPos jarPos = CORNER.offset(4, 0, 1);
        helper.setBlock(jarPos, ModBlocks.ESSENCE_TANK.get());
        helper.<EssenceTankBlockEntity>getBlockEntity(jarPos).tank().set(EssenceMix.pure(Essence.FIRE, 100));
        light(helper);
        HexFoundryBlockEntity foundry = helper.<HexFoundryBlockEntity>getBlockEntity(HATCH);
        foundry.setItem(0, new ItemStack(Items.COBBLESTONE, 4));
        helper.succeedWhen(() -> {
            helper.assertTrue(foundry.fire() > 0 && foundry.heat() > 0, "it burns and warms on the jar's fire");
            // Jars of neighbouring tests may be within reach too: the one it names gave the fire.
            BlockPos from = foundry.drawing();
            helper.assertTrue(from != null, "the stream shows where it came from");
            helper.assertTrue(!from.equals(helper.absolutePos(jarPos)) || helper.<EssenceTankBlockEntity>getBlockEntity(jarPos).tank().amount() < 100,
                    "the jar gave fire");
        });
    }

    @HexTest(maxTicks = 60)
    public static void theFoundrySwallowsWhatFallsOnItsTop(GameTestHelper helper) {
        build(helper);
        light(helper);
        HexFoundryBlockEntity foundry = helper.<HexFoundryBlockEntity>getBlockEntity(HATCH);
        Vec3 top = helper.absoluteVec(Vec3.atLowerCornerOf(CORNER).add(1.5, 3.1, 1.5));
        ItemEntity ore = new ItemEntity(helper.getLevel(), top.x, top.y, top.z, new ItemStack(Items.RAW_COPPER, 3));
        ItemEntity stick = new ItemEntity(helper.getLevel(), top.x + 0.8, top.y, top.z, new ItemStack(Items.STICK));
        helper.getLevel().addFreshEntity(ore);
        helper.getLevel().addFreshEntity(stick);
        helper.succeedWhen(() -> {
            int copper = 0;
            for (int slot = 0; slot < HexFoundryBlockEntity.INPUTS; slot++) {
                copper += foundry.getItem(slot).is(Items.RAW_COPPER) ? foundry.getItem(slot).getCount() : 0;
            }
            helper.assertTrue(copper == 3 && !ore.isAlive(), "the ore falls in through the grate");
            helper.assertTrue(stick.isAlive(), "a stick does not melt, so it stays on top");
        });
    }
}
