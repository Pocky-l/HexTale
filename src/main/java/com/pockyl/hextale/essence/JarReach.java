package com.pockyl.hextale.essence;

import net.minecraft.core.BlockPos;
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
 * Every draw is shown as a stream of motes from the jar into the machine: the server sends a block event with
 * {@link #pack}, the machine keeps a {@link Streams} and ticks it on the client.
 */
public final class JarReach {
    public static final int RADIUS = 5;
    /** How long a draw's stream runs, in ticks. */
    public static final int STREAM_TICKS = 20;

    private JarReach() {
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

    // ---- Streams --------------------------------------------------------------------------------------------------------

    /**
     * A draw as a block event parameter: the jar's offset from the machine (5 bits per axis) and the essence the stream
     * shows ({@code null} = the colour of whatever is in the jar).
     */
    public static int pack(BlockPos machine, BlockPos jar, @Nullable Essence essence) {
        int dx = jar.getX() - machine.getX() + 16;
        int dy = jar.getY() - machine.getY() + 16;
        int dz = jar.getZ() - machine.getZ() + 16;
        return dx | dy << 5 | dz << 10 | (essence == null ? 0 : essence.ordinal() + 1) << 15;
    }

    /** Client side: the streams of a machine's recent draws. */
    public static final class Streams {
        private record Stream(BlockPos jar, @Nullable Essence essence, long until) {
        }

        private final List<Stream> streams = new ArrayList<>();

        /** A draw arrived ({@link #pack}ed by the server). */
        public void start(Level level, BlockPos machine, int param) {
            BlockPos jar = machine.offset((param & 31) - 16, (param >> 5 & 31) - 16, (param >> 10 & 31) - 16);
            int essence = (param >> 15) - 1;
            Essence shown = essence >= 0 && essence < Essence.values().length ? Essence.values()[essence] : null;
            streams.removeIf(stream -> stream.jar.equals(jar));
            streams.add(new Stream(jar, shown, level.getGameTime() + STREAM_TICKS));
        }

        /** Sends motes from every drawing jar into {@code into}; call every client tick. */
        public void tick(Level level, Vec3 into) {
            if (streams.isEmpty()) {
                return;
            }
            long now = level.getGameTime();
            streams.removeIf(stream -> now >= stream.until);
            var random = level.getRandom();
            for (Stream stream : streams) {
                int color;
                if (stream.essence != null) {
                    color = stream.essence.color();
                } else if (level.getBlockEntity(stream.jar) instanceof EssenceTankBlockEntity jar && !jar.tank().isEmpty()) {
                    color = jar.tank().contents().color();
                } else {
                    continue;
                }
                // Out of the liquid under the lid, a little apart each time, so the motes make a ribbon, not a line.
                Vec3 from = Vec3.atBottomCenterOf(stream.jar).add((random.nextDouble() - 0.5) * 0.35, 0.55 + random.nextDouble() * 0.2,
                        (random.nextDouble() - 0.5) * 0.35);
                Vec3 offset = from.subtract(into);
                level.addParticle(ModParticles.flow(color), into.x, into.y, into.z, offset.x, offset.y, offset.z);
            }
        }
    }
}
