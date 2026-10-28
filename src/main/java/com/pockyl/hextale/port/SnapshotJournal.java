package com.pockyl.hextale.port;

import org.jspecify.annotations.Nullable;

/**
 * A handler's state remembered per transaction (26.x transfer API): the first change inside a transaction keeps a
 * snapshot; a transaction closed without a commit puts it back, a committed one calls {@link #onRootCommit}. Outside a
 * transaction (null) the change is final at once.
 *
 * <p>port-of: net.neoforged.neoforge.transfer.transaction.SnapshotJournal
 */
public abstract class SnapshotJournal<S> {
    private @Nullable TransactionContext open;

    protected abstract S createSnapshot();

    protected abstract void revertToSnapshot(S snapshot);

    protected void onRootCommit(S originalState) {
    }

    /** Call before every change: remembers the state the first time in a transaction. */
    protected void updateSnapshots(@Nullable TransactionContext transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("A journaled handler changes only inside a transaction");
        }
        if (open == transaction) {
            return;
        }
        open = transaction;
        S snapshot = createSnapshot();
        transaction.onCommit(() -> {
            open = null;
            onRootCommit(snapshot);
        });
        transaction.onAbort(() -> {
            open = null;
            revertToSnapshot(snapshot);
        });
    }
}
