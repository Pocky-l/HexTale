package com.pockyl.hextale.client.screen.machine;

import net.minecraft.ChatFormatting;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.client.screen.HexPlate;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.menu.MachineMenu;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A screen of the essence works on the mod's brass plate ({@link HexPlate}): a well under every slot (the product's slot
 * ringed in brass), glass tanks in their wells, arrows and tooltips. Tank contents are read from the block entity
 * (synced by the server), the rest from the menu's data.
 */
public abstract class MachineScreen<M extends MachineMenu> extends AbstractContainerScreen<M> {
    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics portGraphics, float portPartialTick, int portMouseX, int portMouseY) {
    }

    protected static final int TANK_W = 16;
    protected static final int TANK_H = 54;
    /** The plate's width; a wider screen keeps the rest for a panel at its right. */
    protected static final int PLATE_W = 176;
    @Nullable
    private List<Component> pendingTooltip;

    protected MachineScreen(M menu, Inventory inventory, Component title) {
        this(menu, inventory, title, PLATE_W);
    }

    /** A screen wider than its plate, for a panel at its right. */
    protected MachineScreen(M menu, Inventory inventory, Component title, int width) {
        super(menu, inventory, title);
        this.imageWidth = width;
        this.imageHeight = MachineMenu.INVENTORY_Y + 82;
        titleLabelX = 8;
        titleLabelY = 7;
        inventoryLabelY = MachineMenu.INVENTORY_Y - 11;
    }

    @Nullable
    protected <T extends BlockEntity> T blockEntity(Class<T> type) {
        BlockEntity entity = minecraft == null || minecraft.level == null ? null : minecraft.level.getBlockEntity(menu.pos());
        return type.isInstance(entity) ? type.cast(entity) : null;
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        pendingTooltip = null;
        super.renderBackground(graphics.vanilla(), mouseX, mouseY, partialTick);
        HexPlate.plate(graphics, leftPos, topPos, PLATE_W, imageHeight);
        HexPlate.divider(graphics, leftPos, topPos + MachineMenu.INVENTORY_Y - 15, PLATE_W);
        // Brass rings first, wells over them: neighbouring product slots share one brass rim.
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory) && isOutput(slot.getContainerSlot())) {
                HexPlate.outputRing(graphics, leftPos + slot.x, topPos + slot.y);
            }
        }
        for (Slot slot : menu.slots) {
            HexPlate.slot(graphics, leftPos + slot.x, topPos + slot.y);
        }
        PixelArt.flush(graphics);
        renderMachine(graphics, mouseX, mouseY);
        PixelArt.flush(graphics);
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        HexPlate.title(graphics, font, title, titleLabelX, titleLabelY, PLATE_W - titleLabelX * 2);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, HexPlate.TEXT, false);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.render(graphics.vanilla(), mouseX, mouseY, partialTick);
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

    /** Whether the machine slot (container index) is where the product comes out: its well gets a brass ring. */
    protected abstract boolean isOutput(int slot);

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
     * A glass tank {@code height} pixels tall at screen position {@code x, y}: its liquid in its mixed colour with a
     * wobbling surface, and a tooltip with what it holds (mB) plus {@code extra} lines.
     */
    protected void tank(GuiGraphicsExtractor graphics, int x, int y, int height, EssenceTank tank, int mouseX, int mouseY,
                        Component... extra) {
        glass(graphics, x, y, height);
        long now = Util.getMillis();
        EssenceMix contents = tank.contents();
        int filled = Math.round(height * tank.amount() / (float) tank.capacity());
        int bottom = y + height;
        if (filled > 0) {
            // One mixed liquid in its own colour.
            graphics.fill(x, bottom - filled, x + TANK_W, bottom, 0xFF000000 | contents.color());
            graphics.fill(x, bottom - filled, x + 2, bottom, 0x40FFFFFF);
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
            lines.add(Component.literal(tank.amount() + "/" + tank.capacity() + " mB").withStyle(ChatFormatting.DARK_GRAY));
            for (Component line : extra) {
                lines.add(line.copy().withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip(lines);
        }
    }

    /** The well a tank's glass sits in, one pixel round it. */
    private static void glass(GuiGraphicsExtractor graphics, int x, int y, int height) {
        HexPlate.well(graphics, x - 1, y - 1, TANK_W + 2, height + 2);
        PixelArt.fill(graphics, x, y, x + TANK_W, y + height, PixelArt.argb(0x1A120B, 1));
        PixelArt.flush(graphics);
    }

    /** A water gauge {@code height} pixels tall: blue water with a wobbling surface. */
    protected void waterTank(GuiGraphicsExtractor graphics, int x, int y, int height, int amount, int capacity, int mouseX, int mouseY,
                             Component... lines) {
        glass(graphics, x, y, height);
        int filled = Math.round(height * Math.min(1, amount / (float) capacity));
        int bottom = y + height;
        if (filled > 0) {
            graphics.fill(x, bottom - filled, x + TANK_W, bottom, 0xFF3F76E4);
            graphics.fill(x, bottom - filled, x + 2, bottom, 0x40FFFFFF);
            long now = Util.getMillis();
            for (int col = 0; col < TANK_W; col++) {
                int top = Math.max(y, bottom - filled + Math.round(Mth.sin(now / 280.0F + col * 0.6F)));
                graphics.fill(x + col, top, x + col + 1, top + 1, 0xFF9CC3FF);
            }
        }
        graphics.fill(x + TANK_W - 3, y + 2, x + TANK_W - 2, y + height - 2, 0x50FFFFFF);
        if (isOver(mouseX, mouseY, x, y, TANK_W, height)) {
            List<Component> out = new ArrayList<>(List.of(lines));
            tooltip(out);
        }
    }

    /** A pulsing red frame around a part that stops the machine. */
    protected static void warn(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int alpha = Math.round(120 + 100 * Mth.sin(Util.getMillis() / 200.0F));
        int colour = alpha << 24 | 0xE0503C;
        graphics.fill(x - 1, y - 1, x + w + 1, y, colour);
        graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, colour);
        graphics.fill(x - 1, y, x, y + h, colour);
        graphics.fill(x + w, y, x + w + 1, y + h, colour);
    }

    /** An arrow carved into the wood pointing right, filled with brass from the left by {@code progress} (0-1). */
    protected static void arrow(GuiGraphicsExtractor graphics, int x, int y, int length, float progress) {
        int done = Math.round(length * Mth.clamp(progress, 0, 1));
        for (int i = 0; i < length; i++) {
            int colour = i < done ? 0xFFE2C67A : 0xFF24170D;
            graphics.fill(x + i, y + 3, x + i + 1, y + 6, colour);
        }
        for (int k = 0; k < 5; k++) {
            int colour = done >= length ? 0xFFE2C67A : 0xFF24170D;
            graphics.fill(x + length + k, y + k, x + length + k + 1, y + 9 - k, colour);
        }
    }
}
