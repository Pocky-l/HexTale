package com.pockyl.hextale.thevoid;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.HextechLamppostBlockEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Hextech's clean answer to the dirt of the work: the light of a Hextech Lamppost. Inside a lit lamppost's reach a share
 * of every gain of the Thinning is burned off before it reaches the land ({@link #burnOff}, asked by
 * {@link Thinning#add}), paid out of the lamppost's light; the chunk a lit lamppost stands in never catches the creep of
 * its neighbours ({@link #shields}); and on the clients the haze thins out in its light ({@link #litAt}). The Void's own
 * feeding of the land around a breach is not burned off.
 *
 * <p>An index of the lampposts per level and side (their block entities join on load and leave when removed), so nothing
 * scans the world.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class Wards {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> SERVER = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> CLIENT = new HashMap<>();

    private Wards() {
    }

    private static Map<ResourceKey<Level>, Set<BlockPos>> side(Level level) {
        return level.isClientSide() ? CLIENT : SERVER;
    }

    public static void add(Level level, BlockPos pos) {
        side(level).computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
    }

    public static void remove(Level level, BlockPos pos) {
        Set<BlockPos> posts = side(level).get(level.dimension());
        if (posts != null) {
            posts.remove(pos);
        }
    }

    /** The nearest lit lamppost whose light covers {@code pos}, or null. */
    public static @Nullable HextechLamppostBlockEntity lightAt(Level level, BlockPos pos) {
        Set<BlockPos> posts = side(level).get(level.dimension());
        if (posts == null || posts.isEmpty()) {
            return null;
        }
        HextechLamppostBlockEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (BlockPos at : posts) {
            if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof HextechLamppostBlockEntity post) || !post.lit()
                    || !post.covers(pos)) {
                continue;
            }
            double distance = at.distSqr(pos);
            if (distance < nearest) {
                best = post;
                nearest = distance;
            }
        }
        return best;
    }

    /**
     * A gain of the Thinning at {@code pos}: a lit lamppost whose light covers the spot burns off its share of it.
     * Returns what is left to reach the land.
     */
    public static float burnOff(ServerLevel level, BlockPos pos, float amount, Thinning.Source source) {
        if (source == Thinning.Source.BREACH || amount <= 0) {
            return amount;
        }
        HextechLamppostBlockEntity post = lightAt(level, pos);
        return post == null ? amount : amount - post.burn(level, pos, amount);
    }

    /** Whether a lit lamppost stands in the chunk: the creep of the sickness around stops at its light. */
    public static boolean shields(ServerLevel level, ChunkPos chunk) {
        Set<BlockPos> posts = SERVER.get(level.dimension());
        if (posts == null || posts.isEmpty()) {
            return false;
        }
        for (BlockPos at : posts) {
            if (new ChunkPos(at).equals(chunk) && level.isLoaded(at)
                    && level.getBlockEntity(at) instanceof HextechLamppostBlockEntity post && post.lit()) {
                return true;
            }
        }
        return false;
    }

    /** Client: whether the spot lies in a lit lamppost's light (the haze thins there). */
    public static boolean litAt(Level level, double x, double y, double z) {
        return lightAt(level, BlockPos.containing(x, y, z)) != null;
    }

    /** Client: forgets the lampposts (on leaving a world). */
    public static void clearClient() {
        CLIENT.clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SERVER.clear();
    }
}
