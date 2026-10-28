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

    public static RenderType entityTranslucentEmissive(ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    public static RenderType itemTranslucent(ResourceLocation texture) {
        return RenderType.itemEntityTranslucentCull(texture);
    }

    public static RenderType eyes(ResourceLocation texture) {
        return RenderType.eyes(texture);
    }
}
