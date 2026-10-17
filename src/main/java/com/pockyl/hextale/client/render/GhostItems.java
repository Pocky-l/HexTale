package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws an item see-through: the quads of its model with a translucent item render type and the alpha folded into
 * every vertex colour (vanilla item rendering is always opaque). Items with a special renderer are drawn as usual.
 */
public final class GhostItems {
    private GhostItems() {
    }

    /** Submits the item at the current pose, {@code alpha} 0-1. */
    public static void submit(ItemStackRenderState item, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, float alpha) {
        for (int i = 0; i < item.activeLayerCount; i++) {
            if (item.layers[i].specialRenderer != null) {
                item.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
                return;
            }
        }
        int a = Math.round(Math.max(0, Math.min(1, alpha)) * 255) << 24;
        for (int i = 0; i < item.activeLayerCount; i++) {
            ItemStackRenderState.LayerRenderState layer = item.layers[i];
            int[] tints = layer.tintLayers().toIntArray();
            // One draw per texture atlas the quads sample.
            Map<Identifier, List<BakedQuad>> byAtlas = new LinkedHashMap<>();
            for (BakedQuad quad : layer.quads.all()) {
                byAtlas.computeIfAbsent(quad.materialInfo().sprite().atlasLocation(), key -> new ArrayList<>()).add(quad);
            }
            poseStack.pushPose();
            layer.applyTransform(poseStack.last());
            for (Map.Entry<Identifier, List<BakedQuad>> entry : byAtlas.entrySet()) {
                List<BakedQuad> quads = entry.getValue();
                collector.submitCustomGeometry(poseStack, RenderTypes.itemTranslucent(entry.getKey()), (pose, buffer) -> {
                    QuadInstance instance = new QuadInstance();
                    instance.setLightCoords(lightCoords);
                    instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
                    for (BakedQuad quad : quads) {
                        BakedQuad.MaterialInfo material = quad.materialInfo();
                        int tint = material.isTinted() && material.tintIndex() < tints.length ? tints[material.tintIndex()] : -1;
                        instance.setColor(a | (tint & 0xFFFFFF));
                        buffer.putBakedQuad(pose, quad, instance);
                    }
                });
            }
            poseStack.popPose();
        }
    }
}
