package com.pockyl.hextale.port;

/**
 * An open transaction of the NeoForge 26.x transfer API (see {@link Transaction}).
 *
 * <p>port-of: net.neoforged.neoforge.transfer.transaction.TransactionContext
 */
public interface TransactionContext {
    /** Runs the real change when the transaction commits; until then the handlers only simulate. */
    void onCommit(Runnable change);

    /** Runs when the transaction closes without a commit. */
    void onAbort(Runnable revert);
}
