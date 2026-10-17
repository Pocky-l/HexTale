package com.pockyl.hextale.network;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import io.netty.buffer.ByteBuf;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.transport.PylonLinking;

/** Client to server: the player clicked into the air while linking a pylon - give the link up. */
public record PylonCancelPayload() implements CustomPacketPayload {
    public static final PylonCancelPayload INSTANCE = new PylonCancelPayload();
    public static final Type<PylonCancelPayload> TYPE = new Type<>(Hextale.id("pylon_cancel"));
    public static final StreamCodec<ByteBuf, PylonCancelPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<PylonCancelPayload> type() {
        return TYPE;
    }

    public static void handle(PylonCancelPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PylonLinking.giveUp(player);
        }
    }
}
