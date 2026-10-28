package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;

import java.util.List;

/**
 * Server to the clients near essence on the move, drawn by the client's {@code EssenceFx}:
 * <ul>
 * <li>{@link #PIPE}: a pulse of light running through brass pipes along {@code path} (the source block, the pipes in
 * order, the receiving block);</li>
 * <li>{@link #POUR}: a living stream (the jar stream's look) from {@code from} into {@code to} for {@code ticks}.</li>
 * </ul>
 */
public record EssenceFxPayload(int kind, List<BlockPos> path, Vec3 from, Vec3 to, int color, int ticks) implements CustomPacketPayload {
    public static final int PIPE = 0;
    public static final int POUR = 1;
    /** Longest pipe run sent: the pulse is shown on the first blocks only beyond it. */
    public static final int MAX_PATH = 64;
    private static final double RADIUS = 48;
    /** Players this near a pipe run see its pulse (pipes are everywhere in a base; only the ones close by show). */
    private static final double PIPE_RADIUS = 20;

    public static final Type<EssenceFxPayload> TYPE = new Type<>(Hextale.id("essence_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EssenceFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EssenceFxPayload::kind,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PATH)), EssenceFxPayload::path,
            Vec3.STREAM_CODEC, EssenceFxPayload::from,
            Vec3.STREAM_CODEC, EssenceFxPayload::to,
            ByteBufCodecs.INT, EssenceFxPayload::color,
            ByteBufCodecs.VAR_INT, EssenceFxPayload::ticks,
            EssenceFxPayload::new);

    /** A pulse through the pipes of {@code path} (source block first, receiving block last). */
    public static void pipe(ServerLevel level, List<BlockPos> path, int color) {
        if (path.size() < 2) {
            return;
        }
        List<BlockPos> sent = path.size() > MAX_PATH ? List.copyOf(path.subList(0, MAX_PATH)) : List.copyOf(path);
        send(level, new EssenceFxPayload(PIPE, sent, Vec3.atCenterOf(sent.getFirst()), Vec3.atCenterOf(sent.getLast()), color, 0));
    }

    /** A stream of essence from {@code from} into {@code to}, flowing for {@code ticks}. */
    public static void pour(ServerLevel level, Vec3 from, Vec3 to, int color, int ticks) {
        send(level, new EssenceFxPayload(POUR, List.of(), from, to, color, ticks));
    }

    /** Where a player's bottle is held: before the chest, a little to the side of the main hand. */
    public static Vec3 hand(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        return player.getEyePosition().add(look.scale(0.45)).add(right.scale(0.3)).add(0, -0.45, 0);
    }

    private static void send(ServerLevel level, EssenceFxPayload payload) {
        Vec3 middle = payload.from.add(payload.to).scale(0.5);
        double reach = payload.kind == PIPE ? PIPE_RADIUS + payload.from.distanceTo(payload.to) / 2 : RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(middle) <= reach * reach) {
                ModNetwork.sendToPlayer(player, payload);
            }
        }
    }

    @Override
    public Type<EssenceFxPayload> type() {
        return TYPE;
    }
}
