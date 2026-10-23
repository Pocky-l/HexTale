package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.transport.PylonFlight;

/**
 * Server to the clients near an Experience Condenser: it has just drunk {@code points} of experience from {@code from}
 * (an orb that never appeared, or a funnel pouring in). The clients draw the stream from this one message.
 */
public record CondensePayload(BlockPos condenser, Vec3 from, int points) implements CustomPacketPayload {
    public static final Type<CondensePayload> TYPE = new Type<>(Hextale.id("condense"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CondensePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CondensePayload::condenser,
            PylonFlight.VEC, CondensePayload::from,
            ByteBufCodecs.VAR_INT, CondensePayload::points,
            CondensePayload::new);

    @Override
    public Type<CondensePayload> type() {
        return TYPE;
    }
}
