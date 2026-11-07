package com.pockyl.hextale.port;

import java.util.ArrayList;
import java.util.List;

/**
 * A root transaction of the NeoForge 26.x transfer API over 1.21.1 handlers: inside it the handlers only simulate
 * (or note their changes in a {@link SnapshotJournal}); {@link #commit()} makes the changes for real, closing without
 * a commit drops them. A nested transaction ({@link #open}) hands what it did to its parent on a commit and undoes it alone
 * when closed without one.
 *
 * <p>port-of: net.neoforged.neoforge.transfer.transaction.Transaction
 */
public final class Transaction implements TransactionContext, AutoCloseable {
    private final List<Runnable> changes = new ArrayList<>();
    private final List<Runnable> aborts = new ArrayList<>();
    private final List<Runnable> closes = new ArrayList<>();
    private final java.util.Map<Object, Integer> promised = new java.util.HashMap<>();
    private final @org.jspecify.annotations.Nullable Transaction parent;
    private boolean committed;

    private Transaction(@org.jspecify.annotations.Nullable Transaction parent) {
        this.parent = parent;
    }

    public static Transaction openRoot() {
        return new Transaction(null);
    }

    public static Transaction open(TransactionContext parent) {
        return new Transaction((Transaction) parent);
    }

    @Override
    public void onCommit(Runnable change) {
        changes.add(change);
    }

    @Override
    public java.util.Map<Object, Integer> promised() {
        return parent != null ? parent.promised() : promised;
    }

    @Override
    public void onClose(Runnable end) {
        closes.add(end);
    }

    @Override
    public void onAbort(Runnable revert) {
        aborts.add(revert);
    }

    public void commit() {
        committed = true;
        closed();
        if (parent != null) {
            parent.changes.addAll(changes);
            parent.aborts.addAll(aborts);
            changes.clear();
            aborts.clear();
            return;
        }
        List<Runnable> run = List.copyOf(changes);
        changes.clear();
        aborts.clear();
        run.forEach(Runnable::run);
    }

    private void closed() {
        List<Runnable> run = List.copyOf(closes);
        closes.clear();
        run.forEach(Runnable::run);
    }

    @Override
    public void close() {
        changes.clear();
        if (!committed) {
            closed();
            List<Runnable> run = new ArrayList<>(aborts);
            aborts.clear();
            java.util.Collections.reverse(run);
            run.forEach(Runnable::run);
        }
    }
}
