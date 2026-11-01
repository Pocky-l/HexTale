package com.pockyl.hextale.port;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import org.jspecify.annotations.Nullable;

/**
 * A 1.21.1 {@link IItemHandler} as a 26.x item resource handler. A null transaction changes the handler at once.
 * 1.21.1 handlers know no transactions, so inside a {@link Transaction} a call simulates and the change waits for the
 * commit (a trial must never feed a voiding storage, and taken items could not always go back, e.g. into a furnace's
 * result slot). What earlier calls of the same transaction promised to or from a slot is counted in
 * ({@link TransactionContext#promised()}), so two calls never both count the same items. On the commit, whatever a
 * storage refuses after all goes into its other slots, else drops at the block.
 */
public final class ItemHandlerResources implements ResourceHandler<ItemResource> {
    private final IItemHandler items;
    /** Where refused items drop; null for handlers that are not a block's. */
    private final @Nullable Level level;
    private final @Nullable BlockPos pos;

    ItemHandlerResources(IItemHandler items) {
        this(items, null, null);
    }

    ItemHandlerResources(IItemHandler items, @Nullable Level level, @Nullable BlockPos pos) {
        this.items = items;
        this.level = level;
        this.pos = pos == null ? null : pos.immutable();
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
        int before = transaction == null ? 0 : transaction.promised().getOrDefault(key(index, true), 0);
        int moved = Math.max(0, amount + before - items.insertItem(index, resource.toStack(amount + before), true).getCount() - before);
        if (moved > 0) {
            if (transaction != null) {
                transaction.promised().merge(key(index, true), moved, Integer::sum);
            }
            if (transaction == null) {
                place(index, resource.toStack(moved));
            } else {
                transaction.onCommit(() -> place(index, resource.toStack(moved)));
            }
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
        if (transaction == null) {
            return items.extractItem(index, amount, false).getCount();
        }
        int before = transaction.promised().getOrDefault(key(index, false), 0);
        int moved = Math.max(0, items.extractItem(index, amount + before, true).getCount() - before);
        if (moved > 0) {
            transaction.promised().merge(key(index, false), moved, Integer::sum);
            transaction.onCommit(() -> {
                int taken = items.extractItem(index, moved, false).getCount();
                if (taken < moved) {
                    com.pockyl.hextale.Hextale.LOGGER.warn("A storage gave {} of {} items it promised", taken, moved);
                }
            });
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

    private Slot key(int index, boolean in) {
        return new Slot(items, index, in);
    }

    /** One slot of one handler, one way: handlers compare by identity, wrappers of the same handler meet here. */
    private record Slot(IItemHandler handler, int index, boolean in) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Slot slot && slot.handler == handler && slot.index == index && slot.in == in;
        }

        @Override
        public int hashCode() {
            return (System.identityHashCode(handler) * 31 + index) * 2 + (in ? 1 : 0);
        }
    }

    /** Puts a stack in for real: its slot first, then any slot, then onto the ground at the block. */
    private void place(int index, ItemStack stack) {
        ItemStack rest = items.insertItem(index, stack, false);
        if (!rest.isEmpty()) {
            rest = ItemHandlerHelper.insertItemStacked(items, rest, false);
        }
        if (rest.isEmpty()) {
            return;
        }
        if (level != null && pos != null && !level.isClientSide()) {
            Block.popResource(level, pos, rest);
        } else {
            com.pockyl.hextale.Hextale.LOGGER.warn("A storage refused {} it had room for and has no place to drop it", rest);
        }
    }
}
