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
 * air at {@code at} and flies into the entity {@code follow}), or {@link #AREA}: a flat hexagon on the ground opening out
 * to a device's reach, or {@link #SIGIL}: a small flat hexagon flaring open and fading with no glyph (a device's moment:
 * a crystal soaked in, a flame lit, stone drinking essence). {@code size} is the sign's radius in blocks, {@code strength} (0-1) its brightness, {@code
 * color} its colour ({@link #RUNE_COLOR} = the rune's).
 */
public record RuneFxPayload(int kind, Vec3 at, ComplexRune rune, float size, float strength, int follow, int color)
        implements CustomPacketPayload {
    public static final int PROC = 0;
    public static final int LEARN = 1;
    public static final int AREA = 2;
    /** A ripple on a block's face ({@code follow} = the face's direction). */
    public static final int RIPPLE = 3;
    public static final int SIGIL = 4;
    public static final int RUNE_COLOR = -1;
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
            ByteBufCodecs.INT, RuneFxPayload::color,
            RuneFxPayload::new);

    /** A proc of {@code count} glints spread {@code spread} blocks: bigger procs open a wider, brighter sign. */
    public static RuneFxPayload proc(Vec3 at, ComplexRune rune, int count, double spread) {
        return new RuneFxPayload(PROC, at, rune, (float) (0.35 + spread * 1.1), Math.min(1, 0.4F + count / 14F), -1, RUNE_COLOR);
    }

    /** {@code player} learned {@code rune} at {@code stars}: the glyph appears an arm's length before their eyes. */
    public static RuneFxPayload learn(Player player, ComplexRune rune, int stars) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 at = player.getEyePosition().add(look.scale(1.4)).add(0, -0.1, 0);
        return new RuneFxPayload(LEARN, at, rune, 0.4F + 0.1F * stars, 0.7F + 0.1F * stars, player.getId(), RUNE_COLOR);
    }

    /** A device's reach shown on the ground round {@code at}: a hexagon of {@code radius} blocks opening out. */
    public static RuneFxPayload area(Vec3 at, ComplexRune rune, float radius, int color) {
        return new RuneFxPayload(AREA, at, rune, radius, 1, -1, color);
    }

    /** A flat hexagon of {@code radius} blocks flaring open round {@code at} and fading; no glyph. */
    public static RuneFxPayload sigil(Vec3 at, float radius, int color) {
        return new RuneFxPayload(SIGIL, at, NO_GLYPH, radius, 1, -1, color);
    }

    /** A small hexagon rippling out over a block's {@code face} at {@code at} (a barrier pushed, a label put on). */
    public static RuneFxPayload ripple(Vec3 at, net.minecraft.core.Direction face, float size, int color) {
        return new RuneFxPayload(RIPPLE, at, NO_GLYPH, size, 1, face.get3DDataValue(), color);
    }

    /** The rune a sign without a glyph carries (the message always names one). */
    private static final ComplexRune NO_GLYPH = ComplexRunes.of(com.pockyl.hextale.rune.Essence.LIGHT, com.pockyl.hextale.rune.Essence.SHADOW);

    /** Sends the sign to every player near it whose client has the mod. */
    public static void send(ServerLevel level, RuneFxPayload payload) {
        double reach = Math.max(RADIUS, payload.size + 24);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(payload.at) <= reach * reach) {
                ModNetwork.sendToPlayer(player, payload);
            }
        }
    }

    @Override
    public Type<RuneFxPayload> type() {
        return TYPE;
    }
}
