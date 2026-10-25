package com.pockyl.hextale.client;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

import com.pockyl.hextale.network.ThinningPayload;
import com.pockyl.hextale.thevoid.Thinning;

/**
 * The Thinning of the chunks around the player, as the server last sent it. Read from the main thread and from the
 * section builders (grass and leaf colours), so every change publishes a fresh copy. Values between chunk centres are
 * blended, so the sickness fades in over the land instead of stopping at chunk lines.
 */
public final class ClientThinning {
    /** Steps of the colour change per chunk; a chunk whose step changes is rebuilt with its neighbours. */
    private static final int TINT_STEPS = 12;
    private static volatile Long2FloatMap values = new Long2FloatOpenHashMap();

    private ClientThinning() {
    }

    public static void accept(ThinningPayload payload) {
        Long2FloatOpenHashMap next = payload.reset() ? new Long2FloatOpenHashMap() : new Long2FloatOpenHashMap(values);
        LongSet redraw = new LongOpenHashSet();
        if (payload.reset()) {
            for (Long2FloatMap.Entry entry : values.long2FloatEntrySet()) {
                if (tintStep(entry.getFloatValue()) != 0) {
                    redraw.add(entry.getLongKey());
                }
            }
        }
        for (ThinningPayload.Entry entry : payload.entries()) {
            float before = next.get(entry.chunk());
            if (entry.value() > 0) {
                next.put(entry.chunk(), entry.value());
            } else {
                next.remove(entry.chunk());
            }
            if (tintStep(before) != tintStep(entry.value())) {
                redraw.add(entry.chunk());
            }
        }
        values = next;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int minSection = level.getMinSectionY();
        int maxSection = level.getMaxSectionY();
        for (long chunk : redraw) {
            int x = ChunkPos.getX(chunk);
            int z = ChunkPos.getZ(chunk);
            level.setSectionRangeDirty(x - 1, minSection, z - 1, x + 1, maxSection, z + 1);
        }
    }

    public static void clear() {
        values = new Long2FloatOpenHashMap();
    }

    /** Whether any chunk around the player has worn thin at all. */
    public static boolean any() {
        return !values.isEmpty();
    }

    /** The value in one chunk. */
    public static float chunk(int chunkX, int chunkZ) {
        return values.get(ChunkPos.pack(chunkX, chunkZ));
    }

    /** The value at a block column, blended between the four nearest chunk centres. */
    public static float at(double x, double z) {
        Long2FloatMap map = values;
        if (map.isEmpty()) {
            return 0;
        }
        double fx = (x - 8) / 16;
        double fz = (z - 8) / 16;
        int x0 = Mth.floor(fx);
        int z0 = Mth.floor(fz);
        float tx = (float) (fx - x0);
        float tz = (float) (fz - z0);
        float a = map.get(ChunkPos.pack(x0, z0));
        float b = map.get(ChunkPos.pack(x0 + 1, z0));
        float c = map.get(ChunkPos.pack(x0, z0 + 1));
        float d = map.get(ChunkPos.pack(x0 + 1, z0 + 1));
        return Mth.lerp(tz, Mth.lerp(tx, a, b), Mth.lerp(tx, c, d));
    }

    /** How far grass, leaves and water have faded towards ash (0-1): from the first haze to the middle of the miasma. */
    public static float tint(float value) {
        float from = Thinning.Stage.HAZE.felt * 0.6F;
        float t = Mth.clamp((Thinning.felt(value) - from) / (Thinning.Stage.MIASMA.felt * 1.5F - from), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static int tintStep(float value) {
        return Math.round(tint(value) * TINT_STEPS);
    }
}
