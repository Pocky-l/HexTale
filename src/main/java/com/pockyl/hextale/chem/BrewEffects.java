package com.pockyl.hextale.chem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTags;
import com.pockyl.hextale.world.WorldEdits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * What a thrown Chem Vial does where it shatters. Every splash bursts in the brew's colour; then
 * <ul>
 * <li>growth tonic: every plant within {@value #GROWTH_RADIUS} blocks takes bone meal twice, young animals grow up;</li>
 * <li>coolant: fire within {@value #COOLANT_RADIUS} goes out, still lava turns to obsidian and flowing lava to
 * cobblestone, burning creatures are put out;</li>
 * <li>glow mix: patches of cold green Chem Glow on the faces of the blocks around, glowing creatures;</li>
 * <li>acid: the stone within {@value #ACID_RADIUS} blocks hisses away (block tag {@code hextale:acid_dissolves}; ores
 * and everything else stay), iron blocks rust into Rusted Plating, copper weathers a stage, monsters are burned;</li>
 * <li>combustible mix: a harmless flash of green flame.</li>
 * </ul>
 */
public final class BrewEffects {
    public static final int GROWTH_RADIUS = 2;
    public static final int COOLANT_RADIUS = 3;
    public static final int GLOW_RADIUS = 2;
    public static final double ACID_RADIUS = 1.6;
    /** Patches of Chem Glow one splash leaves at most. */
    public static final int GLOW_PATCHES = 14;
    private static final float ACID_BURN = 4;
    private static final int GLOWING_TICKS = 600;

    private BrewEffects() {
    }

    public static void splash(ServerLevel level, Vec3 at, Brew brew, @Nullable Entity thrower) {
        splash(level, at, brew, thrower, WorldEdits.actor(level, thrower, null));
    }

    /** {@link #splash(ServerLevel, Vec3, Brew, Entity)} changing only the blocks {@code actor} (who threw it) may change. */
    public static void splash(ServerLevel level, Vec3 at, Brew brew, @Nullable Entity thrower, Player actor) {
        level.sendParticles(ModParticles.drop(brew.color()), at.x, at.y, at.z, 26, 0.2, 0.15, 0.2, 0.18);
        level.sendParticles(ModParticles.wisp(brew.color()), at.x, at.y, at.z, 10, 0.6, 0.35, 0.6, 0.01);
        level.sendParticles(ModParticles.ring(brew.color()), at.x, at.y + 0.05, at.z, 0, 1.6, 1, 0, 1);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), at.x, at.y, at.z, 10,
                0.15, 0.15, 0.15, 0.15);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 0.8F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        level.playSound(null, at.x, at.y, at.z, ModSounds.VIAL_SPLASH.get(), SoundSource.NEUTRAL, 0.7F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        BlockPos center = BlockPos.containing(at);
        switch (brew) {
            case GROWTH -> growth(level, center, thrower);
            case COOLANT -> coolant(level, center, actor);
            case GLOW -> glow(level, center, actor);
            case ACID -> acid(level, at, thrower, actor);
            case FUEL -> level.sendParticles(ModParticles.flame(ModParticles.SOUL_FIRE), at.x, at.y, at.z, 16, 0.4, 0.2, 0.4, 0.03);
        }
    }

    private static void growth(ServerLevel level, BlockPos center, @Nullable Entity thrower) {
        Player player = thrower instanceof Player p ? p : null;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-GROWTH_RADIUS, -1, -GROWTH_RADIUS), center.offset(GROWTH_RADIUS, 1, GROWTH_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            // A plant, not the ground it grows from: grass and nylium would only sprout weeds all over.
            if (!(state.getBlock() instanceof BonemealableBlock) || state.getBlock() instanceof GrassBlock
                    || state.getBlock() instanceof NyliumBlock || state.is(BlockTags.DIRT) || state.is(Blocks.MOSS_BLOCK)) {
                continue;
            }
            BlockPos at = pos.immutable();
            boolean grew = false;
            for (int i = 0; i < 2; i++) {
                grew |= BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), level, at, player);
            }
            if (grew) {
                level.levelEvent(1505, at, 15);
            }
        }
        for (AgeableMob mob : level.getEntitiesOfClass(AgeableMob.class, new AABB(center).inflate(GROWTH_RADIUS + 1))) {
            if (mob.isBaby()) {
                mob.setAge(0);
                level.sendParticles(ModParticles.glint(0x9BE36F), mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(),
                        8, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    private static void coolant(ServerLevel level, BlockPos center, Player actor) {
        boolean hissed = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-COOLANT_RADIUS, -COOLANT_RADIUS, -COOLANT_RADIUS),
                center.offset(COOLANT_RADIUS, COOLANT_RADIUS, COOLANT_RADIUS))) {
            if (pos.distSqr(center) > COOLANT_RADIUS * COOLANT_RADIUS + 1) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            BlockState into = null;
            if (state.is(BlockTags.FIRE)) {
                into = Blocks.AIR.defaultBlockState();
            } else if (fluid.is(Fluids.LAVA) && state.is(Blocks.LAVA)) {
                into = (fluid.isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE).defaultBlockState();
            } else if (state.is(Blocks.MAGMA_BLOCK)) {
                into = Blocks.NETHERRACK.defaultBlockState();
            }
            BlockPos at = pos.immutable();
            if (into != null && WorldEdits.change(level, at, into, Block.UPDATE_ALL, actor)) {
                level.sendParticles(ModParticles.vapour(ModParticles.SOOT), at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.01);
                hissed = true;
            }
        }
        for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(center).inflate(COOLANT_RADIUS))) {
            if (entity.isOnFire()) {
                entity.clearFire();
                hissed = true;
            }
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity item && quench(level, item, actor)) {
                hissed = true;
            }
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 30, 1.2, 0.6, 1.2, 0.02);
        if (hissed) {
            level.playSound(null, center, ModSounds.CRUCIBLE_EVAPORATE.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
        }
    }

    /**
     * The chemtech road to level 2: hexsteel ingots lying hot on a lit Chem Burner, hit by coolant, set charged at once -
     * no essence, but the fumes thin the land ({@link com.pockyl.hextale.thevoid.Thinning#QUENCHED} an ingot). Only for a
     * thrower who has researched charged hexsteel.
     */
    static boolean quench(ServerLevel level, net.minecraft.world.entity.item.ItemEntity item, @org.jspecify.annotations.Nullable Player actor) {
        ItemStack stack = item.getItem();
        BlockPos under = BlockPos.containing(item.getX(), item.getY() - 0.2, item.getZ());
        if (!stack.is(com.pockyl.hextale.registry.ModItems.HEXSTEEL_INGOT.get()) || !com.pockyl.hextale.block.ChemBurnerBlock.isLit(level.getBlockState(under))
                || actor == null || !com.pockyl.hextale.knowledge.Research.of(actor).researched(com.pockyl.hextale.research.ResearchTopics.CHARGED_HEXSTEEL)) {
            return false;
        }
        item.setItem(stack.transmuteCopy(com.pockyl.hextale.registry.ModItems.CHARGED_HEXSTEEL_INGOT.get(), stack.getCount()));
        com.pockyl.hextale.thevoid.Thinning.add(level, under, stack.getCount() * com.pockyl.hextale.thevoid.Thinning.QUENCHED,
                com.pockyl.hextale.thevoid.Thinning.Source.FUMES);
        level.sendParticles(ModParticles.vapour(ModParticles.SOOT), item.getX(), item.getY() + 0.2, item.getZ(), 10, 0.15, 0.2, 0.15, 0.03);
        level.sendParticles(ModParticles.glint(0x7EF0FF), item.getX(), item.getY() + 0.15, item.getZ(), 8, 0.12, 0.1, 0.12, 0.02);
        // The price shows at the act: a violet plume rises from the burner, more of it for a bigger stack.
        level.sendParticles(ModParticles.vapour(ModParticles.INK), item.getX(), item.getY() + 0.6, item.getZ(),
                Math.min(40, 8 + stack.getCount() / 2), 0.25, 0.6, 0.25, 0.02);
        level.playSound(null, under, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 0.6F);
        return true;
    }

    private static void glow(ServerLevel level, BlockPos center, Player actor) {
        MultifaceBlock glow = ModBlocks.CHEM_GLOW.get();
        List<BlockPos> spots = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-GLOW_RADIUS, -GLOW_RADIUS, -GLOW_RADIUS),
                center.offset(GLOW_RADIUS, GLOW_RADIUS, GLOW_RADIUS))) {
            if (pos.distSqr(center) <= GLOW_RADIUS * GLOW_RADIUS + 1) {
                spots.add(pos.immutable());
            }
        }
        Collections.shuffle(spots, new java.util.Random(level.getRandom().nextLong()));
        int placed = 0;
        for (BlockPos pos : spots) {
            if (placed >= GLOW_PATCHES) {
                break;
            }
            BlockState here = level.getBlockState(pos);
            if (!here.isAir() && !here.is(glow)) {
                continue;
            }
            for (Direction face : Direction.values()) {
                BlockState with = glow.getStateForPlacement(here, level, pos, face);
                if (with != null && with != here) {
                    if (WorldEdits.change(level, pos, with, Block.UPDATE_ALL, actor)) {
                        placed++;
                    }
                    break;
                }
            }
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center).inflate(GLOW_RADIUS + 1))) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOWING_TICKS));
        }
        level.sendParticles(ModParticles.glint(Brew.GLOW.color()), center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 16, 1.0, 0.6, 1.0, 0.02);
    }

    private static void acid(ServerLevel level, Vec3 at, @Nullable Entity thrower, Player actor) {
        BlockPos center = BlockPos.containing(at);
        int reach = (int) Math.ceil(ACID_RADIUS);
        int eaten = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
            if (Vec3.atCenterOf(pos).distanceToSqr(at) > ACID_RADIUS * ACID_RADIUS) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            BlockPos here = pos.immutable();
            BlockState into = null;
            if (state.is(Blocks.IRON_BLOCK)) {
                into = ModBlocks.RUSTED_PLATING.get().defaultBlockState();
            } else if (state.getBlock() instanceof WeatheringCopper copper) {
                into = copper.getNext(state).orElse(null);
            } else if (state.is(ModTags.ACID_DISSOLVES) && level.getBlockEntity(here) == null && state.getDestroySpeed(level, here) >= 0) {
                into = Blocks.AIR.defaultBlockState();
            }
            if (into != null && WorldEdits.change(level, here, into, Block.UPDATE_ALL, actor)) {
                if (into.isAir()) {
                    eaten++;
                }
                level.sendParticles(ModParticles.wisp(Brew.ACID.color()), here.getX() + 0.5, here.getY() + 0.5, here.getZ() + 0.5,
                        4, 0.3, 0.3, 0.3, 0);
                level.sendParticles(ModParticles.vapour(ModParticles.SMOKE), here.getX() + 0.5, here.getY() + 0.5, here.getZ() + 0.5, 2, 0.25, 0.25, 0.25, 0.01);
            }
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(at)).inflate(ACID_RADIUS + 0.5))) {
            if (entity instanceof Enemy) {
                entity.hurtServer(level, level.damageSources().indirectMagic(thrower, thrower), ACID_BURN);
            }
        }
        level.playSound(null, at.x, at.y, at.z, ModSounds.ACID_HISS.get(), SoundSource.BLOCKS, eaten > 0 ? 1.0F : 0.6F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
    }
}
