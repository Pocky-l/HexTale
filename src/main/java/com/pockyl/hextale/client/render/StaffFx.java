package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.network.StaffFxPayload;
import com.pockyl.hextale.network.StaffFxPayload.Kind;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.runework.RiftStaff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The Rift Staff in light ({@link StaffFxPayload}). Void violet held in hextech gold: a bolt is a violet streak with a
 * white heart and a small gold hexagon turning round its head, shedding lilac glints; where it lands a hexagon opens
 * back along its way with sparks. The lance is a thick beam that swells and thins away, a wave of gold hexagons running
 * down it, a flash at the staff and a burst ring where it ends.
 *
 * <p>The ward is drawn for every player holding it up: seven hexagon cells before the eyes, the middle one first and the
 * ring unfolding round it, faint violet glass with lit edges and a gold rim. As the ward spends itself its outer cells go
 * dark one by one, so its state is seen, not read. A blow makes the cells near it flare with a hexagon pulse at the spot;
 * a break throws the cells outwards as they fade.
 */
public final class StaffFx {
    public static final int VIOLET = 0x8A5CF5;
    public static final int LILAC = 0xCBB8FF;
    public static final int HEART = 0xF6F0FF;
    public static final int GOLD = ModParticles.GOLD;
    private static final int DIM = 0x3A2C66;

    private static final float LAND_TICKS = 10;
    private static final float LANCE_TICKS = 14;
    private static final float HIT_TICKS = 8;
    private static final float BREAK_TICKS = 12;
    private static final float MIRROR_TICKS = 7;
    private static final float READY_TICKS = 12;
    /** Ticks the ward takes to unfold and to fold away. */
    private static final float UNFOLD = 5;
    private static final float FOLD = 4;
    /** A ward cell's radius and the seven cells' middles in the ward's plane (the middle, then the ring). */
    private static final float CELL = 0.33F;
    private static final float[][] CELLS = cells();
    /** How far behind the middle cell the ring sits, so the ward bows round the wielder. */
    private static final float BOW = 0.12F;

    private static final Map<Integer, Flight> FLIGHTS = new HashMap<>();
    private static final List<Shown> SHOWN = new ArrayList<>();
    /** Wards seen up last tick, by entity id, and the ones folding away (with the tick they were lowered). */
    private static final Map<Integer, Boolean> RAISED = new HashMap<>();
    private static final Map<Integer, Long> FOLDING = new HashMap<>();
    private static @Nullable ClientLevel level;

    private StaffFx() {
    }

    private record Shown(StaffFxPayload payload, long start) {
        float lasts() {
            return switch (payload.shape()) {
                case LAND -> LAND_TICKS;
                case LANCE -> LANCE_TICKS;
                case WARD_HIT -> HIT_TICKS;
                case WARD_BREAK -> BREAK_TICKS;
                case MIRROR -> MIRROR_TICKS;
                case READY -> READY_TICKS;
                case BOLT -> 0;
            };
        }
    }

    /** A bolt seen in flight: where it set off, its step per tick, when; it stops where the server says it landed. */
    private static final class Flight {
        final Vec3 from;
        final Vec3 step;
        final long start;
        final int run;
        @Nullable Vec3 landed;
        long landedAt;

        Flight(Vec3 from, Vec3 step, long start, int run) {
            this.from = from;
            this.step = step;
            this.start = start;
            this.run = run;
        }

        Vec3 at(float time) {
            float t = Math.max(0, time - start);
            Vec3 free = from.add(step.scale(t));
            if (landed != null && free.distanceToSqr(from) >= landed.distanceToSqr(from)) {
                return landed;
            }
            return free;
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(StaffFx::submit);
    }

    public static void forget() {
        FLIGHTS.clear();
        SHOWN.clear();
        RAISED.clear();
        FOLDING.clear();
        level = null;
    }

