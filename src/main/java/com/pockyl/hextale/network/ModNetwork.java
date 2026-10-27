package com.pockyl.hextale.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "13";

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
        registrar.playToClient(CraftingRecipesPayload.TYPE, CraftingRecipesPayload.STREAM_CODEC);
        registrar.playToClient(GuidePayload.TYPE, GuidePayload.STREAM_CODEC);
        registrar.playToClient(OpenStelePayload.TYPE, OpenStelePayload.STREAM_CODEC);
        registrar.playToClient(PylonFlightsPayload.TYPE, PylonFlightsPayload.STREAM_CODEC);
        registrar.playToClient(WhirlPayload.TYPE, WhirlPayload.STREAM_CODEC);
        registrar.playToClient(SawPayload.TYPE, SawPayload.STREAM_CODEC);
        registrar.playToClient(FunnelPayload.TYPE, FunnelPayload.STREAM_CODEC);
        registrar.playToClient(ArcPayload.TYPE, ArcPayload.STREAM_CODEC);
        registrar.playToClient(WarmthPayload.TYPE, WarmthPayload.STREAM_CODEC);
        registrar.playToClient(CondensePayload.TYPE, CondensePayload.STREAM_CODEC);
        registrar.playToClient(HexcoreStrikePayload.TYPE, HexcoreStrikePayload.STREAM_CODEC);
        registrar.playToClient(PylonLinkPayload.TYPE, PylonLinkPayload.STREAM_CODEC);
        registrar.playToClient(OpenStickerPayload.TYPE, OpenStickerPayload.STREAM_CODEC);
        registrar.playToClient(ThinningPayload.TYPE, ThinningPayload.STREAM_CODEC);
        registrar.playToClient(BreachCinematicPayload.TYPE, BreachCinematicPayload.STREAM_CODEC);
        registrar.playToServer(ChooseStickerPayload.TYPE, ChooseStickerPayload.STREAM_CODEC, ChooseStickerPayload::handle);
        registrar.playToServer(PylonCancelPayload.TYPE, PylonCancelPayload.STREAM_CODEC, PylonCancelPayload::handle);
        registrar.playToServer(HexLiftPayload.TYPE, HexLiftPayload.STREAM_CODEC, HexLiftPayload::handle);
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
