package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.ItemModelResolver;
import com.pockyl.hextale.port.client.ItemStackRenderState;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexcoreBlockEntity;
import com.pockyl.hextale.block.HexcoreMatrix;
import com.pockyl.hextale.infusion.Ritual;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hexcore: its rune matrix floats over the cradle and turns slowly; now and then a layer of tiles twists a quarter
 * turn like a puzzle cube. With an item inside, the tiles part so it shows between them. During a ritual the matrix
 * rises, its tiles twist faster and faster and stand off from the crystal, arcs crackle out of it over the circle and
 * strikes the server sends land as heavy bolts. Born from an altar, the matrix assembles itself: the crystal swells out
 * of a flash and the tiles come spiralling in from all sides.
 */
public class HexcoreRenderer implements BlockEntityRenderer<HexcoreBlockEntity, HexcoreRenderer.State> {
    private static final int TILES = HexcoreMatrix.TILES.length;
    private static final float FLASH_TICKS = 30;
    private static final int STALL_COLOR = 0xE0503C;
    private static final int BOLT_COLOR = 0x8FD8FF;
    private final ItemModelResolver itemModelResolver;

    public HexcoreRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = ItemModelResolver.INSTANCE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HexcoreBlockEntity core, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(core, state, partialTick, cameraPosition, breakProgress);
        Level level = core.getLevel();
        float time = HexFx.time(level, partialTick);
        long now = level == null ? 0 : level.getGameTime();
        int seed = (int) core.getBlockPos().asLong();
        state.time = time;
        state.active = core.active();
        state.progress = core.progress(partialTick);
        state.stalled = core.stalled();
        state.finishing = core.finishing();
        state.anger = core.anger();
        state.sinceFinish = now - core.finishedAt() + partialTick;
        state.sinceBirth = core.bornAt() == Long.MIN_VALUE ? -1 : now - core.bornAt() + partialTick;
        state.rise = core.animateRise(time);
        itemModelResolver.updateForTopItem(state.center, core.center(), ItemDisplayContext.GROUND, level, null, seed);
        HextechAltarRenderer.extractGhosts(core, itemModelResolver, state.ghosts, state.extras, seed);
        // The woken altar keeps its circle: the same ground sigil and jar streams as the altar's.
        HextechAltarRenderer.extractSigil(core, state.circle, partialTick);

        // Spin and tumble follow the ritual smoothly (the altar's ring phases, reused).
        float target = state.active ? (state.stalled ? 1.5F : 2.5F + 9 * state.progress) : 0.7F;
        float[] phases = core.animateRings(time, target);
        state.spin = phases[0];
        state.tumble = phases[1];
        state.itemTurn = core.animateItem(time, state.active ? 3 + 9 * state.progress : 1.4F);

        // The puzzle: rare slow twists at rest, quick ones in a ritual, frantic ones when it stalls; none while it assembles.
        if (level != null && !(state.sinceBirth >= 0 && state.sinceBirth < HexcoreBlockEntity.BIRTH_TICKS)) {
            float interval;
            float duration;
            if (!state.active) {
                interval = 110;
                duration = 16;
            } else if (state.stalled) {
                interval = 5;
                duration = 5;
            } else {
                interval = Mth.lerp(state.progress, 16, 2);
                duration = Mth.lerp(state.progress, 11, 5);
            }
            core.matrix().update(time, interval, duration, level.getRandom());
        }
        for (int i = 0; i < TILES; i++) {
            state.tiles[i] = core.matrix().pose(i, time);
        }

        state.arcs.clear();
        Vec3 origin = Vec3.atLowerCornerOf(core.getBlockPos());
        for (HexcoreBlockEntity.Arc arc : core.arcs()) {
            float age = now - arc.born() + partialTick;
            state.arcs.add(new ArcState(arc.to().subtract(origin), age / arc.life(), arc.seed(), arc.strong()));
        }
        state.eye = cameraPosition.subtract(origin);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        HextechAltarRenderer.submitSigil(state.circle, poseStack, collector, camera);
        float time = state.time;
        float flash = state.sinceFinish >= 0 && state.sinceFinish < FLASH_TICKS ? 1 - state.sinceFinish / FLASH_TICKS : 0;
        float birth = state.sinceBirth >= 0 && state.sinceBirth < HexcoreBlockEntity.BIRTH_TICKS ? state.sinceBirth : -1;
        float activity = state.active ? 0.4F + 0.6F * state.progress : 0;
        int color = state.stalled ? RuneDrawing.mix(HexFx.HEXTECH, STALL_COLOR, 0.5F + 0.5F * Mth.sin(time * 0.6F)) : HexFx.HEXTECH;
        // Tiles stand off from the crystal: to show an item inside, and more as a ritual breathes; they close in the finale.
        float open = state.center.isEmpty() ? 0 : 2.2F;
        if (state.active) {
            open = 1.6F + 1.2F * state.progress + 0.5F * Mth.sin(time * 0.35F);
            if (state.finishing) {
                open *= 0.4F;
            }
        }
        float shake = state.stalled ? 0.025F * Mth.sin(time * 2.7F) : 0.03F * state.anger * state.anger * activity * Mth.sin(time * 4.1F);
        float centreY = state.rise + Mth.sin(time * 0.045F) * 0.04F;

