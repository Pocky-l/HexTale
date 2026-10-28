package com.pockyl.hextale.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.pockyl.hextale.port.client.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.network.RuneEffectPayload;
import com.pockyl.hextale.network.RuneEffectPayload.Family;
import com.pockyl.hextale.registry.ModParticles;

import java.util.ArrayList;
import java.util.List;

/**
 * What a rune did, drawn where it did it ({@link RuneEffectPayload}): light in the stele's style ({@link HexGlow}) for
 * the shapes - lightning bolts, a block's glowing outline, a thread along a vein - and the mod's particles for the
 * substance - flames, dust, motes, puffs. Short (under a second) and soft: these play for runes at work all day.
 */
public final class RuneEffects {
    private static final int MAX_SHOWN = 96;
    /** Ticks a bolt lives, and between two blocks being struck / crumbling. */
    private static final float BOLT_TICKS = 8;
    private static final float STAGGER = 0.5F;
    private static final int KINDLE_TICKS = 14;
    private static final int RISE_TICKS = 16;
    private static final int GUST_TICKS = 12;
    /** A traced loop: the light runs round it in at most this many ticks, then the whole line glows and fades. */
    private static final float TRACE_RUN = 24;
    private static final int TRACE_GLOW = 20;
    /** A traced line lies just over the floor of its blocks (chalk height). */
    private static final double TRACE_Y = 0.08;
    private static final int DUST = 0x9C8A70;
    private static final int AIR = 0xEEF4F8;

    private static final List<Shown> SHOWN = new ArrayList<>();
    private static @Nullable ClientLevel level;

    record Shown(RuneEffectPayload payload, long start, int seed) {
        float lasts() {
            int n = payload.blocks().size();
            return switch (payload.kind()) {
                case ARCS -> BOLT_TICKS + n * STAGGER + 2;
                case KINDLE -> KINDLE_TICKS + 2;
                case CRUMBLE -> n * STAGGER + 12;
                case RISE -> RISE_TICKS;
                case GUST -> GUST_TICKS;
                case TRACE -> n * traceStagger(n) + TRACE_GLOW;
            };
        }
    }

    /** Ticks the light of a trace takes from one block to the next. */
    private static float traceStagger(int n) {
        return Math.min(1, TRACE_RUN / Math.max(1, n));
    }

