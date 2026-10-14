package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.RuneGlyphs;

/**
 * Draws a rune scroll as its parchment sprite with the rune's hieroglyph written on it in vector ink: a dark outline
 * and a glowing core in the category colour, on the front and (mirrored, like the sprite) on the back.
 */
public class RuneScrollRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation WHITE = Hextale.id("textures/misc/white.png");
    /** Glyph box on the parchment of the sprite, in model units. */
    private static final float GLYPH_SIZE = 7.2F / 16;
    private static final float GLYPH_X = 8.0F / 16;
    private static final float GLYPH_Y = 8.0F / 16;
    private static final float FRONT = 8.5F / 16 + 0.002F;
    private static final float BACK = 7.5F / 16 - 0.002F;

    public RuneScrollRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light,
                             int overlay) {
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        items.renderModelLists(TableModels.get(TableModels.SCROLL_ITEM), stack, light, overlay, poseStack,
                ItemRenderer.getFoilBufferDirect(buffers, Sheets.translucentItemSheet(), true, stack.hasFoil()));
        ComplexRune rune = RuneScrollItem.rune(stack);
        if (rune == null) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        int outline = 0xFF000000 | RuneDrawing.darken(rune.color(), 0.28F);
        int core = 0xFF000000 | rune.color();
        for (float z : new float[]{FRONT, BACK}) {
            for (float[] stroke : RuneGlyphs.glyph(rune)) {
                polyline(consumer, poseStack, stroke, 1.05F / 16, outline, light, z, z == BACK);
            }
            float coreZ = z == FRONT ? z + 0.001F : z - 0.001F;
            for (float[] stroke : RuneGlyphs.glyph(rune)) {
                polyline(consumer, poseStack, stroke, 0.5F / 16, core, LightTexture.FULL_BRIGHT, coreZ, z == BACK);
            }
        }
    }

    /** A thick polyline of quads with square-ish joints filled by small octagons. */
    private static void polyline(VertexConsumer consumer, PoseStack poseStack, float[] stroke, float width, int color, int light, float z,
                                 boolean mirrored) {
        float half = width / 2;
        Matrix4f pose = poseStack.last().pose();
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
            quad(consumer, poseStack, pose, x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny, z, color, light);
        }
        for (int i = 0; i + 1 < stroke.length; i += 2) {
            float x = mapX(stroke[i], mirrored);
            float y = mapY(stroke[i + 1]);
            for (int k = 0; k < 8; k += 2) {
                float a0 = Mth.TWO_PI * k / 8;
                float a1 = Mth.TWO_PI * (k + 1) / 8;
                float a2 = Mth.TWO_PI * (k + 2) / 8;
                quad(consumer, poseStack, pose, x, y, x + Mth.cos(a0) * half, y + Mth.sin(a0) * half, x + Mth.cos(a1) * half,
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

    private static void quad(VertexConsumer consumer, PoseStack poseStack, Matrix4f pose, float x0, float y0, float x1, float y1, float x2,
                             float y2, float x3, float y3, float z, int color, int light) {
        vertex(consumer, poseStack, pose, x0, y0, z, color, light);
        vertex(consumer, poseStack, pose, x1, y1, z, color, light);
        vertex(consumer, poseStack, pose, x2, y2, z, color, light);
        vertex(consumer, poseStack, pose, x3, y3, z, color, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack poseStack, Matrix4f pose, float x, float y, float z, int color, int light) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(0.5F, 0.5F).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(poseStack.last(), 0, 0, 1);
    }
}
