package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;

/** Server to client: open the diary of this researcher (the server has signed it first if needed). */
public record OpenDiaryPayload(String owner) implements CustomPacketPayload {
    public static final Type<OpenDiaryPayload> TYPE = new Type<>(Hextale.id("open_diary"));

    public static final StreamCodec<ByteBuf, OpenDiaryPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(OpenDiaryPayload::new, OpenDiaryPayload::owner);

    @Override
    public Type<OpenDiaryPayload> type() {
        return TYPE;
    }
}
