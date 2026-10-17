package com.pockyl.hextale.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server: which loaded pylons link to which block, so a broken block can drop its links at once (and a player can untie
 * a block from every pylon) without searching. Pylons register their links when they load and whenever they change
 * them, and leave when they unload or break.
 */
public final class PylonIndex {
    private static final Map<ResourceKey<Level>, Map<BlockPos, Set<BlockPos>>> BY_TARGET = new HashMap<>();

    private PylonIndex() {
    }

    private static Map<BlockPos, Set<BlockPos>> of(Level level) {
        return BY_TARGET.computeIfAbsent(level.dimension(), key -> new HashMap<>());
    }

    public static void add(Level level, BlockPos pylon, BlockPos target) {
        of(level).computeIfAbsent(target.immutable(), key -> new HashSet<>()).add(pylon.immutable());
    }

    public static void remove(Level level, BlockPos pylon, BlockPos target) {
        Map<BlockPos, Set<BlockPos>> map = of(level);
        Set<BlockPos> pylons = map.get(target);
        if (pylons != null && pylons.remove(pylon) && pylons.isEmpty()) {
            map.remove(target);
        }
    }

    public static void removeAll(Level level, BlockPos pylon, List<PylonLink> links) {
        for (PylonLink link : links) {
            remove(level, pylon, link.pos());
        }
    }

    /** The pylons linking to {@code target} (a copy). */
    public static Set<BlockPos> linkingTo(Level level, BlockPos target) {
        Map<BlockPos, Set<BlockPos>> map = BY_TARGET.get(level.dimension());
        Set<BlockPos> pylons = map == null ? null : map.get(target);
        return pylons == null ? Set.of() : new HashSet<>(pylons);
    }

    public static boolean isTarget(Level level, BlockPos pos) {
        Map<BlockPos, Set<BlockPos>> map = BY_TARGET.get(level.dimension());
        return map != null && map.containsKey(pos);
    }

    public static void clear() {
        BY_TARGET.clear();
    }
}
