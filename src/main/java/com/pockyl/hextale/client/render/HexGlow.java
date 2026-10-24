package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;

import java.util.function.Function;

/**
 * Additive light that writes no depth: rune glyphs and sigil rings. Vanilla's lightning type is additive but writes
 * depth, so stacked glow layers in one plane fought over the depth buffer and flickered; translucent types sort their
 * quads, so layers swapped order as the camera moved. Here layers just add up, in any order, with no depth fight.
 *
 * <p>1.21.1: render types over the vanilla lightning and eyes shaders (26.x builds its own pipelines).
 */
public final class HexGlow extends RenderType {
    public static final RenderType TYPE = create("hextale_glow", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
            1536, false, false, CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    /** The same additive light drawn with a texture (entity vertex format): soft sprites like a jet's plume. */
    private static final Function<ResourceLocation, RenderType> TEXTURED = Util.memoize(
            texture -> create("hextale_glow_textured", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_EYES_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL)
                            .createCompositeState(false)));

    private HexGlow(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling, boolean sort,
                    Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }

    public static RenderType textured(ResourceLocation texture) {
        return TEXTURED.apply(texture);
    }
}
