package com.pockyl.hextale.client.render;

import net.minecraft.util.Mth;

/** Colour helpers shared by the renderers. */
public final class RuneDrawing {
    private RuneDrawing() {
    }

    public static int argb(int rgb, float alpha) {
        return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    public static int darken(int rgb, float factor) {
        return mix(rgb, 0x1A120A, 1 - factor);
    }

    public static int lighten(int rgb, float amount) {
        return mix(rgb, 0xFFFFFF, amount);
    }

    /** Blends {@code a} towards {@code b} by {@code t} (0 = a, 1 = b). */
    public static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, a >> 16 & 0xFF, b >> 16 & 0xFF);
        int g = (int) Mth.lerp(t, a >> 8 & 0xFF, b >> 8 & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return r << 16 | g << 8 | bl;
    }
}
