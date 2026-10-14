package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.knowledge.RuneKnowledge;

/** Server to client: the player's whole knowledge, sent on login, respawn, dimension change and after every change. */
public record KnowledgeSyncPayload(RuneKnowledge knowledge) implements CustomPacketPayload {
    public static final Type<KnowledgeSyncPayload> TYPE = new Type<>(Hextale.id("knowledge"));

    public static final StreamCodec<ByteBuf, KnowledgeSyncPayload> STREAM_CODEC =
            RuneKnowledge.STREAM_CODEC.map(KnowledgeSyncPayload::new, KnowledgeSyncPayload::knowledge);

    @Override
    public Type<KnowledgeSyncPayload> type() {
        return TYPE;
    }
}
