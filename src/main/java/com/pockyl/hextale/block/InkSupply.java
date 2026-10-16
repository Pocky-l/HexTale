package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

/**
 * The Research Table's ink: it keeps none of its own and draws it from Essence Tanks of concentrate around the desk -
 * within {@value #REACH} blocks of either half sideways, one block below and above. A tank counts for the element it
 * holds pure; mixtures and empty tanks are no ink. Read on both sides (tanks sync their contents to clients).
 */
public final class InkSupply {
    public static final int REACH = 2;
    /** Millibuckets of ink one section of a thread costs (a bottle is about sixteen sections). */
    public static final int MB_PER_SECTION = 15;

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
        InkSupply supply = new InkSupply();
        BlockPos side = main.relative(TablePart.MAIN.toOther(facing));
        BlockPos min = new BlockPos(Math.min(main.getX(), side.getX()) - REACH, main.getY() - 1, Math.min(main.getZ(), side.getZ()) - REACH);
        BlockPos max = new BlockPos(Math.max(main.getX(), side.getX()) + REACH, main.getY() + 1, Math.max(main.getZ(), side.getZ()) + REACH);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockEntity(pos) instanceof EssenceTankBlockEntity tank) {
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

    /** Takes the ink of {@code sections} (sections per essence) out of the tanks. */
    public void spendSections(int[] sections) {
        int[] ink = new int[sections.length];
        for (int i = 0; i < ink.length; i++) {
            ink[i] = sections[i] * MB_PER_SECTION;
        }
        spend(ink);
    }

    /** Takes {@code ink} (mB per essence) out of the tanks, tank by tank; there must be enough. */
    public void spend(int[] ink) {
        for (int e = 0; e < ink.length; e++) {
            int left = ink[e];
            for (EssenceTankBlockEntity tank : tanks.get(e)) {
                if (left <= 0) {
                    break;
                }
                int taken = Math.min(left, tank.tank().amount());
                tank.tank().remove(EssenceMix.pure(Essence.values()[e], taken));
                tank.setChanged();
                left -= taken;
            }
            available[e] -= ink[e] - Math.max(0, left);
        }
    }
}
