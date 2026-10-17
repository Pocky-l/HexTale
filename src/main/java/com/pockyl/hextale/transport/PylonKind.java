package com.pockyl.hextale.transport;

import net.minecraft.world.phys.Vec3;

/**
 * The three hex pylons. A gathering pylon pulls items lying around into the container under it, a sending pylon takes
 * what the container under it gives out and throws it to its links, a relay passes items on from pylon to pylon.
 */
public enum PylonKind {
    GATHERING(14.5 / 16),
    SENDING(14.0 / 16),
    RELAY(15.0 / 16);

    /** How high the gem floats over the block's floor. */
    private final double gemHeight;

    PylonKind(double gemHeight) {
        this.gemHeight = gemHeight;
    }

    public Vec3 gem(net.minecraft.core.BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + gemHeight, pos.getZ() + 0.5);
    }

    /** Whether it works on the container it stands on (the relay stands anywhere). */
    public boolean standsOnContainer() {
        return this != RELAY;
    }
}
