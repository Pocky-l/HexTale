package com.pockyl.hextale.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.WhirlReaperBlockEntity;
import com.pockyl.hextale.essence.JarReach;

/**
 * Where a whirl is and how big at any tick of its life, shared by the renderer and the particles. It leaves the crystal
 * small and flies on an arc to the plant; on arrival it grows into a funnel standing on the plant (as tall as what it
 * takes), works over it, and when the harvest falls it widens, rises and thins out over {@link #FADE} ticks.
 */
public final class WhirlTimeline {
    public static final int FADE = 12;
    /** Ticks the whirl takes to grow from its flying size into the funnel. */
    public static final int GROW = 6;

    private WhirlTimeline() {
    }

    /** Where the whirl's foot stands over a plant: the bottom middle of its block. */
    public static Vec3 foot(BlockPos target) {
        return Vec3.atBottomCenterOf(target).add(0, 0.02, 0);
    }

    /** How tall the funnel stands: a crop's block, a lifted fruit, a whole stalk. */
    public static float height(WhirlReaperBlockEntity.Shown whirl) {
        return switch (whirl.kind()) {
            case CROP -> 1.1F;
            case GOURD -> 1.8F;
            case STALK -> whirl.column().size() + 1.6F;
        };
    }

    /** The whirl's foot at {@code age} ticks: on the arc while it flies, then on the plant. */
    public static Vec3 position(BlockPos reaper, WhirlReaperBlockEntity.Shown whirl, float age) {
        Vec3 foot = foot(whirl.target());
        if (age >= whirl.travel()) {
            return foot;
        }
        float u = smooth(age / whirl.travel());
        return JarReach.arc(WhirlReaperBlockEntity.crystal(reaper).add(0, -0.15, 0), foot, u);
    }

    /** Size of the funnel: 0.35 while flying, growing to 1 on arrival, a little wider while it thins out. */
    public static float size(WhirlReaperBlockEntity.Shown whirl, float age) {
        if (age < whirl.travel()) {
            return 0.35F;
        }
        float grown = Math.min(1, (age - whirl.travel()) / GROW);
        float fading = fading(whirl, age);
        return Mth.lerp(smooth(grown), 0.35F, 1) + 0.35F * fading;
    }

    /** 0 until the harvest falls, then up to 1 as the whirl thins out. */
    public static float fading(WhirlReaperBlockEntity.Shown whirl, float age) {
        return Mth.clamp((age - whirl.done()) / FADE, 0, 1);
    }

    /** 0-1 through the work over the plant. */
    public static float work(WhirlReaperBlockEntity.Shown whirl, float age) {
        return Mth.clamp((age - whirl.travel()) / whirl.kind().work(), 0, 1);
    }

    public static float smooth(float u) {
        u = Mth.clamp(u, 0, 1);
        return u * u * (3 - 2 * u);
    }

    /**
     * Client, every tick: crumbs of the plant whirling up the funnel while it works, and gusts of wind when it lands and
     * when the harvest falls.
     */
    public static void particles(Level level, WhirlReaperBlockEntity reaper, long now) {
        RandomSource random = level.getRandom();
        for (WhirlReaperBlockEntity.Shown whirl : reaper.shown) {
            float age = now - whirl.start();
            Vec3 foot = foot(whirl.target());
            float height = height(whirl);
            if ((int) age == whirl.travel()) {
                level.addParticle(ParticleTypes.SMALL_GUST, foot.x, foot.y + 0.4, foot.z, 0, 0, 0);
            }
            if ((int) age == whirl.done()) {
                level.addParticle(ParticleTypes.SMALL_GUST, foot.x, foot.y + height * 0.6, foot.z, 0, 0, 0);
                burst(level, whirl, foot, random);
            }
            if (age < whirl.travel() || age > whirl.done()) {
                continue;
            }
            BlockState crumbs = crumbs(level, whirl);
            if (crumbs == null) {
                continue;
            }
            for (int i = 0; i < 2; i++) {
                float t = random.nextFloat();
                float radius = 0.15F + 0.45F * t;
                float angle = random.nextFloat() * Mth.TWO_PI;
                double x = foot.x + Mth.cos(angle) * radius;
                double z = foot.z + Mth.sin(angle) * radius;
                // Swept round the funnel and up it.
                double vx = -Mth.sin(angle) * 0.18;
                double vz = Mth.cos(angle) * 0.18;
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, crumbs), x, foot.y + t * height * 0.7, z, vx, 0.12, vz);
            }
        }
    }

    /** What the whirl tears crumbs off: the crop itself, or the fruit / stalk it carries. */
    private static BlockState crumbs(Level level, WhirlReaperBlockEntity.Shown whirl) {
        if (whirl.kind() == WhirlKind.CROP) {
            BlockState state = level.getBlockState(whirl.target());
            return state.isAir() ? null : state;
        }
        return whirl.column().isEmpty() ? null : whirl.column().getFirst();
    }

    /** The harvest falls: a fruit bursts into crumbs where it hung, a crop sheds a ring of leaves. */
    private static void burst(Level level, WhirlReaperBlockEntity.Shown whirl, Vec3 foot, RandomSource random) {
        BlockState state = crumbs(level, whirl);
        if (state == null) {
            return;
        }
        double y = foot.y + (whirl.kind() == WhirlKind.GOURD ? 1.0 : 0.4);
        for (int i = 0; i < 18; i++) {
            float angle = i * Mth.TWO_PI / 18;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), foot.x + Mth.cos(angle) * 0.3, y,
                    foot.z + Mth.sin(angle) * 0.3, Mth.cos(angle) * 0.25, 0.1 + random.nextFloat() * 0.15, Mth.sin(angle) * 0.25);
        }
    }
}
