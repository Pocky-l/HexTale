package com.pockyl.hextale.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.FulguriteSpikeBlock;
import com.pockyl.hextale.block.HextechClusterBlock;
import com.pockyl.hextale.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A struck geode: an amethyst geode a few blocks under the surface that lightning hit long ago. The geode itself is an
 * ordinary {@link GeodeFeature} (its data names dormant hextech and scorched calcite); on top of it the bolt's way is
 * drawn: a fulgurite channel from the ground down into the hollow, dormant hextech and glowing druses around the point
 * where it broke in, and on the surface the tell a player sees from afar: a shallow crater fused into fulgurite with a pit
 * in the middle, glassy spikes standing on its rim and woken hextech crystals glowing on its floor. Never under water; one per cell of {@code spacing} chunks (see {@link #chosen}). Stays within the 3x3 chunks around its
 * chunk.
 */
public record StruckGeodeFeature(GeodeConfiguration geode, IntProvider depth, int spacing, int separation) implements FeatureConfiguration {
    public static final MapCodec<StruckGeodeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    GeodeConfiguration.CODEC.fieldOf("geode").forGetter(StruckGeodeFeature::geode),
                    IntProviders.codec(1, 24).fieldOf("depth").forGetter(StruckGeodeFeature::depth),
                    com.mojang.serialization.Codec.intRange(2, 128).fieldOf("spacing").forGetter(StruckGeodeFeature::spacing),
                    com.mojang.serialization.Codec.intRange(0, 127).fieldOf("separation").forGetter(StruckGeodeFeature::separation))
            .apply(i, StruckGeodeFeature::new));
    public static final ResourceKey<ConfiguredFeature<?, ?>> KEY = ResourceKey.create(Registries.CONFIGURED_FEATURE, Hextale.id("struck_geode"));
    /** From a geode's origin to its middle: its distribution points lie 4-6 blocks out on every axis. */
    private static final int CENTRE = 5;
    /** The channel leans this far off its axis at most. */
    private static final int WANDER = 2;
    /** Dormant hextech grows this far around the point where the channel broke into the hollow. */
    private static final int STRIKE_RADIUS = 3;
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    /** Salt of the cell pick, so struck geodes do not follow any vanilla structure's grid. */
    private static final int SALT = 51873409;
    /** The smallest crater radius (up to 1.2 more); its floor sinks up to {@link #DEPTH} blocks, its fused rim reaches RIM x radius. */
    private static final float CRATER = 3.4F;
    private static final float DEPTH = 2.4F;
    private static final float RIM = 1.15F;

    public MapCodec<StruckGeodeFeature> codec() {
        return CODEC;
    }

    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        // Placed by hand (/place feature, tests) it goes where it is asked; the world itself keeps them apart.
        if (!(level instanceof ServerLevel) && !chosen(level.getSeed(), SectionPos.blockToSectionCoord(origin.getX()), SectionPos.blockToSectionCoord(origin.getZ()))) {
            return false;
        }
        int x = origin.getX();
        int z = origin.getZ();
        int surface = surface(level, x, z);
        if (surface != level.getHeight(level instanceof ServerLevel ? Heightmap.Types.OCEAN_FLOOR : Heightmap.Types.OCEAN_FLOOR_WG, x, z)) {
            return false;
        }
        BlockPos ground = new BlockPos(x, surface - 1, z);
        int middle = ground.getY() - depth.sample(random) - CENTRE;
        if (middle - CENTRE - 4 < level.getMinY() + 4) {
            return false;
        }
        if (!Feature.GEODE.place(geode, level, chunkGenerator, random, new BlockPos(x - CENTRE, middle - CENTRE, z - CENTRE))) {
            return false;
        }
        BlockPos end = channel(level, random, ground, middle);
        strikePoint(level, random, end);
        surfaceMarks(level, random, ground);
        return true;
    }

    /**
     * The bolt's way down: fulgurite from the ground to the hollow, leaning a little and putting out short side shoots.
     * Amethyst on the way turns into dormant hextech. Returns where it stopped (in the hollow, or at the middle).
     */
    private static BlockPos channel(WorldGenLevel level, RandomSource random, BlockPos ground, int middle) {
        BlockPos.MutableBlockPos pos = ground.mutable();
        int dx = 0;
        int dz = 0;
        while (pos.getY() > middle) {
            BlockState state = level.getBlockState(pos);
            if (pos.getY() < ground.getY() - 2 && state.isAir()) {
                return pos.immutable();
            }
            fuse(level, pos, state);
            if (random.nextInt(4) == 0) {
                fuse(level, pos.offset(random.nextBoolean() ? 1 : -1, 0, 0), null);
            }
            if (random.nextInt(9) == 0) {
                shoot(level, random, pos.immutable());
            }
            if (random.nextInt(3) == 0) {
                int step = random.nextBoolean() ? 1 : -1;
                if (random.nextBoolean() && Math.abs(dx + step) <= WANDER) {
                    dx += step;
                    pos.move(step, 0, 0);
                    fuse(level, pos, level.getBlockState(pos));
                } else if (Math.abs(dz + step) <= WANDER) {
                    dz += step;
                    pos.move(0, 0, step);
                    fuse(level, pos, level.getBlockState(pos));
                }
            }
            pos.move(0, -1, 0);
        }
        return pos.immutable();
    }

    /** A short shoot off the channel, sideways and down. */
    private static void shoot(WorldGenLevel level, RandomSource random, BlockPos from) {
        int sx = random.nextInt(3) - 1;
        int sz = sx == 0 ? (random.nextBoolean() ? 1 : -1) : random.nextInt(3) - 1;
        BlockPos.MutableBlockPos pos = from.mutable();
        for (int i = 2 + random.nextInt(3); i > 0; i--) {
            pos.move(sx, random.nextInt(3) == 0 ? 0 : -1, sz);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return;
            }
            fuse(level, pos, state);
        }
    }

    /** Turns one block on the bolt's way: amethyst wakes into dormant hextech, ground melts into fulgurite. */
    private static void fuse(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (state == null) {
            state = level.getBlockState(pos);
        }
        if (state.isAir() || !state.getFluidState().isEmpty() || state.is(BlockTags.FEATURES_CANNOT_REPLACE)) {
            return;
        }
        if (state.is(Blocks.AMETHYST_BLOCK) || state.is(Blocks.BUDDING_AMETHYST)) {
            level.setBlock(pos, ModBlocks.DORMANT_HEXTECH.get().defaultBlockState(), FLAGS);
        } else if (!state.is(ModBlocks.DORMANT_HEXTECH.get())) {
            level.setBlock(pos, ModBlocks.FULGURITE.get().defaultBlockState(), FLAGS);
        }
    }

    /** Around the point where the bolt broke into the hollow most of the amethyst woke into dormant hextech. */
    private static void strikePoint(WorldGenLevel level, RandomSource random, BlockPos end) {
        for (BlockPos pos : BlockPos.betweenClosed(end.offset(-STRIKE_RADIUS, -STRIKE_RADIUS, -STRIKE_RADIUS),
                end.offset(STRIKE_RADIUS, STRIKE_RADIUS, STRIKE_RADIUS))) {
            double distance = Math.sqrt(pos.distSqr(end));
            if (distance > STRIKE_RADIUS + 0.5 || random.nextFloat() > 1.1F - distance / (STRIKE_RADIUS + 1)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.AMETHYST_BLOCK) || state.is(Blocks.BUDDING_AMETHYST)) {
                level.setBlock(pos, ModBlocks.DORMANT_HEXTECH.get().defaultBlockState(), FLAGS);
            } else {
                Block crystal = woken(state);
                if (crystal != null) {
                    level.setBlock(pos, crystal.defaultBlockState()
                            .setValue(HextechClusterBlock.FACING, state.getValue(HextechClusterBlock.FACING))
                            .setValue(HextechClusterBlock.WATERLOGGED, state.getValue(HextechClusterBlock.WATERLOGGED)), FLAGS);
                }
            }
        }
    }

    /**
     * Whether this chunk is the one of its cell where a struck geode may lie: the world is cut into cells of
     * {@code spacing} chunks and each picks one chunk at least {@code separation} chunks from the next cell's pick (like
     * a structure's random spread), so two geodes never stand close together.
     */
    public boolean chosen(long seed, int chunkX, int chunkZ) {
        int cellX = Math.floorDiv(chunkX, spacing);
        int cellZ = Math.floorDiv(chunkZ, spacing);
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureWithSalt(seed, cellX, cellZ, SALT);
        int spread = Math.max(1, spacing - separation);
        return chunkX == cellX * spacing + random.nextInt(spread) && chunkZ == cellZ * spacing + random.nextInt(spread);
    }

    /** The crater with the spikes and crystals in it. */
    private void surfaceMarks(WorldGenLevel level, RandomSource random, BlockPos ground) {
        float radius = CRATER + random.nextFloat() * 1.2F;
        List<BlockPos> floor = crater(level, random, ground, radius);
        spikes(level, random, ground, radius);
        druses(level, random, floor);
    }

    /**
     * A shallow bowl dug by the strike, its floor fused into fulgurite and its rim half fused; in the middle a pit lined
     * with fulgurite drops into the channel. Returns the floor blocks near the middle (where druses may grow).
     */
    private static List<BlockPos> crater(WorldGenLevel level, RandomSource random, BlockPos centre, float radius) {
        List<BlockPos> floor = new ArrayList<>();
        int reach = Mth.ceil(radius * RIM) + 1;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                float d = (Mth.sqrt(dx * dx + dz * dz) + (random.nextFloat() - 0.5F) * 0.7F) / radius;
                if (d > RIM) {
                    continue;
                }
                BlockPos top = clearedTop(level, centre.getX() + dx, centre.getZ() + dz);
                if (top == null) {
                    continue;
                }
                int depth = d < 1 ? Math.round((1 - d * d) * DEPTH) : 0;
                for (int i = 0; i < depth; i++) {
                    level.setBlock(top.below(i), Blocks.AIR.defaultBlockState(), FLAGS);
                }
                BlockPos bottom = top.below(depth);
                if (d < 0.75F || d < 1 && random.nextFloat() < 0.6F || random.nextFloat() < 0.3F) {
                    level.setBlock(bottom, ModBlocks.FULGURITE.get().defaultBlockState(), FLAGS);
                }
                if (d < 0.8F && (dx != 0 || dz != 0)) {
                    floor.add(bottom);
                }
            }
        }
        // The pit: the bolt went in here, two blocks down into the channel, its walls fused.
        BlockPos pit = new BlockPos(centre.getX(), surface(level, centre.getX(), centre.getZ()) - 1, centre.getZ());
        for (int i = 0; i < 2; i++) {
            BlockPos at = pit.below(i);
            level.setBlock(at, Blocks.AIR.defaultBlockState(), FLAGS);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                fuse(level, at.relative(side), null);
            }
        }
        return floor;
    }

    /** Glassy spikes standing up out of the crater: most on the rim, one or two taller ones inside. */
    private static void spikes(WorldGenLevel level, RandomSource random, BlockPos centre, float radius) {
        int rim = 4 + random.nextInt(4);
        int inner = 1 + random.nextInt(2);
        for (int i = 0; i < rim + inner; i++) {
            boolean inside = i >= rim;
            float angle = random.nextFloat() * Mth.TWO_PI;
            float distance = radius * (inside ? 0.35F + random.nextFloat() * 0.3F : 0.85F + random.nextFloat() * 0.4F);
            int height = inside ? 2 + random.nextInt(3) : 1 + random.nextInt(3);
            spike(level, centre.getX() + Mth.floor(Mth.cos(angle) * distance + 0.5F),
                    centre.getZ() + Mth.floor(Mth.sin(angle) * distance + 0.5F), height);
        }
    }

    private static void spike(WorldGenLevel level, int x, int z, int height) {
        BlockPos top = clearedTop(level, x, z);
        if (top == null || !level.getBlockState(top).isFaceSturdy(level, top, Direction.UP)) {
            return;
        }
        BlockState spike = ModBlocks.FULGURITE_SPIKE.get().defaultBlockState();
        int built = 0;
        while (built < height && level.getBlockState(top.above(built + 1)).isAir()) {
            built++;
        }
        for (int i = 1; i <= built; i++) {
            FulguriteSpikeBlock.Part part = i == built ? FulguriteSpikeBlock.Part.TIP : i == 1 ? FulguriteSpikeBlock.Part.BASE
                    : FulguriteSpikeBlock.Part.MIDDLE;
            level.setBlock(top.above(i), spike.setValue(FulguriteSpikeBlock.PART, part), FLAGS);
        }
    }

    /** The hextech crystal an amethyst bud or cluster wakes into, of its own size; null for anything else. */
    @Nullable
    private static Block woken(BlockState state) {
        if (state.is(Blocks.AMETHYST_CLUSTER)) {
            return ModBlocks.HEXTECH_CLUSTER.get();
        } else if (state.is(Blocks.LARGE_AMETHYST_BUD)) {
            return ModBlocks.LARGE_HEXTECH_BUD.get();
        } else if (state.is(Blocks.MEDIUM_AMETHYST_BUD)) {
            return ModBlocks.MEDIUM_HEXTECH_BUD.get();
        } else if (state.is(Blocks.SMALL_AMETHYST_BUD)) {
            return ModBlocks.SMALL_HEXTECH_BUD.get();
        }
        return null;
    }

    /** Woken hextech crystals of every size on the crater's floor: the glow that gives a struck geode away at night. */
    private static void druses(WorldGenLevel level, RandomSource random, List<BlockPos> floor) {
        int count = 4 + random.nextInt(4);
        for (int i = 0; i < count && !floor.isEmpty(); i++) {
            BlockPos at = floor.remove(random.nextInt(floor.size()));
            if (level.getBlockState(at.above()).isAir() && level.getBlockState(at).isFaceSturdy(level, at, Direction.UP)) {
                Block crystal = switch (random.nextInt(4)) {
                    case 0 -> ModBlocks.SMALL_HEXTECH_BUD.get();
                    case 1 -> ModBlocks.MEDIUM_HEXTECH_BUD.get();
                    case 2 -> ModBlocks.LARGE_HEXTECH_BUD.get();
                    default -> ModBlocks.HEXTECH_CLUSTER.get();
                };
                level.setBlock(at.above(), crystal.defaultBlockState().setValue(HextechClusterBlock.FACING, Direction.UP), FLAGS);
            }
        }
    }

    /**
     * The top ground block of a column with whatever grew on it burned away; null when the top is not ground (water,
     * a tree, something built).
     */
    @Nullable
    private static BlockPos clearedTop(WorldGenLevel level, int x, int z) {
        BlockPos top = new BlockPos(x, surface(level, x, z) - 1, z);
        BlockState state = level.getBlockState(top);
        if (state.canBeReplaced() && !state.isAir() && state.getFluidState().isEmpty()) {
            level.setBlock(top, Blocks.AIR.defaultBlockState(), FLAGS);
            top = top.below();
            state = level.getBlockState(top);
        }
        return ground(state) || state.is(ModBlocks.FULGURITE.get()) ? top : null;
    }

    private static boolean ground(BlockState state) {
        return state.is(BlockTags.SUBSTRATE_OVERWORLD) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.SNOW_BLOCK) || state.is(BlockTags.TERRACOTTA);
    }

    /** The first free height of a column: the worldgen heightmap while the world generates, the live one when placed by hand. */
    private static int surface(WorldGenLevel level, int x, int z) {
        return level.getHeight(level instanceof ServerLevel ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }

    /** The 26.1.2 feature type: places the configured record (26.3 records are features themselves). */
    public static final class Type extends Feature<StruckGeodeFeature> {
        public Type() {
            super(CODEC.codec());
        }

        @Override
        public boolean place(FeaturePlaceContext<StruckGeodeFeature> context) {
            return context.config().place(context.level(), context.chunkGenerator(), context.random(), context.origin());
        }
    }
}
