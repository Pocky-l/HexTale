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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.ArcSpireBlockEntity;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hex Arc Spire at work. A hextech crystal turns slowly at the tip, dim without charge. Every arc the server sends
 * ({@link ArcSpireBlockEntity.Shown}) is a jagged bolt from the crystal to its creature that re-forks while it lasts,
 * with a flash where it lands; the brass coils glow for a moment after each strike. Drawing spark out of a jar shows
 * the jar's stream into the crystal.
 */
public class ArcSpireRenderer implements BlockEntityRenderer<ArcSpireBlockEntity, ArcSpireRenderer.State> {
    private static final int BOLT_COLOR = 0x8FD8FF;
    /** Heights of the coils (in blocks over the base) that glow after a strike, up both halves. */
    private static final float[] COILS = {5.5F / 16, 11.5F / 16, 1 + 3.5F / 16, 1 + 8.5F / 16};
    private final ItemModelResolver itemModelResolver;

    public ArcSpireRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ArcSpireBlockEntity spire, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(spire, state, partialTick, cameraPosition, breakProgress);
        Level level = spire.getLevel();
        BlockPos pos = spire.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        int seed = (int) pos.asLong();
        state.time = HexFx.time(level, partialTick) + (seed & 127);
        state.uncharged = spire.uncharged();
        state.color = PickaxeRuneTint.blend(spire.runes());
        state.crystalAt = spire.crystal().subtract(origin);
        state.tall = spire.tall();
        state.eye = cameraPosition.subtract(origin);
        itemModelResolver.updateForTopItem(state.crystal, new ItemStack(ModItems.HEXTECH_CRYSTAL.get()), ItemDisplayContext.GROUND, level,
                null, seed);
        float now = level == null ? 0 : level.getGameTime() + partialTick;
        state.afterglow = Mth.clamp(1 - (now - spire.struckAt) / 12F, 0, 1);

        state.stream = null;
        BlockPos jar = spire.drawing();
        if (jar != null) {
            float age = now - spire.drewAt();
            state.stream = new JarStreamFx.Stream(JarReach.mouth(jar).subtract(origin), state.crystalAt, Essence.SPARK.color(),
                    age / 12F, (age - (ArcSpireBlockEntity.STREAM_TICKS - 14)) / 12F, seed);
        }
        state.arcs.clear();
        for (ArcSpireBlockEntity.Shown arc : spire.shown) {
            float age = (now - arc.start()) / ArcSpireBlockEntity.ARC_TICKS;
            if (age >= 0 && age <= 1) {
                state.arcs.add(new ArcState(arc.to().subtract(origin), age, arc.seed()));
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        Vec3 crystal = state.crystalAt;
        float calm = state.uncharged ? 0.3F : 1;

        // The coils catch the charge for a moment after every strike.
        if (state.afterglow > 0) {
            for (float y : COILS) {
                if (y > 1 && !state.tall) {
                    break;
                }
                poseStack.pushPose();
                poseStack.translate(0.5, y, 0.5);
                HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.7F, BOLT_COLOR, 0.45F * state.afterglow);
                poseStack.popPose();
            }
        }

        poseStack.pushPose();
        poseStack.translate(crystal.x, crystal.y + Mth.sin(time * 0.08F) * 0.015F, crystal.z);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.45F + 0.3F * state.afterglow, state.color,
                (0.35F + 0.5F * state.afterglow) * calm);
        poseStack.rotate(Axis.YP.rotationDegrees(time * 1.5F));
        poseStack.scale(0.34F, 0.34F, 0.34F);
        poseStack.translate(0, -0.2, 0);
        state.crystal.submit(poseStack, collector, state.uncharged ? state.lightCoords : LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

        Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
        if (state.stream != null) {
            JarStreamFx.submit(poseStack, collector, camera.orientation, eye, state.stream, time);
        }
        Vector3f eyeF = new Vector3f((float) state.eye.x, (float) state.eye.y, (float) state.eye.z);
        for (ArcState arc : state.arcs) {
            bolt(poseStack, collector, crystal, arc, eyeF, time);
            poseStack.pushPose();
            poseStack.translate(arc.to.x, arc.to.y, arc.to.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 1.1F * (1 - arc.age * 0.5F), BOLT_COLOR, 0.7F * (1 - arc.age));
            poseStack.popPose();
        }
    }

    /** A jagged bolt that re-forks a few times while it lasts: a faint wide glow and a white-hot core, one branch off it. */
    private static void bolt(PoseStack poseStack, SubmitNodeCollector collector, Vec3 from, ArcState arc, Vector3f eye, float time) {
        if (arc.to.subtract(from).lengthSqr() < 1.0E-4) {
            return;
        }
        // It flashes, dims and flashes again on the same path, like real lightning.
        float strobe = (int) (arc.age * 12) % 4 < 3 ? 1 : 0.4F;
        float fade = (1 - arc.age * arc.age) * strobe;
        int flicker = (int) (arc.age * 4);
        Vec3[] main = jagged(from, arc.to, arc.seed * 131 + flicker, 0.18F);
        Vec3 forkFrom = main[main.length / 2];
        Vec3 forkTo = forkFrom.add(arc.to.subtract(from).scale(0.3)).add(noise(arc.seed + 7, flicker).scale(0.5));
        Vec3[] fork = jagged(forkFrom, forkTo, arc.seed * 17 + flicker, 0.1F);
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            JarStreamFx.strip(consumer, pose, main, ends(main.length), eye, 0.22F, BOLT_COLOR, 0.35F * fade);
            JarStreamFx.strip(consumer, pose, main, ends(main.length), eye, 0.05F, 0xF4FCFF, 0.95F * fade);
            JarStreamFx.strip(consumer, pose, fork, ends(fork.length), eye, 0.13F, BOLT_COLOR, 0.25F * fade);
            JarStreamFx.strip(consumer, pose, fork, ends(fork.length), eye, 0.03F, 0xF4FCFF, 0.7F * fade);
        });
    }

    private static Vec3[] jagged(Vec3 from, Vec3 to, int seed, float amplitude) {
        double length = to.subtract(from).length();
        int segments = Math.max(5, (int) Math.round(length * 4));
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

    private static float[] ends(int count) {
        float[] light = new float[count];
        for (int i = 0; i < count; i++) {
            light[i] = i == 0 || i == count - 1 ? 0.5F : 1;
        }
        return light;
    }

    @Override
    public AABB getRenderBoundingBox(ArcSpireBlockEntity spire) {
        BlockPos pos = spire.getBlockPos();
        AABB box = new AABB(pos).inflate(0.6).expandTowards(0, 1.8, 0);
        for (ArcSpireBlockEntity.Shown arc : spire.shown) {
            box = box.minmax(AABB.ofSize(arc.to(), 2, 2, 2));
        }
        BlockPos jar = spire.drawing();
        if (jar != null) {
            box = box.minmax(new AABB(jar).inflate(1));
        }
        return box;
    }

    record ArcState(Vec3 to, float age, int seed) {
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState crystal = new ItemStackRenderState();
        final List<ArcState> arcs = new ArrayList<>();
        float time;
        float afterglow;
        boolean uncharged;
        boolean tall;
        int color;
        Vec3 crystalAt = Vec3.ZERO;
        Vec3 eye = Vec3.ZERO;
        JarStreamFx.@Nullable Stream stream;
    }
}
