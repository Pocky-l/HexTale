package com.pockyl.hextale.network;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.realm.VoidCurrents;

/** Server to one player in the Void: the currents of the regions round them (replacing what it knew). */
public record VoidCurrentsPayload(List<VoidCurrents.Current> currents) implements CustomPacketPayload {
    public static final Type<VoidCurrentsPayload> TYPE = new Type<>(Hextale.id("void_currents"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoidCurrentsPayload> STREAM_CODEC = StreamCodec.composite(
            VoidCurrents.Current.STREAM_CODEC.apply(ByteBufCodecs.list(128)), VoidCurrentsPayload::currents,
            VoidCurrentsPayload::new);

    @Override
    public Type<VoidCurrentsPayload> type() {
        return TYPE;
    }
}
