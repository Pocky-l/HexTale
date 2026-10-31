package com.pockyl.hextale.thevoid.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModSounds;

/**
 * Husk crust: a thin floor of chitin grown over a hollow, lighter and plainer than the shell round it (the tell). Stand
 * on it a moment and it cracks with a dry ticking; once cracked it cracks again and gives way into the hollow under it,
 * whoever is on it by then. Who keeps moving gets across. The crust round a collapse cracks along with it; the hole grows shut again, slowly.
 */
public class HuskCrustBlock extends Block {
    /** 0 whole, 1 cracked, 2 breaking, 3 fallen in. */
    public static final IntegerProperty CRACK = IntegerProperty.create("crack", 0, 3);
    public static final int FALLEN = 3;
    /** Ticks between one crack and the next while someone stands on it. */
    public static final int CRACK_TICKS = 14;
    /** Chance per random tick that a fallen-in place grows over again (when nothing is in it). */
    public static final float REGROWS = 0.04F;

    public HuskCrustBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CRACK, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRACK);
    }

    public static boolean fallen(BlockState state) {
        return state.getBlock() instanceof HuskCrustBlock && state.getValue(CRACK) == FALLEN;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return fallen(state) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return fallen(state) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return fallen(state);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        weigh(level, pos, state, entity);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance);
        weigh(level, pos, state, entity);
        // A hard landing goes straight through.
        if (fallDistance > 4 && level instanceof ServerLevel server && weighs(entity) && !fallen(state)) {
            server.scheduleTick(pos, this, 1);
        }
    }

    private void weigh(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && !fallen(state) && weighs(entity) && !server.getBlockTicks().hasScheduledTick(pos, this)) {
            server.scheduleTick(pos, this, CRACK_TICKS);
        }
    }

    /** The world's creatures are heavy enough; the Void's own run over it. */
    private static boolean weighs(Entity entity) {
        return entity instanceof LivingEntity living && !living.isSpectator() && !(living instanceof com.pockyl.hextale.thevoid.Voidling)
                && !(living instanceof com.pockyl.hextale.thevoid.VoidMite) && !(living instanceof RibCrawler);
    }

    private static boolean stoodOn(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1.3,
                pos.getZ() + 1), HuskCrustBlock::weighs).isEmpty();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int crack = state.getValue(CRACK);
        if (crack == FALLEN) {
            return;
        }
        // Once it has cracked it goes on breaking, whether the weight stays or not.
        boolean pushed = crack > 0 || stoodOn(level, pos);
        if (!pushed) {
            return;
        }
        if (crack < 2) {
            level.setBlock(pos, state.setValue(CRACK, crack + 1), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.HUSK_CRUST_CRACK.get(), SoundSource.BLOCKS, 0.8F, 0.9F + crack * 0.15F + random.nextFloat() * 0.1F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5,
                    4 + crack * 4, 0.3, 0.02, 0.3, 0.02);
            level.scheduleTick(pos, this, CRACK_TICKS);
            return;
        }
        collapse(state, level, pos, random);
    }

    private void collapse(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, state.setValue(CRACK, FALLEN), Block.UPDATE_ALL);
        com.pockyl.hextale.quest.Quests.fireNear(level, net.minecraft.world.phys.Vec3.atCenterOf(pos.above()), 1.6,
                com.pockyl.hextale.quest.Quests.VOID_FLOOR_GAVE);
        level.playSound(null, pos, ModSounds.HUSK_CRUST_COLLAPSE.get(), SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.2F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                24, 0.35, 0.15, 0.35, 0.05);
        // The crust round it cracks along: a whole patch goes, not a single block.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos next = pos.offset(dx, 0, dz);
                BlockState near = level.getBlockState(next);
                if ((dx != 0 || dz != 0) && near.is(this) && !fallen(near) && !level.getBlockTicks().hasScheduledTick(next, this)) {
                    level.setBlock(next, near.setValue(CRACK, 2), Block.UPDATE_ALL);
                    level.scheduleTick(next, this, 3 + random.nextInt(6));
                }
            }
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!fallen(state) || random.nextFloat() >= REGROWS || !level.getEntitiesOfClass(Entity.class, new AABB(pos)).isEmpty()) {
            return;
        }
        level.setBlock(pos, state.setValue(CRACK, 0), Block.UPDATE_ALL);
    }
}
