package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import com.pockyl.hextale.Hextale;

/**
 * The abyss seen through a rift: our own shader ({@code shaders/core/abyss_rift}). Opaque and depth-writing like the
 * end portal - the depth is painted by the shader (parallax layers behind the plane), and the torn edge is cut out of
 * the mesh with {@code discard}, so it needs no blending and no transparency sorting. Vertices: position, UV = place on
 * the rift's plane in blocks, colour = edge depth / openness / shard flag / random phase (see the vertex shader).
 */
public final class AbyssVoid {
    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Hextale.id("pipeline/abyss_rift"))
            .withVertexShader(Hextale.id("core/abyss_rift"))
            .withFragmentShader(Hextale.id("core/abyss_rift"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withCull(false)
            .build();
    public static final RenderType TYPE = RenderType.create("hextale_abyss_rift", RenderSetup.builder(PIPELINE).createRenderSetup());

    private AbyssVoid() {
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }
}
