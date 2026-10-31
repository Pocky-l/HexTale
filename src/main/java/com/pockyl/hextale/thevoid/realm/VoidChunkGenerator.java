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
 * Builds the Void out of {@link VoidShapes}: no layers, no heightmap - plates of shell, ribs, skin, husks and digested
 * masses over a bottomless glow, made of nothing but the Void's own matter. Rift ore sits in the husks' walls, Void
 * crystals on the geodes the skin sags round, hollow crystals on the digested masses, the Void's own plants on its soil
 * and flesh.
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
                    section.setBlockState(x, worldY & 15, z, state(kind, (chunkX << 4) + x, worldY, (chunkZ << 4) + z), false);
                    if (kind == SPRING) {
                        // Wakes when the chunk is done, so the ichor starts to pour.
                        chunk.markPosForPostProcessing(new net.minecraft.core.BlockPos((chunkX << 4) + x, worldY, (chunkZ << 4) + z));
                    }
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
    private static final byte CILIA = 50;
    private static final byte TALL_LOW = 51;
    private static final byte TALL_HIGH = 52;
    private static final byte SPORE = 53;
    private static final byte TENDRIL_BODY = 54;
    private static final byte TENDRIL_HEAD = 55;
    private static final byte SINEW = 56;
    private static final byte BLISTER = 57;
    /** A spring of ichor at a plate's lip: it pours over the edge into the abyss as a glowing fall. */
    private static final byte SPRING = 58;
    /** The fall of ichor pouring from a spring down into the abyss. */
    private static final byte FALL = 59;

    /**
     * Plants on open flesh, hollow crystals on open swallowed stone, Void crystals kept only where something holds them.
     * One pass over the column, looking only up.
     */
    private static void decorate(byte[] kinds, int x0, int z0) {
        for (int y = 1; y < VoidShapes.HEIGHT - 2; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int i = VoidShapes.index(x, y, z);
                    byte kind = kinds[i];
                    if (kind == VoidShapes.CRYSTAL) {
                        kinds[i] = solid(kinds[VoidShapes.index(x, y - 1, z)]) ? CRYSTAL_UP : VoidShapes.AIR;
                        continue;
                    }
                    if (kind == VoidShapes.AIR) {
                        curtain(kinds, x, y, z, x0, z0);
                        continue;
                    }
                    int below = VoidShapes.index(x, y - 1, z);
                    if (kinds[below] == VoidShapes.AIR && (kind == VoidShapes.CHITIN || kind == VoidShapes.RIDGE || kind == VoidShapes.FLESH)
                            && VoidShapes.hash(x0 + x, y, z0 + z, 0xF1D0) < 0.05) {
                        hang(kinds, x, y, z, x0, z0);
                    }
                    int above = VoidShapes.index(x, y + 1, z);
                    if (kinds[above] != VoidShapes.AIR) {
                        continue;
                    }
                    boolean room = kinds[VoidShapes.index(x, y + 2, z)] == VoidShapes.AIR;
                    double roll = VoidShapes.hash(x0 + x, y, z0 + z, 0xF10A);
                    int lip = (kind == VoidShapes.SOIL || kind == VoidShapes.FLESH) && roll > 0.9994 ? overLip(kinds, x, y, z) : -1;
                    if (lip >= 0) {
                        kinds[i] = SPRING;
                        int fx = x + LIP_SIDES[lip][0];
                        int fz = z + LIP_SIDES[lip][1];
                        for (int fy = y; fy >= 1 && kinds[VoidShapes.index(fx, fy, fz)] == VoidShapes.AIR; fy--) {
                            kinds[VoidShapes.index(fx, fy, fz)] = FALL;
                        }
                        continue;
                    }
                    if (kind == VoidShapes.SOIL) {
                        // The plates' own carpet: cilia everywhere, tall ones in places, puffballs and the Void's plants.
                        if (roll < 0.004 && inland(kinds, x, y, z)) {
                            kinds[i] = BLISTER;
                        } else if (roll < 0.3) {
                            kinds[above] = CILIA;
                        } else if (roll < 0.36 && room) {
                            kinds[above] = TALL_LOW;
                            kinds[VoidShapes.index(x, y + 2, z)] = TALL_HIGH;
                        } else if (roll < 0.38) {
                            kinds[above] = SPORE;
                        } else if (roll < 0.47) {
                            kinds[above] = FLORA;
                        }
                    } else if (kind == VoidShapes.FLESH) {
                        if (roll < 0.012) {
                            // Outcrops of Void crystal on open flesh.
                            kinds[above] = CRYSTAL_UP;
                        } else if (roll < 0.16) {
                            kinds[above] = FLORA;
                        } else if (roll < 0.2) {
                            kinds[above] = SPORE;
                        } else if (roll < 0.4) {
                            kinds[above] = CILIA;
                        }
                    } else if (kind == VoidShapes.GNAWED && roll < 0.03) {
                        kinds[above] = HOLLOW;
                    }
                }
            }
        }
    }

    private static final int[][] LIP_SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** The side (index into {@link #LIP_SIDES}) on which ground at (x, y, z) stands at a lip over a long drop, or -1. */
    private static int overLip(byte[] kinds, int x, int y, int z) {
        if (x < 1 || x > 14 || z < 1 || z > 14 || y < 12) {
            return -1;
        }
        for (int s = 0; s < LIP_SIDES.length; s++) {
            boolean drop = true;
            for (int dy = 0; dy >= -10 && drop; dy--) {
                drop = kinds[VoidShapes.index(x + LIP_SIDES[s][0], y + dy, z + LIP_SIDES[s][1])] == VoidShapes.AIR;
            }
            if (drop) {
                return s;
            }
        }
        return -1;
    }

    /** Whether the ground at (x, y, z) has ground all round it two blocks out (a blister there throws no one off a ledge). */
    private static boolean inland(byte[] kinds, int x, int y, int z) {
        if (x < 2 || x > 13 || z < 2 || z > 13) {
            return false;
        }
        for (int dx = -2; dx <= 2; dx += 2) {
            for (int dz = -2; dz <= 2; dz += 2) {
                if (!solid(kinds[VoidShapes.index(x + dx, y, z + dz)]) && !solid(kinds[VoidShapes.index(x + dx, y - 1, z + dz)])) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A strand of tendrils hanging from the underside of what is at (x, y, z), down through the air below it. */
    private static void hang(byte[] kinds, int x, int y, int z, int x0, int z0) {
        int length = 2 + (int) (VoidShapes.hash(x0 + x, y, z0 + z, 0xF1D1) * 9);
        int end = y - 1;
        for (int k = 1; k <= length && y - k >= 0; k++) {
            if (kinds[VoidShapes.index(x, y - k, z)] != VoidShapes.AIR) {
                break;
            }
            kinds[VoidShapes.index(x, y - k, z)] = TENDRIL_BODY;
            end = y - k;
        }
        if (end < y) {
            kinds[VoidShapes.index(x, end, z)] = TENDRIL_HEAD;
        }
    }

    /** Sinews stretched across a narrow gap of air between two pieces of chitin (spines of a thicket, a rib's crotch). */
    private static void curtain(byte[] kinds, int x, int y, int z, int x0, int z0) {
        if (x == 0 || x == 15 || z == 0 || z == 15) {
            return;
        }
        boolean acrossX = shell(kinds[VoidShapes.index(x - 1, y, z)]) && shell(kinds[VoidShapes.index(x + 1, y, z)]);
        boolean acrossZ = shell(kinds[VoidShapes.index(x, y, z - 1)]) && shell(kinds[VoidShapes.index(x, y, z + 1)]);
        if ((acrossX || acrossZ) && VoidShapes.hash(x0 + x, y, z0 + z, 0xF1D2) < 0.18) {
            kinds[VoidShapes.index(x, y, z)] = SINEW;
        }
    }

    private static boolean shell(byte kind) {
        return kind == VoidShapes.CHITIN || kind == VoidShapes.RIDGE;
    }

    private static boolean solid(byte kind) {
        return kind != VoidShapes.AIR && kind != VoidShapes.CRYSTAL && kind < FLORA;
    }

    private static BlockState state(byte kind, int x, int y, int z) {
        return switch (kind) {
            case VoidShapes.CHITIN -> ModBlocks.HUSK_CHITIN.get().defaultBlockState();
            case VoidShapes.RIDGE -> ModBlocks.HUSK_CHITIN_RIDGE.get().defaultBlockState();
            case VoidShapes.FLESH -> ModBlocks.VOID_ROCK.get().defaultBlockState();
            case VoidShapes.SOIL -> ModBlocks.VOID_SOIL.get().defaultBlockState();
            case VoidShapes.MEMBRANE -> ModBlocks.VOID_MEMBRANE.get().defaultBlockState();
            case VoidShapes.WORN -> ModBlocks.WORN_VOID_MEMBRANE.get().defaultBlockState();
            case VoidShapes.VEINED -> ModBlocks.VEINED_VOID_MEMBRANE.get().defaultBlockState();
            case VoidShapes.RIFT_ORE -> ModBlocks.RIFT_ORE.get().defaultBlockState();
            case VoidShapes.GNAWED -> ModBlocks.GNAWED_STONE.get().defaultBlockState();
            case VoidShapes.ICHOR, SPRING -> ModBlocks.VOID_ICHOR.get().defaultBlockState();
            case FALL -> com.pockyl.hextale.registry.ModFluids.FLOWING_VOID_ICHOR.get().getFlowing(8, true).createLegacyBlock();
            case CILIA -> ModBlocks.VOID_CILIA.get().defaultBlockState();
            case TALL_LOW -> ModBlocks.TALL_VOID_CILIA.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
            case TALL_HIGH -> ModBlocks.TALL_VOID_CILIA.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
            case SPORE -> ModBlocks.SPORE_PUFF.get().defaultBlockState();
            case TENDRIL_BODY -> ModBlocks.VOID_TENDRILS_PLANT.get().defaultBlockState();
            case TENDRIL_HEAD -> ModBlocks.VOID_TENDRILS.get().defaultBlockState();
            case SINEW -> ModBlocks.SINEW_CURTAIN.get().defaultBlockState();
            case BLISTER -> ModBlocks.SPRING_BLISTER.get().defaultBlockState();
            case FLORA -> {
                double pick = VoidShapes.hash(x, y, z, 0xF20B);
                yield (pick < 0.45 ? ModBlocks.VOID_ROOTS : pick < 0.8 ? ModBlocks.VOID_CORAL : ModBlocks.VOID_BULB).get().defaultBlockState();
            }
            case HOLLOW -> ModBlocks.HOLLOW_CRYSTAL_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP);
            case CRYSTAL_UP -> ModBlocks.VOID_CRYSTAL_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP);
            default -> Blocks.AIR.defaultBlockState();
        };
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
        decorate(kinds, x & ~15, z & ~15);
        BlockState[] column = new BlockState[VoidShapes.HEIGHT];
        for (int y = 0; y < VoidShapes.HEIGHT; y++) {
            column[y] = state(kinds[VoidShapes.index(x & 15, y, z & 15)], x, VoidShapes.MIN_Y + y, z);
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
