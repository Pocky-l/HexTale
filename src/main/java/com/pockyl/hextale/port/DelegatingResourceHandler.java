package com.pockyl.hextale.port;

import org.jspecify.annotations.Nullable;

/**
 * A handler that passes every call to another (override what should differ).
 *
 * <p>port-of: net.neoforged.neoforge.transfer.DelegatingResourceHandler
 */
public class DelegatingResourceHandler<T> implements ResourceHandler<T> {
    protected final ResourceHandler<T> delegate;

    public DelegatingResourceHandler(ResourceHandler<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public T getResource(int index) {
        return delegate.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return delegate.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, T resource) {
        return delegate.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, T resource) {
        return delegate.isValid(index, resource);
    }

    @Override
    public int insert(int index, T resource, int amount, @Nullable TransactionContext transaction) {
        return delegate.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, T resource, int amount, @Nullable TransactionContext transaction) {
        return delegate.extract(index, resource, amount, transaction);
    }

    @Override
    public int insert(T resource, int amount, @Nullable TransactionContext transaction) {
        return delegate.insert(resource, amount, transaction);
    }

    @Override
    public int extract(T resource, int amount, @Nullable TransactionContext transaction) {
        return delegate.extract(resource, amount, transaction);
    }
}
