package com.pockyl.hextale.transport;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * One item's way through the air, for the eye only: the server moves the item at once, the clients show it flying along
 * {@code path} (from where it lay or the container it left, through the gems it passes, into where it went). Legs bend
 * upward in arcs; the flight takes longer the longer the way.
 */
public record PylonFlight(ItemStack icon, List<Vec3> path, boolean gathered) {
    public static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, v -> (float) v.x, ByteBufCodecs.FLOAT, v -> (float) v.y, ByteBufCodecs.FLOAT, v -> (float) v.z, Vec3::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, PylonFlight> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, PylonFlight::icon,
            VEC.apply(ByteBufCodecs.list(12)), PylonFlight::path,
            ByteBufCodecs.BOOL, PylonFlight::gathered,
            PylonFlight::new);
    /** Ticks per block of the way, and the shortest and longest flight. */
    private static final float TICKS_PER_BLOCK = 1.6F;
    private static final int MIN_TICKS = 12;
    private static final int MAX_TICKS = 48;

    public PylonFlight {
        path = List.copyOf(path);
    }

    public int duration() {
        return Mth.clamp(Math.round(length() * TICKS_PER_BLOCK), MIN_TICKS, MAX_TICKS);
    }

    public float length() {
        float total = 0;
        for (int i = 1; i < path.size(); i++) {
            total += (float) path.get(i - 1).distanceTo(path.get(i));
        }
        return total;
    }

    /**
     * The point at {@code t} (0-1) of the whole way, by distance. A leg longer than a block arches upward by a fifth of
     * its length; short legs (into the gem, down into a container) run straight.
     */
    public Vec3 at(float t) {
        float total = length();
        if (total <= 0 || path.size() < 2) {
            return path.getFirst();
        }
        float d = Mth.clamp(t, 0, 1) * total;
        for (int i = 1; i < path.size(); i++) {
            Vec3 a = path.get(i - 1);
            Vec3 b = path.get(i);
            float leg = (float) a.distanceTo(b);
            if (d <= leg || i == path.size() - 1) {
                float u = leg <= 0 ? 1 : Mth.clamp(d / leg, 0, 1);
                Vec3 straight = a.lerp(b, u);
                float lift = leg > 1.2F ? leg * 0.2F : 0;
                return straight.add(0, lift * 4 * u * (1 - u), 0);
            }
            d -= leg;
        }
        return path.getLast();
    }

    /** The flight drawn on a client: when it began and whether its landing was heard. */
    public static final class Shown {
        public final PylonFlight flight;
        public final long start;
        public final int duration;
        public boolean landed;

        public Shown(PylonFlight flight, long start) {
            this.flight = flight;
            this.start = start;
            this.duration = flight.duration();
        }
    }

    /** The first {@code max} flights: a big move shows only a few of its items in the air. */
    public static List<PylonFlight> limit(List<PylonFlight> flights, int max) {
        return flights.size() <= max ? flights : new ArrayList<>(flights.subList(0, max));
    }
}
