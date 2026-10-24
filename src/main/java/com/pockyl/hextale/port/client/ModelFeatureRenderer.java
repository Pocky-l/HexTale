package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Holder of the 26.x crumbling overlay record that block entity renderers receive.
 *
 * <p>port-of: net.minecraft.client.renderer.feature.ModelFeatureRenderer
 */
public final class ModelFeatureRenderer {
    private ModelFeatureRenderer() {
    }

    public record CrumblingOverlay(int progress, PoseStack.Pose cameraPose) {
    }
}
