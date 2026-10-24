package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.resources.ResourceLocation;
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
 * An Essence Jar: its glass and lid (the block model only draws the pipe fittings), the glowing liquid inside, as high as
 * the jar is full, and its Essence Sticker as a small paper label with the essence's sign on every side, except where a
 * pipe joins that side. A right click pushes the jar: it rocks on its foot away from the player and settles.
 */
public class EssenceTankRenderer implements BlockEntityRenderer<EssenceTankBlockEntity, EssenceTankRenderer.State> {
    // Inside the glass (model: body 2-14 with its vertical edges cut back by a pixel, thick glass at the foot up to 1,
    // the shoulder from 11), a quarter pixel off the walls so the faces never lie in one plane.
    private static final float MIN = 2.25F / 16;
    private static final float MAX = 13.75F / 16;
    private static final float CUT = 1F / 16;
    private static final float BOTTOM = 1F / 16;
    private static final float TOP = 10.75F / 16;

    private static final ResourceLocation LABELS = Hextale.id("textures/block/essence_labels.png");
    /**
     * A sticker is a small paper label of 26 x 30 texels drawn at four texels per pixel (6.5 x 7.5 px), so its sign is
     * small and sharp; in the sheet one per 32 x 32 cell of a 512 x 256 texture.
     */
    private static final int LABEL_W = 26;
    private static final int LABEL_H = 30;
    private static final int LABEL_CELL = 32;
    private static final float HALF_W = LABEL_W / 2F / 64;
    private static final float HALF_H = LABEL_H / 2F / 64;
    /** Side stickers lie 0.3 px in front of the glass (2 px inside the block), centred across, a little below the middle. */
    private static final float SIDE_OUT = 0.5F - 1.7F / 16;
    private static final float TOP_OUT = 15.3F / 16 - 0.5F;
    private static final float ACROSS = 0;
    private static final float SIDE_UP = -2F / 16;
    private static final float TOP_UP = 0;

    /** How far a pushed jar leans at first (degrees), how fast it rocks (ticks per swing) and how soon it settles. */
    private static final float WOBBLE_DEGREES = 8;
    private static final float WOBBLE_PERIOD = 9;
    private static final float WOBBLE_DECAY = 9;

    public static final class State extends BlockEntityRenderState {
        boolean creative;
        /** The lean of a pushed jar (degrees) and where it leans to (a horizontal direction's 2D value). */
        float tilt;
        int tiltDirection;
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
        state.creative = tankEntity.isCreative();
        float since = state.time - tankEntity.pushedAt();
        state.tilt = tankEntity.pushedAt() < 0 || since < 0 || since > WOBBLE_DECAY * 5 ? 0
                : WOBBLE_DEGREES * (float) Math.exp(-since / WOBBLE_DECAY) * Mth.sin(since * Mth.TWO_PI / WOBBLE_PERIOD);
        state.tiltDirection = tankEntity.pushedTowards();
        BlockState block = tankEntity.getBlockState();
        for (Direction face : Direction.values()) {
            BooleanProperty port = EssenceMachine.PORTS.get(face);
            boolean piped = block.hasProperty(port) && block.getValue(port);
            // The jar's sticker shows on every side without a pipe.
            state.labels[face.get3DDataValue()] = piped || !face.getAxis().isHorizontal() ? null : tankEntity.sticker();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        if (state.tilt != 0) {
            // Rock on the foot: the top leans towards the push.
            Direction towards = Direction.from2DDataValue(state.tiltDirection);
            poseStack.translate(0.5, 0, 0.5);
            poseStack.rotate(Axis.of(new org.joml.Vector3f(towards.getStepZ(), 0, -towards.getStepX())).rotationDegrees(state.tilt));
            poseStack.translate(-0.5, 0, -0.5);
        }
        TableModels.submit(poseStack, collector, state.creative ? TableModels.CREATIVE_JAR : TableModels.ESSENCE_JAR, state.lightCoords);
        submitContents(poseStack, collector, state.fill, state.color, state.time, state.seed, state.labels, state.lightCoords);
        poseStack.popPose();
    }

    /** The liquid and the stickers, in the jar's own block space (shared with the jar item's renderer). */
    static void submitContents(PoseStack poseStack, SubmitNodeCollector collector, float fill, int color, float time, long seed,
                               @Nullable Essence[] labels, int light) {
        if (fill > 0) {
            // The surface breathes a little, unless the jar is brim-full.
            float wave = fill < 0.98F ? Mth.sin(time * 0.08F + seed % 7) * 0.12F / 16 : 0;
            float top = Math.min(TOP, BOTTOM + (TOP - BOTTOM) * Math.min(1, fill) + wave);
            LiquidBox.submitCutCorners(poseStack, collector, MIN, MIN, MAX, MAX, CUT, BOTTOM, top, RuneDrawing.argb(vivid(color), 0.9F), time, seed, 3);
        }
        boolean any = false;
        for (Essence label : labels) {
            any |= label != null;
        }
        if (!any) {
            return;
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(LABELS), (pose, consumer) -> {
            for (Direction face : Direction.values()) {
                Essence label = labels[face.get3DDataValue()];
                if (label != null) {
                    sticker(consumer, pose, face, label, light);
                }
            }
        });
    }

    /**
     * The liquid's colour as the jar shows it: the glass and the liquid's texture dull an essence's own colour, and mixtures
     * average into murk, so the jar lifts its saturation and brightness - every essence reads clearly through the glass.
     */
    static int vivid(int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255F;
        float g = (rgb >> 8 & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue = 0;
        if (delta > 0) {
            hue = max == r ? (g - b) / delta : max == g ? 2 + (b - r) / delta : 4 + (r - g) / delta;
            hue = (hue / 6 + 1) % 1;
        }
        float saturation = max <= 0 ? 0 : delta / max;
        return Mth.hsvToRgb(hue, Math.min(1, saturation * 1.45F + 0.06F), Math.min(1, Math.max(max * 1.2F, 0.88F))) & 0xFFFFFF;
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
        float w = HALF_W;
        float h = HALF_H;
        // Stickers sit 16 to a row on a 512 x 256 sheet.
        float u0 = essence.ordinal() % 16 * LABEL_CELL / 512F;
        float u1 = (essence.ordinal() % 16 * LABEL_CELL + LABEL_W) / 512F;
        float v0 = essence.ordinal() / 16 * LABEL_CELL / 256F;
        float v1 = (essence.ordinal() / 16 * LABEL_CELL + LABEL_H) / 256F;
        float[][] corners = {
                {cx - rx * w - up[0] * h, cy - ry * w - up[1] * h, cz - rz * w - up[2] * h, u0, v1},
                {cx + rx * w - up[0] * h, cy + ry * w - up[1] * h, cz + rz * w - up[2] * h, u1, v1},
                {cx + rx * w + up[0] * h, cy + ry * w + up[1] * h, cz + rz * w + up[2] * h, u1, v0},
                {cx - rx * w + up[0] * h, cy - ry * w + up[1] * h, cz - rz * w + up[2] * h, u0, v0},
        };
        for (float[] c : corners) {
            consumer.addVertex(pose, c[0], c[1], c[2]).setColor(-1).setUv(c[3], c[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(pose, nx, ny, nz);
        }
    }
}
