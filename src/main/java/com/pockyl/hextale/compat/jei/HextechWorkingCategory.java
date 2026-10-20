package com.pockyl.hextale.compat.jei;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.registry.ModItems;

import java.util.List;

/**
 * How hextech is worked in the world: lightning shatters amethyst into orbs, a thrown orb shatters into dust, dust wakes
 * a crafting table, a furnace and a cauldron, and the lapidary cuts orbs into crystals and crystals into gemstones.
 */
public final class HextechWorkingCategory {
    public static final IRecipeType<StepCategory.Step> TYPE = StepCategory.type("hextech_working");

    private HextechWorkingCategory() {
    }

    public static StepCategory create(IGuiHelper guiHelper) {
        return new StepCategory(TYPE, Component.translatable("jei.hextale.working"), guiHelper.createDrawableItemLike(ModItems.HEXTECH_DUST.get()), 3);
    }

    public static List<StepCategory.Step> all() {
        return List.of(
                step(List.of(new ItemStack(Items.LIGHTNING_ROD.asList().getFirst()), new ItemStack(Items.AMETHYST_BLOCK)),
                        new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), "lightning"),
                step(List.of(new ItemStack(ModItems.UNSTABLE_HEXTECH.get())), new ItemStack(ModItems.HEXTECH_DUST.get(), 2), "throw"),
                step(List.of(new ItemStack(ModItems.HEXTECH_DUST.get()), new ItemStack(Items.CRAFTING_TABLE)),
                        new ItemStack(ModItems.HEX_WORKBENCH.get()), "awaken"),
                step(List.of(new ItemStack(ModItems.HEXTECH_DUST.get()), new ItemStack(Items.FURNACE)),
                        new ItemStack(ModItems.HEX_FURNACE.get()), "awaken"),
                step(List.of(new ItemStack(ModItems.HEXTECH_DUST.get()), new ItemStack(Items.CAULDRON)),
                        new ItemStack(ModItems.CRUCIBLE.get()), "awaken"),
                step(List.of(new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), new ItemStack(ModItems.CUTTING_DISC.get())),
                        new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), "cut"),
                step(List.of(new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), new ItemStack(ModItems.MASTER_CUTTING_DISC.get())),
                        new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), "finish"));
    }

    private static StepCategory.Step step(List<ItemStack> inputs, ItemStack result, String note) {
        return new StepCategory.Step(inputs.stream().map(StepCategory.Input::of).toList(), result,
                Component.translatable("jei.hextale.working." + note));
    }
}
