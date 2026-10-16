package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.rune.Essence;

/**
 * The essence inside an Essence Tank: a glowing liquid between the brass base and cap, as high as the tank is full.
 * Paper stickers with an essence's sign sit on the faces the player marked.
 */
public class EssenceTankRenderer implements BlockEntityRenderer<EssenceTankBlockEntity, EssenceTankRenderer.State> {
    // Inside the posts and glass walls (model: posts 0-2 / 14-16, walls at 1 and 15, base top 2, cap bottom 14),
    // kept a little off the base so the faces never lie in one plane.
    private static final float MIN = 2F / 16;
    private static final float MAX = 14F / 16;
    private static final float BOTTOM = 2.1F / 16;
    private static final float TOP = 13.9F / 16;

    private static final Identifier LABELS = Hextale.id("textures/block/essence_labels.png");
    /** A sticker is a 9 x 9 pixel enamel plate, one texel per pixel; in the sheet one per 16 pixels of a 256 x 16 texture. */
    private static final int LABEL = 9;
    private static final float LABEL_HALF = LABEL / 2F / 16;
    /** Side stickers lie 0.6 px in front of the glass (glass 1 px inside the block), top ones 0.3 px above the cap. */
    private static final float SIDE_INSET = 0.4F / 16;
    private static final float TOP_LIFT = 0.3F / 16;

    public static final class State extends BlockEntityRenderState {
        float fill;
        int color;
        float time;
        long seed;
        final @Nullable Essence[] labels = new Essence[Direction.values().length];
    }

    public EssenceTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EssenceTankBlockEntity tankEntity, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tankEntity, state, partialTick, cameraPosition, breakProgress);
        EssenceTank tank = tankEntity.tank();
        state.fill = tank.isEmpty() ? 0 : tank.amount() / (float) tank.capacity();
        state.color = tank.isEmpty() ? 0 : tank.contents().color();
        state.time = tankEntity.getLevel() == null ? 0 : tankEntity.getLevel().getGameTime() + partialTick;
        state.seed = tankEntity.getBlockPos().asLong();
        for (Direction face : Direction.values()) {
            state.labels[face.get3DDataValue()] = tankEntity.label(face);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fill > 0) {
            // The surface breathes a little, unless the tank is brim-full.
            float wave = state.fill < 0.98F ? Mth.sin(state.time * 0.08F + state.seed % 7) * 0.12F / 16 : 0;
            float top = Math.min(TOP, BOTTOM + (TOP - BOTTOM) * Math.min(1, state.fill) + wave);
            LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, BOTTOM, top, RuneDrawing.argb(state.color, 0.82F),
                    state.time, state.seed, 3);
        }
        boolean any = false;
        for (Essence label : state.labels) {
            any |= label != null;
        }
        if (!any) {
            return;
        }
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(LABELS), (pose, consumer) -> {
            for (Direction face : Direction.values()) {
                Essence label = state.labels[face.get3DDataValue()];
                if (label != null) {
                    sticker(consumer, pose, face, label, light);
                }
            }
        });
    }

    /** One sticker on a face, seen upright from outside: sides with up = +Y, the top with up = north. */
    private static void sticker(VertexConsumer consumer, PoseStack.Pose pose, Direction face, Essence essence, int light) {
        float nx = face.getStepX();
        float ny = face.getStepY();
        float nz = face.getStepZ();
        float[] up = face.getAxis().isVertical() ? new float[]{0, 0, -1} : new float[]{0, 1, 0};
        // right = up x normal
        float rx = up[1] * nz - up[2] * ny;
        float ry = up[2] * nx - up[0] * nz;
        float rz = up[0] * ny - up[1] * nx;
        float out = face == Direction.UP ? 0.5F + TOP_LIFT : 0.5F - SIDE_INSET;
        float cx = 0.5F + nx * out;
        float cy = (face.getAxis().isVertical() ? 0.5F : 8F / 16) + ny * out;
        float cz = 0.5F + nz * out;
        float h = LABEL_HALF;
        float u0 = essence.ordinal() * 16 / 256F;
        float u1 = (essence.ordinal() * 16 + LABEL) / 256F;
        float v0 = 0;
        float v1 = LABEL / 16F;
        float[][] corners = {
                {cx - rx * h - up[0] * h, cy - ry * h - up[1] * h, cz - rz * h - up[2] * h, u0, v1},
                {cx + rx * h - up[0] * h, cy + ry * h - up[1] * h, cz + rz * h - up[2] * h, u1, v1},
                {cx + rx * h + up[0] * h, cy + ry * h + up[1] * h, cz + rz * h + up[2] * h, u1, v0},
                {cx - rx * h + up[0] * h, cy - ry * h + up[1] * h, cz - rz * h + up[2] * h, u0, v0},
        };
        for (float[] c : corners) {
            consumer.addVertex(pose, c[0], c[1], c[2]).setColor(-1).setUv(c[3], c[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(pose, nx, ny, nz);
        }
    }
}
