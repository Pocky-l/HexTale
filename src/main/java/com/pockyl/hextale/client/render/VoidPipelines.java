package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;

import java.io.IOException;

/**
 * The Void's two passes, both with our shaders and the entity vertex format (see {@code shaders/core/the_void.vsh} for
 * what every attribute carries). BODY: the cells of the breach, opaque and depth-writing like the end portal - the
 * depth behind them is painted by the shader. HALO: the glow around the outline, additive and writing no depth, drawn
 * on the free cells next to the breach.
 *
 * <p>1.21.1: core shader programs registered in {@link RegisterShadersEvent} and render types over them.
 */
public final class VoidPipelines extends RenderType {
    private static @Nullable ShaderInstance bodyShader;
    private static @Nullable ShaderInstance haloShader;

    public static final RenderType BODY_TYPE = create("hextale_the_void", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
            1536, false, false, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> bodyShader))
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    public static final RenderType HALO_TYPE = create("hextale_the_void_halo", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
            1536, false, false, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> haloShader))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private VoidPipelines(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling, boolean sort,
                          Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }

    public static void registerShaders(RegisterShadersEvent event) {
        try {
            register(event);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Hextale.id("the_void"), DefaultVertexFormat.NEW_ENTITY),
                shader -> bodyShader = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Hextale.id("the_void_halo"), DefaultVertexFormat.NEW_ENTITY),
                shader -> haloShader = shader);
    }
}
