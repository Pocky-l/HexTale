package com.pockyl.hextale.thevoid.realm;

import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModTags;

/**
 * The Sinew Willow's blocks: its fronds, the sprout it grows from and the ichor pods that ripen under its fronds. The
 * tree's shape lives in {@link VoidTrees}; its wood is plain logs and planks.
 */
public final class SinewWillow {
    /** Spores drift down from the fronds. */
    private static final int SPORE = 0xC9A6E4;

    private SinewWillow() {
    }

    /** Fronds as the Void grows them: they never wither (there is no sun here for them to want). */
    public static BlockState wildFronds() {
        return ModBlocks.SINEW_WILLOW_FRONDS.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** A pod as the Void grows it: most hang half-swollen, some ripe and glowing. */
    public static BlockState wildPod(double roll) {
        return ModBlocks.ICHOR_POD.get().defaultBlockState().setValue(Pod.AGE, roll < 0.45 ? Pod.RIPE : roll < 0.8 ? 1 : 0);
    }

    /** The fronds: lilac, glowing at the tips, shedding spores. */
    public static class Fronds extends LeavesBlock {
        public Fronds(Properties properties) {
            super(0.04F, properties);
        }

        @Override
        public MapCodec<Fronds> codec() {
            return simpleCodec(Fronds::new);
        }

        @Override
        protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
            ParticleUtils.spawnParticleBelow(level, pos, random, ModParticles.glint(SPORE));
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            super.animateTick(state, level, pos, random);
            if (random.nextInt(90) == 0 && level.getBlockState(pos.below()).isAir()) {
                level.addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM, pos.getX() + random.nextDouble(), pos.getY() - 0.05,
                        pos.getZ() + random.nextDouble(), 0, 0, 0);
            }
        }
    }

    /**
     * A pod of ichor hanging from the fronds or a bough: it swells and glows brighter as it ripens; a ripe one is picked
     * by hand. Its seed is the pod itself: hung under fronds again, it grows anew.
     */
    public static class Pod extends Block implements BonemealableBlock {
        public static final int RIPE = 2;
        public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
        /** Light by age: a pod glows brighter as it ripens. */
        public static final int[] LIGHT = {3, 7, 12};
        private static final VoxelShape[] SHAPES = {Block.column(6, 10, 16), Block.column(8, 7, 16), Block.column(10, 4, 16)};

        public Pod(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AGE, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AGE);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPES[state.getValue(AGE)];
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            BlockState above = level.getBlockState(pos.above());
            return above.is(ModBlocks.SINEW_WILLOW_FRONDS.get()) || above.is(ModTags.SINEW_WILLOW_LOGS);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
            return direction == Direction.UP && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return state.getValue(AGE) < RIPE;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(6) == 0) {
                level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (state.getValue(AGE) < RIPE) {
                return InteractionResult.PASS;
            }
            if (level instanceof ServerLevel server) {
                Block.popResourceFromFace(server, pos, Direction.DOWN, new ItemStack(ModItems.ICHOR_POD.get(), 1 + server.getRandom().nextInt(2)));
                server.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.7F + server.getRandom().nextFloat() * 0.2F);
                server.sendParticles(ModParticles.glint(0xFF7AE6), pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.02);
                BlockState picked = state.setValue(AGE, 0);
                server.setBlock(pos, picked, Block.UPDATE_CLIENTS);
                server.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(AGE) == RIPE && random.nextInt(14) == 0) {
                level.addParticle(ModParticles.glint(0xFF9CE8), pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.2,
                        pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0, -0.01, 0);
            }
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return state.getValue(AGE) < RIPE;
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            level.setBlock(pos, state.setValue(AGE, Math.min(RIPE, state.getValue(AGE) + 1)), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * A Sinew Willow sprout: a curl of braided sinew that takes root only in the Void's soil and flesh. Given time (or
     * bone meal) it shoots up into a young willow, now and then an old one.
     */
    public static class Sprout extends VegetationBlock implements BonemealableBlock {
        public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
        private static final VoxelShape SHAPE = Block.column(10, 0, 13);

        public Sprout(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(STAGE);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(ModTags.VOID_GROUND);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(7) == 0) {
                advance(level, pos, state, random);
            }
        }

        private static void advance(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
            if (state.getValue(STAGE) == 0) {
                level.setBlock(pos, state.setValue(STAGE, 1), Block.UPDATE_NONE);
            } else {
                grow(level, pos, random, random.nextInt(5) == 0);
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(6) == 0) {
                level.addParticle(ModParticles.glint(0xE040C8), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.3 + random.nextDouble() * 0.4,
                        pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
            }
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return random.nextFloat() < 0.45F;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            advance(level, pos, state, random);
        }

        @Override
        protected com.mojang.serialization.MapCodec<Sprout> codec() {
            return simpleCodec(Sprout::new);
        }
    }

    /** A grid over the level (or an imagined one) that keeps what the tree would write instead of writing it. */
    private static final class PlanGrid implements VoidTrees.Grid {
        interface World {
            int kind(int x, int y, int z);
        }

        final Map<BlockPos, Byte> planned = new HashMap<>();
        private final Map<BlockPos, Float> depth = new HashMap<>();
        private final World world;

        PlanGrid(World world) {
            this.world = world;
        }

        @Override
        public int get(int x, int y, int z) {
            Byte mine = planned.get(new BlockPos(x, y, z));
            return mine != null ? mine : world.kind(x, y, z);
        }

        @Override
        public void set(int x, int y, int z, byte kind) {
            planned.put(new BlockPos(x, y, z), kind);
        }

        @Override
        public float best(int x, int y, int z) {
            Float known = depth.get(new BlockPos(x, y, z));
            // The Void's soil gives way to a root that runs deep into it; nothing else does.
            return known != null ? known : world.kind(x, y, z) == VoidShapes.SOIL ? -0.3F : -Float.MAX_VALUE;
        }

        @Override
        public void best(int x, int y, int z, float value) {
            depth.put(new BlockPos(x, y, z), value);
        }

        int wood() {
            int count = 0;
            for (byte kind : planned.values()) {
                count += kind == VoidShapes.LOG_X || kind == VoidShapes.LOG_Y || kind == VoidShapes.LOG_Z ? 1 : 0;
            }
            return count;
        }
    }

    /**
     * Grows a willow whose foot stands at {@code pos} (where the sprout is), if its trunk and boughs have room: wood
     * only into air and plants (roots also into the Void's soil), fronds and pods only into air. Returns whether it grew.
     */
    public static boolean grow(ServerLevel level, BlockPos pos, RandomSource random, boolean large) {
        VoidTrees.Tree tree = VoidTrees.plan(new VoidShapes.Random(random.nextLong()), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, large);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        PlanGrid real = new PlanGrid((x, y, z) -> {
            if (!level.isInsideBuildHeight(y) || !level.isLoaded(at.set(x, y, z))) {
                return -1;
            }
            BlockState state = level.getBlockState(at);
            if (state.isAir() || state.is(ModBlocks.SINEW_WILLOW_SPROUT.get()) || state.canBeReplaced() && state.getFluidState().isEmpty()) {
                return VoidShapes.AIR;
            }
            return state.is(ModTags.VOID_GROUND) ? VoidShapes.SOIL : VoidShapes.CHITIN;
        });
        VoidTrees.raster(tree, tree.minX(), tree.minZ(), tree.maxX(), tree.maxZ(), real);
        // The same tree in open air over plain soil: if much of its wood had no room here, it does not grow.
        PlanGrid open = new PlanGrid((x, y, z) -> y < pos.getY() ? VoidShapes.SOIL : VoidShapes.AIR);
        VoidTrees.raster(tree, tree.minX(), tree.minZ(), tree.maxX(), tree.maxZ(), open);
        if (real.wood() < open.wood() * 0.9) {
            return false;
        }
        Map<BlockPos, Byte> planned = real.planned;
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        RandomSource pods = RandomSource.create(random.nextLong());
        for (Map.Entry<BlockPos, Byte> entry : planned.entrySet()) {
            BlockState state = switch (entry.getValue()) {
                case VoidShapes.LOG_X -> ModBlocks.SINEW_WILLOW_WOOD.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                case VoidShapes.LOG_Y -> ModBlocks.SINEW_WILLOW_LOG.get().defaultBlockState();
                case VoidShapes.LOG_Z -> ModBlocks.SINEW_WILLOW_WOOD.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
                case VoidShapes.FRONDS -> wildFronds();
                case VoidShapes.POD -> ModBlocks.ICHOR_POD.get().defaultBlockState().setValue(Pod.AGE, pods.nextInt(2));
                default -> null;
            };
            if (state != null) {
                level.setBlock(entry.getKey(), state, Block.UPDATE_CLIENTS);
            }
        }
        // It shoots up: the sinews twist audibly, spores burst from the new crown.
        level.playSound(null, pos, SoundEvents.WEEPING_VINES_PLACE, SoundSource.BLOCKS, 1.2F, 0.6F);
        level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.8F, 0.5F);
        for (VoidTrees.Crown crown : tree.crowns()) {
            level.sendParticles(ModParticles.glint(SPORE), crown.x(), crown.y(), crown.z(), 14, crown.rx() * 0.5, crown.ry() * 0.5, crown.rz() * 0.5, 0.02);
        }
        return true;
    }
}
