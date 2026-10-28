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

    /** 26.x {@code Mth.length(float, float)} (1.21.1 only has the double one). */
    public static float length(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    public static double length(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }
}
