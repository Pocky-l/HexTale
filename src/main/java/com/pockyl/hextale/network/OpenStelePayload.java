package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import io.netty.buffer.ByteBuf;

import com.pockyl.hextale.Hextale;

/** Server to client: open the glyph picker of the rune stele at {@code pos}. */
public record OpenStelePayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<OpenStelePayload> TYPE = new Type<>(Hextale.id("open_stele"));

    public static final StreamCodec<ByteBuf, OpenStelePayload> STREAM_CODEC = BlockPos.STREAM_CODEC.map(OpenStelePayload::new, OpenStelePayload::pos);

    @Override
    public Type<OpenStelePayload> type() {
        return TYPE;
    }
}
