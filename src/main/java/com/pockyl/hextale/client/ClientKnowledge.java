package com.pockyl.hextale.client;

import com.pockyl.hextale.knowledge.RuneKnowledge;

/**
 * The local player's knowledge as last sent by the server. Kept outside the player entity, which the client replaces
 * on respawn and dimension change.
 */
public final class ClientKnowledge {
    private static RuneKnowledge current = new RuneKnowledge();
    private static int version;

    private ClientKnowledge() {
    }

    public static RuneKnowledge get() {
        return current;
    }

    /** Increases with every update, so screens can tell when to rebuild. */
    public static int version() {
        return version;
    }

    static void set(RuneKnowledge knowledge) {
        current = knowledge;
        version++;
    }

    static void clear() {
        set(new RuneKnowledge());
    }
}
