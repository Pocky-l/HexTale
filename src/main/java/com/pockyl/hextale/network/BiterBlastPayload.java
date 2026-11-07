package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;

/** Server to client: a Biter Grenade burst here. One word; {@code client.render.BiterBlastFx} stages all of it from that. */
public record BiterBlastPayload(Vec3 at) implements CustomPacketPayload {
    public static final Type<BiterBlastPayload> TYPE = new Type<>(Hextale.id("biter_blast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BiterBlastPayload> STREAM_CODEC =
            StreamCodec.composite(com.pockyl.hextale.port.PortCodecs.VEC3, BiterBlastPayload::at, BiterBlastPayload::new);
    /** Blocks within which a blast is shown. */
    public static final double SEEN = 96;

    @Override
    public Type<BiterBlastPayload> type() {
        return TYPE;
    }
}
