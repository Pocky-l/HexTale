package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HextechAltarBlockEntity;
import com.pockyl.hextale.block.PedestalBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.infusion.Ritual;

/**
 * The Hextech Altar: its centre item floats over the dish inside a hextech gimbal (two brass rings turning on crossed
 * axes) wrapped in a soft glow. Idle, everything turns slowly and see-through ghosts of the pedestal items the circle
 * still lacks float over the free pedestals. During a ritual the rings spin up, the glow swells, motes circle the item;
 * in the finale the item rises and a flash closes the ritual. A collapse shakes the altar and greys the glow for a moment.
 * <p>Under a ritual a hexagonal sigil burns on the ground ({@link #sigil}): the ignition sweeps round from the north and
 * lights a line to every pedestal, stele and jar of the circle, the hexagons turn faster as the work goes on, light runs
 * along the lines into the altar (brightest from the pedestal or jar being drawn now), a stall reddens it, a collapse
 * cracks it apart, the finish throws it outwards. Essence comes from the jars as living streams ({@link JarStreamFx}).
 */
public class HextechAltarRenderer implements BlockEntityRenderer<HextechAltarBlockEntity, HextechAltarRenderer.State> {
    private static final float FLASH_TICKS = 30;
    private static final float COLLAPSE_TICKS = 30;
    private static final int STALL_COLOR = 0xE0503C;
    private static final int ASH_COLOR = 0x8A7F78;
    /** Gold of the pedestals' lines in the ground sigil. */
    private static final int PEDESTAL_COLOR = 0xF0D27A;
    /** Corner radius of the sigil's hexagon round the altar's foot (see {@link State#inner}), and of the small ones under the members. */
    private static final float INNER = 1.25F;
    private static final float MEMBER_RING = 0.5F;
    private final ItemModelResolver itemModelResolver;

