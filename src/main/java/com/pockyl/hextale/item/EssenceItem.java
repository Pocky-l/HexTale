package com.pockyl.hextale.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

/**
 * One essence as an item, its pictogram in its colour: the form recipe viewers and the creative tab show essences in
 * (what a crucible recipe asks for, what an item dissolves into). It does nothing in the world.
 */
public class EssenceItem extends Item {
    public EssenceItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Essence essence) {
        return of(essence, 1);
    }

    public static ItemStack of(Essence essence, int count) {
        ItemStack stack = new ItemStack(ModItems.ESSENCE.get(), count);
        stack.set(ModDataComponents.ESSENCE_KIND.get(), essence);
        return stack;
    }

    @Nullable
    public static Essence essence(ItemStack stack) {
        return stack.get(ModDataComponents.ESSENCE_KIND.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        Essence essence = essence(stack);
        return essence == null ? super.getName(stack)
                : Component.translatable("item.hextale.essence.of", Component.translatable(essence.translationKey())).withColor(essence.color());
    }
}
