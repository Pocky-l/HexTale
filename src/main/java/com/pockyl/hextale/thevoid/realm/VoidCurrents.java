package com.pockyl.hextale.thevoid.realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.VoidCurrentsPayload;

/**
 * The Undertow: streams of the Void's air that run from plate to plate in long arcs over the gaps. Inside one a
 * creature is carried along it, steering a little; sneaking lets it go. Some currents break: every
 * {@link #PERIOD} ticks they fade for {@link #BROKEN} ticks and drop whoever rides them - the tell comes
 * {@link #WARN} ticks before, as the streaks thin out and the whoosh sinks.
 *
 * <p>The currents are laid out with the rest of the Void ({@link VoidShapes#currents}); the server sends each player
 * those round them, and the client carries its own player (player movement is the client's), draws the streaks and
 * plays the sounds. The server only keeps riders from taking fall damage.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class VoidCurrents {
    /** How close to a current's line one is caught. */
    public static final double RADIUS = 1.7;
    /** Speed along a current, blocks per tick. */
    public static final double SPEED = 0.8;
    public static final int PERIOD = 900;
    public static final int BROKEN = 160;
    public static final int WARN = 70;
    /** Regions round a player whose currents it is told of. */
    private static final int SEND_EVERY = 40;
    private static final Map<UUID, Long> SENT = new HashMap<>();

    private VoidCurrents() {
    }

    /** One current: points along its line (every ~2 blocks), whether it breaks, and a salt for its timing. */
    public record Current(float[] xs, float[] ys, float[] zs, boolean breaking, int salt) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Current> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list(256)).map(VoidCurrents::floats, VoidCurrents::list), Current::xs,
                ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list(256)).map(VoidCurrents::floats, VoidCurrents::list), Current::ys,
                ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list(256)).map(VoidCurrents::floats, VoidCurrents::list), Current::zs,
                ByteBufCodecs.BOOL, Current::breaking,
                ByteBufCodecs.INT, Current::salt,
                Current::new);

        public int points() {
            return xs.length;
        }

        /** 0 flowing, 1 about to break (the tell), 2 broken. */
        public int state(long gameTime) {
            if (!breaking) {
                return 0;
            }
            long t = Math.floorMod(gameTime + salt, PERIOD);
            return t < BROKEN ? 2 : t >= PERIOD - WARN ? 1 : 0;
        }

        public Vec3 point(int i) {
            return new Vec3(xs[i], ys[i], zs[i]);
        }

        /**
         * Where on the line {@code at} is nearest: {segment, share along it, distance}; segment -1 when it is far
         * from every part of it.
         */
        public double[] nearest(Vec3 at, double reach) {
            double best = reach;
            double[] found = {-1, 0, reach};
            for (int i = 0; i + 1 < points(); i++) {
                double ax = xs[i], ay = ys[i], az = zs[i];
                double ex = xs[i + 1] - ax, ey = ys[i + 1] - ay, ez = zs[i + 1] - az;
                double px = at.x - ax, py = at.y - ay, pz = at.z - az;
                double len2 = ex * ex + ey * ey + ez * ez;
                double t = len2 < 1e-6 ? 0 : Math.max(0, Math.min(1, (px * ex + py * ey + pz * ez) / len2));
                double qx = px - ex * t, qy = py - ey * t, qz = pz - ez * t;
                double d = Math.sqrt(qx * qx + qy * qy + qz * qz);
                if (d < best) {
                    best = d;
                    found = new double[]{i, t, d};
                }
            }
            return found;
        }

        /** The point at {@code segment} + {@code share}. */
        public Vec3 at(int segment, double share) {
            return point(segment).lerp(point(Math.min(points() - 1, segment + 1)), share);
        }

        /** The way the current runs at a segment (unit length). */
        public Vec3 tangent(int segment) {
            int i = Math.min(segment, points() - 2);
            return point(i + 1).subtract(point(i)).normalize();
        }
    }

    private static float[] floats(List<Float> values) {
        float[] out = new float[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    private static List<Float> list(float[] values) {
        List<Float> out = new ArrayList<>(values.length);
        for (float value : values) {
            out.add(value);
        }
        return out;
    }

    /** The currents of the regions round a point of the Void, or none when the level is not the Void. */
    public static List<Current> near(ServerLevel level, double x, double z) {
        if (!(level.getChunkSource().getGenerator() instanceof VoidChunkGenerator generator)) {
            return List.of();
        }
        VoidShapes shapes = generator.shapes(level.getChunkSource().randomState());
        int rx = Math.floorDiv((int) Math.floor(x), VoidShapes.REGION);
        int rz = Math.floorDiv((int) Math.floor(z), VoidShapes.REGION);
        List<Current> found = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                found.addAll(shapes.currents(rx + dx, rz + dz));
            }
        }
        return found;
    }

    /** Players in the Void: told of the currents round them when they reach new regions; riders take no fall damage. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(VoidPassage.LEVEL)) {
            SENT.remove(player.getUUID());
            return;
        }
        long region = VoidShapes.chunkKey(Math.floorDiv(player.getBlockX(), VoidShapes.REGION), Math.floorDiv(player.getBlockZ(), VoidShapes.REGION));
        Long sent = SENT.get(player.getUUID());
        if ((sent == null || sent != region) && player.tickCount % SEND_EVERY == 0 || sent == null) {
            SENT.put(player.getUUID(), region);
            ModNetwork.sendToPlayer(player, new VoidCurrentsPayload(near(level, player.getX(), player.getZ())));
        }
        if (player.tickCount % 4 == 0 && riding(level, player.position().add(0, player.getBbHeight() * 0.5, 0)) != null) {
            player.resetFallDistance();
            if (player.tickCount % 20 == 0) {
                com.pockyl.hextale.quest.Quests.fire(player, com.pockyl.hextale.quest.Quests.VOID_CURRENT);
            }
        }
    }

    /** A chest opened in the Void is a swallowed ruin's (nothing else of ours stands there). */
    @SubscribeEvent
    public static void onContainerOpen(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(VoidPassage.LEVEL)
                && event.getContainer() instanceof net.minecraft.world.inventory.ChestMenu) {
            com.pockyl.hextale.quest.Quests.fire(player, com.pockyl.hextale.quest.Quests.SWALLOWED_RUIN);
        }
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    /** The current carrying something at {@code at} now, or null. */
    public static Current riding(ServerLevel level, Vec3 at) {
        for (Current current : near(level, at.x, at.z)) {
            if (current.state(level.getGameTime()) < 2 && current.nearest(at, RADIUS)[0] >= 0) {
                return current;
            }
        }
        return null;
    }
}
