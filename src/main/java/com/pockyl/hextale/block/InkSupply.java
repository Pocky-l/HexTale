package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Research Table's ink: it keeps none of its own and draws it from Essence Jars of concentrate around the desk -
 * within {@link JarReach#RADIUS} blocks of either half, every way. A jar counts for the essence it holds pure; mixtures
 * and empty jars are no ink; a Creative Essence Jar is ink of every essence. Read on both sides (jars sync their
 * contents to clients).
 */
public final class InkSupply {
    /** Millibuckets of ink one section of a thread costs (a bottle is about sixteen sections). */
    public static final int MB_PER_SECTION = 1;

    private final List<List<EssenceTankBlockEntity>> tanks = new ArrayList<>();
    private final int[] available = new int[Essence.values().length];
    private final int[] capacity = new int[Essence.values().length];

    private InkSupply() {
        for (int i = 0; i < Essence.values().length; i++) {
            tanks.add(new ArrayList<>());
        }
    }

    /** The tanks around the desk whose main block is at {@code main}. */
    public static InkSupply around(Level level, BlockPos main, Direction facing) {
        BlockPos side = main.relative(TablePart.MAIN.toOther(facing));
        BlockPos min = new BlockPos(Math.min(main.getX(), side.getX()), main.getY(), Math.min(main.getZ(), side.getZ()));
        BlockPos max = new BlockPos(Math.max(main.getX(), side.getX()), main.getY(), Math.max(main.getZ(), side.getZ()));
        return around(level, min, max);
    }

    /** The tanks of concentrate within reach of the box {@code min..max} (the Lab Bench is one block). */
    public static InkSupply around(Level level, BlockPos min, BlockPos max) {
        InkSupply supply = new InkSupply();
        for (EssenceTankBlockEntity tank : JarReach.around(level, min, max)) {
            if (tank.isCreative()) {
                for (Essence essence : Essence.values()) {
                    supply.tanks.get(essence.ordinal()).add(tank);
                    supply.available[essence.ordinal()] += EssenceTankBlockEntity.CAPACITY;
                    supply.capacity[essence.ordinal()] += EssenceTankBlockEntity.CAPACITY;
                }
            } else {
                Essence element = tank.tank().contents().pureEssence();
                if (element != null) {
                    supply.tanks.get(element.ordinal()).add(tank);
                    supply.available[element.ordinal()] += tank.tank().amount();
                    supply.capacity[element.ordinal()] += tank.tank().capacity();
                }
            }
        }
        return supply;
    }

    /** The tanks of this essence's concentrate around the desk. */
    public List<EssenceTankBlockEntity> tanks(Essence essence) {
        return Collections.unmodifiableList(tanks.get(essence.ordinal()));
    }

    /** Millibuckets of each essence's concentrate around the desk ({@link Essence#ordinal()}). */
    public int[] available() {
        return available;
    }

    /** What the tanks holding each element's concentrate can hold together; 0 when there is no such tank. */
    public int[] capacity() {
        return capacity;
    }

    /** How many sections of each essence the ink around pays for. */
    public int[] sections() {
        int[] out = new int[available.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = available[i] / MB_PER_SECTION;
        }
        return out;
    }

    /** Takes the ink of {@code sections} (sections per essence) out of the tanks; returns the jars drawn from. */
    public Map<EssenceTankBlockEntity, Essence> spendSections(int[] sections) {
        int[] ink = new int[sections.length];
        for (int i = 0; i < ink.length; i++) {
            ink[i] = sections[i] * MB_PER_SECTION;
        }
        return spend(ink);
    }

    /**
     * Takes {@code ink} (mB per essence) out of the tanks, nearest first; there must be enough.
     *
     * @return each jar drawn from with the essence it gave (the last one when a creative jar gave several)
     */
    public Map<EssenceTankBlockEntity, Essence> spend(int[] ink) {
        Map<EssenceTankBlockEntity, Essence> drawn = new LinkedHashMap<>();
        for (int e = 0; e < ink.length; e++) {
            int left = ink[e];
            for (EssenceTankBlockEntity tank : tanks.get(e)) {
                if (left <= 0) {
                    break;
                }
                int taken = Math.min(left, tank.tank().amount());
                tank.tank().remove(EssenceMix.pure(Essence.values()[e], taken));
                tank.setChanged();
                if (taken > 0) {
                    drawn.put(tank, Essence.values()[e]);
                }
                left -= taken;
            }
            available[e] -= ink[e] - Math.max(0, left);
        }
        return drawn;
    }
}
