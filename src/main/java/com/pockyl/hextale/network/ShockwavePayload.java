package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.runework.WeaponRunes;

/**
 * Client to server: the player swung a weapon with the Shockwave rune at the ground. The server checks everything again
 * (the weapon, the rune, its rest, the swing's charge) and sends the wave off where the player looks.
 */
public record ShockwavePayload() implements CustomPacketPayload {
    public static final ShockwavePayload INSTANCE = new ShockwavePayload();
    public static final Type<ShockwavePayload> TYPE = new Type<>(Hextale.id("shockwave"));
    public static final StreamCodec<ByteBuf, ShockwavePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<ShockwavePayload> type() {
        return TYPE;
    }

    public static void handle(ShockwavePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            WeaponRunes.shockwave(player);
        }
    }
}
