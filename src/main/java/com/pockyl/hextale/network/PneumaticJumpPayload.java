package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.PneumaticBoots;

/** Client to server: the wearer of chem-steel boots jumped in the air. The server checks everything again. */
public record PneumaticJumpPayload() implements CustomPacketPayload {
    public static final PneumaticJumpPayload INSTANCE = new PneumaticJumpPayload();
    public static final Type<PneumaticJumpPayload> TYPE = new Type<>(Hextale.id("pneumatic_jump"));
    public static final StreamCodec<ByteBuf, PneumaticJumpPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<PneumaticJumpPayload> type() {
        return TYPE;
    }

    public static void handle(PneumaticJumpPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PneumaticBoots.jump(player);
        }
    }
}
