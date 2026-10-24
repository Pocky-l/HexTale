package com.pockyl.hextale.port;

import net.minecraft.core.Vec3i;

/** Small math helpers Minecraft 26.x added. */
public final class PortMath {
    private PortMath() {
    }

    /** {@code a.distChessboard(b)}: the largest of the axis distances. */
    public static int distChessboard(Vec3i a, Vec3i b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
    }
}
