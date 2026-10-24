package com.pockyl.hextale.transport;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.HexPylonBlockEntity;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.PylonLinkPayload;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Linking pylons, like tying a thread: right click a pylon, then right click where it should send - a container's face
 * (the face matters: a furnace's top takes what it smelts, its side the fuel) or a relay pylon. Doing it again on the
 * same target unties the link; clicking the pylon itself again, clicking anything that cannot take items, clicking the
 * air or waiting {@value #TIMEOUT_TICKS} ticks gives up. A block that breaks drops its links by itself; sneak + right
 * click with an empty hand on a linked block unties it from every pylon. While a player is linking, a thread of light runs from the pylon to their
 * crosshair (the client is told which pylon).
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class PylonLinking {
    public static final int TIMEOUT_TICKS = 600;
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** Client: the pylon this player is linking from, if any (set by {@link PylonLinkPayload}). */
    @Nullable
    public static BlockPos clientFrom;

    private record Pending(ResourceKey<Level> dimension, BlockPos pos, long since) {
    }

    private PylonLinking() {
    }

    /** A click on a pylon: start a link from it, or give up the one going from it. */
    public static void start(ServerPlayer player, HexPylonBlockEntity pylon) {
        Pending going = PENDING.get(player.getUUID());
        if (going != null && going.pos().equals(pylon.getBlockPos()) && going.dimension() == player.level().dimension()) {
            stop(player);
            player.displayClientMessage(Component.translatable("block.hextale.hex_pylon.link.cancel"), true);
            return;
        }
        PENDING.put(player.getUUID(), new Pending(player.level().dimension(), pylon.getBlockPos(), player.level().getGameTime()));
        ModNetwork.sendToPlayer(player, new PylonLinkPayload(Optional.of(pylon.getBlockPos())));
        player.displayClientMessage(Component.translatable("block.hextale.hex_pylon.link.start", pylon.range(), pylon.links().size(),
                pylon.maxLinks()).withStyle(ChatFormatting.AQUA), true);
        player.level().playSound(null, pylon.getBlockPos(), ModSounds.PYLON_LINK.get(), SoundSource.BLOCKS, 0.4F, 1.3F);
    }

    public static void stop(ServerPlayer player) {
        if (PENDING.remove(player.getUUID()) != null) {
            ModNetwork.sendToPlayer(player, new PylonLinkPayload(Optional.empty()));
        }
    }

    /** A pylon is gone: nobody links from it any more. */
    public static void forget(ServerLevel level, BlockPos pos) {
        Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pending> entry = it.next();
            if (entry.getValue().pos().equals(pos) && entry.getValue().dimension() == level.dimension()) {
                it.remove();
                if (level.getServer().getPlayerList().getPlayer(entry.getKey()) instanceof ServerPlayer player) {
                    ModNetwork.sendToPlayer(player, new PylonLinkPayload(Optional.empty()));
                }
            }
        }
    }

    /** Whether the player is linking now (tests and the event). */
    public static boolean linking(Player player) {
        return PENDING.containsKey(player.getUUID());
    }

    /**
     * Right clicks on blocks. While a link is going: a click on a place items can go (a container's face, a relay, another
     * pylon) ties it there before the block's own use - a chest does not open; a click anywhere else gives the link up
     * and does what it always does (blocks get placed, doors open). Otherwise: sneak + right click with an empty hand on
     * a block pylons send to unties it from all of them.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        boolean going = level.isClientSide() ? clientFrom != null : PENDING.containsKey(player.getUUID());
        if (going) {
            if (level.getBlockEntity(pos) instanceof HexPylonBlockEntity || linkable(level, pos, face)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                if (player instanceof ServerPlayer server) {
                    link(server, pos, face);
                }
            } else {
                giveUp(player);
            }
            return;
        }
        if (player.isSecondaryUseActive() && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()
                && !(level.getBlockEntity(pos) instanceof HexPylonBlockEntity)
                && (level.isClientSide() ? clientLinked.test(level, pos) : PylonIndex.isTarget(level, pos))) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (player instanceof ServerPlayer server) {
                untie(server, pos);
            }
        }
    }

    /** Using an item in the air gives a going link up. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            giveUp(event.getEntity());
        }
    }

    /** A click elsewhere: the going link is given up (the client forgets it at once, the server tells the player). */
    public static void giveUp(Player player) {
        if (player.level().isClientSide()) {
            clientFrom = null;
        } else if (player instanceof ServerPlayer server && PENDING.containsKey(server.getUUID())) {
            stop(server);
            server.displayClientMessage(Component.translatable("block.hextale.hex_pylon.link.cancel"), true);
        }
    }

    /** Whether a pylon could link to the block through {@code face}: a container, an essence receiver or a relay pylon. */
    public static boolean linkable(Level level, BlockPos pos, Direction face) {
        if (level.getBlockEntity(pos) instanceof HexPylonBlockEntity pylon) {
            return pylon.kind() == PylonKind.RELAY;
        }
        return level.getBlockEntity(pos) instanceof EssenceNetwork.Receiver || com.pockyl.hextale.port.ResourceHandler.ofBlock(level, pos, face) != null;
    }

    /** Whether this pylon may send to the block: a relay, or what it carries goes in there (a relay carries items and essence). */
    public static boolean takes(Level level, HexPylonBlockEntity from, BlockPos pos, Direction face) {
        if (level.getBlockEntity(pos) instanceof HexPylonBlockEntity pylon) {
            return pylon.kind() == PylonKind.RELAY;
        }
        boolean essence = level.getBlockEntity(pos) instanceof EssenceNetwork.Receiver;
        boolean items = com.pockyl.hextale.port.ResourceHandler.ofBlock(level, pos, face) != null;
        // A relay carries both: it may link to a chest as well as to a jar.
        return switch (from.kind()) {
            case SIPHON -> essence;
            case RELAY -> essence || items;
            default -> items;
        };
    }

    /** Client: whether some pylon nearby links to the block (set by the client, which can look at the pylons it knows). */
    public static java.util.function.BiPredicate<Level, BlockPos> clientLinked = (level, pos) -> false;

    /** Unties the block from every pylon that sends to it. */
    public static void untie(ServerPlayer player, BlockPos target) {
        ServerLevel level = player.serverLevel();
        int count = 0;
        for (BlockPos pos : PylonIndex.linkingTo(level, target)) {
            if (level.getBlockEntity(pos) instanceof HexPylonBlockEntity pylon && pylon.unlink(target)) {
                count++;
            }
        }
        if (count > 0) {
            player.displayClientMessage(Component.translatable("block.hextale.hex_pylon.link.untied", level.getBlockState(target).getBlock().getName(),
                    count), true);
            level.playSound(null, target, ModSounds.PYLON_UNLINK.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
        }
    }

    /** A block changed: if pylons link to it and it no longer takes items (broken, replaced), their links go away. */
    @SubscribeEvent
    public static void onBlockChanged(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !PylonIndex.isTarget(level, event.getPos())) {
            return;
        }
        for (BlockPos pos : PylonIndex.linkingTo(level, event.getPos())) {
            if (level.getBlockEntity(pos) instanceof HexPylonBlockEntity pylon) {
                for (PylonLink link : java.util.List.copyOf(pylon.links())) {
                    if (link.pos().equals(event.getPos())) {
                        pylon.dropIfBroken(link);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        PylonIndex.clear();
        com.pockyl.hextale.farm.ReachIndex.clearAll();
        PENDING.clear();
    }

    /** Ties (or unties) the player's pending link to the block at {@code target}, entering through {@code face}. */
    public static void link(ServerPlayer player, BlockPos target, Direction face) {
        Pending pending = PENDING.get(player.getUUID());
        ServerLevel level = player.serverLevel();
        if (pending == null) {
            return;
        }
        if (pending.dimension() != level.dimension()
                || !(level.getBlockEntity(pending.pos()) instanceof HexPylonBlockEntity pylon)) {
            stop(player);
            return;
        }
        if (target.equals(pending.pos())) {
            stop(player);
            player.displayClientMessage(Component.translatable("block.hextale.hex_pylon.link.cancel"), true);
            return;
        }
        PylonLink link;
        if (level.getBlockEntity(target) instanceof HexPylonBlockEntity other && other.kind() == PylonKind.SORTING
                && !pylon.kind().essence() && com.pockyl.hextale.port.ResourceHandler.ofBlock(level, target.below(), Direction.UP) != null) {
            // A sorting matrix: what is sent to it goes into the container it reads, to be sorted from there.
            target = target.below();
            face = Direction.UP;
        }
        if (level.getBlockEntity(target) instanceof HexPylonBlockEntity other) {
            if (other.kind() != PylonKind.RELAY) {
                // Another gathering or sending pylon: link from that one instead.
                start(player, other);
                return;
            }
            link = new PylonLink(target, Direction.UP);
        } else {
            if (!takes(level, pylon, target, face)) {
                stop(player);
                player.displayClientMessage(Component.translatable(pylon.kind().essence() ? "block.hextale.hex_pylon.link.no_essence"
                        : pylon.kind() == PylonKind.RELAY ? "block.hextale.hex_pylon.link.no_load" : "block.hextale.hex_pylon.link.nothing")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            link = new PylonLink(target, face);
        }
        Component name = level.getBlockState(target).getBlock().getName();
        if (pylon.linked(link)) {
            pylon.toggleLink(link);
            stop(player);
            player.displayClientMessage(Component.translatable("block.hextale.hex_pylon.link.removed", name), true);
            level.playSound(null, target, ModSounds.PYLON_UNLINK.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        if (!pylon.reaches(target)) {
            int distance = (int) Math.ceil(pylon.gem().distanceTo(Vec3.atCenterOf(target)));
            player.displayClientMessage(Component.translatable("block.hextale.hex_pylon.link.far", distance, pylon.range()).withStyle(ChatFormatting.RED), true);
            stop(player);
            return;
        }
        if (!pylon.toggleLink(link)) {
            player.displayClientMessage(Component.translatable("block.hextale.hex_pylon.link.full", pylon.maxLinks())
                    .withStyle(ChatFormatting.RED), true);
            stop(player);
            return;
        }
        stop(player);
        player.displayClientMessage(Component.translatable("block.hextale.hex_pylon.link.added", name, pylon.links().size(),
                pylon.maxLinks()).withStyle(ChatFormatting.GOLD), true);
        level.playSound(null, target, ModSounds.PYLON_LINK.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
        Vec3 at = link.entry();
        level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 12, 0.15, 0.15, 0.15, 0.05);
    }

    /** Pending links time out; a player who left forgets theirs. */
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            Pending pending = PENDING.get(player.getUUID());
            if (pending != null && player.level().getGameTime() - pending.since() > TIMEOUT_TICKS) {
                stop(player);
                player.displayClientMessage(Component.translatable("block.hextale.hex_pylon.link.cancel"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    /** Client tick of a pylon: landed flights make their sound and a few sparks, then fade away. */
    public static void clientTick(Level level, HexPylonBlockEntity pylon) {
        if (pylon.shown.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<PylonFlight.Shown> it = pylon.shown.iterator();
        while (it.hasNext()) {
            PylonFlight.Shown shown = it.next();
            long age = now - shown.start;
            if (shown.flight.isEssence()) {
                essenceMotes(level, shown, age);
            }
            if (!shown.landed && age >= shown.duration) {
                shown.landed = true;
                Vec3 end = shown.flight.path().getLast();
                level.playLocalSound(end.x, end.y, end.z, shown.flight.isEssence() ? ModSounds.TANK_GLUG.get() : ModSounds.PYLON_ARRIVE.get(),
                        SoundSource.BLOCKS, 0.35F,
                        0.9F + level.getRandom().nextFloat() * 0.2F, false);
                for (int i = 0; i < 4; i++) {
                    level.addParticle(ModParticles.spark(ModParticles.HEXTECH), end.x, end.y, end.z, (level.getRandom().nextDouble() - 0.5) * 0.12,
                            level.getRandom().nextDouble() * 0.08, (level.getRandom().nextDouble() - 0.5) * 0.12);
                }
            }
            if (age > shown.duration + FADE_TICKS) {
                it.remove();
            }
        }
    }

    /**
     * Essence on its way: while a leg lets its motes go, two glowing motes a tick leave the leg's start (a little
     * scattered) and arc into its end - the altar's flow particles, in the essence's colour.
     */
    private static void essenceMotes(Level level, PylonFlight.Shown shown, long age) {
        PylonFlight.Phase phase = shown.flight.phaseAt(shown.phases, age);
        if (phase == null || age - phase.start() >= PylonFlight.MOTE_SPAWN_TICKS) {
            return;
        }
        Vec3 from = shown.flight.path().get(phase.leg());
        Vec3 to = shown.flight.path().get(phase.leg() + 1);
        var random = level.getRandom();
        for (int i = 0; i < 2; i++) {
            Vec3 start = from.add((random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.25);
            Vec3 offset = start.subtract(to);
            level.addParticle(ModParticles.flow(shown.flight.color()), to.x, to.y, to.z, offset.x, offset.y, offset.z);
        }
    }

    /** Ticks a landed flight keeps glowing at its end. */
    public static final int FADE_TICKS = 8;
}
