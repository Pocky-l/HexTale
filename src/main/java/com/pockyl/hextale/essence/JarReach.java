package com.pockyl.hextale.essence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Advanced machines (the Research Table, the Hex Smelter, ...) draw the essence they need straight out of the Essence
 * Jars around them, no pipes: within {@value #RADIUS} blocks every way. The jars are found by walking the block entities
 * of the few chunks the reach touches, never every block of the cube, and machines look only when they need essence.
 *
 * <p>Every draw is shown: the machine syncs which jar it draws from in its block entity data (never in a block event -
 * their parameter is a single byte) and its renderer draws the stream with {@code JarStreamFx}.
 */
public final class JarReach {
    public static final int RADIUS = 5;

    private JarReach() {
    }

    /** A jar that gave essence: where it stands and which essence the stream shows ({@code null} = the jar's mix colour). */
    public record Draw(BlockPos jar, @Nullable Essence essence) {
    }

    /** The jars within {@link #RADIUS} of a block, nearest first. */
    public static List<EssenceTankBlockEntity> around(Level level, BlockPos center) {
        return around(level, center, center);
    }

    /** The jars within {@link #RADIUS} of the box {@code min..max} (a machine of several blocks), nearest first. */
    public static List<EssenceTankBlockEntity> around(Level level, BlockPos min, BlockPos max) {
        int x0 = min.getX() - RADIUS;
        int y0 = min.getY() - RADIUS;
        int z0 = min.getZ() - RADIUS;
        int x1 = max.getX() + RADIUS;
        int y1 = max.getY() + RADIUS;
        int z1 = max.getZ() + RADIUS;
        List<EssenceTankBlockEntity> jars = new ArrayList<>();
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos pos = entity.getBlockPos();
                    if (entity instanceof EssenceTankBlockEntity jar && !jar.isRemoved()
                            && pos.getX() >= x0 && pos.getX() <= x1 && pos.getY() >= y0 && pos.getY() <= y1
                            && pos.getZ() >= z0 && pos.getZ() <= z1) {
                        jars.add(jar);
                    }
                }
            }
        }
        // The chunk maps have no order: sort, so the same jar is drawn first every time.
        Vec3 middle = new Vec3((min.getX() + max.getX()) / 2.0, (min.getY() + max.getY()) / 2.0, (min.getZ() + max.getZ()) / 2.0);
        jars.sort(Comparator.<EssenceTankBlockEntity>comparingDouble(jar -> jar.getBlockPos().distToLowCornerSqr(middle.x, middle.y, middle.z))
                .thenComparingLong(jar -> jar.getBlockPos().asLong()));
        return jars;
    }

    /** Where a stream leaves a jar: just above its lid. */
    public static Vec3 mouth(BlockPos jar) {
        return Vec3.atLowerCornerOf(jar).add(0.5, 1.02, 0.5);
    }

    /** A point of the stream's arc from {@code from} to {@code to} at {@code t} (0-1): it rises with the distance. */
    public static Vec3 arc(Vec3 from, Vec3 to, float t) {
        Vec3 control = from.add(to).scale(0.5).add(0, 0.45 + 0.16 * from.distanceTo(to), 0);
        float u = 1 - t;
        return from.scale(u * u).add(control.scale(2 * u * t)).add(to.scale(t * t));
    }

    /**
     * Client: living essence shed by a stream ({@code from}/{@code to} in world space, drawn between {@code tail} and
     * {@code head}) - now and then a drop slips off the arc and floats away, and the jar breathes a drop out of its lid
     * while it is being drawn. Call every client tick.
     */
    public static void shed(Level level, Vec3 from, Vec3 to, int color, float head, float tail) {
        if (head <= tail) {
            return;
        }
        var random = level.getRandom();
        ColorParticleOption wisp = ModParticles.wisp(color);
        if (random.nextInt(9) == 0) {
            Vec3 at = arc(from, to, tail + random.nextFloat() * (head - tail));
            Vec3 next = arc(from, to, Math.min(1, tail + random.nextFloat() * (head - tail) + 0.05F));
            Vec3 drift = next.subtract(at).normalize().scale(0.01);
            level.addParticle(wisp, at.x, at.y, at.z, drift.x, drift.y + 0.004, drift.z);
        }
        if (tail <= 0 && random.nextInt(10) == 0) {
            level.addParticle(wisp, from.x + (random.nextDouble() - 0.5) * 0.3, from.y - 0.08, from.z + (random.nextDouble() - 0.5) * 0.3,
                    0, 0.005, 0);
        }
    }

    /** The colour a stream from this jar shows, or -1 when there is nothing to show (no jar, or it is empty). */
    public static int color(Level level, Draw draw) {
        if (draw.essence() != null) {
            return draw.essence().color();
        }
        return level.getBlockEntity(draw.jar()) instanceof EssenceTankBlockEntity jar && !jar.tank().isEmpty()
                ? jar.tank().contents().color() : -1;
    }

    // ---- Syncing draws in block entity data -----------------------------------------------------------------------------

    /** Draws as an int array for an update tag: x, y, z, essence ordinal + 1 (0 = mix colour) per draw. */
    public static int[] write(List<Draw> draws) {
        int[] out = new int[draws.size() * 4];
        for (int i = 0; i < draws.size(); i++) {
            Draw draw = draws.get(i);
            out[i * 4] = draw.jar().getX();
            out[i * 4 + 1] = draw.jar().getY();
            out[i * 4 + 2] = draw.jar().getZ();
            out[i * 4 + 3] = draw.essence() == null ? 0 : draw.essence().ordinal() + 1;
        }
        return out;
    }

    public static List<Draw> read(int[] data) {
        List<Draw> draws = new ArrayList<>();
        for (int i = 0; i + 3 < data.length; i += 4) {
            int essence = data[i + 3] - 1;
            draws.add(new Draw(new BlockPos(data[i], data[i + 1], data[i + 2]),
                    essence >= 0 && essence < Essence.values().length ? Essence.values()[essence] : null));
        }
        return draws;
    }
}
