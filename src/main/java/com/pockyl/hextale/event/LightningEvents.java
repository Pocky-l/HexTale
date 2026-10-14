package com.pockyl.hextale.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
import org.joml.Vector3f;

import com.pockyl.hextale.Config;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.knowledge.DiaryNote;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTags;

import org.jetbrains.annotations.Nullable;

/**
 * Where unstable hextech comes from: lightning led by a lightning rod into the amethyst block the rod stands on
 * shatters the block into orbs. Natural strikes and Channeling tridents both go through the bolt's
 * {@link GameEvent#LIGHTNING_STRIKE}, which the server posts right after it powers the rod (NeoForge's
 * {@link VanillaGameEvent}), so no mixin is needed.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class LightningEvents {
    /** Players this close to a notable strike get a diary note about it. */
    private static final double WITNESS_RANGE = 48;
    private static final int CRYSTAL_SEARCH_RADIUS = 3;

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
                awaken(level, base);
                witnesses(level, base, DiaryNote.LIGHTNING_NEAR_AMETHYST);
                return;
            }
            witnesses(level, rod, DiaryNote.ROD_WITHOUT_AMETHYST);
        }
        BlockPos strike = BlockPos.containing(position);
        if (crystalsNear(level, strike)) {
            witnesses(level, strike, DiaryNote.LIGHTNING_NEAR_AMETHYST);
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

    /** Shatters the crystal block into orbs of unstable hextech, with a burst of sparks and shards. */
    public static void awaken(ServerLevel level, BlockPos pos) {
        BlockState crystal = level.getBlockState(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        RandomSource random = level.random;
        int min = Config.MIN_ORBS.get();
        int max = Math.max(min, Config.MAX_ORBS.get());
        int count = min + random.nextInt(max - min + 1);
        Vec3 centre = Vec3.atCenterOf(pos);
        for (int i = 0; i < count; i++) {
            ItemEntity orb = new ItemEntity(level, centre.x, centre.y, centre.z, new ItemStack(ModItems.UNSTABLE_HEXTECH.get()));
            double angle = random.nextDouble() * Math.PI * 2;
            orb.setDeltaMovement(Math.cos(angle) * 0.18, 0.32 + random.nextDouble() * 0.12, Math.sin(angle) * 0.18);
            orb.setDefaultPickUpDelay();
            level.addFreshEntity(orb);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, crystal), centre.x, centre.y, centre.z, 40, 0.3, 0.3, 0.3, 0.15);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 30, 0.4, 0.4, 0.4, 0.4);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.36F, 0.78F, 0.95F), 1.4F), centre.x, centre.y, centre.z,
                24, 0.35, 0.35, 0.35, 0.02);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.6F, 0.8F);
        level.playSound(null, pos, ModSounds.ORB_AWAKEN.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
    }

    private static boolean crystalsNear(ServerLevel level, BlockPos centre) {
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-CRYSTAL_SEARCH_RADIUS, -CRYSTAL_SEARCH_RADIUS, -CRYSTAL_SEARCH_RADIUS),
                centre.offset(CRYSTAL_SEARCH_RADIUS, CRYSTAL_SEARCH_RADIUS, CRYSTAL_SEARCH_RADIUS))) {
            if (level.getBlockState(pos).is(ModTags.RESONANT_CRYSTALS)) {
                return true;
            }
        }
        return false;
    }

    private static void witnesses(ServerLevel level, BlockPos pos, DiaryNote note) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(Vec3.atCenterOf(pos)) <= WITNESS_RANGE * WITNESS_RANGE) {
                Research.unlockNote(player, note);
            }
        }
    }
}
