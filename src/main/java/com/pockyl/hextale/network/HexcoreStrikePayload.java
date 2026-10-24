package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;

/** Server to the clients near a Hexcore: an arc left its matrix and struck {@code target}; the clients draw the bolt. */
public record HexcoreStrikePayload(BlockPos core, Vec3 target, int kind) implements CustomPacketPayload {
    /** A harmless arc (into the ground, a stele, a pedestal). */
    public static final int CALM = 0;
    /** A strike that did harm. */
    public static final int HARM = 1;
    /** Caught by a lightning rod. */
    public static final int GROUNDED = 2;

    public static final Type<HexcoreStrikePayload> TYPE = new Type<>(Hextale.id("hexcore_strike"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HexcoreStrikePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HexcoreStrikePayload::core,
            com.pockyl.hextale.port.PortCodecs.VEC3, HexcoreStrikePayload::target,
            ByteBufCodecs.VAR_INT, HexcoreStrikePayload::kind,
            HexcoreStrikePayload::new);

    @Override
    public Type<HexcoreStrikePayload> type() {
        return TYPE;
    }
}
