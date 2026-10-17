package com.pockyl.hextale.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * The hex pylons. A gathering pylon (a brass bowl) pulls items lying around into the container under it, a sending
 * pylon (a plate with claws) throws what the container under it gives out to its links, a relay (calcite, copper and
 * amethyst) passes items and essence on from pylon to pylon, a siphon pylon (a glass bulb on a jar) draws essence out of
 * the jar under it and pours it to its links.
 */
public enum PylonKind {
    GATHERING(9.0 / 16),
    SENDING(8.5 / 16),
    RELAY(15.0 / 16),
    SIPHON(7.5 / 16);

    /** How high the heart (gem, crystal, swirl) floats over the block's floor. */
    private final double gemHeight;

    PylonKind(double gemHeight) {
        this.gemHeight = gemHeight;
    }

    public Vec3 gem(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + gemHeight, pos.getZ() + 0.5);
    }

    /** Whether it works on the block it stands on (the relay stands anywhere). */
    public boolean standsOnContainer() {
        return this != RELAY;
    }

    /** Whether it carries essence rather than items. */
    public boolean essence() {
        return this == SIPHON;
    }
}
