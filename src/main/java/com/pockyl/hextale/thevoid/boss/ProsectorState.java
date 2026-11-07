package com.pockyl.hextale.thevoid.boss;

/**
 * What the Prosector is doing. One synced int carries this and the {@link Section}; the server owns the timing, the
 * client only reads the state, its section and when it began.
 */
public enum ProsectorState {
    /** Asleep where it was called, eye shut, waiting for someone to come near. */
    DORMANT,
    /** A Seer's Eye called it: the ground is marked ring by ring and it comes up through it. */
    RISING,
    /** The eye opens. */
    WAKING,
    /** Between moves. */
    HOVER,
    INCISION,
    PROBE,
    RETRACTOR,
    EXCISION,
    SECTION,
    RIPOSTE,
    VOLLEY,
    CHARGE,
    CIRCLE,
    /** A raised shield snagged the hooks. */
    REELING,
    /** A Specimen broke: low over the floor, eye open, every hit counts for more. */
    FORGETTING,
    TURN_DISSECTION,
    TURN_PROOF,
    /** High up behind the Proof, between the moves it makes through it. */
    PROVING,
    /** The Proof fell, and so did it. */
    FALLEN,
    DYING;

    private static final ProsectorState[] ALL = values();

    public static ProsectorState byId(int id) {
        return ALL[Math.floorMod(id, ALL.length)];
    }

    public boolean attacking() {
        return ordinal() >= INCISION.ordinal() && ordinal() <= CIRCLE.ordinal();
    }

    /** Nothing gets through in these. */
    public boolean shielded() {
        return this == DORMANT || this == RISING || this == WAKING || this == TURN_DISSECTION || this == TURN_PROOF || this == PROVING;
    }

    /** The windows the fight is built on: it stays low and does not answer. */
    public boolean open() {
        return this == REELING || this == FORGETTING || this == FALLEN;
    }

    /** The parts of an attack. */
    public enum Section {
        NONE, STARTUP, ACTIVE, RECOVERY;

        private static final Section[] ALL = values();

        public static Section byId(int id) {
            return ALL[Math.floorMod(id, ALL.length)];
        }
    }
}
