package com.pockyl.hextale.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import io.netty.buffer.ByteBuf;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.quest.Quests;

/** Client to server: the reader looked at a step of the guide that was new (the mouse went over it in the book). */
public record QuestNoticedPayload(String step) implements CustomPacketPayload {
    public static final Type<QuestNoticedPayload> TYPE = new Type<>(Hextale.id("quest_noticed"));
    public static final StreamCodec<ByteBuf, QuestNoticedPayload> STREAM_CODEC = ByteBufCodecs.stringUtf8(256)
            .map(QuestNoticedPayload::new, QuestNoticedPayload::step);

    @Override
    public Type<QuestNoticedPayload> type() {
        return TYPE;
    }

    public static void handle(QuestNoticedPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Quests.noticed(player, payload.step());
        }
    }
}
