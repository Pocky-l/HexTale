package com.pockyl.hextale.port;

import org.jspecify.annotations.Nullable;

/**
 * Helpers of the NeoForge 26.x transfer API.
 *
 * <p>port-of: net.neoforged.neoforge.transfer.ResourceHandlerUtil
 */
public final class ResourceHandlerUtil {
    private ResourceHandlerUtil() {
    }

    /**
     * Puts the resource first into slots that already hold it, then into the others; returns how many went. Without a
     * transaction it opens one and commits it, as NeoForge 26.x does: journaled handlers ({@link SnapshotJournal}, the
     * crucible's and battery's intake) change only inside one.
     */
    public static int insertStacking(ResourceHandler<ItemResource> handler, ItemResource resource, int amount,
                                     @Nullable TransactionContext transaction) {
        if (transaction == null) {
            try (Transaction root = Transaction.openRoot()) {
                int moved = insertStacking(handler, resource, amount, root);
                root.commit();
                return moved;
            }
        }
        int moved = 0;
        for (int i = 0; i < handler.size() && moved < amount; i++) {
            if (resource.equals(handler.getResource(i))) {
                moved += handler.insert(i, resource, amount - moved, transaction);
            }
        }
        for (int i = 0; i < handler.size() && moved < amount; i++) {
            if (handler.getResource(i).isEmpty()) {
                moved += handler.insert(i, resource, amount - moved, transaction);
            }
        }
        return moved;
    }
}
