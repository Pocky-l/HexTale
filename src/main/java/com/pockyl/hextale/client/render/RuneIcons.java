package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import com.pockyl.hextale.port.client.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.RuneGlyphs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runes on screens, drawn smooth (owner, 2026-10-29: the arcane script, never on a pixel grid). Each rune is rendered
 * once per screen size into a texture of real screen pixels - brush strokes that swell in the middle and thin at the
 * ends, round dots, a soft glow of the ink's colour and a light core - and blitted over its box. The glow spills a
 * little past the box.
 *
 * <p>1.21.1: the texture is a {@link DynamicTexture} switched to linear filtering and clamped edges after its upload;
 * the alpha goes in through the GUI shader colour.
 */
public final class RuneIcons {
    /** Share of the box the glow may spill past each side. */
    private static final float SPILL = 0.25F;
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    private RuneIcons() {
    }

    /** A rune's sign in a {@code size} x {@code size} box at {@code x, y} (top left), in the colour {@code argb}. */
    public static void draw(GuiGraphicsExtractor graphics, ComplexRune rune, int x, int y, int size, int argb) {
        PixelArt.flush(graphics);
        int spill = Math.max(1, Math.round(size * SPILL));
        int box = size + spill * 2;
        int pixels = Math.max(8, Math.round(box * screenScale(graphics)));
        int rgb = argb & 0xFFFFFF;
        ResourceLocation texture = TEXTURES.computeIfAbsent(rune.index() + "_" + Integer.toHexString(rgb) + "_" + pixels,
                key -> upload(key, RuneGlyphs.glyph(rune), pixels, (float) size / box, rgb));
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x - spill, y - spill, 0, 0, box, box, pixels, pixels, pixels, pixels,
                FastColor.ARGB32.alpha(argb) << 24 | 0xFFFFFF);
    }

    /** Screen pixels per GUI unit where the box is drawn: the window's GUI scale times the pose's own scale. */
    private static float screenScale(GuiGraphicsExtractor graphics) {
        Matrix4f pose = graphics.pose().matrix();
        float poseScale = (float) Math.sqrt(pose.m00() * pose.m00() + pose.m01() * pose.m01());
        return (float) Minecraft.getInstance().getWindow().getGuiScale() * Math.max(0.25F, poseScale);
    }

    private static ResourceLocation upload(String key, List<float[]> strokes, int pixels, float glyphShare, int rgb) {
        NativeImage image = new NativeImage(pixels, pixels, true);
        paint(image, strokes, pixels, glyphShare, rgb);
        ResourceLocation id = Hextale.id("rune_icon/" + key);
        Minecraft.getInstance().getTextureManager().register(id, new SmoothTexture(image));
        return id;
    }

    /**
     * Paints the strokes: for every pixel the distance to the nearest stroke gives the ink's cover (a brush that is
     * widest halfway along each stroke), the glow around it and the light core inside it.
     */
    static void paint(NativeImage image, List<float[]> strokes, int pixels, float glyphShare, int rgb) {
        float glyph = pixels * glyphShare;
        float offset = (pixels - glyph) / 2;
        float brush = Math.max(1.3F, glyph * 0.055F);
        float glowRadius = Math.max(1.6F, glyph * 0.11F);
        int core = mix(rgb, 0xFFFFFF, 0.6F);
        // Strokes into pixel space, with each point's position along its stroke (0-1) for the brush's swell.
        int count = strokes.size();
        float[][] xs = new float[count][];
        float[][] along = new float[count][];
        boolean[] dots = new boolean[count];
        for (int s = 0; s < count; s++) {
            float[] stroke = strokes.get(s);
            int points = stroke.length / 2;
            xs[s] = new float[stroke.length];
            along[s] = new float[points];
            float length = 0;
            for (int i = 0; i < points; i++) {
                xs[s][i * 2] = offset + stroke[i * 2] * glyph;
                xs[s][i * 2 + 1] = offset + stroke[i * 2 + 1] * glyph;
                if (i > 0) {
                    length += (float) Math.hypot(xs[s][i * 2] - xs[s][i * 2 - 2], xs[s][i * 2 + 1] - xs[s][i * 2 - 1]);
                }
                along[s][i] = length;
            }
            for (int i = 0; i < points; i++) {
                along[s][i] = length > 0 ? along[s][i] / length : 0.5F;
            }
            dots[s] = RuneGlyphs.isDot(stroke);
        }
        for (int py = 0; py < pixels; py++) {
            for (int px = 0; px < pixels; px++) {
                float cx = px + 0.5F;
                float cy = py + 0.5F;
                float ink = 0;
                float light = 0;
                float nearest = Float.MAX_VALUE;
                for (int s = 0; s < count; s++) {
                    float[] p = xs[s];
                    if (dots[s]) {
                        float d = (float) Math.hypot(cx - (p[0] + p[p.length - 2]) / 2, cy - (p[1] + p[p.length - 1]) / 2);
                        float radius = brush * 0.95F;
                        nearest = Math.min(nearest, Math.max(0, d - radius));
                        ink = Math.max(ink, clamp(radius + 0.5F - d));
                        light = Math.max(light, clamp(radius * 0.45F + 0.5F - d));
                        continue;
                    }
                    for (int i = 0; i + 3 < p.length; i += 2) {
                        float ax = p[i];
                        float ay = p[i + 1];
                        float dx = p[i + 2] - ax;
                        float dy = p[i + 3] - ay;
                        float lengthSq = dx * dx + dy * dy;
                        float t = lengthSq > 0 ? clamp(((cx - ax) * dx + (cy - ay) * dy) / lengthSq) : 0;
                        float d = (float) Math.hypot(cx - (ax + dx * t), cy - (ay + dy * t));
                        float position = along[s][i / 2] + (along[s][i / 2 + 1] - along[s][i / 2]) * t;
                        float half = brush * (0.32F + 0.38F * (float) Math.sin(Math.PI * position));
                        nearest = Math.min(nearest, Math.max(0, d - half));
                        ink = Math.max(ink, clamp(half + 0.5F - d));
                        light = Math.max(light, clamp(half * 0.4F + 0.5F - d));
                    }
                }
                float glow = nearest == Float.MAX_VALUE ? 0 : 0.5F * (float) Math.exp(-(nearest / glowRadius) * (nearest / glowRadius) * 2);
                // Over each other: glow, then the ink, then the core.
                float a = glow;
                float r = FastColor.ARGB32.red(rgb) * glow;
                float g = FastColor.ARGB32.green(rgb) * glow;
                float b = FastColor.ARGB32.blue(rgb) * glow;
                r = r * (1 - ink) + FastColor.ARGB32.red(rgb) * ink;
                g = g * (1 - ink) + FastColor.ARGB32.green(rgb) * ink;
                b = b * (1 - ink) + FastColor.ARGB32.blue(rgb) * ink;
                a = a * (1 - ink) + ink;
                r = r * (1 - light) + FastColor.ARGB32.red(core) * light;
                g = g * (1 - light) + FastColor.ARGB32.green(core) * light;
                b = b * (1 - light) + FastColor.ARGB32.blue(core) * light;
                a = a * (1 - light) + light;
                if (a <= 0.003F) {
                    continue;
                }
                // The sums above are premultiplied; the texture is not.
                image.setPixelRGBA(px, py, FastColor.ABGR32.fromArgb32(FastColor.ARGB32.color(Math.round(a * 255), Math.round(r / a), Math.round(g / a), Math.round(b / a))));
            }
        }
    }

    private static float clamp(float value) {
        return value < 0 ? 0 : value > 1 ? 1 : value;
    }

    private static int mix(int a, int b, float t) {
        return FastColor.ARGB32.color(0, Math.round(FastColor.ARGB32.red(a) + (FastColor.ARGB32.red(b) - FastColor.ARGB32.red(a)) * t), Math.round(FastColor.ARGB32.green(a) + (FastColor.ARGB32.green(b) - FastColor.ARGB32.green(a)) * t),
                Math.round(FastColor.ARGB32.blue(a) + (FastColor.ARGB32.blue(b) - FastColor.ARGB32.blue(a)) * t)) & 0xFFFFFF;
    }

    /** A rune texture sampled smoothly, so it stays soft under any pose scale. */
    private static final class SmoothTexture extends DynamicTexture {
        SmoothTexture(NativeImage image) {
            super(image);
            // The upload left it nearest and repeating: linear, clamped to the transparent edge instead.
            setFilter(true, false);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }
    }
}
