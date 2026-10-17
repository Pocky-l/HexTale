package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.block.HexSmelterBlock;
import com.pockyl.hextale.block.HexSmelterBlockEntity;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;
import com.pockyl.hextale.runework.SmelterRune;

/** The Hex Smelter: fiery essence is its fuel, runes change its numbers, and they stay with it. */
public final class SmelterGameTests {
    private static final BlockPos SMELTER = new BlockPos(1, 1, 1);

    private SmelterGameTests() {
    }

    private static HexSmelterBlockEntity smelter(GameTestHelper helper) {
        helper.setBlock(SMELTER, ModBlocks.HEX_SMELTER.get().defaultBlockState());
        return helper.getBlockEntity(SMELTER, HexSmelterBlockEntity.class);
    }

    /** Raw iron and a bottle of fire become an ingot; the heat a bottle of fire gives is enough for two items. */
    @HexTest(maxTicks = 300)
    public static void fieryEssenceSmelts(GameTestHelper helper) {
        HexSmelterBlockEntity smelter = smelter(helper);
        smelter.setItem(HexSmelterBlockEntity.INPUT, new ItemStack(Items.RAW_IRON, 2));
        smelter.setItem(HexSmelterBlockEntity.BOTTLE_IN, EssenceFlaskItem.filled(EssenceMix.pure(Essence.FIRE, EssenceFlaskItem.CAPACITY)));
        helper.succeedWhen(() -> {
            helper.assertTrue(smelter.getItem(HexSmelterBlockEntity.OUTPUT).is(Items.IRON_INGOT)
                    && smelter.getItem(HexSmelterBlockEntity.OUTPUT).getCount() == 2, "two ingots");
            helper.assertTrue(smelter.getItem(HexSmelterBlockEntity.BOTTLE_OUT).is(Items.GLASS_BOTTLE), "the bottle came back empty");
            helper.assertTrue(smelter.tank().isEmpty(), "16 mB of fire smelted two items");
        });
    }

    /** Essence without fire neither goes in nor heats anything. */
    @HexTest
    public static void essenceWithoutFireIsRefused(GameTestHelper helper) {
        HexSmelterBlockEntity smelter = smelter(helper);
        helper.assertFalse(HexSmelterBlockEntity.accepts(HexSmelterBlockEntity.BOTTLE_IN,
                EssenceFlaskItem.filled(EssenceMix.pure(Essence.WATER, EssenceFlaskItem.CAPACITY))), "water is not fuel");
        helper.assertTrue(smelter.receive(Direction.SOUTH, EssenceMix.pure(Essence.WATER, 8), true) == 0, "pipes cannot bring water essence");
        helper.assertTrue(smelter.receive(Direction.NORTH, EssenceMix.pure(Essence.FIRE, 8), true) == 0, "the mouth takes no pipe");
        helper.assertTrue(smelter.receive(Direction.SOUTH, EssenceMix.pure(Essence.MAGMA, 8), true) == 8, "magma has fire in it");
        helper.assertTrue(Math.abs(HexSmelterBlockEntity.fire(EssenceMix.pure(Essence.MAGMA, 8)) - 0.5) < 0.01, "magma is half fire");
        helper.succeed();
    }

    /** Burned-in runes change the numbers, and the block keeps them on its item. */
    @HexTest
    public static void runesStayWithTheSmelter(GameTestHelper helper) {
        ItemStack item = new ItemStack(ModItems.HEX_SMELTER.get());
        RuneHolder.SMELTER.set(item, PickaxeRunes.EMPTY.with(SmelterRune.BELLOWS.rune(), 2).with(SmelterRune.TEMPERING.rune(), 1));
        helper.setBlock(SMELTER, ModBlocks.HEX_SMELTER.get().defaultBlockState().setValue(HexSmelterBlock.FACING, Direction.SOUTH));
        HexSmelterBlockEntity smelter = helper.getBlockEntity(SMELTER, HexSmelterBlockEntity.class);
        smelter.applyComponentsFromItemStack(item);
        helper.assertTrue(smelter.cookTicks() == HexSmelterBlockEntity.COOK_TICKS / 2, "Bellows with two stars: twice as fast");
        helper.assertTrue(smelter.heatPerItem() < HexSmelterBlockEntity.HEAT_PER_ITEM * 100, "Tempering saves heat");
        ItemStack back = new ItemStack(ModItems.HEX_SMELTER.get());
        back.applyComponents(smelter.collectComponents());
        helper.assertTrue(back.getOrDefault(ModDataComponents.SMELTER_RUNES.get(), PickaxeRunes.EMPTY).count() == 2, "its item keeps both runes");
        helper.succeed();
    }
}
