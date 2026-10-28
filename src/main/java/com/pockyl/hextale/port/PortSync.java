package com.pockyl.hextale.port;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.network.ModNetwork;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoForge 26.x syncs entity attachments built with {@code .sync(codec)} to the entity's own client and every client
 * tracking it ({@code entity.syncData(type)}); NeoForge 21.1 has no attachment syncing. Attachments registered through
 * {@link #synced} travel in a {@link Payload}: on {@link #syncData}, when a player starts tracking the entity, and to a
 * player's own client after login, respawn and a change of dimension.
 */
public final class PortSync {
    private static final Map<ResourceLocation, StreamCodec<? super RegistryFriendlyByteBuf, ?>> CODECS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, DeferredHolder<AttachmentType<?>, ? extends AttachmentType<?>>> TYPES = new ConcurrentHashMap<>();
    private static boolean listening;

    private PortSync() {
    }

    /** The 26.x {@code AttachmentType.Builder#sync(codec)}: registers the attachment for syncing, returns the holder. */
    public static synchronized <T, H extends DeferredHolder<AttachmentType<?>, AttachmentType<T>>> H synced(H holder,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        CODECS.put(holder.getId(), codec);
        TYPES.put(holder.getId(), holder);
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
                if (event.getEntity() instanceof ServerPlayer tracker) {
                    sendAll(event.getTarget(), tracker);
                }
            });
            NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> self(event.getEntity()));
            NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> self(event.getEntity()));
            NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> self(event.getEntity()));
        }
        return holder;
    }

    /** The 26.x {@code entity.syncData(type)}: sends the entity's value to itself (a player) and every player near it. */
    public static void syncData(Entity entity, java.util.function.Supplier<? extends AttachmentType<?>> type) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        ResourceLocation id = NeoForgeRegistries.ATTACHMENT_TYPES.getKey(type.get());
        if (id == null || !CODECS.containsKey(id)) {
            return;
        }
        Payload payload = new Payload(entity.getId(), id, entity.getData(type.get()));
        double range = entity.getType().clientTrackingRange() * 16.0;
        for (ServerPlayer player : level.players()) {
            if (player == entity || player.distanceToSqr(entity) <= range * range) {
                ModNetwork.sendToPlayer(player, payload);
            }
        }
    }

    private static void self(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer server) {
            sendAll(server, server);
        }
    }

    private static void sendAll(Entity entity, ServerPlayer to) {
        for (Map.Entry<ResourceLocation, DeferredHolder<AttachmentType<?>, ? extends AttachmentType<?>>> entry : TYPES.entrySet()) {
            AttachmentType<?> type = entry.getValue().get();
            if (entity.hasData(type)) {
                ModNetwork.sendToPlayer(to, new Payload(entity.getId(), entry.getKey(), entity.getData(type)));
            }
        }
    }

    /** Client side: puts a synced value on the entity. */
    @SuppressWarnings("unchecked")
    public static void accept(Payload payload, net.minecraft.world.level.Level level) {
        Entity entity = level.getEntity(payload.entity());
        DeferredHolder<AttachmentType<?>, ? extends AttachmentType<?>> holder = TYPES.get(payload.attachment());
        if (entity != null && holder != null) {
            entity.setData((AttachmentType<Object>) holder.get(), payload.value());
        }
    }

    /** One synced attachment value of one entity. */
    public record Payload(int entity, ResourceLocation attachment, Object value) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Hextale.id("attachment_sync"));

        @SuppressWarnings("unchecked")
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.of((buf, payload) -> {
            buf.writeVarInt(payload.entity());
            buf.writeResourceLocation(payload.attachment());
            ((StreamCodec<? super RegistryFriendlyByteBuf, Object>) CODECS.get(payload.attachment())).encode(buf, payload.value());
        }, buf -> {
            int entity = buf.readVarInt();
            ResourceLocation type = buf.readResourceLocation();
            return new Payload(entity, type, CODECS.get(type).decode(buf));
        });

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }
}
