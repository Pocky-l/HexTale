package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.block.ChemCarboyBlock;
import com.pockyl.hextale.block.ChemCarboyBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.CarboyContents;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.ItemModelResolver;
import com.pockyl.hextale.port.client.ItemStackRenderState;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * A Chem Carboy's brew: a box 1 px inside the glass, as high as the carboy is full. The body is darker and deeper than the
 * brew's colour and a lit line runs along its surface, so a green brew stands apart from the green glass; bubbles rise
 * through it and shimmer glows. The plate on the neck carries the brew's vial as its label.
 */
public class ChemCarboyRenderer implements BlockEntityRenderer<ChemCarboyBlockEntity, ChemCarboyRenderer.State> {
    private static final float MIN = 3 / 16F;
    private static final float MAX = 13 / 16F;
    private static final float BOTTOM = 3 / 16F;
    private static final float HEIGHT = 12 / 16F;
    /** The level line's height, and the least the brew shows however little is left. */
    private static final float LINE = 0.75F / 16;
    private static final float LEAST = 0.03F;
    private static final float BODY_SHADE = 0.22F;
    private static final float LINE_LIGHT = 0.7F;
    /** The label: 4 px on the plate's face at z 5, just proud of it. */
    private static final float LABEL_X = 8 / 16F;
    private static final float LABEL_Y = 23 / 16F;
    private static final float LABEL_Z = 4.88F / 16;
    private static final float LABEL_SCALE = 4 / 16F;

    private final ItemModelResolver itemModelResolver;

    public ChemCarboyRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = ItemModelResolver.INSTANCE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChemCarboyBlockEntity carboy, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(carboy, state, partialTick, cameraPosition, breakProgress);
        CarboyContents contents = carboy.contents();
        state.fill = contents.isEmpty() ? 0 : Math.max(LEAST, contents.level());
        Brew brew = contents.brew();
        state.color = brew.color();
        state.glows = !contents.isEmpty() && brew.shimmer();
        state.time = HexFx.time(carboy.getLevel(), partialTick);
        state.facing = carboy.getBlockState().getValue(ChemCarboyBlock.FACING);
        if (contents.isEmpty()) {
            state.label.clear();
        } else {
            itemModelResolver.updateForTopItem(state.label, ChemVialItem.of(brew), ItemDisplayContext.FIXED, carboy.getLevel(), null,
                    (int) carboy.getBlockPos().asLong());
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fill <= 0) {
            return;
        }
        poseStack.pushPose();
        TableModels.rotateToFacing(poseStack, state.facing);
        float top = BOTTOM + HEIGHT * state.fill;
        float body = Math.max(BOTTOM, top - LINE);
        int deep = RuneDrawing.mix(state.color, 0x000000, BODY_SHADE);
        int alpha = state.glows ? 0xE6 : 0xD8;
        long seed = state.blockPos.asLong();
        LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, BOTTOM, body, alpha << 24 | deep, state.time, seed, state.glows ? 5 : 3);
        LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, body, top, 0xF0 << 24 | RuneDrawing.mix(state.color, 0xFFFFFF, LINE_LIGHT));
        if (state.glows) {
            // Shimmer lights itself: a second, brighter skin of the same liquid.
            LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, BOTTOM, body, 0x30 << 24 | state.color);
        }
        if (!state.label.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(LABEL_X, LABEL_Y, LABEL_Z);
            poseStack.scale(LABEL_SCALE, LABEL_SCALE, LABEL_SCALE);
            state.label.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** The plate stands in the cell above. */
    @Override
    public AABB getRenderBoundingBox(ChemCarboyBlockEntity carboy) {
        return new AABB(carboy.getBlockPos()).expandTowards(0, 1, 0);
    }

    public static final class State extends BlockEntityRenderState {
        float fill;
        int color;
        boolean glows;
        float time;
        Direction facing = Direction.NORTH;
        final ItemStackRenderState label = new ItemStackRenderState();
    }
}
