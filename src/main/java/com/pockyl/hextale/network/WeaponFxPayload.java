package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;

import java.util.List;

/**
 * Server to client: a weapon rune's moment, drawn by {@code client.render.WeaponFx} - the heaving ground of a Shockwave
 * ({@code blocks} with the {@code steps} they rise on, from {@code from} along to {@code to}), a Blink's shadow path, a
 * Detonator's burst, a Whirlpool, a Whirlwind's circle.
 */
public record WeaponFxPayload(int kind, Vec3 from, Vec3 to, List<BlockPos> blocks, List<Integer> steps, int color, float size)
        implements CustomPacketPayload {
    public static final int MAX_BLOCKS = 96;
    private static final double RADIUS = 64;

    public enum Kind {
        WAVE,
        BLINK,
        BURST,
        VORTEX,
        SWEEP
    }

    public static final Type<WeaponFxPayload> TYPE = new Type<>(Hextale.id("weapon_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WeaponFxPayload::kind,
            Vec3.STREAM_CODEC, WeaponFxPayload::from,
            Vec3.STREAM_CODEC, WeaponFxPayload::to,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_BLOCKS)), WeaponFxPayload::blocks,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_BLOCKS)), WeaponFxPayload::steps,
            ByteBufCodecs.INT, WeaponFxPayload::color,
            ByteBufCodecs.FLOAT, WeaponFxPayload::size,
            WeaponFxPayload::new);

    public Kind shape() {
        return Kind.values()[Math.clamp(kind, 0, Kind.values().length - 1)];
    }

    @Override
    public Type<WeaponFxPayload> type() {
        return TYPE;
    }

    /** Sends the moment to the players near {@code from}. */
    public static void send(ServerLevel level, Kind kind, Vec3 from, Vec3 to, List<BlockPos> blocks, List<Integer> steps, int color, float size) {
        int n = Math.min(MAX_BLOCKS, Math.min(blocks.size(), steps.size()));
        WeaponFxPayload payload = new WeaponFxPayload(kind.ordinal(), from, to, List.copyOf(blocks.subList(0, n)),
                List.copyOf(steps.subList(0, n)), color, size);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(from) <= RADIUS * RADIUS) {
                ModNetwork.sendToPlayer(player, payload);
            }
        }
    }

    public static void send(ServerLevel level, Kind kind, Vec3 from, Vec3 to, int color, float size) {
        send(level, kind, from, to, List.of(), List.of(), color, size);
    }
}
