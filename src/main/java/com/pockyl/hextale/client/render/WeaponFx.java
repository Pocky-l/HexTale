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
 *
 * <p>The gear runes' moments are drawn the same way. A Wavewalk step spreads two rings of light over the water with a
 * splash of droplets; a Static spark is a jagged bolt that twitches from the robe to the attacker; a Mirage leaves a
 * shimmering figure of light where the wearer stood, crumbling into motes; a Flamethrower's jets lick out a tongue of
 * fire, white at the nozzle and red at the tip, shedding embers; a Sunlight Cell drinks a soft shaft of sunlight with
 * motes drifting down it; an Arc Spire's Beam is a hot white-gold line with a flare and flames where it lands.
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
    private static final float RIPPLE_TICKS = 26;
    private static final float ZAP_TICKS = 7;
    private static final float MIRAGE_TICKS = 40;
    private static final float SCORCH_TICKS = 7;
    private static final float SUNBEAM_TICKS = 36;
    private static final float BEAM_TICKS = 9;
    /** The colours of fire, from the red edge to the white heart. */
    private static final int FIRE_EDGE = 0xFF5A1E;
    private static final int FIRE_BODY = 0xFFA640;
    private static final int FIRE_HEART = 0xFFF0C8;
    private static final int SUNLIGHT = 0xFFE7A0;
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
                case RIPPLE -> RIPPLE_TICKS;
                case ZAP -> ZAP_TICKS;
                case MIRAGE -> MIRAGE_TICKS;
                case SCORCH -> SCORCH_TICKS;
                case SUNBEAM -> SUNBEAM_TICKS;
                case BEAM -> BEAM_TICKS + 2;
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
                case RIPPLE -> rippleParticles(current, payload, age, random);
                case ZAP -> zapParticles(current, payload, age, random);
                case MIRAGE -> mirageParticles(current, payload, age, random);
                case SCORCH -> scorchParticles(current, payload, age, random);
                case SUNBEAM -> sunbeamParticles(current, payload, age, random);
                case BEAM -> beamParticles(current, payload, age, random);
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
        // No vanilla sweep sprite: its flat white crescent hid the crescent of light drawn in the rune's colour.
        for (int i = 0; i < 18; i++) {
            float angle = i * Mth.TWO_PI / 18;
            level.addParticle(ModParticles.glint(payload.color()), c.x + Mth.cos(angle) * radius * 0.9, c.y, c.z + Mth.sin(angle) * radius * 0.9,
                    Mth.cos(angle) * 0.05, 0.02, Mth.sin(angle) * 0.05);
        }
    }

    private static void rippleParticles(ClientLevel level, WeaponFxPayload payload, int age, RandomSource random) {
        if (age != 0) {
            return;
        }
        Vec3 c = payload.from();
        // Droplets kicked up by the step, and a few glints caught on the first ring.
        for (int k = 0; k < 4; k++) {
            level.addParticle(ParticleTypes.SPLASH, c.x + (random.nextDouble() - 0.5) * 0.5, c.y + 0.05, c.z + (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.1, 0.12, (random.nextDouble() - 0.5) * 0.1);
        }
        for (int k = 0; k < 3; k++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            level.addParticle(ModParticles.glint(payload.color()), c.x + Mth.cos(angle) * 0.4, c.y + 0.06, c.z + Mth.sin(angle) * 0.4,
                    Mth.cos(angle) * 0.03, 0.01, Mth.sin(angle) * 0.03);
        }
    }

    private static void zapParticles(ClientLevel level, WeaponFxPayload payload, int age, RandomSource random) {
        if (age != 0) {
            return;
        }
        Vec3 from = payload.from();
        Vec3 to = payload.to();
        for (int i = 1; i < 6; i++) {
            Vec3 p = from.lerp(to, i / 6.0);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, (random.nextDouble() - 0.5) * 0.1, 0.02, (random.nextDouble() - 0.5) * 0.1);
        }
        for (int k = 0; k < 9; k++) {
            level.addParticle(ModParticles.spark(payload.color()), to.x, to.y, to.z,
                    (random.nextDouble() - 0.5) * 0.3, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.3);
        }
        for (int k = 0; k < 4; k++) {
            level.addParticle(ModParticles.glint(payload.color()), from.x, from.y, from.z,
                    (random.nextDouble() - 0.5) * 0.12, 0.03, (random.nextDouble() - 0.5) * 0.12);
        }
    }

    private static void mirageParticles(ClientLevel level, WeaponFxPayload payload, int age, RandomSource random) {
        Vec3 c = payload.from();
        float height = payload.size();
        int light = RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.4F);
        if (age == 0) {
            // The wearer slips out of sight in a breath of light.
            for (int k = 0; k < 14; k++) {
                level.addParticle(ModParticles.glint(light), c.x + (random.nextDouble() - 0.5) * 0.7, c.y + height * random.nextDouble(),
                        c.z + (random.nextDouble() - 0.5) * 0.7, (random.nextDouble() - 0.5) * 0.08, 0.02, (random.nextDouble() - 0.5) * 0.08);
            }
            for (int k = 0; k < 5; k++) {
                level.addParticle(ModParticles.vapour(light), c.x, c.y + height * 0.5, c.z,
                        (random.nextDouble() - 0.5) * 0.1, 0.01, (random.nextDouble() - 0.5) * 0.1);
            }
        } else if (age == MIRAGE_TICKS - 10) {
            // The figure crumbles into motes that rise away.
            for (int k = 0; k < 16; k++) {
                level.addParticle(ModParticles.glint(light), c.x + (random.nextDouble() - 0.5) * 0.5, c.y + height * random.nextDouble(),
                        c.z + (random.nextDouble() - 0.5) * 0.5, 0, 0.04 + random.nextDouble() * 0.04, 0);
            }
        } else if (age % 3 == 0 && age < MIRAGE_TICKS - 10) {
            level.addParticle(ModParticles.glint(light), c.x + (random.nextDouble() - 0.5) * 0.45, c.y + height * random.nextDouble(),
                    c.z + (random.nextDouble() - 0.5) * 0.45, 0, 0.02, 0);
        }
    }

    private static void scorchParticles(ClientLevel level, WeaponFxPayload payload, int age, RandomSource random) {
        Vec3 from = payload.from();
        Vec3 way = payload.to().subtract(from);
        if (age == 0) {
            for (int k = 0; k < 8; k++) {
                double t = random.nextDouble() * 0.8;
                Vec3 p = from.add(way.scale(t));
                double spread = payload.size() * t * 0.5;
                level.addParticle(ModParticles.flame(ModParticles.FIRE), p.x + (random.nextDouble() - 0.5) * spread, p.y + (random.nextDouble() - 0.5) * spread,
                        p.z + (random.nextDouble() - 0.5) * spread, way.x * 0.06, way.y * 0.06, way.z * 0.06);
            }
            for (int k = 0; k < 3; k++) {
                // Embers shed by the tongue, falling behind.
                Vec3 p = from.add(way.scale(0.3 + random.nextDouble() * 0.6));
                level.addParticle(ModParticles.spark(FIRE_EDGE), p.x, p.y, p.z, (random.nextDouble() - 0.5) * 0.06, -0.04, (random.nextDouble() - 0.5) * 0.06);
            }
        } else if (age == 3) {
            Vec3 tip = payload.to();
            level.addParticle(ModParticles.vapour(ModParticles.SMOKE), tip.x, tip.y, tip.z, 0, 0.03, 0);
        }
    }

    private static void sunbeamParticles(ClientLevel level, WeaponFxPayload payload, int age, RandomSource random) {
        if (age % 3 != 0 || age > SUNBEAM_TICKS - 8) {
            return;
        }
        // Motes of sunlight drifting down the shaft into the battery.
        Vec3 c = payload.from();
        double top = payload.size() * (0.6 + 0.4 * random.nextDouble());
        level.addParticle(ModParticles.glint(SUNLIGHT), c.x + (random.nextDouble() - 0.5) * 0.4, c.y + top, c.z + (random.nextDouble() - 0.5) * 0.4,
                0, -0.07, 0);
    }

    private static void beamParticles(ClientLevel level, WeaponFxPayload payload, int age, RandomSource random) {
        if (age != 0) {
            return;
        }
        Vec3 from = payload.from();
        Vec3 to = payload.to();
        for (int i = 1; i < 10; i++) {
            Vec3 p = from.lerp(to, i / 10.0);
            level.addParticle(ModParticles.spark(FIRE_BODY), p.x, p.y, p.z, 0, 0.01, 0);
        }
        for (int k = 0; k < 7; k++) {
            level.addParticle(ModParticles.flame(ModParticles.FIRE), to.x + (random.nextDouble() - 0.5) * 0.4, to.y + (random.nextDouble() - 0.5) * 0.5,
                    to.z + (random.nextDouble() - 0.5) * 0.4, (random.nextDouble() - 0.5) * 0.05, 0.04, (random.nextDouble() - 0.5) * 0.05);
        }
        level.addParticle(ModParticles.vapour(ModParticles.SMOKE), to.x, to.y + 0.3, to.z, 0, 0.03, 0);
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
                case RIPPLE -> collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> ripple(consumer, pose, payload, age, camera, eye));
                case ZAP -> collector.submitCustomGeometry(poseStack, HexGlow.TYPE,
                        (pose, consumer) -> zap(consumer, pose, payload, age, camera, eye, shown.seed()));
                case MIRAGE -> collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> mirage(consumer, pose, payload, age, camera, eye));
                case SCORCH -> collector.submitCustomGeometry(poseStack, HexGlow.TYPE,
                        (pose, consumer) -> scorch(consumer, pose, payload, age, camera, eye, shown.seed()));
                case SUNBEAM -> collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> sunbeam(consumer, pose, payload, age, camera, eye));
                case BEAM -> collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> beam(consumer, pose, payload, age, camera, eye));
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
        JarStreamFx.flatStrip(consumer, pose, points, light, UP, 0.6F, payload.color(), 0.35F * fade);
        JarStreamFx.flatStrip(consumer, pose, points, light, UP, 0.08F, RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.35F), 0.7F * fade);
    }

    /** Two rings of light spreading over the water from a step, the second a little behind the first. */
    private static void ripple(VertexConsumer consumer, PoseStack.Pose pose, WeaponFxPayload payload, float age, Vec3 camera, Vector3f eye) {
        Vec3 c = payload.from().subtract(camera).add(0, 0.03, 0);
        int core = RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.55F);
        float span = RIPPLE_TICKS - 6;
        for (int k = 0; k < 2; k++) {
            float a = age - k * 5;
            if (a < 0 || a >= span) {
                continue;
            }
            float t = a / span;
            float share = k == 0 ? 1 : 0.7F;
            float radius = 0.2F + payload.size() * (1 - (1 - t) * (1 - t)) * share;
            float fade = (1 - t) * (1 - t) * share;
            ring(consumer, pose, c, radius, 0, 40, 0.24F * (1 - t) + 0.05F, payload.color(), 0.4F * fade, eye);
            ring(consumer, pose, c, radius, 0, 40, 0.05F, core, 0.85F * fade, eye);
        }
    }

    /** A jagged bolt from the robe to the attacker, twitching into a new shape every other tick, with one branch. */
    private static void zap(VertexConsumer consumer, PoseStack.Pose pose, WeaponFxPayload payload, float age, Vec3 camera, Vector3f eye, int seed) {
        float fade = 1 - age / ZAP_TICKS;
        if (fade <= 0) {
            return;
        }
        Vec3 from = payload.from().subtract(camera);
        Vec3 to = payload.to().subtract(camera);
        Vec3 way = to.subtract(from);
        Vec3 a = way.cross(new Vec3(0, 1, 0));
        a = a.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : a.normalize();
        Vec3 b = way.cross(a).normalize();
        RandomSource shape = RandomSource.create(seed * 31L + (int) (age / 2));
        int n = 9;
        Vec3[] points = new Vec3[n];
        float[] light = new float[n];
        double jag = Math.min(0.35, way.length() * 0.12);
        for (int i = 0; i < n; i++) {
            float s = i / (float) (n - 1);
            double bend = Mth.sin(Mth.PI * s) * jag;
            points[i] = from.add(way.scale(s)).add(a.scale((shape.nextDouble() - 0.5) * 2 * bend)).add(b.scale((shape.nextDouble() - 0.5) * 2 * bend));
            light[i] = 1;
        }
        float flicker = ((int) age) % 2 == 0 ? 1 : 0.65F;
        int core = RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.7F);
        JarStreamFx.strip(consumer, pose, points, light, eye, 0.38F, payload.color(), 0.45F * fade * flicker);
        JarStreamFx.strip(consumer, pose, points, light, eye, 0.06F, core, 0.95F * fade);
        // A branch splitting off halfway and dying out.
        Vec3 fork = points[n / 2];
        Vec3 off = a.scale(shape.nextDouble() - 0.5).add(b.scale(shape.nextDouble() - 0.5)).add(way.normalize().scale(0.3)).normalize().scale(0.6);
        Vec3[] branch = {fork, fork.add(off.scale(0.5)).add(a.scale((shape.nextDouble() - 0.5) * 0.15)), fork.add(off)};
        float[] dying = {1, 0.6F, 0};
        JarStreamFx.strip(consumer, pose, branch, dying, eye, 0.2F, payload.color(), 0.35F * fade * flicker);
        JarStreamFx.strip(consumer, pose, branch, dying, eye, 0.04F, core, 0.8F * fade);
    }

    /**
     * A figure of light where the wearer stood: legs, body, arms and head as soft strips facing the camera, wavering and
     * breathing, on a faint ring of light; it fades in at once and out over its last ticks.
     */
    private static void mirage(VertexConsumer consumer, PoseStack.Pose pose, WeaponFxPayload payload, float age, Vec3 camera, Vector3f eye) {
        float fade = Math.min(1, age / 3) * Math.min(1, (MIRAGE_TICKS - age) / 12);
        if (fade <= 0) {
            return;
        }
        float h = payload.size();
        Vec3 c = payload.from().subtract(camera);
        Vec3 right = new Vec3(c.z, 0, -c.x);
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        float shimmer = 0.75F + 0.25F * Mth.sin(age * 0.9F);
        Vec3 sway = right.scale(0.04 * Mth.sin(age * 0.55F));
        int body = RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.35F);
        int core = RuneDrawing.mix(payload.color(), 0xFFFFFF, 0.75F);
        float alpha = 0.32F * fade * shimmer;
        float[] even = {0.85F, 1};
        for (int side = -1; side <= 1; side += 2) {
            // A leg and an arm on each side.
            limb(consumer, pose, eye, c.add(right.scale(side * 0.12)).add(0, 0.03, 0),
                    c.add(right.scale(side * 0.09)).add(0, h * 0.46, 0).add(sway.scale(0.5)), 0.17F, body, alpha);
            limb(consumer, pose, eye, c.add(right.scale(side * 0.3)).add(0, h * 0.42, 0).add(sway),
                    c.add(right.scale(side * 0.27)).add(0, h * 0.75, 0).add(sway), 0.12F, body, alpha);
        }
        Vec3[] torso = {c.add(0, h * 0.45, 0).add(sway.scale(0.6)), c.add(0, h * 0.78, 0).add(sway)};
        JarStreamFx.strip(consumer, pose, torso, even, eye, 0.44F, body, alpha);
        Vec3[] head = {c.add(0, h * 0.8, 0).add(sway), c.add(0, h * 0.99, 0).add(sway.scale(1.3))};
        JarStreamFx.strip(consumer, pose, head, even, eye, 0.34F, body, alpha * 1.1F);
        // A thin bright spine and a wide haze round the whole figure.
        Vec3[] spine = {c.add(0, h * 0.1, 0), c.add(0, h * 0.95, 0).add(sway)};
        JarStreamFx.strip(consumer, pose, spine, new float[]{0.3F, 1}, eye, 0.05F, core, 0.55F * fade * shimmer);
        JarStreamFx.strip(consumer, pose, new Vec3[]{c, c.add(0, h, 0).add(sway)}, new float[]{0.6F, 0.4F}, eye, 1.0F, payload.color(), 0.1F * fade);
        ring(consumer, pose, c.add(0, 0.03, 0), 0.45F + 0.05F * Mth.sin(age * 0.4F), age * 0.05F, 28, 0.1F, payload.color(), 0.4F * fade, eye);
    }

    private static void limb(VertexConsumer consumer, PoseStack.Pose pose, Vector3f eye, Vec3 from, Vec3 to, float width, int color, float alpha) {
        JarStreamFx.strip(consumer, pose, new Vec3[]{from, to}, new float[]{0.8F, 1}, eye, width, color, alpha);
    }

    /** A tongue of fire licking out of the jets: a red edge, an orange body and a white heart, all tapering to the tip. */
    private static void scorch(VertexConsumer consumer, PoseStack.Pose pose, WeaponFxPayload payload, float age, Vec3 camera, Vector3f eye, int seed) {
        float fade = 1 - age / SCORCH_TICKS;
        if (fade <= 0) {
            return;
        }
        Vec3 from = payload.from().subtract(camera);
        Vec3 way = payload.to().subtract(payload.from());
        Vec3 side = way.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        RandomSource lick = RandomSource.create(seed * 17L + (int) age);
        int n = 7;
        Vec3[] points = new Vec3[n];
        float[] light = new float[n];
        float[] heart = new float[n];
        for (int i = 0; i < n; i++) {
            float s = i / (float) (n - 1);
            // The tongue flutters more towards its tip.
            Vec3 flutter = side.scale((lick.nextDouble() - 0.5) * 0.3 * s).add(0, (lick.nextDouble() - 0.3) * 0.25 * s, 0);
            points[i] = from.add(way.scale(s * (0.85 + 0.15 * fade))).add(flutter);
            light[i] = 1 - s * 0.9F;
            heart[i] = Math.max(0, 1 - s * 2.2F);
        }
        float width = payload.size();
        JarStreamFx.strip(consumer, pose, points, light, eye, width, FIRE_EDGE, 0.3F * fade);
        JarStreamFx.strip(consumer, pose, points, light, eye, width * 0.5F, FIRE_BODY, 0.5F * fade);
        JarStreamFx.strip(consumer, pose, points, heart, eye, 0.12F, FIRE_HEART, 0.85F * fade);
    }

    /** A soft shaft of sunlight falling into the battery, swelling and fading, with a ring of light where it lands. */
    private static void sunbeam(VertexConsumer consumer, PoseStack.Pose pose, WeaponFxPayload payload, float age, Vec3 camera, Vector3f eye) {
        float glow = Mth.sin(Mth.PI * Math.min(1, age / SUNBEAM_TICKS));
        if (glow <= 0) {
            return;
        }
        Vec3 c = payload.from().subtract(camera);
        Vec3[] shaft = {c.add(0, payload.size(), 0), c.add(0, payload.size() * 0.5, 0), c};
        float[] light = {0, 0.6F, 1};
        JarStreamFx.strip(consumer, pose, shaft, light, eye, 0.55F, SUNLIGHT, 0.22F * glow);
        JarStreamFx.strip(consumer, pose, shaft, light, eye, 0.1F, RuneDrawing.mix(SUNLIGHT, 0xFFFFFF, 0.6F), 0.5F * glow);
        ring(consumer, pose, c.add(0, 0.02, 0), 0.32F + 0.04F * Mth.sin(age * 0.3F), 0, 28, 0.08F, payload.color(), 0.5F * glow, eye);
    }

    /** A hot beam from the spire's crystal to the creature: a white-gold core in an orange glow, flaring where it lands. */
    private static void beam(VertexConsumer consumer, PoseStack.Pose pose, WeaponFxPayload payload, float age, Vec3 camera, Vector3f eye) {
        float t = age / BEAM_TICKS;
        if (t >= 1) {
            return;
        }
        float fade = (1 - t) * (1 - t * 0.5F);
        Vec3 from = payload.from().subtract(camera);
        Vec3 to = payload.to().subtract(camera);
        Vec3[] line = {from, from.lerp(to, 0.5), to};
        float[] light = {0.8F, 1, 1};
        JarStreamFx.strip(consumer, pose, line, light, eye, 0.55F * (1 - 0.5F * t), FIRE_EDGE, 0.35F * fade);
        JarStreamFx.strip(consumer, pose, line, light, eye, 0.2F, FIRE_BODY, 0.65F * fade);
        JarStreamFx.strip(consumer, pose, line, light, eye, 0.05F, FIRE_HEART, fade);
        if (t < 0.5F) {
            float flare = 1 - t / 0.5F;
            Vec3[] spot = {to.add(0, -0.35 * flare, 0), to.add(0, 0.35 * flare, 0)};
            JarStreamFx.strip(consumer, pose, spot, new float[]{1, 1}, eye, 0.8F * flare, FIRE_BODY, 0.45F * flare);
            Vec3[] mouth = {from.add(0, -0.2 * flare, 0), from.add(0, 0.2 * flare, 0)};
            JarStreamFx.strip(consumer, pose, mouth, new float[]{1, 1}, eye, 0.45F * flare, FIRE_HEART, 0.5F * flare);
        }
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
