package com.pockyl.hextale.port.client;

/**
 * A 26.x GUI render pipeline as a name: on 1.21.1 the {@link GuiGraphicsExtractor} only picks the blending from it.
 *
 * <p>port-of: com.mojang.renderpearl.api.pipeline.RenderPipeline
 */
public final class RenderPipeline {
    private final String name;

    RenderPipeline(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
