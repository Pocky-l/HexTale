package com.pockyl.hextale.transport;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.HexPylonBlockEntity;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.PylonLinkPayload;
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
 * same target unties the link; clicking the pylon itself again, or waiting {@value #TIMEOUT_TICKS} ticks, gives up. While a player is linking, a thread of light runs from the pylon to their
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
            player.sendOverlayMessage(Component.translatable("block.hextale.hex_pylon.link.cancel"));
            return;
        }
        PENDING.put(player.getUUID(), new Pending(player.level().dimension(), pylon.getBlockPos(), player.level().getGameTime()));
        ModNetwork.sendToPlayer(player, new PylonLinkPayload(Optional.of(pylon.getBlockPos())));
        player.sendOverlayMessage(Component.translatable("block.hextale.hex_pylon.link.start", pylon.range(), pylon.links().size(),
                HexPylonBlockEntity.MAX_LINKS).withStyle(ChatFormatting.AQUA));
        player.level().playSound(null, pylon.getBlockPos(), ModSounds.PYLON_LINK.get(), SoundSource.BLOCKS, 0.4F, 1.3F);
    }

    private static void stop(ServerPlayer player) {
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
     * The second click: while a link is going, right click on any block ties it there (or unties it), before the block's
     * own use - a chest does not open.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            if (clientFrom != null) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        Pending pending = PENDING.get(server.getUUID());
        if (pending == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        link(server, event.getPos(), event.getFace() == null ? Direction.UP : event.getFace());
    }

    /** Ties (or unties) the player's pending link to the block at {@code target}, entering through {@code face}. */
    public static void link(ServerPlayer player, BlockPos target, Direction face) {
        Pending pending = PENDING.get(player.getUUID());
        ServerLevel level = player.level();
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
            player.sendOverlayMessage(Component.translatable("block.hextale.hex_pylon.link.cancel"));
            return;
        }
        PylonLink link;
        if (level.getBlockEntity(target) instanceof HexPylonBlockEntity other) {
            if (other.kind() != PylonKind.RELAY) {
                // Another gathering or sending pylon: link from that one instead.
                start(player, other);
                return;
            }
            link = new PylonLink(target, Direction.UP);
        } else {
            if (level.getCapability(Capabilities.Item.BLOCK, target, face) == null) {
                player.sendOverlayMessage(Component.translatable("block.hextale.hex_pylon.link.nothing").withStyle(ChatFormatting.RED));
                return;
            }
            link = new PylonLink(target, face);
        }
        Component name = level.getBlockState(target).getBlock().getName();
        if (pylon.linked(link)) {
            pylon.toggleLink(link);
            stop(player);
            player.sendOverlayMessage(Component.translatable("block.hextale.hex_pylon.link.removed", name));
            level.playSound(null, target, ModSounds.PYLON_UNLINK.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        if (!pylon.reaches(target)) {
            int distance = (int) Math.ceil(pylon.gem().distanceTo(Vec3.atCenterOf(target)));
            player.sendOverlayMessage(Component.translatable("block.hextale.hex_pylon.link.far", distance, pylon.range()).withStyle(ChatFormatting.RED));
            return;
        }
        if (!pylon.toggleLink(link)) {
            player.sendOverlayMessage(Component.translatable("block.hextale.hex_pylon.link.full", HexPylonBlockEntity.MAX_LINKS)
                    .withStyle(ChatFormatting.RED));
            return;
        }
        stop(player);
        player.sendOverlayMessage(Component.translatable("block.hextale.hex_pylon.link.added", name, pylon.links().size(),
                HexPylonBlockEntity.MAX_LINKS).withStyle(ChatFormatting.GOLD));
        level.playSound(null, target, ModSounds.PYLON_LINK.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
        Vec3 at = link.entry();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 12, 0.15, 0.15, 0.15, 0.05);
    }

    /** Pending links time out; a player who left forgets theirs. */
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            Pending pending = PENDING.get(player.getUUID());
            if (pending != null && player.level().getGameTime() - pending.since() > TIMEOUT_TICKS) {
                stop(player);
                player.sendOverlayMessage(Component.translatable("block.hextale.hex_pylon.link.cancel"));
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
            if (!shown.landed && age >= shown.duration) {
                shown.landed = true;
                Vec3 end = shown.flight.path().getLast();
                level.playLocalSound(end.x, end.y, end.z, ModSounds.PYLON_ARRIVE.get(), SoundSource.BLOCKS, 0.35F,
                        0.9F + level.getRandom().nextFloat() * 0.2F, false);
                for (int i = 0; i < 4; i++) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, (level.getRandom().nextDouble() - 0.5) * 0.12,
                            level.getRandom().nextDouble() * 0.08, (level.getRandom().nextDouble() - 0.5) * 0.12);
                }
            }
            if (age > shown.duration + FADE_TICKS) {
                it.remove();
            }
        }
    }

    /** Ticks a landed flight keeps glowing at its end. */
    public static final int FADE_TICKS = 8;
}
