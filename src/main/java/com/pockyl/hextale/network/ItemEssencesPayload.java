package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.research.ItemEssences;

import java.util.HashMap;
import java.util.Map;

/** Server to client: the essences of every item, sent on login and after data packs reload. */
public record ItemEssencesPayload(Map<Identifier, ItemEssences> essences) implements CustomPacketPayload {
    public static final Type<ItemEssencesPayload> TYPE = new Type<>(Hextale.id("item_elements"));

    public static final StreamCodec<ByteBuf, ItemEssencesPayload> STREAM_CODEC =
            ByteBufCodecs.map(HashMap<Identifier, ItemEssences>::new, Identifier.STREAM_CODEC, ItemEssences.STREAM_CODEC)
                    .map(ItemEssencesPayload::new, payload -> new HashMap<>(payload.essences()));

    @Override
    public Type<ItemEssencesPayload> type() {
        return TYPE;
    }
}
