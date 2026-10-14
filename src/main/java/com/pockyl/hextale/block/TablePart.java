package com.pockyl.hextale.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * The two blocks of the Research Table. The main block is the left half as seen from the front and holds the block
 * entity; the side block is the right half.
 */
public enum TablePart implements StringRepresentable {
    MAIN("main"),
    SIDE("side");

    private final String name;

    TablePart(String name) {
        this.name = name;
    }

    /** Direction from this half to the other one for a desk facing {@code facing}. */
    public Direction toOther(Direction facing) {
        return this == MAIN ? facing.getCounterClockWise() : facing.getClockWise();
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
