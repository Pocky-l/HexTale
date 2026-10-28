package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.network.RuneFxPayload;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.ArrayList;
import java.util.List;

/**
 * Rune signs in the world, from {@link RuneFxPayload}, in the stele's light ({@link HexGlow}, additive).
 * <p>A proc follows the three beats of a hit: in the first quarter a flat hexagon snaps open round the spot and the
 * rune's glyph writes itself and flares white-hot over it (the peak), then the hexagon turns and fades while a fainter
 * one keeps running outwards and the glyph drifts up and dies away.
 * <p>A learned rune writes itself stroke by stroke in the air before its reader inside an upright hexagon, holds there
 * breathing for a moment, then shrinks and flies into the reader's chest with a last flash.
 * <p>An area opens a flat hexagon on the ground out to a device's reach (and a smaller one turning against it), the
 * rune's glyph rising over the middle, and fades. A ripple is a small hexagon flaring on a barrier's face where
 * something pushed against it. A sigil is a small flat hexagon snapping open round a device's moment and fading, a
 * fainter one running on outwards.
 */
public final class RuneFx {
    private static final int PROC_TICKS = 22;
    /** The proc's peak: the hexagon is open and the glyph written. */
    private static final float PROC_PEAK = 4;
    private static final int WRITE_TICKS = 28;
    private static final int HOLD_TICKS = 12;
    private static final int FLY_TICKS = 16;
    private static final int LEARN_TICKS = WRITE_TICKS + HOLD_TICKS + FLY_TICKS;
    private static final int MAX_SIGNS = 96;
    private static final int AREA_TICKS = 60;
    private static final int RIPPLE_TICKS = 12;
    private static final int SIGIL_TICKS = 20;

    private static final List<Sign> SIGNS = new ArrayList<>();
    private static @Nullable ClientLevel level;

    record Sign(RuneFxPayload payload, long start, float phase) {
        int length() {
            return switch (payload.kind()) {
                case RuneFxPayload.LEARN -> LEARN_TICKS;
                case RuneFxPayload.AREA -> AREA_TICKS;
                case RuneFxPayload.RIPPLE -> RIPPLE_TICKS;
                case RuneFxPayload.SIGIL -> SIGIL_TICKS;
                default -> PROC_TICKS;
            };
        }

        int color() {
            return payload.color() == RuneFxPayload.RUNE_COLOR ? payload.rune().color() : payload.color();
        }
    }

    private RuneFx() {
    }

