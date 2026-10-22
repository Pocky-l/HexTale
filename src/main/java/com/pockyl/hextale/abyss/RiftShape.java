package com.pockyl.hextale.abyss;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/**
 * How an abyss rift tears through the world. Every shape is laid out along its own main axis (local y, {@code size}
 * blocks long) on a plane; the client builds the jagged mesh from the rift's seed, the server only needs the size and
 * a rough idea of where the opening lies (particles, aiming at a rift).
 */
public enum RiftShape {
    /** A single slash in the air, slightly bent, a lip torn here and there. */
    TEAR("tear", 1.6F, 4.2F, false, 0.16F),
    /** A long crack lying on the ground: the floor opens into the abyss. */
    FISSURE("fissure", 3.0F, 8.0F, true, 0.10F),
    /** Space broken like glass: arms of cracks running out of a torn hub. */
    SHATTER("shatter", 1.8F, 4.6F, false, 0.10F),
    /** A wide gaping wound that opens like an eye. */
    MAW("maw", 2.0F, 5.5F, false, 0.27F),
    /** A long cleft that forks like frozen lightning. */
    CLEFT("cleft", 3.0F, 7.0F, false, 0.12F);

    private final String id;
    private final float minSize;
    private final float maxSize;
    private final boolean lying;
    /** Half the opening's width at its widest, as a share of the size. */
    private final float width;

    RiftShape(String id, float minSize, float maxSize, boolean lying, float width) {
        this.id = id;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.lying = lying;
        this.width = width;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "rift_shape.hextale." + id;
    }

    /** Lies flat on the ground instead of standing in the air. */
    public boolean lying() {
        return lying;
    }

    public float minSize() {
        return minSize;
    }

    public float maxSize() {
        return maxSize;
    }

    /** Half the widest opening in blocks for a rift of {@code size}. */
    public float halfWidth(float size) {
        return size * width;
    }

    public float randomSize(RandomSource random) {
        // Small rifts are commoner than huge ones.
        float t = random.nextFloat();
        return Mth.lerp(t * t, minSize, maxSize);
    }

    /**
     * A point somewhere in the opening (rift-local blocks, plane z = 0), roughly following the shape: what particles
     * aim at. {@code rim} pushes it out to the torn edge.
     */
    public Vector3f samplePoint(RandomSource random, float size, boolean rim) {
        float half = size / 2;
        switch (this) {
            case SHATTER -> {
                float angle = random.nextFloat() * Mth.TWO_PI;
                float r = half * (rim ? 0.25F + 0.65F * random.nextFloat() : 0.7F * random.nextFloat() * random.nextFloat());
                return new Vector3f(Mth.cos(angle) * r, Mth.sin(angle) * r, 0);
            }
            case MAW -> {
                float angle = random.nextFloat() * Mth.TWO_PI;
                float r = rim ? 0.85F + 0.1F * random.nextFloat() : (float) Math.sqrt(random.nextFloat()) * 0.8F;
                return new Vector3f(Mth.cos(angle) * halfWidth(size) * r, Mth.sin(angle) * half * r, 0);
            }
            default -> {
                float along = (random.nextFloat() * 2 - 1) * 0.85F;
                float across = halfWidth(size) * Mth.sin((1 - Math.abs(along)) * Mth.HALF_PI)
                        * (rim ? (random.nextBoolean() ? 0.9F : -0.9F) : (random.nextFloat() * 2 - 1) * 0.6F);
                return new Vector3f(across, along * half, 0);
            }
        }
    }

    public static RiftShape byId(String id) {
        for (RiftShape shape : values()) {
            if (shape.id.equals(id)) {
                return shape;
            }
        }
        return TEAR;
    }

    public static RiftShape byOrdinal(int ordinal) {
        RiftShape[] all = values();
        return all[Math.floorMod(ordinal, all.length)];
    }
}
