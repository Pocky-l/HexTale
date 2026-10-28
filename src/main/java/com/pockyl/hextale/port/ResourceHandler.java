package com.pockyl.hextale.port;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import org.jspecify.annotations.Nullable;

/**
 * A handler of the NeoForge 26.x transfer API: indexed slots of item or fluid resources, changed inside transactions.
 * On 1.21.1 the capabilities hold {@link IItemHandler} / {@link IFluidHandler}: {@link #ofBlock} and
 * {@link #ofFluidBlock} wrap what a block offers, {@link #asItemHandler} and {@link #asFluidHandler} turn the mod's own
 * handlers into what other mods expect. A null transaction changes the handler at once.
 *
 * <p>port-of: net.neoforged.neoforge.transfer.ResourceHandler
 */
public interface ResourceHandler<T> {
    int size();

    T getResource(int index);

    long getAmountAsLong(int index);

    default int getAmountAsInt(int index) {
        return (int) Math.min(Integer.MAX_VALUE, getAmountAsLong(index));
    }

    long getCapacityAsLong(int index, T resource);

    default int getCapacityAsInt(int index, T resource) {
        return (int) Math.min(Integer.MAX_VALUE, getCapacityAsLong(index, resource));
    }

    boolean isValid(int index, T resource);

    int insert(int index, T resource, int amount, @Nullable TransactionContext transaction);

    int extract(int index, T resource, int amount, @Nullable TransactionContext transaction);

    /** Puts up to amount in, slot by slot; returns how much went. */
    default int insert(T resource, int amount, @Nullable TransactionContext transaction) {
        int moved = 0;
        for (int i = 0; i < size() && moved < amount; i++) {
            moved += insert(i, resource, amount - moved, transaction);
        }
        return moved;
    }

    /** Takes up to amount out, slot by slot; returns how much came. */
    default int extract(T resource, int amount, @Nullable TransactionContext transaction) {
        int moved = 0;
        for (int i = 0; i < size() && moved < amount; i++) {
            moved += extract(i, resource, amount - moved, transaction);
        }
        return moved;
    }

    @SuppressWarnings("unchecked")
    static ResourceHandler<FluidResource> of(IFluidHandler fluids) {
        return fluids instanceof Handed<?> handed ? (ResourceHandler<FluidResource>) handed.handler() : new FluidHandlerResources(fluids);
    }

    /** One of the mod's handlers handed over as a 1.21.1 capability. */
    interface Handed<T> {
        ResourceHandler<T> handler();
    }

    interface HandedItems extends IItemHandler, Handed<ItemResource> {
    }

    interface HandedFluids extends IFluidHandler, Handed<FluidResource> {
    }

    /** {@code level.getCapability(Capabilities.Item.BLOCK, pos, side)} of 26.x. */
    static @Nullable ResourceHandler<ItemResource> ofBlock(Level level, BlockPos pos, @Nullable Direction side) {
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        return items == null ? null : of(items);
    }

    /** A capability that is one of the mod's own handlers handed over: the handler itself (transactions reach it whole). */
    @SuppressWarnings("unchecked")
    static ResourceHandler<ItemResource> of(IItemHandler items) {
        return items instanceof Handed<?> handed ? (ResourceHandler<ItemResource>) handed.handler() : new ItemHandlerResources(items);
    }

    /** {@code level.getCapability(Capabilities.Fluid.BLOCK, pos, side)} of 26.x. */
    static @Nullable ResourceHandler<FluidResource> ofFluidBlock(Level level, BlockPos pos, @Nullable Direction side) {
        IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        return fluids == null ? null : of(fluids);
    }

    /** One of the mod's item handlers as the 1.21.1 capability (each call its own transaction). */
    static @Nullable IItemHandler asItemHandler(@Nullable ResourceHandler<ItemResource> handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ItemHandlerResources wrapped) {
            return wrapped.items();
        }
        return new HandedItems() {
            @Override
            public ResourceHandler<ItemResource> handler() {
                return handler;
            }

            @Override
            public int getSlots() {
                return handler.size();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return handler.getResource(slot).toStack(handler.getAmountAsInt(slot));
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (stack.isEmpty()) {
                    return stack;
                }
                int moved;
                try (Transaction transaction = Transaction.openRoot()) {
                    moved = handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
                    if (!simulate) {
                        transaction.commit();
                    }
                }
                return stack.copyWithCount(stack.getCount() - moved);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ItemResource resource = handler.getResource(slot);
                if (resource.isEmpty() || amount <= 0) {
                    return ItemStack.EMPTY;
                }
                int moved;
                try (Transaction transaction = Transaction.openRoot()) {
                    moved = handler.extract(slot, resource, amount, transaction);
                    if (!simulate) {
                        transaction.commit();
                    }
                }
                return resource.toStack(moved);
            }

            @Override
            public int getSlotLimit(int slot) {
                return handler.getCapacityAsInt(slot, ItemResource.EMPTY);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return handler.isValid(slot, ItemResource.of(stack));
            }
        };
    }

    /** One of the mod's fluid handlers as the 1.21.1 capability (each call its own transaction). */
    static @Nullable IFluidHandler asFluidHandler(@Nullable ResourceHandler<FluidResource> handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FluidHandlerResources wrapped) {
            return wrapped.fluids();
        }
        return new HandedFluids() {
            @Override
            public ResourceHandler<FluidResource> handler() {
                return handler;
            }

            @Override
            public int getTanks() {
                return handler.size();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return handler.getResource(tank).toStack(handler.getAmountAsInt(tank));
            }

            @Override
            public int getTankCapacity(int tank) {
                return handler.getCapacityAsInt(tank, handler.getResource(tank));
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return handler.isValid(tank, FluidResource.of(stack));
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) {
                    return 0;
                }
                try (Transaction transaction = Transaction.openRoot()) {
                    int moved = handler.insert(FluidResource.of(resource), resource.getAmount(), transaction);
                    if (action.execute()) {
                        transaction.commit();
                    }
                    return moved;
                }
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) {
                    return FluidStack.EMPTY;
                }
                FluidResource kind = FluidResource.of(resource);
                try (Transaction transaction = Transaction.openRoot()) {
                    int moved = handler.extract(kind, resource.getAmount(), transaction);
                    if (action.execute()) {
                        transaction.commit();
                    }
                    return kind.toStack(moved);
                }
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                for (int i = 0; i < handler.size(); i++) {
                    FluidResource kind = handler.getResource(i);
                    if (!kind.isEmpty() && handler.getAmountAsLong(i) > 0) {
                        return drain(kind.toStack(maxDrain), action);
                    }
                }
                return FluidStack.EMPTY;
            }
        };
    }
}
