package com.pockyl.hextale.port;

/**
 * Argument checks of the NeoForge 26.x transfer API.
 *
 * <p>port-of: net.neoforged.neoforge.transfer.TransferPreconditions
 */
public final class TransferPreconditions {
    private TransferPreconditions() {
    }

    public static void checkNonEmptyNonNegative(Object resource, int amount) {
        boolean empty = resource instanceof ItemResource item ? item.isEmpty() : resource instanceof FluidResource fluid && fluid.isEmpty();
        if (empty) {
            throw new IllegalArgumentException("Expected a non-empty resource, got " + resource);
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Expected a non-negative amount, got " + amount);
        }
    }
}
