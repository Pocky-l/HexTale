package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;

/**
 * Server to the player being drawn through a breach ({@code into}) or a gate: the middle it is pulled toward and how
 * many ticks the pull lasts before the crossing; {@code cancel} = the way shut, let go.
 */
public record VoidPullPayload(double x, double y, double z, int ticks, boolean into, boolean cancel) implements CustomPacketPayload {
    public static final Type<VoidPullPayload> TYPE = new Type<>(Hextale.id("void_pull"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoidPullPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, VoidPullPayload::x,
            ByteBufCodecs.DOUBLE, VoidPullPayload::y,
            ByteBufCodecs.DOUBLE, VoidPullPayload::z,
            ByteBufCodecs.VAR_INT, VoidPullPayload::ticks,
            ByteBufCodecs.BOOL, VoidPullPayload::into,
            ByteBufCodecs.BOOL, VoidPullPayload::cancel,
            VoidPullPayload::new);

    @Override
    public Type<VoidPullPayload> type() {
        return TYPE;
    }
}
