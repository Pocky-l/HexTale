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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.pockyl.hextale.port.client.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.CrucibleBlockEntity;
import com.pockyl.hextale.client.GogglesView;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.pockyl.hextale.block.CrucibleBlockEntity.Phase;

/**
 * The water in the Crucible: tinted by the essence in it; in researcher's goggles every essence it holds floats over it
 * as an icon with its amount. It sways as it
 * heats; at a boil bubbles swell and break all over its surface. Essence poured in falls in a stream, blooms over the
 * surface and swirls into the middle; an item drawn in falls from where it was caught, then sinks, turning and
 * shrinking, while rings spread over the water. Boiling dry, the whole surface heaves as it sinks.
 */
public class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity, CrucibleRenderer.State> {
    private static final float MIN = CrucibleBlockEntity.INSIDE_MIN + 0.01F;
    private static final float MAX = CrucibleBlockEntity.INSIDE_MAX - 0.01F;
    private static final int WATER = 0x3F76E4;
    private static final ResourceLocation ICONS = Hextale.id("textures/gui/essence_icons.png");
    /** Essence pictograms on the sheet: 18 texels each, one every 20, 16 a row, sheet 512 x 128. */
    private static final float ICON_U = 18F / 512;
    private static final float ICON_V = 18F / 128;
    private static final int ICONS_PER_ROW = 16;
    /** The essences over the crucible: icon size and spacing (blocks), icons a row, how far they are seen. */
    private static final float ICON_SIZE = 0.2F;
    private static final float ICON_STEP = 0.3F;
    private static final float ROW_STEP = 0.34F;
    private static final int PER_ROW = 4;
    private static final int MAX_SHOWN = 12;
    private static final float SEEN_FROM = 9;
    private static final float FADE = 2;
    /** Ticks an essence that just grew pops and sends motes up from the water. */
    private static final float GROW_TICKS = 24;
    private static final float POUR_TICKS = 40;
    private static final float SINK_TICKS = 32;
    /** The first part of the sink animation: the item falls from where it was caught onto the water. */
    private static final float FALL_SHARE = 0.3F;
    private static final float STREAM_TICKS = 12;
    private static final int SWIRL_MOTES = 12;
    private static final int RIPPLE_BEADS = 14;

    private final ItemModelResolver itemModelResolver;

    public static final class State extends BlockEntityRenderState {
        float surface;
        boolean wet;
        int color;
        Phase phase = Phase.COLD;
        float time;
        long seed;
        float pourAge;
        int pourColor;
        float sinkAge;
        float sinkFrom;
        boolean drying;
        boolean brewed;
        final ItemStackRenderState sinking = new ItemStackRenderState();
        /** The essences in the water, most first, with their amounts and when each last grew (only seen in goggles). */
        final List<Essence> shown = new ArrayList<>();
        final List<Integer> amounts = new ArrayList<>();
        final List<Float> grewAge = new ArrayList<>();
    }

