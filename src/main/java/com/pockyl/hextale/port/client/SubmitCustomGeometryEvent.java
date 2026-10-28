package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 26.x's hook for custom world geometry, posted on the game bus from the 1.21.1 level renderer after the particles
 * (camera-relative pose, the camera's turn is on the model view matrix); what is drawn into the collector is flushed
 * right after the listeners ran.
 *
 * <p>port-of: net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent
 */
public class SubmitCustomGeometryEvent extends Event {
    private final LevelRenderState levelRenderState;
    private final PoseStack poseStack;
    private final SubmitNodeCollector collector;

    private SubmitCustomGeometryEvent(LevelRenderState levelRenderState, PoseStack poseStack, SubmitNodeCollector collector) {
        this.levelRenderState = levelRenderState;
        this.poseStack = poseStack;
        this.collector = collector;
    }

    public LevelRenderState getLevelRenderState() {
        return levelRenderState;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public SubmitNodeCollector getSubmitNodeCollector() {
        return collector;
    }

    /** Wires the event to the level renderer (once, from the client set-up). */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent::onStage);
    }

    private static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        NeoForge.EVENT_BUS.post(new SubmitCustomGeometryEvent(LevelRenderState.current(partialTick), poseStack, new SubmitNodeCollector(buffers)));
        poseStack.popPose();
        buffers.endBatch();
    }
}
