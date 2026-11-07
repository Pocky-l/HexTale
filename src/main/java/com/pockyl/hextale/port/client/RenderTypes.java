package com.pockyl.hextale.port.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The 26.x render type factory names over the 1.21.1 {@link RenderType} statics.
 *
 * <p>port-of: net.minecraft.client.renderer.rendertype.RenderTypes
 */
public final class RenderTypes {
    private RenderTypes() {
    }

    public static RenderType entityCutout(ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }

    /** 26.x's culling cutout is what 1.21.1 calls plain {@code entityCutout}. */
    public static RenderType entityCutoutCull(ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }

    public static RenderType entityTranslucent(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    public static RenderType itemTranslucent(ResourceLocation texture) {
        return RenderType.itemEntityTranslucentCull(texture);
    }

    /** Painted over with the texture's alpha, as 26.x does (1.21.1's own eyes type adds its colour). */
    public static RenderType eyes(ResourceLocation texture) {
        return EyesRenderType.of(texture);
    }

    public static RenderType entitySolid(ResourceLocation texture) {
        return RenderType.entitySolid(texture);
    }
}
