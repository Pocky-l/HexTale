package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;

import com.pockyl.hextale.block.HexFurnaceBlockEntity;
import com.pockyl.hextale.block.HexWorkbenchBlockEntity;
import com.pockyl.hextale.block.LapidaryBlockEntity;
import com.pockyl.hextale.crafting.Cleavage;
import com.pockyl.hextale.crafting.HexCraftingRecipe;
import com.pockyl.hextale.event.BlockAwakening;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * The woken workshop: hextech dust turns a crafting table and a furnace into their hextech kin, the Hex Workbench crafts
 * on its crystal's charge, the Hex Furnace melts alloys (researched ones only for an owner who knows them), and the
 * lapidary cuts crystals along their cleavage planes.
 */
public final class CraftingGameTests {
    private CraftingGameTests() {
    }

    private static final BlockPos AT = new BlockPos(0, 1, 0);

    @HexTest(maxTicks = 80)
    public static void dustWakesACraftingTableAndAFurnace(GameTestHelper helper) {
        BlockPos furnace = new BlockPos(1, 1, 0);
        helper.setBlock(AT, Blocks.CRAFTING_TABLE);
        helper.setBlock(furnace, Blocks.FURNACE);
        BlockAwakening.start(helper.getLevel(), helper.absolutePos(AT), Direction.SOUTH, null, false);
        BlockAwakening.start(helper.getLevel(), helper.absolutePos(furnace), Direction.SOUTH, null, false);
        helper.runAfterDelay(BlockAwakening.TICKS / 2, () -> helper.assertBlockPresent(Blocks.CRAFTING_TABLE, AT));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(ModBlocks.HEX_WORKBENCH.get(), AT);
            helper.assertBlockPresent(ModBlocks.HEX_FURNACE.get(), furnace);
        });
    }

    /** Three brass make pipes on the workbench; the craft spends one charge, and the first one breaks an orb into five. */
    /** Hexsteel gear matches diamond gear and opens with the hexsteel research. */
    @HexTest
    public static void hexsteelGearIsAsGoodAsDiamond(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(ModItems.HEXSTEEL_PICKAXE.get()).getMaxDamage() == new ItemStack(Items.DIAMOND_PICKAXE).getMaxDamage(),
                "the pickaxe wears like a diamond one");
        helper.assertTrue(new ItemStack(ModItems.HEXSTEEL_CHESTPLATE.get()).getMaxDamage() == new ItemStack(Items.DIAMOND_CHESTPLATE).getMaxDamage(),
                "the chestplate wears like a diamond one");
        helper.assertTrue(new ItemStack(ModItems.HEXSTEEL_PICKAXE.get()).isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
                "the pickaxe mines obsidian");
        helper.assertTrue(com.pockyl.hextale.research.ResearchTopics.of(ModItems.HEXSTEEL_BOOTS.get()) == com.pockyl.hextale.research.ResearchTopics.HEXSTEEL,
                "the gear waits for the hexsteel research");
        helper.succeed();
    }

    @HexTest
    public static void theWorkbenchCraftsOnItsCrystalsCharge(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.HEX_WORKBENCH.get());
        HexWorkbenchBlockEntity bench = helper.getBlockEntity(AT, HexWorkbenchBlockEntity.class);
        for (int i = 0; i < 3; i++) {
            bench.setItem(3 + i, new ItemStack(ModItems.BRASS_INGOT.get()));
        }
        var found = HexCraftingRecipe.find(bench.input().input(), false);
        helper.assertTrue(found != null && found.getValue().result().item().value() == ModItems.BRASS_PIPE.get(), "brass in a row is pipes");
        helper.assertFalse(bench.spend(1), "no crystal, no craft");
        bench.setItem(HexWorkbenchBlockEntity.CRYSTAL, new ItemStack(ModItems.UNSTABLE_HEXTECH.get()));
        helper.assertTrue(bench.spend(found.getValue().charge()), "an orb pays for it");
        helper.assertTrue(bench.charge() == HexWorkbenchBlockEntity.UNSTABLE_CHARGE - 1 && bench.getItem(HexWorkbenchBlockEntity.CRYSTAL).isEmpty(),
                "the orb was used up into charge, one of it spent");
        List<ItemStack> vanilla = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            vanilla.add(i < 3 ? new ItemStack(Items.COPPER_INGOT) : ItemStack.EMPTY);
        }
        helper.assertTrue(HexCraftingRecipe.find(CraftingInput.of(3, 3, vanilla), false) == null, "copper in a row is nothing");
        helper.succeed();
    }

    @HexTest(maxTicks = 400)
    public static void theFurnaceMeltsBrass(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.HEX_FURNACE.get());
        HexFurnaceBlockEntity furnace = helper.getBlockEntity(AT, HexFurnaceBlockEntity.class);
        furnace.setItem(0, new ItemStack(ModItems.ZINC_INGOT.get()));
        furnace.setItem(2, new ItemStack(Items.COPPER_INGOT, 3));
        furnace.setItem(HexFurnaceBlockEntity.FUEL, new ItemStack(Items.COAL));
        helper.succeedWhen(() -> {
            ItemStack out = furnace.getItem(HexFurnaceBlockEntity.OUTPUT);
            helper.assertTrue(out.is(ModItems.BRASS_INGOT.get()) && out.getCount() == 4, "three copper and a zinc melt into four brass");
            helper.assertTrue(furnace.getItem(0).isEmpty() && furnace.getItem(2).isEmpty(), "the metals are used up");
        });
    }

    /** Hexsteel is researched: a furnace whose owner does not know it waits, whatever is in it. */
    @HexTest(maxTicks = 80)
    public static void hexsteelWaitsForItsResearch(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.HEX_FURNACE.get());
        HexFurnaceBlockEntity furnace = helper.getBlockEntity(AT, HexFurnaceBlockEntity.class);
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        furnace.setOwner(owner);
        furnace.setItem(0, new ItemStack(Items.IRON_INGOT));
        furnace.setItem(1, new ItemStack(ModItems.HEXTECH_DUST.get(), 2));
        furnace.setItem(2, new ItemStack(Items.COAL));
        furnace.setItem(HexFurnaceBlockEntity.FUEL, new ItemStack(Items.COAL));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue("locked".equals(furnace.blocked()), "it says the alloy is not researched, got " + furnace.blocked());
            helper.assertTrue(furnace.getItem(HexFurnaceBlockEntity.FUEL).getCount() == 1, "and burns no fuel on it");
            helper.succeed();
        });
    }

    /** Grinding along every plane in short strokes cuts the crystal clean; grinding across the planes cracks it. */
    @HexTest
    public static void cleavageCutsAlongThePlanes(GameTestHelper helper) {
        long seed = 12345;
        Cleavage.Cut clean = new Cleavage.Cut(Cleavage.Grade.FIRST, seed);
        for (float plane : clean.planes) {
            int left = Cleavage.Grade.FIRST.grindTicks;
            while (left > 0) {
                int stroke = Math.min(left, 20);
                clean.stroke(40, plane, stroke, 1);
                left -= stroke;
            }
        }
        helper.assertTrue(clean.finished() && clean.cracks == 0, "every facet cut, no cracks: " + clean.facetsCut() + "/" + clean.cracks);
        Cleavage.Cut rough = new Cleavage.Cut(Cleavage.Grade.FIRST, seed);
        float across = clean.planes[0] + 180.0F / Cleavage.Grade.FIRST.facets / 2;
        rough.stroke(100, across, Cleavage.CRACK_TICKS * Cleavage.Grade.FIRST.maxCracks, 1);
        helper.assertTrue(rough.shattered(), "grinding across the planes shatters it");
        Cleavage.Cut hot = new Cleavage.Cut(Cleavage.Grade.FIRST, seed, new int[Cleavage.Grade.FIRST.facets], 0, Cleavage.MAX_HEAT - 1);
        hot.stroke(0, hot.planes[0], 2, 1);
        helper.assertTrue(hot.cracks == 1, "holding it on the disc too long cracks it");
        helper.succeed();
    }

    /** On the bench: strokes along the planes turn an orb into a hextech crystal and wear the disc. */
    @HexTest(maxTicks = 400)
    public static void theLapidaryCutsAnOrbIntoACrystal(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.HEX_LAPIDARY.get());
        LapidaryBlockEntity lapidary = helper.getBlockEntity(AT, LapidaryBlockEntity.class);
        lapidary.setItem(LapidaryBlockEntity.DISC, new ItemStack(ModItems.CUTTING_DISC.get()));
        lapidary.setItem(LapidaryBlockEntity.INPUT, new ItemStack(ModItems.UNSTABLE_HEXTECH.get()));
        ServerPlayer worker = helper.makeMockServerPlayerInLevel();
        Cleavage.Cut cut = lapidary.cut();
        helper.assertTrue(cut != null && cut.grade == Cleavage.Grade.FIRST, "a raw orb on a cutting disc is a first cut");
        // A stroke per plane, with time to cool between them (the server measures the rest on its own clock).
        for (int i = 0; i < cut.planes.length; i++) {
            float plane = cut.planes[i];
            helper.runAfterDelay(1 + i * 70L, () -> lapidary.stroke(worker, plane, Cleavage.Grade.FIRST.grindTicks));
        }
        helper.runAfterDelay(1 + cut.planes.length * 70L, () -> {
            helper.assertTrue(lapidary.getItem(LapidaryBlockEntity.OUTPUT).is(ModItems.HEXTECH_CRYSTAL.get()), "the orb is cut into a crystal");
        helper.assertTrue(lapidary.getItem(LapidaryBlockEntity.INPUT).isEmpty(), "the orb is gone from the clamp");
            helper.assertTrue(lapidary.getItem(LapidaryBlockEntity.DISC).getDamageValue() >= 1, "the disc wore");
            helper.succeed();
        });
    }
}
