package com.pockyl.hextale.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.VaultRune;

/**
 * The price of distance. A take through a terminal costs an eighth of a mB of rift essence from the jars round it, paid a
 * whole mB at a time through {@link JarReach#pay}, as a Rift Gate's toll is; jars that run dry only slow takes to one
 * every {@value #SLOW_TICKS} ticks, so items are never locked away. Kept per player, so reopening the screen changes nothing.
 */
public final class VaultToll {
    /** Takes one mB of rift essence pays for. */
    public static final int TAKES_PER_MB = 8;
    /** Ticks between takes while no jar pays. */
    public static final int SLOW_TICKS = 60;

    private int credit;
    private long nextSlowTake;
    private boolean dry;

    /** Whether a take may go now: pays for it, or lets a slow one through. */
    public boolean pass(ServerLevel level, BlockPos jarsAround, Vec3 to, int thrift) {
        if (credit > 0) {
            credit--;
            dry = false;
            return true;
        }
        if (thrift > 0 && level.getRandom().nextFloat() < HolderRune.at(VaultRune.FREE_TAKE, thrift)) {
            dry = false;
            return true;
        }
        if (JarReach.pay(level, jarsAround, to, Essence.RIFT, 1)) {
            credit = TAKES_PER_MB - 1;
            dry = false;
            return true;
        }
        dry = true;
        long now = level.getGameTime();
        if (now < nextSlowTake) {
            return false;
        }
        nextSlowTake = now + SLOW_TICKS;
        return true;
    }

    /** Ticks until a dry take may go; 0 when one may. */
    public int waiting(long now) {
        return dry ? (int) Math.max(0, nextSlowTake - now) : 0;
    }

    public boolean dry() {
        return dry;
    }
}
