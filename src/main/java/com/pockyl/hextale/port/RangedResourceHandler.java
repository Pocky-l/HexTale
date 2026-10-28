package com.pockyl.hextale.port;

import net.neoforged.neoforge.items.IItemHandler;

import org.jspecify.annotations.Nullable;

/**
 * A run of another handler's slots seen as a handler of its own.
 *
 * <p>port-of: net.neoforged.neoforge.transfer.RangedResourceHandler
 */
public final class RangedResourceHandler<T> implements ResourceHandler<T> {
    private final ResourceHandler<T> handler;
    private final int start;
    private final int end;

    private RangedResourceHandler(ResourceHandler<T> handler, int start, int end) {
        this.handler = handler;
        this.start = start;
        this.end = end;
    }

    public static <T> RangedResourceHandler<T> ofSingleIndex(ResourceHandler<T> handler, int index) {
        return new RangedResourceHandler<>(handler, index, index + 1);
    }

    /** The 26.x wrappers hand out resource handlers; the 1.21.1 port classes hand out item handlers. */
    public static RangedResourceHandler<ItemResource> ofSingleIndex(IItemHandler items, int index) {
        return ofSingleIndex(ResourceHandler.of(items), index);
    }

    @Override
    public int size() {
        return end - start;
    }

    @Override
    public T getResource(int index) {
        return handler.getResource(start + index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return handler.getAmountAsLong(start + index);
    }

    @Override
    public long getCapacityAsLong(int index, T resource) {
        return handler.getCapacityAsLong(start + index, resource);
    }

    @Override
    public boolean isValid(int index, T resource) {
        return handler.isValid(start + index, resource);
    }

    @Override
    public int insert(int index, T resource, int amount, @Nullable TransactionContext transaction) {
        return handler.insert(start + index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, T resource, int amount, @Nullable TransactionContext transaction) {
        return handler.extract(start + index, resource, amount, transaction);
    }
}
