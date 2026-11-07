package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;

/** Server to clients: a jet tank's wearer lit or cut the jets; repeated while they burn (handled in {@code ChemJetsClient}). */
public record JetFlamePayload(int entity, boolean burning) implements CustomPacketPayload {
    public static final Type<JetFlamePayload> TYPE = new Type<>(Hextale.id("jet_flame"));
    public static final StreamCodec<ByteBuf, JetFlamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, JetFlamePayload::entity,
            ByteBufCodecs.BOOL, JetFlamePayload::burning,
            JetFlamePayload::new);

    @Override
    public Type<JetFlamePayload> type() {
        return TYPE;
    }
}
