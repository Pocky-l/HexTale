package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.runework.RiftStaff;

/** Client to server: a left click with the Rift Staff; the server decides whether it casts ({@link RiftStaff#cast}). */
public record StaffCastPayload() implements CustomPacketPayload {
    public static final StaffCastPayload INSTANCE = new StaffCastPayload();
    public static final Type<StaffCastPayload> TYPE = new Type<>(Hextale.id("staff_cast"));
    public static final StreamCodec<ByteBuf, StaffCastPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<StaffCastPayload> type() {
        return TYPE;
    }

    public static void handle(StaffCastPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            RiftStaff.cast(player);
        }
    }
}
