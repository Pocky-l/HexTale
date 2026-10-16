package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.essence.Essence;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.List;

/**
 * The Research Table's ink: it keeps none of its own and draws it from Essence Tanks of concentrate around the desk -
 * within {@value #REACH} blocks of either half sideways, one block below and above. A tank counts for the element it
 * holds pure; mixtures and empty tanks are no ink. Read on both sides (tanks sync their contents to clients).
 */
public final class InkSupply {
    public static final int REACH = 2;

    private final List<List<EssenceTankBlockEntity>> tanks = new ArrayList<>();
    private final int[] available = new int[SimpleRune.values().length];
    private final int[] capacity = new int[SimpleRune.values().length];

    private InkSupply() {
        for (int i = 0; i < SimpleRune.values().length; i++) {
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
                SimpleRune element = tank.tank().contents().pureElement();
                if (element != null) {
                    supply.tanks.get(element.ordinal()).add(tank);
                    supply.available[element.ordinal()] += tank.tank().amount();
                    supply.capacity[element.ordinal()] += tank.tank().capacity();
                }
            }
        }
        return supply;
    }

    /** Drops of each element's concentrate around the desk ({@link SimpleRune#ordinal()}). */
    public int[] available() {
        return available;
    }

    /** What the tanks holding each element's concentrate can hold together; 0 when there is no such tank. */
    public int[] capacity() {
        return capacity;
    }

    /** Takes {@code ink} (drops per element) out of the tanks, tank by tank; there must be enough. */
    public void spend(int[] ink) {
        for (int e = 0; e < ink.length; e++) {
            int left = ink[e];
            for (EssenceTankBlockEntity tank : tanks.get(e)) {
                if (left <= 0) {
                    break;
                }
                int taken = Math.min(left, tank.tank().amount());
                tank.tank().remove(Essence.pure(SimpleRune.values()[e], taken));
                tank.setChanged();
                left -= taken;
            }
            available[e] -= ink[e] - Math.max(0, left);
        }
    }
}
