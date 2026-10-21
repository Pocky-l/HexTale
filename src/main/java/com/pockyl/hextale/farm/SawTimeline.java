package com.pockyl.hextale.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.block.HexSawmillBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * Where a Hex Sawmill's saw is and how far its tree has fallen at any tick, shared by the renderer and the particles.
 * The saw leaves the hub and flies on an arc to the trunk's side facing the mill; it saws through, sawdust and sparks
 * flying; the tree tips over away from the mill about the edge of its foot, slowly and then faster, and breaks apart into
 * crumbs where it hits the ground.
 */
public final class SawTimeline {
    /** Ticks the saw glows out after the tree is down. */
    public static final int FADE = 10;

    private SawTimeline() {
    }

    /** Where the saw cuts: low on the trunk, on the side facing the mill. */
    public static Vec3 cutPoint(BlockPos mill, HexSawmillBlockEntity.Shown saw) {
        Direction toward = saw.away().getOpposite();
        return Vec3.atBottomCenterOf(saw.foot()).add(toward.getStepX() * 0.62, 0.35, toward.getStepZ() * 0.62);
    }

    /** The saw at {@code age} ticks: on the arc while it flies, at the cut while it saws, then rising back a little. */
    public static Vec3 position(BlockPos mill, HexSawmillBlockEntity.Shown saw, float age) {
        Vec3 cut = cutPoint(mill, saw);
        if (age >= saw.travel()) {
            float after = Math.max(0, age - saw.cut());
            return cut.add(0, after * 0.03, 0);
        }
        float u = age / saw.travel();
        u = u * u * (3 - 2 * u);
        return JarReach.arc(HexSawmillBlockEntity.hub(mill), cut, u);
    }

    /** How far the tree has turned over (radians, 0 standing - pi/2 on the ground), or -1 while it still stands. */
    public static float fall(HexSawmillBlockEntity.Shown saw, float age) {
        if (age < saw.cut()) {
            return -1;
        }
        float u = Mth.clamp((age - saw.cut()) / HexSawmillBlockEntity.FALL_TICKS, 0, 1);
        return Mth.HALF_PI * u * u;
    }

    /** The edge of the foot the tree turns over. */
    public static Vec3 pivot(HexSawmillBlockEntity.Shown saw) {
        return Vec3.atBottomCenterOf(saw.foot()).add(saw.away().getStepX() * 0.5, 0, saw.away().getStepZ() * 0.5);
    }

    /** The turn that tips the tree over by {@code angle}: up goes towards the side it falls to. */
    public static Quaternionf turn(HexSawmillBlockEntity.Shown saw, float angle) {
        Vector3f axis = new Vector3f(0, 1, 0).cross(new Vector3f(saw.away().getStepX(), 0, saw.away().getStepZ()));
        return new Quaternionf().rotateAxis(angle, axis);
    }

    /** Where a point of the standing tree is once the tree has turned by {@code angle}. */
    public static Vec3 turned(HexSawmillBlockEntity.Shown saw, Vec3 point, float angle) {
        Vec3 pivot = pivot(saw);
        Vector3f r = new Vector3f((float) (point.x - pivot.x), (float) (point.y - pivot.y), (float) (point.z - pivot.z));
        turn(saw, angle).transform(r);
        return pivot.add(r.x, r.y, r.z);
    }

    /** Client, every tick: the sawing's sound, sawdust and sparks; the fallen tree breaking into crumbs. */
    public static void particles(Level level, HexSawmillBlockEntity mill, long now) {
        RandomSource random = level.getRandom();
        BlockPos pos = mill.getBlockPos();
        for (HexSawmillBlockEntity.Shown saw : mill.shown) {
            int age = (int) (now - saw.start());
            Vec3 cut = cutPoint(pos, saw);
            if (age == saw.travel()) {
                level.playLocalSound(cut.x, cut.y, cut.z, ModSounds.SAW_CUT.get(), SoundSource.BLOCKS, 0.7F,
                        0.9F + random.nextFloat() * 0.2F, false);
            }
            if (age >= saw.travel() && age < saw.cut() && !saw.states().isEmpty()) {
                BlockState log = saw.states().getFirst();
                Direction toward = saw.away().getOpposite();
                for (int i = 0; i < 3; i++) {
                    double side = (random.nextDouble() - 0.5) * 0.5;
                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, log), cut.x + toward.getStepZ() * side, cut.y,
                            cut.z + toward.getStepX() * side, toward.getStepX() * 0.15 + (random.nextDouble() - 0.5) * 0.2,
                            0.12 + random.nextDouble() * 0.1, toward.getStepZ() * 0.15 + (random.nextDouble() - 0.5) * 0.2);
                }
                if (random.nextInt(2) == 0) {
                    level.addParticle(ModParticles.spark(ModParticles.HEXTECH), cut.x, cut.y, cut.z, (random.nextDouble() - 0.5) * 0.4, 0.15,
                            (random.nextDouble() - 0.5) * 0.4);
                }
            }
            if (age == saw.done() && !saw.blocks().isEmpty()) {
                for (int i = 0; i < 48; i++) {
                    int k = random.nextInt(saw.blocks().size());
                    Vec3 at = turned(saw, Vec3.atCenterOf(saw.blocks().get(k)), Mth.HALF_PI);
                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, saw.states().get(k)), at.x, at.y, at.z,
                            (random.nextDouble() - 0.5) * 0.3, 0.2 + random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.3);
                }
                Vec3 foot = Vec3.atBottomCenterOf(saw.foot());
                level.addParticle(ParticleTypes.SMALL_GUST, foot.x, foot.y + 0.5, foot.z, 0, 0, 0);
            }
        }
    }
}
