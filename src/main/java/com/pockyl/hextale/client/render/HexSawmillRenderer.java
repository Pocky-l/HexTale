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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexSawmillBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.SawTimeline;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hex Sawmill at work. Its saw disc (its own model) turns in the slot of the stump - fast while it saws, slowly while
 * it waits, hardly at all without wind - under a hextech crystal floating over the guard. A saw that flies out is a
 * glowing copy of the disc lying flat and spinning: it flies to the trunk, saws, and when it is through the tree is
 * drawn tipping over about the edge of its foot ({@link SawTimeline}), block by block as it stood, until it hits the
 * ground and breaks into crumbs. Drawing wind out of a jar shows the jar's stream into the crystal.
 */
public class HexSawmillRenderer implements BlockEntityRenderer<HexSawmillBlockEntity, HexSawmillRenderer.State> {
    /** The disc's axle in block space (the disc model is built round it). */
    private static final double AXLE_Y = 8 / 16.0;
    private static final double AXLE_Z = 8.5 / 16.0;
    /** Blocks of one falling tree drawn at most. */
    private static final int MAX_BLOCKS = 600;
    private static final int GLOW = 0x8FE6FF;
    private final ItemModelResolver itemModelResolver;

    public HexSawmillRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HexSawmillBlockEntity mill, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(mill, state, partialTick, cameraPosition, breakProgress);
        Level level = mill.getLevel();
        BlockPos pos = mill.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        int seed = (int) pos.asLong();
        state.time = HexFx.time(level, partialTick) + (seed & 127);
        state.disc = Mth.lerp(partialTick, mill.discBefore, mill.disc);
        state.becalmed = mill.becalmed();
        state.color = PickaxeRuneTint.blend(mill.runes());
        state.crystalAt = HexSawmillBlockEntity.crystal(pos).subtract(origin);
        itemModelResolver.updateForTopItem(state.crystal, new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), ItemDisplayContext.GROUND, level,
                null, seed);
        float now = level == null ? 0 : level.getGameTime() + partialTick;
        state.stream = null;
        BlockPos jar = mill.drawing();
        if (jar != null) {
            float age = now - mill.drewAt();
            state.stream = new JarStreamFx.Stream(JarReach.mouth(jar).subtract(origin), state.crystalAt, Essence.AIR.color(),
                    age / 12F, (age - (HexSawmillBlockEntity.STREAM_TICKS - 14)) / 12F, seed);
        }

        state.sawCount = 0;
        state.blockCount = 0;
        for (HexSawmillBlockEntity.Shown saw : mill.shown) {
            float age = now - saw.start();
            if (age < 0 || age > saw.done() + SawTimeline.FADE) {
                continue;
            }
            SawState ss = at(state.saws, state.sawCount++, SawState::new);
            ss.at = SawTimeline.position(pos, saw, age).subtract(origin);
            ss.alpha = Math.min(1, age / 4F) * (1 - Mth.clamp((age - saw.done()) / SawTimeline.FADE, 0, 1));
            ss.sawing = age >= saw.travel() && age < saw.cut();
            ss.spin = age * (ss.sawing ? 70 : 35);
            float angle = SawTimeline.fall(saw, age);
            if (angle >= 0 && age < saw.done() && level != null) {
                Quaternionf turn = SawTimeline.turn(saw, angle);
                Vec3 pivot = SawTimeline.pivot(saw).subtract(origin);
                for (int i = 0; i < saw.blocks().size() && state.blockCount < MAX_BLOCKS; i++) {
                    BlockPos block = saw.blocks().get(i);
                    BlockRender render = at(state.blocks, state.blockCount++, BlockRender::new);
                    render.block.blockState = saw.states().get(i);
                    render.block.blockPos = block;
                    render.block.randomSeedPos = block;
                    render.block.biome = level.getBiome(block);
                    render.block.lightEngine = level.getLightEngine();
                    render.pivot = pivot;
                    render.turn = turn;
                    render.offset = Vec3.atLowerCornerOf(block).subtract(origin).subtract(pivot);
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

        // The disc turns round its axle, which runs north-south through its hub.
        poseStack.pushPose();
        poseStack.translate(0.5, AXLE_Y, AXLE_Z);
        poseStack.rotate(Axis.ZP.rotationDegrees(-state.disc));
        poseStack.translate(-0.5, -AXLE_Y, -AXLE_Z);
        TableModels.submit(poseStack, collector, TableModels.HEX_SAW_DISC, state.lightCoords);
        poseStack.popPose();

        Vec3 crystal = state.crystalAt;
        float bob = Mth.sin(time * 0.07F) * 0.025F;
        poseStack.pushPose();
        poseStack.translate(crystal.x, crystal.y + bob, crystal.z);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.5F, state.color, state.becalmed ? 0.15F : 0.4F);
        poseStack.rotate(Axis.YP.rotationDegrees(time * 2));
        poseStack.scale(0.4F, 0.4F, 0.4F);
        poseStack.translate(0, -0.2, 0);
        state.crystal.submit(poseStack, collector, state.becalmed ? state.lightCoords : LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

        if (state.stream != null) {
            JarStreamFx.submit(poseStack, collector, camera.orientation, eye, state.stream, time);
        }
        for (int i = 0; i < state.sawCount; i++) {
            SawState ss = state.saws.get(i);
            if (ss.alpha <= 0.01F) {
                continue;
            }
            // A glowing copy of the disc, lying flat and spinning, in a ring of light.
            poseStack.pushPose();
            poseStack.translate(ss.at.x, ss.at.y, ss.at.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, ss.sawing ? 0.9F : 0.6F, GLOW, 0.45F * ss.alpha);
            poseStack.pushPose();
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            poseStack.rotate(Axis.ZP.rotationDegrees(ss.spin));
            HexFx.sigilRing(poseStack, collector, 0.36F, 12, GLOW, 0.8F * ss.alpha);
            poseStack.popPose();
            poseStack.rotate(Axis.YP.rotationDegrees(ss.spin));
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            poseStack.scale(0.7F, 0.7F, 0.7F);
            poseStack.translate(-0.5, -0.5, -8.5 / 16);
            TableModels.submit(poseStack, collector, TableModels.HEX_SAW_DISC, LightCoordsUtil.FULL_BRIGHT);
            poseStack.popPose();
        }
        for (int i = 0; i < state.blockCount; i++) {
            BlockRender render = state.blocks.get(i);
            poseStack.pushPose();
            poseStack.translate(render.pivot.x, render.pivot.y, render.pivot.z);
            poseStack.rotate(render.turn);
            poseStack.translate(render.offset.x, render.offset.y, render.offset.z);
            collector.submitMovingBlock(poseStack, render.block, 0);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(HexSawmillBlockEntity mill) {
        BlockPos pos = mill.getBlockPos();
        AABB box = new AABB(pos).inflate(0.6).expandTowards(0, 1, 0);
        for (HexSawmillBlockEntity.Shown saw : mill.shown) {
            int height = saw.top() - saw.foot().getY() + 2;
            box = box.minmax(new AABB(saw.foot()).inflate(height + 1, 1, height + 1).expandTowards(0, height, 0));
        }
        BlockPos jar = mill.drawing();
        if (jar != null) {
            box = box.minmax(new AABB(jar).inflate(1));
        }
        return box;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState crystal = new ItemStackRenderState();
        final List<SawState> saws = new ArrayList<>();
        final List<BlockRender> blocks = new ArrayList<>();
        int sawCount;
        int blockCount;
        float time;
        float disc;
        boolean becalmed;
        int color;
        Vec3 crystalAt = Vec3.ZERO;
        JarStreamFx.@Nullable Stream stream;
    }

    static final class SawState {
        Vec3 at = Vec3.ZERO;
        float alpha;
        float spin;
        boolean sawing;
    }

    static final class BlockRender {
        final MovingBlockRenderState block = new MovingBlockRenderState();
        Vec3 pivot = Vec3.ZERO;
        Quaternionf turn = new Quaternionf();
        Vec3 offset = Vec3.ZERO;
    }
}
