package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.function.BiConsumer;

/**
 * The Void while a shader pack is on: a pack runs its own programs for everything drawn inside the world, and ours
 * cannot be one of them (they read data, not colours, from the vertices). 26.x keeps the breaches seen in the frame and
 * draws them with our own shaders once the pack has finished the picture.
 *
 * <p>1.21.1: Iris finishes its picture after the last level render event, so there is no point to draw over it; the
 * breach keeps its plain look (a starry dark with a glowing rim) while a pack is on.
 */
public final class VoidOverlay {
    private VoidOverlay() {
    }

    public static boolean works() {
        return false;
    }

    static void queue(PoseStack.Pose pose, BiConsumer<VertexConsumer, PoseStack.Pose> body, BiConsumer<VertexConsumer, PoseStack.Pose> halo) {
    }
}
