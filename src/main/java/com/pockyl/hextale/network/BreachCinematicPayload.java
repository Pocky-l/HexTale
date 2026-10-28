package com.pockyl.hextale.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import io.netty.buffer.ByteBuf;

import com.pockyl.hextale.Hextale;

/** Server to a player near a breach that is tearing open: watch it (the entity id of the breach). */
public record BreachCinematicPayload(int breach) implements CustomPacketPayload {
    public static final Type<BreachCinematicPayload> TYPE = new Type<>(Hextale.id("breach_cinematic"));
    public static final StreamCodec<ByteBuf, BreachCinematicPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            BreachCinematicPayload::new, BreachCinematicPayload::breach);

    @Override
    public Type<BreachCinematicPayload> type() {
        return TYPE;
    }
}