    public static void accept(StaffFxPayload payload) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null) {
            return;
        }
        if (current != level) {
            forget();
            level = current;
        }
        long now = current.getGameTime();
        switch (payload.shape()) {
            case BOLT -> {
                FLIGHTS.put(payload.id(), new Flight(payload.from(), payload.to(), now, Math.round(payload.value())));
                muzzleParticles(current, payload.from(), payload.to(), false);
            }
            case LAND -> {
                Flight flight = FLIGHTS.get(payload.id());
                if (flight != null) {
                    flight.landed = payload.from();
                    flight.landedAt = now;
                }
                SHOWN.add(new Shown(payload, now));
                landParticles(current, payload);
            }
            default -> {
                SHOWN.add(new Shown(payload, now));
                momentParticles(current, payload);
            }
        }
        if (SHOWN.size() > 64) {
            SHOWN.removeFirst();
        }
    }

    // ---- Particles --------------------------------------------------------------------------------------------------

    private static void tick() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null || current != level) {
            return;
        }
        long now = current.getGameTime();
        RandomSource random = current.getRandom();
        SHOWN.removeIf(shown -> now - shown.start() > shown.lasts() + 1 || now < shown.start());
        for (Iterator<Flight> it = FLIGHTS.values().iterator(); it.hasNext(); ) {
            Flight flight = it.next();
            if (flight.landed != null ? now - flight.landedAt > 3 : now - flight.start > RiftStaff.BOLT_TICKS + 10) {
                it.remove();
                continue;
            }
            if (flight.landed == null) {
                Vec3 p = flight.at(now);
                current.addParticle(ModParticles.glint(LILAC), p.x, p.y, p.z, (random.nextDouble() - 0.5) * 0.04,
                        (random.nextDouble() - 0.5) * 0.04, (random.nextDouble() - 0.5) * 0.04);
                if (random.nextInt(3) == 0) {
                    current.addParticle(ModParticles.voidMote(VIOLET), p.x, p.y, p.z, 0, 0.01, 0);
                }
            }
        }
        // Wards lowered since the last tick fold away.
        Map<Integer, Boolean> seen = new HashMap<>();
        for (Player player : current.players()) {
            if (warding(player)) {
                seen.put(player.getId(), true);
                FOLDING.remove(player.getId());
            } else if (RAISED.containsKey(player.getId())) {
                FOLDING.put(player.getId(), now);
            }
        }
        RAISED.clear();
        RAISED.putAll(seen);
        FOLDING.values().removeIf(at -> now - at > FOLD);
    }

    private static void muzzleParticles(ClientLevel level, Vec3 at, Vec3 way, boolean lance) {
        RandomSource random = level.getRandom();
        Vec3 dir = way.lengthSqr() < 1.0E-6 ? Vec3.ZERO : way.normalize();
        int n = lance ? 10 : 4;
        for (int i = 0; i < n; i++) {
            level.addParticle(ModParticles.glint(i % 3 == 0 ? GOLD : LILAC), at.x, at.y, at.z,
                    dir.x * 0.15 + (random.nextDouble() - 0.5) * 0.12, dir.y * 0.15 + (random.nextDouble() - 0.5) * 0.12,
                    dir.z * 0.15 + (random.nextDouble() - 0.5) * 0.12);
        }
    }

    private static void landParticles(ClientLevel level, StaffFxPayload payload) {
        RandomSource random = level.getRandom();
        Vec3 at = payload.from();
        Vec3 back = payload.to().lengthSqr() < 1.0E-6 ? Vec3.ZERO : payload.to().normalize().scale(-1);
        for (int i = 0; i < 10; i++) {
            level.addParticle(ModParticles.spark(i % 4 == 0 ? GOLD : VIOLET), at.x, at.y, at.z,
                    back.x * 0.12 + (random.nextDouble() - 0.5) * 0.3, back.y * 0.12 + random.nextDouble() * 0.15,
                    back.z * 0.12 + (random.nextDouble() - 0.5) * 0.3);
        }
        for (int i = 0; i < 5; i++) {
            level.addParticle(ModParticles.glint(LILAC), at.x, at.y, at.z, (random.nextDouble() - 0.5) * 0.15,
                    (random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15);
        }
        level.addParticle(ModParticles.vapour(DIM), at.x, at.y, at.z, back.x * 0.03, 0.02, back.z * 0.03);
    }

    private static void momentParticles(ClientLevel level, StaffFxPayload payload) {
        RandomSource random = level.getRandom();
        Vec3 from = payload.from();
        Vec3 to = payload.to();
        switch (payload.shape()) {
            case LANCE -> {
                muzzleParticles(level, from, to.subtract(from), true);
                double length = to.distanceTo(from);
                int n = (int) Math.min(48, length * 1.5);
                for (int i = 0; i < n; i++) {
                    Vec3 p = from.lerp(to, random.nextDouble());
                    level.addParticle(ModParticles.glint(i % 3 == 0 ? GOLD : LILAC), p.x, p.y, p.z, (random.nextDouble() - 0.5) * 0.08,
                            (random.nextDouble() - 0.5) * 0.08, (random.nextDouble() - 0.5) * 0.08);
                    if (i % 4 == 0) {
                        level.addParticle(ModParticles.voidMote(VIOLET), p.x, p.y, p.z, 0, 0.015, 0);
                    }
                }
                if (payload.value() > 0) {
                    for (int i = 0; i < 24; i++) {
                        float angle = i * Mth.TWO_PI / 24;
                        double speed = payload.value() * 0.12;
                        level.addParticle(ModParticles.spark(i % 3 == 0 ? GOLD : VIOLET), to.x, to.y + 0.1, to.z,
                                Mth.cos(angle) * speed, 0.05 + random.nextDouble() * 0.15, Mth.sin(angle) * speed);
                    }
                    for (int i = 0; i < 4; i++) {
                        level.addParticle(ModParticles.vapour(DIM), to.x + (random.nextDouble() - 0.5) * payload.value(), to.y + 0.1,
                                to.z + (random.nextDouble() - 0.5) * payload.value(), 0, 0.03, 0);
                    }
                }
            }
            case WARD_HIT -> {
                Entity holder = level.getEntity(payload.id());
                if (holder == Minecraft.getInstance().getCameraEntity() && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                    // The wielder sees the ward farther off: the sparks go where it is seen, not into the eyes.
                    Vec3 eye = holder.getEyePosition();
                    from = eye.add(from.subtract(eye).normalize().scale(OWN_DISTANCE));
                }
                for (int i = 0; i < 6; i++) {
                    level.addParticle(ModParticles.glint(i % 2 == 0 ? LILAC : GOLD), from.x, from.y, from.z, (random.nextDouble() - 0.5) * 0.2,
                            (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2);
                }
            }
            case WARD_BREAK -> {
                // Shards of the cells, flung out from where the ward stood.
                Entity holder = level.getEntity(payload.id());
                Vec3 centre = holder != null ? holder.getEyePosition().add(holder.getLookAngle().scale(RiftStaff.WARD_DISTANCE)) : from;
                for (int i = 0; i < 28; i++) {
                    level.addParticle(ModParticles.spark(i % 3 == 0 ? GOLD : VIOLET), centre.x + (random.nextDouble() - 0.5) * 1.4,
                            centre.y + (random.nextDouble() - 0.5) * 1.4, centre.z + (random.nextDouble() - 0.5) * 1.4,
                            (random.nextDouble() - 0.5) * 0.35, (random.nextDouble() - 0.3) * 0.3, (random.nextDouble() - 0.5) * 0.35);
                }
                for (int i = 0; i < 8; i++) {
                    level.addParticle(ModParticles.glint(LILAC), centre.x + (random.nextDouble() - 0.5), centre.y + (random.nextDouble() - 0.5),
                            centre.z + (random.nextDouble() - 0.5), 0, -0.02, 0);
                }
            }
            case MIRROR -> {
                for (int i = 1; i < 6; i++) {
                    Vec3 p = from.lerp(to, i / 6.0);
                    level.addParticle(ModParticles.glint(GOLD), p.x, p.y, p.z, 0, 0.01, 0);
                }
            }
            case READY -> {
                Entity holder = level.getEntity(payload.id());
                Vec3 at = holder != null ? RiftStaff.muzzle((net.minecraft.world.entity.LivingEntity) holder) : from;
                for (int i = 0; i < 8; i++) {
                    float angle = i * Mth.TWO_PI / 8;
                    level.addParticle(ModParticles.glint(GOLD), at.x + Mth.cos(angle) * 0.2, at.y, at.z + Mth.sin(angle) * 0.2,
                            -Mth.cos(angle) * 0.02, 0.02, -Mth.sin(angle) * 0.02);
                }
            }
            default -> {
            }
        }
    }

    // ---- Light ------------------------------------------------------------------------------------------------------

    private static void submit(SubmitCustomGeometryEvent event) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null) {
            return;
        }
        LevelRenderState state = event.getLevelRenderState();
        Vec3 camera = state.cameraRenderState.pos;
        float partial = state.worldPartialTicks;
        float now = state.gameTime + partial;
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        PoseStack poseStack = event.getPoseStack();
        Vector3f eye = new Vector3f();
        if (current == level) {
            for (Flight flight : FLIGHTS.values()) {
                bolt(poseStack, collector, flight, now, camera, eye);
            }
            for (Shown shown : SHOWN) {
                float age = now - shown.start();
                StaffFxPayload payload = shown.payload();
                switch (payload.shape()) {
                    case LAND -> land(poseStack, collector, payload, age, camera);
                    case LANCE -> lance(poseStack, collector, payload, age, camera, eye);
                    case MIRROR -> collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> mirror(consumer, pose, payload, age, camera, eye));
                    case READY -> ready(poseStack, collector, current, payload, age, partial, camera);
                    default -> {
                    }
                }
            }
        }
        for (Player player : current.players()) {
            if (player.distanceToSqr(camera) > 64 * 64) {
                continue;
            }
            boolean up = warding(player);
            Long folding = FOLDING.get(player.getId());
            Shown broke = lastOf(Kind.WARD_BREAK, player.getId(), now, BREAK_TICKS);
            if (broke != null) {
                shatter(poseStack, collector, player, now - broke.start(), partial, camera);
            } else if (up || folding != null) {
                float open = up ? Math.min(1, (player.getTicksUsingItem() + partial) / UNFOLD) : 1 - Math.min(1, (now - folding) / FOLD);
                ward(poseStack, collector, player, open, now, partial, camera);
            }
        }
    }

    private static boolean warding(Player player) {
        return player.isUsingItem() && RiftStaff.is(player.getUseItem());
    }

    private static @Nullable Shown lastOf(Kind kind, int id, float now, float within) {
        Shown found = null;
        for (Shown shown : SHOWN) {
            if (shown.payload().shape() == kind && shown.payload().id() == id && now - shown.start() <= within) {
                found = shown;
            }
        }
        return found;
    }

    /** A bolt: a violet streak tapering behind a white heart, a gold hexagon turning round its head. */
    private static void bolt(PoseStack poseStack, SubmitNodeCollector collector, Flight flight, float now, Vec3 camera, Vector3f eye) {
        float fade = flight.landed == null ? 1 : Math.max(0, 1 - (now - flight.landedAt) / 2F);
        if (fade <= 0) {
            return;
        }
        Vec3 head = flight.at(now);
        Vec3 dir = flight.step.normalize();
        double flown = head.distanceTo(flight.from);
        double tail = Math.min(2.4, flown);
        if (tail < 0.05) {
            return;
        }
        int n = 6;
        Vec3[] points = new Vec3[n];
        float[] light = new float[n];
        for (int i = 0; i < n; i++) {
            float s = i / (float) (n - 1);
            points[i] = head.subtract(dir.scale(tail * (1 - s))).subtract(camera);
            light[i] = s * s;
        }
        float swell = flight.run >= RiftStaff.LANCE_AFTER ? 1.15F : 1;
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            JarStreamFx.strip(consumer, pose, points, light, eye, 0.6F * swell, VIOLET, 0.65F * fade);
            JarStreamFx.strip(consumer, pose, points, light, eye, 0.24F * swell, VIOLET, 0.8F * fade);
            JarStreamFx.strip(consumer, pose, points, light, eye, 0.08F, LILAC, 0.9F * fade);
            Vec3 h = head.subtract(camera);
            Vec3[] glow = {h.subtract(dir.scale(0.12)), h.add(dir.scale(0.12))};
            JarStreamFx.strip(consumer, pose, glow, new float[]{1, 1}, eye, 0.5F * swell, VIOLET, 0.7F * fade);
            JarStreamFx.strip(consumer, pose, glow, new float[]{1, 1}, eye, 0.16F * swell, HEART, 0.8F * fade);
        });
        poseStack.pushPose();
        Vec3 h = head.subtract(camera);
        poseStack.translate(h.x, h.y, h.z);
        poseStack.rotate(facing(dir));
        poseStack.rotate(com.mojang.math.Axis.ZP.rotation(now * 0.35F));
        HexFx.hexRing(poseStack, collector, 0.17F, GOLD, 0.8F * fade);
        poseStack.popPose();
    }

    /** Where a bolt lands: a hexagon opening back along its way, and a soft flash. */
    private static void land(PoseStack poseStack, SubmitNodeCollector collector, StaffFxPayload payload, float age, Vec3 camera) {
        float t = age / LAND_TICKS;
        if (t >= 1 || t < 0) {
            return;
        }
        Vec3 at = payload.from().subtract(camera);
        Vec3 dir = payload.to().lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : payload.to().normalize();
        float ease = 1 - (1 - t) * (1 - t);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.rotate(facing(dir));
        HexFx.hexRing(poseStack, collector, 0.15F + 0.5F * ease, VIOLET, (1 - t) * 0.9F);
        HexFx.hexRing(poseStack, collector, 0.1F + 0.28F * ease, GOLD, (1 - t) * (1 - t) * 0.8F);
        poseStack.popPose();
        if (t < 0.4F) {
            float flash = 1 - t / 0.4F;
            Vector3f eye = new Vector3f();
            collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
                Vec3[] spot = {at.add(0, -0.3 * flash, 0), at.add(0, 0.3 * flash, 0)};
                JarStreamFx.strip(consumer, pose, spot, new float[]{1, 1}, eye, 0.7F * flash, LILAC, 0.5F * flash);
            });
        }
    }

    /** The lance: a beam swelling and thinning away, gold hexagons running down it, a burst ring where it ends. */
    private static void lance(PoseStack poseStack, SubmitNodeCollector collector, StaffFxPayload payload, float age, Vec3 camera, Vector3f eye) {
        float t = age / LANCE_TICKS;
        if (t >= 1 || t < 0) {
            return;
        }
        Vec3 from = payload.from().subtract(camera);
        Vec3 to = payload.to().subtract(camera);
        Vec3 way = to.subtract(from);
        double length = way.length();
        if (length < 0.1) {
            return;
        }
        Vec3 dir = way.scale(1 / length);
        float swell = Math.min(1, age / 2F);
        float fade = (1 - t) * (1 - t * 0.4F);
        float width = swell * (1 - 0.6F * t);
        int n = Math.max(3, (int) (length / 2));
        Vec3[] line = new Vec3[n + 1];
        float[] light = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            line[i] = from.add(way.scale(i / (double) n));
            light[i] = i == 0 ? 0.6F : 1;
        }
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            JarStreamFx.strip(consumer, pose, line, light, eye, 1.2F * width, VIOLET, 0.55F * fade);
            JarStreamFx.strip(consumer, pose, line, light, eye, 0.5F * width, VIOLET, 0.8F * fade);
            JarStreamFx.strip(consumer, pose, line, light, eye, 0.16F * width, LILAC, 0.9F * fade);
            JarStreamFx.strip(consumer, pose, line, light, eye, 0.05F, HEART, fade);
            if (t < 0.3F) {
                float flare = 1 - t / 0.3F;
                Vec3[] mouth = {from.subtract(dir.scale(0.15)), from.add(dir.scale(0.15))};
                JarStreamFx.strip(consumer, pose, mouth, new float[]{1, 1}, eye, 0.9F * flare, HEART, 0.6F * flare);
            }
        });
        // A wave of gold hexagons running from the staff to the end.
        Quaternionf face = facing(dir);
        float front = (float) Math.min(1, age / 4F) * (float) length;
        for (double d = 3; d < length; d += 2.5) {
            float since = front - (float) d;
            if (since < 0) {
                continue;
            }
            float k = Math.min(1, since / 3F);
            float alpha = (1 - k * 0.6F) * fade;
            Vec3 p = from.add(dir.scale(d));
            poseStack.pushPose();
            poseStack.translate(p.x, p.y, p.z);
            poseStack.rotate(face);
            poseStack.rotate(com.mojang.math.Axis.ZP.rotation((float) d * 0.6F + age * 0.08F));
            HexFx.hexRing(poseStack, collector, 0.22F + 0.18F * k, GOLD, 0.7F * alpha);
            poseStack.popPose();
        }
        if (payload.value() > 0 && age >= 2) {
            float b = Math.min(1, (age - 2) / 8F);
            float ease = 1 - (1 - b) * (1 - b);
            poseStack.pushPose();
            poseStack.translate(to.x, to.y, to.z);
            poseStack.rotate(face);
            HexFx.hexRing(poseStack, collector, payload.value() * ease, VIOLET, (1 - b) * 0.9F);
            HexFx.hexRing(poseStack, collector, payload.value() * 0.6F * ease, GOLD, (1 - b) * 0.8F);
            poseStack.popPose();
        }
    }

    private static void mirror(VertexConsumer consumer, PoseStack.Pose pose, StaffFxPayload payload, float age, Vec3 camera, Vector3f eye) {
        float t = age / MIRROR_TICKS;
        if (t >= 1 || t < 0) {
            return;
        }
        Vec3 from = payload.from().subtract(camera);
        Vec3 to = payload.to().subtract(camera);
        Vec3[] line = {from, from.lerp(to, 0.5), to};
        float[] light = {1, 1, 0.7F};
        JarStreamFx.strip(consumer, pose, line, light, eye, 0.25F, GOLD, 0.5F * (1 - t));
        JarStreamFx.strip(consumer, pose, line, light, eye, 0.06F, HEART, 0.8F * (1 - t));
    }

    /** The run is full: a gold hexagon closes in on the staff's head. */
    private static void ready(PoseStack poseStack, SubmitNodeCollector collector, ClientLevel level, StaffFxPayload payload, float age, float partial,
                              Vec3 camera) {
        float t = age / READY_TICKS;
        if (t >= 1 || t < 0) {
            return;
        }
        Entity holder = level.getEntity(payload.id());
        if (holder == null || holder == Minecraft.getInstance().getCameraEntity() && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        Vec3 at = RiftStaff.muzzle((net.minecraft.world.entity.LivingEntity) holder).subtract(camera);
        Vec3 look = holder.getViewVector(partial);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.rotate(facing(look));
        HexFx.hexRing(poseStack, collector, 0.6F * (1 - t) + 0.12F, GOLD, Mth.sin(Mth.PI * t));
        poseStack.popPose();
    }

    // ---- The ward ---------------------------------------------------------------------------------------------------

    /** The ward's frame: middle before the eyes, its plane across the look, its up with the wielder's up. */
    private record Frame(Vec3 centre, Vec3 forward, Quaternionf turn) {
    }

    /** The wielder's own ward, seen from the eyes: farther, so it covers the middle of the view and not all of it. */
    private static final double OWN_DISTANCE = 2.6;
    private static final float OWN_SIZE = 0.7F;

    private static Frame ownFrame(Player player, float partial) {
        Vec3 look = player.getViewVector(partial);
        return new Frame(player.getEyePosition(partial).add(look.scale(OWN_DISTANCE)), look, facing(look));
    }

    private static Frame frame(Player player, float partial) {
        Vec3 look = player.getViewVector(partial);
        Vec3 eye = player.getEyePosition(partial);
        return new Frame(eye.add(look.scale(RiftStaff.WARD_DISTANCE)), look, facing(look));
    }

    private static void ward(PoseStack poseStack, SubmitNodeCollector collector, Player player, float open, float now, float partial, Vec3 camera) {
        if (open <= 0) {
            return;
        }
        ItemStack staff = player.isUsingItem() ? player.getUseItem() : player.getMainHandItem();
        float held = RiftStaff.is(staff) ? RiftStaff.wardLeft(staff) / RiftStaff.wardMax(staff) : 1;
        int lit = Math.max(1, Mth.ceil(held * CELLS.length - 0.001F));
        boolean own = player == Minecraft.getInstance().getCameraEntity() && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        Frame frame = own ? ownFrame(player, partial) : frame(player, partial);
        // The wielder's own ward stands farther and smaller and is fainter: it must not hide what comes.
        float strength = own ? 0.55F : 1;
        float size = own ? OWN_SIZE : 1;
        Shown hit = lastOf(Kind.WARD_HIT, player.getId(), now, HIT_TICKS);
        Vector3f hitLocal = null;
        float hitGlow = 0;
        if (hit != null) {
            // Where on the ward the blow met it, in the ward's own cells (the server's ward stands at the usual distance).
            Frame usual = frame(player, partial);
            Vec3 off = hit.payload().from().subtract(usual.centre());
            hitLocal = new Quaternionf(usual.turn()).conjugate().transform(new Vector3f((float) off.x, (float) off.y, (float) off.z));
            hitGlow = 1 - (now - hit.start()) / HIT_TICKS;
        }
        Vec3 c = frame.centre().subtract(camera);
        poseStack.pushPose();
        poseStack.translate(c.x, c.y, c.z);
        poseStack.rotate(frame.turn());
        poseStack.scale(size, size, size);
        Vector3f pulseAt = hitLocal;
        float pulse = hitGlow;
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            for (int i = 0; i < CELLS.length; i++) {
                // The middle opens first, the ring after it, one cell after another.
                float own0 = i == 0 ? 0 : 0.25F + 0.1F * i;
                float k = Mth.clamp((open - own0) / (1 - own0 * 0.8F), 0, 1);
                if (k <= 0) {
                    continue;
                }
                float x = CELLS[i][0];
                float y = CELLS[i][1];
                float z = i == 0 ? 0 : -BOW;
                boolean alive = i < lit;
                float flare = 0;
                if (pulseAt != null) {
                    float dx = x - pulseAt.x;
                    float dy = y - pulseAt.y;
                    flare = pulse * Math.max(0, 1 - Mth.sqrt(dx * dx + dy * dy) / (CELL * 2.6F));
                }
                float shimmer = 0.85F + 0.15F * Mth.sin(now * 0.18F + i * 1.7F);
                float alpha = (alive ? shimmer : 0.25F + 0.2F * flicker(now, i)) * k * strength;
                cell(consumer, pose, x, y, z, CELL * (0.55F + 0.45F * k) * 0.94F, alive ? VIOLET : DIM, alpha, flare * strength, i != 0);
            }
        });
        if (pulseAt != null && pulse > 0) {
            poseStack.pushPose();
            poseStack.translate(pulseAt.x, pulseAt.y, 0.02F);
            HexFx.hexRing(poseStack, collector, 0.12F + 0.3F * (1 - pulse), GOLD, pulse * strength);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static float flicker(float now, int i) {
        return Mth.sin(now * 1.3F + i * 4.1F) > 0.6F ? 1 : 0;
    }

    /** One hexagon cell: faint glass, lit edges, a gold rim on the ward's outer edge. */
    private static void cell(VertexConsumer consumer, PoseStack.Pose pose, float cx, float cy, float z, float r, int color, float alpha, float flare,
                             boolean ring) {
        float a = Math.min(1, alpha + flare);
        int fillCentre = RuneDrawing.argb(color, 0.05F * a + 0.18F * flare);
        int fillEdge = RuneDrawing.argb(color, 0.14F * a + 0.25F * flare);
        for (int s = 0; s < 6; s++) {
            float a0 = Mth.TWO_PI * s / 6;
            float a1 = Mth.TWO_PI * (s + 1) / 6;
            float x0 = cx + Mth.cos(a0) * r;
            float y0 = cy + Mth.sin(a0) * r;
            float x1 = cx + Mth.cos(a1) * r;
            float y1 = cy + Mth.sin(a1) * r;
            consumer.addVertex(pose, cx, cy, z).setColor(fillCentre);
            consumer.addVertex(pose, x0, y0, z).setColor(fillEdge);
            consumer.addVertex(pose, x1, y1, z).setColor(fillEdge);
            consumer.addVertex(pose, cx, cy, z).setColor(fillCentre);
        }
        PoseStack.Pose lifted = pose.copy();
        lifted.translate(0, 0, z);
        for (int s = 0; s < 6; s++) {
            float a0 = Mth.TWO_PI * s / 6;
            float a1 = Mth.TWO_PI * (s + 1) / 6;
            float x0 = cx + Mth.cos(a0) * r;
            float y0 = cy + Mth.sin(a0) * r;
            float x1 = cx + Mth.cos(a1) * r;
            float y1 = cy + Mth.sin(a1) * r;
            HexFx.glowSegment(consumer, lifted, x0, y0, x1, y1, 0.07F, color, 0.45F * a);
            HexFx.glowSegment(consumer, lifted, x0, y0, x1, y1, 0.014F, LILAC, 0.45F * a);
            // The outer edges of the ring carry the gold rim.
            float mx = (x0 + x1) / 2;
            float my = (y0 + y1) / 2;
            if (ring && mx * mx + my * my > cx * cx + cy * cy + 0.01F) {
                HexFx.glowSegment(consumer, lifted, x0, y0, x1, y1, 0.012F, GOLD, 0.7F * a);
            }
        }
    }

    /** A broken ward: its cells thrown outwards and forwards, turning and fading. */
    private static void shatter(PoseStack poseStack, SubmitNodeCollector collector, Player player, float age, float partial, Vec3 camera) {
        float t = age / BREAK_TICKS;
        if (t >= 1 || t < 0) {
            return;
        }
        Frame frame = frame(player, partial);
        Vec3 c = frame.centre().subtract(camera);
        poseStack.pushPose();
        poseStack.translate(c.x, c.y, c.z);
        poseStack.rotate(frame.turn());
        float ease = 1 - (1 - t) * (1 - t);
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            for (int i = 0; i < CELLS.length; i++) {
                float x = CELLS[i][0];
                float y = CELLS[i][1];
                float out = i == 0 ? 0 : 1;
                float dx = x * out * 2.2F * ease;
                float dy = y * out * 2.2F * ease - 0.4F * t * t;
                float dz = 0.6F * ease;
                PoseStack.Pose spun = pose.copy();
                spun.translate(x + dx, y + dy, dz);
                spun.rotate(com.mojang.math.Axis.ZP.rotation((i % 2 == 0 ? 1 : -1) * 2.5F * ease));
                cell(consumer, spun, 0, 0, 0, CELL * (1 - 0.5F * t), i == 0 ? HEART : VIOLET, (1 - t) * 0.9F, (1 - t) * 0.5F, false);
            }
        });
        poseStack.popPose();
    }

    /** A turn that lays the pose's XY plane across {@code dir} (its Z along it), keeping its Y up as far as it can. */
    private static Quaternionf facing(Vec3 dir) {
        Vector3f forward = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
        if (forward.lengthSquared() < 1.0E-6F) {
            return new Quaternionf();
        }
        forward.normalize();
        Vector3f up = Math.abs(forward.y) > 0.98F ? new Vector3f(0, 0, 1) : new Vector3f(0, 1, 0);
        Vector3f right = new Vector3f(up).cross(forward).normalize();
        Vector3f realUp = new Vector3f(forward).cross(right).normalize();
        return new Quaternionf().setFromNormalized(new Matrix3f(right, realUp, forward));
    }

    private static float[][] cells() {
        float[][] cells = new float[7][2];
        float step = Mth.sqrt(3) * CELL;
        for (int i = 0; i < 6; i++) {
            float angle = Mth.PI / 6 + i * Mth.TWO_PI / 6;
            cells[i + 1][0] = Mth.cos(angle) * step;
            cells[i + 1][1] = Mth.sin(angle) * step;
        }
        return cells;
    }
}
