package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.research.ItemElements;

import java.util.HashMap;
import java.util.Map;

/** Server to client: the elements of every item, sent on login and after data packs reload. */
public record ItemElementsPayload(Map<ResourceLocation, ItemElements> elements) implements CustomPacketPayload {
    public static final Type<ItemElementsPayload> TYPE = new Type<>(Hextale.id("item_elements"));

    public static final StreamCodec<ByteBuf, ItemElementsPayload> STREAM_CODEC =
            ByteBufCodecs.map(HashMap<ResourceLocation, ItemElements>::new, ResourceLocation.STREAM_CODEC, ItemElements.STREAM_CODEC)
                    .map(ItemElementsPayload::new, payload -> new HashMap<>(payload.elements()));

    @Override
    public Type<ItemElementsPayload> type() {
        return TYPE;
    }
}
