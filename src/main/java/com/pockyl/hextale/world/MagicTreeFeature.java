package com.pockyl.hextale.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.TreeFeature;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModBlocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The magic tree, grown from a Magic Tree Sprout: a slender dark trunk that may kink once on its way up, bark buttresses
 * and roots biting into the ground at its foot, three or four long branches reaching out and up from under the top, and
 * a flat, wide pad of leaves on each of them and on the top: a tiered umbrella of a crown. Short strands of leaves hang
 * from the pads' rims. Every leaf lies within
 * {@value #MAX_LEAF_DISTANCE} steps of the wood, so nothing of it decays.
 */
public record MagicTreeFeature() implements FeatureConfiguration {
    public static final MapCodec<MagicTreeFeature> CODEC = MapCodec.unit(MagicTreeFeature::new);
    /** The configured feature {@code data/hextale/worldgen/configured_feature/magic_tree.json} the sprout grows. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> KEY = ResourceKey.create(Registries.CONFIGURED_FEATURE, Hextale.id("magic_tree"));
    public static final TreeGrower GROWER = new TreeGrower("hextale_magic", Optional.empty(), Optional.of(KEY), Optional.empty());
    private static final int MAX_LEAF_DISTANCE = LeavesBlock.DECAY_DISTANCE - 1;
    /** Like vanilla trees: no neighbour shape updates while the tree is set down, the leaves get their distances here. */
    private static final int FLAGS = 19;

    public MapCodec<MagicTreeFeature> codec() {
        return CODEC;
    }

    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        Plan plan = plan(random, origin);
        int top = origin.getY() + plan.height + 4;
        if (top >= level.getMaxY()) {
            return false;
        }
        for (BlockPos pos : plan.trunk) {
            if (!pos.equals(origin) && !TreeFeature.validTreePos(level, pos)) {
                return false;
            }
        }
        Set<BlockPos> wood = new LinkedHashSet<>();
        for (Map.Entry<BlockPos, BlockState> log : plan.logs.entrySet()) {
            BlockPos pos = log.getKey();
            boolean root = pos.getY() < origin.getY();
            if (pos.equals(origin) || TreeFeature.validTreePos(level, pos) || root && level.getBlockState(pos).is(BlockTags.DIRT)) {
                level.setBlock(pos, log.getValue(), FLAGS);
                wood.add(pos);
            }
        }
        Set<BlockPos> leaves = new LinkedHashSet<>();
        for (BlockPos pos : plan.leaves) {
            if (!wood.contains(pos) && TreeFeature.validTreePos(level, pos)) {
                leaves.add(pos);
            }
        }
        BlockState leaf = ModBlocks.MAGIC_LEAVES.get().defaultBlockState();
        distances(wood, leaves).forEach((pos, distance) ->
                level.setBlock(pos, leaf.setValue(LeavesBlock.DISTANCE, distance).setValue(LeavesBlock.PERSISTENT, false), FLAGS));
        return true;
    }

    /** Each leaf's distance to the nearest wood through other leaves; leaves too far away are left out. */
    private static Map<BlockPos, Integer> distances(Set<BlockPos> wood, Set<BlockPos> leaves) {
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos pos : wood) {
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (leaves.contains(next) && !distance.containsKey(next)) {
                    distance.put(next, 1);
                    queue.add(next);
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int d = distance.get(pos);
            if (d >= MAX_LEAF_DISTANCE) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (leaves.contains(next) && !distance.containsKey(next)) {
                    distance.put(next, d + 1);
                    queue.add(next);
                }
            }
        }
        return distance;
    }

    /** The whole tree worked out before a block is set: trunk, wood (with roots and branches) and leaves. */
    static Plan plan(RandomSource random, BlockPos origin) {
        Plan plan = new Plan();
        plan.height = 8 + random.nextInt(3);
        BlockState log = ModBlocks.MAGIC_LOG.get().defaultBlockState();
        BlockState bark = ModBlocks.MAGIC_WOOD.get().defaultBlockState();

        // Trunk: straight, or kinked once - a knot of bark carries it one block aside.
        Direction lean = random.nextBoolean() ? Direction.Plane.HORIZONTAL.getRandomDirection(random) : null;
        int bend = 2 + random.nextInt(2);
        BlockPos column = origin;
        for (int y = 0; y <= plan.height; y++) {
            if (lean != null && y == bend + 1) {
                BlockPos knot = column.relative(lean);
                plan.wood(knot, bark);
                plan.trunk.add(knot);
                column = knot;
            }
            BlockPos pos = column.atY(origin.getY() + y);
            column = pos;
            plan.wood(pos, log);
            plan.trunk.add(pos);
        }
        BlockPos crown = column;

        // Buttresses of bark around the foot, some with a root diving into the ground.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (random.nextFloat() < 0.7F) {
                BlockPos foot = origin.relative(direction);
                plan.wood(foot, bark);
                if (random.nextFloat() < 0.3F) {
                    plan.wood(foot.above(), bark);
                }
                if (random.nextFloat() < 0.5F) {
                    plan.wood(foot.relative(direction).below(), bark);
                }
            }
        }

        // Branches reach out and up from the upper trunk, each ending in a crown.
        int branches = 3 + random.nextInt(2);
        float turn = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < branches; i++) {
            float angle = turn + i * Mth.TWO_PI / branches + (random.nextFloat() - 0.5F) * 0.8F;
            BlockPos start = crown.atY(crown.getY() - 2 - random.nextInt(3));
            BlockPos end = branch(plan, start, angle, 3 + random.nextInt(2));
            canopy(plan, random, end.above(), 2.2F + random.nextFloat() * 0.6F, 1.3F);
        }
        canopy(plan, random, crown.above(), 3.0F, 1.6F);
        weep(plan, random);
        return plan;
    }

    /** Lays a branch from the trunk outwards, rising two blocks in three; diagonal steps go through a corner block. */
    private static BlockPos branch(Plan plan, BlockPos start, float angle, int length) {
        BlockPos previous = start;
        float fx = 0;
        float fz = 0;
        for (int step = 1; step <= length; step++) {
            fx += Mth.cos(angle);
            fz += Mth.sin(angle);
            int rise = (2 * step + 1) / 3;
            BlockPos next = start.offset(Math.round(fx), rise, Math.round(fz));
            previous = connect(plan, previous, next);
        }
        return previous;
    }

    /** Joins two branch blocks by face-touching logs: along x, then z, then up. */
    private static BlockPos connect(Plan plan, BlockPos from, BlockPos to) {
        BlockPos pos = from;
        while (!pos.equals(to)) {
            Direction.Axis axis;
            if (pos.getX() != to.getX()) {
                pos = pos.offset(Integer.signum(to.getX() - pos.getX()), 0, 0);
                axis = Direction.Axis.X;
            } else if (pos.getZ() != to.getZ()) {
                pos = pos.offset(0, 0, Integer.signum(to.getZ() - pos.getZ()));
                axis = Direction.Axis.Z;
            } else {
                pos = pos.offset(0, Integer.signum(to.getY() - pos.getY()), 0);
                axis = Direction.Axis.Y;
            }
            if (!plan.logs.containsKey(pos)) {
                plan.wood(pos, ModBlocks.MAGIC_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
            }
        }
        return pos;
    }

    /** A flat pad of leaves with a ragged rim and a low cap, a little fuller above its centre than below. */
    private static void canopy(Plan plan, RandomSource random, BlockPos centre, float radius, float halfHeight) {
        int r = Mth.ceil(radius);
        int h = Mth.ceil(halfHeight);
        for (int dy = -h; dy <= h; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    float vertical = dy < 0 ? dy * 1.4F : dy;
                    float d = (dx * dx + dz * dz) / (radius * radius) + vertical * vertical / (halfHeight * halfHeight);
                    if (d <= 0.8F || d <= 1.15F && random.nextFloat() < 0.45F) {
                        BlockPos pos = centre.offset(dx, dy, dz);
                        plan.leaves.add(pos);
                        plan.rims.put(pos, dx * dx + dz * dz >= (radius - 1.3F) * (radius - 1.3F));
                    }
                }
            }
        }
    }

    /** Strands of leaves hang from the underside of the crowns' rims. */
    private static void weep(Plan plan, RandomSource random) {
        List<BlockPos> undersides = new ArrayList<>();
        for (BlockPos pos : plan.leaves) {
            if (plan.rims.getOrDefault(pos, false) && !plan.leaves.contains(pos.below()) && !plan.logs.containsKey(pos.below())) {
                undersides.add(pos);
            }
        }
        for (BlockPos pos : undersides) {
            if (random.nextFloat() < 0.35F) {
                int length = 1 + random.nextInt(2);
                for (int i = 1; i <= length; i++) {
                    plan.leaves.add(pos.below(i));
                }
            }
        }
    }

    static final class Plan {
        int height;
        /** The trunk's blocks: they must all be free for the tree to grow. */
        final List<BlockPos> trunk = new ArrayList<>();
        final Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
        final Set<BlockPos> leaves = new LinkedHashSet<>();
        /** Whether a crown's leaf lies on its rim (strands hang only from there). */
        final Map<BlockPos, Boolean> rims = new HashMap<>();

        void wood(BlockPos pos, BlockState state) {
            logs.put(pos, state);
        }
    }

    /** The 26.1.2 feature type: places the configured record (26.3 records are features themselves). */
    public static final class Type extends Feature<MagicTreeFeature> {
        public Type() {
            super(CODEC.codec());
        }

        @Override
        public boolean place(FeaturePlaceContext<MagicTreeFeature> context) {
            return context.config().place(context.level(), context.chunkGenerator(), context.random(), context.origin());
        }
    }
}
