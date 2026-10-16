package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.AlembicBlock;
import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ResearchItems;

/**
 * The essence inside the Alembic's flask: a glowing liquid in the colour of what it holds (or of the item boiling),
 * rising while an item boils and swaying a little on its surface.
 */
public class AlembicRenderer implements BlockEntityRenderer<AlembicBlockEntity, AlembicRenderer.State> {
    private static final float MIN = 4.6F / 16;
    private static final float MAX = 11.4F / 16;
    private static final float BOTTOM = 4.4F / 16;
    private static final float TOP = 10.6F / 16;

    public static final class State extends BlockEntityRenderState {
        float level;
        int color;
        float time;
        boolean working;
    }

    public AlembicRenderer(BlockEntityRendererProvider.Context context) {
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
        ItemElements elements = ResearchItems.of(alembic.input());
        state.working = elements != null && alembic.getBlockState().getValue(AlembicBlock.LIT);
        EssenceTank tank = alembic.tank();
        int held = tank.isEmpty() ? elements == null ? 0x7EC8E8 : elements.main().color() : tank.contents().color();
        state.color = state.working ? RuneDrawing.mix(held, elements.main().color(), alembic.clientProgress * 0.5F) : held;
        float fill = tank.amount() / (float) tank.capacity();
        state.level = Math.max(fill, state.working ? 0.15F + 0.6F * alembic.clientProgress : 0);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.level <= 0.01F) {
            return;
        }
        float sway = state.working ? Mth.sin(state.time * 0.3F) * 0.15F / 16 : 0;
        float top = BOTTOM + (TOP - BOTTOM) * Math.min(1, state.level) + sway;
        float alpha = 0.55F + (state.working ? 0.15F * (0.5F + 0.5F * Mth.sin(state.time * 0.2F)) : 0);
        LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, BOTTOM, top, RuneDrawing.argb(state.color, alpha));
    }
}
