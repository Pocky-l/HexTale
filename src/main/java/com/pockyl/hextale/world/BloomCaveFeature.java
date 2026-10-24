package com.pockyl.hextale.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import com.pockyl.hextale.block.ShimmerbloomBlock;
import com.pockyl.hextale.registry.ModBlocks;

/**
 * Bloom caves: inside a {@link BloomRegions bloom region} the caves of a chunk light up with shimmerbloom - patches of
 * standing blooms on the floors with violet moss and moss carpet around them, clusters hanging from the ceilings above
 * and blooms sticking out of the walls around.
 * Outside the regions it places nothing, so a cave is either thick with blooms or has none. Runs once per chunk (the
 * placed feature has no placement modifiers: the origin is the chunk's lowest corner) and stays inside the chunk.
 */
public final class BloomCaveFeature extends Feature<NoneFeatureConfiguration> {
    /** Blooms grow only below this height and well under the surface. */
    public static final int TOP = 40;
    private static final int UNDER_SURFACE = 8;
    /** Tries per chunk at full strength; lush caves get half as many again. */
    private static final int TRIES = 160;
    private static final int FLOOR_SEARCH = 10;
    private static final int CEILING_SEARCH = 9;
    /** How far from a patch a wall is looked for. */
    private static final int WALL_SEARCH = 5;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public BloomCaveFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return place(context.level(), context.chunkGenerator(), context.random(), context.origin());
    }

    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        long seed = level.getSeed();
        int minX = origin.getX();
        int minZ = origin.getZ();
        float strength = Math.max(Math.max(BloomRegions.strength(seed, minX, minZ), BloomRegions.strength(seed, minX + 15, minZ + 15)),
                Math.max(Math.max(BloomRegions.strength(seed, minX + 15, minZ), BloomRegions.strength(seed, minX, minZ + 15)),
                        BloomRegions.strength(seed, minX + 8, minZ + 8)));
        if (strength <= 0) {
            return false;
        }
        int bottom = level.getMinBuildHeight() + 4;
        int top = Math.min(TOP, (level.getMaxBuildHeight() - 1) - 1);
        if (top <= bottom) {
            return false;
        }
        boolean placed = false;
        int tries = Math.round(TRIES * strength);
        if (level.getBiome(origin.atY((bottom + top) / 2).offset(8, 0, 8)).is(Biomes.LUSH_CAVES)) {
            tries += tries / 2;
        }
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < tries; i++) {
            int x = minX + 1 + random.nextInt(14);
            int z = minZ + 1 + random.nextInt(14);
            float here = BloomRegions.strength(seed, x, z);
            if (here <= 0 || random.nextFloat() > here * 1.4F) {
                continue;
            }
            int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - UNDER_SURFACE;
            int y = bottom + random.nextInt(top - bottom);
            if (y >= surface) {
                continue;
            }
            at.set(x, y, z);
            if (findFloor(level, at)) {
                placed |= patch(level, random, at.immutable(), minX, minZ);
            }
        }
        return placed;
    }

    /** Moves {@code at} down onto the first cave floor: air standing on a solid top. */
    private static boolean findFloor(WorldGenLevel level, BlockPos.MutableBlockPos at) {
        if (!level.getBlockState(at).isAir()) {
            return false;
        }
        for (int i = 0; i < FLOOR_SEARCH; i++) {
            BlockPos below = at.below();
            BlockState under = level.getBlockState(below);
            if (under.isFaceSturdy(level, below, Direction.UP)) {
                return true;
            }
            if (!under.isAir()) {
                return false;
            }
            at.move(Direction.DOWN);
        }
        return false;
    }

    /** A patch around a floor spot: blooms, moss under some, carpet around, hanging blooms on the ceiling above. */
    private static boolean patch(WorldGenLevel level, RandomSource random, BlockPos centre, int minX, int minZ) {
        boolean placed = false;
        int blooms = 2 + random.nextInt(4);
        for (int i = 0; i < blooms + 6; i++) {
            BlockPos spot = i == 0 ? centre : centre.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
            if (!inside(spot, minX, minZ) || !level.getBlockState(spot).isAir()) {
                continue;
            }
            BlockPos below = spot.below();
            BlockState ground = level.getBlockState(below);
            if (!ground.isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }
            if (mossable(ground) && random.nextInt(3) != 0) {
                level.setBlock(below, ModBlocks.SHIMMER_MOSS.get().defaultBlockState(), FLAGS);
            }
            if (i < blooms) {
                level.setBlock(spot, ModBlocks.SHIMMERBLOOM.get().defaultBlockState(), FLAGS);
                placed = true;
                hang(level, random, spot, minX, minZ);
                walls(level, random, spot, minX, minZ);
            } else if (random.nextInt(3) != 0) {
                level.setBlock(spot, ModBlocks.SHIMMER_MOSS_CARPET.get().defaultBlockState(), FLAGS);
                placed = true;
            }
        }
        return placed;
    }

    /** Looks up from a bloom for the cave ceiling and mostly hangs a small cluster of blooms from it. */
    private static void hang(WorldGenLevel level, RandomSource random, BlockPos from, int minX, int minZ) {
        if (random.nextInt(6) == 0) {
            return;
        }
        BlockPos.MutableBlockPos at = from.mutable();
        for (int i = 0; i < CEILING_SEARCH; i++) {
            at.move(Direction.UP);
            BlockState state = level.getBlockState(at);
            if (!state.isAir()) {
                if (i < 2) {
                    return;
                }
                int cluster = 1 + random.nextInt(3);
                for (int n = 0; n < cluster * 2; n++) {
                    BlockPos spot = at.below().offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
                    if (inside(spot, minX, minZ) && level.getBlockState(spot).isAir() && ShimmerbloomBlock.canGrow(level, spot, true)) {
                        level.setBlock(spot, ModBlocks.SHIMMERBLOOM.get().defaultBlockState().setValue(ShimmerbloomBlock.HANGING, true), FLAGS);
                        if (--cluster <= 0) {
                            return;
                        }
                    }
                }
                return;
            }
        }
    }

    /** Looks sideways from a bloom for the cave walls and sets blooms on a few of them, a little above the floor. */
    private static void walls(WorldGenLevel level, RandomSource random, BlockPos from, int minX, int minZ) {
        for (int n = random.nextInt(3); n > 0; n--) {
            Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos.MutableBlockPos at = from.mutable().move(Direction.UP, random.nextInt(3));
            for (int i = 0; i < WALL_SEARCH; i++) {
                BlockPos next = at.relative(side);
                if (!inside(next, minX, minZ)) {
                    break;
                }
                if (!level.getBlockState(next).isAir()) {
                    if (level.getBlockState(at).isAir() && ShimmerbloomBlock.canHold(level, at, side)) {
                        level.setBlock(at, ModBlocks.SHIMMERBLOOM.get().onWall(side), FLAGS);
                    }
                    break;
                }
                at.move(side);
            }
        }
    }

    private static boolean mossable(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(BlockTags.MOSS_REPLACEABLE);
    }

    private static boolean inside(BlockPos pos, int minX, int minZ) {
        return pos.getX() >= minX && pos.getX() < minX + 16 && pos.getZ() >= minZ && pos.getZ() < minZ + 16;
    }
}
