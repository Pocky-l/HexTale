package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pockyl.hextale.block.VialRackBlock;
import com.pockyl.hextale.block.VialRackBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * The vials on a Vial Rack: each a 2 x 6 x 2 px tube in its place of the model, the brew's colour up to the shoulder, then
 * a narrower glass neck and a cork. Lit by the world, only shimmer shines by itself.
 */
public class VialRackRenderer implements BlockEntityRenderer<VialRackBlockEntity, VialRackRenderer.State> {
    /** Column centres in px of the north-facing model, from the player's left. */
    private static final float[] COLUMNS = {13, 10, 6, 3};
    private static final int GLASS = 0xFFBFE0CE;
    private static final int CORK = 0xFF8A6238;
    private static final float HALF = 1;
    private static final float NECK = 0.6F;
    private static final float FOOT = 1;
    private static final float SURFACE = 3.4F;
    private static final float SHOULDER = 4;
    private static final float FOOT_SHADE = 0.3F;
    private static final float SURFACE_LIGHT = 0.4F;
    private static final float CORK_FROM = 4.8F;
    private static final float HEIGHT = 6;

    public VialRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VialRackBlockEntity rack, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(rack, state, partialTick, cameraPosition, breakProgress);
        state.facing = rack.getBlockState().getValue(VialRackBlock.FACING);
        state.wall = rack.getBlockState().getValue(VialRackBlock.WALL);
        state.any = false;
        for (int slot = 0; slot < VialRackBlockEntity.SLOTS; slot++) {
            state.brews[slot] = ChemVialItem.brew(rack.getItem(slot));
            state.any |= state.brews[slot] != null;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.any) {
            return;
        }
        poseStack.pushPose();
        TableModels.rotateToFacing(poseStack, state.facing);
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(HexFx.WHITE), (pose, consumer) -> {
            for (int slot = 0; slot < VialRackBlockEntity.SLOTS; slot++) {
                Brew brew = state.brews[slot];
                if (brew == null) {
                    continue;
                }
                boolean high = slot >= 4;
                float x = COLUMNS[slot % 4];
                float z = state.wall ? 13 : high ? 10 : 5;
                float y = state.wall ? high ? 9 : 1 : high ? 5 : 1;
                int rgb = brew.color();
                int lit = brew.shimmer() ? LightCoordsUtil.FULL_BRIGHT : light;
                // Deep at the foot, a lit line at the surface: the same read as the carboy's brew.
                box(consumer, pose, x, y, z, HALF, 0, FOOT, 0xFF000000 | RuneDrawing.mix(rgb, 0x000000, FOOT_SHADE), lit);
                box(consumer, pose, x, y, z, HALF, FOOT, SURFACE, 0xFF000000 | rgb, lit);
                box(consumer, pose, x, y, z, HALF, SURFACE, SHOULDER, 0xFF000000 | RuneDrawing.mix(rgb, 0xFFFFFF, SURFACE_LIGHT), lit);
                box(consumer, pose, x, y, z, NECK, SHOULDER, CORK_FROM, GLASS, light);
                box(consumer, pose, x, y, z, NECK, CORK_FROM, HEIGHT, CORK, light);
            }
        });
        poseStack.popPose();
    }

    /** A box in px round the column {@code x, z}, from {@code from} to {@code to} above {@code y}. */
    private static void box(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float half, float from, float to, int argb,
                            int light) {
        float x0 = (x - half) / 16;
        float x1 = (x + half) / 16;
        float z0 = (z - half) / 16;
        float z1 = (z + half) / 16;
        float y0 = (y + from) / 16;
        float y1 = (y + to) / 16;
        quad(consumer, pose, argb, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(consumer, pose, argb, light, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
        quad(consumer, pose, argb, light, 0, 0, -1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
        quad(consumer, pose, argb, light, 0, 0, 1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
        quad(consumer, pose, argb, light, -1, 0, 0, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
        quad(consumer, pose, argb, light, 1, 0, 0, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int argb, int light, float nx, float ny, float nz, float... points) {
        for (int i = 0; i < 12; i += 3) {
            consumer.addVertex(pose, points[i], points[i + 1], points[i + 2]).setColor(argb).setUv(i < 6 ? 0 : 1, i == 0 || i == 9 ? 0 : 1)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        }
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        boolean wall;
        boolean any;
        final Brew[] brews = new Brew[VialRackBlockEntity.SLOTS];
    }
}
