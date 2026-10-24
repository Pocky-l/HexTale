package com.pockyl.hextale.port.client;

/**
 * The 26.x GUI pipelines the mod's screens pass to blits.
 *
 * <p>port-of: net.minecraft.client.renderer.RenderPipelines
 */
public final class RenderPipelines {
    public static final RenderPipeline GUI = new RenderPipeline("gui");
    public static final RenderPipeline GUI_TEXTURED = new RenderPipeline("gui_textured");
    public static final RenderPipeline VIGNETTE = new RenderPipeline("vignette");

    private RenderPipelines() {
    }
}
