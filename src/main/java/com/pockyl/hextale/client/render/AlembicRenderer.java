package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.AlembicBlock;
import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ResearchItems;

/**
 * The ink inside the Alembic's flask: a glowing liquid in the colour of what is being distilled (or of the ink waiting
 * in its tanks), rising while an item dissolves and swaying a little on its surface.
 */
public class AlembicRenderer implements BlockEntityRenderer<AlembicBlockEntity, AlembicRenderer.State> {
    private static final Identifier WHITE_TEXTURE = Hextale.id("textures/misc/white.png");
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
        state.color = state.working ? RuneDrawing.mix(alembic.strongestInk(), elements.main().color(), alembic.clientProgress)
                : alembic.strongestInk();
        float fill = alembic.fill();
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
        int argb = RuneDrawing.argb(state.color, alpha);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(WHITE_TEXTURE), (pose, consumer) -> {
            float[][] corners = {{MIN, MIN}, {MAX, MIN}, {MAX, MAX}, {MIN, MAX}};
            // Surface (both windings, seen from above and through the glass from below).
            for (int i = 0; i < 4; i++) {
                consumer.addVertex(pose, corners[i][0], top, corners[i][1]).setColor(argb).setUv(0, 0)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
            }
            for (int i = 3; i >= 0; i--) {
                consumer.addVertex(pose, corners[i][0], top, corners[i][1]).setColor(argb).setUv(0, 0)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, -1, 0);
            }
            // Sides.
            for (int i = 0; i < 4; i++) {
                float[] a = corners[i];
                float[] b = corners[(i + 1) % 4];
                for (boolean outside : new boolean[]{true, false}) {
                    float[][] quad = outside ? new float[][]{{a[0], BOTTOM, a[1]}, {b[0], BOTTOM, b[1]}, {b[0], top, b[1]}, {a[0], top, a[1]}}
                            : new float[][]{{a[0], top, a[1]}, {b[0], top, b[1]}, {b[0], BOTTOM, b[1]}, {a[0], BOTTOM, a[1]}};
                    for (float[] v : quad) {
                        consumer.addVertex(pose, v[0], v[1], v[2]).setColor(argb).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
                    }
                }
            }
        });
    }
}
