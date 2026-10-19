package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.CrucibleBlockEntity;
import com.pockyl.hextale.block.CrucibleBlockEntity.Phase;

/**
 * The water in the Crucible: tinted by the essence poured into it and clouded by what dissolved in it. It sways as it
 * heats; at a boil bubbles swell and break all over its surface. Essence poured in falls in a stream, blooms over the
 * surface and swirls into the middle; an item drawn in falls from where it was caught, then sinks, turning and
 * shrinking, while rings spread over the water. Boiling dry, the whole surface heaves as it sinks.
 */
public class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity, CrucibleRenderer.State> {
    private static final float MIN = CrucibleBlockEntity.INSIDE_MIN + 0.01F;
    private static final float MAX = CrucibleBlockEntity.INSIDE_MAX - 0.01F;
    private static final int WATER = 0x3F76E4;
    /** What dissolved in the water clouds it towards this murky green. */
    private static final int MURK = 0x55602E;
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
        final ItemStackRenderState sinking = new ItemStackRenderState();
    }

    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
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
        state.wet = crucible.water() > 0;
        state.surface = crucible.surface();
        state.phase = crucible.phase();
        int color = WATER;
        if (!crucible.essence().isEmpty()) {
            // Tinted by how much of the liquid is essence.
            float share = crucible.essence().total() / (float) Math.max(1, crucible.water() + crucible.essence().total());
            color = RuneDrawing.mix(color, crucible.essence().color(), 0.3F + 0.6F * Math.min(1, share * 4));
        }
        state.color = RuneDrawing.mix(color, MURK, Math.min(0.55F, crucible.dissolvedCount() / 40F));
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
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.wet) {
            return;
        }
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
        };
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

    /** Bubbles swelling on the surface and breaking, each in its own place and time. */
    private static void rollingSurface(State state, PoseStack poseStack, SubmitNodeCollector collector, float top, int count) {
        int light = RuneDrawing.mix(state.color, 0xFFFFFF, 0.45F);
        for (int i = 0; i < count; i++) {
            float period = 14 + Math.floorMod(state.seed * 7 + i * 13, 11);
            float phase = (state.time + i * 5.3F) / period;
            int cycle = (int) Math.floor(phase);
            float t = phase - cycle;
            long h = state.seed * 6364136223846793005L + i * 1442695040888963407L + cycle * 2862933555777941L;
            float fx = ((h >>> 20) & 1023) / 1023F;
            float fz = ((h >>> 40) & 1023) / 1023F;
            float radius = (0.35F + 0.65F * ((h >>> 8) & 7) / 7F) * Mth.sin(t * Mth.PI) * 0.9F / 16;
            if (radius < 0.08F / 16) {
                continue;
            }
            float x = Mth.lerp(fx, MIN + 1F / 16, MAX - 1F / 16);
            float z = Mth.lerp(fz, MIN + 1F / 16, MAX - 1F / 16);
            LiquidBox.submit(poseStack, collector, x - radius, z - radius, x + radius, z + radius, top - radius * 0.3F, top + radius * 0.7F,
                    RuneDrawing.argb(light, 0.75F * (1 - t * 0.6F)));
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
            poseStack.rotate(Axis.YP.rotationDegrees(state.time * 9));
            poseStack.rotate(Axis.XP.rotationDegrees(25 + t * 40));
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
