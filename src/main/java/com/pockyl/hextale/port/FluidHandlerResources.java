package com.pockyl.hextale.port;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.jspecify.annotations.Nullable;

/**
 * A 1.21.1 {@link IFluidHandler} as a 26.x fluid resource handler. A null transaction changes the handler at once;
 * inside a {@link Transaction} the call simulates and the change waits for the commit. What earlier calls of the same
 * transaction promised to or from the handler is counted in ({@link TransactionContext#promised()}): 1.21.1 handlers
 * fill and drain as a whole, so a loop over the tanks of a multi-tank container never counts the same fluid twice.
 */
public final class FluidHandlerResources implements ResourceHandler<FluidResource> {
    private final IFluidHandler fluids;

    FluidHandlerResources(IFluidHandler fluids) {
        this.fluids = fluids;
    }

    public IFluidHandler fluids() {
        return fluids;
    }

    @Override
    public int size() {
        return fluids.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
        return FluidResource.of(fluids.getFluidInTank(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return fluids.getFluidInTank(index).getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return fluids.getTankCapacity(index);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return fluids.isFluidValid(index, resource.toStack(1));
    }

    /** 1.21.1 fluid handlers fill as a whole, not tank by tank: any index fills the handler. */
    @Override
    public int insert(int index, FluidResource resource, int amount, @Nullable TransactionContext transaction) {
        return insert(resource, amount, transaction);
    }

    @Override
    public int insert(FluidResource resource, int amount, @Nullable TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        Key key = new Key(fluids, true);
        int before = transaction == null ? 0 : transaction.promised().getOrDefault(key, 0);
        int moved = Math.max(0, fluids.fill(resource.toStack(amount + before), IFluidHandler.FluidAction.SIMULATE) - before);
        if (moved > 0) {
            if (transaction != null) {
                transaction.promised().merge(key, moved, Integer::sum);
            }
            change(transaction, () -> fill(resource.toStack(moved)));
        }
        return moved;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, @Nullable TransactionContext transaction) {
        return extract(resource, amount, transaction);
    }

    @Override
    public int extract(FluidResource resource, int amount, @Nullable TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        if (transaction == null) {
            return fluids.drain(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
        Key key = new Key(fluids, false);
        int before = transaction.promised().getOrDefault(key, 0);
        int moved = Math.max(0, fluids.drain(resource.toStack(amount + before), IFluidHandler.FluidAction.SIMULATE).getAmount() - before);
        if (moved > 0) {
            transaction.promised().merge(key, moved, Integer::sum);
            transaction.onCommit(() -> {
                int drained = fluids.drain(resource.toStack(moved), IFluidHandler.FluidAction.EXECUTE).getAmount();
                if (drained < moved) {
                    com.pockyl.hextale.Hextale.LOGGER.warn("A fluid storage gave {} of {} mB it promised", drained, moved);
                }
            });
        }
        return moved;
    }

    /** One handler, one way, compared by identity. */
    private record Key(IFluidHandler handler, boolean in) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && key.handler == handler && key.in == in;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(handler) * 2 + (in ? 1 : 0);
        }
    }

    private void fill(FluidStack stack) {
        int filled = fluids.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        if (filled < stack.getAmount()) {
            com.pockyl.hextale.Hextale.LOGGER.warn("A fluid storage took {} of {} mB it had room for", filled, stack.getAmount());
        }
    }

    private static void change(@Nullable TransactionContext transaction, Runnable change) {
        if (transaction == null) {
            change.run();
        } else {
            transaction.onCommit(change);
        }
    }
}
