package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The 26.x GUI drawing surface over the 1.21.1 immediate {@link GuiGraphics}: same method names, so screens stay
 * line for line the same as on {@code main}. Pipelines only pick the blending; tooltips go to the screen's deferred
 * tooltip, which 1.21.1 draws after the screen.
 *
 * <p>port-of: net.minecraft.client.gui.GuiGraphicsExtractor
 */
public final class GuiGraphicsExtractor {
    private static @Nullable GuiGraphicsExtractor last;

    private final GuiGraphics graphics;
    private final GuiPose pose;

    private GuiGraphicsExtractor(GuiGraphics graphics) {
        this.graphics = graphics;
        this.pose = new GuiPose(graphics.pose());
    }

    /** The surface for a vanilla graphics object (one per frame on 1.21.1, so the last wrapper is reused). */
    public static GuiGraphicsExtractor of(GuiGraphics graphics) {
        GuiGraphicsExtractor cached = last;
        if (cached == null || cached.graphics != graphics) {
            cached = new GuiGraphicsExtractor(graphics);
            last = cached;
        }
        return cached;
    }

    public GuiGraphics vanilla() {
        return graphics;
    }

    public int guiWidth() {
        return graphics.guiWidth();
    }

    public int guiHeight() {
        return graphics.guiHeight();
    }

    public GuiPose pose() {
        return pose;
    }

    /** 26.x starts a new layer above everything drawn so far; 1.21.1 draws in order, so the batch is just flushed. */
    public void nextStratum() {
        graphics.flush();
    }

    /** Flushes first: what was drawn before must not be clipped by the new scissor (1.21.1 clips at draw time). */
    public void enableScissor(int x0, int y0, int x1, int y1) {
        graphics.flush();
        graphics.enableScissor(x0, y0, x1, y1);
    }

    public void disableScissor() {
        graphics.flush();
        graphics.disableScissor();
    }

    public boolean containsPointInScissor(int x, int y) {
        return graphics.containsPointInScissor(x, y);
    }

    /** The innermost scissor rectangle, or null (1.21.1 keeps the stack private, so this tracks the GL state). */
    public @Nullable ScreenRectangle peekScissorStack() {
        return null;
    }

    public void horizontalLine(int x0, int x1, int y, int color) {
        graphics.hLine(x0, x1, y, color);
    }

    public void verticalLine(int x, int y0, int y1, int color) {
        graphics.vLine(x, y0, y1, color);
    }

    public void fill(int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0, y0, x1, y1, color);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int color0, int color1) {
        graphics.fillGradient(x0, y0, x1, y1, color0, color1);
    }

    public void outline(int x, int y, int width, int height, int color) {
        graphics.renderOutline(x, y, width, height, color);
    }

    public void text(Font font, @Nullable String text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, true);
    }

    public void text(Font font, @Nullable String text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, true);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, true);
    }

    public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void centeredText(Font font, String text, int x, int y, int color) {
        graphics.drawCenteredString(font, text, x, y, color);
    }

    public void centeredText(Font font, Component text, int x, int y, int color) {
        graphics.drawCenteredString(font, text, x, y, color);
    }

    public void centeredText(Font font, FormattedCharSequence text, int x, int y, int color) {
        graphics.drawCenteredString(font, text, x, y, color);
    }

    public int textWithWordWrap(Font font, FormattedText text, int x, int y, int width, int color) {
        return textWithWordWrap(font, text, x, y, width, color, true);
    }

    public int textWithWordWrap(Font font, FormattedText text, int x, int y, int width, int color, boolean shadow) {
        for (FormattedCharSequence line : font.split(text, width)) {
            graphics.drawString(font, line, x, y, color, shadow);
            y += font.lineHeight;
        }
        return y;
    }

    public void blit(RenderPipeline pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height,
                     int textureWidth, int textureHeight) {
        blit(pipeline, texture, x, y, u, v, width, height, width, height, textureWidth, textureHeight, -1);
    }

    public void blit(RenderPipeline pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height,
                     int textureWidth, int textureHeight, int color) {
        blit(pipeline, texture, x, y, u, v, width, height, width, height, textureWidth, textureHeight, color);
    }

    public void blit(RenderPipeline pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height,
                     int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        blit(pipeline, texture, x, y, u, v, width, height, srcWidth, srcHeight, textureWidth, textureHeight, -1);
    }

    public void blit(RenderPipeline pipeline, ResourceLocation texture, int x, int y, float u, float v, int width, int height,
                     int srcWidth, int srcHeight, int textureWidth, int textureHeight, int color) {
        begin(pipeline, color);
        graphics.blit(texture, x, y, width, height, u, v, srcWidth, srcHeight, textureWidth, textureHeight);
        end(pipeline);
    }

    public void blitSprite(RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height) {
        blitSprite(pipeline, sprite, x, y, width, height, -1);
    }

    public void blitSprite(RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height, float alpha) {
        blitSprite(pipeline, sprite, x, y, width, height, (int) (alpha * 255) << 24 | 0xFFFFFF);
    }

    public void blitSprite(RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height, int color) {
        begin(pipeline, color);
        graphics.blitSprite(sprite, x, y, width, height);
        end(pipeline);
    }

    private void begin(RenderPipeline pipeline, int color) {
        RenderSystem.enableBlend();
        if (pipeline == RenderPipelines.VIGNETTE) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        if (color != -1) {
            graphics.setColor((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F, (color >>> 24) / 255F);
        }
    }

    private void end(RenderPipeline pipeline) {
        graphics.setColor(1, 1, 1, 1);
        if (pipeline == RenderPipelines.VIGNETTE) {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableBlend();
    }

    public void item(ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
    }

    public void item(ItemStack stack, int x, int y, int seed) {
        graphics.renderItem(stack, x, y, seed);
    }

    public void fakeItem(ItemStack stack, int x, int y) {
        graphics.renderFakeItem(stack, x, y);
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y) {
        graphics.renderItemDecorations(font, stack, x, y);
    }

    public void itemDecorations(Font font, ItemStack stack, int x, int y, @Nullable String countText) {
        graphics.renderItemDecorations(font, stack, x, y, countText);
    }

    public void setTooltipForNextFrame(Font font, Component text, int x, int y) {
        tooltip(font, List.of(text.getVisualOrderText()), x, y);
    }

    public void setTooltipForNextFrame(Font font, List<? extends FormattedCharSequence> lines, int x, int y) {
        tooltip(font, new ArrayList<>(lines), x, y);
    }

    public void setTooltipForNextFrame(Font font, ItemStack stack, int x, int y) {
        graphics.renderTooltip(font, stack, x, y);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int x, int y) {
        List<FormattedCharSequence> sequences = new ArrayList<>(lines.size());
        for (Component line : lines) {
            sequences.add(line.getVisualOrderText());
        }
        tooltip(font, sequences, x, y);
    }

    private void tooltip(Font font, List<FormattedCharSequence> lines, int x, int y) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            screen.setTooltipForNextRenderPass(lines);
        } else {
            graphics.renderTooltip(font, lines, x, y);
        }
    }
}
