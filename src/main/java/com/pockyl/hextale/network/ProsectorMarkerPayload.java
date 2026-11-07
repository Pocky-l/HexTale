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
 * Server to client: one telegraph or strike of the Prosector's fight, a word for {@code client.render.ProsectorFx}, which
 * draws it for {@code ticks}. One payload per marker: what is fixed in the world travels here, what follows a moving
 * thing (the live Incision line, the Probe, the hooks) goes through the Prosector's synced data.
 *
 * @param marker what to draw
 * @param a      where it stands (a line's start)
 * @param b      a line's end, or the way an arc faces
 * @param size   its radius or width
 * @param ticks  how long it lasts
 * @param color  its light
 */
public record ProsectorMarkerPayload(Marker marker, Vec3 a, Vec3 b, float size, int ticks, int color) implements CustomPacketPayload {
    public static final Type<ProsectorMarkerPayload> TYPE = new Type<>(Hextale.id("prosector_marker"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProsectorMarkerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(id -> Marker.values()[Math.floorMod(id, Marker.values().length)], Marker::ordinal), ProsectorMarkerPayload::marker,
            Vec3.STREAM_CODEC, ProsectorMarkerPayload::a,
            Vec3.STREAM_CODEC, ProsectorMarkerPayload::b,
            ByteBufCodecs.FLOAT, ProsectorMarkerPayload::size,
            ByteBufCodecs.VAR_INT, ProsectorMarkerPayload::ticks,
            ByteBufCodecs.INT, ProsectorMarkerPayload::color,
            ProsectorMarkerPayload::new);
    /** Blocks within which a marker is sent: wider than the fight, so nobody in it ever misses one. */
    public static final double SEEN = 96;

    /** The shapes the fight draws. */
    public enum Marker {
        /** A warning line along the floor, a to b. */
        LINE,
        /** The cut: the line opening. */
        CUT,
        /** A hexagon on the floor. */
        HEX,
        /** A rune circle on the floor. */
        CIRCLE,
        /** A plain ring on the floor: where something will land. */
        RING,
        /** An arc on the floor round a, facing b. */
        ARC,
        /** Light bursting up from a spot. */
        ERUPT,
        /** Something heavy came down: dust, ring and shake. */
        SLAM,
        /** A glyph in the air where a hit went dull or something was written down. */
        GLYPH,
        /** A jar broke. */
        BREAK,
        /** A great ring of light: a turn. */
        TURN,
        /** The view shakes, no more. */
        SHAKE,
        /** A line of light from the holder of a Seer's Eye to the ground it calls to. */
        WAY,
        /** A cell of the ground cut open: a hexagon of light with sparks over it. */
        WOUND,
        /** Every {@link #WOUND} closes at once. */
        HEAL
    }

    public static void send(ServerLevel level, Marker marker, Vec3 a, Vec3 b, float size, int ticks, int color) {
        ProsectorMarkerPayload word = new ProsectorMarkerPayload(marker, a, b, size, ticks, color);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(a) < SEEN * SEEN) {
                ModNetwork.sendToPlayer(player, word);
            }
        }
    }

    @Override
    public Type<ProsectorMarkerPayload> type() {
        return TYPE;
    }
}
