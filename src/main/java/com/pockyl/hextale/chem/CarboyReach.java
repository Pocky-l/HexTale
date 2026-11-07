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
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

/**
 * Devices that use brews draw them from the Chem Carboys around them, no pipes: the reach of {@link JarReach}, a dose at
 * a time, and only once they have run out of what was put into them. Every draw shows: the brew runs out of the tap to
 * the device.
 */
public final class CarboyReach {
    /** How often a device that found nothing looks again. */
    private static final int LOOK_TICKS = 20;
    private static final int STREAM_TICKS = 14;

    private CarboyReach() {
    }

    /** Whether a device that wants a brew looks round this tick (devices look apart from each other). */
    public static boolean due(Level level, BlockPos device) {
        return Math.floorMod(level.getGameTime() + device.asLong(), LOOK_TICKS) == 0;
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
