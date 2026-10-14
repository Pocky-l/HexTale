package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.FusionResult;

/** Server to client: how the fusion requested by {@link FuseRunesPayload} went, for the table screen's animation. */
public record FusionResultPayload(FusionResult result) implements CustomPacketPayload {
    public static final Type<FusionResultPayload> TYPE = new Type<>(Hextale.id("fusion_result"));

    public static final StreamCodec<ByteBuf, FusionResultPayload> STREAM_CODEC =
            FusionResult.STREAM_CODEC.map(FusionResultPayload::new, FusionResultPayload::result);

    @Override
    public Type<FusionResultPayload> type() {
        return TYPE;
    }
}
