package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import com.pockyl.hextale.Hextale;

/**
 * The Void's two passes, both with our shaders and the entity vertex format (see {@code shaders/core/the_void.vsh} for
 * what every attribute carries). BODY: the cells of the breach, opaque and depth-writing like the end portal - the
 * depth behind them is painted by the shader. HALO: the glow around the outline, additive and writing no depth, drawn
 * on the free cells next to the breach.
 */
public final class VoidPipelines {
    public static final RenderPipeline BODY = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Hextale.id("pipeline/the_void"))
            .withVertexShader(Hextale.id("core/the_void"))
            .withFragmentShader(Hextale.id("core/the_void"))
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withCull(false)
            .build();
    public static final RenderPipeline HALO = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Hextale.id("pipeline/the_void_halo"))
            .withVertexShader(Hextale.id("core/the_void"))
            .withFragmentShader(Hextale.id("core/the_void_halo"))
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    public static final RenderType BODY_TYPE = RenderType.create("hextale_the_void", RenderSetup.builder(BODY).createRenderSetup());
    public static final RenderType HALO_TYPE = RenderType.create("hextale_the_void_halo", RenderSetup.builder(HALO).createRenderSetup());

    private VoidPipelines() {
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(BODY);
        event.registerPipeline(HALO);
    }
}
