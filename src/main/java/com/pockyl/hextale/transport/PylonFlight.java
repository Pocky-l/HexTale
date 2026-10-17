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
 * One item's way, for the eye only: the server moves the item at once, the clients show the magic fetching it along
 * {@code path} - from where it lay (or the container it left), into the pylon's gem, through the relays' gems, into
 * where it went. The way is played as a {@link #phases() timeline}:
 * <ol>
 * <li>the pull ({@code path[0]} -> {@code path[1]}): a tendril of light reaches out of the gem to the item
 * ({@link Move#REACH}), its tip closes around it ({@link Move#GRAB}), the tendril draws back and drags the item along
 * ({@link Move#DRAG}), the item sinks into the gem ({@link Move#ABSORB});</li>
 * <li>every next leg: a tendril stretches out to the next point ({@link Move#EXTEND}) and the item slides along it
 * while the tendril's tail follows ({@link Move#SLIDE}); a short last leg into a container is a plain sink
 * ({@link Move#SINK}).</li>
 * </ol>
 * Essence ({@code color} >= 0, no item) flies instead as the altar's motes: every leg a {@link Move#STREAM}. Legs longer than a block bend
 * upward; {@link #curve} gives the point of a leg.
 */
public record PylonFlight(ItemStack icon, List<Vec3> path, boolean gathered, int color) {
    public static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, v -> (float) v.x, ByteBufCodecs.FLOAT, v -> (float) v.y, ByteBufCodecs.FLOAT, v -> (float) v.z, Vec3::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, PylonFlight> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, PylonFlight::icon,
            VEC.apply(ByteBufCodecs.list(12)), PylonFlight::path,
            ByteBufCodecs.BOOL, PylonFlight::gathered,
            ByteBufCodecs.INT, PylonFlight::color,
            PylonFlight::new);
    /** Ticks during which an essence leg lets its motes go. */
    public static final int MOTE_SPAWN_TICKS = 8;

    /** How long an essence mote flies a leg of this length (the altar's flow motes, at their longest). */
    public static float moteTicks(double length) {
        return (float) Mth.clamp(10 + length * 3.5, 12, 48) + 6;
    }

    /** A last leg shorter than this is a sink straight into the container, not a tendril. */
    private static final double SINK_BELOW = 1.5;

    public enum Move {
        REACH, GRAB, DRAG, ABSORB, EXTEND, SLIDE, SINK,
        /** Essence along a leg: glowing motes arcing from its start to its end, like the altar's. */
        STREAM
    }

    /** One step of the timeline: what happens on which leg, from tick {@code start} for {@code length} ticks. */
    public record Phase(Move move, int leg, float start, float length) {
        public float progress(float age) {
            return Mth.clamp((age - start) / length, 0, 1);
        }
    }

    public PylonFlight {
        path = List.copyOf(path);
    }

    /** An item's flight. */
    public PylonFlight(ItemStack icon, List<Vec3> path, boolean gathered) {
        this(icon, path, gathered, -1);
    }

    /** Essence of this colour poured along the way (no item). */
    public static PylonFlight essence(List<Vec3> path, int color) {
        return new PylonFlight(ItemStack.EMPTY, path, false, color & 0xFFFFFF);
    }

    public boolean isEssence() {
        return color >= 0;
    }

    public double legLength(int leg) {
        return path.get(leg).distanceTo(path.get(leg + 1));
    }

    /** The timeline of the whole way, in ticks. */
    public List<Phase> phases() {
        List<Phase> out = new ArrayList<>();
        if (path.size() < 2) {
            return out;
        }
        float at = 0;
        if (isEssence()) {
            // Essence flies like the altar's: motes are let go for MOTE_SPAWN_TICKS, each arcing to the leg's end in about
            // as long as the altar's motes take (MOTE_TICKS); the next leg starts when they have arrived.
            for (int leg = 0; leg < path.size() - 1; leg++) {
                at = add(out, Move.STREAM, leg, at, MOTE_SPAWN_TICKS + moteTicks(legLength(leg)));
            }
            return out;
        }
        double pull = legLength(0);
        at = add(out, Move.REACH, 0, at, (float) Mth.clamp(4 + pull * 1.1, 6, 16));
        at = add(out, Move.GRAB, 0, at, 6);
        at = add(out, Move.DRAG, 0, at, (float) Mth.clamp(7 + pull * 1.5, 9, 26));
        at = add(out, Move.ABSORB, 0, at, 5);
        for (int leg = 1; leg < path.size() - 1; leg++) {
            double length = legLength(leg);
            if (leg == path.size() - 2 && length < SINK_BELOW) {
                at = add(out, Move.SINK, leg, at, 8);
            } else {
                at = add(out, Move.EXTEND, leg, at, (float) Mth.clamp(3 + length * 0.6, 5, 12));
                at = add(out, Move.SLIDE, leg, at, (float) Mth.clamp(5 + length * 1.3, 8, 24));
            }
        }
        return out;
    }

    private static float add(List<Phase> out, Move move, int leg, float at, float length) {
        out.add(new Phase(move, leg, at, length));
        return at + length;
    }

    public int duration() {
        List<Phase> phases = phases();
        if (phases.isEmpty()) {
            return 1;
        }
        Phase last = phases.getLast();
        return Math.round(last.start() + last.length());
    }

    /** The phase at {@code age} ticks, or null after the end. */
    public Phase phaseAt(List<Phase> phases, float age) {
        for (Phase phase : phases) {
            if (age < phase.start() + phase.length()) {
                return phase;
            }
        }
        return null;
    }

    /**
     * The point at {@code t} (0-1) of leg {@code leg} ({@code path[leg]} -> {@code path[leg + 1]}). A leg longer than a
     * block arches upward, more the longer it is; short legs run straight.
     */
    public Vec3 curve(int leg, float t) {
        Vec3 a = path.get(leg);
        Vec3 b = path.get(leg + 1);
        double length = a.distanceTo(b);
        Vec3 straight = a.lerp(b, t);
        double lift = length > 1.2 ? 0.25 + 0.12 * length : 0;
        return straight.add(0, lift * 4 * t * (1 - t), 0);
    }

    /** Ease in and out (a sine), for moves that start and stop softly. */
    public static float smooth(float t) {
        return 0.5F - 0.5F * Mth.cos(Mth.clamp(t, 0, 1) * Mth.PI);
    }

    /** Ease out (fast, then settling), for a tendril shooting out. */
    public static float out(float t) {
        float u = 1 - Mth.clamp(t, 0, 1);
        return 1 - u * u * u;
    }

    /** The flight drawn on a client: when it began and whether its landing was heard. */
    public static final class Shown {
        public final PylonFlight flight;
        public final List<Phase> phases;
        public final long start;
        public final int duration;
        public boolean landed;

        public Shown(PylonFlight flight, long start) {
            this.flight = flight;
            this.phases = flight.phases();
            this.start = start;
            this.duration = flight.duration();
        }
    }

    /** The first {@code max} flights: a big move shows only a few of its items in the air. */
    public static List<PylonFlight> limit(List<PylonFlight> flights, int max) {
        return flights.size() <= max ? flights : new ArrayList<>(flights.subList(0, max));
    }
}
