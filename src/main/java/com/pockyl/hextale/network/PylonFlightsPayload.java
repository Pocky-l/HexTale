package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.transport.PylonFlight;

import java.util.List;

/** Server to the clients near a pylon: items it has just moved, to be shown flying ({@link PylonFlight}). */
public record PylonFlightsPayload(BlockPos pylon, List<PylonFlight> flights) implements CustomPacketPayload {
    public static final Type<PylonFlightsPayload> TYPE = new Type<>(Hextale.id("pylon_flights"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PylonFlightsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PylonFlightsPayload::pylon,
            PylonFlight.STREAM_CODEC.apply(ByteBufCodecs.list(16)), PylonFlightsPayload::flights,
            PylonFlightsPayload::new);

    @Override
    public Type<PylonFlightsPayload> type() {
        return TYPE;
    }
}
