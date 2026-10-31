package com.pockyl.hextale.thevoid.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModTags;
import com.pockyl.hextale.thevoid.VoidFloraBlock;
import com.pockyl.hextale.thevoid.VoidMite;
import com.pockyl.hextale.thevoid.Voidling;

/** What grows in the Void and what its skin and flesh do to those who walk among them. */
public final class VoidGrowth {
    private VoidGrowth() {
    }

    /** Only the world's creatures are caught or carried by the Void's growth; its own pass freely. */
    static boolean ofTheWorld(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !(entity instanceof Voidling) && !(entity instanceof VoidMite)
                && !entity.isSpectator();
    }

    /** Cilia, two blocks tall. */
    public static class TallCilia extends DoublePlantBlock {
        public TallCilia(Properties properties) {
            super(properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(ModTags.VOID_GROUND);
        }
    }

    /** A strand of tendrils hanging from the underside of a plate, its tip glowing; one climbs down it into a gap. */
    public static class Tendrils extends GrowingPlantHeadBlock {
        private static final VoxelShape SHAPE = Block.column(8.0, 9.0, 16.0);

        public Tendrils(Properties properties) {
            super(properties, Direction.DOWN, SHAPE, false, 0.1);
        }

        @Override
        protected int getBlocksToGrowWhenBonemealed(RandomSource random) {
            return 1 + random.nextInt(2);
        }

        @Override
        protected Block getBodyBlock() {
            return ModBlocks.VOID_TENDRILS_PLANT.get();
        }

        @Override
        protected boolean canGrowInto(BlockState state) {
            return state.isAir();
        }
    }

    /** The body of a strand of tendrils. */
    public static class TendrilsPlant extends GrowingPlantBodyBlock {
        private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

        public TendrilsPlant(Properties properties) {
            super(properties, Direction.DOWN, SHAPE, false);
        }

        @Override
        protected GrowingPlantHeadBlock getHeadBlock() {
            return ModBlocks.VOID_TENDRILS.get();
        }
    }

    /**
     * A puffball of spores: brushed through, it bursts and the spores bear whoever walked into it up a little - they fall
     * slowly for a few seconds (a chance to live through a step over the edge). It swells again after a while.
     */
    public static class SporePuff extends VoidFloraBlock {
        public static final BooleanProperty RIPE = BooleanProperty.create("ripe");
        public static final int SPORE_TICKS = 100;
        public static final int REGROWS = 600;

        public SporePuff(Properties properties) {
            super(true, properties);
            registerDefaultState(stateDefinition.any().setValue(RIPE, true));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(RIPE);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
            if (!(level instanceof ServerLevel server) || !state.getValue(RIPE) || !ofTheWorld(entity)) {
                return;
            }
            ((LivingEntity) entity).addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SPORE_TICKS, 0, false, true, true));
            server.setBlock(pos, state.setValue(RIPE, false), Block.UPDATE_ALL);
            server.scheduleTick(pos, this, REGROWS);
            server.playSound(null, pos, SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.BLOCKS, 0.5F, 0.6F);
            server.sendParticles(ModParticles.glint(0xE6B8F0), pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 24, 0.4, 0.3, 0.4, 0.04);
            server.sendParticles(ModParticles.vapour(0xB48CD8), pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.02);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!state.getValue(RIPE)) {
                level.setBlock(pos, state.setValue(RIPE, true), Block.UPDATE_ALL);
            }
        }
    }

    /** Sinews stretched across a gap: one pushes through them, slowly. */
    public static class SinewCurtain extends Block {
        private static final Vec3 SLOWS = new Vec3(0.72, 0.8, 0.72);

        public SinewCurtain(Properties properties) {
            super(properties);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
            if (ofTheWorld(entity)) {
                entity.makeStuckInBlock(state, SLOWS);
            }
        }
    }

    /**
     * A blister of taut skin over flesh: stepped on, it throws one high into the air (sneaking presses it down without a
     * throw), and a fall onto it does no harm.
     */
    public static class SpringBlister extends Block {
        /** The upward speed it throws with (blocks per tick): about eight blocks up. */
        public static final double THROW = 1.25;

        public SpringBlister(Properties properties) {
            super(properties);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            if (!(entity instanceof LivingEntity) || entity.isSteppingCarefully() || entity.getDeltaMovement().y > 0.2) {
                return;
            }
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x * 1.2, THROW, motion.z * 1.2);
            entity.needsSync = true;
            entity.resetFallDistance();
            if (level instanceof ServerLevel server) {
                server.playSound(null, pos, SoundEvents.SLIME_JUMP, SoundSource.BLOCKS, 0.8F, 0.55F);
                server.sendParticles(ModParticles.ring(0xE040C8), pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 1.2, 1, 0, 1);
                server.sendParticles(ModParticles.glint(0xFF7AE6), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.3, 0.05, 0.3, 0.05);
            }
        }

        @Override
        public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
            entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
        }
    }
}
