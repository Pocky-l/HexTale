package com.pockyl.hextale.port;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import org.jspecify.annotations.Nullable;

/**
 * An item kind with its components and no count, as in the NeoForge 26.x transfer API.
 *
 * <p>port-of: net.neoforged.neoforge.transfer.item.ItemResource
 */
public final class ItemResource {
    public static final ItemResource EMPTY = new ItemResource(ItemStack.EMPTY);

    private final ItemStack prototype;

    private ItemResource(ItemStack prototype) {
        this.prototype = prototype;
    }

    public static ItemResource of(ItemStack stack) {
        return stack.isEmpty() ? EMPTY : new ItemResource(stack.copyWithCount(1));
    }

    public static ItemResource of(ItemLike item) {
        return of(new ItemStack(item));
    }

    public ItemStack toStack(int count) {
        return prototype.copyWithCount(count);
    }

    public ItemStack toStack() {
        return toStack(1);
    }

    public int getMaxStackSize() {
        return toStack().getMaxStackSize();
    }

    public Item getItem() {
        return prototype.getItem();
    }

    public boolean isEmpty() {
        return prototype.isEmpty();
    }

    public boolean matches(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(prototype, stack);
    }

    public boolean is(Item item) {
        return prototype.is(item);
    }

    public boolean is(TagKey<Item> tag) {
        return prototype.is(tag);
    }

    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        return prototype.get(type);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ItemResource resource && ItemStack.isSameItemSameComponents(prototype, resource.prototype);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(prototype);
    }

    @Override
    public String toString() {
        return prototype.toString();
    }
}
