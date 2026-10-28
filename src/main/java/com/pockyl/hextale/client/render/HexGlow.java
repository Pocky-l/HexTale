package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import com.pockyl.hextale.Hextale;

import java.util.function.Function;

/**
 * Additive light that writes no depth: rune glyphs and sigil rings. Vanilla's lightning type is additive but writes
 * depth, so stacked glow layers in one plane fought over the depth buffer and flickered; translucent types sort their
 * quads, so layers swapped order as the camera moved. Here layers just add up, in any order, with no depth fight.
 */
public final class HexGlow {
    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Hextale.id("pipeline/hex_glow"))
            .withVertexShader("core/rendertype_lightning")
            .withFragmentShader("core/rendertype_lightning")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    public static final RenderType TYPE = RenderType.create("hextale_glow",
            RenderSetup.builder(PIPELINE).createRenderSetup());

    /** The same additive light drawn with a texture (entity vertex format): soft sprites like a jet's plume. */
    public static final RenderPipeline TEXTURED_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Hextale.id("pipeline/hex_glow_textured"))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    private static final Function<Identifier, RenderType> TEXTURED = Util.memoize(
            texture -> RenderType.create("hextale_glow_textured", RenderSetup.builder(TEXTURED_PIPELINE)
                    .withTexture("Sampler0", texture).createRenderSetup()));

    private HexGlow() {
    }

    public static RenderType textured(Identifier texture) {
        return TEXTURED.apply(texture);
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
        event.registerPipeline(TEXTURED_PIPELINE);
    }
}
