package com.pockyl.hextale.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.pockyl.hextale.Config;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.UnstableHextechItem;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTags;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Where unstable hextech comes from: lightning led by a lightning rod into the amethyst block the rod stands on
 * shatters it into orbs - and runs on as a crackling wave through every amethyst block and cluster that touches it. Natural strikes and Channeling tridents both go through the bolt's
 * {@link GameEvent#LIGHTNING_STRIKE}, which the server posts right after it powers the rod (NeoForge's
 * {@link VanillaGameEvent}), so no mixin is needed.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class LightningEvents {
    private LightningEvents() {
    }

    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (!event.getVanillaEvent().is(GameEvent.LIGHTNING_STRIKE.key()) || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getCause() instanceof LightningBolt)) {
            return;
        }
        Vec3 position = event.getEventPosition();
        BlockPos rod = findRod(level, position);
        if (rod != null) {
            BlockPos base = rod.relative(level.getBlockState(rod).getValue(LightningRodBlock.FACING).getOpposite());
            if (level.getBlockState(base).is(ModTags.AWAKENS_INTO_HEXTECH)) {
                strike(level, base);
            }
        }
    }

    /** Lightning-born orbs are not destroyed by the very strike that made them. */
    @SubscribeEvent
    public static void onStruckByLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ItemEntity item && item.getItem().is(ModItems.UNSTABLE_HEXTECH.get())) {
            event.setCanceled(true);
        }
    }

    /**
     * The rod the bolt hit. Natural strikes stand on top of the rod, while a bolt summoned at the rod's own block
     * (a Channeling trident hitting its side, or a command) stands inside it.
     */
    @Nullable
    private static BlockPos findRod(ServerLevel level, Vec3 position) {
        BlockPos below = BlockPos.containing(position.x, position.y - 1.0E-6, position.z);
        if (level.getBlockState(below).getBlock() instanceof LightningRodBlock) {
            return below;
        }
        BlockPos inside = BlockPos.containing(position);
        return level.getBlockState(inside).getBlock() instanceof LightningRodBlock ? inside : null;
    }

    /**
     * The strike runs on through the amethyst the struck block touches (faces), up to {@link Config#CHAIN_RADIUS} blocks
     * every way and {@link Config#MAX_CHAIN} blocks: a crackling wave that shatters one ring of blocks after another.
     * Budding amethyst carries the wave but is never shattered (it cannot be made again).
     */
    public static void strike(ServerLevel level, BlockPos base) {
        int radius = Config.CHAIN_RADIUS.get();
        int cap = Config.MAX_CHAIN.get();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Map<BlockPos, Integer> ring = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        cameFrom.put(base, base);
        ring.put(base, 0);
        queue.add(base);
        List<BlockPos> shattered = new ArrayList<>();
        while (!queue.isEmpty() && shattered.size() < cap) {
            BlockPos pos = queue.poll();
            if (level.getBlockState(pos).is(ModTags.AWAKENS_INTO_HEXTECH)) {
                shattered.add(pos);
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!cameFrom.containsKey(next) && next.distChessboard(base) <= radius && level.getBlockState(next).is(ModTags.CARRIES_LIGHTNING)) {
                    cameFrom.put(next, pos);
                    ring.put(next, ring.get(pos) + 1);
                    queue.add(next);
                }
            }
        }
        long now = level.getGameTime();
        for (BlockPos pos : shattered) {
            WAVES.add(new Shatter(level.dimension(), pos, cameFrom.get(pos), now + (long) ring.get(pos) * RING_TICKS, ring.get(pos)));
        }
        Vec3 centre = Vec3.atCenterOf(base);
        level.playSound(null, base, ModSounds.ORB_AWAKEN.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 30, 0.4, 0.4, 0.4, 0.4);
        tickWaves(level);
    }

    /** Ticks between two rings of the wave. */
    public static final int RING_TICKS = 3;

    /** One block the wave will shatter: when, and from which block the spark jumps into it. */
    private record Shatter(ResourceKey<Level> dimension, BlockPos pos, BlockPos from, long at, int ring) {
    }

    private static final List<Shatter> WAVES = new ArrayList<>();

    /** True while a wave still has blocks to shatter (game tests wait for it). */
    public static boolean waving() {
        return !WAVES.isEmpty();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!WAVES.isEmpty() && event.getLevel() instanceof ServerLevel level) {
            tickWaves(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WAVES.clear();
    }

    private static void tickWaves(ServerLevel level) {
        long now = level.getGameTime();
        List<Shatter> due = new ArrayList<>();
        for (Iterator<Shatter> it = WAVES.iterator(); it.hasNext(); ) {
            Shatter shatter = it.next();
            if (shatter.dimension() == level.dimension() && shatter.at() <= now) {
                due.add(shatter);
                it.remove();
            }
        }
        for (Shatter shatter : due) {
            if (level.getBlockState(shatter.pos()).is(ModTags.AWAKENS_INTO_HEXTECH)) {
                arc(level, shatter.from(), shatter.pos());
                awaken(level, shatter.pos(), shatter.ring());
            }
        }
    }

    /** A spark jumps from block to block: motes along an arc and a crackle on the way. */
    private static void arc(ServerLevel level, BlockPos from, BlockPos to) {
        if (from.equals(to)) {
            return;
        }
        Vec3 end = Vec3.atCenterOf(to);
        Vec3 offset = Vec3.atCenterOf(from).subtract(end);
        // Flow motes are spawned at their destination with the offset to their start as speed.
        for (int i = 0; i < 6; i++) {
            level.sendParticles(ModParticles.flow(0x7FD8FF), end.x, end.y, end.z, 0, offset.x, offset.y, offset.z, 1.0);
        }
        for (int i = 1; i <= 4; i++) {
            Vec3 at = end.add(offset.scale(i / 5.0));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 2, 0.08, 0.08, 0.08, 0.05);
        }
    }

    /** Shatters one crystal block into orbs of unstable hextech, with sparks and shards; the crack rises ring by ring. */
    public static void awaken(ServerLevel level, BlockPos pos, int ring) {
        BlockState crystal = level.getBlockState(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        RandomSource random = level.getRandom();
        int min = Config.MIN_ORBS.get();
        int max = Math.max(min, Config.MAX_ORBS.get());
        int count = min + random.nextInt(max - min + 1);
        Vec3 centre = Vec3.atCenterOf(pos);
        for (int i = 0; i < count; i++) {
            ItemEntity orb = new ItemEntity(level, centre.x, centre.y, centre.z, new ItemStack(ModItems.UNSTABLE_HEXTECH.get()));
            double angle = random.nextDouble() * Math.PI * 2;
            orb.setDeltaMovement(Math.cos(angle) * 0.14, 0.26 + random.nextDouble() * 0.1, Math.sin(angle) * 0.14);
            orb.setDefaultPickUpDelay();
            orb.addTag(UnstableHextechItem.BORN_TAG);
            level.addFreshEntity(orb);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, crystal), centre.x, centre.y, centre.z, 24, 0.3, 0.3, 0.3, 0.15);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 14, 0.35, 0.35, 0.35, 0.35);
        level.sendParticles(new DustParticleOptions(0x5CC7F2, 1.4F), centre.x, centre.y, centre.z, 10, 0.35, 0.35, 0.35, 0.02);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.2F, Math.min(1.8F, 0.8F + ring * 0.08F));
    }
}
