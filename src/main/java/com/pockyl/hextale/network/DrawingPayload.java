package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.menu.ResearchTableMenu;

/** Client to server, at most a few times a second: the player is drawing, so the orb on the table stirs for everyone. */
public record DrawingPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<DrawingPayload> TYPE = new Type<>(Hextale.id("drawing"));

    public static final StreamCodec<ByteBuf, DrawingPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(DrawingPayload::new, DrawingPayload::pos);

    @Override
    public Type<DrawingPayload> type() {
        return TYPE;
    }

    public static void handle(DrawingPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ResearchTableBlockEntity table = ResearchTableMenu.openTable(player, payload.pos());
            if (table != null) {
                table.markDrawing(player);
            }
        }
    }
}
