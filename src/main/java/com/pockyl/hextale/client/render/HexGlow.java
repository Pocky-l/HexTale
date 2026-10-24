package com.pockyl.hextale.client.render;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import com.pockyl.hextale.Hextale;

import java.util.function.Function;

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

    /** The same additive light drawn with a texture (entity vertex format): soft sprites like a jet's plume. */
    public static final RenderPipeline TEXTURED_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET,
                    RenderPipelines.EYES_SNIPPET)
            .withLocation(Hextale.id("pipeline/hex_glow_textured"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    private static final Function<ResourceLocation, RenderType> TEXTURED = Util.memoize(
            texture -> RenderType.create("hextale_glow_textured", RenderSetup.builder(TEXTURED_PIPELINE)
                    .setOitPipelines(RenderPipelines.OIT_EYES).withTexture("Sampler0", texture).createRenderSetup()));

    private HexGlow() {
    }

    public static RenderType textured(ResourceLocation texture) {
        return TEXTURED.apply(texture);
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
        event.registerPipeline(TEXTURED_PIPELINE);
    }
}
