package com.pockyl.hextale.farm;

/**
 * How a Whirl Reaper's whirl takes a ripe plant. The kind decides the whole timeline: how long the whirl works over the
 * plant and what the clients show it doing.
 */
public enum WhirlKind {
    /** A crop, wart, bean pod or berry bush: the whirl strips it where it stands and it starts growing again. */
    CROP(24),
    /** A melon or pumpkin on its stem: the whirl lifts the fruit off, spins it and breaks it open where it lay. */
    GOURD(30),
    /** Sugar cane, cactus, bamboo: the whirl twists off everything above the lowest block and drops it beside it. */
    STALK(30);

    /** Ticks the whirl works over the plant before the harvest falls. */
    private final int work;

    WhirlKind(int work) {
        this.work = work;
    }

    public int work() {
        return work;
    }

    /** Whether the whirl takes the plant up into itself on arrival (a fruit or the stalk's top), not reaping it in place. */
    public boolean lifts() {
        return this != CROP;
    }

    public static WhirlKind byIndex(int index) {
        WhirlKind[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
