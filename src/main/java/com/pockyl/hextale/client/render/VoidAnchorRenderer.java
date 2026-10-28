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
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.VoidAnchorBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;

/**
 * A Void Anchor: its crystal hangs in the window through the stone and turns slowly, a plum shadow growing in it as it
 * clouds. Tied to a breach, a thread of light runs from the crystal to the breach's edge - taut, cold and pulsing toward
 * the breach once the ring holds it, hot magenta and jerking while the breach still fights; beads of what it feeds the
 * breach run along it. The jar it takes the waste from streams into the stone.
 */
public class VoidAnchorRenderer implements BlockEntityRenderer<VoidAnchorBlockEntity, VoidAnchorRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public VoidAnchorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = ItemModelResolver.INSTANCE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VoidAnchorBlockEntity anchor, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(anchor, state, partialTick, cameraPosition, breakProgress);
        Level level = anchor.getLevel();
        BlockPos pos = anchor.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        int seed = (int) pos.asLong();
        state.time = HexFx.time(level, partialTick) + (seed & 127);
        state.seed = seed;
        state.hasCrystal = !anchor.crystal().isEmpty();
        state.clouding = anchor.clouding();
        state.color = PickaxeRuneTint.blend(anchor.runes());
        state.crystalAt = VoidAnchorBlockEntity.crystalAt(pos).subtract(origin);
        state.eye = cameraPosition.subtract(origin);
        if (state.hasCrystal) {
            itemModelResolver.updateForTopItem(state.crystal, anchor.crystal(), ItemDisplayContext.GROUND, level, null, seed);
        }
        state.tied = anchor.tied();
        state.subdued = anchor.subdued();
        state.threadEnd = anchor.threadEnd().subtract(origin);
        float now = level == null ? 0 : level.getGameTime() + partialTick;
        state.eating = Mth.clamp(1 - (now - anchor.ateAt()) / 30F, 0, 1);
        state.stream = null;
        BlockPos jar = anchor.drawing();
        if (jar != null) {
            float age = now - anchor.drewAt();
            state.stream = new JarStreamFx.Stream(JarReach.mouth(jar).subtract(origin), state.crystalAt, EssenceMix.UNKNOWN_COLOR | 0x302838,
                    age / 12F, (age - (VoidAnchorBlockEntity.STREAM_TICKS - 14)) / 12F, seed);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        Vec3 crystal = state.crystalAt;
        if (state.hasCrystal) {
            float clouding = state.clouding;
            boolean clouded = clouding >= 1;
            poseStack.pushPose();
            poseStack.translate(crystal.x, crystal.y + (clouded ? 0 : Mth.sin(time * 0.07F) * 0.02F), crystal.z);
            float light = 1 - clouding;
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.42F, state.color, 0.4F * light);
            if (clouding > 0) {
                HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.26F + 0.3F * clouding, 0x7A2A6E, 0.5F * clouding);
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(clouded ? 0 : time * (state.tied ? 0.6F : 1.6F)));
            poseStack.scale(0.3F, 0.3F, 0.3F);
            poseStack.translate(0, -0.2, 0);
            state.crystal.submit(poseStack, collector, clouded ? state.lightCoords : LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (state.tied) {
            thread(poseStack, collector, state, time);
        }
        if (state.stream != null) {
            Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
            JarStreamFx.submit(poseStack, collector, camera.orientation, eye, state.stream, time);
        }
    }

    /** The thread from the crystal to the breach: taut and cold when it holds, hot and jerking while it fights. */
    private static void thread(PoseStack poseStack, SubmitNodeCollector collector, State state, float time) {
        Vec3 from = state.crystalAt;
        Vec3 to = state.threadEnd;
        double length = to.subtract(from).length();
        if (length < 0.2) {
            return;
        }
        int segments = Math.max(8, (int) Math.round(length * 3));
        Vec3[] points = new Vec3[segments + 1];
        float jerk = state.subdued ? 0 : 0.12F + 0.1F * Mth.sin(time * 0.9F);
        for (int k = 0; k <= segments; k++) {
            float t = k / (float) segments;
            Vec3 at = from.lerp(to, t);
            if (k > 0 && k < segments) {
                // A fighting breach tugs the thread: it shivers along its length.
                float wave = Mth.sin(t * 9 + time * 0.8F + state.seed) * jerk * Mth.sin(t * Mth.PI);
                at = at.add(0, wave, wave * 0.6);
            }
            points[k] = at;
        }
        float[] light = new float[points.length];
        for (int k = 0; k < light.length; k++) {
            float t = k / (float) (light.length - 1);
            // A pulse runs from the anchor into the breach; a fed breach swallows a bright bead along it.
            float pulse = 0.55F + 0.45F * Mth.sin(t * 7 - time * (state.subdued ? 0.25F : 0.6F));
            light[k] = (k == 0 || k == light.length - 1 ? 0.5F : 1) * pulse;
        }
        int colour = state.subdued ? VoidAnchorBlockEntity.COLD : VoidAnchorBlockEntity.HOT;
        Vector3f eye = new Vector3f((float) state.eye.x, (float) state.eye.y, (float) state.eye.z);
        float alpha = 0.55F + 0.35F * state.eating;
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            JarStreamFx.strip(consumer, pose, points, light, eye, 0.16F, colour, 0.3F * alpha);
            JarStreamFx.strip(consumer, pose, points, light, eye, 0.035F, 0xF4F0FF, 0.8F * alpha);
        });
    }

    @Override
    public AABB getRenderBoundingBox(VoidAnchorBlockEntity anchor) {
        AABB box = new AABB(anchor.getBlockPos()).inflate(0.5);
        if (anchor.tied()) {
            box = box.minmax(AABB.ofSize(anchor.threadEnd(), 1, 1, 1));
        }
        BlockPos jar = anchor.drawing();
        if (jar != null) {
            box = box.minmax(new AABB(jar).inflate(1));
        }
        return box;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState crystal = new ItemStackRenderState();
        float time;
        int seed;
        boolean hasCrystal;
        boolean tied;
        boolean subdued;
        float clouding;
        float eating;
        int color;
        Vec3 crystalAt = Vec3.ZERO;
        Vec3 threadEnd = Vec3.ZERO;
        Vec3 eye = Vec3.ZERO;
        JarStreamFx.@Nullable Stream stream;
    }
}
