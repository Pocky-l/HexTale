package com.pockyl.hextale.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import com.pockyl.hextale.multiblock.Multiblock;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The 3D model of a multiblock in a dark frame with a bar under it to step through the layers - the same in the research
 * book and in JEI. The left arrow and the right arrow step down and up; past the last layer (or before the first) it goes
 * back to building itself. Dragging over the model turns it. Draws with the graphics' own fills and text, so it follows a
 * translated pose; callers that batch pixels flush them first.
 */
public final class MultiblockPreview {
    public static final int BAR = 14;
    private static final int FRAME = 0xFF8A7344;
    private static final int BACK = 0xFF0B0918;
    private static final int BAR_BACK = 0xFF15122A;
    private static final int TEXT = 0xFFE8DFC8;
    private static final int MUTED = 0xFF9A90B8;
    private static final int ARROW = 0xFFD8B46A;
    private static final int ARROW_HOVER = 0xFFFFE6A8;
    private static final int ARROW_W = 16;

    private final Multiblock multiblock;
    private int layer = MultiblockDiorama.BUILD;
    private float yaw;
    // The box last drawn, in the caller's coordinates, for clicks and drags.
    private int x;
    private int y;
    private int width;
    private int height;

    public MultiblockPreview(Multiblock multiblock) {
        this.multiblock = multiblock;
    }

    public Multiblock multiblock() {
        return multiblock;
    }

    /** The chosen layer, or {@link MultiblockDiorama#BUILD} while the model builds itself. */
    public int layer() {
        return layer;
    }

    /**
     * Draws the model in a {@code width} x {@code height} box at {@code x, y} (the bar included at its foot); returns the
     * tooltip of what the mouse is over, or null. {@code time} in ticks.
     */
    @Nullable
    public List<Component> render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY, float time) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        Font font = Minecraft.getInstance().font;
        int sceneBottom = y + height - BAR;
        graphics.fill(x, y, x + width, y + height, FRAME);
        graphics.fill(x + 1, y + 1, x + width - 1, sceneBottom, BACK);
        graphics.fill(x + 1, sceneBottom, x + width - 1, y + height - 1, BAR_BACK);
        MultiblockDiorama.draw(graphics, multiblock, layer, time, yaw, x + 1, y + 1, x + width - 1, sceneBottom);

        int count = multiblock.pattern().height();
        int shown = layer == MultiblockDiorama.BUILD ? MultiblockDiorama.buildingLayer(multiblock, time) : layer;
        Component label = layer == MultiblockDiorama.BUILD
                ? Component.translatable("gui.hextale.multiblock.building")
                : Component.translatable("gui.hextale.multiblock.layer", layer + 1, count);
        int textY = sceneBottom + (BAR - 8) / 2;
        graphics.centeredText(font, label, x + width / 2, textY, layer == MultiblockDiorama.BUILD ? MUTED : TEXT);
        // Small pips, one per layer: the chosen (or the one being laid) lights up.
        int pipsWidth = count * 4 - 1;
        int px = x + width - ARROW_W - 4 - pipsWidth;
        for (int i = 0; i < count; i++) {
            int color = i == shown ? ARROW_HOVER : i < shown || (layer != MultiblockDiorama.BUILD && i < layer) ? ARROW : 0xFF3E3858;
            graphics.fill(px + i * 4, sceneBottom + BAR / 2 - 1, px + i * 4 + 3, sceneBottom + BAR / 2 + 2, color);
        }
        arrow(graphics, x + 2, sceneBottom, false, overArrow(mouseX, mouseY, false));
        arrow(graphics, x + width - ARROW_W - 2, sceneBottom, true, overArrow(mouseX, mouseY, true));
        return tooltip(mouseX, mouseY);
    }

    /** The tooltip of what the mouse is over in the box last drawn, or null. */
    @Nullable
    public List<Component> tooltip(double mouseX, double mouseY) {
        if (overArrow(mouseX, mouseY, false)) {
            return List.of(Component.translatable("gui.hextale.multiblock.down"));
        }
        if (overArrow(mouseX, mouseY, true)) {
            return List.of(Component.translatable("gui.hextale.multiblock.up"));
        }
        if (overScene(mouseX, mouseY)) {
            return List.of(Component.translatable("gui.hextale.multiblock.hint").withColor(MUTED));
        }
        return null;
    }

    private static void arrow(GuiGraphicsExtractor graphics, int ax, int top, boolean right, boolean hovered) {
        int color = hovered ? ARROW_HOVER : ARROW;
        int cy = top + BAR / 2;
        for (int i = 0; i < 4; i++) {
            int col = right ? ax + 6 + i : ax + 9 - i;
            graphics.fill(col, cy - 3 + i, col + 1, cy + 4 - i, color);
        }
    }

    private boolean overArrow(double mouseX, double mouseY, boolean right) {
        int ax = right ? x + width - ARROW_W - 2 : x + 2;
        int top = y + height - BAR;
        return mouseX >= ax && mouseX < ax + ARROW_W && mouseY >= top && mouseY < y + height;
    }

    /** A click: the arrows step through the layers; true when it was used. */
    public boolean click(double mouseX, double mouseY) {
        int count = multiblock.pattern().height();
        if (overArrow(mouseX, mouseY, true)) {
            layer = layer == MultiblockDiorama.BUILD ? 0 : layer + 1 >= count ? MultiblockDiorama.BUILD : layer + 1;
        } else if (overArrow(mouseX, mouseY, false)) {
            layer = layer == MultiblockDiorama.BUILD ? count - 1 : layer - 1;
        } else {
            return false;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.4F, 0.5F));
        return true;
    }

    /** True when the point lies on the model (where a drag turns it). */
    public boolean overScene(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height - BAR;
    }

    /** Turns the model by a horizontal drag. */
    public void drag(double dragX) {
        yaw += (float) dragX * 1.6F;
    }

    /** The mouse wheel over the model steps through the layers like the arrows; true when it was used. */
    public boolean scroll(double mouseX, double mouseY, double amount) {
        if (!overScene(mouseX, mouseY) || amount == 0) {
            return false;
        }
        int count = multiblock.pattern().height();
        if (amount > 0) {
            layer = layer == MultiblockDiorama.BUILD ? 0 : Math.min(count - 1, layer + 1);
        } else {
            layer = layer <= 0 ? MultiblockDiorama.BUILD : layer - 1;
        }
        return true;
    }
}
