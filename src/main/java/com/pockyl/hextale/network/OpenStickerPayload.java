package com.pockyl.hextale.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

import io.netty.buffer.ByteBuf;

import com.pockyl.hextale.Hextale;

/** Server to client: open the essence picker of the sticker in this hand. */
public record OpenStickerPayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<OpenStickerPayload> TYPE = new Type<>(Hextale.id("open_sticker"));
    public static final StreamCodec<ByteBuf, OpenStickerPayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(
            main -> new OpenStickerPayload(main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND),
            payload -> payload.hand() == InteractionHand.MAIN_HAND);

    @Override
    public Type<OpenStickerPayload> type() {
        return TYPE;
    }
}
