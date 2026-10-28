package com.pockyl.hextale.chem;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ChemBoilerBlockEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Chemtech's quick road: the steam of a Chem Boiler drives the machines around it - they work as many times faster as the
 * boiler pushes ({@link ChemBoilerBlockEntity#push()}), and the boiler pays in fumes that wear the land.
 *
 * <p>Machines ask where they move their work on ({@link #steps}, {@link #factor}): nothing scans the world. The nearest
 * boiler whose reach covers the machine and that drives fewer than {@link ChemBoilerBlockEntity#MAX_DRIVEN} machines
 * (or this one already) takes it on; a machine that stops asking is let go after a while. A boiler without pressure takes
 * the request and heats up, the machine works at its own pace until the steam is there. Only machines of matter and
 * fire ask - crystal devices (purifier, anchors, pylons, the altar, lampposts) do not take steam.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class Overdrive {
    /** Loaded boilers per level, server side. */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> BOILERS = new HashMap<>();

    private Overdrive() {
    }

    public static void add(Level level, BlockPos pos) {
        if (level instanceof ServerLevel) {
            BOILERS.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
        }
    }

    public static void remove(Level level, BlockPos pos) {
        if (level instanceof ServerLevel) {
            Set<BlockPos> boilers = BOILERS.get(level.dimension());
            if (boilers != null) {
                boilers.remove(pos);
            }
        }
    }

    /**
     * How many times faster the machine at {@code machine} works now: 1 on its own, the boiler's push when one drives it.
     * Asking is what puts a machine under steam, so call it only while the machine really works.
     */
    public static float factor(@Nullable Level level, BlockPos machine) {
        ChemBoilerBlockEntity boiler = boilerFor(level, machine);
        return boiler == null ? 1 : boiler.drive(machine);
    }

    /**
     * Work steps the machine makes this tick instead of one: the factor, its fraction dithered so that a push of 2.5
     * gives 2 and 3 steps by turns on average.
     */
    public static int steps(@Nullable Level level, BlockPos machine) {
        float factor = factor(level, machine);
        if (factor <= 1 || level == null) {
            return 1;
        }
        int whole = (int) factor;
        return whole + (level.getRandom().nextFloat() < factor - whole ? 1 : 0);
    }

    /** A wait of {@code ticks} shortened by the factor (for machines that rest between jobs), at least one tick. */
    public static int shorten(@Nullable Level level, BlockPos machine, int ticks) {
        float factor = factor(level, machine);
        return factor <= 1 ? ticks : Math.max(1, Math.round(ticks / factor));
    }

    /** Whether the fumes of a driving boiler reach {@code pos} (hextech lamps choke in them now and then). */
    public static boolean fumesAt(Level level, BlockPos pos) {
        Set<BlockPos> boilers = BOILERS.get(level.dimension());
        if (boilers == null || boilers.isEmpty()) {
            return false;
        }
        for (BlockPos at : boilers) {
            if (level.isLoaded(at) && level.getBlockEntity(at) instanceof ChemBoilerBlockEntity boiler && boiler.driving()
                    && boiler.reaches(pos)) {
                return true;
            }
        }
        return false;
    }

    /** The boiler that drives (or would take on) the machine, or null. */
    private static @Nullable ChemBoilerBlockEntity boilerFor(@Nullable Level level, BlockPos machine) {
        if (!(level instanceof ServerLevel)) {
            return null;
        }
        Set<BlockPos> boilers = BOILERS.get(level.dimension());
        if (boilers == null || boilers.isEmpty()) {
            return null;
        }
        ChemBoilerBlockEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (BlockPos at : boilers) {
            if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof ChemBoilerBlockEntity boiler) || !boiler.reaches(machine)) {
                continue;
            }
            if (boiler.drives(machine)) {
                return boiler;
            }
            double distance = at.distSqr(machine);
            if (boiler.hasRoom() && distance < nearest) {
                best = boiler;
                nearest = distance;
            }
        }
        return best;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BOILERS.clear();
    }
}
