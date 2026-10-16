package com.pockyl.hextale.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "4";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(ResearchActionPayload.TYPE, ResearchActionPayload.STREAM_CODEC, ResearchActionPayload::handle);
        // Client-bound: their handlers are registered by the client (ClientPayloads.register).
        registrar.playToClient(KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC);
        registrar.playToClient(ItemEssencesPayload.TYPE, ItemEssencesPayload.STREAM_CODEC);
        registrar.playToClient(ResearchMessagePayload.TYPE, ResearchMessagePayload.STREAM_CODEC);
        registrar.playToClient(OpenDiaryPayload.TYPE, OpenDiaryPayload.STREAM_CODEC);
        registrar.playToClient(InfusionRecipesPayload.TYPE, InfusionRecipesPayload.STREAM_CODEC);
        registrar.playToClient(OpenStelePayload.TYPE, OpenStelePayload.STREAM_CODEC);
        registrar.playToServer(SteleRunePayload.TYPE, SteleRunePayload.STREAM_CODEC, SteleRunePayload::handle);
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
