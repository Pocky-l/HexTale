package com.pockyl.hextale.thevoid.boss.attack;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.boss.HexCells;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorConfig;
import com.pockyl.hextale.thevoid.boss.ProsectorDamage;
import com.pockyl.hextale.thevoid.boss.ProsectorState.Section;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Phase two's question: where can I stand. The ground round it is cut by a grid of hexagons: cells glow for the startup, then
 * open in a pattern (a ring, three spokes or a spiral) and close after a while. Who stands on an opening cell takes a hit and
 * is thrown up; an open cell throws off whoever steps on it. No block is touched: see {@link
 * com.pockyl.hextale.thevoid.boss.CutGround}.
 */
final class FloorSection extends Attack {
    private static final int COLOR = 0xE040C8;
    /** How hard an opening cell throws who stands on it. */
    private static final double THROW = 0.9;
    private final List<HexCells.Cell> cells = new ArrayList<>();
    private Vec3 origin = Vec3.ZERO;
    private int dropped;

    FloorSection(Prosector boss) {
        super(boss, AttackType.SECTION);
    }

    /** The middle of a cell, on the ground that is there. */
    private Vec3 middle(HexCells.Cell cell) {
        return boss.groundAt(origin.x + HexCells.cellX(cell), origin.z + HexCells.cellZ(cell), targetGone() ? origin.y : target.getY());
    }

    @Override
    protected void onStart(ServerLevel level) {
        origin = boss.groundUnder(boss.position());
        List<HexCells.Cell> all = HexCells.cells();
        switch (boss.getRandom().nextInt(3)) {
            case 0 -> ring(all);
            case 1 -> spokes(all);
            default -> spiral(all);
        }
        int warning = section(Section.STARTUP);
        for (HexCells.Cell cell : cells) {
            Vec3 middle = middle(cell);
            ProsectorMarkerPayload.send(level, Marker.HEX, middle, middle, size(), warning, COLOR);
        }
    }

    /** Every cell of one ring round it, the one the target stands on if it can. */
    private void ring(List<HexCells.Cell> all) {
        int widest = all.stream().mapToInt(HexCells.Cell::ring).max().orElse(1);
        int ring = targetGone() ? 2 : HexCells.cellOf(target.getX() - origin.x, target.getZ() - origin.z).ring();
        ring = Math.max(1, Math.min(widest, ring));
        for (HexCells.Cell cell : all) {
            if (cell.ring() == ring) {
                cells.add(cell);
            }
        }
    }

    /** Cells along three lines through the middle. */
    private void spokes(List<HexCells.Cell> all) {
        boolean flip = boss.getRandom().nextBoolean();
        for (HexCells.Cell cell : all) {
            int q = cell.q();
            int r = cell.r();
            boolean spoke = flip ? q == 0 || r == 0 || q + r == 0 : q == r || q == -2 * r || r == -2 * q;
            if (spoke && cell.ring() > 0) {
                cells.add(cell);
            }
        }
    }

    /** Every other cell round the rings, in order: they open one by one. */
    private void spiral(List<HexCells.Cell> all) {
        List<HexCells.Cell> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.comparingInt(HexCells.Cell::ring)
                .thenComparingDouble(cell -> Math.atan2(HexCells.cellZ(cell), HexCells.cellX(cell))));
        for (int i = 1; i < sorted.size(); i += 2) {
            cells.add(sorted.get(i));
        }
    }

    @Override
    protected void active(ServerLevel level, int tick) {
        boss.hold();
        int length = section(Section.ACTIVE);
        int due = (int) Math.ceil(cells.size() * (tick + 1) / (double) length);
        while (dropped < due && dropped < cells.size()) {
            open(level, cells.get(dropped++));
        }
    }

    private void open(ServerLevel level, HexCells.Cell cell) {
        Vec3 middle = middle(cell);
        boss.cut().cut(level, origin, cell, middle, ProsectorConfig.CELL_RETURN_TICKS.get());
        boss.playAt(middle, ModSounds.PROSECTOR_FLOOR_DROP.get(), 1.4F, 0.9F + boss.getRandom().nextFloat() * 0.2F);
        for (LivingEntity victim : boss.victims(new AABB(middle, middle).inflate(size() + 1, 3, size() + 1))) {
            if (HexCells.cellOf(victim.getX() - origin.x, victim.getZ() - origin.z).equals(cell) && strike(level, victim)) {
                ProsectorDamage.throwAt(victim, new Vec3(0, THROW, 0));
            }
        }
    }
}
