package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;

/**
 * Server to client: the Rift Staff's moments, drawn by {@code client.render.StaffFx}. A bolt sets off ({@code id} = the
 * bolt, {@code to} = its step per tick, {@code value} = the cast in the run) and lands ({@code from} = where, {@code to}
 * = the way it flew, {@code value} 1 on a creature); a lance runs from {@code from} to {@code to} ({@code value} = the
 * burst's radius at its end, 0 for none); the ward of entity {@code id} takes a blow at {@code from} ({@code value} =
 * the share it still holds), breaks, or sends a blow back to {@code to}.
 */
public record StaffFxPayload(int kind, int id, Vec3 from, Vec3 to, float value) implements CustomPacketPayload {
    private static final double RADIUS = 72;

    public enum Kind {
        BOLT,
        LAND,
        LANCE,
        WARD_HIT,
        WARD_BREAK,
        MIRROR,
        READY
    }

    public static final Type<StaffFxPayload> TYPE = new Type<>(Hextale.id("staff_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StaffFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StaffFxPayload::kind,
            ByteBufCodecs.VAR_INT, StaffFxPayload::id,
            Vec3.STREAM_CODEC, StaffFxPayload::from,
            Vec3.STREAM_CODEC, StaffFxPayload::to,
            ByteBufCodecs.FLOAT, StaffFxPayload::value,
            StaffFxPayload::new);

    public Kind shape() {
        return Kind.values()[Math.clamp(kind, 0, Kind.values().length - 1)];
    }

    @Override
    public Type<StaffFxPayload> type() {
        return TYPE;
    }

    public static void send(ServerLevel level, Kind kind, int id, Vec3 from, Vec3 to, float value) {
        StaffFxPayload payload = new StaffFxPayload(kind.ordinal(), id, from, to, value);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(from) <= RADIUS * RADIUS) {
                ModNetwork.sendToPlayer(player, payload);
            }
        }
    }
}
