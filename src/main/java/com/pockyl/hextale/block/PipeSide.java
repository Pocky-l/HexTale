package com.pockyl.hextale.block;

import net.minecraft.util.StringRepresentable;

/**
 * What one side of a Brass Pipe does. As a block state value it is also the arm the side shows: nothing, a plain arm,
 * an arm with a green collar (into the block there), an arm with a blue collar (out of it) or a capped stub.
 */
public enum PipeSide implements StringRepresentable {
    /** Nothing to join there. */
    NONE("none"),
    /** Joined, liquid goes both ways (what the blocks on both ends let through). */
    PIPE("pipe"),
    /** Joined to a vessel that only takes from the pipe here: nothing comes out of it through this side. */
    IN("in"),
    /** Joined to a vessel that only gives to the pipe here; a jar is pumped out actively. */
    OUT("out"),
    /** Shut off by hand: a capped stub, nothing joins. */
    CLOSED("closed");

    private final String name;

    PipeSide(String name) {
        this.name = name;
    }

    /** Whether the side is joined at all. */
    public boolean open() {
        return this == PIPE || this == IN || this == OUT;
    }

    /** Whether liquid may run from the pipe into the block on this side. */
    public boolean intoBlock() {
        return this == PIPE || this == IN;
    }

    /** Whether liquid may run from the block on this side into the pipe. */
    public boolean outOfBlock() {
        return this == PIPE || this == OUT;
    }

    /** The next mode a sneak-click sets: a side towards a vessel cycles all four, one towards a pipe opens or shuts. */
    public PipeSide next(boolean towardsPipe) {
        if (towardsPipe) {
            return this == CLOSED ? PIPE : CLOSED;
        }
        return switch (this) {
            case PIPE -> IN;
            case IN -> OUT;
            case OUT -> CLOSED;
            case CLOSED, NONE -> PIPE;
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
