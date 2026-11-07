package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.thevoid.boss.Surface;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the fight marks on the world, from one word of the server per mark ({@link ProsectorMarkerPayload}): the
 * lines, hexagons and circles on the floor, drawn in additive light that rises as the blow nears, the flash when it lands,
 * the glyph at a dull hit, the great ring of a turn, the shake of a slam. The fight goes on over any ground, so a mark
 * that warns of a blow is laid over the blocks point by point ({@link Surface}) when it arrives and never sinks into a hill.
 * All of it follows the player's screen effects setting where it moves the view.
 */
public final class ProsectorFx {
    private static final float GROUND = 0.07F;
    private static final float SHAKE_RANGE = 30;
    private static final float SHAKE_DEGREES = 1.8F;
    private static final double HALF_ARC = Math.toRadians(70);
    /** Blocks between two points of a mark laid over the ground. */
    private static final double STEP = 0.5;
    /** Ticks a wound takes to fade at the end of its life. */
    private static final float WOUND_FADE = 20;
    private static final List<Mark> MARKS = new ArrayList<>();

    private ProsectorFx() {
    }

    /** {@code laid}: the lines of the mark over the ground as it was when the mark came, relative to where it stands. */
    private record Mark(ProsectorMarkerPayload word, long start, RandomSource random, List<List<Vector3f>> laid) {
        Marker kind() {
            return word.marker();
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ProsectorFx::tick);
        NeoForge.EVENT_BUS.addListener(ProsectorFx::submit);
        NeoForge.EVENT_BUS.addListener(ProsectorFx::shake);
    }

    public static void accept(ProsectorMarkerPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (payload.marker() == Marker.HEAL) {
            MARKS.removeIf(mark -> mark.kind() == Marker.WOUND);
            return;
        }
        MARKS.add(new Mark(payload, level.getGameTime(), RandomSource.create(), lay(level, payload)));
    }

    private static List<List<Vector3f>> lay(ClientLevel level, ProsectorMarkerPayload word) {
        float size = word.size();
        Vec3 at = word.a();
        return switch (word.marker()) {
            case LINE, CUT -> List.of(laid(level, at, new double[][]{{0, 0}, {word.b().x - at.x, word.b().z - at.z}}));
            case HEX, RING -> List.of(laid(level, at, hexagon(size)));
            case CIRCLE, WOUND -> List.of(laid(level, at, hexagon(size)), laid(level, at, hexagon(size * 0.7F)));
            case ARC -> List.of(laid(level, at, arc(word)));
            default -> List.of();
        };
    }

    private static double[][] hexagon(float radius) {
        double[][] corners = new double[7][];
        for (int i = 0; i <= 6; i++) {
            double a = Mth.TWO_PI * i / 6;
            corners[i] = new double[]{Math.cos(a) * radius, Math.sin(a) * radius};
        }
        return corners;
    }

    /** The outline of a slice: out from the middle, round, and back. */
    private static double[][] arc(ProsectorMarkerPayload word) {
        double facing = Math.atan2(word.b().z - word.a().z, word.b().x - word.a().x);
        int steps = 14;
        double[][] points = new double[steps + 3][];
        points[0] = new double[]{0, 0};
        for (int i = 0; i <= steps; i++) {
            double a = facing - HALF_ARC + 2 * HALF_ARC * i / steps;
            points[i + 1] = new double[]{Math.cos(a) * word.size(), Math.sin(a) * word.size()};
        }
        points[steps + 2] = new double[]{0, 0};
        return points;
    }