    private final Font font;

    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = ItemModelResolver.INSTANCE;
        font = context.getFont();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrucibleBlockEntity crucible, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(crucible, state, partialTick, cameraPosition, breakProgress);
        Level level = crucible.getLevel();
        state.time = level == null ? 0 : level.getGameTime() + partialTick;
        state.seed = crucible.getBlockPos().asLong();
        state.wet = crucible.liquid() > 0;
        state.surface = crucible.surface();
        state.phase = crucible.phase();
        int color = WATER;
        if (!crucible.essence().isEmpty()) {
            // Tinted by how much of the liquid is essence.
            float share = crucible.essence().total() / (float) Math.max(1, crucible.water() + crucible.essence().total());
            color = RuneDrawing.mix(color, crucible.essence().color(), 0.3F + 0.6F * Math.min(1, share * 4));
        }
        if (crucible.brew() != null) {
            // A brew colours the whole pot, more deeply the more doses are left.
            color = RuneDrawing.mix(color, crucible.brew().color(), 0.7F + 0.25F * crucible.doses() / (float) CrucibleBlockEntity.MAX_DOSES);
        }
        state.brewed = crucible.brew() != null;
        state.color = color;
        state.shown.clear();
        state.amounts.clear();
        state.grewAge.clear();
        if (!crucible.drying() && GogglesView.wearing(Minecraft.getInstance().player)) {
            Map<Essence, Integer> held = crucible.essence().asMap();
            held.entrySet().stream()
                    .sorted(Map.Entry.<Essence, Integer>comparingByValue().reversed().thenComparing(entry -> entry.getKey().ordinal()))
                    .limit(MAX_SHOWN)
                    .forEach(entry -> {
                        state.shown.add(entry.getKey());
                        state.amounts.add(entry.getValue());
                        state.grewAge.add(level == null ? GROW_TICKS : level.getGameTime() + partialTick - crucible.grewAt(entry.getKey()));
                    });
        }
        state.pourAge = state.time - crucible.pouredAt();
        state.pourColor = crucible.pouredColor();
        state.sinkAge = state.time - crucible.sankAt();
        state.sinkFrom = crucible.sankFrom();
        state.drying = crucible.drying();
        if (state.sinkAge < SINK_TICKS && !crucible.sank().isEmpty()) {
            itemModelResolver.updateForTopItem(state.sinking, crucible.sank(), ItemDisplayContext.FIXED, level, null, (int) state.seed);
        } else {
            state.sinking.clear();
        }
    }

    @Override
    public AABB getRenderBoundingBox(CrucibleBlockEntity crucible) {
        BlockPos pos = crucible.getBlockPos();
        return new AABB(pos).expandTowards(0, 2.5, 0).inflate(0.6, 0, 0.6);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.wet) {
            return;
        }
        essences(state, poseStack, collector, camera);
        float stir = switch (state.phase) {
            case COLD -> 0;
            case WARMING -> 0.05F;
            case SIMMERING -> 0.12F;
            case BOILING -> 0.25F;
        };
        float top = state.surface + Mth.sin(state.time * 0.35F) * stir / 16;
        int bubbles = state.drying ? 16 : switch (state.phase) {
            case COLD -> 0;
            case WARMING -> 2;
            case SIMMERING -> 4;
            case BOILING -> 9;
        } + (state.brewed ? 5 : 0);
        LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, CrucibleBlockEntity.BOTTOM, top, RuneDrawing.argb(state.color, 0.86F),
                state.time, state.seed, bubbles);
        if (state.drying || state.phase == Phase.BOILING || state.phase == Phase.SIMMERING) {
            rollingSurface(state, poseStack, collector, top, state.drying ? 20 : state.phase == Phase.BOILING ? 11 : 4);
        }
        if (state.pourAge >= 0 && state.pourAge < POUR_TICKS) {
            pour(state, poseStack, collector, top);
            swirl(state, poseStack, collector, top);
        }
        if (state.sinkAge >= 0 && state.sinkAge < SINK_TICKS) {
            sink(state, poseStack, collector, top);
        }
    }

    /**
     * What the water holds, floating over the crucible for a researcher in goggles: each essence's pictogram in a soft
     * glow of its colour, brightly tinted, with its amount under it, in rows of {@value #PER_ROW} turned to the viewer. An essence
     * that just grew swells for a moment while motes of its colour rise to it from the water. Seen from up to
     * {@value #SEEN_FROM} blocks.
     */
    private void essences(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.shown.isEmpty()) {
            return;
        }
        Vec3 centre = Vec3.atLowerCornerOf(state.blockPos).add(0.5, 1.3, 0.5);
        float distance = (float) camera.pos.distanceTo(centre);
        float fade = Mth.clamp((SEEN_FROM - distance) / FADE, 0, 1);
        if (fade <= 0) {
            return;
        }
        int count = state.shown.size();
        int rows = (count + PER_ROW - 1) / PER_ROW;
        float bob = Mth.sin(state.time * 0.06F) * 0.025F;
        Quaternionf facing = new Quaternionf();
        for (int i = 0; i < count; i++) {
            Essence essence = state.shown.get(i);
            int row = i / PER_ROW;
            int inRow = Math.min(PER_ROW, count - row * PER_ROW);
            float x = ((i % PER_ROW) - (inRow - 1) / 2F) * ICON_STEP;
            float y = 1.25F + (rows - 1 - row) * ROW_STEP + bob + Mth.sin(state.time * 0.08F + i * 1.7F) * 0.01F;
            float age = state.grewAge.get(i);
            float pop = age >= 0 && age < GROW_TICKS ? Mth.sin(age / GROW_TICKS * Mth.PI) : 0;
            int color = RuneDrawing.lighten(essence.color(), 0.15F);
            poseStack.pushPose();
            poseStack.translate(0.5F, 0, 0.5F);
            if (pop > 0) {
                rising(state, poseStack, collector, camera, age, color, y, x, i);
            }
            poseStack.translate(0, y, 0);
            poseStack.mulPose(camera.orientation);
            poseStack.translate(x, 0, 0);
            float size = ICON_SIZE * (1 + 0.35F * pop);
            HexFx.sprite(poseStack, collector, facing, HexFx.HALO, size * (1.9F + pop), color, (0.45F + 0.4F * pop) * fade);
            icon(poseStack, collector, essence, size, color, fade);
            amount(poseStack, collector, state.amounts.get(i), color, fade, size);
            poseStack.popPose();
        }
    }

    /** The essence's pictogram from the GUI sheet on a quad in the pose's plane (the pose already faces the camera). */
    private static void icon(PoseStack poseStack, SubmitNodeCollector collector, Essence essence, float size, int color, float alpha) {
        float h = size / 2;
        float u0 = essence.ordinal() % ICONS_PER_ROW * 20F / 512;
        float v0 = essence.ordinal() / ICONS_PER_ROW * 20F / 128;
        float u1 = u0 + ICON_U;
        float v1 = v0 + ICON_V;
        int tint = RuneDrawing.lighten(color, 0.5F);
        collector.submitCustomGeometry(poseStack, HexFx.glow(ICONS), (pose, consumer) -> {
            HexFx.vertex(consumer, pose, -h, -h, 0.002F, u0, v1, tint, alpha);
            HexFx.vertex(consumer, pose, h, -h, 0.002F, u1, v1, tint, alpha);
            HexFx.vertex(consumer, pose, h, h, 0.002F, u1, v0, tint, alpha);
            HexFx.vertex(consumer, pose, -h, h, 0.002F, u0, v0, tint, alpha);
            HexFx.vertex(consumer, pose, -h, h, 0.002F, u0, v0, tint, alpha);
            HexFx.vertex(consumer, pose, h, h, 0.002F, u1, v0, tint, alpha);
            HexFx.vertex(consumer, pose, h, -h, 0.002F, u1, v1, tint, alpha);
            HexFx.vertex(consumer, pose, -h, -h, 0.002F, u0, v1, tint, alpha);
        });
    }

    /** The amount in mB under the icon, outlined so it reads over anything. */
    private void amount(PoseStack poseStack, SubmitNodeCollector collector, int amount, int color, float alpha, float size) {
        String text = String.valueOf(amount);
        poseStack.pushPose();
        poseStack.translate(0, -size / 2 - 0.04F, 0.004F);
        poseStack.scale(0.0105F, -0.0105F, 0.0105F);
        float x = -font.width(text) / 2F;
        float a = Math.max(0.1F, alpha);
        collector.submitText(poseStack, x, 0, FormattedCharSequence.forward(text, Style.EMPTY), false, Font.DisplayMode.NORMAL,
                LightTexture.FULL_BRIGHT, RuneDrawing.argb(RuneDrawing.lighten(color, 0.55F), a), 0, RuneDrawing.argb(0x0A0812, a));
        poseStack.popPose();
    }

    /** Motes of an essence that just grew, rising from the water to its icon. */
    private static void rising(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, float age,
                               int color, float iconY, float iconX, int index) {
        float t = age / GROW_TICKS;
        // The icon's place in the world: along the camera's right from the column's middle.
        Vector3f right = new Vector3f(1, 0, 0).rotate(camera.orientation).mul(iconX);
        for (int m = 0; m < 5; m++) {
            float p = Mth.clamp(t * 1.6F - m * 0.12F, 0, 1);
            if (p <= 0 || p >= 1) {
                continue;
            }
            float ease = p * p * (3 - 2 * p);
            float angle = index * 2.1F + m * 1.3F + p * 4;
            float spread = (1 - ease) * 0.18F;
            poseStack.pushPose();
            poseStack.translate(right.x * ease + Mth.cos(angle) * spread, Mth.lerp(ease, state.surface, iconY), right.z * ease + Mth.sin(angle) * spread);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, 0.06F, color, 0.9F * (1 - p * 0.5F));
            poseStack.popPose();
        }
    }

    /** Bubbles swelling on the surface and bursting, each in its own place and time. */
    private static void rollingSurface(State state, PoseStack poseStack, SubmitNodeCollector collector, float top, int count) {
        for (int i = 0; i < count; i++) {
            float period = 14 + Math.floorMod(state.seed * 7 + i * 13, 11);
            float phase = (state.time + i * 5.3F) / period;
            int cycle = (int) Math.floor(phase);
            float t = phase - cycle;
            long h = state.seed * 6364136223846793005L + i * 1442695040888963407L + cycle * 2862933555777941L;
            float fx = ((h >>> 20) & 1023) / 1023F;
            float fz = ((h >>> 40) & 1023) / 1023F;
            float half = (0.8F + 0.7F * ((h >>> 8) & 7) / 7F) / 16;
            float x = Mth.lerp(fx, MIN + 1.5F / 16, MAX - 1.5F / 16);
            float z = Mth.lerp(fz, MIN + 1.5F / 16, MAX - 1.5F / 16);
            poseStack.pushPose();
            poseStack.translate(x, top, z);
            LiquidBox.surfaceBubble(poseStack, collector, t, 0.75F, half, state.color, 0.85F);
            poseStack.popPose();
        }
    }

    /**
     * Essence just poured in: a stream of it falls from over the rim into the middle, thinning as the bottle empties,
     * and a bloom of its colour spreads over the surface and fades into the water.
     */
    private static void pour(State state, PoseStack poseStack, SubmitNodeCollector collector, float top) {
        float t = state.pourAge / POUR_TICKS;
        int color = RuneDrawing.mix(state.pourColor, 0xFFFFFF, 0.15F);
        if (state.pourAge < STREAM_TICKS) {
            float half = 0.6F / 16 * (1 - state.pourAge / STREAM_TICKS * 0.7F);
            // The head of the stream reaches the water in three ticks; it keeps falling from over the rim.
            float head = top + 0.95F * (1 - Math.min(1, state.pourAge / 3));
            LiquidBox.submit(poseStack, collector, 0.5F - half, 0.5F - half, 0.5F + half, 0.5F + half, head, top + 0.95F,
                    RuneDrawing.argb(color, 0.85F));
        }
        float reach = Math.min(0.5F - MIN, 0.04F + (0.5F - MIN) * Math.min(1, t * 2.2F));
        LiquidBox.submit(poseStack, collector, 0.5F - reach, 0.5F - reach, 0.5F + reach, 0.5F + reach, top - 0.1F / 16, top + 0.04F / 16,
                RuneDrawing.argb(color, 0.55F * (1 - t)));
    }

    /** Essence just poured in: beads of its colour spiral into the middle and fade into the water. */
    private static void swirl(State state, PoseStack poseStack, SubmitNodeCollector collector, float top) {
        float t = state.pourAge / POUR_TICKS;
        float size = 0.45F / 16 * (1 - t * 0.5F);
        int color = RuneDrawing.mix(state.pourColor, 0xFFFFFF, 0.25F);
        for (int i = 0; i < SWIRL_MOTES; i++) {
            float along = i / (float) SWIRL_MOTES;
            float angle = state.pourAge * 0.32F + along * Mth.TWO_PI * 1.5F;
            float radius = (1 - t) * (0.06F + 0.24F * along);
            float x = 0.5F + Mth.cos(angle) * radius;
            float z = 0.5F + Mth.sin(angle) * radius;
            LiquidBox.submit(poseStack, collector, x - size, z - size, x + size, z + size, top - 0.2F / 16, top + 0.12F / 16,
                    RuneDrawing.argb(color, 0.9F * (1 - t)));
        }
    }

    /**
     * An item drawn in: it falls from where it was caught onto the water, then turns, sinks and shrinks away while two
     * rings spread from where it fell.
     */
    private void sink(State state, PoseStack poseStack, SubmitNodeCollector collector, float top) {
        float age = state.sinkAge / SINK_TICKS;
        float fall = Math.min(1, age / FALL_SHARE);
        // The rest of the animation runs from the moment it hits the water.
        float t = Math.max(0, (age - FALL_SHARE) / (1 - FALL_SHARE));
        if (!state.sinking.isEmpty()) {
            float y = fall < 1 ? Mth.lerp(fall * fall, Math.max(state.sinkFrom, top + 0.12F), top + 0.12F) : top + 0.12F - t * 0.4F;
            poseStack.pushPose();
            poseStack.translate(0.5F, y, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.time * 9));
            poseStack.mulPose(Axis.XP.rotationDegrees(25 + t * 40));
            float scale = 0.32F * (1 - t * 0.7F);
            poseStack.scale(scale, scale, scale);
            state.sinking.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (fall < 1) {
            return;
        }
        int ring = RuneDrawing.mix(state.color, 0xFFFFFF, 0.6F);
        float bead = 0.3F / 16;
        for (int wave = 0; wave < 2; wave++) {
            float w = t * 1.3F - wave * 0.3F;
            if (w <= 0 || w >= 1) {
                continue;
            }
            float radius = 0.04F + w * 0.27F;
            for (int i = 0; i < RIPPLE_BEADS; i++) {
                float angle = i * Mth.TWO_PI / RIPPLE_BEADS;
                float x = 0.5F + Mth.cos(angle) * radius;
                float z = 0.5F + Mth.sin(angle) * radius;
                if (x - bead < MIN || x + bead > MAX || z - bead < MIN || z + bead > MAX) {
                    continue;
                }
                LiquidBox.submit(poseStack, collector, x - bead, z - bead, x + bead, z + bead, top - 0.05F / 16, top + 0.1F / 16,
                        RuneDrawing.argb(ring, 0.7F * (1 - w)));
            }
        }
    }
}
