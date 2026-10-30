package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.network.WeaponFxPayload;
import com.pockyl.hextale.network.WeaponFxPayload.Kind;
import com.pockyl.hextale.registry.ModParticles;

import java.util.ArrayList;
import java.util.List;

/**
 * The weapon runes' moments ({@link WeaponFxPayload}), each made of the real thing and of light. A Shockwave heaves the
 * ground block by block - each block jumps up out of its place and settles back, spitting crumbs of itself - while a
 * glowing crack runs ahead along the ground. A Blink leaves a streak of shadow and a fading silhouette where the player
 * stood. A Detonator's burst is a flash, a ring of light racing out and sparks. A Whirlpool turns two arms of light on
 * the ground, tightening, with water swirling in. A Whirlwind is a crescent of light sweeping round the player.
 */
public final class WeaponFx {
    private static final int MAX_SHOWN = 32;
    /** Ticks a block of a Shockwave takes to jump up and settle back. */
    private static final float POP = 7;
    /** How high the first block of a wave jumps (later ones a little less). */
    private static final float POP_HEIGHT = 0.6F;
    private static final float CRACK_TICKS = 14;
    private static final float BLINK_TICKS = 10;
    private static final float BURST_TICKS = 9;
    private static final float SWEEP_TICKS = 6;
    private static final int SHADOW = 0x2A1640;
    private static final int WATER = 0x3E6EA8;
    /** Shapes on the ground lie flat across this. */
    private static final Vector3f UP = new Vector3f(0, 1, 0);

    private static final List<Shown> SHOWN = new ArrayList<>();
    private static @Nullable ClientLevel level;

    private record Shown(WeaponFxPayload payload, long start, int seed) {
        float lasts() {
            return switch (payload.shape()) {
                case WAVE -> lastStep(payload) + POP + CRACK_TICKS;
                case BLINK -> BLINK_TICKS;
                case BURST -> BURST_TICKS + 4;
                case VORTEX -> (float) payload.to().x + 6;
                case SWEEP -> SWEEP_TICKS + 2;
            };
        }
    }

    private static int lastStep(WeaponFxPayload payload) {
        int last = 0;
        for (int step : payload.steps()) {
            last = Math.max(last, step);
        }
        return last;
    }

