package com.pockyl.hextale.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "17";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(ResearchActionPayload.TYPE, ResearchActionPayload.STREAM_CODEC, ResearchActionPayload::handle);
        // Client-bound: their handlers are registered by the client (ClientPayloads.register).
        // 1.21.1: synced attachments (26.x syncs them itself).
        registrar.playToClient(com.pockyl.hextale.port.PortSync.Payload.TYPE, com.pockyl.hextale.port.PortSync.Payload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(ItemEssencesPayload.TYPE, ItemEssencesPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(ResearchMessagePayload.TYPE, ResearchMessagePayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(OpenDiaryPayload.TYPE, OpenDiaryPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(RecipesPayload.TYPE, RecipesPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(GuidePayload.TYPE, GuidePayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(OpenStelePayload.TYPE, OpenStelePayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(PylonFlightsPayload.TYPE, PylonFlightsPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(WhirlPayload.TYPE, WhirlPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(SawPayload.TYPE, SawPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(FunnelPayload.TYPE, FunnelPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(ArcPayload.TYPE, ArcPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(WarmthPayload.TYPE, WarmthPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(CondensePayload.TYPE, CondensePayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(HexcoreStrikePayload.TYPE, HexcoreStrikePayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(RuneFxPayload.TYPE, RuneFxPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(EssenceFxPayload.TYPE, EssenceFxPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(RuneEffectPayload.TYPE, RuneEffectPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(PylonLinkPayload.TYPE, PylonLinkPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(OpenStickerPayload.TYPE, OpenStickerPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(ThinningPayload.TYPE, ThinningPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToClient(BreachCinematicPayload.TYPE, BreachCinematicPayload.STREAM_CODEC, com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent::handle);
        registrar.playToServer(ChooseStickerPayload.TYPE, ChooseStickerPayload.STREAM_CODEC, ChooseStickerPayload::handle);
        registrar.playToServer(PylonCancelPayload.TYPE, PylonCancelPayload.STREAM_CODEC, PylonCancelPayload::handle);
        registrar.playToServer(GrappleRopePayload.TYPE, GrappleRopePayload.STREAM_CODEC, GrappleRopePayload::handle);
        registrar.playToServer(SteleRunePayload.TYPE, SteleRunePayload.STREAM_CODEC, SteleRunePayload::handle);
        registrar.playToServer(LapidaryStrokePayload.TYPE, LapidaryStrokePayload.STREAM_CODEC, LapidaryStrokePayload::handle);
        registrar.playToServer(LabActionPayload.TYPE, LabActionPayload.STREAM_CODEC, LabActionPayload::handle);
        registrar.playToServer(BreachCinematicDonePayload.TYPE, BreachCinematicDonePayload.STREAM_CODEC, BreachCinematicDonePayload::handle);
        registrar.playToServer(QuestNoticedPayload.TYPE, QuestNoticedPayload.STREAM_CODEC, QuestNoticedPayload::handle);
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
