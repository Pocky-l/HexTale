package com.pockyl.hextale.thevoid.realm;

import com.mojang.serialization.MapCodec;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModBlocks;

/**
 * Builds the Void out of {@link VoidShapes}: no ground, no layers, no heightmap - ribs, skin, husks and swallowed
 * islands over a bottomless glow. Ores of our world sit in the swallowed stone, rift ore in the husks' walls, Void
 * crystals on the geodes the skin sags round, hollow crystals and the Void's own plants on whatever lies open.
 */
public class VoidChunkGenerator extends ChunkGenerator {
    public static final MapCodec<VoidChunkGenerator> CODEC = BiomeSource.CODEC.fieldOf("biome_source")
            .xmap(VoidChunkGenerator::new, ChunkGenerator::getBiomeSource).stable();

    private static final int VOLUME = 16 * 16 * VoidShapes.HEIGHT;
    private static final ThreadLocal<byte[]> KINDS = ThreadLocal.withInitial(() -> new byte[VOLUME]);
    private static final ThreadLocal<float[]> BEST = ThreadLocal.withInitial(() -> new float[VOLUME]);

    private volatile @Nullable VoidShapes shapes;

    public VoidChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private VoidShapes shapes(RandomState random) {
        VoidShapes known = shapes;
        if (known == null || known.seed() != random.seed()) {
            known = new VoidShapes(random.seed());
            shapes = known;
        }
        return known;
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState random, StructureManager structures,
            BiomeManager biomes, @Nullable WorldGenRegion carverRegion, Set<Holder<Biome>> possibleBiomes) {
        byte[] kinds = KINDS.get();
        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        shapes(random).fill(chunkX, chunkZ, kinds, BEST.get());
        decorate(kinds, chunkX << 4, chunkZ << 4);
        BlockState[] palette = palette();
        int minY = chunk.getMinY();
        for (int y = 0; y < VoidShapes.HEIGHT; y++) {
            int worldY = VoidShapes.MIN_Y + y;
            if (worldY < minY || worldY >= minY + chunk.getHeight()) {
                continue;
            }
            LevelChunkSection section = null;
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    byte kind = kinds[VoidShapes.index(x, y, z)];
                    if (kind == VoidShapes.AIR) {
                        continue;
                    }
                    if (section == null) {
                        section = chunk.getSection(chunk.getSectionIndex(worldY));
                    }
                    section.setBlockState(x, worldY & 15, z, state(kind, palette, (chunkX << 4) + x, worldY, (chunkZ << 4) + z), false);
                }
            }
        }
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
        return CompletableFuture.completedFuture(chunk);
    }

    // Kinds beyond the shapes' own, set by decorate.
    private static final byte FLORA = 40;
    private static final byte HOLLOW = 41;
    private static final byte CRYSTAL_UP = 42;

    /**
     * Plants on open flesh, hollow crystals on open swallowed stone, Void crystals kept only where something holds them.
     * One pass over the column, looking only up.
     */
    private static void decorate(byte[] kinds, int x0, int z0) {
        for (int y = 0; y < VoidShapes.HEIGHT - 1; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int i = VoidShapes.index(x, y, z);
                    byte kind = kinds[i];
                    int above = VoidShapes.index(x, y + 1, z);
                    if (kind == VoidShapes.CRYSTAL) {
                        byte below = y > 0 ? kinds[VoidShapes.index(x, y - 1, z)] : VoidShapes.AIR;
                        kinds[i] = solid(below) ? CRYSTAL_UP : VoidShapes.AIR;
                        continue;
                    }
                    if (kinds[above] != VoidShapes.AIR) {
                        continue;
                    }
                    double roll = VoidShapes.hash(x0 + x, y, z0 + z, 0xF10A);
                    if (kind == VoidShapes.FLESH && roll < 0.09) {
                        kinds[above] = FLORA;
                    } else if ((kind == VoidShapes.GNAWED || kind == VoidShapes.STONE || kind == VoidShapes.DEEP) && roll < 0.025) {
                        kinds[above] = HOLLOW;
                    }
                }
            }
        }
    }

    private static boolean solid(byte kind) {
        return kind != VoidShapes.AIR && kind != VoidShapes.CRYSTAL && kind < FLORA;
    }

    private static BlockState[] palette() {
        return new BlockState[]{
                Blocks.AIR.defaultBlockState(),
                ModBlocks.HUSK_CHITIN.get().defaultBlockState(),
                ModBlocks.HUSK_CHITIN_RIDGE.get().defaultBlockState(),
                ModBlocks.VOID_ROCK.get().defaultBlockState(),
                ModBlocks.VOID_MEMBRANE.get().defaultBlockState(),
                ModBlocks.WORN_VOID_MEMBRANE.get().defaultBlockState(),
                ModBlocks.VEINED_VOID_MEMBRANE.get().defaultBlockState(),
                ModBlocks.RIFT_ORE.get().defaultBlockState(),
                Blocks.MOSS_BLOCK.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(),
                Blocks.STONE.defaultBlockState(),
                Blocks.DEEPSLATE.defaultBlockState(),
                ModBlocks.GNAWED_STONE.get().defaultBlockState(),
        };
    }

    private static BlockState state(byte kind, BlockState[] palette, int x, int y, int z) {
        if (kind == FLORA) {
            double pick = VoidShapes.hash(x, y, z, 0xF20B);
            return (pick < 0.45 ? ModBlocks.VOID_ROOTS : pick < 0.8 ? ModBlocks.VOID_CORAL : ModBlocks.VOID_BULB).get().defaultBlockState();
        }
        if (kind == HOLLOW) {
            return ModBlocks.HOLLOW_CRYSTAL_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP);
        }
        if (kind == CRYSTAL_UP) {
            return ModBlocks.VOID_CRYSTAL_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP);
        }
        if (kind == VoidShapes.STONE || kind == VoidShapes.DEEP) {
            BlockState ore = ore(kind == VoidShapes.DEEP, x, y, z);
            if (ore != null) {
                return ore;
            }
        }
        return palette[kind];
    }

    /** Ores of our world, as they were in the stone the Void swallowed - in small clumps, by a hash of the block. */
    private static @Nullable BlockState ore(boolean deep, int x, int y, int z) {
        double vein = VoidShapes.noise(x / 2.2, y / 2.2, z / 2.2, 0x0AE5);
        if (vein < 0.78) {
            return null;
        }
        double pick = VoidShapes.hash(x >> 2, y >> 2, z >> 2, 0x0AE6);
        if (pick < 0.3) {
            return (deep ? Blocks.DEEPSLATE_COAL_ORE : Blocks.COAL_ORE).defaultBlockState();
        } else if (pick < 0.52) {
            return (deep ? Blocks.DEEPSLATE_IRON_ORE : Blocks.IRON_ORE).defaultBlockState();
        } else if (pick < 0.7) {
            return (deep ? Blocks.DEEPSLATE_COPPER_ORE : Blocks.COPPER_ORE).defaultBlockState();
        } else if (pick < 0.8) {
            return (deep ? Blocks.DEEPSLATE_REDSTONE_ORE : Blocks.REDSTONE_ORE).defaultBlockState();
        } else if (pick < 0.88) {
            return (deep ? Blocks.DEEPSLATE_LAPIS_ORE : Blocks.LAPIS_ORE).defaultBlockState();
        } else if (pick < 0.985) {
            return (deep ? Blocks.DEEPSLATE_GOLD_ORE : Blocks.GOLD_ORE).defaultBlockState();
        }
        return (deep ? Blocks.DEEPSLATE_DIAMOND_ORE : Blocks.DIAMOND_ORE).defaultBlockState();
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        BlockState[] column = column(x, z, random);
        for (int y = column.length - 1; y >= 0; y--) {
            if (type.isOpaque().test(column[y])) {
                return VoidShapes.MIN_Y + y + 1;
            }
        }
        return level.getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        return new NoiseColumn(VoidShapes.MIN_Y, column(x, z, random));
    }

    /** One column of the Void (the whole chunk is worked out; columns are only asked for rarely). */
    private BlockState[] column(int x, int z, RandomState random) {
        byte[] kinds = new byte[VOLUME];
        shapes(random).fill(x >> 4, z >> 4, kinds, new float[VOLUME]);
        BlockState[] palette = palette();
        BlockState[] column = new BlockState[VoidShapes.HEIGHT];
        for (int y = 0; y < VoidShapes.HEIGHT; y++) {
            byte kind = kinds[VoidShapes.index(x & 15, y, z & 15)];
            column[y] = kind < palette.length ? palette[kind] : Blocks.AIR.defaultBlockState();
        }
        return column;
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState random, BlockPos pos, SamplerContext context) {
        result.add("Void zone: " + VoidZones.zone(pos.getX(), pos.getZ()).id() + String.format(" (depth %.2f)", VoidZones.depth(pos.getX(), pos.getZ())));
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getMinY() {
        return VoidShapes.MIN_Y;
    }

    @Override
    public int getGenDepth() {
        return VoidShapes.HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return VoidShapes.MIN_Y - 1;
    }
}
