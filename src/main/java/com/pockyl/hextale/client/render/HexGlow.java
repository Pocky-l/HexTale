package com.pockyl.hextale.client.render;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import com.pockyl.hextale.Hextale;

/**
 * Additive light that writes no depth: rune glyphs and sigil rings. Vanilla's lightning type is additive but writes
 * depth, so stacked glow layers in one plane fought over the depth buffer and flickered; translucent types sort their
 * quads, so layers swapped order as the camera moved. Here layers just add up, in any order, with no depth fight.
 */
public final class HexGlow {
    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.LIGHTNING_SNIPPET)
            .withLocation(Hextale.id("pipeline/hex_glow"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    public static final RenderType TYPE = RenderType.create("hextale_glow",
            RenderSetup.builder(PIPELINE).setOitPipelines(RenderPipelines.OIT_LIGHTNING).createRenderSetup());

    private HexGlow() {
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }
}
