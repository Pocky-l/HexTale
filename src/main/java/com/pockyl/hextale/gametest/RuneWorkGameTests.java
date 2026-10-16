package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.RuneEngraverBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechTools;
import com.pockyl.hextale.runework.RuneSlots;
import com.pockyl.hextale.runework.ToolRunes;

import java.util.List;

/** Rune works: engraving rules, the essence cell and the runes firing on hextech tools. */
public final class RuneWorkGameTests {
    private RuneWorkGameTests() {
    }

    private static ItemStack pickaxe(int fire) {
        ItemStack tool = new ItemStack(ModItems.HEXTECH_PICKAXE.get());
        HextechTools.setRunes(tool, RuneSlots.EMPTY.with(new RuneSlots.Engraving(0, ToolRunes.SMELTING, 2)));
        HextechTools.setCell(tool, EssenceMix.pure(Essence.FIRE, fire));
        return tool;
    }

    /** Breaks iron ore with the tool (as a player) and returns the items it dropped. */
    private static List<ItemEntity> mine(GameTestHelper helper, ItemStack tool) {
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockState ore = Blocks.IRON_ORE.defaultBlockState();
        helper.getLevel().setBlockAndUpdate(pos, ore);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Block.dropResources(ore, helper.getLevel(), pos, null, player, tool);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
    }

    @HexTest
    public static void smeltingRuneSmeltsDrops(GameTestHelper helper) {
        ItemStack tool = pickaxe(10);
        List<ItemEntity> drops = mine(helper, tool);
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.IRON_INGOT)), "raw iron comes out as an ingot");
        helper.assertTrue(drops.stream().noneMatch(drop -> drop.getItem().is(Items.RAW_IRON)), "no raw iron left");
        helper.assertTrue(HextechTools.cell(tool).get(Essence.FIRE) == 9, "one mB of fire burned per item");
        helper.succeed();
    }

    @HexTest
    public static void smeltingSleepsWithoutFire(GameTestHelper helper) {
        ItemStack tool = pickaxe(0);
        List<ItemEntity> drops = mine(helper, tool);
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.RAW_IRON)), "without fire the drop stays raw");
        helper.succeed();
    }

    @HexTest
    public static void engravingRules(GameTestHelper helper) {
        ItemStack tool = new ItemStack(ModItems.HEXTECH_PICKAXE.get());
        ItemStack scroll = RuneScrollItem.of(ToolRunes.SMELTING, 3, ModItems.RUNE_SCROLL.get());
        RuneKnowledge novice = new RuneKnowledge();
        helper.assertTrue("unknown".equals(RuneEngraverBlockEntity.refusal(false, novice, tool, scroll, 0)), "unknown runes are refused");
        helper.assertTrue(RuneEngraverBlockEntity.refusal(true, novice, tool, scroll, 0) == null, "creative may engrave");
        helper.assertTrue("no_tool".equals(RuneEngraverBlockEntity.refusal(true, novice, new ItemStack(Items.IRON_PICKAXE), scroll, 0)),
                "only hextech tools take runes");
        RuneKnowledge expert = new RuneKnowledge();
        expert.learn(ToolRunes.SMELTING, 3, 0);
        helper.assertTrue(RuneEngraverBlockEntity.refusal(false, expert, tool, scroll, 1) == null, "a known rune goes in");
        HextechTools.setRunes(tool, HextechTools.runes(tool).with(new RuneSlots.Engraving(1, ToolRunes.SMELTING, 3)));
        helper.assertTrue("slot_taken".equals(RuneEngraverBlockEntity.refusal(false, expert, tool, scroll, 1)), "one rune per slot");
        helper.assertTrue("already".equals(RuneEngraverBlockEntity.refusal(false, expert, tool, scroll, 2)), "no rune twice on a tool");
        helper.assertTrue(HextechTools.runes(tool).get(1).stars() == 3, "the rune keeps the scroll's stars");
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, Hextale.id("rune_engraver"))).isPresent(), "engraver recipe loads");
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, Hextale.id("hextech_pickaxe"))).isPresent(), "pickaxe recipe loads");
        helper.succeed();
    }

    @HexTest
    public static void cellFillsFromTanks(GameTestHelper helper) {
        ItemStack tool = new ItemStack(ModItems.HEXTECH_PICKAXE.get());
        EssenceTank tank = new EssenceTank(256);
        tank.set(EssenceMix.pure(Essence.FIRE, 100));
        helper.assertTrue(HextechTools.fillFrom(tool, tank) == HextechTools.CELL, "the cell fills up to its size");
        helper.assertTrue(tank.amount() == 100 - HextechTools.CELL, "the tank gives what the cell took");
        helper.assertTrue(HextechTools.fillFrom(tool, tank) == 0, "a full cell takes nothing");
        EssenceTank murky = new EssenceTank(256);
        murky.set(EssenceMix.unknown(50));
        helper.assertTrue(HextechTools.fillFrom(new ItemStack(ModItems.HEXTECH_PICKAXE.get()), murky) < 0, "unknown essence is refused");
        helper.assertTrue(HextechTools.spend(tool, Essence.FIRE, 4) && HextechTools.cell(tool).get(Essence.FIRE) == HextechTools.CELL - 4,
                "a firing burns its essence");
        helper.assertFalse(HextechTools.spend(tool, Essence.WATER, 1), "nothing burns without that essence");
        helper.succeed();
    }
}
