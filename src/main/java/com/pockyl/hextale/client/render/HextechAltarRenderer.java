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
import com.pockyl.hextale.infusion.Ritual;

/**
 * The Hextech Altar: its centre item floats over the dish inside a hextech gimbal (two brass rings turning on crossed
 * axes) wrapped in a soft glow. Idle, everything turns slowly and a small ghost of what a ritual would make hovers
 * above. During a ritual the rings spin up, the glow swells, motes circle the item; in the finale the item rises and a
 * flash closes the ritual. A collapse shakes the altar and greys the glow for a moment.
 */
public class HextechAltarRenderer implements BlockEntityRenderer<HextechAltarBlockEntity, HextechAltarRenderer.State> {
    private static final float FLASH_TICKS = 30;
    private static final float COLLAPSE_TICKS = 30;
    private static final int STALL_COLOR = 0xE0503C;
    private static final int ASH_COLOR = 0x8A7F78;
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
        float time = level == null ? 0 : level.getGameTime() + partialTick;
        int seed = (int) altar.getBlockPos().asLong();
        itemModelResolver.updateForTopItem(state.center, altar.center(), ItemDisplayContext.GROUND, level, null, seed);
        ItemStack preview = altar.preview();
        boolean showPreview = !altar.active() && !preview.isEmpty() && !ItemStack.isSameItemSameComponents(preview, altar.center());
        itemModelResolver.updateForTopItem(state.preview, showPreview ? preview : ItemStack.EMPTY, ItemDisplayContext.GROUND, level, null, seed + 1);
        state.time = time;
        state.active = altar.active();
        state.charging = altar.kind() == Ritual.Kind.CHARGE;
        state.progress = altar.progress(partialTick);
        state.ritualTime = altar.ritualTime(partialTick);
        state.stalled = altar.stalled();
        state.finishing = altar.finishing();
        state.sinceFinish = time - altar.finishedAt();
        state.sinceCollapse = time - altar.collapsedAt();
        // The rings keep their phase on the block entity: their speed follows the ritual smoothly.
        float target = state.active ? (state.stalled ? 2.5F : 4 + 14 * state.progress) : 0.8F;
        float[] rings = altar.animateRings(time, target);
        state.outerPhase = rings[0];
        state.innerPhase = rings[1];
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
        poseStack.translate(0.5 + shake, HextechAltarBlockEntity.ITEM_Y + bob + rise, 0.5);

        // Glow under and around the item.
        float glow = 0.35F + 0.5F * activity + 0.8F * flash;
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.75F + 0.35F * activity + 0.4F * flash, color, Math.min(0.85F, 0.25F * glow + 0.15F));

        // The gimbal around the item.
        poseStack.pushPose();
        poseStack.rotate(Axis.YP.rotationDegrees(state.outerPhase));
        poseStack.pushPose();
        poseStack.translate(-0.5, -0.5, -0.5);
        TableModels.submit(poseStack, collector, TableModels.ALTAR_RING_OUTER, LightCoordsUtil.FULL_BRIGHT);
        poseStack.popPose();
        poseStack.rotate(Axis.XP.rotationDegrees(state.innerPhase));
        poseStack.translate(-0.5, -0.5, -0.5);
        TableModels.submit(poseStack, collector, TableModels.ALTAR_RING_INNER, LightCoordsUtil.FULL_BRIGHT);
        poseStack.popPose();

        // The centre item, turning; faster as the ritual goes on.
        if (!state.center.isEmpty()) {
            poseStack.pushPose();
            float turn = state.active ? state.ritualTime * (3 + 9 * state.progress) : time * 1.6F;
            poseStack.rotate(Axis.YP.rotationDegrees(turn));
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

        // What a ritual would make: a small ghost over the gimbal.
        if (!state.preview.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5, HextechAltarBlockEntity.ITEM_Y + 0.72 + Mth.sin(time * 0.1F) * 0.04F, 0.5);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.42F, 0xF0D27A, 0.35F + 0.1F * Mth.sin(time * 0.15F));
            poseStack.rotate(Axis.YP.rotationDegrees(-time * 2.4F));
            poseStack.scale(0.45F, 0.45F, 0.45F);
            poseStack.translate(0, -0.15, 0);
            state.preview.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    /** Sparkles on tilted orbits around the centre item. */
    private static void motes(PoseStack poseStack, SubmitNodeCollector collector, Quaternionfc camera, State state, int color, float intensity) {
        int count = state.charging ? 5 : 9;
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
        return new AABB(altar.getBlockPos()).inflate(1, 2, 1);
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState center = new ItemStackRenderState();
        final ItemStackRenderState preview = new ItemStackRenderState();
        float time;
        boolean active;
        boolean charging;
        boolean stalled;
        boolean finishing;
        float progress;
        float ritualTime;
        float sinceFinish;
        float sinceCollapse;
        float outerPhase;
        float innerPhase;
    }
}
