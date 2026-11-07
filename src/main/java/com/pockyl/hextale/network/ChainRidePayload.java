package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import io.netty.buffer.ByteBuf;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ChainAnchorBlockEntity;
import com.pockyl.hextale.chem.Zipline;
import com.pockyl.hextale.entity.ZiplineTrolley;

/** Client to server: the player clicked the chain shot from {@code owner} to {@code other} at {@code t} to hang on it. */
public record ChainRidePayload(BlockPos owner, BlockPos other, float t) implements CustomPacketPayload {
    public static final Type<ChainRidePayload> TYPE = new Type<>(Hextale.id("chain_ride"));
    public static final StreamCodec<ByteBuf, ChainRidePayload> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC,
            ChainRidePayload::owner, BlockPos.STREAM_CODEC, ChainRidePayload::other, ByteBufCodecs.FLOAT, ChainRidePayload::t,
            ChainRidePayload::new);
    /** How far from the player's eyes the clicked point may be. */
    public static final double REACH = 4.5;

    @Override
    public Type<ChainRidePayload> type() {
        return TYPE;
    }

    public static void handle(ChainRidePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level) || !Float.isFinite(payload.t())) {
            return;
        }
        Zipline.Line line = ChainAnchorBlockEntity.line(level, payload.owner(), payload.other());
        if (line == null) {
            return;
        }
        Vec3 at = line.point(Math.clamp(payload.t(), 0, 1));
        if (at.distanceTo(player.getEyePosition()) <= REACH + 1) {
            ZiplineTrolley.mount(level, player, payload.owner(), payload.other(), payload.t());
        }
    }
}