    /** A line through these (dx, dz) offsets, cut into short steps, each on the ground that is there. */
    private static List<Vector3f> laid(ClientLevel level, Vec3 at, double[][] offsets) {
        List<Vector3f> points = new ArrayList<>();
        for (int i = 0; i + 1 < offsets.length; i++) {
            double dx = offsets[i + 1][0] - offsets[i][0];
            double dz = offsets[i + 1][1] - offsets[i][1];
            int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz) / STEP));
            for (int step = 0; step < steps; step++) {
                points.add(onGround(level, at, offsets[i][0] + dx * step / steps, offsets[i][1] + dz * step / steps));
            }
        }
        double[] last = offsets[offsets.length - 1];
        points.add(onGround(level, at, last[0], last[1]));
        return points;
    }

    private static Vector3f onGround(ClientLevel level, Vec3 at, double dx, double dz) {
        return new Vector3f((float) dx, (float) (Surface.height(level, at.x + dx, at.z + dz, at.y) - at.y), (float) dz);
    }

    private static void tick(ClientTickEvent.Post event) {
        if (MARKS.isEmpty()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            MARKS.clear();
            return;
        }
        MARKS.removeIf(mark -> {
            int age = (int) (level.getGameTime() - mark.start());
            if (age < 0 || age > mark.word().ticks()) {
                return true;
            }
            particles(level, mark, age);
            return false;
        });
    }

    private static void particles(ClientLevel level, Mark mark, int age) {
        ProsectorMarkerPayload word = mark.word();
        Vec3 at = word.a();
        RandomSource random = mark.random();
        int color = word.color();
        switch (mark.kind()) {
            case ERUPT -> {
                for (int i = 0; i < 6; i++) {
                    double a = random.nextDouble() * Mth.TWO_PI;
                    double r = Math.sqrt(random.nextDouble()) * word.size();
                    level.addParticle(ModParticles.flame(color), at.x + Math.cos(a) * r, at.y + 0.1, at.z + Math.sin(a) * r, 0, 0.25 + random.nextDouble() * 0.2, 0);
                }
            }
            case SLAM -> {
                if (age == 0) {
                    level.addParticle(ModParticles.ring(color), at.x, at.y + 0.1, at.z, word.size() * 2.5, 1, 0);
                    for (int i = 0; i < 30; i++) {
                        double a = random.nextDouble() * Mth.TWO_PI;
                        level.addParticle(ModParticles.vapour(ModParticles.SMOKE), at.x + Math.cos(a) * word.size(), at.y + 0.2, at.z + Math.sin(a) * word.size(),
                                Math.cos(a) * 0.15, 0.05, Math.sin(a) * 0.15);
                    }
                }
            }
            case CUT -> {
                if (age == 0) {
                    Vec3 way = word.b().subtract(at);
                    for (int i = 0; i < 40; i++) {
                        Vec3 p = at.add(way.scale(random.nextDouble()));
                        level.addParticle(ModParticles.spark(color), p.x, p.y + 0.1, p.z, 0, 0.2 + random.nextDouble() * 0.2, 0);
                    }
                }
            }
            case BREAK -> {
                if (age == 0) {
                    for (int i = 0; i < 24; i++) {
                        level.addParticle(ModParticles.drop(color), at.x, at.y + 0.6, at.z, (random.nextDouble() - 0.5) * 0.5, random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.5);
                    }
                    level.addParticle(ModParticles.ring(color), at.x, at.y + 0.6, at.z, word.size(), 0, 0);
                }
            }
            case WAY -> {
                Vec3 way = word.b().subtract(at);
                Vec3 p = at.add(way.scale(age / (double) Math.max(1, word.ticks())));
                for (int i = 0; i < 3; i++) {
                    level.addParticle(ModParticles.glint(color), p.x, p.y, p.z, (random.nextDouble() - 0.5) * 0.05, -0.02, (random.nextDouble() - 0.5) * 0.05);
                }
            }
            case WOUND -> {
                int count = age == 0 ? 14 : age % 4 == 0 ? 1 : 0;
                for (int i = 0; i < count; i++) {
                    double a = random.nextDouble() * Mth.TWO_PI;
                    double r = Math.sqrt(random.nextDouble()) * word.size() * 0.8;
                    double x = at.x + Math.cos(a) * r;
                    double z = at.z + Math.sin(a) * r;
                    double y = Surface.height(level, x, z, at.y) + 0.1;
                    level.addParticle(age == 0 ? ModParticles.spark(color) : ModParticles.voidMote(color), x, y, z, 0, 0.1 + random.nextDouble() * 0.15, 0);
                }
            }
            default -> {
            }
        }
    }

    private static void submit(SubmitCustomGeometryEvent event) {
        if (MARKS.isEmpty()) {
            return;
        }
        LevelRenderState state = event.getLevelRenderState();
        CameraRenderState camera = state.cameraRenderState;
        float now = state.gameTime + net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Quaternionfc facing = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        PoseStack pose = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (Mark mark : MARKS) {
            float age = now - mark.start();
            float life = Mth.clamp(age / Math.max(1, mark.word().ticks()), 0, 1);
            Vec3 at = mark.word().a().subtract(camera.pos);
            pose.pushPose();
            pose.translate(at.x, at.y + GROUND, at.z);
            draw(mark, pose, collector, facing, age, life);
            pose.popPose();
        }
    }

    private static void draw(Mark mark, PoseStack pose, SubmitNodeCollector collector, Quaternionfc facing, float age, float life) {
        ProsectorMarkerPayload word = mark.word();
        int color = word.color();
        float size = word.size();
        // Warning marks brighten toward the blow, with a beat that quickens: the last moment reads without looking at a timer.
        float warn = (0.35F + 0.65F * life) * (0.8F + 0.2F * Mth.sin(age * (0.5F + life)));
        switch (mark.kind()) {
            case LINE -> laid(mark, pose, collector, size, 0.18F, color, warn);
            case CUT -> laid(mark, pose, collector, size * 2, 0.18F, 0xFFFFFF, 1 - life);
            case HEX -> laid(mark, pose, collector, outline(size), 0.3F, color, warn);
            case CIRCLE -> laid(mark, pose, collector, outline(size), 0.3F, color, warn);
            case RING -> {
                laid(mark, pose, collector, outline(size), 0.3F, color, warn);
                flat(pose, () -> HexFx.sigilRing(pose, collector, size * life, 0, color, 0.5F));
            }
            case ARC -> laid(mark, pose, collector, 0.5F, 0.3F, color, warn);
            case WOUND -> {
                float left = (1 - life) * mark.word().ticks();
                float glow = Mth.clamp(left / WOUND_FADE, 0, 1) * (0.75F + 0.25F * Mth.sin(age * 0.2F));
                laid(mark, pose, collector, 0.5F, 0.3F, color, glow);
            }
            case ERUPT -> flat(pose, () -> HexFx.hexRing(pose, collector, size, color, 1 - life));
            case SLAM -> flat(pose, () -> HexFx.hexRing(pose, collector, size * (1 + life), color, 1 - life));
            case TURN -> flat(pose, () -> HexFx.hexRing(pose, collector, size * (0.3F + 1.2F * life), color, 1 - life));
            case GLYPH -> {
                pose.mulPose(facing);
                HexFx.sigilRing(pose, collector, size * (0.6F + 0.4F * life), 6, color, 1 - life);
            }
            default -> {
            }
        }
    }

    /** Lays what {@code draw} does flat on the floor. */
    private static void flat(PoseStack pose, Runnable draw) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(90));
        draw.run();
        pose.popPose();
    }

    /** How wide the glow round a hexagon's line is: it grows with the hexagon only up to a small one. */
    private static float outline(float radius) {
        return Math.min(radius, 1.2F) * 0.16F;
    }

    /** The mark's lines over the ground: a soft glow {@code width} wide and a near-white core {@code core} of that. */
    private static void laid(Mark mark, PoseStack pose, SubmitNodeCollector collector, float width, float core, int color, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        int bright = RuneDrawing.mix(color, 0xFFFFFF, 0.6F);
        collector.submitCustomGeometry(pose, HexGlow.TYPE, (matrix, consumer) -> {
            for (List<Vector3f> line : mark.laid()) {
                for (int i = 0; i + 1 < line.size(); i++) {
                    HexFx.glowRibbon(consumer, matrix, line.get(i), line.get(i + 1), width, color, alpha * 0.5F);
                    HexFx.glowRibbon(consumer, matrix, line.get(i), line.get(i + 1), width * core, bright, alpha);
                }
            }
        });
    }

    private static void shake(ViewportEvent.ComputeCameraAngles event) {
        if (MARKS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float now = minecraft.level.getGameTime() + (float) event.getPartialTick();
        Vec3 view = event.getCamera().position();
        float strength = 0;
        for (Mark mark : MARKS) {
            Marker kind = mark.kind();
            if (kind != Marker.SHAKE && kind != Marker.SLAM) {
                continue;
            }
            float left = 1 - (now - mark.start()) / Math.max(1, mark.word().ticks());
            float near = 1 - Mth.clamp((float) mark.word().a().distanceTo(view) / SHAKE_RANGE, 0, 1);
            float scale = kind == Marker.SLAM ? 1 : mark.word().size();
            if (left > 0) {
                strength = Math.max(strength, near * near * left * left * scale);
            }
        }
        if (strength <= 0.001F) {
            return;
        }
        float degrees = strength * SHAKE_DEGREES * minecraft.options.screenEffectScale().get().floatValue();
        event.setPitch(event.getPitch() + Mth.sin(now * 2.7F) * degrees);
        event.setYaw(event.getYaw() + Mth.sin(now * 3.3F + 1) * degrees * 0.7F);
        event.setRoll(event.getRoll() + Mth.sin(now * 2.1F + 2) * degrees * 0.6F);
    }
}
