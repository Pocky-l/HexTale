package com.pockyl.hextale.compat.jei;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.crafting.AwakeningRecipe;
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
        List<StepCategory.Step> steps = new java.util.ArrayList<>();
        steps.add(step(List.of(new ItemStack(Items.LIGHTNING_ROD.asList().getFirst()), new ItemStack(Items.AMETHYST_BLOCK)),
                new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), "lightning"));
        steps.add(step(List.of(new ItemStack(ModItems.UNSTABLE_HEXTECH.get())), new ItemStack(ModItems.HEXTECH_DUST.get(), 2), "throw"));
        // Awakenings are data (recipe type hextale:awakening): one step per recipe whose blocks have items.
        for (AwakeningRecipe recipe : AwakeningRecipe.ALL.get(true).values()) {
            List<ItemStack> blocks = recipe.block().stream().map(block -> new ItemStack(block.value().asItem()))
                    .filter(stack -> !stack.isEmpty()).toList();
            List<ItemStack> catalyst = recipe.catalyst().map(ingredient -> ingredient.items().map(ItemStack::new).toList())
                    .orElse(List.of(new ItemStack(ModItems.HEXTECH_DUST.get())));
            ItemStack result = new ItemStack(recipe.result().asItem());
            if (!blocks.isEmpty() && !result.isEmpty()) {
                steps.add(new StepCategory.Step(List.of(StepCategory.Input.of(catalyst.toArray(ItemStack[]::new)),
                        StepCategory.Input.of(blocks.toArray(ItemStack[]::new))), result, Component.translatable("jei.hextale.working.awaken")));
            }
        }
        steps.add(step(List.of(new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), new ItemStack(ModItems.CUTTING_DISC.get())),
                new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), "cut"));
        steps.add(step(List.of(new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), new ItemStack(ModItems.MASTER_CUTTING_DISC.get())),
                new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), "finish"));
        steps.add(step(List.of(new ItemStack(Items.DIAMOND), new ItemStack(ModItems.MASTER_CUTTING_DISC.get())),
                new ItemStack(ModItems.ENGRAVED_DIAMOND.get()), "engrave"));
        return steps;
    }

    private static StepCategory.Step step(List<ItemStack> inputs, ItemStack result, String note) {
        return new StepCategory.Step(inputs.stream().map(StepCategory.Input::of).toList(), result,
                Component.translatable("jei.hextale.working." + note));
    }
}
