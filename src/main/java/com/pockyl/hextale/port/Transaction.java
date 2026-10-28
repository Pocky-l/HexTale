package com.pockyl.hextale.port;

import java.util.ArrayList;
import java.util.List;

/**
 * A root transaction of the NeoForge 26.x transfer API over 1.21.1 handlers: inside it the handlers only simulate
 * (or note their changes in a {@link SnapshotJournal}); {@link #commit()} makes the changes for real, closing without
 * a commit drops them. Enough for the mod's uses (no nesting).
 *
 * <p>port-of: net.neoforged.neoforge.transfer.transaction.Transaction
 */
public final class Transaction implements TransactionContext, AutoCloseable {
    private final List<Runnable> changes = new ArrayList<>();
    private final List<Runnable> aborts = new ArrayList<>();
    private boolean committed;

    private Transaction() {
    }

    public static Transaction openRoot() {
        return new Transaction();
    }

    @Override
    public void onCommit(Runnable change) {
        changes.add(change);
    }

    @Override
    public void onAbort(Runnable revert) {
        aborts.add(revert);
    }

    public void commit() {
        committed = true;
        List<Runnable> run = List.copyOf(changes);
        changes.clear();
        aborts.clear();
        run.forEach(Runnable::run);
    }

    @Override
    public void close() {
        changes.clear();
        if (!committed) {
            List<Runnable> run = List.copyOf(aborts);
            aborts.clear();
            run.forEach(Runnable::run);
        }
    }
}
