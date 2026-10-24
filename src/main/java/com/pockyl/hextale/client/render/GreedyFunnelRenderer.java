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
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.GreedyFunnelBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.FunnelTimeline;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

/**
 * The Greedy Funnel at work. A slow vortex of shadow turns in its mouth - faster while it draws a death in, hardly at
 * all when it is hungry - under a hextech crystal that glows greener the more experience it holds. Every swallowed death
 * is drawn from its timeline ({@link FunnelTimeline}): the drops are plucked up where the creature fell and pulled
 * into the bowl, the experience follows as motes into the crystal. Drawing decay out of a jar shows the jar's stream.
 */
public class GreedyFunnelRenderer implements BlockEntityRenderer<GreedyFunnelBlockEntity, GreedyFunnelRenderer.State> {
    private static final int VORTEX_MOTES = 7;
    private final ItemModelResolver itemModelResolver;

    public GreedyFunnelRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GreedyFunnelBlockEntity funnel, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(funnel, state, partialTick, cameraPosition, breakProgress);
        Level level = funnel.getLevel();
        BlockPos pos = funnel.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        int seed = (int) pos.asLong();
        state.time = HexFx.time(level, partialTick) + (seed & 127);
        state.vortex = Mth.lerp(partialTick, funnel.vortexBefore, funnel.vortex);
        state.hungry = funnel.hungry();
        state.full = funnel.full();
        state.filled = Math.min(1, funnel.experience() / (float) GreedyFunnelBlockEntity.EXPERIENCE_CAP);
        state.color = PickaxeRuneTint.blend(funnel.runes());
        state.crystalAt = GreedyFunnelBlockEntity.crystal(pos).subtract(origin);
        state.mouthAt = GreedyFunnelBlockEntity.mouth(pos).subtract(origin);
        itemModelResolver.updateForTopItem(state.crystal, new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), ItemDisplayContext.GROUND, level,
                null, seed);
        float now = level == null ? 0 : level.getGameTime() + partialTick;

        state.stream = null;
        BlockPos jar = funnel.drawing();
        if (jar != null) {
            float age = now - funnel.drewAt();
            state.stream = new JarStreamFx.Stream(JarReach.mouth(jar).subtract(origin), state.crystalAt, Essence.DECAY.color(),
                    age / 12F, (age - (GreedyFunnelBlockEntity.STREAM_TICKS - 14)) / 12F, seed);
        }

        state.dropCount = 0;
        state.moteCount = 0;
        state.drawing = false;
        for (GreedyFunnelBlockEntity.Shown death : funnel.shown) {
            float age = now - death.start();
            if (age < 0 || age > FunnelTimeline.TOTAL) {
                continue;
            }
            state.drawing = true;
            for (int i = 0; i < death.icons().size(); i++) {
                Vec3 at = FunnelTimeline.drop(pos, death, i, age);
                if (at == null) {
                    continue;
                }
                DropState drop = at(state.drops, state.dropCount++, DropState::new);
                drop.at = at.subtract(origin);
                drop.scale = FunnelTimeline.dropScale(i, age);
                drop.spin = age * 14 + i * 70;
                itemModelResolver.updateForTopItem(drop.item, death.icons().get(i), ItemDisplayContext.GROUND, level, null,
                        death.seed() + i * 31);
            }
            int motes = FunnelTimeline.motes(death.experience());
            for (int i = 0; i < motes; i++) {
                Vec3 at = FunnelTimeline.mote(pos, death, i, age);
                if (at != null) {
                    MoteState mote = at(state.motes, state.moteCount++, MoteState::new);
                    mote.at = at.subtract(origin);
                    mote.size = 0.1F + 0.04F * Mth.sin(age * 0.8F + i);
                }
            }
        }
    }

    private static <T> T at(List<T> list, int index, java.util.function.Supplier<T> fresh) {
        while (list.size() <= index) {
            list.add(fresh.get());
        }
        return list.get(index);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
        float calm = state.hungry ? 0.3F : 1;

        // The vortex: two sigil rings lying in the mouth turning against each other, motes spiralling down into the dark.
        Vec3 mouth = state.mouthAt;
        poseStack.pushPose();
        poseStack.translate(mouth.x, mouth.y + 0.02, mouth.z);
        poseStack.pushPose();
        poseStack.rotate(Axis.YP.rotationDegrees(state.vortex));
        poseStack.rotate(Axis.XP.rotationDegrees(90));
        HexFx.sigilRing(poseStack, collector, 0.36F, 12, FunnelTimeline.SHADOW, 0.55F * calm);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.rotate(Axis.YP.rotationDegrees(-state.vortex * 1.6F));
        poseStack.rotate(Axis.XP.rotationDegrees(90));
        HexFx.sigilRing(poseStack, collector, 0.2F, 6, FunnelTimeline.SHADOW, 0.45F * calm);
        poseStack.popPose();
        for (int i = 0; i < VORTEX_MOTES; i++) {
            // Each mote circles in from the rim and sinks; phases spread so the spiral never empties.
            float phase = ((state.vortex / 360F) * 1.5F + i / (float) VORTEX_MOTES) % 1;
            float radius = 0.4F * (1 - phase);
            float angle = Mth.DEG_TO_RAD * state.vortex * 2 + i * Mth.TWO_PI / VORTEX_MOTES + phase * 5;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * radius, -0.18 * phase, Mth.sin(angle) * radius);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, 0.09F, FunnelTimeline.SHADOW,
                    0.8F * calm * Mth.sin(phase * Mth.PI));
            poseStack.popPose();
        }
        poseStack.popPose();

        // The crystal: greener with the experience it holds, dim when hungry, reddened when it can hold no more.
        Vec3 crystal = state.crystalAt;
        float bob = Mth.sin(time * 0.06F) * 0.03F;
        poseStack.pushPose();
        poseStack.translate(crystal.x, crystal.y + bob, crystal.z);
        int halo = state.full ? 0xE0604A : state.color;
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.5F, halo, 0.35F * calm);
        if (state.filled > 0) {
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.35F + 0.35F * state.filled, FunnelTimeline.EXPERIENCE,
                    0.15F + 0.45F * state.filled);
        }
        poseStack.rotate(Axis.YP.rotationDegrees(state.vortex * 0.4F));
        poseStack.scale(0.36F, 0.36F, 0.36F);
        poseStack.translate(0, -0.2, 0);
        state.crystal.submit(poseStack, collector, state.hungry ? state.lightCoords : LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

        if (state.stream != null) {
            JarStreamFx.submit(poseStack, collector, camera.orientation, eye, state.stream, time);
        }

        for (int i = 0; i < state.dropCount; i++) {
            DropState drop = state.drops.get(i);
            poseStack.pushPose();
            poseStack.translate(drop.at.x, drop.at.y, drop.at.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.45F * drop.scale, FunnelTimeline.SHADOW, 0.35F);
            poseStack.rotate(Axis.YP.rotationDegrees(drop.spin));
            poseStack.scale(drop.scale, drop.scale, drop.scale);
            poseStack.translate(0, -0.125, 0);
            drop.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        for (int i = 0; i < state.moteCount; i++) {
            MoteState mote = state.motes.get(i);
            poseStack.pushPose();
            poseStack.translate(mote.at.x, mote.at.y, mote.at.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, mote.size, FunnelTimeline.EXPERIENCE, 0.9F);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(GreedyFunnelBlockEntity funnel) {
        BlockPos pos = funnel.getBlockPos();
        AABB box = new AABB(pos).inflate(0.6).expandTowards(0, 0.6, 0);
        for (GreedyFunnelBlockEntity.Shown death : funnel.shown) {
            box = box.minmax(AABB.ofSize(death.from(), 2, 2, 2));
        }
        BlockPos jar = funnel.drawing();
        if (jar != null) {
            box = box.minmax(new AABB(jar).inflate(1));
        }
        return box;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState crystal = new ItemStackRenderState();
        final List<DropState> drops = new ArrayList<>();
        final List<MoteState> motes = new ArrayList<>();
        int dropCount;
        int moteCount;
        float time;
        float vortex;
        float filled;
        boolean hungry;
        boolean full;
        boolean drawing;
        int color;
        Vec3 crystalAt = Vec3.ZERO;
        Vec3 mouthAt = Vec3.ZERO;
        JarStreamFx.@Nullable Stream stream;
    }

    static final class DropState {
        final ItemStackRenderState item = new ItemStackRenderState();
        Vec3 at = Vec3.ZERO;
        float scale = 1;
        float spin;
    }

    static final class MoteState {
        Vec3 at = Vec3.ZERO;
        float size;
    }
}
