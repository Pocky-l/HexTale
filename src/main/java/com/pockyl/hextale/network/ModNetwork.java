package com.pockyl.hextale.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.pockyl.hextale.client.ClientPayloads;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "3";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(ResearchActionPayload.TYPE, ResearchActionPayload.STREAM_CODEC, ResearchActionPayload::handle);
        // Client-bound handlers live in client code; the lambdas only resolve it when a packet arrives on a client.
        registrar.playToClient(KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloads.knowledge(payload));
        registrar.playToClient(ItemElementsPayload.TYPE, ItemElementsPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloads.itemElements(payload));
        registrar.playToClient(ResearchMessagePayload.TYPE, ResearchMessagePayload.STREAM_CODEC,
                (payload, context) -> ClientPayloads.researchMessage(payload));
        registrar.playToClient(OpenDiaryPayload.TYPE, OpenDiaryPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloads.openDiary(payload));
    }

    /** Sends to the player if their client has this mod (fake and game-test players do not). */
    public static boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection == null || !player.connection.hasChannel(payload)) {
            return false;
        }
        PacketDistributor.sendToPlayer(player, payload);
        return true;
    }
}
