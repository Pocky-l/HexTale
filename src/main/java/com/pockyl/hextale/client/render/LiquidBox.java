package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

import com.pockyl.hextale.Hextale;

/**
 * A glowing liquid filling a box inside a glass vessel: translucent, full bright, in one colour. Coordinates are in
 * block units. Every face is drawn with both windings, so the liquid shows through the glass from any side.
 */
public final class LiquidBox {
    private static final Identifier WHITE_TEXTURE = Hextale.id("textures/misc/white.png");

    private LiquidBox() {
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, float minX, float minZ, float maxX, float maxZ,
                              float bottom, float top, int argb) {
        if (top - bottom <= 0.001F) {
            return;
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(WHITE_TEXTURE), (pose, consumer) -> {
            float[][] corners = {{minX, minZ}, {maxX, minZ}, {maxX, maxZ}, {minX, maxZ}};
            for (int i = 0; i < 4; i++) {
                vertex(consumer, pose, corners[i][0], top, corners[i][1], argb, 1);
            }
            for (int i = 3; i >= 0; i--) {
                vertex(consumer, pose, corners[i][0], top, corners[i][1], argb, -1);
            }
            for (int i = 0; i < 4; i++) {
                float[] a = corners[i];
                float[] b = corners[(i + 1) % 4];
                for (boolean outside : new boolean[]{true, false}) {
                    float[][] quad = outside ? new float[][]{{a[0], bottom, a[1]}, {b[0], bottom, b[1]}, {b[0], top, b[1]}, {a[0], top, a[1]}}
                            : new float[][]{{a[0], top, a[1]}, {b[0], top, b[1]}, {b[0], bottom, b[1]}, {a[0], bottom, a[1]}};
                    for (float[] v : quad) {
                        consumer.addVertex(pose, v[0], v[1], v[2]).setColor(argb).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
                    }
                }
            }
        });
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int argb, int normalY) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, normalY, 0);
    }
}
