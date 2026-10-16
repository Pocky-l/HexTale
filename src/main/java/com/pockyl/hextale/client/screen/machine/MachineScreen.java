package com.pockyl.hextale.client.screen.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.essence.Essence;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.menu.MachineMenu;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A screen of the essence works: the generated background, glass tanks whose liquid is layered by element, arrows and
 * tooltips. Tank contents are read from the block entity (synced by the server), the rest from the menu's data.
 */
public abstract class MachineScreen<M extends MachineMenu> extends AbstractContainerScreen<M> {
    protected static final int TANK_W = 16;
    protected static final int TANK_H = 54;
    private final Identifier background;
    @Nullable
    private List<Component> pendingTooltip;

    protected MachineScreen(M menu, Inventory inventory, Component title, Identifier background) {
        this(menu, inventory, title, background, 176);
    }

    /** A screen wider than its 176 px background, for a panel at its right. */
    protected MachineScreen(M menu, Inventory inventory, Component title, Identifier background, int width) {
        super(menu, inventory, title, width, 166);
        this.background = background;
        inventoryLabelY = MachineMenu.INVENTORY_Y - 11;
    }

    @Nullable
    protected <T extends BlockEntity> T blockEntity(Class<T> type) {
        BlockEntity entity = minecraft == null || minecraft.level == null ? null : minecraft.level.getBlockEntity(menu.pos());
        return type.isInstance(entity) ? type.cast(entity) : null;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        pendingTooltip = null;
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0, 0, 176, 166, 176, 166);
        renderMachine(graphics, mouseX, mouseY);
        PixelArt.flush(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (pendingTooltip != null && hoveredSlot == null) {
            graphics.setComponentTooltipForNextFrame(font, pendingTooltip, mouseX, mouseY);
        }
        // An empty machine slot says what goes into it.
        if (hoveredSlot != null && !hoveredSlot.hasItem() && minecraft != null && minecraft.player != null
                && hoveredSlot.container != minecraft.player.getInventory()) {
            String hint = slotHint(hoveredSlot.getContainerSlot());
            if (hint != null) {
                graphics.setTooltipForNextFrame(font, font.split(Component.translatable("gui.hextale.slot." + hint), 180), mouseX, mouseY);
            }
        }
    }

    /** The key suffix of the hint for an empty machine slot ({@code gui.hextale.slot.<hint>}), or null. */
    @Nullable
    protected abstract String slotHint(int slot);

    /** The machine's own drawing over the background (tanks, arrows, icons). */
    protected abstract void renderMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

    protected void tooltip(List<Component> lines) {
        pendingTooltip = lines;
    }

    protected static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /**
     * A glass tank {@code height} pixels tall at screen position {@code x, y}: its liquid layered by element from the
     * bottom, a wobbling surface, and a tooltip with what it holds plus {@code extra} lines.
     */
    protected void tank(GuiGraphicsExtractor graphics, int x, int y, int height, EssenceTank tank, int mouseX, int mouseY,
                        Component... extra) {
        long now = Util.getMillis();
        Essence contents = tank.contents();
        int filled = Math.round(height * tank.amount() / (float) tank.capacity());
        int bottom = y + height;
        int used = 0;
        if (contents.isUnknown() && filled > 0) {
            // An unknown essence shows as one murky liquid, not its layers.
            graphics.fill(x, bottom - filled, x + TANK_W, bottom, 0xFF000000 | contents.color());
            graphics.fill(x, bottom - filled, x + 2, bottom, 0x40FFFFFF);
            used = filled;
        }
        for (SimpleRune element : SimpleRune.values()) {
            if (used >= filled) {
                break;
            }
            int drops = contents.get(element);
            if (drops == 0) {
                continue;
            }
            int layer = Math.max(1, Math.round(filled * drops / (float) Math.max(1, contents.total())));
            layer = Math.min(layer, filled - used);
            if (layer <= 0) {
                continue;
            }
            graphics.fill(x, bottom - used - layer, x + TANK_W, bottom - used, 0xFF000000 | element.color());
            graphics.fill(x, bottom - used - layer, x + 2, bottom - used, 0x40FFFFFF);
            used += layer;
        }
        if (filled > 0) {
            for (int col = 0; col < TANK_W; col++) {
                int wobble = Math.round(Mth.sin(now / 280.0F + col * 0.6F));
                int top = Math.max(y, bottom - filled + wobble);
                graphics.fill(x + col, top, x + col + 1, top + 1, 0xFF000000 | PixelArt.mix(contents.color(), 0xFFFFFF, 0.5F));
            }
        }
        graphics.fill(x + TANK_W - 3, y + 2, x + TANK_W - 2, y + height - 2, 0x50FFFFFF);
        for (int mark = 1; mark < 4; mark++) {
            int my = y + height * mark / 4;
            graphics.fill(x, my, x + 4, my + 1, 0x80140C06);
        }
        if (isOver(mouseX, mouseY, x, y, TANK_W, height)) {
            List<Component> lines = new ArrayList<>();
            lines.add(tank.isEmpty() ? Component.translatable("block.hextale.essence.empty").withStyle(ChatFormatting.GRAY)
                    : contents.describe());
            lines.add(Component.literal(tank.amount() + "/" + tank.capacity()).withStyle(ChatFormatting.DARK_GRAY));
            for (Component line : extra) {
                lines.add(line.copy().withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip(lines);
        }
    }

    /** A brass arrow pointing right, filled from the left by {@code progress} (0-1). */
    protected static void arrow(GuiGraphicsExtractor graphics, int x, int y, int length, float progress) {
        int done = Math.round(length * Mth.clamp(progress, 0, 1));
        for (int i = 0; i < length; i++) {
            int colour = i < done ? 0xFFF0C24A : 0xFF4A3A28;
            graphics.fill(x + i, y + 3, x + i + 1, y + 6, colour);
        }
        for (int k = 0; k < 5; k++) {
            int colour = done >= length ? 0xFFF0C24A : 0xFF4A3A28;
            graphics.fill(x + length + k, y + k, x + length + k + 1, y + 9 - k, colour);
        }
    }
}
