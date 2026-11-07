package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.hextale.block.ChainAnchorBlockEntity;
import com.pockyl.hextale.chem.Zipline;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The chains of a Chain Post or Claw: every line this end shot (the far end draws nothing, so each chain is drawn once).
 * A shot unreels the chain straight with the harpoon head flying at its tip; a held line sags.
 */
public class ChainAnchorRenderer implements BlockEntityRenderer<ChainAnchorBlockEntity, ChainAnchorRenderer.State> {
    /** Farthest a line is drawn from the camera (measured to its nearest point). */
    private static final double VIEW = 112;

    public ChainAnchorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChainAnchorBlockEntity anchor, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(anchor, state, partialTick, cameraPosition, breakProgress);
        Vec3 origin = Vec3.atLowerCornerOf(anchor.getBlockPos());
        state.chains.clear();
        for (ChainAnchorBlockEntity.Link link : anchor.links()) {
            if (!link.owner()) {
                continue;
            }
            Zipline.Line line = link.line();
            if (!line.point(line.nearest(cameraPosition)).closerThan(cameraPosition, VIEW)) {
                continue;
            }
            double time = anchor.getLevel() == null ? link.shotAt() : anchor.getLevel().getGameTime() + (double) partialTick;
            float unreeled = link.unreeled(time);
            Zipline.Line drawn = unreeled >= 1 ? line
                    : new Zipline.Line(line.start(), line.start().add(line.end().subtract(line.start()).scale(unreeled)), 0);
            Chain chain = new Chain();
            chain.points = ChainRender.points(drawn, 1, origin);
            chain.light = anchor.getLevel() == null ? new int[chain.points.length] : ChainRender.light(anchor.getLevel(), chain.points, origin);
            if (unreeled < 1) {
                chain.head = drawn.end().subtract(origin);
                chain.direction = line.end().subtract(line.start()).normalize();
                chain.headLight = chain.light[chain.light.length - 1];
            }
            state.chains.add(chain);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (Chain chain : state.chains) {
            ChainRender.submitChain(poseStack, collector, chain.points, chain.light);
            if (chain.head != null) {
                poseStack.pushPose();
                poseStack.translate(chain.head.x, chain.head.y, chain.head.z);
                poseStack.rotate(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), chain.direction.toVector3f().negate()));
                poseStack.translate(-0.5, -Zipline.CLAW_EYE, -0.5);
                TableModels.submit(poseStack, collector, TableModels.CHAIN_CLAW, chain.headLight);
                poseStack.popPose();
            }
        }
    }

    /** A turn that points -Z along {@code forward} with +Y as near {@code normal} as it can. */
    static Quaternionf turn(Vec3 forward, Vec3 normal) {
        Vector3f f = forward.toVector3f().normalize();
        Vector3f up = normal.toVector3f();
        up.sub(new Vector3f(f).mul(up.dot(f)));
        if (up.lengthSquared() < 1.0E-4F) {
            up.set(Math.abs(f.y) > 0.9F ? 1 : 0, Math.abs(f.y) > 0.9F ? 0 : 1, 0);
            up.sub(new Vector3f(f).mul(up.dot(f)));
        }
        up.normalize();
        Vector3f right = new Vector3f(f).cross(up).normalize();
        Matrix3f basis = new Matrix3f(right, up, new Vector3f(f).negate());
        return basis.getNormalizedRotation(new Quaternionf());
    }

    @Override
    public boolean shouldRender(ChainAnchorBlockEntity anchor, Vec3 cameraPosition) {
        for (ChainAnchorBlockEntity.Link link : anchor.links()) {
            Zipline.Line line = link.line();
            if (link.owner() && line.point(line.nearest(cameraPosition)).closerThan(cameraPosition, VIEW)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A line reaches far past its end's chunk section: drawn with the section, it would vanish whenever that section is
     * off screen or occluded while the chain itself is in view. The render box below does the frustum check instead.
     */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(ChainAnchorBlockEntity anchor) {
        AABB box = new AABB(anchor.getBlockPos()).inflate(0.5);
        for (ChainAnchorBlockEntity.Link link : anchor.links()) {
            if (link.owner()) {
                Zipline.Line line = link.line();
                box = box.minmax(new AABB(line.start(), line.end()).inflate(0.5, line.sag() + 1, 0.5));
            }
        }
        return box;
    }

    static final class Chain {
        Vec3[] points = new Vec3[0];
        int[] light = new int[0];
        @Nullable Vec3 head;
        Vec3 direction = new Vec3(0, 0, -1);
        int headLight;
    }

    public static final class State extends BlockEntityRenderState {
        final List<Chain> chains = new ArrayList<>();
    }
}
