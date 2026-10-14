package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;

/** Server to client: what just happened at the table, for the screen's message line. */
public record ResearchMessagePayload(Component text, Tone tone) implements CustomPacketPayload {
    public static final Type<ResearchMessagePayload> TYPE = new Type<>(Hextale.id("research_message"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchMessagePayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, ResearchMessagePayload::text,
            ByteBufCodecs.VAR_INT.map(i -> Tone.values()[Math.floorMod(i, Tone.values().length)], Tone::ordinal),
            ResearchMessagePayload::tone,
            ResearchMessagePayload::new);

    public enum Tone {
        INFO,
        GOOD,
        BAD,
        WIN
    }

    @Override
    public Type<ResearchMessagePayload> type() {
        return TYPE;
    }
}
