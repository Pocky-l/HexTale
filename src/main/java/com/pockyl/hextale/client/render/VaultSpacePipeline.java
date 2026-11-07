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
 * The space behind a Hex Vault's door: one opaque hexagon whose depth is painted by {@code shaders/core/vault_space.fsh}. UV0 =
 * the point in the door's own plane (blocks, middle = 0); Color: r = how far the door is open, g = how full the vault is.
 */
public final class VaultSpacePipeline {
    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Hextale.id("pipeline/vault_space"))
            .withVertexShader(Hextale.id("core/vault_space"))
            .withFragmentShader(Hextale.id("core/vault_space"))
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withCull(false)
            .build();
    public static final RenderType TYPE = RenderType.create("hextale_vault_space", RenderSetup.builder(PIPELINE).createRenderSetup());

    private VaultSpacePipeline() {
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }
}
