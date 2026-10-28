package com.pockyl.hextale.port;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import org.jspecify.annotations.Nullable;

/**
 * A 1.21.1 {@link IItemHandler} as a 26.x item resource handler. A null transaction changes the handler at once; inside
 * a {@link Transaction} the call simulates and the change waits for the commit.
 */
public final class ItemHandlerResources implements ResourceHandler<ItemResource> {
    private final IItemHandler items;

    ItemHandlerResources(IItemHandler items) {
        this.items = items;
    }

    public IItemHandler items() {
        return items;
    }

    @Override
    public int size() {
        return items.getSlots();
    }

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.of(items.getStackInSlot(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return items.getStackInSlot(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return resource.isEmpty() ? items.getSlotLimit(index) : Math.min(items.getSlotLimit(index), resource.toStack().getMaxStackSize());
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return items.isItemValid(index, resource.toStack());
    }

    /** Puts up to amount into one slot; returns how many went. */
    @Override
    public int insert(int index, ItemResource resource, int amount, @Nullable TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        ItemStack offer = resource.toStack(amount);
        int moved = amount - items.insertItem(index, offer, true).getCount();
        if (moved > 0) {
            change(transaction, () -> items.insertItem(index, resource.toStack(moved), false));
        }
        return moved;
    }

    /**
     * Puts up to amount in, slot by slot; returns how many went. Slots do not affect each other, so the per-slot
     * simulation inside a transaction adds up exactly.
     */
    @Override
    public int insert(ItemResource resource, int amount, @Nullable TransactionContext transaction) {
        int moved = 0;
        for (int i = 0; i < size() && moved < amount; i++) {
            moved += insert(i, resource, amount - moved, transaction);
        }
        return moved;
    }

    /** Takes up to amount of the resource out of one slot; returns how many came. */
    @Override
    public int extract(int index, ItemResource resource, int amount, @Nullable TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0 || !resource.matches(items.getStackInSlot(index))) {
            return 0;
        }
        int moved = items.extractItem(index, amount, true).getCount();
        if (moved > 0) {
            change(transaction, () -> items.extractItem(index, moved, false));
        }
        return moved;
    }

    /** Takes up to amount of the resource out of any slots; returns how many came. */
    @Override
    public int extract(ItemResource resource, int amount, @Nullable TransactionContext transaction) {
        int moved = 0;
        for (int i = 0; i < size() && moved < amount; i++) {
            moved += extract(i, resource, amount - moved, transaction);
        }
        return moved;
    }

    private static void change(@Nullable TransactionContext transaction, Runnable change) {
        if (transaction == null) {
            change.run();
        } else {
            transaction.onCommit(change);
        }
    }
}
