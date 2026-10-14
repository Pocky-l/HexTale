package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.FusionResult;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.rune.RuneStrokes;

/**
 * Client to server: both circles hold a recognised radical, fuse them. The server recognises the strokes again and
 * checks the table itself; the client only shows what it was told back in a {@link FusionResultPayload}.
 */
public record FuseRunesPayload(BlockPos pos, RuneStrokes action, RuneStrokes target) implements CustomPacketPayload {
    public static final Type<FuseRunesPayload> TYPE = new Type<>(Hextale.id("fuse_runes"));

    public static final StreamCodec<ByteBuf, FuseRunesPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FuseRunesPayload::pos,
            RuneStrokes.STREAM_CODEC, FuseRunesPayload::action,
            RuneStrokes.STREAM_CODEC, FuseRunesPayload::target,
            FuseRunesPayload::new);

    @Override
    public Type<FuseRunesPayload> type() {
        return TYPE;
    }

    public static void handle(FuseRunesPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ResearchTableBlockEntity table = ResearchTableMenu.openTable(player, payload.pos());
        if (table == null) {
            return;
        }
        FusionResult result = table.fuse(player, payload.action(), payload.target());
        ModNetwork.sendToPlayer(player, new FusionResultPayload(result));
    }
}
