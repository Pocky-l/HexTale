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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Skin worn thin: it looks almost like the rest of the sheet (a shade paler, the veins nearly gone), holds for a
 * moment under a creature's weight and then tears - usually over the abyss. The tear spreads through the worn patch
 * round it; a torn place has nothing in it (no face, no collision) and slowly grows shut again.
 */
public class WornMembraneBlock extends MembraneBlock {
    public static final BooleanProperty TORN = BooleanProperty.create("torn");
    /** Ticks it holds once something stands on it. */
    public static final int HOLDS = 18;
    /** Chance a worn neighbour tears along with a tearing block. */
    public static final float SPREAD = 0.55F;
    /** Chance per random tick that a torn place grows shut (when nothing is in it). */
    public static final float REGROWS = 0.08F;

    public WornMembraneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TORN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TORN);
    }

    public static boolean torn(BlockState state) {
        return state.getBlock() instanceof WornMembraneBlock && state.getValue(TORN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TORN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(TORN) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(TORN);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        weigh(level, pos, state, entity);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance);
        weigh(level, pos, state, entity);
    }

    /** Something stands or lands on it: it starts to give. */
    private void weigh(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!(level instanceof ServerLevel server) || state.getValue(TORN) || !(entity instanceof LivingEntity living)
                || !weighs(living) || server.getBlockTicks().hasScheduledTick(pos, this)) {
            return;
        }
        server.scheduleTick(pos, this, HOLDS);
        // It gives under the weight: a wet stretch, the skin sagging round the feet.
        server.playSound(null, pos, com.pockyl.hextale.registry.ModSounds.WORN_MEMBRANE_STRETCH.get(), SoundSource.BLOCKS, 0.8F, 0.9F + server.getRandom().nextFloat() * 0.2F);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5,
                6, 0.3, 0.02, 0.3, 0.02);
    }

    /** Only the world's creatures weigh on it; the Void's own walk its skin freely. */
    private static boolean weighs(LivingEntity living) {
        return !(living instanceof com.pockyl.hextale.thevoid.Voidling) && !(living instanceof com.pockyl.hextale.thevoid.VoidMite)
                && !living.isSpectator();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(TORN)) {
            return;
        }
        level.setBlock(pos, state.setValue(TORN, true), Block.UPDATE_ALL);
        com.pockyl.hextale.quest.Quests.fireNear(level, net.minecraft.world.phys.Vec3.atCenterOf(pos.above()), 1.6,
                com.pockyl.hextale.quest.Quests.VOID_FLOOR_GAVE);
        level.playSound(null, pos, com.pockyl.hextale.registry.ModSounds.WORN_MEMBRANE_TEAR.get(), SoundSource.BLOCKS, 1.0F, 0.85F + random.nextFloat() * 0.3F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                18, 0.35, 0.1, 0.35, 0.05);
        // The tear runs on through the worn skin round it.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos next = pos.offset(dx, dy, dz);
                    BlockState near = level.getBlockState(next);
                    if (near.is(this) && !near.getValue(TORN) && random.nextFloat() < SPREAD
                            && !level.getBlockTicks().hasScheduledTick(next, this)) {
                        level.scheduleTick(next, this, 2 + random.nextInt(4));
                    }
                }
            }
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(TORN) || random.nextFloat() >= REGROWS
                || !level.getEntitiesOfClass(Entity.class, new net.minecraft.world.phys.AABB(pos)).isEmpty()) {
            return;
        }
        level.setBlock(pos, state.setValue(TORN, false), Block.UPDATE_ALL);
    }
}
