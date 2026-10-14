package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.menu.ResearchTableMenu;

/**
 * Client to server: the orb did not recognise a drawing. Costs nothing; it only counts the attempt and makes the
 * diary write down drawing tips the first time, and the orb on the table flickers for everyone.
 */
public record RecognitionFailedPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<RecognitionFailedPayload> TYPE = new Type<>(Hextale.id("recognition_failed"));

    public static final StreamCodec<ByteBuf, RecognitionFailedPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(RecognitionFailedPayload::new, RecognitionFailedPayload::pos);

    @Override
    public Type<RecognitionFailedPayload> type() {
        return TYPE;
    }

    public static void handle(RecognitionFailedPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ResearchTableBlockEntity table = ResearchTableMenu.openTable(player, payload.pos());
            if (table != null) {
                Research.recordFailure(player);
                table.fizzle(player);
            }
        }
    }
}
