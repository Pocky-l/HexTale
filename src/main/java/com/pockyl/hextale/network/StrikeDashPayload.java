package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;

/**
 * Server to client: a Strike Gauntlet flight began or ended, for the pose and trail on the clients that watch it. The
 * wearer's own client flies by itself and only listens for {@code wall}: the server found a wall first.
 */
public record StrikeDashPayload(int entity, Vec3 dir, int stage, boolean start, boolean wall) implements CustomPacketPayload {
    private static final double RADIUS = 128;
    public static final Type<StrikeDashPayload> TYPE = new Type<>(Hextale.id("strike_dash"));
    public static final StreamCodec<ByteBuf, StrikeDashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StrikeDashPayload::entity,
            Vec3.STREAM_CODEC, StrikeDashPayload::dir,
            ByteBufCodecs.VAR_INT, StrikeDashPayload::stage,
            ByteBufCodecs.BOOL, StrikeDashPayload::start,
            ByteBufCodecs.BOOL, StrikeDashPayload::wall,
            StrikeDashPayload::new);

    @Override
    public Type<StrikeDashPayload> type() {
        return TYPE;
    }

    public static void send(ServerLevel level, Player flier, Vec3 dir, int stage, boolean start, boolean wall) {
        StrikeDashPayload payload = new StrikeDashPayload(flier.getId(), dir, stage, start, wall);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(flier) <= RADIUS * RADIUS) {
                ModNetwork.sendToPlayer(player, payload);
            }
        }
    }
}
