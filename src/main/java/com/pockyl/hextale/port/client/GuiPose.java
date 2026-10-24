package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * The 26.x 2D GUI pose stack ({@code Matrix3x2fStack}) over the 1.21.1 {@link PoseStack} of a {@code GuiGraphics}.
 */
public final class GuiPose {
    private final PoseStack stack;

    GuiPose(PoseStack stack) {
        this.stack = stack;
    }

    public PoseStack stack() {
        return stack;
    }

    public Matrix4f matrix() {
        return stack.last().pose();
    }

    public GuiPose pushMatrix() {
        stack.pushPose();
        return this;
    }

    public GuiPose popMatrix() {
        stack.popPose();
        return this;
    }

    public GuiPose translate(float x, float y) {
        stack.translate(x, y, 0);
        return this;
    }

    public GuiPose scale(float x, float y) {
        stack.scale(x, y, 1);
        return this;
    }

    public GuiPose scale(float xy) {
        stack.scale(xy, xy, 1);
        return this;
    }

    /** Rotation in radians, clockwise on screen like the 26.x stack. */
    public GuiPose rotate(float angle) {
        stack.mulPose(Axis.ZP.rotation(angle));
        return this;
    }

    public Vector2f transformPosition(float x, float y, Vector2f dest) {
        Vector4f v = matrix().transform(new Vector4f(x, y, 0, 1));
        return dest.set(v.x, v.y);
    }
}
