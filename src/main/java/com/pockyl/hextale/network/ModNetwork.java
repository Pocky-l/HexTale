package com.pockyl.hextale.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.pockyl.hextale.client.ClientPayloads;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(FuseRunesPayload.TYPE, FuseRunesPayload.STREAM_CODEC, FuseRunesPayload::handle);
        registrar.playToServer(DrawingPayload.TYPE, DrawingPayload.STREAM_CODEC, DrawingPayload::handle);
        registrar.playToServer(RecognitionFailedPayload.TYPE, RecognitionFailedPayload.STREAM_CODEC, RecognitionFailedPayload::handle);
        registrar.playToServer(NotesReadPayload.TYPE, NotesReadPayload.STREAM_CODEC, NotesReadPayload::handle);
        // Client-bound handlers live in client code; the lambdas only resolve it when a packet arrives on a client.
        registrar.playToClient(KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloads.knowledge(payload));
        registrar.playToClient(FusionResultPayload.TYPE, FusionResultPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloads.fusionResult(payload));
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
