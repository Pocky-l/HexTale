package com.pockyl.hextale.compat.jei;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.AxolotlTankBlockEntity;
import com.pockyl.hextale.block.ChemStillBlockEntity;
import com.pockyl.hextale.block.DrainValveBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.ArrayList;
import java.util.List;

/**
 * The devices that do one simple thing, each shown as a {@link StepCategory}: the Chem Still refines shimmer, the Axolotl
 * Tank sheds slime, an open Drain Valve soaks stone into magic stone, and the altar writes a rune onto a blank scroll.
 */
public final class DeviceSteps {
    public static final RecipeType<StepCategory.Step> STILL = StepCategory.type("chem_still");
    public static final RecipeType<StepCategory.Step> TANK = StepCategory.type("axolotl_tank");
    public static final RecipeType<StepCategory.Step> SOAKING = StepCategory.type("soaking");
    public static final RecipeType<StepCategory.Step> WRITING = StepCategory.type("rune_writing");
    private static final TagKey<Item> SOAKS_ESSENCE = TagKey.create(Registries.ITEM, Hextale.id("soaks_essence"));

    private DeviceSteps() {
    }

    public static List<StepCategory> categories(IGuiHelper guiHelper) {
        return List.of(
                new StepCategory(STILL, Component.translatable("block.hextale.chem_still"), guiHelper.createDrawableItemLike(ModItems.CHEM_STILL.get()), 2),
                new StepCategory(TANK, Component.translatable("block.hextale.axolotl_tank"), guiHelper.createDrawableItemLike(ModItems.AXOLOTL_TANK.get()), 3),
                new StepCategory(SOAKING, Component.translatable("jei.hextale.soaking"), guiHelper.createDrawableItemLike(ModItems.DRAIN_VALVE.get()), 3),
                new StepCategory(WRITING, Component.translatable("jei.hextale.rune_writing"), guiHelper.createDrawableItemLike(ModItems.BLANK_SCROLL.get()), 3));
    }

    public static List<StepCategory.Step> still() {
        return List.of(new StepCategory.Step(
                List.of(StepCategory.Input.of(ChemVialItem.of(Brew.RAW_SHIMMER, ChemStillBlockEntity.RAW_PER_REFINED))),
                ChemVialItem.of(Brew.REFINED_SHIMMER),
                Component.translatable("jei.hextale.chem_still.note", seconds(ChemStillBlockEntity.REFINE_TICKS))));
    }

    public static List<StepCategory.Step> tank() {
        List<ItemStack> food = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            if (stack.is(ItemTags.FISHES)) {
                food.add(stack);
            }
        }
        food.add(new ItemStack(Items.TROPICAL_FISH_BUCKET));
        return List.of(new StepCategory.Step(
                List.of(StepCategory.Input.of(new ItemStack(Items.AXOLOTL_BUCKET)),
                        StepCategory.Input.of(food, Component.translatable("jei.hextale.axolotl_tank.food", AxolotlTankBlockEntity.BUCKET_MEALS))),
                new ItemStack(ModItems.REGENERATIVE_SLIME.get()),
                Component.translatable("jei.hextale.axolotl_tank.note", seconds(AxolotlTankBlockEntity.SHED_TICKS))));
    }

    public static List<StepCategory.Step> soaking() {
        List<ItemStack> stones = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            if (stack.is(SOAKS_ESSENCE)) {
                stones.add(stack);
            }
        }
        return List.of(new StepCategory.Step(
                List.of(StepCategory.Input.of(List.of(new ItemStack(ModItems.ESSENCE_FLASK.get())),
                                Component.translatable("jei.hextale.soaking.essence", DrainValveBlockEntity.ESSENCE_PER_STONE)),
                        new StepCategory.Input(stones, null)),
                new ItemStack(ModItems.MAGIC_STONE.get()),
                Component.translatable("jei.hextale.soaking.note", DrainValveBlockEntity.ESSENCE_PER_STONE)));
    }

    /** One entry: the blank scroll gives the scroll of whichever rune the stele shows. */
    public static List<StepCategory.Step> writing() {
        List<ItemStack> scrolls = new ArrayList<>();
        for (ComplexRune rune : ComplexRunes.all()) {
            scrolls.add(RuneScrollItem.of(rune, 1, ModItems.RUNE_SCROLL.get()));
        }
        return List.of(new StepCategory.Step(
                List.of(StepCategory.Input.of(new ItemStack(ModItems.BLANK_SCROLL.get()))),
                new StepCategory.Input(scrolls, null),
                Component.translatable("jei.hextale.rune_writing.note")));
    }

    static String seconds(int ticks) {
        return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format("%.1f", ticks / 20.0F);
    }
}
