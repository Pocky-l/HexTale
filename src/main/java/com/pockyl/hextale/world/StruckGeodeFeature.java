package com.pockyl.hextale.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.GeodeFeature;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModBlocks;

/**
 * A struck geode: an amethyst geode a few blocks under the surface that lightning hit long ago. The geode itself is an
 * ordinary {@link GeodeFeature} (its data names dormant hextech and scorched calcite); on top of it the bolt's way is
 * drawn: a fulgurite channel from the ground down into the hollow, dormant hextech around the point where it broke in,
 * and on the surface a branching scar of fulgurite (a Lichtenberg figure) with a glassy pit in the middle - the one tell a
 * player sees from afar. Never under water. Stays within the 3x3 chunks around its chunk.
 */
public record StruckGeodeFeature(GeodeFeature geode, IntProvider depth, int scarReach) implements Feature {
    public static final MapCodec<StruckGeodeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    GeodeFeature.CODEC.fieldOf("geode").forGetter(StruckGeodeFeature::geode),
                    IntProviders.codec(1, 24).fieldOf("depth").forGetter(StruckGeodeFeature::depth),
                    Codec.intRange(2, 12).fieldOf("scar_reach").forGetter(StruckGeodeFeature::scarReach))
            .apply(i, StruckGeodeFeature::new));
    public static final ResourceKey<Feature> KEY = ResourceKey.create(Registries.FEATURE, Hextale.id("struck_geode"));
    /** From a geode's origin to its middle: its distribution points lie 4-6 blocks out on every axis. */
    private static final int CENTRE = 5;
    /** The channel leans this far off its axis at most. */
    private static final int WANDER = 2;
    /** Dormant hextech grows this far around the point where the channel broke into the hollow. */
    private static final int STRIKE_RADIUS = 3;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    @Override
    public MapCodec<StruckGeodeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
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
        if (!geode.place(level, chunkGenerator, random, new BlockPos(x - CENTRE, middle - CENTRE, z - CENTRE))) {
            return false;
        }
        BlockPos end = channel(level, random, ground, middle);
        strikePoint(level, random, end);
        scar(level, random, ground);
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
            }
        }
    }

    /** The scar on the surface: branches of fulgurite running out of a glassy pit, forking as they go. */
    private void scar(WorldGenLevel level, RandomSource random, BlockPos ground) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 || dz == 0 || random.nextBoolean()) {
                    melt(level, ground.getX() + dx, ground.getZ() + dz);
                }
            }
        }
        // The pit: the bolt went in here.
        level.setBlock(ground, Blocks.AIR.defaultBlockState(), FLAGS);
        int branches = 5 + random.nextInt(3);
        float turn = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < branches; i++) {
            float angle = turn + i * Mth.TWO_PI / branches + (random.nextFloat() - 0.5F) * 0.6F;
            branch(level, random, ground.getX() + 0.5F, ground.getZ() + 0.5F, angle,
                    scarReach * (0.6F + 0.4F * random.nextFloat()), 0);
        }
    }

    private void branch(WorldGenLevel level, RandomSource random, float x, float z, float angle, float length, int depth) {
        for (int step = 0; step < length; step++) {
            angle += (random.nextFloat() - 0.5F) * 0.9F;
            x += Mth.cos(angle);
            z += Mth.sin(angle);
            melt(level, Mth.floor(x), Mth.floor(z));
            if (depth < 2 && random.nextFloat() < 0.18F) {
                float side = random.nextBoolean() ? 0.8F : -0.8F;
                branch(level, random, x, z, angle + side, (length - step) * 0.55F, depth + 1);
            }
        }
    }

    /** The top block of the column fuses into fulgurite if it is ground; whatever grew on it burns away. */
    private static void melt(WorldGenLevel level, int x, int z) {
        BlockPos top = new BlockPos(x, surface(level, x, z) - 1, z);
        BlockState state = level.getBlockState(top);
        BlockPos ground = top;
        if (state.canBeReplaced() && !state.isAir() && state.getFluidState().isEmpty()) {
            level.setBlock(top, Blocks.AIR.defaultBlockState(), FLAGS);
            ground = top.below();
            state = level.getBlockState(ground);
        }
        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.SNOW_BLOCK) || state.is(BlockTags.TERRACOTTA)) {
            level.setBlock(ground, ModBlocks.FULGURITE.get().defaultBlockState(), FLAGS);
        }
    }

    /** The first free height of a column: the worldgen heightmap while the world generates, the live one when placed by hand. */
    private static int surface(WorldGenLevel level, int x, int z) {
        return level.getHeight(level instanceof ServerLevel ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }
}
