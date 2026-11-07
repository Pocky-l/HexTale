package com.pockyl.hextale.thevoid.boss;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The cells Section has opened. The fight happens in the player's own world, so no block is touched: a cut is a wound of
 * light laid on the ground, and whoever stands on it is hurt and thrown off it. Lives in the Prosector and is not saved: a
 * reload simply closes them all.
 */
public final class CutGround {
    private static final int COLOR = 0xE040C8;
    /** Ticks between two throws off the open cells. */
    private static final int EVERY = 10;
    /** How far over a cut it still reaches. */
    private static final double HEIGHT = 2.5;
    private static final double THROW_OUT = 0.6;
    private static final double THROW_UP = 0.8;

    private static final class Cut {
        private final Vec3 origin;
        private final HexCells.Cell cell;
        private final Vec3 middle;
        private int left;

        Cut(Vec3 origin, HexCells.Cell cell, Vec3 middle, int left) {
            this.origin = origin;
            this.cell = cell;
            this.middle = middle;
            this.left = left;
        }

        boolean covers(Vec3 at) {
            return Math.abs(at.y - middle.y) <= HEIGHT && HexCells.cellOf(at.x - origin.x, at.z - origin.z).equals(cell);
        }
    }

    private final List<Cut> cuts = new ArrayList<>();
    private int clock;

    /** How many cells stand open. */
    public int open() {
        return cuts.size();
    }

    public boolean covers(Vec3 at) {
        return cuts.stream().anyMatch(cut -> cut.covers(at));
    }

    /** Opens a cell of the grid round {@code origin}, whose middle lies on the ground at {@code middle}. */
    public void cut(ServerLevel level, Vec3 origin, HexCells.Cell cell, Vec3 middle, int ticks) {
        cuts.add(new Cut(origin, cell, middle, ticks));
        ProsectorMarkerPayload.send(level, Marker.WOUND, middle, middle, HexCells.CELL, ticks, COLOR);
    }

    public void tick(ServerLevel level, Prosector boss) {
        if (cuts.isEmpty()) {
            return;
        }
        Iterator<Cut> all = cuts.iterator();
        while (all.hasNext()) {
            Cut cut = all.next();
            if (--cut.left <= 0) {
                all.remove();
            }
        }
        if (++clock % EVERY != 0) {
            return;
        }
        for (Cut cut : cuts) {
            AABB box = new AABB(cut.middle, cut.middle).inflate(HexCells.CELL + 1, HEIGHT, HexCells.CELL + 1);
            for (LivingEntity victim : boss.victims(box)) {
                if (cut.covers(victim.position())) {
                    throwOff(level, boss, victim, cut);
                }
            }
        }
    }

    private static void throwOff(ServerLevel level, Prosector boss, LivingEntity victim, Cut cut) {
        ProsectorDamage.strike(level, victim, boss, ProsectorConfig.CELL_DAMAGE.get().floatValue());
        Vec3 away = victim.position().subtract(cut.middle).multiply(1, 0, 1);
        Vec3 out = away.lengthSqr() < 1.0E-3 ? new Vec3(1, 0, 0) : away.normalize();
        ProsectorDamage.throwAt(victim, out.scale(THROW_OUT).add(0, THROW_UP, 0));
    }

    /** Closes every cell at once. */
    public void clear(ServerLevel level, Vec3 near) {
        if (!cuts.isEmpty()) {
            cuts.clear();
            ProsectorMarkerPayload.send(level, Marker.HEAL, near, near, 0, 0, 0);
        }
    }
}
