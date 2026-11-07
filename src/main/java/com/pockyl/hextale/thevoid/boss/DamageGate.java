package com.pockyl.hextale.thevoid.boss;

/**
 * The bucket every hit passes, in the plan's order: study, Forgetting, a queued turn, the cap per hit, damage per second.
 * Hits are only charged to the bucket once they really landed ({@link #commit}), so one the hit cooldown swallowed is free.
 */
public final class DamageGate {
    /** What is let through when the bucket is full: never nothing, the hit still has to read as a hit. */
    public static final float TRICKLE = 0.1F;
    private float bucket;

    /** The bucket drains every tick, faster while Forgetting widens the window. */
    public void tick(float factor) {
        bucket = Math.max(0, bucket - ProsectorConfig.DPS_CAP.get().floatValue() * factor / 20);
    }

    public float bucket() {
        return bucket;
    }

    /** What a hit of {@code amount} becomes. Does not change the gate. */
    public float pass(float amount, float studyFactor, float forgetting, float pending) {
        float cap = ProsectorConfig.HIT_CAP.get().floatValue();
        float gated = Math.min(amount * studyFactor * forgetting * pending, cap);
        return Math.max(TRICKLE, Math.min(gated, cap - bucket));
    }

    public void commit(float passed) {
        bucket += passed;
    }

    public void clear() {
        bucket = 0;
    }
}
