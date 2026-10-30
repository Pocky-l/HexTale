package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import com.pockyl.hextale.Hextale;

import java.util.HashMap;
import java.util.Map;

/**
 * Concentric translucent pixel discs ({@link PixelArt#disc} fills, drawn one over another) baked once into a texture and
 * blitted as one quad. The diary's hazes are a few wide faint discs per step: as runs they were thousands of quads a
 * frame; baked, they look the same pixel for pixel (the layers are blended exactly as the screen would blend them).
 */
public final class DiscStack {
    private static final Map<String, Baked> TEXTURES = new HashMap<>();

    private record Baked(Identifier id, int size) {
    }

    private DiscStack() {
    }

    /**
     * Draws the discs centred on the pixel corner {@code cx, cy}, in the given order (the first lies at the bottom).
     * {@code diameters[i]} is filled with {@code argb[i]}.
     */
    public static void draw(GuiGraphicsExtractor graphics, int cx, int cy, int[] diameters, int[] argb) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < diameters.length; i++) {
            key.append(diameters[i]).append(':').append(Integer.toHexString(argb[i])).append('_');
        }
        Baked baked = TEXTURES.computeIfAbsent(key.toString(), k -> bake(diameters, argb));
        int r = baked.size / 2;
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, baked.id, cx - r, cy - r, 0, 0, baked.size, baked.size, baked.size, baked.size);
    }

    private static Baked bake(int[] diameters, int[] argb) {
        int big = 0;
        for (int diameter : diameters) {
            big = Math.max(big, diameter / 2);
        }
        int size = Math.max(2, big * 2);
        // Straight (not premultiplied) colour and alpha of each pixel after every layer, blended "over" like the GUI does.
        float[] red = new float[size * size];
        float[] green = new float[size * size];
        float[] blue = new float[size * size];
        float[] alpha = new float[size * size];
        for (int i = 0; i < diameters.length; i++) {
            int r = diameters[i] / 2;
            float a = ARGB.alpha(argb[i]) / 255.0F;
            float cr = ARGB.red(argb[i]);
            float cg = ARGB.green(argb[i]);
            float cb = ARGB.blue(argb[i]);
            for (int row = 0; row < r * 2; row++) {
                for (int col = 0; col < r * 2; col++) {
                    // The same test as PixelArt.Circle's outer mask.
                    float x = col - r + 0.5F;
                    float y = row - r + 0.5F;
                    if (x * x + y * y > r * r) {
                        continue;
                    }
                    int p = (row + big - r) * size + col + big - r;
                    float under = alpha[p] * (1 - a);
                    float out = a + under;
                    red[p] = (cr * a + red[p] * under) / out;
                    green[p] = (cg * a + green[p] * under) / out;
                    blue[p] = (cb * a + blue[p] * under) / out;
                    alpha[p] = out;
                }
            }
        }
        NativeImage image = new NativeImage(size, size, true);
        for (int p = 0; p < size * size; p++) {
            image.setPixel(p % size, p / size, alpha[p] <= 0 ? 0
                    : ARGB.color(Math.round(alpha[p] * 255), Math.round(red[p]), Math.round(green[p]), Math.round(blue[p])));
        }
        Identifier id = Hextale.id("disc_stack/" + TEXTURES.size());
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
        return new Baked(id, size);
    }
}