        poseStack.pushPose();
        poseStack.translate(0.5 + shake, centreY, 0.5 - shake);
        float swell = birth >= 0 ? Mth.clamp((birth - 60) / 20, 0, 1) * (1 - Mth.clamp((birth - 80) / 10, 0, 1)) : 0;
        float breath = 0.5F + 0.5F * Mth.sin(time * 0.06F);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.9F + 0.15F * breath + 0.5F * activity + 1.2F * flash + 1.5F * swell,
                RuneDrawing.mix(color, 0xFFFFFF, 0.3F + 0.5F * flash), 0.9F);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 2.4F + 0.3F * breath + 1.2F * activity + 2.5F * swell,
                RuneDrawing.mix(0x5FA8F0, color, 0.5F), 0.16F + 0.08F * breath + 0.2F * activity + 0.4F * swell);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(14 + Mth.sin(state.tumble * Mth.DEG_TO_RAD) * (10 + 20 * activity)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.009F) * 12));
        Vector3f offset = new Vector3f();
        for (int i = 0; i < TILES; i++) {
            float away = open;
            float swirl = 0;
            if (birth >= 0) {
                // Tile i comes in from far off, spiralling, after its own delay.
                float u = Mth.clamp((birth - 14 - (i % 13) * 3 - (i / 13) * 2) / 24, 0, 1);
                if (u <= 0) {
                    continue;
                }
                float eased = 1 - (1 - u) * (1 - u) * (1 - u);
                away = open + (1 - eased) * 30;
                swirl = (1 - eased) * 220;
            }
            poseStack.pushPose();
            if (swirl != 0) {
                poseStack.mulPose(Axis.YP.rotationDegrees(swirl));
            }
            poseStack.mulPose(state.tiles[i]);
            HexcoreMatrix.NORMALS[i].mul(away / 16, offset);
            poseStack.translate(offset.x, offset.y, offset.z);
            poseStack.translate(-0.5, -0.5, -0.5);
            TableModels.submit(poseStack, collector, TableModels.HEXCORE_TILES.get(i), LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
        if (state.center.isEmpty()) {
            float grow = birth >= 0 ? Mth.clamp(birth / 18, 0, 1) : 1;
            float pulse = grow * (1 + 0.06F * Mth.sin(time * 0.2F) + 0.12F * activity * Mth.sin(time * 0.5F));
            poseStack.pushPose();
            poseStack.scale(pulse, pulse, pulse);
            poseStack.translate(-0.5, -0.5, -0.5);
            TableModels.submit(poseStack, collector, TableModels.HEXCORE_CRYSTAL, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
        poseStack.popPose();

        // The item held in the heart of the matrix.
        if (!state.center.isEmpty()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(state.itemTurn));
            poseStack.scale(0.6F, 0.6F, 0.6F);
            poseStack.translate(0, -0.15, 0);
            state.center.submit(poseStack, collector, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.popPose();

        Vec3 centre = new Vec3(0.5, centreY, 0.5);
        Vector3f eye = new Vector3f((float) state.eye.x, (float) state.eye.y, (float) state.eye.z);
        for (ArcState arc : state.arcs) {
            bolt(poseStack, collector, centre, arc, eye, time);
            if (arc.strong && arc.age >= 0 && arc.age <= 1) {
                // A flash where it lands, fading with the bolt.
                poseStack.pushPose();
                poseStack.translate(arc.to.x, arc.to.y, arc.to.z);
                HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 2.4F * (1 - 0.6F * arc.age), 0xDFF6FF, 0.9F * (1 - arc.age));
                HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.9F, 0xFFFFFF, 1 - arc.age);
                poseStack.popPose();
            }
        }
        HextechAltarRenderer.ghosts(state.ghosts, state.extras, time, poseStack, collector, camera);
    }

    /**
     * A jagged bolt from the matrix to the arc's target that re-forks a few times a second: a wide faint glow and a
     * white-hot core, with one branch off its middle. Strong strikes are thicker and brighter.
     */
    private static void bolt(PoseStack poseStack, SubmitNodeCollector collector, Vec3 centre, ArcState arc, Vector3f eye, float time) {
        if (arc.age < 0 || arc.age > 1) {
            return;
        }
        Vec3 dir = arc.to.subtract(centre);
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 from = centre.add(dir.normalize().scale(0.6));
        float fade = arc.strong ? 1 - arc.age * arc.age : 1 - arc.age;
        int flicker = (int) (time * 0.7F);
        if (arc.strong) {
            // Like real lightning it flashes, dims and flashes again on the same path.
            float strobe = (int) (arc.age * 16) % 5 < 3 ? 1 : 0.35F;
            fade *= strobe;
            flicker = (int) (arc.age * 3);
        }
        Vec3[] main = jagged(from, arc.to, arc.seed * 131 + flicker, arc.strong ? 0.22F : 0.14F);
        Vec3 forkFrom = main[main.length / 2];
        Vec3 forkTo = forkFrom.add(arc.to.subtract(from).scale(0.35)).add(noise(arc.seed + 7, flicker).scale(0.6));
        Vec3[] fork = jagged(forkFrom, forkTo, arc.seed * 17 + flicker, 0.1F);
        Vec3 fork2From = main[main.length * 3 / 4];
        Vec3[] fork2 = arc.strong ? jagged(fork2From, fork2From.add(noise(arc.seed + 11, flicker).scale(0.8)), arc.seed * 23 + flicker, 0.12F) : null;
        float wide = arc.strong ? 0.55F : 0.12F;
        float core = arc.strong ? 0.16F : 0.035F;
        int glow = arc.strong ? RuneDrawing.mix(BOLT_COLOR, 0xFFFFFF, 0.2F) : BOLT_COLOR;
        float shown = fade;
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            JarStreamFx.strip(consumer, pose, main, ends(main.length), eye, wide, glow, 0.35F * shown);
            JarStreamFx.strip(consumer, pose, main, ends(main.length), eye, core, 0xF4FCFF, 0.95F * shown);
            JarStreamFx.strip(consumer, pose, fork, ends(fork.length), eye, wide * 0.6F, glow, 0.25F * shown);
            JarStreamFx.strip(consumer, pose, fork, ends(fork.length), eye, core * 0.6F, 0xF4FCFF, 0.7F * shown);
            if (fork2 != null) {
                JarStreamFx.strip(consumer, pose, fork2, ends(fork2.length), eye, wide * 0.5F, glow, 0.25F * shown);
                JarStreamFx.strip(consumer, pose, fork2, ends(fork2.length), eye, core * 0.5F, 0xF4FCFF, 0.7F * shown);
            }
        });
    }

    private static Vec3[] jagged(Vec3 from, Vec3 to, int seed, float amplitude) {
        double length = to.subtract(from).length();
        int segments = Math.max(5, (int) Math.round(length * 5));
        Vec3[] points = new Vec3[segments + 1];
        for (int k = 0; k <= segments; k++) {
            float t = k / (float) segments;
            Vec3 at = from.lerp(to, t);
            if (k > 0 && k < segments) {
                at = at.add(noise(seed, k).scale(amplitude * Math.sqrt(length) * Mth.sin(t * Mth.PI)));
            }
            points[k] = at;
        }
        return points;
    }

    /** A pseudo-random vector in the unit cube centred on 0, the same for the same seed and index. */
    private static Vec3 noise(int seed, int index) {
        int h = seed * 0x9E3779B1 + index * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return new Vec3((h & 255) / 255.0 - 0.5, ((h >>> 8) & 255) / 255.0 - 0.5, ((h >>> 16) & 255) / 255.0 - 0.5).scale(2);
    }

    /** Full light along a bolt, dimmer at its two ends. */
    private static float[] ends(int count) {
        float[] light = new float[count];
        for (int i = 0; i < count; i++) {
            light[i] = i == 0 || i == count - 1 ? 0.5F : 1;
        }
        return light;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(HexcoreBlockEntity core) {
        // Ghosts and arcs reach anywhere in the circle; the matrix rises over two blocks.
        return new AABB(core.getBlockPos()).inflate(Ritual.TANK_RADIUS + 2, 3, Ritual.TANK_RADIUS + 2).expandTowards(0, 1, 0);
    }

    record ArcState(Vec3 to, float age, int seed, boolean strong) {
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState center = new ItemStackRenderState();
        /** The ground sigil and jar streams, drawn by the altar's renderer. */
        final HextechAltarRenderer.State circle = new HextechAltarRenderer.State();
        final Quaternionf[] tiles = new Quaternionf[TILES];
        final List<ArcState> arcs = new ArrayList<>();
        final List<HextechAltarRenderer.GhostState> ghosts = new ArrayList<>();
        final List<Vec3> extras = new ArrayList<>();
        Vec3 eye = Vec3.ZERO;
        float time;
        boolean active;
        boolean stalled;
        boolean finishing;
        float progress;
        float anger;
        float sinceFinish;
        float sinceBirth;
        float rise;
        float spin;
        float tumble;
        float itemTurn;
    }
}