    public HextechAltarRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HextechAltarBlockEntity altar, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(altar, state, partialTick, cameraPosition, breakProgress);
        Level level = altar.getLevel();
        float time = HexFx.time(level, partialTick);
        int seed = (int) altar.getBlockPos().asLong();
        itemModelResolver.updateForTopItem(state.center, altar.center(), ItemDisplayContext.GROUND, level, null, seed);
        extractGhosts(altar, itemModelResolver, state.ghosts, state.extras, seed);
        state.time = time;
        state.active = altar.active();
        state.progress = altar.progress(partialTick);
        state.ritualTime = altar.ritualTime(partialTick);
        state.stalled = altar.stalled();
        state.finishing = altar.finishing();
        state.sigilY = altar.sigilY();
        state.itemY = (float) altar.itemY();
        state.sinceFinish = level == null ? -1 : level.getGameTime() - altar.finishedAt() + partialTick;
        state.sinceCollapse = level == null ? -1 : level.getGameTime() - altar.collapsedAt() + partialTick;
        // The rings keep their phase on the block entity: their speed follows the ritual smoothly.
        float target = state.active ? (state.stalled ? 2.5F : 4 + 14 * state.progress) : 0.8F;
        float[] rings = altar.animateRings(time, target);
        state.outerPhase = rings[0];
        state.innerPhase = rings[1];
        // The item's turn is integrated too: a change of speed never makes it jump.
        state.itemTurn = altar.animateItem(time, state.active ? 3 + 9 * state.progress : 1.6F);
        state.sigilPhase = altar.animateSigil(time, state.active ? (state.stalled ? 0.002F : 0.006F + 0.03F * state.progress) : 0.004F);
        extractCircle(altar, state, partialTick);
    }

    /** The circle's members for the ground sigil and the jars' streams, relative to the altar block. */
    private static void extractCircle(HextechAltarBlockEntity altar, State state, float partialTick) {
        state.members.clear();
        state.streams.clear();
        Level level = altar.getLevel();
        Ritual.Circle circle = altar.shownCircle();
        boolean shown = state.active || fade(state.sinceFinish, FLASH_TICKS) > 0 || fade(state.sinceCollapse, COLLAPSE_TICKS) > 0;
        if (level == null || circle == null || !shown) {
            return;
        }
        BlockPos origin = altar.getBlockPos();
        BlockPos drawing = altar.drawing();
        for (var pedestal : circle.pedestals()) {
            state.members.add(member(origin, pedestal.getBlockPos(), PEDESTAL_COLOR, pedestal.getBlockPos().equals(drawing)));
        }
        for (var stele : circle.steles()) {
            state.members.add(member(origin, stele.getBlockPos(), stele.rune() == null ? HexFx.HEXTECH : stele.rune().color(), false));
        }
        for (var tank : circle.tanks()) {
            var drawn = altar.streams().stream().filter(stream -> stream.end() < 0 && stream.jar().equals(tank.getBlockPos())).findFirst();
            state.members.add(member(origin, tank.getBlockPos(), drawn.map(stream -> stream.essence().color()).orElse(HexFx.HEXTECH),
                    drawn.isPresent()));
        }
        long now = level.getGameTime();
        Vec3 corner = Vec3.atLowerCornerOf(origin);
        Vec3 to = altar.itemPosition().subtract(corner);
        int seed = (int) origin.asLong();
        for (HextechAltarBlockEntity.Stream stream : altar.streams()) {
            state.streams.add(new JarStreamFx.Stream(JarReach.mouth(stream.jar()).subtract(corner), to, stream.essence().color(),
                    stream.head(now, partialTick), stream.tail(now, partialTick), seed + stream.jar().hashCode()));
        }
    }

    private static Member member(BlockPos origin, BlockPos at, int color, boolean busy) {
        return new Member(at.getX() - origin.getX(), at.getY() - origin.getY(), at.getZ() - origin.getZ(), color, busy);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        float flash = fade(state.sinceFinish, FLASH_TICKS);
        float collapse = fade(state.sinceCollapse, COLLAPSE_TICKS);
        float activity = state.active ? 0.5F + 0.5F * state.progress : 0;
        int color = state.stalled ? RuneDrawing.mix(HexFx.HEXTECH, STALL_COLOR, 0.5F + 0.5F * Mth.sin(time * 0.6F))
                : RuneDrawing.mix(HexFx.HEXTECH, 0xFFFFFF, flash * 0.6F);
        if (collapse > 0) {
            color = RuneDrawing.mix(color, ASH_COLOR, collapse);
        }
        float shake = collapse * 0.03F * Mth.sin(time * 3.3F);
        float rise = state.finishing ? 0.25F * Mth.clamp(state.progress * 12 - 11, 0, 1) : 0;
        float bob = Mth.sin(time * 0.08F) * 0.03F;

        poseStack.pushPose();
        poseStack.translate(0.5 + shake, state.itemY + bob + rise, 0.5);

        // Glow under and around the item.
        float glow = 0.35F + 0.5F * activity + 0.8F * flash;
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.75F + 0.35F * activity + 0.4F * flash, color, Math.min(0.85F, 0.25F * glow + 0.15F));

        // The gimbal around the item.
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.outerPhase));
        poseStack.pushPose();
        poseStack.translate(-0.5, -0.5, -0.5);
        TableModels.submit(poseStack, collector, TableModels.ALTAR_RING_OUTER, LightCoordsUtil.FULL_BRIGHT);
        poseStack.popPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(state.innerPhase));
        poseStack.translate(-0.5, -0.5, -0.5);
        TableModels.submit(poseStack, collector, TableModels.ALTAR_RING_INNER, LightCoordsUtil.FULL_BRIGHT);
        poseStack.popPose();

        // The centre item, turning; faster as the ritual goes on.
        if (!state.center.isEmpty()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(state.itemTurn));
            float pop = 1 + 0.25F * flash * Mth.sin(Math.min(1, state.sinceFinish / 8) * Mth.PI);
            poseStack.scale(0.9F * pop, 0.9F * pop, 0.9F * pop);
            poseStack.translate(0, -0.15, 0);
            state.center.submit(poseStack, collector, state.active || flash > 0 ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords,
                    OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (state.active || flash > 0) {
            motes(poseStack, collector, camera.orientation, state, color, activity + flash);
        }
        poseStack.popPose();

        ghosts(state.ghosts, state.extras, state.time, poseStack, collector, camera);
        submitSigil(state, poseStack, collector, camera);
    }

    /**
     * The ritual's ground sigil and jar streams for a state filled by {@link #extractSigil} - the Hexcore, a woken altar,
     * draws the same circle through these two.
     */
    public static void extractSigil(HextechAltarBlockEntity altar, State state, float partialTick) {
        Level level = altar.getLevel();
        state.blockPos = altar.getBlockPos();
        state.time = HexFx.time(level, partialTick);
        state.active = altar.active();
        state.progress = altar.progress(partialTick);
        state.ritualTime = altar.ritualTime(partialTick);
        state.stalled = altar.stalled();
        state.sinceFinish = level == null ? -1 : level.getGameTime() - altar.finishedAt() + partialTick;
        state.sinceCollapse = level == null ? -1 : level.getGameTime() - altar.collapsedAt() + partialTick;
        state.sigilPhase = altar.animateSigil(state.time, state.active ? (state.stalled ? 0.002F : 0.006F + 0.03F * state.progress) : 0.004F);
        extractCircle(altar, state, partialTick);
    }

    public static void submitSigil(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float flash = fade(state.sinceFinish, FLASH_TICKS);
        float collapse = fade(state.sinceCollapse, COLLAPSE_TICKS);
        int color = state.stalled ? RuneDrawing.mix(HexFx.HEXTECH, STALL_COLOR, 0.5F + 0.5F * Mth.sin(state.time * 0.6F))
                : RuneDrawing.mix(HexFx.HEXTECH, 0xFFFFFF, flash * 0.6F);
        if (collapse > 0) {
            color = RuneDrawing.mix(color, ASH_COLOR, collapse);
        }
        sigil(poseStack, collector, state, color, flash, collapse);
        if (!state.streams.isEmpty()) {
            Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
            for (JarStreamFx.Stream stream : state.streams) {
                JarStreamFx.submit(poseStack, collector, camera.orientation, eye, stream, state.time);
            }
        }
    }

    /** The ritual's ground sigil (see the class comment); hexagons lie flat, a hair over the altar's floor. */
    private static void sigil(PoseStack poseStack, SubmitNodeCollector collector, State state, int color, float flash, float collapse) {
        float alpha = state.active ? 0.55F + 0.35F * state.progress : Math.max(flash, collapse * 0.8F);
        if (alpha <= 0.01F || state.members.isEmpty() && !state.active) {
            return;
        }
        float time = state.time;
        float inner = state.inner;
        float lit = state.active ? Mth.clamp(state.ritualTime / Ritual.WARMUP_TICKS, 0, 1) : 1;
        float open = 1 - cube(1 - Math.min(1, (state.active ? state.ritualTime : 99) / 20F));
        float reach = 0;
        for (Member member : state.members) {
            reach = Math.max(reach, Mth.sqrt(member.dx * member.dx + member.dz * member.dz));
        }
        // After the finish the sigil is thrown outwards as it fades.
        float thrown = !state.active && flash > 0 ? 1 + 0.35F * (1 - flash) : 1;
        float outer = Mth.clamp(reach + 0.9F, 2.6F, 8.5F) * open * thrown;
        float crack = collapse > 0 && !state.active ? collapse : 0;
        int core = RuneDrawing.mix(color, 0xFFFFFF, 0.5F);
        float sweep = lit * Mth.TWO_PI;

        poseStack.pushPose();
        poseStack.translate(0.5, state.sigilY, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        // Two great hexagons turning against each other, and a tight one round the altar's foot.
        float flicker = crack > 0 ? 0.4F + 0.6F * Math.abs(Mth.sin(time * 2.3F)) : 1;
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation(state.sigilPhase * 0.4F));
        HexFx.hexRing(poseStack, collector, outer, color, 0.55F * alpha * flicker);
        poseStack.mulPose(Axis.ZP.rotation(-state.sigilPhase * 0.9F + Mth.PI / 6));
        HexFx.hexRing(poseStack, collector, outer * 0.86F, color, 0.3F * alpha * flicker);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation(state.sigilPhase * 1.6F));
        HexFx.hexRing(poseStack, collector, inner * Math.max(0.3F, open), core, 0.8F * alpha * flicker);
        poseStack.popPose();
        // The ignition: a hexagon running out over the ground and a bright ray sweeping round from the north.
        if (state.active && state.ritualTime < 24) {
            float k = state.ritualTime / 24;
            HexFx.hexRing(poseStack, collector, outer * (1 - cube(1 - k)) + 0.4F, core, 0.9F * (1 - k));
        }
        float rayAlpha = state.active && lit < 1 ? 0.9F : 0;
        // Lines to every member, lit when the sweep passes them; light running along them into the altar.
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            if (rayAlpha > 0) {
                float sx = Mth.sin(sweep);
                float sy = -Mth.cos(sweep);
                HexFx.glowSegment(consumer, pose, sx * inner, sy * inner, sx * outer, sy * outer, 0.3F, color, 0.35F * rayAlpha);
                HexFx.glowSegment(consumer, pose, sx * inner, sy * inner, sx * outer, sy * outer, 0.07F, core, rayAlpha);
            }
            int index = 0;
            for (Member member : state.members) {
                float distance = Mth.sqrt(member.dx * member.dx + member.dz * member.dz);
                float reveal = Mth.clamp((sweep - bearing(member)) / 0.5F, 0, 1);
                float broken = crack > 0 ? (Mth.sin(time * 1.7F + index * 2.9F) > 1 - 2 * crack ? 1 : 0.15F) : 1;
                float a = alpha * reveal * broken;
                index++;
                if (a <= 0.01F || distance < inner + MEMBER_RING + 0.2F) {
                    continue;
                }
                float ux = member.dx / distance;
                float uy = member.dz / distance;
                float x0 = ux * inner;
                float y0 = uy * inner;
                float x1 = member.dx - ux * MEMBER_RING;
                float y1 = member.dz - uy * MEMBER_RING;
                int lineColor = RuneDrawing.mix(member.color, color, 0.35F);
                int lineCore = RuneDrawing.mix(lineColor, 0xFFFFFF, 0.5F);
                HexFx.glowSegment(consumer, pose, x0, y0, x1, y1, 0.16F, lineColor, 0.22F * a);
                HexFx.glowSegment(consumer, pose, x0, y0, x1, y1, 0.035F, lineCore, 0.6F * a);
                if (state.active && !state.stalled && member.busy) {
                    // One focus (owner: not too much): light runs inwards only on the line being drawn now; the
                    // others glow still.
                    int pulses = 4;
                    float speed = 0.06F;
                    float length = Math.max(1, distance - inner - MEMBER_RING);
                    for (int k = 0; k < pulses; k++) {
                        float u = frac(time * speed + k / (float) pulses + index * 0.31F);
                        float front = 1 - u;
                        float back = Math.min(1, front + 0.35F / length);
                        HexFx.glowSegment(consumer, pose, x0 + (x1 - x0) * front, y0 + (y1 - y0) * front, x0 + (x1 - x0) * back,
                                y0 + (y1 - y0) * back, member.busy ? 0.16F : 0.11F, lineCore,
                                (member.busy ? 0.9F : 0.5F) * a * Mth.sin(u * Mth.PI));
                    }
                }
            }
        });
        // A small hexagon under every member, on its own floor.
        int index = 0;
        for (Member member : state.members) {
            float reveal = Mth.clamp((sweep - bearing(member)) / 0.5F, 0, 1);
            float broken = crack > 0 ? (Mth.sin(time * 1.3F + index * 4.1F) > 1 - 2 * crack ? 1 : 0.1F) : 1;
            float pulse = member.busy ? 0.75F + 0.25F * Mth.sin(time * 0.6F) : 0.6F;
            index++;
            poseStack.pushPose();
            poseStack.translate(member.dx, member.dz, -member.dy);
            poseStack.mulPose(Axis.ZP.rotation(-state.sigilPhase * 2 + index));
            HexFx.hexRing(poseStack, collector, MEMBER_RING * (member.busy ? 1.15F : 1), RuneDrawing.mix(member.color, color, 0.25F),
                    alpha * reveal * broken * pulse);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** The member's bearing from the altar, clockwise from the north (0 - 2 pi): the order the ignition reaches it in. */
    private static float bearing(Member member) {
        float angle = (float) Mth.atan2(member.dx, -member.dz);
        return angle < 0 ? angle + Mth.TWO_PI : angle;
    }

    private static float cube(float x) {
        return x * x * x;
    }

    private static float frac(float x) {
        return x - Mth.floor(x);
    }

    /** Fills the ghosts of the items the circle lacks and the pedestals holding something it does not take. */
    static void extractGhosts(HextechAltarBlockEntity altar, ItemModelResolver resolver, java.util.List<GhostState> ghosts,
                              java.util.List<Vec3> extras, int seed) {
        Level level = altar.getLevel();
        ghosts.clear();
        int index = 0;
        for (HextechAltarBlockEntity.Ghost ghost : altar.ghosts()) {
            ItemStackRenderState item = new ItemStackRenderState();
            resolver.updateForTopItem(item, ghost.item(), ItemDisplayContext.GROUND, level, null, seed + 2 + index++);
            ghosts.add(new GhostState(ghost.at(), item));
        }
        extras.clear();
        for (BlockPos pos : altar.extras()) {
            extras.add(Vec3.atLowerCornerOf(pos.subtract(altar.getBlockPos())).add(0.5, PedestalBlockEntity.ITEM_Y, 0.5));
        }
    }

    /**
     * The items the circle still lacks, as ghosts over the free pedestals: see-through, small, slowly turning, breathing
     * in and out inside a pale cyan haze; pedestals holding something the ritual does not take get a dull red haze.
     */
    static void ghosts(java.util.List<GhostState> ghostStates, java.util.List<Vec3> extras, float time, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        int i = 0;
        for (GhostState ghost : ghostStates) {
            float phase = time * 0.12F + i * 1.7F;
            float pulse = 0.5F + 0.5F * Mth.sin(phase);
            poseStack.pushPose();
            poseStack.translate(ghost.at.x, ghost.at.y + Mth.sin(time * 0.09F + i) * 0.04F, ghost.at.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.55F + 0.08F * pulse, 0x9FE6FF, 0.22F + 0.2F * pulse);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, 0.12F, 0xD8F6FF, 0.35F + 0.4F * pulse);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.5F + i * 40));
            float scale = 0.48F + 0.04F * pulse;
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0, -0.15, 0);
            // See-through and fully lit, breathing between faint and half visible.
            GhostItems.submit(ghost.item, poseStack, collector, LightCoordsUtil.FULL_BRIGHT, 0.3F + 0.25F * pulse);
            poseStack.popPose();
            i++;
        }
        for (Vec3 extra : extras) {
            poseStack.pushPose();
            poseStack.translate(extra.x, extra.y, extra.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.6F, STALL_COLOR, 0.25F + 0.12F * Mth.sin(time * 0.2F));
            poseStack.popPose();
        }
    }

    /** Sparkles on tilted orbits around the centre item. */
    private static void motes(PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera, State state, int color, float intensity) {
        int count = 9;
        float time = state.time;
        for (int i = 0; i < count; i++) {
            float angle = time * (0.06F + 0.1F * state.progress) * (1 + i * 0.11F) + i * Mth.TWO_PI / count;
            float tilt = i * 1.3F;
            float radius = 0.42F + 0.04F * Mth.sin(time * 0.09F + i);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * radius, Mth.sin(angle) * radius * Mth.sin(tilt) * 0.6F, Mth.sin(angle) * radius * Mth.cos(tilt));
            float twinkle = 0.6F + 0.4F * Mth.sin(time * 0.5F + i * 2.1F);
            HexFx.sprite(poseStack, collector, camera, HexFx.MOTE, 0.08F, RuneDrawing.lighten(color, 0.45F), Math.min(1, 0.6F * twinkle * intensity));
            poseStack.popPose();
        }
    }

    private static float fade(float since, float duration) {
        return since >= 0 && since < duration ? 1 - since / duration : 0;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(HextechAltarBlockEntity altar) {
        // Ghosts float over pedestals anywhere in the circle; the sigil and the streams reach its jars.
        return new AABB(altar.getBlockPos()).inflate(Ritual.TANK_RADIUS + 2, 3, Ritual.TANK_RADIUS + 2);
    }

    public static final class State extends BlockEntityRenderState {
        float sigilY = 0.03F;
        float itemY = (float) HextechAltarBlockEntity.ITEM_Y;
        final ItemStackRenderState center = new ItemStackRenderState();
        float time;
        boolean active;
        boolean stalled;
        boolean finishing;
        float progress;
        float ritualTime;
        float sinceFinish;
        float sinceCollapse;
        float outerPhase;
        float innerPhase;
        float itemTurn;
        float sigilPhase;
        /**
         * Corner radius of the hexagon round the foot, where the member lines start: the altar's {@link #INNER}; the
         * Hexcore's wider cradle sets its own.
         */
        float inner = INNER;
        final java.util.List<Member> members = new java.util.ArrayList<>();
        final java.util.List<JarStreamFx.Stream> streams = new java.util.ArrayList<>();
        final java.util.List<GhostState> ghosts = new java.util.ArrayList<>();
        final java.util.List<Vec3> extras = new java.util.ArrayList<>();
    }

    record GhostState(Vec3 at, ItemStackRenderState item) {
    }

    /** A pedestal, stele or jar of the circle, relative to the altar block; {@code busy} = being drawn now. */
    record Member(float dx, float dy, float dz, int color, boolean busy) {
    }
}
