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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.AlembicBlock;
import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchItems;

/**
 * Inside the Alembic's flask: the water, tinted by the essence boiled into it (or by the item boiling); bubbles rising
 * through it while an item boils; and the item itself, turning slowly in the water.
 */
public class AlembicRenderer implements BlockEntityRenderer<AlembicBlockEntity, AlembicRenderer.State> {
    private static final float MIN = 4.6F / 16;
    private static final float MAX = 11.4F / 16;
    private static final float BOTTOM = 4.4F / 16;
    private static final float TOP = 10.6F / 16;
    /** The round flask's corners are cut back by a pixel; the water follows them. */
    private static final float CUT = 1F / 16;
    private static final int WATER_COLOR = 0x3F76E4;
    private static final int BUBBLES = 7;

    private final ItemModelResolver itemModelResolver;

    public static final class State extends BlockEntityRenderState {
        float level;
        int color;
        float time;
        boolean working;
        final ItemStackRenderState item = new ItemStackRenderState();
    }

    public AlembicRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = ItemModelResolver.INSTANCE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AlembicBlockEntity alembic, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(alembic, state, partialTick, cameraPosition, breakProgress);
        Level level = alembic.getLevel();
        state.time = level == null ? 0 : level.getGameTime() + partialTick;
        ItemEssences elements = ResearchItems.of(alembic.input());
        state.working = elements != null && alembic.getBlockState().getValue(AlembicBlock.LIT)
                && alembic.water() >= AlembicBlockEntity.WATER_PER_ITEM;
        EssenceTank tank = alembic.tank();
        // Water, coloured by the essence in it: the more essence, the stronger its colour.
        float share = Math.min(1, tank.amount() / (float) tank.capacity() * 2);
        int color = tank.isEmpty() ? WATER_COLOR : RuneDrawing.mix(WATER_COLOR, tank.contents().color(), 0.35F + 0.65F * share);
        state.color = state.working ? RuneDrawing.mix(color, elements.main().color(), alembic.clientProgress * 0.5F) : color;
        float water = alembic.water() / (float) AlembicBlockEntity.WATER_CAPACITY;
        state.level = Math.max(water, tank.isEmpty() ? 0 : 0.12F);
        itemModelResolver.updateForTopItem(state.item, alembic.input(), ItemDisplayContext.FIXED, level, null, (int) alembic.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float top = BOTTOM + (TOP - BOTTOM) * Math.min(1, state.level);
        if (!state.item.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5F, BOTTOM + 1.6F / 16, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.time * 0.6F));
            poseStack.scale(0.28F, 0.28F, 0.28F);
            state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (state.level <= 0.01F) {
            return;
        }
        float sway = state.working ? Mth.sin(state.time * 0.3F) * 0.15F / 16 : 0;
        LiquidBox.submitCutCorners(poseStack, collector, MIN, MIN, MAX, MAX, CUT, BOTTOM, top + sway, RuneDrawing.argb(state.color, 0.75F),
                0, 0, 0);
        if (state.working) {
            // Bubbles rise from the bottom and burst at the surface, each on its own track.
            float size = 0.45F / 16;
            for (int i = 0; i < BUBBLES; i++) {
                float t = ((state.time * 0.025F + i * 0.37F) % 1 + 1) % 1;
                float y = BOTTOM + 0.2F / 16 + (top - BOTTOM - 0.6F / 16) * t;
                float x = 5.4F / 16 + ((i * 0.618F) % 1) * (5.2F / 16) + Mth.sin(state.time * 0.2F + i) * 0.2F / 16;
                float z = 5.4F / 16 + ((i * 0.381F + 0.5F) % 1) * (5.2F / 16);
                LiquidBox.submit(poseStack, collector, x, z, x + size, z + size, y, y + size, RuneDrawing.argb(0xFFFFFF, 0.75F * (1 - t * 0.5F)));
            }
        }
    }
}
