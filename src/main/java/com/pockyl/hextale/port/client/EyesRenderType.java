package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * 26.x's eyes layer: full bright and painted over the body with the texture's alpha (its pipeline blends translucent).
 * 1.21.1's {@link RenderType#eyes} adds its colour instead, so a glow texture made for 26.x came out overbright.
 *
 * <p>port-of: net.minecraft.client.renderer.RenderPipelines#EYES
 */
final class EyesRenderType extends RenderType {
    private static final Function<ResourceLocation, RenderType> EYES = Util.memoize(texture -> create("hextale_eyes",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, TRANSIENT_BUFFER_SIZE, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)));

    private EyesRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling,
                           boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }

    static RenderType of(ResourceLocation texture) {
        return EYES.apply(texture);
    }
}
