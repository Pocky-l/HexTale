package com.pockyl.hextale.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.GreedyFunnelBlockEntity;
import com.pockyl.hextale.runework.FunnelRune;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Server: the loaded Greedy Funnels by chunk, so a death asks only the few chunks around it whether a funnel swallows it
 * - no funnel ever searches its area for creatures, items or orbs. Funnels join when they load and leave when they unload
 * or break.
 */
public final class FunnelIndex {
    private static final Map<ResourceKey<Level>, Map<Long, Set<BlockPos>>> BY_CHUNK = new HashMap<>();

    private FunnelIndex() {
    }

    private static Map<Long, Set<BlockPos>> of(Level level) {
        return BY_CHUNK.computeIfAbsent(level.dimension(), key -> new HashMap<>());
    }

    public static void add(Level level, BlockPos funnel) {
        of(level).computeIfAbsent(ChunkPos.pack(funnel), key -> new HashSet<>()).add(funnel.immutable());
    }

    public static void remove(Level level, BlockPos funnel) {
        Map<Long, Set<BlockPos>> map = of(level);
        long key = ChunkPos.pack(funnel);
        Set<BlockPos> funnels = map.get(key);
        if (funnels != null && funnels.remove(funnel) && funnels.isEmpty()) {
            map.remove(key);
        }
    }

    /** The nearest funnel whose reach holds this point, or null. */
    @Nullable
    public static GreedyFunnelBlockEntity covering(ServerLevel level, Vec3 at) {
        Map<Long, Set<BlockPos>> map = BY_CHUNK.get(level.dimension());
        if (map == null || map.isEmpty()) {
            return null;
        }
        int x = (int) Math.floor(at.x);
        int z = (int) Math.floor(at.z);
        GreedyFunnelBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int cx = (x - FunnelRune.MAX_RADIUS) >> 4; cx <= (x + FunnelRune.MAX_RADIUS) >> 4; cx++) {
            for (int cz = (z - FunnelRune.MAX_RADIUS) >> 4; cz <= (z + FunnelRune.MAX_RADIUS) >> 4; cz++) {
                Set<BlockPos> funnels = map.get(ChunkPos.pack(cx, cz));
                if (funnels == null) {
                    continue;
                }
                for (BlockPos pos : funnels) {
                    double distance = Vec3.atCenterOf(pos).distanceToSqr(at);
                    if (distance < bestDistance && level.getBlockEntity(pos) instanceof GreedyFunnelBlockEntity funnel && funnel.covers(at)) {
                        best = funnel;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    public static void clear() {
        BY_CHUNK.clear();
    }
}
