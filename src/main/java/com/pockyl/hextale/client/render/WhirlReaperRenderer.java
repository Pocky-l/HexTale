package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.WhirlReaperBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.WhirlKind;
import com.pockyl.hextale.farm.WhirlTimeline;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

/**
 * The Whirl Reaper at work. Its brass vanes (their own model) turn round the mast - slowly while it waits, fast while
 * whirls fly, hardly at all without wind - under a hextech crystal floating in a small whirl of its own. Every whirl is
 * drawn from its timeline ({@link WhirlTimeline}): it leaves the crystal and flies to the plant, stands on it as a
 * funnel of wind ({@link WhirlFx}); a fruit is lifted up into it and spun, a stalk's pieces rise turning round the
 * funnel; then the funnel widens and thins out. Drawing wind out of a jar shows the jar's stream into the crystal.
 */
public class WhirlReaperRenderer implements BlockEntityRenderer<WhirlReaperBlockEntity, WhirlReaperRenderer.State> {
    /** The mast's axis in block space (the vanes model turns round it). */
    private static final double MAST = 8 / 16.0;
    private final ItemModelResolver itemModelResolver;

    public WhirlReaperRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WhirlReaperBlockEntity reaper, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(reaper, state, partialTick, cameraPosition, breakProgress);
        Level level = reaper.getLevel();
        BlockPos pos = reaper.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        int seed = (int) pos.asLong();
        state.time = HexFx.time(level, partialTick) + (seed & 127);
        state.vanes = Mth.lerp(partialTick, reaper.vanesBefore, reaper.vanes);
        state.becalmed = reaper.becalmed();
        state.color = PickaxeRuneTint.blend(reaper.runes());
        state.crystalAt = WhirlReaperBlockEntity.crystal(pos).subtract(origin);
        itemModelResolver.updateForTopItem(state.crystal, new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), ItemDisplayContext.GROUND, level,
                null, seed);
        float now = level == null ? 0 : level.getGameTime() + partialTick;

        state.stream = null;
        BlockPos jar = reaper.drawing();
        if (jar != null) {
            float age = now - reaper.drewAt();
            state.stream = new JarStreamFx.Stream(JarReach.mouth(jar).subtract(origin), state.crystalAt, Essence.AIR.color(),
                    age / 12F, (age - (WhirlReaperBlockEntity.STREAM_TICKS - 14)) / 12F, seed);
        }

        state.whirlCount = 0;
        state.blockCount = 0;
        for (WhirlReaperBlockEntity.Shown whirl : reaper.shown) {
            float age = now - whirl.start();
            if (age < 0 || age > whirl.done() + WhirlTimeline.FADE) {
                continue;
            }
            WhirlState ws = at(state.whirls, state.whirlCount++, WhirlState::new);
            ws.foot = WhirlTimeline.position(pos, whirl, age).subtract(origin);
            ws.height = age < whirl.travel() ? 0.55F : WhirlTimeline.height(whirl);
            ws.size = WhirlTimeline.size(whirl, age);
            float fading = WhirlTimeline.fading(whirl, age);
            ws.alpha = Math.min(1, age / 4F) * (1 - fading);
            ws.foot = ws.foot.add(0, fading * 0.8, 0);
            ws.seed = seed + whirl.target().hashCode();
            if (whirl.kind().lifts() && age >= whirl.travel() && age < whirl.done() && level != null) {
                lifted(state, level, whirl, WhirlTimeline.work(whirl, age), age, origin);
            }
        }
    }

    /** The fruit or stalk pieces the whirl carries, placed by how far through its work it is. */
    private static void lifted(State state, Level level, WhirlReaperBlockEntity.Shown whirl, float work, float age, Vec3 origin) {
        Vec3 foot = WhirlTimeline.foot(whirl.target()).subtract(origin);
        for (int i = 0; i < whirl.column().size(); i++) {
            BlockState block = whirl.column().get(i);
            BlockState shown = level.getBlockState(whirl.target().above(whirl.kind() == WhirlKind.GOURD ? 0 : i + 1));
            if (shown == block) {
                // The server has not taken it yet: the real block still stands there.
                continue;
            }
            BlockRender render = at(state.blocks, state.blockCount++, BlockRender::new);
            render.block.blockState = block;
            render.block.blockPos = whirl.target();
            render.block.randomSeedPos = whirl.target();
            render.block.biome = level.getBiome(whirl.target());
            render.block.lightEngine = level.getLightEngine();
            float rise = WhirlTimeline.smooth(work * 2.5F);
            if (whirl.kind() == WhirlKind.GOURD) {
                // Up into the funnel, turning faster and faster, a little smaller as it is about to break.
                render.at = foot.add(0, 0.15 + 0.55 * rise + 0.05 * Mth.sin(age * 0.5F), 0);
                render.spin = age * (6 + 18 * work);
                render.scale = 1 - 0.3F * WhirlTimeline.smooth((work - 0.75F) * 4);
                render.tilt = 12 * rise;
            } else {
                // The pieces stood one over another (the lowest first in the column); they lift and spread round the funnel.
                int height = i + 1;
                float angle = age * 0.25F + i * Mth.TWO_PI / Math.max(1, whirl.column().size());
                double radius = 0.45 * rise;
                render.at = foot.add(Mth.cos(angle) * radius, height + 0.3 * rise * height, Mth.sin(angle) * radius);
                render.spin = age * 9 + i * 40;
                render.scale = 1 - 0.35F * rise;
                render.tilt = 20 * rise;
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

        poseStack.pushPose();
        poseStack.translate(MAST, 0, MAST);
        poseStack.rotate(Axis.YP.rotationDegrees(-state.vanes));
        poseStack.translate(-MAST, 0, -MAST);
        TableModels.submit(poseStack, collector, TableModels.WHIRL_REAPER_VANES, state.lightCoords);
        poseStack.popPose();

        // The crystal in its own small whirl; without wind it hangs dim and the whirl dies down.
        Vec3 crystal = state.crystalAt;
        float bob = Mth.sin(time * 0.07F) * 0.025F;
        float calm = state.becalmed ? 0.35F : 1;
        poseStack.pushPose();
        poseStack.translate(crystal.x, crystal.y + bob, crystal.z);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.5F, state.color, 0.4F * calm);
        poseStack.rotate(Axis.YP.rotationDegrees(state.vanes * 0.6F));
        poseStack.scale(0.4F, 0.4F, 0.4F);
        poseStack.translate(0, -0.2, 0);
        state.crystal.submit(poseStack, collector, state.becalmed ? state.lightCoords : LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        if (!state.becalmed) {
            WhirlFx.submit(poseStack, collector, camera.orientation, eye, crystal.add(0, -0.2, 0), 0.4F, 0.22F, 0.55F, time,
                    (int) state.blockPos.asLong());
        }

        if (state.stream != null) {
            JarStreamFx.submit(poseStack, collector, camera.orientation, eye, state.stream, time);
        }
        for (int i = 0; i < state.whirlCount; i++) {
            WhirlState ws = state.whirls.get(i);
            WhirlFx.submit(poseStack, collector, camera.orientation, eye, ws.foot, ws.height, ws.size, ws.alpha, time, ws.seed);
        }
        for (int i = 0; i < state.blockCount; i++) {
            BlockRender render = state.blocks.get(i);
            poseStack.pushPose();
            poseStack.translate(render.at.x, render.at.y, render.at.z);
            poseStack.rotate(Axis.YP.rotationDegrees(render.spin));
            poseStack.rotate(Axis.XP.rotationDegrees(render.tilt));
            poseStack.scale(render.scale, render.scale, render.scale);
            poseStack.translate(-0.5, 0, -0.5);
            collector.submitMovingBlock(poseStack, render.block, 0);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(WhirlReaperBlockEntity reaper) {
        BlockPos pos = reaper.getBlockPos();
        AABB box = new AABB(pos).inflate(0.6);
        for (WhirlReaperBlockEntity.Shown whirl : reaper.shown) {
            box = box.minmax(new AABB(whirl.target()).inflate(1, 0, 1).expandTowards(0, whirl.column().size() + 3, 0));
        }
        BlockPos jar = reaper.drawing();
        if (jar != null) {
            box = box.minmax(new AABB(jar).inflate(1));
        }
        return box;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState crystal = new ItemStackRenderState();
        final List<WhirlState> whirls = new ArrayList<>();
        final List<BlockRender> blocks = new ArrayList<>();
        int whirlCount;
        int blockCount;
        float time;
        float vanes;
        boolean becalmed;
        int color;
        Vec3 crystalAt = Vec3.ZERO;
        JarStreamFx.@Nullable Stream stream;
    }

    static final class WhirlState {
        Vec3 foot = Vec3.ZERO;
        float height;
        float size;
        float alpha;
        int seed;
    }

    static final class BlockRender {
        final MovingBlockRenderState block = new MovingBlockRenderState();
        Vec3 at = Vec3.ZERO;
        float spin;
        float tilt;
        float scale = 1;
    }
}
