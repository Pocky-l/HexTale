package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.RuneGlyphs;

import java.util.function.Consumer;

/**
 * Writes the rune's hieroglyph on a rune scroll in vector ink: a dark outline and a glowing core in the category colour,
 * on the front and (mirrored, like the sprite) on the back. The parchment itself is the item's plain sprite model; the
 * item model definition ({@code items/rune_scroll.json}) puts this special renderer on top of it.
 */
public final class RuneScrollRenderer implements SpecialModelRenderer<ComplexRune> {
    public static final Identifier ID = Hextale.id("rune_scroll");
    private static final Identifier WHITE = Hextale.id("textures/misc/white.png");
    /** Glyph box on the parchment of the sprite, in model units. */
    private static final float GLYPH_SIZE = 7.2F / 16;
    private static final float GLYPH_X = 8.0F / 16;
    private static final float GLYPH_Y = 8.0F / 16;
    private static final float FRONT = 8.5F / 16 + 0.002F;
    private static final float BACK = 7.5F / 16 - 0.002F;

    @Override
    public @Nullable ComplexRune extractArgument(ItemStack stack) {
        return RuneScrollItem.rune(stack);
    }

    @Override
    public void submit(@Nullable ComplexRune rune, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil,
                       int outlineColor) {
        if (rune == null) {
            return;
        }
        int outline = 0xFF000000 | RuneDrawing.darken(rune.color(), 0.28F);
        int core = 0xFF000000 | rune.color();
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(WHITE), (pose, consumer) -> {
            for (float z : new float[]{FRONT, BACK}) {
                for (float[] stroke : RuneGlyphs.glyph(rune)) {
                    polyline(consumer, pose, stroke, 1.05F / 16, outline, light, z, z == BACK);
                }
                float coreZ = z == FRONT ? z + 0.001F : z - 0.001F;
                for (float[] stroke : RuneGlyphs.glyph(rune)) {
                    polyline(consumer, pose, stroke, 0.5F / 16, core, LightCoordsUtil.FULL_BRIGHT, coreZ, z == BACK);
                }
            }
        });
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        float half = GLYPH_SIZE / 2;
        for (float z : new float[]{FRONT, BACK}) {
            output.accept(new Vector3f(GLYPH_X - half, GLYPH_Y - half, z));
            output.accept(new Vector3f(GLYPH_X + half, GLYPH_Y + half, z));
        }
    }

    /** A thick polyline of quads with square-ish joints filled by small octagons. */
    private static void polyline(VertexConsumer consumer, PoseStack.Pose pose, float[] stroke, float width, int color, int light, float z,
                                 boolean mirrored) {
        float half = width / 2;
        for (int i = 0; i + 3 < stroke.length; i += 2) {
            float x0 = mapX(stroke[i], mirrored);
            float y0 = mapY(stroke[i + 1]);
            float x1 = mapX(stroke[i + 2], mirrored);
            float y1 = mapY(stroke[i + 3]);
            float dx = x1 - x0;
            float dy = y1 - y0;
            float length = Mth.sqrt(dx * dx + dy * dy);
            if (length < 1.0E-5F) {
                continue;
            }
            float nx = -dy / length * half;
            float ny = dx / length * half;
            quad(consumer, pose, x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny, z, color, light);
        }
        for (int i = 0; i + 1 < stroke.length; i += 2) {
            float x = mapX(stroke[i], mirrored);
            float y = mapY(stroke[i + 1]);
            for (int k = 0; k < 8; k += 2) {
                float a0 = Mth.TWO_PI * k / 8;
                float a1 = Mth.TWO_PI * (k + 1) / 8;
                float a2 = Mth.TWO_PI * (k + 2) / 8;
                quad(consumer, pose, x, y, x + Mth.cos(a0) * half, y + Mth.sin(a0) * half, x + Mth.cos(a1) * half,
                        y + Mth.sin(a1) * half, x + Mth.cos(a2) * half, y + Mth.sin(a2) * half, z, color, light);
            }
        }
    }

    private static float mapX(float u, boolean mirrored) {
        float x = GLYPH_X + (u - 0.5F) * GLYPH_SIZE;
        return mirrored ? 1 - x : x;
    }

    private static float mapY(float v) {
        return GLYPH_Y - (v - 0.5F) * GLYPH_SIZE;
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float x2, float y2,
                             float x3, float y3, float z, int color, int light) {
        vertex(consumer, pose, x0, y0, z, color, light);
        vertex(consumer, pose, x1, y1, z, color, light);
        vertex(consumer, pose, x2, y2, z, color, light);
        vertex(consumer, pose, x3, y3, z, color, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int color, int light) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, 0, 0, 1);
    }

    /** The JSON side: {@code {"type": "hextale:rune_scroll"}} in an item model definition. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<ComplexRune> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<ComplexRune> bake(SpecialModelRenderer.BakingContext context) {
            return new RuneScrollRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
