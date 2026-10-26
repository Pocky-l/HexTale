package com.pockyl.hextale.thevoid;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * How the Void breaks through the world. Every shape is a blocky breach laid out on its own grid of cells
 * ({@link VoidLayout}): {@code size} blocks along its main axis (local y), {@code aspect} as wide. The client draws the
 * cells, the server only needs the same layout for particles and aiming.
 */
public enum VoidShape {
    /** A tall ragged doorway: rectangles of nothing laid over each other, teeth and loose cells around. */
    GATE("gate", 2.6F, 5.0F, 0.8F, 0.3F),
    /** A rounder breach with a blinding whirl in its heart. */
    EYE("eye", 2.2F, 4.6F, 0.95F, 1.0F),
    /** A narrow crack standing upright, stepping left and right as it climbs. */
    SLIT("slit", 2.0F, 4.2F, 0.32F, 0.5F),
    /** Space falling apart into several breaches clustered together. */
    SCATTER("scatter", 2.4F, 4.6F, 1.0F, 0.4F);

    private final String id;
    private final float minSize;
    private final float maxSize;
    private final float aspect;
    private final float core;

    VoidShape(String id, float minSize, float maxSize, float aspect, float core) {
        this.id = id;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.aspect = aspect;
        this.core = core;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "void_shape.hextale." + id;
    }

    /** Width over length. */
    public float aspect() {
        return aspect;
    }

    /** How bright the whirl deep inside burns, 0-1. */
    public float core() {
        return core;
    }

    public float randomSize(RandomSource random) {
        // Small breaches are commoner than huge ones.
        float t = random.nextFloat();
        return Mth.lerp(t * t, minSize, maxSize);
    }

    /** The shape of that id; an unknown one (such as the lying breach of old saves) stands as a gate. */
    public static VoidShape byId(String id) {
        for (VoidShape shape : values()) {
            if (shape.id.equals(id)) {
                return shape;
            }
        }
        return GATE;
    }

    public static VoidShape byOrdinal(int ordinal) {
        VoidShape[] all = values();
        return all[Math.floorMod(ordinal, all.length)];
    }
}
