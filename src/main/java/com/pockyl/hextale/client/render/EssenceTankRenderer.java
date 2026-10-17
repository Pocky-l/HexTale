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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.EssenceMachine;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.rune.Essence;

/**
 * The essence inside an Essence Jar: a glowing liquid inside the glass, as high as the jar is full. Enamel stickers with
 * an essence's sign sit on the faces the player marked, except where a pipe joins that face.
 */
public class EssenceTankRenderer implements BlockEntityRenderer<EssenceTankBlockEntity, EssenceTankRenderer.State> {
    // Inside the glass (model: body 2-14 with its vertical edges cut back by a pixel, thick glass at the foot up to 1,
    // the shoulder from 11), a quarter pixel off the walls so the faces never lie in one plane.
    private static final float MIN = 2.25F / 16;
    private static final float MAX = 13.75F / 16;
    private static final float CUT = 1F / 16;
    private static final float BOTTOM = 1F / 16;
    private static final float TOP = 10.75F / 16;

    private static final Identifier LABELS = Hextale.id("textures/block/essence_labels.png");
    /** A sticker is a 9 x 9 pixel enamel plate, one texel per pixel; in the sheet one per 16 x 16 cell of a 256 x 128 texture. */
    private static final int LABEL = 9;
    private static final float LABEL_HALF = LABEL / 2F / 16;
    /**
     * Side stickers lie 0.3 px in front of the glass (2 px inside the block) between its foot and its top rim, pixels
     * 4-13 from the left and 1-10 from the bottom; the top one 0.3 px above the lid (top at 15), 4-13 from the west and
     * 3-12 from the north. The centres are given from the middle of the face, so the plates sit on the pixel grid.
     */
    private static final float SIDE_OUT = 0.5F - 1.7F / 16;
    private static final float TOP_OUT = 15.3F / 16 - 0.5F;
    private static final float ACROSS = 0.5F / 16;
    private static final float SIDE_UP = -2.5F / 16;
    private static final float TOP_UP = 0.5F / 16;

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
        if (tankEntity.isCreative() && tankEntity.getLevel() != null) {
            float t = (tankEntity.getLevel().getGameTime() % 4800 + partialTick) / 30F;
            int a = (int) t % Essence.COUNT;
            state.color = RuneDrawing.mix(Essence.values()[a].color(), Essence.values()[(a + 1) % Essence.COUNT].color(), t - (int) t);
        }
        state.time = tankEntity.getLevel() == null ? 0 : tankEntity.getLevel().getGameTime() + partialTick;
        state.seed = tankEntity.getBlockPos().asLong();
        BlockState block = tankEntity.getBlockState();
        for (Direction face : Direction.values()) {
            BooleanProperty port = EssenceMachine.PORTS.get(face);
            boolean piped = block.hasProperty(port) && block.getValue(port);
            state.labels[face.get3DDataValue()] = piped ? null : tankEntity.label(face);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fill > 0) {
            // The surface breathes a little, unless the jar is brim-full.
            float wave = state.fill < 0.98F ? Mth.sin(state.time * 0.08F + state.seed % 7) * 0.12F / 16 : 0;
            float top = Math.min(TOP, BOTTOM + (TOP - BOTTOM) * Math.min(1, state.fill) + wave);
            LiquidBox.submitCutCorners(poseStack, collector, MIN, MIN, MAX, MAX, CUT, BOTTOM, top, RuneDrawing.argb(state.color, 0.82F),
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
        float out = face == Direction.UP ? TOP_OUT : SIDE_OUT;
        float lift = face == Direction.UP ? TOP_UP : SIDE_UP;
        float cx = 0.5F + nx * out + rx * ACROSS + up[0] * lift;
        float cy = 0.5F + ny * out + ry * ACROSS + up[1] * lift;
        float cz = 0.5F + nz * out + rz * ACROSS + up[2] * lift;
        float h = LABEL_HALF;
        // Stickers sit 16 to a row on a 256 x 128 sheet.
        float u0 = essence.ordinal() % 16 * 16 / 256F;
        float u1 = (essence.ordinal() % 16 * 16 + LABEL) / 256F;
        float v0 = essence.ordinal() / 16 * 16 / 128F;
        float v1 = (essence.ordinal() / 16 * 16 + LABEL) / 128F;
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
