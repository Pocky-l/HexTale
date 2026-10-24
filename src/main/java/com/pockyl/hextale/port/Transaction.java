package com.pockyl.hextale.port;

import java.util.ArrayList;
import java.util.List;

/**
 * A root transaction of the NeoForge 26.x transfer API over 1.21.1 item handlers: inside it the handlers only simulate
 * and remember the change; {@link #commit()} makes the remembered changes for real, closing without a commit drops them.
 * Enough for the mod's "trial, then move exactly that many" use (no nesting, one handler change per call).
 *
 * <p>port-of: net.neoforged.neoforge.transfer.transaction.Transaction
 */
public final class Transaction implements TransactionContext, AutoCloseable {
    private final List<Runnable> changes = new ArrayList<>();

    private Transaction() {
    }

    public static Transaction openRoot() {
        return new Transaction();
    }

    @Override
    public void onCommit(Runnable change) {
        changes.add(change);
    }

    public void commit() {
        List<Runnable> run = List.copyOf(changes);
        changes.clear();
        run.forEach(Runnable::run);
    }

    @Override
    public void close() {
        changes.clear();
    }
}
