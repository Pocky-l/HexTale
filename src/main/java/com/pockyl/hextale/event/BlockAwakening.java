package com.pockyl.hextale.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.CrucibleBlockEntity;
import com.pockyl.hextale.block.HexFurnaceBlockEntity;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * Hextech dust wakes ordinary blocks: sprinkled on a crafting table it makes a Hex Workbench, on a furnace a Hex Furnace,
 * on a cauldron a Crucible (keeping the cauldron's water).
 * The change takes {@value #TICKS} ticks: the dust swirls into the block, sparks climb its sides, and the new block
 * appears in a flash. Breaking or changing the block meanwhile stops it and gives the dust back.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class BlockAwakening {
    public static final int TICKS = 40;
    private static final int HEXTECH = 0x7FD8FF;

    /** What a block wakes into. */
    public record Awakening(Block from, Supplier<? extends Block> to) {
    }

    private static final List<Awakening> TABLE = List.of(
            new Awakening(Blocks.CRAFTING_TABLE, ModBlocks.HEX_WORKBENCH),
            new Awakening(Blocks.FURNACE, ModBlocks.HEX_FURNACE),
            new Awakening(Blocks.CAULDRON, ModBlocks.CRUCIBLE),
            new Awakening(Blocks.WATER_CAULDRON, ModBlocks.CRUCIBLE));

    private record Pending(ResourceKey<Level> dimension, BlockPos pos, Awakening awakening, Direction facing, @Nullable UUID player,
                           long start, boolean refund) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private BlockAwakening() {
    }

    public static @Nullable Awakening of(BlockState state) {
        for (Awakening awakening : TABLE) {
            if (state.is(awakening.from())) {
                return awakening;
            }
        }
        return null;
    }

    public static List<Awakening> table() {
        return TABLE;
    }

    public static boolean waking(Level level, BlockPos pos) {
        return PENDING.stream().anyMatch(p -> p.dimension() == level.dimension() && p.pos().equals(pos));
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.HEXTECH_DUST.get())) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Awakening awakening = of(level.getBlockState(pos));
        if (awakening == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(level instanceof ServerLevel server) || waking(level, pos)) {
            return;
        }
        Player player = event.getEntity();
        BlockState state = level.getBlockState(pos);
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING)
                : player.getDirection().getOpposite();
        boolean refund = !player.getAbilities().instabuild;
        if (refund) {
            stack.shrink(1);
        }
        start(server, pos, facing, player.getUUID(), refund);
    }

    /** Starts waking the block at {@code pos} (game tests call it directly). */
    public static void start(ServerLevel level, BlockPos pos, Direction facing, @Nullable UUID player, boolean refund) {
        Awakening awakening = of(level.getBlockState(pos));
        if (awakening == null || waking(level, pos)) {
            return;
        }
        PENDING.add(new Pending(level.dimension(), pos.immutable(), awakening, facing, player, level.getGameTime(), refund));
        Vec3 centre = Vec3.atCenterOf(pos);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.BLOCK_AWAKEN.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (PENDING.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending pending = it.next();
            if (pending.dimension() != level.dimension()) {
                continue;
            }
            BlockState state = level.getBlockState(pending.pos());
            if (!state.is(pending.awakening().from())) {
                // The block was broken or changed: the dust falls back out.
                if (pending.refund()) {
                    Containers.dropItemStack(level, pending.pos().getX() + 0.5, pending.pos().getY() + 0.5, pending.pos().getZ() + 0.5,
                            new ItemStack(ModItems.HEXTECH_DUST.get()));
                }
                it.remove();
                continue;
            }
            long age = now - pending.start();
            if (age >= TICKS) {
                finish(level, pending);
                it.remove();
            } else {
                swirl(level, pending.pos(), age / (float) TICKS);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }

    /** The dust spirals in, tighter and faster as the change nears; blue sparks climb the block's sides. */
    private static void swirl(ServerLevel level, BlockPos pos, float progress) {
        Vec3 centre = Vec3.atCenterOf(pos);
        double radius = 1.1 - 0.6 * progress;
        int motes = 2 + Math.round(4 * progress);
        for (int i = 0; i < motes; i++) {
            double angle = level.getGameTime() * (0.25 + 0.35 * progress) + i * Math.PI * 2 / motes;
            double height = (level.getRandom().nextDouble() - 0.3) * 0.9;
            // Flow motes fly from (destination + speed) into the destination they were spawned at.
            level.sendParticles(ModParticles.flow(HEXTECH), centre.x, centre.y, centre.z, 0,
                    Math.cos(angle) * radius, height, Math.sin(angle) * radius, 1.0);
        }
        if (level.getGameTime() % 3 == 0) {
            Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom());
            double along = level.getRandom().nextDouble() - 0.5;
            double x = centre.x + side.getStepX() * 0.52 + (side.getAxis() == Direction.Axis.Z ? along : 0);
            double z = centre.z + side.getStepZ() * 0.52 + (side.getAxis() == Direction.Axis.X ? along : 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + progress, z, 2, 0.02, 0.1, 0.02, 0.02);
        }
    }

    /** The new block appears in a flash; what the old one held is dropped. */
    private static void finish(ServerLevel level, Pending pending) {
        BlockPos pos = pending.pos();
        if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container container) {
            Containers.dropContents(level, pos, container);
        }
        BlockState old = level.getBlockState(pos);
        BlockState next = pending.awakening().to().get().defaultBlockState();
        if (next.hasProperty(HorizontalDirectionalBlock.FACING)) {
            next = next.setValue(HorizontalDirectionalBlock.FACING, pending.facing());
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        if (old.is(Blocks.WATER_CAULDRON) && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            crucible.setWater(old.getValue(LayeredCauldronBlock.LEVEL) * CrucibleBlockEntity.WATER_CAPACITY / LayeredCauldronBlock.MAX_FILL_LEVEL);
        }
        if (pending.player() != null && level.getBlockEntity(pos) instanceof HexFurnaceBlockEntity furnace) {
            Player player = level.getPlayerByUUID(pending.player());
            if (player != null) {
                furnace.setOwner(player);
            }
        }
        // The woken block's step in the guide is done by the one who woke it.
        if (pending.player() != null && level.getPlayerByUUID(pending.player()) instanceof net.minecraft.server.level.ServerPlayer player) {
            net.minecraft.world.item.Item woken = next.getBlock().asItem();
            Guide.all().stream().filter(quest -> quest.icon().is(woken)).findFirst()
                    .ifPresent(quest -> Quests.complete(player, quest));
        }
        Vec3 centre = Vec3.atCenterOf(pos);
        level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFF000000 | HEXTECH), centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, centre.x, centre.y, centre.z, 16, 0.35, 0.35, 0.35, 0.08);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y + 0.5, centre.z, 20, 0.4, 0.2, 0.4, 0.3);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.ORB_AWAKEN.get(), SoundSource.BLOCKS, 0.6F, 1.4F);
    }
}
