package com.pockyl.hextale.thevoid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import com.pockyl.hextale.port.SavedData;
import com.pockyl.hextale.port.SavedDataType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Config;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.network.BreachCinematicPayload;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Where the Thinning tears the world open. When a chunk near a player has gone all the way ({@link Thinning.Stage#BREACH}, the most it can wear: 2000),
 * a breach of the Void opens in it - on open ground under the sky, away from anything built - but never within
 * {@link #SPACING} blocks of another one: one sick land, one breach. The places are remembered per level (even while
 * their chunks sleep). Players near a breach that opens are shown it ({@link BreachCinematicPayload}) and come to no
 * harm while they watch.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class Breaches {
    /** No two breaches closer than this (blocks, across the ground). */
    public static final int SPACING = 1000;
    /** Ticks between two looks for land gone far enough. */
    public static final int CHECK_TICKS = 100;
    /** Chunks every way around a player that are looked at. */
    public static final int SEARCH_RADIUS = 4;
    /** Players this close (blocks) watch a breach open. */
    public static final double WATCH_RADIUS = 64;
    /** Ticks from the omen's start until the cinematic is over at the latest. */
    public static final int CINEMATIC_TICKS = TheVoid.OMEN_TICKS + TheVoid.ROOTED_OPEN_TICKS + TheVoid.FIRST_PUSH_DELAY
            + TheVoid.PUSH_TICKS + 60;
    private static final VoidShape[] SHAPES = {VoidShape.GATE, VoidShape.EYE, VoidShape.SLIT, VoidShape.SCATTER};

    /** Players watching a breach open, and the game time until which nothing hurts them. */
    private static final Map<UUID, Long> WATCHING = new HashMap<>();
    /**
     * Players who saw a breach tear open, and the game time their diary notes it: once the first voidling is out and the
     * camera is back, so the line is not lost under the cinematic.
     */
    private static final Map<UUID, Long> WITNESSES = new HashMap<>();
    /** How long before the cinematic's end at the latest the diary notes the breach. */
    private static final int NOTE_BEFORE_END = 40;

    private Breaches() {
    }

    /** The breaches of one level. */
    public static final class Sites extends SavedData {
        public static final Codec<Sites> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.listOf().fieldOf("sites").forGetter(sites -> sites.sites),
                Rest.CODEC.listOf().optionalFieldOf("resting", List.of()).forGetter(sites -> sites.resting)
        ).apply(i, Sites::new));
        public static final SavedDataType<Sites> TYPE = new SavedDataType<>(Hextale.id("breaches"), Sites::new, CODEC);

        private final List<BlockPos> sites;
        /** Places where a breach was lately gone, and the game time until which no new one opens near them. */
        private final List<Rest> resting;

        public Sites() {
            this(List.of(), List.of());
        }

        private Sites(List<BlockPos> sites, List<Rest> resting) {
            this.sites = new ArrayList<>(sites);
            this.resting = new ArrayList<>(resting);
        }

        /** Whether a breach was gone within {@link #SPACING} blocks of {@code pos} too lately for a new one to open. */
        public boolean resting(BlockPos pos, long now) {
            if (resting.removeIf(rest -> rest.until() <= now)) {
                setDirty();
            }
            for (Rest rest : resting) {
                double dx = rest.pos().getX() - pos.getX();
                double dz = rest.pos().getZ() - pos.getZ();
                if (dx * dx + dz * dz < (double) SPACING * SPACING) {
                    return true;
                }
            }
            return false;
        }

        /** No new breach opens near {@code pos} until {@code until}. */
        public void rest(BlockPos pos, long until) {
            resting.add(new Rest(pos.immutable(), until));
            setDirty();
        }

        public List<BlockPos> sites() {
            return List.copyOf(sites);
        }

        /** The nearest breach within {@link #SPACING} blocks of {@code pos} (across the ground), or null. */
        public @Nullable BlockPos near(BlockPos pos) {
            BlockPos best = null;
            double bestDistance = (double) SPACING * SPACING;
            for (BlockPos site : sites) {
                double dx = site.getX() - pos.getX();
                double dz = site.getZ() - pos.getZ();
                double distance = dx * dx + dz * dz;
                if (distance < bestDistance) {
                    best = site;
                    bestDistance = distance;
                }
            }
            return best;
        }

        public void add(BlockPos pos) {
            sites.add(pos.immutable());
            setDirty();
        }

        void remove(BlockPos pos) {
            BlockPos nearest = null;
            double best = 16 * 16;
            for (BlockPos site : sites) {
                double distance = site.distSqr(pos);
                if (distance <= best) {
                    nearest = site;
                    best = distance;
                }
            }
            if (nearest != null) {
                sites.remove(nearest);
                setDirty();
            }
        }
    }

    public static Sites sites(ServerLevel level) {
        return Sites.TYPE.get(level.getDataStorage());
    }

    /** A place a breach was gone from, and the game time until which the land around it rests. */
    public record Rest(BlockPos pos, long until) {
        static final Codec<Rest> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Rest::pos),
                Codec.LONG.fieldOf("until").forGetter(Rest::until)
        ).apply(i, Rest::new));
    }

    /**
     * A rooted breach that is gone for good no longer keeps the land around from opening again - but only after a rest
     * ({@link Config#BREACH_COOLDOWN}): the land does not tear open again the moment one closes or is carried away.
     */
    static void forget(ServerLevel level, TheVoid breach) {
        Sites sites = sites(level);
        sites.remove(breach.blockPosition());
        long rest = Config.BREACH_COOLDOWN.get() * 60L * 20L;
        if (rest > 0) {
            sites.rest(breach.blockPosition(), level.getGameTime() + rest);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!WITNESSES.isEmpty() && level.getGameTime() % 10 == 3) {
            noteWitnesses(level);
        }
        if (level.getGameTime() % CHECK_TICKS != 7 || !Config.THINNING.get()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && tryOpenNear(level, player)) {
                return;
            }
        }
    }

    /** Opens a breach in the sickest chunk around the player if it has gone all the way and no breach is near. */
    private static boolean tryOpenNear(ServerLevel level, ServerPlayer player) {
        int cx = player.blockPosition().getX() >> 4;
        int cz = player.blockPosition().getZ() >> 4;
        List<LevelChunk> ready = new ArrayList<>();
        for (int x = cx - SEARCH_RADIUS; x <= cx + SEARCH_RADIUS; x++) {
            for (int z = cz - SEARCH_RADIUS; z <= cz + SEARCH_RADIUS; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk != null && Thinning.get(chunk) >= Thinning.Stage.BREACH.from) {
                    ready.add(chunk);
                }
            }
        }
        if (ready.isEmpty()) {
            return false;
        }
        // The sickest first: that is where the edge gives.
        ready.sort((a, b) -> Float.compare(Thinning.get(b), Thinning.get(a)));
        Sites sites = sites(level);
        for (LevelChunk chunk : ready) {
            BlockPos middle = chunk.getPos().getMiddleBlockPosition(0);
            if (sites.near(middle) != null || sites.resting(middle, level.getGameTime())) {
                return false;
            }
            BlockPos ground = findGround(level, chunk.getPos(), level.getRandom());
            if (ground != null) {
                open(level, ground, player.position());
                return true;
            }
        }
        return false;
    }

    /**
     * A spot in the chunk where a breach can stand: natural ground (what the blight takes, or has taken) under the open
     * sky, roughly level, with room above it and nothing built anywhere near. Null when the chunk has none.
     */
    public static @Nullable BlockPos findGround(ServerLevel level, ChunkPos chunk, RandomSource random) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = chunk.getMinBlockX() + 3 + random.nextInt(10);
            int z = chunk.getMinBlockZ() + 3 + random.nextInt(10);
            BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
            if (standsOn(level, ground)) {
                return ground;
            }
        }
        return null;
    }

    private static boolean standsOn(ServerLevel level, BlockPos ground) {
        if (!natural(level.getBlockState(ground)) || !level.canSeeSky(ground.above())) {
            return false;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = ground.getX() + dx;
                int z = ground.getZ() + dz;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                if (Math.abs(top - ground.getY()) > 2 || !natural(level.getBlockState(new BlockPos(x, top, z)))) {
                    return false;
                }
                // Room above: nothing but air and plants where the breach will hang.
                for (int dy = 1; dy <= 6; dy++) {
                    BlockState above = level.getBlockState(new BlockPos(x, ground.getY() + dy, z));
                    if (!above.isAir() && !above.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return !builtNear(level, ground, 10);
    }

    private static boolean natural(BlockState state) {
        return state.is(ModTags.BLIGHTS_TO_SOIL) || state.is(ModTags.BLIGHTS_TO_ROCK) || state.is(ModTags.VOID_GROUND);
    }

    /** Anything a player made - a chest, a furnace, a device - within {@code radius} blocks. */
    private static boolean builtNear(ServerLevel level, BlockPos pos, int radius) {
        int c0x = (pos.getX() - radius) >> 4;
        int c1x = (pos.getX() + radius) >> 4;
        int c0z = (pos.getZ() - radius) >> 4;
        int c1z = (pos.getZ() + radius) >> 4;
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity.getBlockPos().distSqr(pos) <= radius * radius) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Tears a rooted breach open over {@code ground}, its front to {@code facing}, remembers the place and shows it to
     * every player near.
     */
    public static TheVoid open(ServerLevel level, BlockPos ground, Vec3 facing) {
        RandomSource random = level.getRandom();
        VoidShape shape = SHAPES[random.nextInt(SHAPES.length)];
        float size = 3.6F + random.nextFloat() * 1.0F;
        int seed = random.nextInt();
        VoidLayout layout = VoidLayout.of(shape, size, seed);
        Vec3 centre = new Vec3(ground.getX() + 0.5, ground.getY() + 1 - layout.bottom() + 0.6, ground.getZ() + 0.5);
        double dx = facing.x - centre.x;
        double dz = facing.z - centre.z;
        float yaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        TheVoid breach = TheVoid.rooted(level, centre, yaw, shape, size, seed);
        level.addFreshEntity(breach);
        sites(level).add(breach.blockPosition());
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.THE_VOID_OMEN.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
        long until = level.getGameTime() + CINEMATIC_TICKS;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.position().distanceTo(centre) > WATCH_RADIUS) {
                continue;
            }
            WITNESSES.put(player.getUUID(), until - NOTE_BEFORE_END);
            if (ModNetwork.sendToPlayer(player, new BreachCinematicPayload(breach.getId()))) {
                WATCHING.put(player.getUUID(), until);
            }
        }
        return breach;
    }

    /** Tells the guide about the breach for the witnesses whose time has come. */
    private static void noteWitnesses(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            Long at = WITNESSES.get(player.getUUID());
            if (at != null && level.getGameTime() >= at) {
                WITNESSES.remove(player.getUUID());
                Quests.fire(player, Quests.BREACH_OPENED);
            }
        }
    }

    /** The player stopped watching (it ended, was skipped or is turned off). */
    public static void stopWatching(ServerPlayer player) {
        WATCHING.remove(player.getUUID());
    }

    public static boolean watching(ServerPlayer player) {
        Long until = WATCHING.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() > until) {
            WATCHING.remove(player.getUUID());
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && watching(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        WATCHING.remove(event.getEntity().getUUID());
        WITNESSES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WATCHING.clear();
        WITNESSES.clear();
    }
}
