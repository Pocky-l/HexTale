package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.function.BiConsumer;

/**
 * The Void and the Hex Vault's gate while a shader pack is on: a pack runs its own programs for everything drawn inside
 * the world, and ours cannot be one of them (they read data, not colours, from the vertices). 26.x keeps what is seen in
 * the frame and draws it with our own shaders once the pack has finished the picture.
 *
 * <p>1.21.1: Iris finishes its picture after the last level render event, so there is no point to draw over it; the
 * breach keeps its plain look (a starry dark with a glowing rim) and the gate its plain door while a pack is on.
 */
public final class VoidOverlay {
    private VoidOverlay() {
    }

    public static boolean works() {
        return false;
    }

    /** One breach: its cells and its glow. */
    static void queue(PoseStack.Pose pose, BiConsumer<VertexConsumer, PoseStack.Pose> body, BiConsumer<VertexConsumer, PoseStack.Pose> halo) {
    }

    /** The space behind one Hex Vault's door. */
    static void queueSpace(PoseStack.Pose pose, BiConsumer<VertexConsumer, PoseStack.Pose> body) {
    }

    /** Additive light in {@link HexGlow}'s format, position and colour. */
    static void queueLight(PoseStack.Pose pose, BiConsumer<VertexConsumer, PoseStack.Pose> light) {
    }
}
