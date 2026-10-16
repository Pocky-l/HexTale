package com.pockyl.hextale.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.PixelArt;

/** Pieces of the mod's magic screens: the tiling nebula and the brass-and-hextech nine-slice frame. */
public final class HexGui {
    public static final Identifier COSMOS = Hextale.id("textures/gui/cosmos.png");
    public static final Identifier FRAME = Hextale.id("textures/gui/star_book_frame.png");
    public static final int COSMOS_SIZE = 256;
    private static final int FRAME_SIZE = 48;
    private static final int FRAME_CORNER = 16;

    private HexGui() {
    }

    /** Tiles the nebula over the rectangle, shifted by {@code offsetX, offsetY} (parallax), clipped to it. */
    public static void cosmos(GuiGraphicsExtractor graphics, int x0, int y0, int w, int h, int offsetX, int offsetY) {
        PixelArt.flush(graphics);
        graphics.enableScissor(x0, y0, x0 + w, y0 + h);
        int ox = Math.floorMod(offsetX, COSMOS_SIZE);
        int oy = Math.floorMod(offsetY, COSMOS_SIZE);
        for (int ty = -oy; ty < h; ty += COSMOS_SIZE) {
            for (int tx = -ox; tx < w; tx += COSMOS_SIZE) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, COSMOS, x0 + tx, y0 + ty, 0, 0, COSMOS_SIZE, COSMOS_SIZE, COSMOS_SIZE, COSMOS_SIZE);
            }
        }
        graphics.disableScissor();
    }

    /** The nine-slice frame around a rectangle (drawn over its outer 16 px). */
    public static void frame(GuiGraphicsExtractor graphics, int left, int top, int width, int height) {
        int c = FRAME_CORNER;
        int mid = FRAME_SIZE - 2 * c;
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left, top, 0, 0, c, c, FRAME_SIZE, FRAME_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left + width - c, top, FRAME_SIZE - c, 0, c, c, FRAME_SIZE, FRAME_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left, top + height - c, 0, FRAME_SIZE - c, c, c, FRAME_SIZE, FRAME_SIZE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left + width - c, top + height - c, FRAME_SIZE - c, FRAME_SIZE - c, c, c,
                FRAME_SIZE, FRAME_SIZE);
        for (int x = left + c; x < left + width - c; x += mid) {
            int piece = Math.min(mid, left + width - c - x);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, x, top, c, 0, piece, c, FRAME_SIZE, FRAME_SIZE);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, x, top + height - c, c, FRAME_SIZE - c, piece, c, FRAME_SIZE, FRAME_SIZE);
        }
        for (int y = top + c; y < top + height - c; y += mid) {
            int piece = Math.min(mid, top + height - c - y);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left, y, 0, c, c, piece, FRAME_SIZE, FRAME_SIZE);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left + width - c, y, FRAME_SIZE - c, c, c, piece, FRAME_SIZE, FRAME_SIZE);
        }
    }
}
