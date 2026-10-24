package com.pockyl.hextale.transport;

import java.util.UUID;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * Who is putting an item into a block right now, when it is not a player at a screen: the owner of the hex pylon that
 * delivers it. Machines whose work depends on the researcher's knowledge (the Alembic boils only what its boiler has
 * studied) ask {@link #current()} while the item goes in.
 */
public final class Deliverer {
    /** Java 21 has scoped values only as a preview, so 1.21.1 keeps the deliverer in a thread local. */
    private static final ThreadLocal<UUID> WHO = new ThreadLocal<>();

    private Deliverer() {
    }

    /** Runs the insertion on behalf of {@code player} (nobody when null). */
    public static <T> T as(@Nullable UUID player, Supplier<T> insertion) {
        if (player == null) {
            return insertion.get();
        }
        UUID before = WHO.get();
        WHO.set(player);
        try {
            return insertion.get();
        } finally {
            WHO.set(before);
        }
    }

    /** The player an item is being delivered for, or null. */
    @Nullable
    public static UUID current() {
        return WHO.get();
    }
}
