package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

/**
 * Server to the clients near a rune sign: a rune just worked at {@code at} ({@link #PROC}: a flat hexagon opens round
 * the spot and the rune's glyph flares over it), or a player learned it ({@link #LEARN}: the glyph writes itself in the
 * air at {@code at} and flies into the entity {@code follow}). {@code size} is the sign's radius in blocks, {@code
 * strength} (0-1) its brightness.
 */
public record RuneFxPayload(int kind, Vec3 at, ComplexRune rune, float size, float strength, int follow) implements CustomPacketPayload {
    public static final int PROC = 0;
    public static final int LEARN = 1;
    /** Players this far from a sign see it. */
    public static final double RADIUS = 48;

    public static final Type<RuneFxPayload> TYPE = new Type<>(Hextale.id("rune_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuneFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RuneFxPayload::kind,
            Vec3.STREAM_CODEC, RuneFxPayload::at,
            ComplexRunes.STREAM_CODEC.cast(), RuneFxPayload::rune,
            ByteBufCodecs.FLOAT, RuneFxPayload::size,
            ByteBufCodecs.FLOAT, RuneFxPayload::strength,
            ByteBufCodecs.VAR_INT, RuneFxPayload::follow,
            RuneFxPayload::new);

    /** A proc of {@code count} glints spread {@code spread} blocks: bigger procs open a wider, brighter sign. */
    public static RuneFxPayload proc(Vec3 at, ComplexRune rune, int count, double spread) {
        return new RuneFxPayload(PROC, at, rune, (float) (0.35 + spread * 1.1), Math.min(1, 0.4F + count / 14F), -1);
    }

    /** {@code player} learned {@code rune} at {@code stars}: the glyph appears an arm's length before their eyes. */
    public static RuneFxPayload learn(Player player, ComplexRune rune, int stars) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 at = player.getEyePosition().add(look.scale(1.4)).add(0, -0.1, 0);
        return new RuneFxPayload(LEARN, at, rune, 0.4F + 0.1F * stars, 0.7F + 0.1F * stars, player.getId());
    }

    /** Sends the sign to every player near it whose client has the mod. */
    public static void send(ServerLevel level, RuneFxPayload payload) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(payload.at) <= RADIUS * RADIUS) {
                ModNetwork.sendToPlayer(player, payload);
            }
        }
    }

    @Override
    public Type<RuneFxPayload> type() {
        return TYPE;
    }
}
