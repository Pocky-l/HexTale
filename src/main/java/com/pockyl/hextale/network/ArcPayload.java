package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.transport.PylonFlight;

import java.util.List;

/**
 * Server to the clients near a Hex Arc Spire: an arc has just left its crystal and struck the creatures at
 * {@code targets}. The clients draw the bolts from this one message.
 */
public record ArcPayload(BlockPos spire, List<Vec3> targets) implements CustomPacketPayload {
    public static final Type<ArcPayload> TYPE = new Type<>(Hextale.id("arc"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArcPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ArcPayload::spire,
            PylonFlight.VEC.apply(ByteBufCodecs.list(8)), ArcPayload::targets,
            ArcPayload::new);

    @Override
    public Type<ArcPayload> type() {
        return TYPE;
    }
}
