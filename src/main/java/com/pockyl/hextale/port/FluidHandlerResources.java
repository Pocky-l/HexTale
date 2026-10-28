package com.pockyl.hextale.port;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.jspecify.annotations.Nullable;

/**
 * A 1.21.1 {@link IFluidHandler} as a 26.x fluid resource handler. A null transaction changes the handler at once;
 * inside a {@link Transaction} the call simulates and the change waits for the commit.
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
        int moved = fluids.fill(resource.toStack(amount), IFluidHandler.FluidAction.SIMULATE);
        if (moved > 0) {
            change(transaction, () -> fluids.fill(resource.toStack(moved), IFluidHandler.FluidAction.EXECUTE));
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
        FluidStack drained = fluids.drain(resource.toStack(amount), IFluidHandler.FluidAction.SIMULATE);
        int moved = drained.getAmount();
        if (moved > 0) {
            change(transaction, () -> fluids.drain(resource.toStack(moved), IFluidHandler.FluidAction.EXECUTE));
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
