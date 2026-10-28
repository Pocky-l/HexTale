package com.pockyl.hextale.thevoid;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTags;

import java.util.HashMap;
import java.util.Map;

/**
 * The land heals where the Thinning has been drawn back out of it. A chunk the blight took ground in is {@code scarred}
 * (a chunk attachment). While its value stays below {@link #HEALS_BELOW} the Void's ground recedes the way it came - in
 * patches, from a spot next to land the Void still holds: void soil turns back into what lies under it (sand, gravel,
 * earth that greens under the sky), void rock into the rock beneath, and the Void's growth withers. When no Void ground
 * is left in the chunk it is no longer scarred. Land healed while a Purifier with Fertility draws on it may sprout grass
 * and flowers ({@link #bless}).
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class Healing {
    /** Below this the blight recedes (a little under where it begins, so the edge does not flicker). */
    public static final float HEALS_BELOW = Thinning.Stage.BLIGHT.from - 100;
    /** Chance per flow that a scarred chunk heals a patch (x 0.4 just under the line, x 1.4 when clean). */
    public static final float HEAL_CHANCE = 0.3F;
    /** Players this close hear about it in their diary. */
    public static final double NOTE_RADIUS = 24;
    private static final int HEAL_TICKS = Thinning.SPREAD_TICKS;

    private static final Map<ResourceKey<Level>, LongSet> SCARRED = new HashMap<>();
    /** Chunks a Purifier with Fertility draws on: the game time until which, packed with the strength in the low bits. */
    private static final Map<ResourceKey<Level>, Long2LongMap> BLESSED = new HashMap<>();

    private Healing() {
    }

    /** The blight took ground in this chunk. */
    public static void scar(ServerLevel level, LevelChunk chunk) {
        if (!chunk.getData(ModAttachments.SCARRED)) {
            chunk.setData(ModAttachments.SCARRED, true);
            chunk.setUnsaved(true);
        }
        SCARRED.computeIfAbsent(level.dimension(), key -> new LongOpenHashSet()).add(chunk.getPos().toLong());
    }

    public static boolean scarred(LevelChunk chunk) {
        return chunk.getData(ModAttachments.SCARRED);
    }

    private static void unscar(ServerLevel level, LevelChunk chunk) {
        chunk.removeData(ModAttachments.SCARRED);
        chunk.setUnsaved(true);
        LongSet set = SCARRED.get(level.dimension());
        if (set != null) {
            set.remove(chunk.getPos().toLong());
        }
    }

    /** A Purifier with Fertility of {@code strength} draws on this chunk for a while: healed spots may bloom. */
    public static void bless(ServerLevel level, LevelChunk chunk, int strength, int ticks) {
        BLESSED.computeIfAbsent(level.dimension(), key -> new Long2LongOpenHashMap())
                .put(chunk.getPos().toLong(), ((level.getGameTime() + ticks) << 3) | Mth.clamp(strength, 0, 4));
    }

    private static int blessing(ServerLevel level, ChunkPos chunk) {
        Long2LongMap map = BLESSED.get(level.dimension());
        if (map == null) {
            return 0;
        }
        long packed = map.get(chunk.toLong());
        if (packed == 0 || (packed >> 3) < level.getGameTime()) {
            return 0;
        }
        return (int) (packed & 7);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk && scarred(chunk)) {
            SCARRED.computeIfAbsent(level.dimension(), key -> new LongOpenHashSet()).add(chunk.getPos().toLong());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LongSet set = SCARRED.get(level.dimension());
            if (set != null) {
                set.remove(event.getChunk().getPos().toLong());
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % HEAL_TICKS == HEAL_TICKS / 2) {
            heal(level);
        }
    }

    /** One round: every scarred chunk below the line may heal a patch. */
    public static void heal(ServerLevel level) {
        LongSet set = SCARRED.get(level.dimension());
        if (set == null || set.isEmpty()) {
            return;
        }
        Long2LongMap blessed = BLESSED.get(level.dimension());
        if (blessed != null) {
            long now = level.getGameTime();
            blessed.values().removeIf(packed -> (packed >> 3) < now);
        }
        RandomSource random = level.getRandom();
        for (LongIterator it = new LongOpenHashSet(set).iterator(); it.hasNext(); ) {
            long key = it.nextLong();
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk == null) {
                set.remove(key);
                continue;
            }
            float value = Thinning.get(chunk);
            if (value >= HEALS_BELOW) {
                continue;
            }
            float k = 1 - value / HEALS_BELOW;
            if (random.nextFloat() < HEAL_CHANCE * (0.4F + k)) {
                healPatch(level, chunk, random);
            }
        }
    }

    /**
     * Heals one patch of the chunk: around a picked spot if the Void holds it, else at the nearest Void ground a line
     * through the spot meets, else wherever a look over the whole chunk finds some - or, finding none, the chunk is whole.
     * Returns whether anything healed.
     */
    public static boolean healPatch(ServerLevel level, LevelChunk chunk, RandomSource random) {
        ChunkPos pos = chunk.getPos();
        int x = pos.getMinBlockX() + random.nextInt(16);
        int z = pos.getMinBlockZ() + random.nextInt(16);
        int[] spot = held(level, x, z) ? new int[] {x, z} : heldNear(level, x, z, random);
        if (spot == null) {
            spot = anyHeld(level, pos);
            if (spot == null) {
                unscar(level, chunk);
                return false;
            }
        }
        int radius = 1 + (random.nextFloat() < 0.5F ? 1 : 0);
        int healed = 0;
        BlockPos sound = null;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + random.nextFloat() * radius - radius * 0.5F) {
                    continue;
                }
                int cx = spot[0] + dx;
                int cz = spot[1] + dz;
                LevelChunk owner = level.getChunkSource().getChunkNow(cx >> 4, cz >> 4);
                // Only where the land is well enough: a sick neighbour keeps its patch.
                if (owner == null || Thinning.get(owner) >= HEALS_BELOW) {
                    continue;
                }
                BlockPos ground = Thinning.surface(level, cx, cz);
                if (ground != null && healAt(level, ground, random, blessing(level, owner.getPos()))) {
                    healed++;
                    sound = ground;
                }
            }
        }
        if (sound != null) {
            level.playSound(null, sound, ModSounds.LAND_HEAL.get(), SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            // Where the land came back: a hexagon of life opening over it, the rune of growth rising.
            com.pockyl.hextale.network.RuneFxPayload.send(level, com.pockyl.hextale.network.RuneFxPayload.area(
                    net.minecraft.world.phys.Vec3.atBottomCenterOf(sound.above()), com.pockyl.hextale.rune.ComplexRunes.of(
                            com.pockyl.hextale.rune.Essence.LIFE, com.pockyl.hextale.rune.Essence.EARTH), 2.5F,
                    com.pockyl.hextale.network.RuneFxPayload.RUNE_COLOR));
            Quests.fireNear(level, Vec3.atCenterOf(sound), NOTE_RADIUS, Quests.LAND_HEALED);
        }
        return healed > 0;
    }

    private static boolean held(ServerLevel level, int x, int z) {
        BlockPos ground = Thinning.surface(level, x, z);
        return ground != null && level.getBlockState(ground).is(ModTags.VOID_GROUND);
    }

    /** The first Void ground a random line through ({@code x}, {@code z}) meets within the blight's reach, or null. */
    private static int[] heldNear(ServerLevel level, int x, int z, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        for (int step = 1; step <= Thinning.GROW_REACH; step++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                int cx = x + (int) Math.round(dx * step * sign);
                int cz = z + (int) Math.round(dz * step * sign);
                if (held(level, cx, cz)) {
                    return new int[] {cx, cz};
                }
            }
        }
        return null;
    }

    /** Any column of the chunk the Void still holds, or null. */
    private static int[] anyHeld(ServerLevel level, ChunkPos pos) {
        for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) {
            for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++) {
                if (held(level, x, z)) {
                    return new int[] {x, z};
                }
            }
        }
        return null;
    }

    /** Heals one column's top: the ground turns back, the Void's growth on it withers. */
    private static boolean healAt(ServerLevel level, BlockPos pos, RandomSource random, int blessing) {
        BlockState ground = level.getBlockState(pos);
        if (!ground.is(ModTags.VOID_GROUND) || ground.is(ModTags.VOID_IMMUNE)) {
            return false;
        }
        BlockState healed = restored(level, pos, ground);
        level.setBlockAndUpdate(pos, healed);
        Vec3 at = Vec3.atCenterOf(pos).add(0, 0.55, 0);
        level.sendParticles(ModParticles.glint(0xE8F2C8), at.x, at.y, at.z, 3, 0.35, 0.05, 0.35, 0);
        level.sendParticles(ModParticles.vapour(0x3A2440), at.x, at.y, at.z, 2, 0.3, 0.05, 0.3, 0.01);
        BlockPos above = pos.above();
        BlockState plant = level.getBlockState(above);
        if (plant.getBlock() instanceof VoidFloraBlock && !plant.is(ModTags.VOID_IMMUNE)) {
            BlockState dead = Blocks.DEAD_BUSH.defaultBlockState();
            level.setBlockAndUpdate(above, random.nextFloat() < 0.4F && dead.canSurvive(level, above) ? dead : Blocks.AIR.defaultBlockState());
            level.sendParticles(ModParticles.vapour(0x2A1A30), above.getX() + 0.5, above.getY() + 0.3, above.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.01);
            plant = level.getBlockState(above);
        }
        if (blessing > 0 && plant.isAir() && healed.is(Blocks.GRASS_BLOCK) && random.nextFloat() < com.pockyl.hextale.runework.PurifierRune.bloomChance(blessing)) {
            BlockState bloom = bloom(random, blessing);
            if (bloom.canSurvive(level, above)) {
                level.setBlockAndUpdate(above, bloom);
                level.sendParticles(ModParticles.glint(0xFFE08A), above.getX() + 0.5, above.getY() + 0.4, above.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0);
            }
        }
        return true;
    }

    /** What a Fertility-blessed spot sprouts: grass mostly, flowers more often the stronger the rune. */
    private static BlockState bloom(RandomSource random, int blessing) {
        if (random.nextFloat() < 0.55F - blessing * 0.08F) {
            return Blocks.SHORT_GRASS.defaultBlockState();
        }
        Block[] flowers = {Blocks.DANDELION, Blocks.POPPY, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.ALLIUM};
        return flowers[random.nextInt(flowers.length)].defaultBlockState();
    }

    /**
     * What the Void's ground was: the land remembers what lies under it. Soil over sand, red sand or gravel becomes that
     * (sandstone under it gives its sand); over anything else it is earth, grass when the sky is open above it. Rock
     * becomes the rock under it, else stone.
     */
    public static BlockState restored(ServerLevel level, BlockPos pos, BlockState ground) {
        BlockState below = level.getBlockState(pos.below());
        if (ground.is(com.pockyl.hextale.registry.ModBlocks.VOID_ROCK.get())) {
            return below.is(ModTags.BLIGHTS_TO_ROCK) ? below.getBlock().defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        if (below.is(Blocks.SAND) || below.is(Blocks.SANDSTONE)) {
            return Blocks.SAND.defaultBlockState();
        }
        if (below.is(Blocks.RED_SAND) || below.is(Blocks.RED_SANDSTONE)) {
            return Blocks.RED_SAND.defaultBlockState();
        }
        if (below.is(Blocks.GRAVEL)) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (below.is(Blocks.MUD)) {
            return Blocks.MUD.defaultBlockState();
        }
        return level.canSeeSky(pos.above()) ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SCARRED.clear();
        BLESSED.clear();
    }
}