    private static Vec3 traceAt(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + TRACE_Y, pos.getZ() + 0.5);
    }

    private RuneEffects() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(RuneEffects::submit);
    }

    public static void accept(RuneEffectPayload payload) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null) {
            return;
        }
        if (current != level) {
            SHOWN.clear();
            level = current;
        }
        if (SHOWN.size() >= MAX_SHOWN) {
            SHOWN.remove(0);
        }
        SHOWN.add(new Shown(payload, current.getGameTime(), current.getRandom().nextInt()));
    }

    /** The substance of each effect: particles, spawned tick by tick while it lives. */
    private static void tick() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null || current != level || SHOWN.isEmpty()) {
            return;
        }
        long now = current.getGameTime();
        SHOWN.removeIf(shown -> now - shown.start() > shown.lasts() || now < shown.start());
        RandomSource random = current.getRandom();
        for (Shown shown : SHOWN) {
            RuneEffectPayload payload = shown.payload();
            int age = (int) (now - shown.start());
            List<BlockPos> blocks = payload.blocks();
            Vec3 at = payload.at();
            switch (payload.kind()) {
                case ARCS -> {
                    for (int i = 0; i < blocks.size(); i++) {
                        if ((int) (i * STAGGER) == age) {
                            Vec3 c = Vec3.atCenterOf(blocks.get(i));
                            for (int k = 0; k < 3; k++) {
                                current.addParticle(ModParticles.spark(payload.color()), c.x, c.y, c.z, (random.nextDouble() - 0.5) * 0.2,
                                        random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.2);
                            }
                        }
                    }
                }
                case KINDLE -> {
                    if (age < KINDLE_TICKS) {
                        for (BlockPos pos : blocks) {
                            // Flames lick up the block's sides from its foot, thinning out as the fire dies.
                            for (int k = 0; k < 2; k++) {
                            if (random.nextFloat() < 0.8F * (1 - age / (float) KINDLE_TICKS)) {
                                double x = pos.getX() + random.nextDouble();
                                double z = pos.getZ() + random.nextDouble();
                                boolean side = random.nextBoolean();
                                x = side ? pos.getX() + (random.nextBoolean() ? 0.02 : 0.98) : x;
                                z = side ? z : pos.getZ() + (random.nextBoolean() ? 0.02 : 0.98);
                                current.addParticle(ModParticles.flame(ModParticles.FIRE), x, pos.getY() + random.nextDouble() * 0.6, z, 0, 0.02, 0);
                            }
                            }
                            if (random.nextInt(8) == 0) {
                                current.addParticle(ModParticles.spark(ModParticles.FIRE), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                                        (random.nextDouble() - 0.5) * 0.1, 0.08, (random.nextDouble() - 0.5) * 0.1);
                            }
                        }
                    }
                }
                case CRUMBLE -> {
                    for (int i = 0; i < blocks.size(); i++) {
                        if ((int) (i * STAGGER) == age) {
                            BlockPos pos = blocks.get(i);
                            for (int k = 0; k < 4; k++) {
                                current.addParticle(ModParticles.vapour(DUST), pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                                        pos.getZ() + random.nextDouble(), 0, -0.01, 0);
                            }
                            current.addParticle(ModParticles.spark(payload.color()), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0.02, 0);
                        }
                    }
                }
                case RISE -> {
                    if (age < RISE_TICKS) {
                        for (int k = 0; k < 2; k++) {
                            float angle = shown.seed() + age * 0.6F + k * Mth.PI;
                            float r = 0.35F * payload.size() * (1 - age / (float) RISE_TICKS) + 0.05F;
                            current.addParticle(ModParticles.glint(payload.color()), at.x + Mth.cos(angle) * r, at.y + age * 0.06 * payload.size(),
                                    at.z + Mth.sin(angle) * r, 0, 0.01, 0);
                        }
                    }
                }
                case GUST -> {
                    if (age < GUST_TICKS && age % 2 == 0) {
                        for (int k = 0; k < 3; k++) {
                            float angle = shown.seed() + age * 0.5F + k * Mth.TWO_PI / 3;
                            float r = 0.55F * payload.size();
                            double tx = -Mth.sin(angle);
                            double tz = Mth.cos(angle);
                            current.addParticle(ModParticles.vapour(payload.color() == 0 ? AIR : payload.color()), at.x + Mth.cos(angle) * r,
                                    at.y + (random.nextDouble() - 0.3) * 0.5, at.z + Mth.sin(angle) * r, tx * 0.08, 0.02, tz * 0.08);
                        }
                    }
                }
                case TRACE -> {
                    int n = blocks.size();
                    if (n == 0) {
                        break;
                    }
                    float stagger = traceStagger(n);
                    int head = (int) (age / stagger);
                    if (head < n) {
                        // A glint rides the head of the light.
                        Vec3 c = traceAt(blocks.get(head));
                        current.addParticle(ModParticles.glint(payload.color()), c.x, c.y + 0.05, c.z, 0, 0.01, 0);
                    } else if (age == (int) Math.ceil(n * stagger)) {
                        // Closed: the whole line breathes out a few motes.
                        for (int i = 0; i < n; i += 3) {
                            Vec3 c = traceAt(blocks.get(i));
                            current.addParticle(ModParticles.glint(payload.color()), c.x + (random.nextDouble() - 0.5) * 0.4, c.y + 0.1,
                                    c.z + (random.nextDouble() - 0.5) * 0.4, 0, 0.03, 0);
                        }
                    }
                }
            }
        }
    }

    /** The shapes of each effect: light, drawn every frame. */
    private static void submit(SubmitCustomGeometryEvent event) {
        if (SHOWN.isEmpty() || Minecraft.getInstance().level != level) {
            return;
        }
        LevelRenderState state = event.getLevelRenderState();
        Vec3 camera = state.cameraRenderState.pos;
        float now = state.gameTime + state.worldPartialTicks;
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        Vector3f eye = new Vector3f();
        for (Shown shown : SHOWN) {
            float age = now - shown.start();
            RuneEffectPayload payload = shown.payload();
            List<BlockPos> blocks = payload.blocks();
            switch (payload.kind()) {
                case ARCS -> collector.submitCustomGeometry(event.getPoseStack(), HexGlow.TYPE, (pose, consumer) -> {
                    Vec3 from = payload.at().subtract(camera);
                    int core = RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.7F);
                    for (int i = 0; i < blocks.size(); i++) {
                        float a = age - i * STAGGER;
                        if (a < 0 || a > BOLT_TICKS) {
                            continue;
                        }
                        float fade = 1 - a / BOLT_TICKS;
                        Vec3[] bolt = bolt(from, Vec3.atCenterOf(blocks.get(i)).subtract(camera), shown.seed() + i * 31 + (int) (a / 2));
                        float[] light = new float[bolt.length];
                        java.util.Arrays.fill(light, 1);
                        JarStreamFx.strip(consumer, pose, bolt, light, eye, 0.14F, payload.color(), 0.5F * fade);
                        JarStreamFx.strip(consumer, pose, bolt, light, eye, 0.035F, core, fade);
                    }
                });
                case KINDLE -> {
                    float fade = 1 - age / KINDLE_TICKS;
                    if (fade > 0) {
                        collector.submitCustomGeometry(event.getPoseStack(), HexGlow.TYPE, (pose, consumer) -> {
                            for (BlockPos pos : blocks) {
                                outline(consumer, pose, Vec3.atLowerCornerOf(pos).subtract(camera), eye, ModParticles.FIRE, 0.28F * fade);
                            }
                        });
                    }
                }
                case CRUMBLE -> collector.submitCustomGeometry(event.getPoseStack(), HexGlow.TYPE, (pose, consumer) -> {
                    for (int i = 1; i < blocks.size(); i++) {
                        float a = age - i * STAGGER;
                        if (a < 0 || a > 10) {
                            continue;
                        }
                        Vec3[] link = {Vec3.atCenterOf(blocks.get(i - 1)).subtract(camera), Vec3.atCenterOf(blocks.get(i)).subtract(camera)};
                        float[] light = {1, 1};
                        JarStreamFx.strip(consumer, pose, link, light, eye, 0.08F, payload.color(), 0.6F * (1 - a / 10));
                    }
                });
                case TRACE -> collector.submitCustomGeometry(event.getPoseStack(), HexGlow.TYPE, (pose, consumer) -> {
                    int n = blocks.size();
                    float stagger = traceStagger(n);
                    float run = n * stagger;
                    int core = RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.65F);
                    // After the loop closes the whole line flares and fades.
                    float closed = age < run ? 0 : Math.max(0, 1 - (age - run) / TRACE_GLOW);
                    float flare = age < run ? 0 : closed * (age - run < 4 ? 1 + 0.6F * (1 - (age - run) / 4) : 1);
                    for (int i = 0; i < n; i++) {
                        // Segment i runs from block i to the next, the last one back to the first.
                        float lit = (age - i * stagger) / stagger;
                        if (lit <= 0 || n < 2) {
                            continue;
                        }
                        BlockPos from = blocks.get(i);
                        BlockPos to = blocks.get((i + 1) % n);
                        if (from.distManhattan(to) != 1) {
                            // Not neighbours: the line jumps (a branch, or a figure longer than one message) - no light across.
                            continue;
                        }
                        Vec3 a = traceAt(from).subtract(camera);
                        Vec3 b = traceAt(to).subtract(camera);
                        Vec3 end = lit < 1 ? a.add(b.subtract(a).scale(lit)) : b;
                        // Bright at the head, a glowing trail behind it.
                        float behind = age - (i + 1) * stagger;
                        float trail = age < run ? Math.max(0.35F, 1 - Math.max(0, behind) / 8) : flare;
                        Vec3[] link = {a, end};
                        float[] light = {1, 1};
                        JarStreamFx.strip(consumer, pose, link, light, eye, 0.24F, payload.color(), 0.3F * trail);
                        JarStreamFx.strip(consumer, pose, link, light, eye, 0.05F, core, 0.85F * trail);
                    }
                });
                default -> {
                }
            }
        }
    }

    /** A jagged bolt from {@code from} to {@code to}: kinks across the way, widest in the middle. */
    private static Vec3[] bolt(Vec3 from, Vec3 to, int seed) {
        RandomSource random = RandomSource.create(seed);
        int n = 7;
        Vec3 way = to.subtract(from);
        double length = way.length();
        Vec3 axis = length < 1.0E-4 ? new Vec3(0, 1, 0) : way.scale(1 / length);
        Vec3 side = axis.cross(Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 up = side.cross(axis).normalize();
        Vec3[] points = new Vec3[n + 1];
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double kink = i == 0 || i == n ? 0 : Math.sin(Math.PI * t) * length * 0.14;
            points[i] = from.add(way.scale(t)).add(side.scale((random.nextDouble() - 0.5) * 2 * kink))
                    .add(up.scale((random.nextDouble() - 0.5) * 2 * kink));
        }
        return points;
    }

    /** The twelve edges of a block as soft lines of light (a block glowing hot). */
    private static void outline(com.mojang.blaze3d.vertex.VertexConsumer consumer, com.mojang.blaze3d.vertex.PoseStack.Pose pose, Vec3 corner,
                                Vector3f eye, int color, float alpha) {
        float[] light = {1, 1};
        for (int axis = 0; axis < 3; axis++) {
            for (int a = 0; a <= 1; a++) {
                for (int b = 0; b <= 1; b++) {
                    double[] p0 = new double[3];
                    double[] p1 = new double[3];
                    int u = (axis + 1) % 3;
                    int v = (axis + 2) % 3;
                    p0[u] = p1[u] = a;
                    p0[v] = p1[v] = b;
                    p1[axis] = 1;
                    Vec3[] edge = {corner.add(p0[0], p0[1], p0[2]), corner.add(p1[0], p1[1], p1[2])};
                    JarStreamFx.strip(consumer, pose, edge, light, eye, 0.07F, color, alpha);
                }
            }
        }
    }
}
