package com.pockyl.hextale.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import io.netty.buffer.ByteBuf;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.HexLiftBlock;

/** Client to server: the player standing on a Hex Lift jumped (up) or sneaked (down). */
public record HexLiftPayload(boolean up) implements CustomPacketPayload {
    public static final Type<HexLiftPayload> TYPE = new Type<>(Hextale.id("hex_lift"));
    public static final StreamCodec<ByteBuf, HexLiftPayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(HexLiftPayload::new, HexLiftPayload::up);

    @Override
    public Type<HexLiftPayload> type() {
        return TYPE;
    }

    public static void handle(HexLiftPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            HexLiftBlock.ride(player, payload.up());
        }
    }
}
