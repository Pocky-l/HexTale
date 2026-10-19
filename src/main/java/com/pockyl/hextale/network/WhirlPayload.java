package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.farm.WhirlKind;

/**
 * Server to the clients near a Whirl Reaper: a whirl has just left it for a ripe plant. The clients play its whole
 * timeline from this one message: {@code travel} ticks of flight, then the kind's work over the plant.
 */
public record WhirlPayload(BlockPos reaper, BlockPos target, WhirlKind kind, int travel) implements CustomPacketPayload {
    public static final Type<WhirlPayload> TYPE = new Type<>(Hextale.id("whirl"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WhirlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WhirlPayload::reaper,
            BlockPos.STREAM_CODEC, WhirlPayload::target,
            ByteBufCodecs.VAR_INT.map(WhirlKind::byIndex, WhirlKind::ordinal), WhirlPayload::kind,
            ByteBufCodecs.VAR_INT, WhirlPayload::travel,
            WhirlPayload::new);

    @Override
    public Type<WhirlPayload> type() {
        return TYPE;
    }
}
