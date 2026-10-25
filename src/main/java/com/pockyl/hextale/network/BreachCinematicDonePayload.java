package com.pockyl.hextale.network;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import io.netty.buffer.ByteBuf;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.Breaches;

/** Client to server: the player is done watching a breach open (it ended, was skipped or is turned off). */
public record BreachCinematicDonePayload() implements CustomPacketPayload {
    public static final BreachCinematicDonePayload INSTANCE = new BreachCinematicDonePayload();
    public static final Type<BreachCinematicDonePayload> TYPE = new Type<>(Hextale.id("breach_cinematic_done"));
    public static final StreamCodec<ByteBuf, BreachCinematicDonePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<BreachCinematicDonePayload> type() {
        return TYPE;
    }

    public static void handle(BreachCinematicDonePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Breaches.stopWatching(player);
        }
    }
}
