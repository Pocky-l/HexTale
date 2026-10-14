package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.knowledge.Research;

/** Client to server: the player has seen the diary notes, so their "new" markers can go. */
public record NotesReadPayload() implements CustomPacketPayload {
    public static final NotesReadPayload INSTANCE = new NotesReadPayload();
    public static final Type<NotesReadPayload> TYPE = new Type<>(Hextale.id("notes_read"));

    public static final StreamCodec<ByteBuf, NotesReadPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<NotesReadPayload> type() {
        return TYPE;
    }

    public static void handle(NotesReadPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Research.markNotesRead(player);
        }
    }
}
