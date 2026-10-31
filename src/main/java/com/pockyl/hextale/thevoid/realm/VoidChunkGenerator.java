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
import net.minecraft.world.level.block.RotatedPillarBlock;
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

    public VoidShapes shapes(RandomState random) {
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
        features(kinds, chunkX << 4, chunkZ << 4);
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
                    BlockState state = state(kind, (chunkX << 4) + x, worldY, (chunkZ << 4) + z);
                    section.setBlockState(x, worldY & 15, z, state, false);
                    if (kind == SPRING) {
                        // Wakes when the chunk is done, so the ichor starts to pour.
                        chunk.markPosForPostProcessing(new net.minecraft.core.BlockPos((chunkX << 4) + x, worldY, (chunkZ << 4) + z));
                    } else if (kind >= CHEST && kind < CHEST + 4) {
                        // What the swallowed house still holds.
                        BlockPos at = new BlockPos((chunkX << 4) + x, worldY, (chunkZ << 4) + z);
                        var chest = new net.minecraft.world.level.block.entity.ChestBlockEntity(at, state);
                        chest.setLootTable(RUIN_LOOT, VoidShapes.hash(at.getX(), at.getY(), at.getZ(), 0xC4E5) > 0.5 ? at.asLong() : ~at.asLong());
                        chunk.setBlockEntity(chest);
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
    /** Husk crust over a hollow. */
    private static final byte CRUST = 63;
    // The swallowed ruins: our world's blocks, gnawed; a chest (four kinds, by the way it faces).
    private static final byte RUIN_COBBLE = 64;
    private static final byte RUIN_PLANKS = 65;
    private static final byte RUIN_STONE_BRICKS = 66;
    private static final byte RUIN_BRICKS = 67;
    private static final byte CHEST = 68;
    /** Sacs turned to face away from what they hang on: 6 kinds per sac (a {@link Direction} each) from here. */
    private static final byte SAC_FACING = 80;
    private static final byte PEARL_FACING = 86;
    private static final byte MIMIC_FACING = 92;
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> RUIN_LOOT =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    com.pockyl.hextale.Hextale.id("chests/swallowed_ruin"));

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
                    if (kind == VoidShapes.SAC || kind == VoidShapes.PEARL_SAC) {
                        kinds[i] = sacFacing(kinds, x, y, z, kind == VoidShapes.SAC ? SAC_FACING
                                : VoidShapes.hash(x0 + x, y, z0 + z, 0x313C) < 0.4 ? MIMIC_FACING : PEARL_FACING);
                        continue;
                    }
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

    /**
     * A sac at (x, y, z) facing away from the first solid neighbour (the floor first, then the walls, then a ceiling):
     * {@code base} + the facing's index, or air when nothing holds it.
     */
    private static byte sacFacing(byte[] kinds, int x, int y, int z, byte base) {
        Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};
        for (Direction on : order) {
            int nx = x + on.getStepX(), ny = y + on.getStepY(), nz = z + on.getStepZ();
            if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || ny < 0 || ny >= VoidShapes.HEIGHT) {
                continue;
            }
            byte near = kinds[VoidShapes.index(nx, ny, nz)];
            if (solid(near) && near != VoidShapes.MEMBRANE && near != VoidShapes.WORN) {
                return (byte) (base + on.getOpposite().ordinal());
            }
        }
        return VoidShapes.AIR;
    }

    /**
     * What lies on the plates once they are dressed: husk crust grown over hollows (deeper in, more of it), the
     * swallowed ruins on the shoals' islands, now and then a field of spore puffs at the Threshold and in the Reach.
     */
    private static void features(byte[] kinds, int x0, int z0) {
        VoidZones.Zone zone = VoidZones.zone(x0 + 8, z0 + 8);
        double crust = switch (zone) {
            case THRESHOLD -> 0.0;
            case REACH -> 0.12;
            case SHOALS -> 0.2;
            case DRIFT -> 0.3;
            case VORTEX -> 0.3;
        };
        if (VoidShapes.hash(x0, 0, z0, 0xC705) < crust) {
            crust(kinds, x0, z0);
        }
        if (zone == VoidZones.Zone.SHOALS && VoidShapes.hash(x0, 1, z0, 0x2B1A) < 0.5) {
            ruin(kinds, x0, z0);
        }
        if ((zone == VoidZones.Zone.THRESHOLD || zone == VoidZones.Zone.REACH) && VoidShapes.hash(x0, 2, z0, 0x5F0E) < 0.05) {
            spores(kinds);
        }
    }

    /** The topmost open ground in the column, or -1. */
    private static int top(byte[] kinds, int x, int z, int from, int to) {
        for (int y = Math.min(from, VoidShapes.HEIGHT - 3); y >= Math.max(1, to); y--) {
            byte kind = kinds[VoidShapes.index(x, y, z)];
            if (solid(kind) && !solid(kinds[VoidShapes.index(x, y + 1, z)]) && !solid(kinds[VoidShapes.index(x, y + 2, z)])) {
                return y;
            }
        }
        return -1;
    }

    /** A patch of crust over a hollow carved into a plate; a few crystals in the hollow's bottom for who breaks in. */
    private static void crust(byte[] kinds, int x0, int z0) {
        int cx = 4 + (int) (VoidShapes.hash(x0, 3, z0, 0xC706) * 8);
        int cz = 4 + (int) (VoidShapes.hash(x0, 4, z0, 0xC707) * 8);
        int y = top(kinds, cx, cz, VoidShapes.HEIGHT - 3, 20);
        if (y < 0) {
            return;
        }
        byte ground = kinds[VoidShapes.index(cx, y, cz)];
        if (ground != VoidShapes.SOIL && ground != VoidShapes.FLESH && ground != VoidShapes.CHITIN) {
            return;
        }
        double radius = 1.8 + VoidShapes.hash(x0, 5, z0, 0xC708) * 1.6;
        int depth = 3 + (int) (VoidShapes.hash(x0, 6, z0, 0xC709) * 3);
        int r = (int) Math.ceil(radius);
        // Only where the whole patch lies level: a crust on a slope would show its edge.
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= radius * radius && top(kinds, cx + dx, cz + dz, y + 1, y - 1) != y) {
                    return;
                }
            }
        }
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                int x = cx + dx, z = cz + dz;
                kinds[VoidShapes.index(x, y + 1, z)] = VoidShapes.AIR;
                kinds[VoidShapes.index(x, y, z)] = CRUST;
                for (int k = 1; k <= depth && y - k > 0; k++) {
                    kinds[VoidShapes.index(x, y - k, z)] = VoidShapes.AIR;
                }
                int bottom = y - depth - 1;
                if (bottom > 0 && solid(kinds[VoidShapes.index(x, bottom, z)]) && VoidShapes.hash(x0 + x, bottom, z0 + z, 0xC70A) < 0.25) {
                    kinds[VoidShapes.index(x, bottom + 1, z)] = HOLLOW;
                }
            }
        }
    }

    /**
     * A piece of our world the Void swallowed: a corner of a house or a laboratory on a shoal, walls bitten through,
     * a chest still standing in it.
     */
    private static void ruin(byte[] kinds, int x0, int z0) {
        int w = 5 + (int) (VoidShapes.hash(x0, 7, z0, 0x2B1B) * 3);
        int d = 5 + (int) (VoidShapes.hash(x0, 8, z0, 0x2B1C) * 3);
        // On the highest swallowed stone the chunk holds: the crown of a shoal.
        int ground = -1, cx = 8, cz = 8;
        for (int x = 3; x <= 12; x++) {
            for (int z = 3; z <= 12; z++) {
                int y = top(kinds, x, z, VoidShapes.HEIGHT - 3, 20);
                if (y > ground && (kinds[VoidShapes.index(x, y, z)] == VoidShapes.GNAWED || kinds[VoidShapes.index(x, y - 1, z)] == VoidShapes.GNAWED)) {
                    ground = y;
                    cx = x;
                    cz = z;
                }
            }
        }
        if (ground < 0) {
            return;
        }
        int ax = Math.max(1, Math.min(15 - w, cx - w / 2));
        int az = Math.max(1, Math.min(15 - d, cz - d / 2));
        boolean stone = VoidShapes.hash(x0, 11, z0, 0x2B1F) < 0.4;
        byte low = stone ? RUIN_STONE_BRICKS : RUIN_COBBLE;
        byte high = stone ? RUIN_BRICKS : RUIN_PLANKS;
        byte floorKind = stone ? RUIN_STONE_BRICKS : RUIN_PLANKS;
        int floor = ground + 1;
        // The wall it leans on longest is whole up to three blocks; the opposite one has been eaten to a stub.
        for (int x = ax; x < ax + w; x++) {
            for (int z = az; z < az + d; z++) {
                int local = top(kinds, x, z, floor + 2, floor - 6);
                if (local < 0) {
                    continue;
                }
                for (int y = local + 1; y < floor; y++) {
                    kinds[VoidShapes.index(x, y, z)] = VoidShapes.GNAWED;
                }
                boolean eaten = VoidShapes.noise((x0 + x) / 2.5, floor / 2.5, (z0 + z) / 2.5, 0x2B20) > 0.7;
                if (!eaten) {
                    kinds[VoidShapes.index(x, floor, z)] = floorKind;
                }
                boolean wall = x == ax || z == az || x == ax + w - 1 || z == az + d - 1;
                if (!wall) {
                    continue;
                }
                // Height falls off across the ruin: whole at the corner (ax, az), gone at the far one.
                double share = 1 - ((x - ax) / (double) w + (z - az) / (double) d) * 0.5;
                int height = (int) Math.round(share * 3.4 + (VoidShapes.hash(x0 + x, 0, z0 + z, 0x2B21) - 0.5));
                boolean door = (x == ax && z == az + d / 2) || (z == az && x == ax + w / 2);
                for (int k = 1; k <= height; k++) {
                    int y = floor + k;
                    if (door && k <= 2 || VoidShapes.noise((x0 + x) / 2.2, y / 2.2, (z0 + z) / 2.2, 0x2B22) > 0.66) {
                        continue;
                    }
                    kinds[VoidShapes.index(x, y, z)] = k == 1 ? low : high;
                }
            }
        }
        // The chest against the whole wall, facing into the room.
        int chestX = ax + 1 + (int) (VoidShapes.hash(x0, 12, z0, 0x2B23) * (w - 2));
        int chestZ = az + 1;
        kinds[VoidShapes.index(chestX, floor, chestZ)] = floorKind;
        kinds[VoidShapes.index(chestX, floor + 1, chestZ)] = CHEST;
        kinds[VoidShapes.index(chestX, floor + 2, chestZ)] = VoidShapes.AIR;
    }

    /** A field of spore puffs: the plates' soil here grew little else. */
    private static void spores(byte[] kinds) {
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int y = top(kinds, x, z, VoidShapes.HEIGHT - 3, 20);
                if (y >= 0 && kinds[VoidShapes.index(x, y, z)] == VoidShapes.SOIL && VoidShapes.hash(x, y, z, 0x5F0F) < 0.35) {
                    kinds[VoidShapes.index(x, y + 1, z)] = SPORE;
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
        return kind != VoidShapes.AIR && kind != VoidShapes.CRYSTAL && kind != VoidShapes.SAC && kind != VoidShapes.PEARL_SAC && kind < FLORA;
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
            case VoidShapes.GNAWED -> gnawed(x, y, z);
            case VoidShapes.ICHOR, SPRING -> ModBlocks.VOID_ICHOR.get().defaultBlockState();
            case VoidShapes.LOG_X -> ModBlocks.SINEW_WILLOW_WOOD.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            case VoidShapes.LOG_Y -> ModBlocks.SINEW_WILLOW_LOG.get().defaultBlockState();
            case VoidShapes.LOG_Z -> ModBlocks.SINEW_WILLOW_WOOD.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
            case VoidShapes.FRONDS -> SinewWillow.wildFronds();
            case VoidShapes.POD -> SinewWillow.wildPod(VoidShapes.hash(x, y, z, 0x90D));
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
            case CRUST -> ModBlocks.HUSK_CRUST.get().defaultBlockState();
            case RUIN_COBBLE -> ModBlocks.GNAWED_COBBLESTONE.get().defaultBlockState();
            case RUIN_PLANKS -> ModBlocks.GNAWED_PLANKS.get().defaultBlockState();
            case RUIN_STONE_BRICKS -> ModBlocks.GNAWED_STONE_BRICKS.get().defaultBlockState();
            case RUIN_BRICKS -> ModBlocks.GNAWED_BRICKS.get().defaultBlockState();
            case CHEST, CHEST + 1, CHEST + 2, CHEST + 3 -> Blocks.CHEST.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.from2DDataValue(kind - CHEST));
            default -> sac(kind);
        };
    }

    private static BlockState sac(byte kind) {
        if (kind >= SAC_FACING && kind < MIMIC_FACING + 6) {
            Direction facing = Direction.values()[(kind - SAC_FACING) % 6];
            BlockState state = (kind < PEARL_FACING ? ModBlocks.VOID_SAC : kind < MIMIC_FACING ? ModBlocks.PEARL_SAC : ModBlocks.MIMIC_SAC)
                    .get().defaultBlockState();
            return state.setValue(VoidSacs.FACING, facing);
        }
        return Blocks.AIR.defaultBlockState();
    }

    /**
     * Gnawed stone, with the metal of our world the Void swallowed with it: small clots of iron through the shoals, rarer
     * gold, each in its own lumps.
     */
    private static BlockState gnawed(int x, int y, int z) {
        if (VoidShapes.noise(x / 2.6, y / 2.6, z / 2.6, 0x1205) > 0.8) {
            return ModBlocks.GNAWED_IRON_ORE.get().defaultBlockState();
        }
        if (VoidShapes.noise(x / 2.2, y / 2.2, z / 2.2, 0x601D) > 0.84) {
            return ModBlocks.GNAWED_GOLD_ORE.get().defaultBlockState();
        }
        return ModBlocks.GNAWED_STONE.get().defaultBlockState();
    }

    /** Every block of one chunk of the Void as the generator lays it (index {@link VoidShapes#index}); for tests and previews. */
    public static BlockState[] chunkStates(VoidShapes shapes, int chunkX, int chunkZ) {
        byte[] kinds = new byte[VOLUME];
        shapes.fill(chunkX, chunkZ, kinds, new float[VOLUME]);
        decorate(kinds, chunkX << 4, chunkZ << 4);
        features(kinds, chunkX << 4, chunkZ << 4);
        BlockState[] states = new BlockState[VOLUME];
        for (int y = 0; y < VoidShapes.HEIGHT; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int i = VoidShapes.index(x, y, z);
                    states[i] = state(kinds[i], (chunkX << 4) + x, VoidShapes.MIN_Y + y, (chunkZ << 4) + z);
                }
            }
        }
        return states;
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
        features(kinds, x & ~15, z & ~15);
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

    /**
     * What waits in a fresh chunk: a nursery's brood round its mound, and Pit Maws lying flush in open ground (the
     * ground under one is taken away, so it lies level with what is round it).
     */
    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        net.minecraft.world.level.ChunkPos chunk = region.getCenter();
        VoidShapes shapes = shapes(region.getLevel().getChunkSource().randomState());
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(VoidShapes.chunkKey(chunk.x(), chunk.z()) ^ region.getSeed());
        for (VoidShapes.Nursery nursery : shapes.nurseriesIn(chunk.x(), chunk.z())) {
            int brood = 4 + random.nextInt(3);
            for (int i = 0; i < brood; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double d = 6 + random.nextDouble() * (nursery.a() - 10);
                BlockPos at = BlockPos.containing(nursery.x() + Math.cos(angle) * d, nursery.floor() + 1.2, nursery.z() + Math.sin(angle) * d);
                spawn(region, com.pockyl.hextale.registry.ModEntities.VOIDLING.get(), at, mob -> {
                    if (mob instanceof com.pockyl.hextale.thevoid.Voidling voidling && random.nextInt(4) == 0) {
                        voidling.setStage(com.pockyl.hextale.thevoid.Voidling.GROWN);
                    }
                });
            }
        }
        double maws = switch (VoidZones.zone(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ())) {
            case THRESHOLD -> 0.02;
            case REACH -> 0.1;
            case SHOALS -> 0.08;
            case DRIFT -> 0.12;
            case VORTEX -> 0.16;
        };
        if (random.nextDouble() < maws) {
            pitMaw(region, chunk, random);
        }
    }

    private static <T extends net.minecraft.world.entity.Mob> void spawn(WorldGenRegion region, net.minecraft.world.entity.EntityType<T> type,
            BlockPos at, java.util.function.Consumer<T> setup) {
        if (!region.getBlockState(at).getCollisionShape(region, at).isEmpty()) {
            return;
        }
        T mob = type.create(region.getLevel(), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
        if (mob == null) {
            return;
        }
        mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, region.getRandom().nextFloat() * 360, 0);
        mob.finalizeSpawn(region, region.getCurrentDifficultyAt(at), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE, null);
        mob.setPersistenceRequired();
        setup.accept(mob);
        region.addFreshEntityWithPassengers(mob);
    }

    /** A Pit Maw on a level patch of open soil, skin or flesh, three by three, with room above it. */
    private static void pitMaw(WorldGenRegion region, net.minecraft.world.level.ChunkPos chunk, net.minecraft.util.RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < 6; attempt++) {
            int x = chunk.getMinBlockX() + 2 + random.nextInt(12);
            int z = chunk.getMinBlockZ() + 2 + random.nextInt(12);
            int y = region.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            pos.set(x, y, z);
            if (!maw(region.getBlockState(pos))) {
                continue;
            }
            boolean level = true;
            for (int dx = -1; dx <= 1 && level; dx++) {
                for (int dz = -1; dz <= 1 && level; dz++) {
                    BlockPos at = new BlockPos(x + dx, y, z + dz);
                    level = maw(region.getBlockState(at)) && region.getBlockState(at.above()).isAir() && region.getBlockState(at.above(2)).isAir()
                            && !region.getBlockState(at.below()).isAir();
                }
            }
            if (!level) {
                continue;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    region.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
            }
            PitMaw maw = com.pockyl.hextale.registry.ModEntities.PIT_MAW.get().create(region.getLevel(), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
            if (maw != null) {
                maw.snapTo(x + 0.5, y + 1 - maw.getBbHeight(), z + 0.5, random.nextFloat() * 360, 0);
                region.addFreshEntity(maw);
            }
            return;
        }
    }

    private static boolean maw(BlockState state) {
        return state.is(ModBlocks.VOID_SOIL.get()) || state.is(ModBlocks.VOID_MEMBRANE.get()) || state.is(ModBlocks.VEINED_VOID_MEMBRANE.get())
                || state.is(ModBlocks.VOID_ROCK.get());
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
