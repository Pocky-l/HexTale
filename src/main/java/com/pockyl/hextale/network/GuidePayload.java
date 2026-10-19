package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.quest.QuestDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

/** Server to client: every step of the researcher's guide as loaded from the data packs, on login and after a reload. */
public record GuidePayload(Map<String, QuestDefinition> steps) implements CustomPacketPayload {
    public static final Type<GuidePayload> TYPE = new Type<>(Hextale.id("guide"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuidePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(LinkedHashMap<String, QuestDefinition>::new, ByteBufCodecs.STRING_UTF8, QuestDefinition.STREAM_CODEC),
            payload -> new LinkedHashMap<>(payload.steps()),
            GuidePayload::new);

    @Override
    public Type<GuidePayload> type() {
        return TYPE;
    }
}
