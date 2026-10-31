package com.pockyl.hextale.thevoid.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.VoidMite;
import com.pockyl.hextale.thevoid.Voidling;

/**
 * The Void's sacs, grown on the inner walls of its nurseries: bladders of skin that hold what the Void is growing.
 * A nursery sac ({@link Nursery}) holds a curled voidling and bursts when someone comes near or breaks it; a pearl sac
 * ({@link Pearl}) holds a Void Pearl; a mimic ({@link Mimic}) looks like a pearl sac but twitches when someone is near
 * (the tell) and bursts into mites when touched.
 */
public final class VoidSacs {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty BURST = BooleanProperty.create("burst");
    public static final BooleanProperty TWITCH = BooleanProperty.create("twitch");
    /** Reach within which a nursery sac may hatch, a mimic twitches. */
    public static final double HATCH_REACH = 7;
    public static final double TWITCH_REACH = 6;

    private VoidSacs() {
    }

    /** A sac hanging off the face it grew on (facing = away from it). */
    public abstract static class Sac extends Block {
        private final VoxelShape[] shapes;

        protected Sac(Properties properties, int width, int height) {
            super(properties);
            double w = width / 2.0;
            shapes = new VoxelShape[6];
            for (Direction direction : Direction.values()) {
                shapes[direction.ordinal()] = switch (direction) {
                    case UP -> Block.box(8 - w, 0, 8 - w, 8 + w, height, 8 + w);
                    case DOWN -> Block.box(8 - w, 16 - height, 8 - w, 8 + w, 16, 8 + w);
                    case NORTH -> Block.box(8 - w, 8 - w, 16 - height, 8 + w, 8 + w, 16);
                    case SOUTH -> Block.box(8 - w, 8 - w, 0, 8 + w, 8 + w, height);
                    case WEST -> Block.box(16 - height, 8 - w, 8 - w, 16, 8 + w, 8 + w);
                    case EAST -> Block.box(0, 8 - w, 8 - w, height, 8 + w, 8 + w);
                };
            }
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shapes[state.getValue(FACING).ordinal()];
        }

        @Override
        public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
            return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            Direction facing = state.getValue(FACING);
            BlockPos on = pos.relative(facing.getOpposite());
            return level.getBlockState(on).isFaceSturdy(level, on, facing);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
            return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
        }

        /** It pops: skin flies, and whatever it held comes out. */
        protected static void pop(ServerLevel level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, ModSounds.VOID_SAC_BURST.get(), SoundSource.BLOCKS, 1.0F, 0.85F + level.getRandom().nextFloat() * 0.3F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    20, 0.25, 0.25, 0.25, 0.08);
        }

        protected static @Nullable Player near(ServerLevel level, BlockPos pos, double reach) {
            Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, reach, false);
            return player == null || player.isCreative() || player.isSpectator() ? null : player;
        }

        protected static <T extends Mob> @Nullable T hatch(ServerLevel level, BlockPos pos, Direction facing,
                net.minecraft.world.entity.EntityType<T> type, @Nullable Player target) {
            T mob = type.create(level, EntitySpawnReason.TRIGGERED);
            if (mob == null) {
                return null;
            }
            mob.snapTo(pos.getX() + 0.5 + facing.getStepX() * 0.3, pos.getY() + Math.max(0, facing.getStepY()) * 0.3, pos.getZ() + 0.5
                    + facing.getStepZ() * 0.3, level.getRandom().nextFloat() * 360, 0);
            level.addFreshEntity(mob);
            if (target != null) {
                mob.setTarget(target);
            }
            return mob;
        }
    }

    /** A nursery sac: a voidling curled inside. It hatches when someone comes near, or when it is broken open. */
    public static class Nursery extends Sac {
        public Nursery(Properties properties) {
            super(properties, 12, 13);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(BURST, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, BURST);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return !state.getValue(BURST);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            Player player = near(level, pos, HATCH_REACH);
            if (player != null && random.nextFloat() < 0.35F) {
                burst(state, level, pos, player);
            }
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            if (level instanceof ServerLevel server && !state.getValue(BURST) && !player.isCreative()) {
                hatchOut(server, pos, state, player);
            }
            return super.playerWillDestroy(level, pos, state, player);
        }

        private static void burst(BlockState state, ServerLevel level, BlockPos pos, @Nullable Player player) {
            level.setBlock(pos, state.setValue(BURST, true), Block.UPDATE_ALL);
            hatchOut(level, pos, state, player);
        }

        private static void hatchOut(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player player) {
            pop(level, pos, state);
            Direction facing = state.getValue(FACING);
            if (level.getRandom().nextFloat() < 0.6F) {
                Voidling voidling = hatch(level, pos, facing, ModEntities.VOIDLING.get(), player);
                if (voidling != null) {
                    voidling.emerged();
                }
            } else {
                for (int i = 0; i < 2; i++) {
                    hatch(level, pos, facing, ModEntities.VOID_MITE.get(), player);
                }
            }
        }
    }

    /** A pearl sac: tight skin round a Void Pearl. */
    public static class Pearl extends Sac {
        public Pearl(Properties properties) {
            super(properties, 8, 9);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
    }

    /** A mimic: a pearl sac to look at, mites inside. It twitches when someone is near and bursts when touched. */
    public static class Mimic extends Sac {
        public Mimic(Properties properties) {
            super(properties, 8, 9);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(TWITCH, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, TWITCH);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (near(level, pos, TWITCH_REACH) != null && !level.getBlockTicks().hasScheduledTick(pos, this)) {
                level.scheduleTick(pos, this, 4);
            }
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            Player player = near(level, pos, TWITCH_REACH);
            if (player == null) {
                if (state.getValue(TWITCH)) {
                    level.setBlock(pos, state.setValue(TWITCH, false), Block.UPDATE_CLIENTS);
                }
                return;
            }
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) < 2.2) {
                burst(state, level, pos, player);
                return;
            }
            boolean twitch = !state.getValue(TWITCH) && random.nextFloat() < 0.5F;
            level.setBlock(pos, state.setValue(TWITCH, twitch), Block.UPDATE_CLIENTS);
            if (twitch) {
                level.playSound(null, pos, ModSounds.VOID_SAC_TWITCH.get(), SoundSource.BLOCKS, 0.45F, 0.9F + random.nextFloat() * 0.3F);
            }
            level.scheduleTick(pos, this, twitch ? 3 + random.nextInt(4) : 10 + random.nextInt(25));
        }

        @Override
        protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
            if (level instanceof ServerLevel server && !player.isCreative()) {
                burst(state, server, pos, player);
            }
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            if (level instanceof ServerLevel server && !player.isCreative()) {
                pop(server, pos, state);
                spill(server, pos, state, player);
            }
            return super.playerWillDestroy(level, pos, state, player);
        }

        private void burst(BlockState state, ServerLevel level, BlockPos pos, Player player) {
            level.removeBlock(pos, false);
            pop(level, pos, state);
            spill(level, pos, state, player);
        }

        private static void spill(ServerLevel level, BlockPos pos, BlockState state, Player player) {
            int count = 3 + level.getRandom().nextInt(3);
            for (int i = 0; i < count; i++) {
                VoidMite mite = hatch(level, pos, state.getValue(FACING), ModEntities.VOID_MITE.get(), player);
                if (mite != null) {
                    mite.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.4, 0.25, (level.getRandom().nextDouble() - 0.5) * 0.4);
                }
            }
        }
    }
}
