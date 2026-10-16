package com.pockyl.hextale.block;

import net.minecraft.util.StringRepresentable;

/**
 * What a Separator side does with a pipe: takes the mixture in, lets the wanted product (the concentrate) out, or the
 * by-product (the rest of the mixture) - or nothing at all ({@link #NONE}: closed, no joint, no pipe joins it). As a
 * block state value it is the joint the side shows.
 */
public enum PortMode implements StringRepresentable {
    NONE("none"),
    INLET("inlet"),
    PRODUCT("product"),
    WASTE("waste");

    /** The modes a side cycles through. */
    public static final PortMode[] CYCLE = {INLET, PRODUCT, WASTE, NONE};

    private final String name;

    PortMode(String name) {
        this.name = name;
    }

    public PortMode next() {
        return switch (this) {
            case INLET -> PRODUCT;
            case PRODUCT -> WASTE;
            case WASTE -> NONE;
            case NONE -> INLET;
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
