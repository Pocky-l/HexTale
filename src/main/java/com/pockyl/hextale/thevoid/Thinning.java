package com.pockyl.hextale.thevoid;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.pockyl.hextale.Config;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.ThinningPayload;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModTags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Thinning: how worn the edge of the world is in a chunk. Careless magic - essence poured away, raw crystals
 * bursting, rituals breaking - wears it thin, and the Void, which hungers for magic, seeps in where it is thinnest:
 * first as a haze, then as a sickness of the land, at last as a breach. Everyday work with magic wears it too, a little
 * at a time - boiling essence, separating it, drawing ink, rituals, charged crafts, waking crystals - so a researcher's
 * home sickens slowly as the research goes on: noticeable in the middle of it, at its worst near the end. A value per
 * chunk (a chunk attachment) that never heals by itself; the land around catches it, each ring of chunks a share
 * ({@link #FALLOFF}) of the one inside, so a base and its surroundings sicken together. Players get the values of the
 * chunks around them for the haze, fog and colours.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class Thinning {
    /** The first part of the scale shows nothing: the world takes this much before the Void is felt at all. */
    public static final float SAFE = 500;
    public static final float MAX = SAFE + 1500;
    /** Ticks between two flows. */
    public static final int SPREAD_TICKS = 40;
    /** A neighbour catches the sickness up to this share of a chunk's value. */
    public static final float FALLOFF = 0.6F;
    /** Share of the way to that level a neighbour goes per flow. */
    public static final float SPREAD_RATE = 0.05F;
    /** A neighbour less than this short of its level is left alone (so the edge settles). */
    public static final float SPREAD_MIN = 2;
    /** Chance per flow that a blighted chunk's patches grow (x 0.3 at the start of the blight, x 1.3 at its end). */
    public static final float BLIGHT_CHANCE = 0.25F;
    /** Chance that a pick far from every patch starts a new focus instead of nothing. */
    public static final float NEW_FOCUS_CHANCE = 0.06F;
    /** How far from a pick a patch may lie and still grow towards it. */
    public static final int GROW_REACH = 6;
    /** Chance that a blighted spot grows the Void's flora. */
    public static final float FLORA_CHANCE = 0.22F;
    public static final int SYNC_TICKS = 20;
    /** Chunks every way around a player whose values the player gets. */
    public static final int SYNC_RADIUS = 8;

    // Everyday work, per unit: slow on purpose - the haze shows in the middle of the research, the worst near its end.
    /** Essence boiled out of matter in an alembic, per mB (out of unknown things: double). */
    public static final float BOILED = 0.04F;
    /** Essence a separator splits, per mB of the portion. */
    public static final float SEPARATED = 0.01F;
    /** A section of ink drawn on a research parchment. */
    public static final float INK_SECTION = 0.25F;
    /** A finished ritual, plus per mB of essence it drew. */
    public static final float RITUAL = 3;
    public static final float RITUAL_PER_MB = 0.08F;
    /** Charge a Hex Workbench spends on a craft, per point. */
    public static final float CHARGE = 0.15F;
    /** A crystal block lightning shatters into raw hextech. */
    public static final float AWAKENED = 1;

    // Carelessness and accidents.
    /** Essence poured away and lost on the ground, per mB (unknown essence counts double). */
    public static final float POURED_LOST = 0.25F;
    /** Essence poured away that soaked into stone, per mB. */
    public static final float POURED_SOAKED = 1 / 32F;
    /** A breath of a Miasma Vent. */
    public static final float MIASMA_PUFF = 0.5F;
    /** An unstable hextech orb bursting, per orb. */
    public static final float ORB_BURST = 12;
    public static final float RITUAL_COLLAPSE = 60;
    public static final float LAB_BURST = 15;
    public static final float PARCHMENT_BURN = 5;

    /**
     * How far past the {@link #SAFE} part the land looks and sickens its worst (the haze, the fog, the blight's pace).
     * The breach waits longer, until the land has gone all the way ({@link Stage#BREACH} = {@link #MAX}).
     */
    public static final float WORST = 1000;

    /** How far the land has gone; {@code felt} counts from the end of the {@link #SAFE} part. */
    public enum Stage {
        CLEAN(-SAFE), HAZE(100), MIASMA(300), BLIGHT(600), BREACH(1500);

        public final float from;
        public final float felt;

        Stage(float felt) {
            this.felt = felt;
            this.from = SAFE + felt;
        }

        public static Stage of(float value) {
            Stage[] stages = values();
            for (int i = stages.length - 1; i > 0; i--) {
                if (value >= stages[i].from) {
                    return stages[i];
                }
            }
            return CLEAN;
        }

        public String key() {
            return "thinning.hextale.stage." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Chunks with a value above zero, per loaded level - the only ones the flow looks at. */
    private static final Map<ResourceKey<Level>, LongSet> ACTIVE = new HashMap<>();
    /** What each player was last sent. */
    private static final Map<UUID, Long2FloatMap> SENT = new HashMap<>();

    private Thinning() {
    }

    public static float get(LevelChunk chunk) {
        return chunk.getData(ModAttachments.THINNING);
    }

    /** The value in the chunk holding {@code pos}, 0 when it is not loaded. */
    public static float get(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? 0 : get(chunk);
    }

    /** How much of {@code value} is felt: what lies past the {@link #SAFE} part (0 inside it). */
    public static float felt(float value) {
        return Math.max(0, value - SAFE);
    }

    public static Stage stage(Level level, BlockPos pos) {
        return Stage.of(get(level, pos));
    }

    /** Wears the edge thinner where {@code pos} is (scaled by the config; nothing when the Thinning is off). */
    public static void add(ServerLevel level, BlockPos pos, float amount) {
        if (!Config.THINNING.get() || amount <= 0) {
            return;
        }
        LevelChunk chunk = level.getChunkAt(pos);
        set(level, chunk, get(chunk) + amount * Config.THINNING_RATE.get().floatValue());
    }

    /** Essence poured away at {@code pos}: lost on the ground, or soaked into stone (much less). */
    public static void poured(ServerLevel level, BlockPos pos, EssenceMix mix, int amount, boolean soaked) {
        if (amount <= 0 || mix.isEmpty()) {
            return;
        }
        float unknownShare = mix.unknown() / (float) mix.total();
        add(level, pos, amount * (soaked ? POURED_SOAKED : POURED_LOST) * (1 + unknownShare));
    }

    public static void set(ServerLevel level, LevelChunk chunk, float value) {
        float clamped = Mth.clamp(value, 0, MAX);
        if (clamped < 0.01F) {
            clamped = 0;
        }
        if (clamped == 0 && !chunk.hasData(ModAttachments.THINNING)) {
            return;
        }
        chunk.setData(ModAttachments.THINNING, clamped);
        chunk.markUnsaved();
        LongSet active = ACTIVE.computeIfAbsent(level.dimension(), key -> new LongOpenHashSet());
        if (clamped > 0) {
            active.add(chunk.getPos().pack());
        } else {
            chunk.removeData(ModAttachments.THINNING);
            active.remove(chunk.getPos().pack());
        }
    }

    /** Sets every loaded chunk within {@code radius} chunks of {@code pos}; returns how many. */
    public static int setAround(ServerLevel level, BlockPos pos, int radius, float value) {
        int count = 0;
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk != null) {
                    set(level, chunk, value);
                    count++;
                }
            }
        }
        return count;
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk && get(chunk) > 0) {
            ACTIVE.computeIfAbsent(level.dimension(), key -> new LongOpenHashSet()).add(chunk.getPos().pack());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LongSet active = ACTIVE.get(level.dimension());
            if (active != null) {
                active.remove(event.getChunk().getPos().pack());
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % SPREAD_TICKS == 0) {
            flow(level);
        }
    }

    /**
     * One flow: every loaded neighbour of a sick chunk creeps towards {@link #FALLOFF} of its value (the highest of its
     * sick neighbours counts). Nothing heals: the land only catches what lies beside it.
     */
    public static void flow(ServerLevel level) {
        LongSet active = ACTIVE.get(level.dimension());
        if (active == null || active.isEmpty()) {
            return;
        }
        Long2FloatOpenHashMap rise = new Long2FloatOpenHashMap();
        Map<Long, LevelChunk> chunks = new HashMap<>();
        int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (LongIterator it = active.iterator(); it.hasNext(); ) {
            long key = it.nextLong();
            int x = ChunkPos.getX(key);
            int z = ChunkPos.getZ(key);
            LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
            if (chunk == null) {
                it.remove();
                continue;
            }
            float level0 = get(chunk) * FALLOFF;
            for (int[] side : sides) {
                LevelChunk next = level.getChunkSource().getChunkNow(x + side[0], z + side[1]);
                if (next == null) {
                    continue;
                }
                float gap = level0 - get(next);
                if (gap > SPREAD_MIN) {
                    long nextKey = next.getPos().pack();
                    rise.put(nextKey, Math.max(rise.get(nextKey), gap * SPREAD_RATE));
                    chunks.putIfAbsent(nextKey, next);
                }
            }
        }
        for (Long2FloatMap.Entry entry : rise.long2FloatEntrySet()) {
            LevelChunk chunk = chunks.get(entry.getLongKey());
            set(level, chunk, get(chunk) + entry.getFloatValue());
        }
        for (LongIterator it = active.iterator(); it.hasNext(); ) {
            long key = it.nextLong();
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk != null && get(chunk) >= Stage.BLIGHT.from) {
                blight(level, chunk, get(chunk));
            }
        }
    }

    /**
     * Blighted land sickens in a few large foci rather than in specks: now and then a spot of the chunk is picked. On or
     * next to land the Void already holds, the patch grows outwards there; far from any, a new focus is born only rarely
     * ({@link #NEW_FOCUS_CHANCE}). Deeper blight picks spots more often and grows the patches in bigger bites. The world's
     * plants on the blighted ground die back and the Void's own growth takes root in their place.
     */
    private static void blight(ServerLevel level, LevelChunk chunk, float value) {
        RandomSource random = level.getRandom();
        float k = Mth.clamp((felt(value) - Stage.BLIGHT.felt) / (WORST - Stage.BLIGHT.felt), 0, 1);
        if (random.nextFloat() >= BLIGHT_CHANCE * (0.3F + k)) {
            return;
        }
        int x = chunk.getPos().getMinBlockX() + random.nextInt(16);
        int z = chunk.getPos().getMinBlockZ() + random.nextInt(16);
        int[] edge = edgeNear(level, x, z, random);
        if (edge != null) {
            disc(level, edge[0], edge[1], 1 + (random.nextFloat() < 0.4F + k * 0.4F ? 1 : 0), random);
        } else if (random.nextFloat() < NEW_FOCUS_CHANCE) {
            disc(level, x, z, 2 + random.nextInt(2), random);
            level.playSound(null, new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z),
                    SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 0.8F, 0.5F);
        }
    }

    /**
     * Where a patch near ({@code x}, {@code z}) can grow: walking out from the spot in a random direction, the first
     * column past the Void's ground (or the spot itself when a patch lies beside it). Null when no patch is within
     * {@link #GROW_REACH}.
     */
    private static int[] edgeNear(ServerLevel level, int x, int z, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        boolean inside = false;
        for (int step = -GROW_REACH; step <= GROW_REACH; step++) {
            int cx = x + (int) Math.round(dx * step);
            int cz = z + (int) Math.round(dz * step);
            BlockPos ground = surface(level, cx, cz);
            boolean held = ground != null && level.getBlockState(ground).is(ModTags.VOID_GROUND);
            if (held) {
                inside = true;
            } else if (inside && ground != null) {
                return new int[] {cx, cz};
            }
        }
        return null;
    }

    /** Blights a ragged disc of columns around ({@code x}, {@code z}). */
    private static void disc(ServerLevel level, int x, int z, int radius, RandomSource random) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                float d = dx * dx + dz * dz;
                if (d <= radius * radius + random.nextFloat() * radius - radius * 0.5F) {
                    blightColumn(level, x + dx, z + dz, random);
                }
            }
        }
    }

    /** Blights the surface of one column (true if it found ground the blight takes). */
    public static boolean blightColumn(ServerLevel level, int x, int z, RandomSource random) {
        BlockPos pos = surface(level, x, z);
        if (pos != null) {
            blightAt(level, pos, random, false);
        }
        return pos != null;
    }

    /** The topmost ground block of a column that the blight takes (or already took), or null. */
    private static BlockPos surface(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x,
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
        for (int step = 0; step < 3; step++) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModTags.BLIGHTS_TO_SOIL) || state.is(ModTags.BLIGHTS_TO_ROCK) || state.is(ModTags.VOID_GROUND)) {
                return pos.immutable();
            }
            // Water (and anything else liquid) keeps what lies under it: the blight takes only dry land.
            if (!state.getFluidState().isEmpty() || (!state.isAir() && !state.canBeReplaced())) {
                return null;
            }
            pos.move(0, -1, 0);
        }
        return null;
    }

    private static void blightAt(ServerLevel level, BlockPos pos, RandomSource random, boolean loud) {
        BlockState ground = level.getBlockState(pos);
        BlockState blighted = ground.is(ModTags.BLIGHTS_TO_SOIL) ? ModBlocks.VOID_SOIL.get().defaultBlockState()
                : ground.is(ModTags.BLIGHTS_TO_ROCK) ? ModBlocks.VOID_ROCK.get().defaultBlockState() : null;
        if (blighted != null) {
            level.setBlockAndUpdate(pos, blighted);
            if (loud) {
                level.playSound(null, pos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 0.6F, 0.6F);
            }
            level.sendParticles(ModParticles.voidMote(0x24162E), pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 4, 0.35, 0.05, 0.35, 0);
        }
        BlockPos above = pos.above();
        BlockState plant = level.getBlockState(above);
        if (plant.getBlock() instanceof VoidFloraBlock || !plant.getFluidState().isEmpty() || !(plant.isAir() || plant.canBeReplaced())) {
            return;
        }
        if (random.nextFloat() < FLORA_CHANCE) {
            float pick = random.nextFloat();
            Block flora = pick < 0.4F ? ModBlocks.VOID_ROOTS.get() : pick < 0.8F ? ModBlocks.VOID_CORAL.get() : ModBlocks.VOID_BULB.get();
            level.setBlockAndUpdate(above, flora.defaultBlockState());
        } else if (!plant.isAir()) {
            level.destroyBlock(above, false);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % SYNC_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            sync(player);
        }
    }

    /** Sends the player the values around them that changed since the last time (all of them after a reset). */
    private static void sync(ServerPlayer player) {
        boolean reset = !SENT.containsKey(player.getUUID());
        Long2FloatMap sent = SENT.computeIfAbsent(player.getUUID(), id -> new Long2FloatOpenHashMap());
        LongSet active = ACTIVE.get(player.level().dimension());
        if (!reset && sent.isEmpty() && (active == null || active.isEmpty())) {
            return;
        }
        ServerLevel level = player.level();
        int cx = player.blockPosition().getX() >> 4;
        int cz = player.blockPosition().getZ() >> 4;
        List<ThinningPayload.Entry> changes = new ArrayList<>();
        for (int x = cx - SYNC_RADIUS; x <= cx + SYNC_RADIUS; x++) {
            for (int z = cz - SYNC_RADIUS; z <= cz + SYNC_RADIUS; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                long key = ChunkPos.pack(x, z);
                float value = get(chunk);
                if (sent.containsKey(key) ? Math.abs(sent.get(key) - value) >= 0.5F || (value == 0 && sent.get(key) != 0)
                        : value > 0) {
                    changes.add(new ThinningPayload.Entry(key, value));
                    sent.put(key, value);
                }
            }
        }
        // Forget what lies far behind: the client drops it too, and it is sent again on return.
        sent.keySet().removeIf(key -> Math.abs(ChunkPos.getX(key) - cx) > SYNC_RADIUS + 2
                || Math.abs(ChunkPos.getZ(key) - cz) > SYNC_RADIUS + 2);
        if (reset || !changes.isEmpty()) {
            ModNetwork.sendToPlayer(player, new ThinningPayload(reset, changes));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
        SENT.clear();
    }
}
