package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.LapidaryBlockEntity;
import com.pockyl.hextale.menu.LapidaryMenu;

/** Client to server: one stroke on the lapidary - the crystal held at {@code angle} on the disc for {@code ticks}. */
public record LapidaryStrokePayload(BlockPos pos, float angle, int ticks) implements CustomPacketPayload {
    public static final Type<LapidaryStrokePayload> TYPE = new Type<>(Hextale.id("lapidary_stroke"));

    public static final StreamCodec<ByteBuf, LapidaryStrokePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LapidaryStrokePayload::pos,
            ByteBufCodecs.FLOAT, LapidaryStrokePayload::angle,
            ByteBufCodecs.VAR_INT, LapidaryStrokePayload::ticks,
            LapidaryStrokePayload::new);

    @Override
    public Type<LapidaryStrokePayload> type() {
        return TYPE;
    }

    public static void handle(LapidaryStrokePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof LapidaryMenu menu
                && menu.pos().equals(payload.pos) && menu.stillValid(player)
                && player.level().getBlockEntity(payload.pos) instanceof LapidaryBlockEntity lapidary && Float.isFinite(payload.angle)) {
            lapidary.stroke(player, payload.angle, payload.ticks);
        }
    }
}
