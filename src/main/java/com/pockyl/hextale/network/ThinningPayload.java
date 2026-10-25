package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;

import java.util.List;

/**
 * Server to one player: the Thinning of the chunks around them that changed ({@code reset} = forget all you knew first,
 * after a login or a change of dimension). Keys are packed chunk positions.
 */
public record ThinningPayload(boolean reset, List<Entry> entries) implements CustomPacketPayload {
    public static final Type<ThinningPayload> TYPE = new Type<>(Hextale.id("thinning"));

    public record Entry(long chunk, float value) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Entry::chunk,
                ByteBufCodecs.FLOAT, Entry::value,
                Entry::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ThinningPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ThinningPayload::reset,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(1024)), ThinningPayload::entries,
            ThinningPayload::new);

    @Override
    public Type<ThinningPayload> type() {
        return TYPE;
    }
}
