package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;

/**
 * Server to the clients near a Hex Sawmill: its saw has just left for a tree. The clients play the whole felling from
 * this one message: {@code travel} ticks of flight, the sawing, the fall.
 */
public record SawPayload(BlockPos mill, BlockPos foot, int travel) implements CustomPacketPayload {
    public static final Type<SawPayload> TYPE = new Type<>(Hextale.id("saw"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SawPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SawPayload::mill,
            BlockPos.STREAM_CODEC, SawPayload::foot,
            ByteBufCodecs.VAR_INT, SawPayload::travel,
            SawPayload::new);

    @Override
    public Type<SawPayload> type() {
        return TYPE;
    }
}
