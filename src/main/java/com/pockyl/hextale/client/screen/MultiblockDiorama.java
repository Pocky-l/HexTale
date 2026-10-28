package com.pockyl.hextale.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import org.joml.Quaternionf;
import org.joml.Vector2f;

import com.pockyl.hextale.client.render.HexFx;
import com.pockyl.hextale.multiblock.Multiblock;
import com.pockyl.hextale.multiblock.MultiblockPattern;
import com.pockyl.hextale.multiblock.Multiblocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A multiblock as a turning 3D model, for the research book and JEI. Left alone ({@link #BUILD}) it builds itself: the
 * layers rise from the bottom up, each block dropping onto its place one after another while a frame of light marks the
 * layer being laid, the hollow cells glint as they are left empty, then a pinch of hextech dust falls on the finished
 * structure, a wave of light climbs it, and it melts away to start again. With a layer chosen it shows the layers below
 * it and that layer in its frame of light, the hollow cells of the layer marked, nothing above.
 */
public final class MultiblockDiorama {
    /** The layer index that means "build it by itself". */
    public static final int BUILD = -1;
    /** Ticks one block takes to drop onto its place. */
    private static final float DROP = 9;
    /** Ticks between two blocks of a layer starting to drop. */
    private static final float STAGGER = 2.2F;
    /** A breath after each layer is laid. */
    private static final float LAYER_PAUSE = 8;
    private static final float DUST = 18;
    private static final float WAKE = 30;
    private static final float ADMIRE = 46;
    private static final float CLEAR = 18;
    private static final float TILT = 30;
    private static final float DROP_HEIGHT = 1.4F;
    private static final int LIGHT = 0x5CC8F2;
    private static final int DUST_COLOR = 0xFFD27A;
    private static final Map<String, Plan> PLANS = new HashMap<>();

    private MultiblockDiorama() {
    }

    public static void register(RegisterPictureInPictureRenderersEvent event) {
        event.register(State.class, Renderer::new);
    }

    /**
     * Submits the structure into a screen box (in the graphics' current pose, so it works inside JEI's translated recipe
     * area too). {@code layer} = {@link #BUILD} or a layer index, {@code time} in ticks, {@code yaw} in degrees added to the
     * slow turn.
     */
    public static void draw(GuiGraphicsExtractor graphics, Multiblock multiblock, int layer, float time, float yaw, int x0, int y0, int x1, int y1) {
        Vector2f a = graphics.pose().transformPosition(x0, y0, new Vector2f());
        Vector2f b = graphics.pose().transformPosition(x1, y1, new Vector2f());
        int ax = Math.round(a.x);
        int ay = Math.round(a.y);
        int bx = Math.round(b.x);
        int by = Math.round(b.y);
        MultiblockPattern pattern = multiblock.pattern();
        float span = Mth.sqrt(pattern.width() * pattern.width() + pattern.depth() * pattern.depth());
        float tall = pattern.height() * Mth.cos(TILT * Mth.DEG_TO_RAD) + span * Mth.sin(TILT * Mth.DEG_TO_RAD);
        float scale = Math.min((bx - ax) / (span * 1.05F), (by - ay) / (tall * 1.06F + DROP_HEIGHT * 0.3F));
        graphics.submitPictureInPictureRenderState(new State(multiblock.id(), layer, time, yaw, ax, ay, bx, by, scale, graphics.peekScissorStack()));
    }

    /** One loop of the building animation, in ticks. */
    public static float loop(Multiblock multiblock) {
        return plan(multiblock).loop;
    }

    /** The layer the building animation is laying at {@code time}, or {@link #BUILD} while it is not laying any. */
    public static int buildingLayer(Multiblock multiblock, float time) {
        Plan plan = plan(multiblock);
        float t = time % plan.loop;
        for (int layer = 0; layer < plan.layers.size(); layer++) {
            if (t >= plan.starts[layer] && t < plan.starts[layer + 1]) {
                return layer;
            }
        }
        return BUILD;
    }

    record State(String multiblock, int layer, float time, float yaw, int x0, int y0, int x1, int y1, float scale,
                 @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
        State(String multiblock, int layer, float time, float yaw, int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea) {
            this(multiblock, layer, time, yaw, x0, y0, x1, y1, scale, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
        }
    }

    /** The order the cells are laid in: layer by layer, row by row, and when each layer starts. */
    private static final class Plan {
        final List<List<MultiblockPattern.Cell>> layers = new ArrayList<>();
        /** Start tick of each layer; the last entry is the tick the structure stands finished. */
        final float[] starts;
        final float loop;

        Plan(MultiblockPattern pattern) {
            for (int y = 0; y < pattern.height(); y++) {
                layers.add(new ArrayList<>());
            }
            for (MultiblockPattern.Cell cell : pattern.cells()) {
                layers.get(cell.y()).add(cell);
            }
            starts = new float[layers.size() + 1];
            float t = 6;
            for (int y = 0; y < layers.size(); y++) {
                List<MultiblockPattern.Cell> layer = layers.get(y);
                layer.sort(Comparator.comparingInt(MultiblockPattern.Cell::z).thenComparingInt(MultiblockPattern.Cell::x));
                starts[y] = t;
                t += DROP + Math.max(0, layer.size() - 1) * STAGGER + LAYER_PAUSE;
            }
            starts[layers.size()] = t;
            loop = t + DUST + WAKE + ADMIRE + CLEAR;
        }
    }

    private static Plan plan(Multiblock multiblock) {
        return PLANS.computeIfAbsent(multiblock.id(), id -> new Plan(multiblock.pattern()));
    }

    /** What stands in a cell: the block of the item the pattern shows there (a tag cycles once a second), turned outwards. */
    @Nullable
    static BlockState stateOf(MultiblockPattern pattern, MultiblockPattern.Cell cell, float time) {
        ItemStack stack = shown(cell, time);
        if (!(stack.getItem() instanceof BlockItem item)) {
            return null;
        }
        BlockState state = item.getBlock().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction out = cell.z() == pattern.depth() - 1 ? Direction.SOUTH : cell.z() == 0 ? Direction.NORTH
                    : cell.x() == 0 ? Direction.WEST : cell.x() == pattern.width() - 1 ? Direction.EAST : Direction.SOUTH;
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, out);
        }
        return state;
    }

    static ItemStack shown(MultiblockPattern.Cell cell, float time) {
        List<ItemStack> shown = cell.key().shown().get();
        return shown.isEmpty() ? ItemStack.EMPTY : shown.get((int) (time / 20) % shown.size());
    }

    private static float easeOut(float k) {
        float u = 1 - k;
        return 1 - u * u * u;
    }

    static final class Renderer extends PictureInPictureRenderer<State> {
        Renderer(net.minecraft.client.renderer.MultiBufferSource.BufferSource portBuffers) {
            super(portBuffers);
        }

        private final BlockModelRenderState block = new BlockModelRenderState();
        private final List<ItemStackRenderState> items = new ArrayList<>();
        private final BlockDisplayContext context = BlockDisplayContext.create();

        @Override
        public Class<State> getRenderStateClass() {
            return State.class;
        }

        @Override
        protected void renderToTexture(State state, PoseStack poseStack) {
            net.minecraft.client.renderer.feature.FeatureRenderDispatcher portFeatures =
                    net.minecraft.client.Minecraft.getInstance().gameRenderer.getFeatureRenderDispatcher();
            renderToTexture(state, poseStack, portFeatures.getSubmitNodeStorage());
            portFeatures.renderAllFeatures();
        }

        private void renderToTexture(State state, PoseStack poseStack, SubmitNodeCollector collector) {
            Multiblock multiblock = Multiblocks.byId(state.multiblock());
            if (multiblock == null) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
            MultiblockPattern pattern = multiblock.pattern();
            Plan plan = plan(multiblock);
            float time = state.time();
            float t = time % plan.loop;
            boolean building = state.layer() == BUILD;
            float built = plan.starts[plan.layers.size()];
            float wakeStart = built + DUST;
            float clearStart = wakeStart + WAKE + ADMIRE;

            float yaw = time * 0.5F + 30 + state.yaw();
            poseStack.scale(1, -1, -1);
            poseStack.mulPose(Axis.XP.rotationDegrees(TILT));
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            Quaternionf camera = new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD).rotateX(-TILT * Mth.DEG_TO_RAD);
            float cx = pattern.width() / 2F;
            float cz = pattern.depth() / 2F;
            poseStack.translate(-cx, -pattern.height() / 2F, -cz);
            int light = LightCoordsUtil.FULL_BRIGHT;
            float pulse = 0.5F + 0.5F * Mth.sin(time * 0.15F);
            int itemIndex = 0;

            for (int y = 0; y < plan.layers.size(); y++) {
                if (!building && y > state.layer()) {
                    break;
                }
                boolean current = building ? t >= plan.starts[y] && t < plan.starts[y + 1] : y == state.layer();
                if (current) {
                    float k = building ? Mth.clamp((t - plan.starts[y]) / 6, 0, 1) * Mth.clamp((plan.starts[y + 1] - t) / 6, 0, 1) : 1;
                    layerFrame(poseStack, collector, pattern, y, (0.35F + 0.25F * pulse) * k);
                }
                // In the clearing the top layers melt away first.
                float melt = building ? Mth.clamp((t - clearStart - (plan.layers.size() - 1 - y) * 3) / 9, 0, 1) : 0;
                List<MultiblockPattern.Cell> cells = plan.layers.get(y);
                for (int i = 0; i < cells.size(); i++) {
                    MultiblockPattern.Cell cell = cells.get(i);
                    float k = building ? Mth.clamp((t - plan.starts[y] - i * STAGGER) / DROP, 0, 1) : 1;
                    if (k <= 0 || melt >= 1) {
                        continue;
                    }
                    if (cell.key() == MultiblockPattern.Key.AIR) {
                        boolean laying = building ? current : y == state.layer();
                        if (laying) {
                            hollow(poseStack, collector, camera, cell, time, k);
                        }
                        continue;
                    }
                    float e = easeOut(k);
                    float size = (0.45F + 0.55F * e) * (1 - melt);
                    poseStack.pushPose();
                    poseStack.translate(cell.x() + 0.5F, cell.y() + 0.5F + (1 - e) * DROP_HEIGHT + melt * 0.4F, cell.z() + 0.5F);
                    poseStack.scale(size, size, size);
                    poseStack.translate(-0.5F, -0.5F, -0.5F);
                    BlockState blockState = stateOf(pattern, cell, time);
                    if (blockState != null) {
                        minecraft.getBlockModelResolver().update(block, blockState, context);
                        block.submitMultiLayer(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
                    } else {
                        if (itemIndex == items.size()) {
                            items.add(new ItemStackRenderState());
                        }
                        ItemStackRenderState item = items.get(itemIndex++);
                        minecraft.getItemModelResolver().updateForTopItem(item, shown(cell, time), ItemDisplayContext.NONE, minecraft.level, null, 0);
                        poseStack.translate(0.5F, 0.5F, 0.5F);
                        item.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
                    }
                    poseStack.popPose();
                }
            }

            if (building && t >= built && t < clearStart + CLEAR) {
                wake(poseStack, collector, camera, pattern, t - built, time);
            }
        }

        /** A thin plate of light under a layer, a little wider than the structure: here is where the blocks go now. */
        private static void layerFrame(PoseStack poseStack, SubmitNodeCollector collector, MultiblockPattern pattern, int y, float alpha) {
            if (alpha <= 0.01F) {
                return;
            }
            poseStack.pushPose();
            poseStack.translate(pattern.width() / 2F, y - 0.02F, pattern.depth() / 2F);
            poseStack.scale(pattern.width() + 0.5F, 0.03F, pattern.depth() + 0.5F);
            HexFx.cube(poseStack, collector, 1, LIGHT, alpha);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(pattern.width() / 2F, y + 0.01F, pattern.depth() / 2F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            HexFx.sigilRing(poseStack, collector, Math.max(pattern.width(), pattern.depth()) * 0.74F, 12, LIGHT, alpha * 1.4F);
            poseStack.popPose();
        }

        /** A hollow cell: a small turning crystal of light where nothing may stand. */
        private static void hollow(PoseStack poseStack, SubmitNodeCollector collector, Quaternionf camera, MultiblockPattern.Cell cell, float time, float k) {
            poseStack.pushPose();
            poseStack.translate(cell.x() + 0.5F, cell.y() + 0.5F, cell.z() + 0.5F);
            HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 0.7F, LIGHT, 0.35F * k);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 4));
            poseStack.mulPose(Axis.XP.rotationDegrees(35));
            HexFx.cube(poseStack, collector, 0.18F * k, 0xCFF3FF, 0.7F * k);
            poseStack.popPose();
        }

        /** The finished structure wakes: dust falls on it, a band of light climbs it, a halo blooms and fades. */
        private static void wake(PoseStack poseStack, SubmitNodeCollector collector, Quaternionf camera, MultiblockPattern pattern, float t, float time) {
            float top = pattern.height();
            float cx = pattern.width() / 2F;
            float cz = pattern.depth() / 2F;
            // The pinch of dust: glittering motes drifting down onto the top.
            if (t < DUST + 8) {
                for (int m = 0; m < 14; m++) {
                    float delay = m * 1.1F;
                    float k = (t - delay) / 14;
                    if (k <= 0 || k >= 1) {
                        continue;
                    }
                    float angle = m * 2.4F;
                    float r = 0.25F + (m % 4) * 0.22F;
                    poseStack.pushPose();
                    poseStack.translate(cx + Mth.cos(angle) * r * pattern.width() / 3, top + 1.6F - k * 1.7F, cz + Mth.sin(angle) * r * pattern.depth() / 3);
                    HexFx.sprite(poseStack, collector, camera, HexFx.MOTE, 0.16F, m % 3 == 0 ? LIGHT : DUST_COLOR, Mth.sin(k * Mth.PI));
                    poseStack.popPose();
                }
            }
            // The wave of light climbing the layers.
            float w = (t - DUST) / WAKE;
            if (w > 0 && w < 1) {
                float y = w * top;
                poseStack.pushPose();
                poseStack.translate(cx, y, cz);
                poseStack.scale(pattern.width() + 0.3F, 0.06F, pattern.depth() + 0.3F);
                HexFx.cube(poseStack, collector, 1, LIGHT, 0.55F * Mth.sin(w * Mth.PI));
                poseStack.popPose();
                poseStack.pushPose();
                poseStack.translate(cx, y, cz);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(time * 2));
                HexFx.sigilRing(poseStack, collector, Math.max(pattern.width(), pattern.depth()) * 0.8F, 12, 0xCFF3FF, 0.9F * Mth.sin(w * Mth.PI));
                poseStack.popPose();
            }
            // The structure stands awake: a soft halo over it, swelling when the wave reaches the top.
            float glow = t < DUST + WAKE ? Math.max(0, w) : Math.max(0, 1 - (t - DUST - WAKE) / (ADMIRE + CLEAR));
            if (glow > 0.01F) {
                poseStack.pushPose();
                poseStack.translate(cx, top + 0.3F, cz);
                HexFx.sprite(poseStack, collector, camera, HexFx.HALO, 1.2F + glow * Math.max(pattern.width(), pattern.depth()), LIGHT, 0.45F * glow);
                poseStack.popPose();
            }
        }

        @Override
        protected float getTranslateY(int height, int guiScale) {
            return height / 2.0F;
        }

        @Override
        protected String getTextureLabel() {
            return "hextale multiblock diorama";
        }
    }
}
