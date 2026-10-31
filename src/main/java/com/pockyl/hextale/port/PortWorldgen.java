package com.pockyl.hextale.port;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Stands in for 26.x {@code RandomState#seed()}: 1.21.1's random state does not keep the level seed; a
 * value drawn from its seeded random factory stands in (same world seed, same value).
 */
public final class PortWorldgen {
    private static final ResourceLocation SEED = ResourceLocation.fromNamespaceAndPath("hextale", "void_shapes");

    private PortWorldgen() {
    }

    public static long seed(RandomState random) {
        return random.getOrCreateRandomFactory(SEED).at(0, 0, 0).nextLong();
    }
}
