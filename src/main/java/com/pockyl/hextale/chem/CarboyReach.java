package com.pockyl.hextale.chem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.ChemCarboyBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.network.EssenceFxPayload;
import com.pockyl.hextale.registry.ModSounds;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

/**
 * Devices that use brews draw them from the Chem Carboys around them, no pipes: the reach of {@link JarReach}, a dose at
 * a time, and only once they have run out of what was put into them. Every draw shows: the brew runs out of the tap to
 * the device. Heat devices leave refined shimmer alone ({@link #burns}). A device that finds nothing looks less and less
 * often ({@link Look}), until a carboy in the level gets a brew again.
 */
public final class CarboyReach {
    /** How often a device that wants a brew looks round. */
    private static final int LOOK_TICKS = 20;
    /** Times a look's wait doubles while nothing is found: 20 ticks up to 16 seconds. */
    private static final int MOST_DOUBLINGS = 4;
    private static final int STREAM_TICKS = 14;
    /** The game tick a carboy of each level last came to hold a brew. Server thread only. */
    private static final Map<Level, Long> FILLED = new WeakHashMap<>();

    private CarboyReach() {
    }

    /**
     * One device's looking round, for one thing it wants. Looks are held apart from other devices' and wait longer after
     * each one that found nothing; a carboy that newly holds a brew anywhere in the level ends the wait.
     */
    public static final class Look {
        private int misses;
        private long last = Long.MIN_VALUE / 2;

        /** Whether a device that wants a brew looks round this tick. Ask only while it wants one: a yes counts as a look. */
        public boolean due(Level level, BlockPos device) {
            return due(level.getGameTime(), device.asLong(), FILLED.getOrDefault(level, Long.MIN_VALUE));
        }

        /** The same on plain numbers: this tick, what sets the device apart, and the tick a carboy last got a brew. */
        public boolean due(long now, long seed, long filled) {
            boolean waiting = now - last < (long) LOOK_TICKS << Math.min(misses, MOST_DOUBLINGS) && filled < last;
            if (Math.floorMod(now + seed, LOOK_TICKS) != 0 || waiting) {
                return false;
            }
            last = now;
            misses++;
            return true;
        }

        /** {@link CarboyReach#draw} that ends the waiting when it finds something. */
        public @Nullable Brew draw(ServerLevel level, BlockPos center, Vec3 to, Predicate<Brew> wanted) {
            return draw(level, center, to, wanted, null);
        }

        /** {@link CarboyReach#draw} that ends the waiting when it finds something. */
        public @Nullable Brew draw(ServerLevel level, BlockPos center, Vec3 to, Predicate<Brew> wanted, @Nullable Comparator<Brew> first) {
            Brew drawn = CarboyReach.draw(level, center, to, wanted, first);
            if (drawn != null) {
                misses = 0;
            }
            return drawn;
        }
    }

    /** A carboy came to hold a brew, or was set down: the devices around stop waiting. */
    public static void filled(Level level) {
        if (level instanceof ServerLevel) {
            FILLED.put(level, level.getGameTime());
        }
    }

    /**
     * What a heat device may take from a carboy: its fuels without refined shimmer, the best fuel, which a carboy beside a
     * burner must not lose unnoticed. A vial put in by hand still burns.
     */
    public static Predicate<Brew> burns(Predicate<Brew> fuel) {
        return fuel.and(brew -> brew != Brew.REFINED_SHIMMER);
    }

    /** The carboys within reach of a block, nearest first. */
    public static List<ChemCarboyBlockEntity> around(Level level, BlockPos center) {
        return JarReach.within(level, center, center, ChemCarboyBlockEntity.class);
    }

    /** One dose of a wanted brew from the nearest carboy that has one; returns the brew, or null when there is none. */
    public static @Nullable Brew draw(ServerLevel level, BlockPos center, Vec3 to, Predicate<Brew> wanted) {
        return draw(level, center, to, wanted, null);
    }

    /**
     * One dose of a wanted brew; {@code first} says which brew goes first when carboys in reach hold several (equal ones:
     * the nearest). Returns the brew, or null when there is none.
     */
    public static @Nullable Brew draw(ServerLevel level, BlockPos center, Vec3 to, Predicate<Brew> wanted, @Nullable Comparator<Brew> first) {
        ChemCarboyBlockEntity best = null;
        for (ChemCarboyBlockEntity carboy : around(level, center)) {
            if (!carboy.contents().isEmpty() && wanted.test(carboy.contents().brew())
                    && (best == null || first != null && first.compare(carboy.contents().brew(), best.contents().brew()) < 0)) {
                best = carboy;
            }
        }
        if (best == null) {
            return null;
        }
        Brew brew = best.contents().brew();
        best.takeDose();
        Vec3 tap = best.tap();
        EssenceFxPayload.pour(level, tap, to, brew.color(), STREAM_TICKS);
        level.playSound(null, tap.x, tap.y, tap.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.2F, 0.8F);
        return brew;
    }
}
