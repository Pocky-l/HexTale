package com.pockyl.hextale.port;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoForge 26.x registers the handlers of client-bound payloads in a client event; 1.21.1 wants a handler when the
 * payload is registered. The common registration hands every client-bound payload to {@link #handle}, which runs the
 * handler the client registered here at start-up (a dedicated server never receives these payloads).
 *
 * <p>port-of: net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent
 */
public final class RegisterClientPayloadHandlersEvent {
    private static final Map<CustomPacketPayload.Type<?>, IPayloadHandler<?>> HANDLERS = new ConcurrentHashMap<>();

    public <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        HANDLERS.put(type, handler);
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void handle(T payload, IPayloadContext context) {
        IPayloadHandler<T> handler = (IPayloadHandler<T>) HANDLERS.get(payload.type());
        if (handler != null) {
            handler.handle(payload, context);
        }
    }
}