    private WeaponFx() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(WeaponFx::submit);
    }

    public static void accept(WeaponFxPayload payload) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null) {
            return;
        }
        if (current != level) {
            SHOWN.clear();
            level = current;
        }
        if (SHOWN.size() >= MAX_SHOWN) {
            SHOWN.removeFirst();
        }
        SHOWN.add(new Shown(payload, current.getGameTime(), current.getRandom().nextInt()));
    }

    // ---- Particles, tick by tick ------------------------------------------------------------------------------------

    private static void tick() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null || current != level || SHOWN.isEmpty()) {
            return;
        }
        long now = current.getGameTime();
        SHOWN.removeIf(shown -> now - shown.start() > shown.lasts() || now < shown.start());
        RandomSource random = current.getRandom();
        for (Shown shown : SHOWN) {
            WeaponFxPayload payload = shown.payload();
            int age = (int) (now - shown.start());
            switch (payload.shape()) {
                case WAVE -> waveParticles(current, payload, age, random);
                case BLINK -> blinkParticles(current, payload, age, random);
                case BURST -> burstParticles(current, payload, age, random);
                case VORTEX -> vortexParticles(current, payload, age, random, shown.seed());
                case SWEEP -> sweepParticles(current, payload, age, random);
            }
        }
    }

    private static void waveParticles(ClientLevel level, WeaponFxPayload payload, int age, RandomSource random) {
        if (age == 0) {
            // The blow itself: a ring of dust thrown out round the spot the weapon hit.
            Vec3 at = payload.from();
            for (int i = 0; i < 14; i++) {
                float angle = i * Mth.TWO_PI / 14;
                level.addParticle(ModParticles.vapour(0x9C8A70), at.x, at.y + 0.1, at.z, Mth.cos(angle) * 0.18, 0.02, Mth.sin(angle) * 0.18);
            }
        }
        List<BlockPos> blocks = payload.blocks();
        for (int i = 0; i < blocks.size(); i++) {
            if (payload.steps().get(i) != age) {
                continue;
            }
            BlockPos pos = blocks.get(i);
            BlockState state = level.getBlockState(pos);
            double top = pos.getY() + 1.02;
            if (!state.isAir()) {
                // Crumbs of the very block, tossed up as it heaves.
                BlockParticleOption crumbs = new BlockParticleOption(ParticleTypes.BLOCK, state);
                for (int k = 0; k < 7; k++) {
                    level.addParticle(crumbs, pos.getX() + random.nextDouble(), top, pos.getZ() + random.nextDouble(),
                            (random.nextDouble() - 0.5) * 0.25, 0.18 + random.nextDouble() * 0.22, (random.nextDouble() - 0.5) * 0.25);
                }
            }
            for (int k = 0; k < 2; k++) {
                level.addParticle(ModParticles.vapour(0x9C8A70), pos.getX() + random.nextDouble(), top, pos.getZ() + random.nextDouble(),
                        0, 0.03, 0);
            }
            level.addParticle(ModParticles.spark(payload.color()), pos.getX() + 0.5, top, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.1, 0.12, (random.nextDouble() - 0.5) * 0.1);
        }
    }

    private static void blinkParticles(ClientLevel level, WeaponFxPayload payload, int age, RandomSource random) {
        Vec3 from = payload.from();
        Vec3 to = payload.to();
        float height = payload.size();
        if (age == 0) {
            // Shadow smoke along the way, thickest where the step began.
            for (int i = 0; i <= 20; i++) {
                Vec3 p = from.lerp(to, i / 20.0).add(0, height * (0.3 + 0.4 * random.nextDouble()), 0);
                level.addParticle(ModParticles.vapour(SHADOW), p.x, p.y, p.z, 0, 0.01, 0);
                if (i % 3 == 0) {
                    level.addParticle(ModParticles.glint(payload.color()), p.x, p.y, p.z, 0, 0.02, 0);
                }
            }
            for (int k = 0; k < 10; k++) {
                level.addParticle(ModParticles.glint(payload.color()), to.x, to.y + height * random.nextDouble(), to.z,
                        (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.3);
            }
        }
        if (age < 6) {
            // The silhouette left behind crumbles into the dark.
            for (int k = 0; k < 3; k++) {
                level.addParticle(ModParticles.vapour(SHADOW), from.x + (random.nextDouble() - 0.5) * 0.5, from.y + height * random.nextDouble(),
                        from.z + (random.nextDouble() - 0.5) * 0.5, 0, 0.02, 0);
            }
        }
    }

    private static void burstParticles(ClientLevel level, WeaponFxPayload payload, int age, RandomSource random) {
        Vec3 at = payload.from();
        float radius = payload.size();
        if (age == 0) {
            for (int i = 0; i < 28; i++) {
                float angle = i * Mth.TWO_PI / 28 + random.nextFloat() * 0.2F;
                double speed = radius * 0.11;
                level.addParticle(ModParticles.spark(payload.color()), at.x, at.y, at.z,
                        Mth.cos(angle) * speed, 0.05 + random.nextDouble() * 0.12, Mth.sin(angle) * speed);
            }
            for (int k = 0; k < 10; k++) {
                level.addParticle(ModParticles.flame(ModParticles.FIRE), at.x + (random.nextDouble() - 0.5) * 0.6, at.y + (random.nextDouble() - 0.5) * 0.6,
                        at.z + (random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.12, 0.04, (random.nextDouble() - 0.5) * 0.12);
            }
        }
        if (age < 5) {
            level.addParticle(ModParticles.vapour(ModParticles.SMOKE), at.x + (random.nextDouble() - 0.5) * radius, at.y,
                    at.z + (random.nextDouble() - 0.5) * radius, 0, 0.04, 0);
        }
    }

    private static void vortexParticles(ClientLevel level, WeaponFxPayload payload, int age, RandomSource random, int seed) {
        if (age > payload.to().x) {
            return;
        }
        Vec3 c = payload.from();
        float radius = payload.size();
        for (int k = 0; k < 3; k++) {
            float angle = seed + age * 0.35F + k * Mth.TWO_PI / 3;
            float r = radius * (0.35F + 0.6F * random.nextFloat());
            double x = c.x + Mth.cos(angle) * r;
            double z = c.z + Mth.sin(angle) * r;
            // Swirling in: across the radius and a little towards the middle.
            double vx = -Mth.sin(angle) * 0.16 - Mth.cos(angle) * 0.05;
            double vz = Mth.cos(angle) * 0.16 - Mth.sin(angle) * 0.05;
            level.addParticle(ModParticles.vapour(WATER), x, c.y + 0.15 + random.nextDouble() * 0.3, z, vx, 0.005, vz);
            if (random.nextInt(3) == 0) {
                level.addParticle(ModParticles.glint(payload.color()), x, c.y + 0.3, z, vx, 0.02, vz);
            }
        }
    }

    private static void sweepParticles(ClientLevel level, WeaponFxPayload payload, int age, RandomSource random) {
        if (age != 1) {
            return;
        }
        Vec3 c = payload.from();
        float radius = payload.size();
        level.addParticle(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 0, 0, 0);
        for (int i = 0; i < 18; i++) {
            float angle = i * Mth.TWO_PI / 18;
            level.addParticle(ModParticles.glint(payload.color()), c.x + Mth.cos(angle) * radius * 0.9, c.y, c.z + Mth.sin(angle) * radius * 0.9,
                    Mth.cos(angle) * 0.05, 0.02, Mth.sin(angle) * 0.05);
        }
    }

    // ---- Shapes: moving blocks and light ----------------------------------------------------------------------------

    private static void submit(SubmitCustomGeometryEvent event) {
        ClientLevel current = Minecraft.getInstance().level;
        if (SHOWN.isEmpty() || current != level || current == null) {
            return;
        }
        LevelRenderState state = event.getLevelRenderState();
        Vec3 camera = state.cameraRenderState.pos;
        float now = state.gameTime + state.worldPartialTicks;
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        PoseStack poseStack = event.getPoseStack();
        Vector3f eye = new Vector3f();
        for (Shown shown : SHOWN) {
            float age = now - shown.start();
            WeaponFxPayload payload = shown.payload();
            switch (payload.shape()) {
                case WAVE -> {
                    heave(current, poseStack, collector, payload, age, camera);
                    collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> crack(consumer, pose, payload, age, camera, eye));
                }
                case BLINK -> collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> blink(consumer, pose, payload, age, camera, eye));
                case BURST -> collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> burst(consumer, pose, payload, age, camera, eye));
                case VORTEX -> collector.submitCustomGeometry(poseStack, HexGlow.TYPE,
                        (pose, consumer) -> vortex(consumer, pose, payload, age, camera, eye, shown.seed()));
                case SWEEP -> collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> sweep(consumer, pose, payload, age, camera, eye));
            }
        }
    }

    /** Each block of the wave jumps out of its place and settles back, tipping a little away from the blow. */
    private static void heave(ClientLevel level, PoseStack poseStack, SubmitNodeCollector collector, WeaponFxPayload payload, float age, Vec3 camera) {
        List<BlockPos> blocks = payload.blocks();
        int last = Math.max(1, lastStep(payload));
        Vec3 way = payload.to().subtract(payload.from()).multiply(1, 0, 1);
        Vec3 dir = way.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : way.normalize();
        for (int i = 0; i < blocks.size(); i++) {
            int step = payload.steps().get(i);
            float a = age - step;
            if (a <= 0 || a >= POP) {
                continue;
            }
            BlockPos pos = blocks.get(i);
            BlockState block = level.getBlockState(pos);
            if (block.isAir()) {
                continue;
            }
            float t = a / POP;
            // Up fast, down with a small bounce at the end.
            float lift = Mth.sin(Mth.PI * Math.min(1, t * 1.15F)) * POP_HEIGHT * (1 - 0.35F * step / last) * (0.85F + 0.3F * payload.size() / 3);
            float tip = Mth.sin(Mth.PI * t) * 9 * ((i * 7919 % 3) - 1);
            MovingBlockRenderState render = new MovingBlockRenderState();
            render.blockState = block;
            render.blockPos = pos.above();
            render.randomSeedPos = pos;
            render.biome = level.getBiome(pos);
            render.lightEngine = level.getLightEngine();
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x + 0.5, pos.getY() - camera.y + lift + 0.002, pos.getZ() - camera.z + 0.5);
            poseStack.rotate(Axis.of(new Vector3f((float) dir.z, 0, (float) -dir.x)).rotationDegrees(-6 * Mth.sin(Mth.PI * t)));
            poseStack.rotate(Axis.YP.rotationDegrees(tip));
            poseStack.translate(-0.5, 0, -0.5);
            collector.submitMovingBlock(poseStack, render, 0);
            poseStack.popPose();
        }
    }

    /** A crack of light running along the ground ahead of the heaving blocks, cooling behind them. */
    private static void crack(VertexConsumer consumer, PoseStack.Pose pose, WeaponFxPayload payload, float age, Vec3 camera, Vector3f eye) {
        List<BlockPos> blocks = payload.blocks();
        int core = RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.35F);
        BlockPos previous = null;
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos pos = blocks.get(i);
            int step = payload.steps().get(i);
            boolean middle = i == 0 || payload.steps().get(i - 1).intValue() != step;
            if (!middle) {
                continue;
            }
            if (previous != null) {
                float a = age - step;
                if (a > -0.5F && a < CRACK_TICKS) {
                    float glow = a < 0 ? 0.5F : 1 - a / CRACK_TICKS;
                    Vec3 from = new Vec3(previous.getX() + 0.5, previous.getY() + 1.03, previous.getZ() + 0.5).subtract(camera);
                    Vec3 to = new Vec3(pos.getX() + 0.5, pos.getY() + 1.03, pos.getZ() + 0.5).subtract(camera);
                    // A jagged crack, not a ruled line: two kinks across the way per block, the same every frame.
                    Vec3 way = to.subtract(from);
                    Vec3 across = new Vec3(-way.z, 0, way.x).normalize();
                    RandomSource kinks = RandomSource.create(pos.asLong());
                    Vec3[] link = {from, from.add(way.scale(0.33)).add(across.scale((kinks.nextFloat() - 0.5) * 0.5)),
                            from.add(way.scale(0.66)).add(across.scale((kinks.nextFloat() - 0.5) * 0.5)), to};
                    float[] light = {1, 1, 1, 1};
                    JarStreamFx.flatStrip(consumer, pose, link, light, UP, 0.7F, payload.color(), 0.45F * glow);
                    JarStreamFx.flatStrip(consumer, pose, link, light, UP, 0.14F, core, 0.9F * glow);
                }
            }
            previous = pos;
        }
    }

    private static void blink(VertexConsumer consumer, PoseStack.Pose pose, WeaponFxPayload payload, float age, Vec3 camera, Vector3f eye) {
        float fade = 1 - age / BLINK_TICKS;
        if (fade <= 0) {
            return;
        }
        float height = payload.size();
        Vec3 from = payload.from().subtract(camera);
        Vec3 to = payload.to().subtract(camera);
        float[] light = {1, 1};
        // The streak of the step, at chest height.
        Vec3[] streak = {from.add(0, height * 0.55, 0), to.add(0, height * 0.55, 0)};
        JarStreamFx.strip(consumer, pose, streak, light, eye, 0.5F * fade, payload.color(), 0.28F * fade);
        JarStreamFx.strip(consumer, pose, streak, light, eye, 0.08F, RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.5F), 0.8F * fade);
        // The silhouette where the player stood, thinning upwards.
        Vec3[] figure = {from.add(0, 0.05, 0), from.add(0, height, 0)};
        JarStreamFx.strip(consumer, pose, figure, light, eye, 0.55F, payload.color(), 0.22F * fade);
    }

    private static void burst(VertexConsumer consumer, PoseStack.Pose pose, WeaponFxPayload payload, float age, Vec3 camera, Vector3f eye) {
        float t = age / BURST_TICKS;
        if (t >= 1) {
            return;
        }
        Vec3 c = payload.from().subtract(camera);
        float radius = payload.size() * (0.25F + 0.75F * (1 - (1 - t) * (1 - t)));
        float fade = 1 - t;
        ring(consumer, pose, c, radius, 0, 32, 0.35F * fade + 0.05F, payload.color(), 0.55F * fade, eye);
        ring(consumer, pose, c, radius * 0.97F, 0, 32, 0.06F, RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.6F), 0.9F * fade, eye);
        if (t < 0.35F) {
            // The flash at the heart of it.
            float flash = 1 - t / 0.35F;
            Vec3[] beam = {c.add(0, -0.6 * flash, 0), c.add(0, 0.6 * flash, 0)};
            JarStreamFx.strip(consumer, pose, beam, new float[]{1, 1}, eye, 1.2F * flash, payload.color(), 0.6F * flash);
        }
    }

    private static void vortex(VertexConsumer consumer, PoseStack.Pose pose, WeaponFxPayload payload, float age, Vec3 camera, Vector3f eye, int seed) {
        float life = (float) payload.to().x;
        float fade = Math.min(1, age / 4) * Math.min(1, (life + 6 - age) / 8);
        if (fade <= 0) {
            return;
        }
        Vec3 c = payload.from().subtract(camera).add(0, 0.06, 0);
        float radius = payload.size();
        for (int arm = 0; arm < 2; arm++) {
            // An arm of light winding from the rim to the middle, turning faster as it tightens.
            int n = 24;
            Vec3[] points = new Vec3[n];
            float[] light = new float[n];
            for (int i = 0; i < n; i++) {
                float s = i / (float) (n - 1);
                float r = radius * (1 - s) + 0.2F;
                float angle = seed + arm * Mth.PI + s * Mth.PI * 1.6F + age * 0.25F;
                points[i] = c.add(Mth.cos(angle) * r, 0, Mth.sin(angle) * r);
                light[i] = 0.4F + 0.6F * s;
            }
            JarStreamFx.flatStrip(consumer, pose, points, light, UP, 0.3F, payload.color(), 0.4F * fade);
            JarStreamFx.flatStrip(consumer, pose, points, light, UP, 0.07F, RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.5F), 0.8F * fade);
        }
        ring(consumer, pose, c, radius, age * 0.05F, 40, 0.1F, payload.color(), 0.3F * fade, eye);
    }

    private static void sweep(VertexConsumer consumer, PoseStack.Pose pose, WeaponFxPayload payload, float age, Vec3 camera, Vector3f eye) {
        float t = age / SWEEP_TICKS;
        if (t >= 1) {
            return;
        }
        Vec3 c = payload.from().subtract(camera);
        float radius = payload.size() * (0.55F + 0.45F * t);
        float fade = 1 - t * t;
        // A crescent that runs once round the player, from where they look, its head bright and its tail fading.
        float startYaw = (float) payload.to().x * Mth.DEG_TO_RAD + Mth.HALF_PI;
        int n = 36;
        float head = Math.min(1, t * 1.6F) * Mth.TWO_PI;
        Vec3[] points = new Vec3[n];
        float[] light = new float[n];
        for (int i = 0; i < n; i++) {
            float s = i / (float) (n - 1);
            float angle = startYaw + head * s;
            points[i] = c.add(Mth.cos(angle) * radius, 0.15 * Mth.sin(s * Mth.PI), Mth.sin(angle) * radius);
            light[i] = s;
        }
        JarStreamFx.flatStrip(consumer, pose, points, light, UP, 0.6F, payload.color(), 0.45F * fade);
        JarStreamFx.flatStrip(consumer, pose, points, light, UP, 0.1F, RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.6F), 0.95F * fade);
    }

    /** A flat ring of light round {@code c}. */
    private static void ring(VertexConsumer consumer, PoseStack.Pose pose, Vec3 c, float radius, float turn, int n, float width, int color,
                             float alpha, Vector3f eye) {
        Vec3[] points = new Vec3[n + 1];
        float[] light = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            float angle = turn + i * Mth.TWO_PI / n;
            points[i] = c.add(Mth.cos(angle) * radius, 0, Mth.sin(angle) * radius);
            light[i] = 1;
        }
        JarStreamFx.flatStrip(consumer, pose, points, light, UP, width, color, alpha);
    }
}