    public static void accept(RuneFxPayload payload) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null) {
            return;
        }
        if (current != level) {
            SIGNS.clear();
            level = current;
        }
        if (SIGNS.size() >= MAX_SIGNS) {
            SIGNS.remove(0);
        }
        SIGNS.add(new Sign(payload, current.getGameTime(), current.getRandom().nextFloat() * Mth.TWO_PI));
    }

    public static void submit(SubmitCustomGeometryEvent event) {
        if (SIGNS.isEmpty()) {
            return;
        }
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null || current != level) {
            SIGNS.clear();
            return;
        }
        LevelRenderState state = event.getLevelRenderState();
        CameraRenderState camera = state.cameraRenderState;
        float now = state.gameTime + state.worldPartialTicks;
        SIGNS.removeIf(sign -> now - sign.start() > sign.length() || now < sign.start() - 1);
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (Sign sign : SIGNS) {
            float age = Math.max(0, now - sign.start());
            switch (sign.payload().kind()) {
                case RuneFxPayload.LEARN -> learn(poseStack, collector, camera, current, sign, age, state.worldPartialTicks);
                case RuneFxPayload.AREA -> area(poseStack, collector, camera, sign, age);
                case RuneFxPayload.RIPPLE -> ripple(poseStack, collector, camera, sign, age);
                case RuneFxPayload.SIGIL -> sigil(poseStack, collector, camera, sign, age);
                default -> proc(poseStack, collector, camera, sign, age);
            }
        }
    }

    private static void proc(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, Sign sign, float age) {
        RuneFxPayload payload = sign.payload();
        ComplexRune rune = payload.rune();
        int color = sign.color();
        float size = payload.size();
        float strength = payload.strength();
        com.pockyl.hextale.ClientConfig.RuneSigns shown = com.pockyl.hextale.ClientConfig.RUNE_SIGNS.get();
        if (shown == com.pockyl.hextale.ClientConfig.RuneSigns.NONE) {
            return;
        }
        if (onMe(payload.at())) {
            // A rune on the viewer's own tools or clothes: a smaller, fainter sign (or none, as set).
            if (shown == com.pockyl.hextale.ClientConfig.RuneSigns.NOT_MINE) {
                return;
            }
            size *= 0.6F;
            strength *= 0.55F;
        }
        float t = age / PROC_TICKS;
        float open = 1 - cube(1 - Math.min(1, age / PROC_PEAK));
        float fade = age < PROC_PEAK ? 1 : 1 - Mth.square((age - PROC_PEAK) / (PROC_TICKS - PROC_PEAK));
        Vec3 rel = payload.at().subtract(camera.pos);
        poseStack.pushPose();
        poseStack.translate(rel.x, rel.y, rel.z);
        // The flat hexagon round the spot: snaps open, flares at the peak, turns slowly as it fades.
        poseStack.pushPose();
        poseStack.rotate(Axis.XP.rotationDegrees(90));
        poseStack.rotate(Axis.ZP.rotation(sign.phase() + age * 0.04F));
        float flare = 1 + 0.8F * Math.max(0, 1 - Math.abs(age - PROC_PEAK) / PROC_PEAK);
        HexFx.hexRing(poseStack, collector, size * (0.45F + 0.55F * open), color, 0.7F * strength * fade * flare);
        // The leading edge: a fainter hexagon running on outwards.
        HexFx.hexRing(poseStack, collector, size * (0.5F + 1.1F * (1 - cube(1 - t))), color, 0.35F * strength * Mth.square(1 - t));
        poseStack.popPose();
        // The glyph over it, upright and turned to the viewer; skipped right in a first-person viewer's face.
        double flat = rel.x * rel.x + rel.z * rel.z;
        if (!(camera.isFirstPerson && flat < 1.5)) {
            poseStack.translate(0, size * 0.25F + 0.3F * t, 0);
            poseStack.rotate(Axis.YP.rotation((float) Mth.atan2(-rel.x, -rel.z)));
            float glyph = age < PROC_PEAK ? 0.6F + 0.4F * age / PROC_PEAK : fade;
            HexFx.glyph(poseStack, collector, rune, size * 0.75F, color, Math.min(1, strength * 1.2F) * glyph,
                    Math.min(1, age / (PROC_PEAK - 1)));
        }
        poseStack.popPose();
    }

    private static void learn(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, ClientLevel current,
                              Sign sign, float age, float partialTick) {
        RuneFxPayload payload = sign.payload();
        ComplexRune rune = payload.rune();
        int color = sign.color();
        float size = payload.size();
        float strength = payload.strength();
        Vec3 at = payload.at();
        float scale = 1;
        float alpha = 1;
        float flight = Math.max(0, (age - WRITE_TICKS - HOLD_TICKS) / FLY_TICKS);
        if (flight > 0) {
            Entity reader = current.getEntity(payload.follow());
            Vec3 chest = reader == null ? at : reader.getPosition(partialTick).add(0, reader.getBbHeight() * 0.6, 0);
            float ease = flight * flight * (3 - 2 * flight);
            at = at.add(chest.subtract(at).scale(ease)).add(0, Mth.sin(flight * Mth.PI) * 0.35, 0);
            scale = 1 - 0.8F * ease;
            alpha = flight > 0.85F ? 1 + 2 * (flight - 0.85F) / 0.15F : 1;
        }
        float write = Math.min(1, age / WRITE_TICKS);
        float breathe = age > WRITE_TICKS && flight == 0 ? 0.85F + 0.15F * Mth.sin((age - WRITE_TICKS) * 0.5F) : 1;
        Vec3 rel = at.subtract(camera.pos);
        poseStack.pushPose();
        poseStack.translate(rel.x, rel.y, rel.z);
        poseStack.rotate(Axis.YP.rotation((float) Mth.atan2(-rel.x, -rel.z)));
        // The hexagon draws itself first, the glyph writes inside it.
        poseStack.pushPose();
        poseStack.rotate(Axis.ZP.rotation(sign.phase() + age * 0.03F));
        float ring = Math.min(1, age / 8F);
        HexFx.hexRing(poseStack, collector, size * scale * (0.7F + 0.3F * (1 - cube(1 - ring))), color, 0.6F * strength * ring * alpha * breathe);
        poseStack.popPose();
        HexFx.glyph(poseStack, collector, rune, size * 1.1F * scale, color, Math.min(1, strength * alpha * breathe),
                write * write * (3 - 2 * write));
        poseStack.popPose();
    }

    private static void area(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, Sign sign, float age) {
        RuneFxPayload payload = sign.payload();
        int color = sign.color();
        float open = 1 - cube(1 - Math.min(1, age / 18F));
        float fade = age < 30 ? 1 : 1 - (age - 30) / (AREA_TICKS - 30);
        Vec3 rel = payload.at().subtract(camera.pos);
        poseStack.pushPose();
        poseStack.translate(rel.x, rel.y + 0.05, rel.z);
        poseStack.pushPose();
        poseStack.rotate(Axis.XP.rotationDegrees(90));
        poseStack.rotate(Axis.ZP.rotation(sign.phase() + age * 0.01F));
        HexFx.hexRing(poseStack, collector, payload.size() * (0.1F + 0.9F * open), color, 0.8F * fade);
        poseStack.rotate(Axis.ZP.rotation(-age * 0.025F + Mth.PI / 6));
        HexFx.hexRing(poseStack, collector, payload.size() * (0.1F + 0.75F * open), color, 0.35F * fade);
        poseStack.popPose();
        // The rune of the device rising over the middle.
        float rise = Math.min(1, age / 20F);
        poseStack.translate(0, 0.8 + 0.5 * rise, 0);
        poseStack.rotate(Axis.YP.rotation((float) Mth.atan2(-rel.x, -rel.z)));
        HexFx.glyph(poseStack, collector, payload.rune(), 0.7F, color, 0.9F * fade, rise);
        poseStack.popPose();
    }

    private static void ripple(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, Sign sign, float age) {
        RuneFxPayload payload = sign.payload();
        float k = age / RIPPLE_TICKS;
        net.minecraft.core.Direction face = net.minecraft.core.Direction.from3DDataValue(payload.follow());
        Vec3 rel = payload.at().subtract(camera.pos);
        poseStack.pushPose();
        poseStack.translate(rel.x, rel.y, rel.z);
        poseStack.rotate(face.getRotation());
        poseStack.rotate(Axis.XP.rotationDegrees(90));
        poseStack.rotate(Axis.ZP.rotation(sign.phase()));
        HexFx.hexRing(poseStack, collector, payload.size() * (0.3F + 0.7F * (1 - cube(1 - k))), sign.color(), 0.9F * (1 - k));
        poseStack.popPose();
    }

    private static void sigil(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, Sign sign, float age) {
        RuneFxPayload payload = sign.payload();
        float t = age / SIGIL_TICKS;
        float open = 1 - cube(1 - Math.min(1, age / 4F));
        float fade = age < 4 ? 1 : 1 - Mth.square((age - 4) / (SIGIL_TICKS - 4));
        float flare = 1 + 0.7F * Math.max(0, 1 - Math.abs(age - 3) / 3);
        Vec3 rel = payload.at().subtract(camera.pos);
        poseStack.pushPose();
        poseStack.translate(rel.x, rel.y, rel.z);
        poseStack.rotate(Axis.XP.rotationDegrees(90));
        poseStack.rotate(Axis.ZP.rotation(sign.phase() + age * 0.05F));
        HexFx.hexRing(poseStack, collector, payload.size() * (0.4F + 0.6F * open), sign.color(), 0.75F * fade * flare);
        HexFx.hexRing(poseStack, collector, payload.size() * (0.5F + 0.9F * (1 - cube(1 - t))), sign.color(), 0.35F * Mth.square(1 - t));
        poseStack.popPose();
    }

    /** Whether {@code at} lies on the local player's own body (a proc of their tools or clothes). */
    private static boolean onMe(Vec3 at) {
        net.minecraft.world.entity.player.Player me = Minecraft.getInstance().player;
        if (me == null) {
            return false;
        }
        double dx = at.x - me.getX();
        double dz = at.z - me.getZ();
        double dy = at.y - me.getY();
        return dx * dx + dz * dz < 1.0 && dy > -0.5 && dy < me.getBbHeight() + 0.5;
    }

    /** Client only: shows {@code payload} here without a message from the server (a barrier's ripple). */
    public static void local(RuneFxPayload payload) {
        accept(payload);
    }

    private static float cube(float x) {
        return x * x * x;
    }
}
