package com.pockyl.hextale.block;

import net.minecraft.util.Mth;

/**
 * Client-side animation state of one Research Table: the spin of the gimbal rings and the orb's brightness. Speeds
 * ease towards their targets and the phases integrate them, so the rings speed up and slow down smoothly instead of
 * jumping when someone starts or stops drawing.
 */
public final class TableAnimation {
    private static final float IDLE_OUTER = 0.9F;
    private static final float IDLE_INNER = 1.6F;
    private static final float DRAWING_OUTER = 4.5F;
    private static final float DRAWING_INNER = 7.0F;

    /** Rotation of the outer ring around the vertical axis and of the inner ring around the outer ring's pivots, degrees. */
    public float outerPhase;
    public float innerPhase;
    /** Smoothed orb brightness, 0.5 idle and 1 while drawing. */
    public float glow = 0.5F;
    /**
     * The loupe arm, 0 at rest to 1 looking: the first third swings it over the parchment, the second and the last fold
     * its two hinged lenses over the main one; it runs back the same way.
     */
    public float loupe;
    private float outerSpeed = IDLE_OUTER;
    private float innerSpeed = IDLE_INNER;
    private float lastTime = Float.NaN;

    /** Advances the animation to {@code time} (game ticks with the partial tick). */
    public void update(float time, boolean drawing, float flare, boolean looking) {
        float dt = Float.isNaN(lastTime) ? 0 : Mth.clamp(time - lastTime, 0, 5);
        lastTime = time;
        float ease = Math.min(1, dt * 0.08F);
        float outerTarget = (drawing ? DRAWING_OUTER : IDLE_OUTER) + flare * 18;
        float innerTarget = (drawing ? DRAWING_INNER : IDLE_INNER) + flare * 26;
        outerSpeed += (outerTarget - outerSpeed) * ease;
        innerSpeed += (innerTarget - innerSpeed) * ease;
        outerPhase = (outerPhase + outerSpeed * dt) % 360;
        innerPhase = (innerPhase + innerSpeed * dt) % 360;
        glow += ((drawing ? 1.0F : 0.5F) - glow) * ease;
        // The loupe takes LOUPE_TICKS to swing over and fold its lenses, and goes back a little quicker.
        loupe = Mth.clamp(loupe + (looking ? dt : -dt * 1.5F) / LOUPE_TICKS, 0, 1);
    }

    /** How long the loupe arm takes from rest to looking with every lens folded in (as the screen's lenses). */
    public static final float LOUPE_TICKS = 27;
}
