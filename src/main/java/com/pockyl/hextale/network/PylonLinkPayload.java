package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import io.netty.buffer.ByteBuf;

import com.pockyl.hextale.Hextale;

import java.util.Optional;

/** Server to client: the pylon the player is linking from now (a thread runs from it to the crosshair), or none. */
public record PylonLinkPayload(Optional<BlockPos> from) implements CustomPacketPayload {
    public static final Type<PylonLinkPayload> TYPE = new Type<>(Hextale.id("pylon_link"));
    public static final StreamCodec<ByteBuf, PylonLinkPayload> STREAM_CODEC = ByteBufCodecs.optional(BlockPos.STREAM_CODEC)
            .map(PylonLinkPayload::new, PylonLinkPayload::from);

    @Override
    public Type<PylonLinkPayload> type() {
        return TYPE;
    }
}
