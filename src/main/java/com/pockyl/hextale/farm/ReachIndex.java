package com.pockyl.hextale.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import org.jspecify.annotations.Nullable;

/**
 * Server: loaded devices of one kind by chunk, so an event at a point (a death, an orb appearing) asks only the few
 * chunks around it whether a device reaches it - no device ever searches its area. Devices join when they load and
 * leave when they unload or break; {@link #clearAll()} empties every index when the server stops.
 */
public final class ReachIndex<T extends BlockEntity> {
    private static final java.util.List<ReachIndex<?>> ALL = new java.util.ArrayList<>();

    private final Map<ResourceKey<Level>, Map<Long, Set<BlockPos>>> byChunk = new HashMap<>();
    private final Class<T> type;
    private final int maxRadius;
    private final BiPredicate<T, Vec3> covers;

    /** @param maxRadius the farthest any device of the kind reaches (the index looks this far) */
    public ReachIndex(Class<T> type, int maxRadius, BiPredicate<T, Vec3> covers) {
        this.type = type;
        this.maxRadius = maxRadius;
        this.covers = covers;
        ALL.add(this);
    }

    private Map<Long, Set<BlockPos>> of(Level level) {
        return byChunk.computeIfAbsent(level.dimension(), key -> new HashMap<>());
    }

    public void add(Level level, BlockPos device) {
        of(level).computeIfAbsent(ChunkPos.asLong(device), key -> new HashSet<>()).add(device.immutable());
    }

    public void remove(Level level, BlockPos device) {
        Map<Long, Set<BlockPos>> map = of(level);
        long key = ChunkPos.asLong(device);
        Set<BlockPos> devices = map.get(key);
        if (devices != null && devices.remove(device) && devices.isEmpty()) {
            map.remove(key);
        }
    }

    /** The nearest device whose reach holds this point, or null. */
    @Nullable
    public T covering(ServerLevel level, Vec3 at) {
        Map<Long, Set<BlockPos>> map = byChunk.get(level.dimension());
        if (map == null || map.isEmpty()) {
            return null;
        }
        int x = (int) Math.floor(at.x);
        int z = (int) Math.floor(at.z);
        T best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int cx = (x - maxRadius) >> 4; cx <= (x + maxRadius) >> 4; cx++) {
            for (int cz = (z - maxRadius) >> 4; cz <= (z + maxRadius) >> 4; cz++) {
                Set<BlockPos> devices = map.get(ChunkPos.asLong(cx, cz));
                if (devices == null) {
                    continue;
                }
                for (BlockPos pos : devices) {
                    double distance = Vec3.atCenterOf(pos).distanceToSqr(at);
                    if (distance < bestDistance && type.isInstance(level.getBlockEntity(pos))) {
                        T device = type.cast(level.getBlockEntity(pos));
                        if (covers.test(device, at)) {
                            best = device;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }
        return best;
    }

    public static void clearAll() {
        for (ReachIndex<?> index : ALL) {
            index.byChunk.clear();
        }
    }
}
