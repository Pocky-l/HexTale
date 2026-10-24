package com.pockyl.hextale.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import org.jspecify.annotations.Nullable;

/**
 * The item side of the NeoForge 26.x transfer API over a 1.21.1 {@link IItemHandler}. A null transaction changes the
 * handler at once; inside a {@link Transaction} the call simulates and the change waits for the commit.
 *
 * <p>port-of: net.neoforged.neoforge.transfer.ResourceHandler
 */
public final class ResourceHandler<T> {
    private final IItemHandler items;

    private ResourceHandler(IItemHandler items) {
        this.items = items;
    }

    public static ResourceHandler<ItemResource> of(IItemHandler items) {
        return new ResourceHandler<>(items);
    }

    /** {@code level.getCapability(Capabilities.Item.BLOCK, pos, side)} of 26.x. */
    public static @Nullable ResourceHandler<ItemResource> ofBlock(Level level, BlockPos pos, @Nullable Direction side) {
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        return items == null ? null : new ResourceHandler<>(items);
    }

    public IItemHandler items() {
        return items;
    }

    public int size() {
        return items.getSlots();
    }

    public ItemResource getResource(int index) {
        return ItemResource.of(items.getStackInSlot(index));
    }

    public int getAmountAsInt(int index) {
        return items.getStackInSlot(index).getCount();
    }

    public long getAmountAsLong(int index) {
        return getAmountAsInt(index);
    }

    public int getCapacityAsInt(int index, ItemResource resource) {
        return resource.isEmpty() ? items.getSlotLimit(index) : Math.min(items.getSlotLimit(index), resource.toStack().getMaxStackSize());
    }

    public boolean isValid(int index, ItemResource resource) {
        return items.isItemValid(index, resource.toStack());
    }

    /** Puts up to amount into one slot; returns how many went. */
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
    public int insert(ItemResource resource, int amount, @Nullable TransactionContext transaction) {
        int moved = 0;
        for (int i = 0; i < size() && moved < amount; i++) {
            moved += insert(i, resource, amount - moved, transaction);
        }
        return moved;
    }

    /** Takes up to amount of the resource out of one slot; returns how many came. */
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
