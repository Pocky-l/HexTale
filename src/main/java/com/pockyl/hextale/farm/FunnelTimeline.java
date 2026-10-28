package com.pockyl.hextale.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.GreedyFunnelBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.rune.Essence;

import org.jspecify.annotations.Nullable;

/**
 * Client: how a swallowed death is drawn into a Greedy Funnel, shared by its renderer and its particles. Every drop
 * shown ({@link GreedyFunnelBlockEntity.Shown#icons()}) leaves a little after the one before it: it is plucked up from
 * where the creature died ({@link #RISE} ticks, turning), pulled along an arc to the mouth ({@link #FLY}) and sinks into
 * the bowl ({@link #SINK}), trailing shadow. The experience follows as a string of motes into the mouth.
 */
public final class FunnelTimeline {
    public static final int RISE = 8;
    public static final int FLY = 16;
    public static final int SINK = 6;
    /** Ticks between two drops leaving. */
    public static final int STAGGER = 3;
    public static final int MOTE_TICKS = 18;
    /** Ticks a death is kept for (the last drop and the last mote are in by then). */
    public static final int TOTAL = RISE + FLY + SINK + STAGGER * 3 + 4;
    /** Shadow, the colour of what the funnel draws in. */
    public static final int SHADOW = Essence.SHADOW.color();
    /** Experience, green-gold like the vanilla orbs. */
    public static final int EXPERIENCE = 0xC6F25A;

    private FunnelTimeline() {
    }

    public static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** How many motes carry this much experience. */
    public static int motes(int experience) {
        return experience <= 0 ? 0 : Math.min(10, 2 + experience / 3);
    }

    /** Where the {@code index}-th drop is plucked up to: a little over the spot, spread round it. */
    private static Vec3 plucked(Vec3 from, int index, int seed) {
        float angle = index * 2.4F + (seed & 15) * 0.4F;
        return from.add(Mth.cos(angle) * 0.35, 0.55 + 0.1 * index, Mth.sin(angle) * 0.35);
    }

    /** The {@code index}-th drop's place at {@code age} ticks (world space), or null before it shows or after it sank. */
    @Nullable
    public static Vec3 drop(BlockPos funnel, GreedyFunnelBlockEntity.Shown death, int index, float age) {
        float a = age - index * STAGGER;
        if (a < 0 || a > RISE + FLY + SINK) {
            return null;
        }
        Vec3 from = death.from();
        Vec3 up = plucked(from, index, death.seed());
        Vec3 mouth = GreedyFunnelBlockEntity.mouth(funnel);
        Vec3 over = mouth.add(0, 0.45, 0);
        if (a < RISE) {
            return from.lerp(up, smooth(a / RISE));
        }
        if (a < RISE + FLY) {
            float u = smooth((a - RISE) / FLY);
            Vec3 at = JarReach.arc(up, over, u);
            // It spirals in as it nears the mouth.
            float swirl = (1 - u) * u * 0.6F;
            float angle = a * 0.55F + index;
            return at.add(Mth.cos(angle) * swirl, 0, Mth.sin(angle) * swirl);
        }
        return over.lerp(mouth.add(0, -0.15, 0), smooth((a - RISE - FLY) / SINK));
    }

    /** The drop's size (1 = an item on the ground) at {@code age}. */
    public static float dropScale(int index, float age) {
        float a = age - index * STAGGER;
        if (a < RISE) {
            return 0.6F + 0.4F * smooth(a / RISE);
        }
        if (a < RISE + FLY) {
            return 1 - 0.35F * smooth((a - RISE) / FLY);
        }
        return 0.65F * (1 - smooth((a - RISE - FLY) / SINK));
    }

    /** The {@code index}-th experience mote's place, or null when it is not flying. */
    @Nullable
    public static Vec3 mote(BlockPos funnel, GreedyFunnelBlockEntity.Shown death, int index, float age) {
        float a = age - 4 - index * 1.5F;
        if (a < 0 || a > MOTE_TICKS) {
            return null;
        }
        float angle = index * 2.1F + (death.seed() & 7);
        Vec3 from = death.from().add(Mth.cos(angle) * 0.25, 0.2 + index * 0.04, Mth.sin(angle) * 0.25);
        return JarReach.arc(from, GreedyFunnelBlockEntity.mouth(funnel), smooth(a / MOTE_TICKS));
    }

    /**
     * Client, every tick: a breath of shadow where the creature died, a thread of it behind every flying drop, a puff in
     * the mouth as each one sinks.
     */
    public static void particles(Level level, GreedyFunnelBlockEntity funnel, long now) {
        RandomSource random = level.getRandom();
        BlockPos pos = funnel.getBlockPos();
        var wisp = ModParticles.wisp(SHADOW);
        for (GreedyFunnelBlockEntity.Shown death : funnel.shown) {
            float age = now - death.start();
            Vec3 from = death.from();
            if (age < 1) {
                for (int i = 0; i < 10; i++) {
                    float angle = i * Mth.TWO_PI / 10;
                    level.addParticle(wisp, from.x + Mth.cos(angle) * 0.3, from.y, from.z + Mth.sin(angle) * 0.3,
                            -Mth.cos(angle) * 0.02, 0.03 + random.nextFloat() * 0.03, -Mth.sin(angle) * 0.02);
                }
            }
            for (int i = 0; i < death.icons().size(); i++) {
                Vec3 at = drop(pos, death, i, age);
                if (at != null && random.nextInt(2) == 0) {
                    level.addParticle(wisp, at.x, at.y, at.z, 0, 0.005, 0);
                }
                if ((int) (age - i * STAGGER) == RISE + FLY + SINK - 1) {
                    Vec3 mouth = GreedyFunnelBlockEntity.mouth(pos);
                    for (int k = 0; k < 6; k++) {
                        float angle = k * Mth.TWO_PI / 6 + random.nextFloat();
                        level.addParticle(wisp, mouth.x + Mth.cos(angle) * 0.25, mouth.y + 0.05, mouth.z + Mth.sin(angle) * 0.25,
                                Mth.cos(angle) * 0.015, 0.02, Mth.sin(angle) * 0.015);
                    }
                }
            }
        }
    }
}
