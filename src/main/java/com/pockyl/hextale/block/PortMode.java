package com.pockyl.hextale.block;

import net.minecraft.util.StringRepresentable;

/**
 * What a Separator face does with a pipe: takes the mixture in, lets the wanted product (the concentrate) out, or the
 * by-product (the rest of the mixture). As a block state value it is what the face shows: {@link #NONE} without a
 * pipe, else the joint of its mode.
 */
public enum PortMode implements StringRepresentable {
    NONE("none"),
    INLET("inlet"),
    PRODUCT("product"),
    WASTE("waste");

    /** The modes a face cycles through in the Separator's screen. */
    public static final PortMode[] CYCLE = {INLET, PRODUCT, WASTE};

    private final String name;

    PortMode(String name) {
        this.name = name;
    }

    public PortMode next() {
        return this == INLET ? PRODUCT : this == PRODUCT ? WASTE : INLET;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
