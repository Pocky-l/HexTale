package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.joml.Matrix3x2f;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.RuneGlyphs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Runes on screens, drawn smooth (owner, 2026-10-29: the arcane script, never on a pixel grid). Each rune is rendered
 * once per screen size into two white masks of real screen pixels - the body (brush strokes that swell in the middle and
 * thin at the ends, round dots, a soft glow round them) and the light core inside the strokes - and both are blitted over
 * its box, tinted with the ink's colour and a lighter one. The masks carry no colour, so one rune in any colour (hover,
 * chosen, dimmed) costs one bake. The glow spills a little past the box.
 * <p>
 * The masks are painted on a background thread and uploaded when ready: a screen full of runes never stalls a frame (the
 * stele's codex shows 72 at once and lagged on opening while they were painted on the render thread); a rune not ready
 * yet is simply not drawn for a frame or two.
 */
public final class RuneIcons {
    /** Share of the box the glow may spill past each side. */
    private static final float SPILL = 0.25F;
    /** Glow beyond this many glow radii is too faint to paint (0.5 * e^-9.7). */
    private static final float GLOW_REACH = 2.2F;
    private static final Map<String, Identifier[]> TEXTURES = new HashMap<>();
    /** Masks being painted in the background, by the same key. */
    private static final Map<String, CompletableFuture<float[][]>> PAINTING = new HashMap<>();

    private RuneIcons() {
    }

    /** A rune's sign in a {@code size} x {@code size} box at {@code x, y} (top left), in the colour {@code argb}. */
    public static void draw(GuiGraphicsExtractor graphics, ComplexRune rune, int x, int y, int size, int argb) {
        PixelArt.flush(graphics);
        int spill = Math.max(1, Math.round(size * SPILL));
        int box = size + spill * 2;
        int pixels = Math.max(8, Math.round(box * screenScale(graphics)));
        Identifier[] textures = textures(rune, pixels, (float) size / box);
        if (textures == null) {
            return;
        }
        int alpha = ARGB.alpha(argb);
        int rgb = argb & 0xFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, textures[0], x - spill, y - spill, 0, 0, box, box, pixels, pixels, pixels, pixels,
                ARGB.color(alpha, rgb));
        graphics.blit(RenderPipelines.GUI_TEXTURED, textures[1], x - spill, y - spill, 0, 0, box, box, pixels, pixels, pixels, pixels,
                ARGB.color(alpha, mix(rgb, 0xFFFFFF, 0.6F)));
    }

    /** The rune's body and core textures at this size; null while its masks are still being painted. */
    private static Identifier[] textures(ComplexRune rune, int pixels, float glyphShare) {
        String key = rune.index() + "_" + pixels;
        Identifier[] textures = TEXTURES.get(key);
        if (textures != null) {
            return textures;
        }
        List<float[]> strokes = RuneGlyphs.glyph(rune);
        CompletableFuture<float[][]> painting = PAINTING.computeIfAbsent(key,
                k -> CompletableFuture.supplyAsync(() -> mask(strokes, pixels, glyphShare), Util.backgroundExecutor()));
        if (!painting.isDone()) {
            return null;
        }
        PAINTING.remove(key);
        float[][] masks = painting.join();
        textures = new Identifier[]{upload(key + "_body", masks[0], pixels), upload(key + "_core", masks[1], pixels)};
        TEXTURES.put(key, textures);
        return textures;
    }

    /** Screen pixels per GUI unit where the box is drawn: the window's GUI scale times the pose's own scale. */
    private static float screenScale(GuiGraphicsExtractor graphics) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        float poseScale = (float) Math.sqrt(pose.m00() * pose.m00() + pose.m01() * pose.m01());
        return Minecraft.getInstance().getWindow().getGuiScale() * Math.max(0.25F, poseScale);
    }

    private static Identifier upload(String key, float[] cover, int pixels) {
        NativeImage image = new NativeImage(pixels, pixels, true);
        for (int py = 0; py < pixels; py++) {
            for (int px = 0; px < pixels; px++) {
                float a = cover[py * pixels + px];
                if (a > 0.003F) {
                    image.setPixel(px, py, ARGB.color(Math.round(a * 255), 0xFFFFFF));
                }
            }
        }
        Identifier id = Hextale.id("rune_icon/" + key);
        Minecraft.getInstance().getTextureManager().register(id, new SmoothTexture(id.toString(), image));
        return id;
    }

    /**
     * The cover of the rune's two layers per pixel ({@code [y * pixels + x]}): [0] the body = the glow under the ink,
     * [1] the light core over it. The distance to the nearest stroke gives the ink (a brush that is widest halfway along
     * each stroke), the glow around it and the core inside it. Every segment only visits the pixels its brush and glow
     * can reach.
     */
    static float[][] mask(List<float[]> strokes, int pixels, float glyphShare) {
        float glyph = pixels * glyphShare;
        float offset = (pixels - glyph) / 2;
        float brush = Math.max(1.3F, glyph * 0.055F);
        float glowRadius = Math.max(1.6F, glyph * 0.11F);
        int area = pixels * pixels;
        float[] ink = new float[area];
        float[] light = new float[area];
        float[] nearest = new float[area];
        Arrays.fill(nearest, Float.MAX_VALUE);
        for (float[] stroke : strokes) {
            // The stroke in pixel space, with each point's position along it (0-1) for the brush's swell.
            int points = stroke.length / 2;
            float[] p = new float[stroke.length];
            float[] along = new float[points];
            float length = 0;
            for (int i = 0; i < points; i++) {
                p[i * 2] = offset + stroke[i * 2] * glyph;
                p[i * 2 + 1] = offset + stroke[i * 2 + 1] * glyph;
                if (i > 0) {
                    length += (float) Math.sqrt(sq(p[i * 2] - p[i * 2 - 2]) + sq(p[i * 2 + 1] - p[i * 2 - 1]));
                }
                along[i] = length;
            }
            for (int i = 0; i < points; i++) {
                along[i] = length > 0 ? along[i] / length : 0.5F;
            }
            if (RuneGlyphs.isDot(stroke)) {
                float dx = (p[0] + p[p.length - 2]) / 2;
                float dy = (p[1] + p[p.length - 1]) / 2;
                float radius = brush * 0.95F;
                float reach = radius + glowRadius * GLOW_REACH;
                int x0 = Math.max(0, (int) (dx - reach));
                int x1 = Math.min(pixels - 1, (int) (dx + reach) + 1);
                int y0 = Math.max(0, (int) (dy - reach));
                int y1 = Math.min(pixels - 1, (int) (dy + reach) + 1);
                for (int py = y0; py <= y1; py++) {
                    for (int px = x0; px <= x1; px++) {
                        int at = py * pixels + px;
                        float d = (float) Math.sqrt(sq(px + 0.5F - dx) + sq(py + 0.5F - dy));
                        nearest[at] = Math.min(nearest[at], Math.max(0, d - radius));
                        ink[at] = Math.max(ink[at], clamp(radius + 0.5F - d));
                        light[at] = Math.max(light[at], clamp(radius * 0.45F + 0.5F - d));
                    }
                }
                continue;
            }
            float reach = brush * 0.7F + glowRadius * GLOW_REACH;
            for (int i = 0; i + 3 < p.length; i += 2) {
                float ax = p[i];
                float ay = p[i + 1];
                float dx = p[i + 2] - ax;
                float dy = p[i + 3] - ay;
                float lengthSq = dx * dx + dy * dy;
                float from = along[i / 2];
                float span = along[i / 2 + 1] - from;
                int x0 = Math.max(0, (int) (Math.min(ax, ax + dx) - reach));
                int x1 = Math.min(pixels - 1, (int) (Math.max(ax, ax + dx) + reach) + 1);
                int y0 = Math.max(0, (int) (Math.min(ay, ay + dy) - reach));
                int y1 = Math.min(pixels - 1, (int) (Math.max(ay, ay + dy) + reach) + 1);
                for (int py = y0; py <= y1; py++) {
                    float cy = py + 0.5F;
                    for (int px = x0; px <= x1; px++) {
                        float cx = px + 0.5F;
                        float t = lengthSq > 0 ? clamp(((cx - ax) * dx + (cy - ay) * dy) / lengthSq) : 0;
                        float d = (float) Math.sqrt(sq(cx - (ax + dx * t)) + sq(cy - (ay + dy * t)));
                        if (d > reach) {
                            continue;
                        }
                        float half = brush * (0.32F + 0.38F * (float) Math.sin(Math.PI * (from + span * t)));
                        int at = py * pixels + px;
                        nearest[at] = Math.min(nearest[at], Math.max(0, d - half));
                        ink[at] = Math.max(ink[at], clamp(half + 0.5F - d));
                        light[at] = Math.max(light[at], clamp(half * 0.4F + 0.5F - d));
                    }
                }
            }
        }
        // The body: the glow with the ink over it; the core goes over the body when drawn.
        float[] body = new float[area];
        for (int at = 0; at < area; at++) {
            float n = nearest[at];
            float glow = n == Float.MAX_VALUE ? 0 : 0.5F * (float) Math.exp(-(n / glowRadius) * (n / glowRadius) * 2);
            body[at] = glow * (1 - ink[at]) + ink[at];
        }
        return new float[][]{body, light};
    }

    private static float sq(float value) {
        return value * value;
    }

    private static float clamp(float value) {
        return value < 0 ? 0 : value > 1 ? 1 : value;
    }

    private static int mix(int a, int b, float t) {
        return ARGB.color(0, Math.round(ARGB.red(a) + (ARGB.red(b) - ARGB.red(a)) * t), Math.round(ARGB.green(a) + (ARGB.green(b) - ARGB.green(a)) * t),
                Math.round(ARGB.blue(a) + (ARGB.blue(b) - ARGB.blue(a)) * t)) & 0xFFFFFF;
    }

    /** A rune texture sampled smoothly, so it stays soft under any pose scale. */
    private static final class SmoothTexture extends DynamicTexture {
        SmoothTexture(String label, NativeImage image) {
            super(() -> label, image);
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
    }
}
