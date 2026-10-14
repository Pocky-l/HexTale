package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Vector ink for screens: thick polylines with round joins and caps, built from triangles in GUI coordinates, so
 * runes stay crisp at every GUI scale. A soft one-pixel fringe smooths the edges; the glow variant fades to nothing
 * and blends additively, like light on parchment.
 */
public final class InkRenderer {
    private static final RenderType INK = RenderType.create("hextale_ink", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES, 1 << 16, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_GUI_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final RenderType GLOW = RenderType.create("hextale_ink_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES, 1 << 16, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_GUI_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    /** Width of the anti-aliasing fringe in GUI pixels. */
    private static final float FRINGE = 0.55F;

    private final GuiGraphics graphics;
    private final Matrix4f pose;

    private InkRenderer(GuiGraphics graphics) {
        this.graphics = graphics;
        this.pose = graphics.pose().last().pose();
    }

    /** Switching between the two render types draws the previous batch, which keeps the drawing order. */
    private VertexConsumer buffer(boolean additive) {
        return graphics.bufferSource().getBuffer(additive ? GLOW : INK);
    }

    /** Starts a batch with the current pose; call {@link #end()} to draw it before anything else goes on top. */
    public static InkRenderer begin(GuiGraphics graphics) {
        graphics.flush();
        return new InkRenderer(graphics);
    }

    public void end() {
        graphics.flush();
    }

    /**
     * A solid stroke.
     *
     * @param points {@code [x0, y0, x1, y1, ...]} in GUI coordinates
     * @param color  ARGB
     */
    public void stroke(float[] points, int count, float width, int color) {
        VertexConsumer ink = buffer(false);
        float half = Math.max(0.3F, width / 2);
        int fringe = color & 0x00FFFFFF;
        for (int i = 0; i + 3 < count * 2; i += 2) {
            segment(ink, points[i], points[i + 1], points[i + 2], points[i + 3], half, color, color, 0);
            segment(ink, points[i], points[i + 1], points[i + 2], points[i + 3], half + FRINGE, color, fringe, half);
        }
        for (int i = 0; i < count * 2; i += 2) {
            disc(ink, points[i], points[i + 1], half, color, color);
            ring(ink, points[i], points[i + 1], half, half + FRINGE, color, fringe);
        }
    }

    public void stroke(float[] points, float width, int color) {
        stroke(points, points.length / 2, width, color);
    }

    /** A soft additive halo around a stroke: brightest on the line, fading out at {@code width / 2}. */
    public void glow(float[] points, float width, int color) {
        VertexConsumer glow = buffer(true);
        float half = width / 2;
        int edge = color & 0x00FFFFFF;
        int count = points.length / 2;
        for (int i = 0; i + 3 < count * 2; i += 2) {
            segment(glow, points[i], points[i + 1], points[i + 2], points[i + 3], half, color, edge, 0);
        }
        for (int i = 0; i < count * 2; i += 2) {
            disc(glow, points[i], points[i + 1], half, color, edge);
        }
    }

    /** A filled disc, optionally soft: the colour goes from {@code inner} at the centre to {@code outer} at the rim. */
    public void disc(float x, float y, float radius, int inner, int outer, boolean additive) {
        disc(buffer(additive), x, y, radius, inner, outer);
    }

    /** A ring between two radii, colours from inside to outside. */
    public void ring(float x, float y, float from, float to, int inner, int outer, boolean additive) {
        ring(buffer(additive), x, y, from, to, inner, outer);
    }

    /** An axis-aligned rectangle, for overlays drawn in the same batch. */
    public void rect(float x0, float y0, float x1, float y1, int color) {
        VertexConsumer ink = buffer(false);
        vertex(ink, x0, y0, color);
        vertex(ink, x1, y0, color);
        vertex(ink, x1, y1, color);
        vertex(ink, x0, y0, color);
        vertex(ink, x1, y1, color);
        vertex(ink, x0, y1, color);
    }

    /**
     * A band along the segment between distances {@code from} and {@code half} on both sides of the line: colour
     * {@code inner} at {@code from}, {@code outer} at {@code half}.
     */
    private void segment(VertexConsumer consumer, float x0, float y0, float x1, float y1, float half, int inner, int outer,
                         float from) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4F) {
            return;
        }
        float nx = -dy / length;
        float ny = dx / length;
        for (int side = -1; side <= 1; side += 2) {
            float ax = nx * from * side;
            float ay = ny * from * side;
            float bx = nx * half * side;
            float by = ny * half * side;
            vertex(consumer, x0 + ax, y0 + ay, inner);
            vertex(consumer, x1 + ax, y1 + ay, inner);
            vertex(consumer, x1 + bx, y1 + by, outer);
            vertex(consumer, x0 + ax, y0 + ay, inner);
            vertex(consumer, x1 + bx, y1 + by, outer);
            vertex(consumer, x0 + bx, y0 + by, outer);
        }
    }

    private void disc(VertexConsumer consumer, float x, float y, float radius, int inner, int outer) {
        int steps = sides(radius);
        for (int k = 0; k < steps; k++) {
            float a0 = Mth.TWO_PI * k / steps;
            float a1 = Mth.TWO_PI * (k + 1) / steps;
            vertex(consumer, x, y, inner);
            vertex(consumer, x + Mth.cos(a0) * radius, y + Mth.sin(a0) * radius, outer);
            vertex(consumer, x + Mth.cos(a1) * radius, y + Mth.sin(a1) * radius, outer);
        }
    }

    private void ring(VertexConsumer consumer, float x, float y, float from, float to, int inner, int outer) {
        int steps = sides(to);
        for (int k = 0; k < steps; k++) {
            float a0 = Mth.TWO_PI * k / steps;
            float a1 = Mth.TWO_PI * (k + 1) / steps;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            vertex(consumer, x + c0 * from, y + s0 * from, inner);
            vertex(consumer, x + c1 * from, y + s1 * from, inner);
            vertex(consumer, x + c1 * to, y + s1 * to, outer);
            vertex(consumer, x + c0 * from, y + s0 * from, inner);
            vertex(consumer, x + c1 * to, y + s1 * to, outer);
            vertex(consumer, x + c0 * to, y + s0 * to, outer);
        }
    }

    private static int sides(float radius) {
        return Mth.clamp((int) (radius * 3), 8, 40);
    }

    private void vertex(VertexConsumer consumer, float x, float y, int color) {
        consumer.addVertex(pose, x, y, 0).setColor(color);
    }
}
