package com.pockyl.hextale.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.pockyl.hextale.port.client.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.menu.ItemFilterMenu;
import com.pockyl.hextale.transport.ItemFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * The Pylon Filter's screen: a brass-framed plate with nine ghost slots (click with an item to put it in, with an empty
 * hand to clear; scroll to change its number, with shift by ten), three switches beside them (white or black list, by
 * kind or exact, the rule for the numbers) and a line saying what the filter does now, over the player's inventory.
 */
public class ItemFilterScreen extends AbstractContainerScreen<ItemFilterMenu> {
    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics portGraphics, float portPartialTick, int portMouseX, int portMouseY) {
    }

    private static final int GRID_X = 12;
    private static final int GRID_Y = 20;
    private static final int CELL = 18;
    private static final int BUTTON_X = 72;
    private static final int BUTTON_W = 96;
    private static final int BUTTON_H = 14;
    private static final int[] BUTTON_Y = {20, 40, 60};
    /** The line on what the filter does: under the grid and the buttons, across the plate, two lines at most. */
    private static final int SUMMARY_Y = 80;
    private static final int SUMMARY_LINES = 2;

    public ItemFilterScreen(ItemFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, ItemFilterMenu.INVENTORY_Y + 82);
        inventoryLabelY = ItemFilterMenu.INVENTORY_Y - 11;
        titleLabelX = 8;
        titleLabelY = 7;
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.renderBackground(graphics.vanilla(), mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        HexPlate.plate(graphics, x, y, imageWidth, imageHeight);
        HexPlate.divider(graphics, x, y + ItemFilterMenu.INVENTORY_Y - 15, imageWidth);
        ItemFilter filter = menu.filter();
        // Ghost slots: dark wells, the items drawn faded by being under a veil.
        for (int slot = 0; slot < ItemFilter.SLOTS; slot++) {
            int sx = x + GRID_X + slot % 3 * CELL;
            int sy = y + GRID_Y + slot / 3 * CELL;
            HexPlate.well(graphics, sx, sy, CELL, CELL);
        }
        for (int i = 0; i < 3; i++) {
            HexPlate.button(graphics, font, x + BUTTON_X, y + BUTTON_Y[i], BUTTON_W, BUTTON_H, label(filter, i),
                    over(mouseX, mouseY, x + BUTTON_X, y + BUTTON_Y[i], BUTTON_W, BUTTON_H), true);
        }
        // Inventory wells.
        for (Slot slot : menu.slots) {
            HexPlate.slot(graphics, x + slot.x, y + slot.y);
        }
        PixelArt.flush(graphics);
        for (ItemFilter.Entry entry : filter.entries()) {
            int sx = x + GRID_X + entry.slot() % 3 * CELL + 1;
            int sy = y + GRID_Y + entry.slot() / 3 * CELL + 1;
            graphics.item(entry.item(), sx, sy);
        }
        PixelArt.flush(graphics);
        // The numbers, when a rule uses them (whitelist only).
        if (filter.rule() != ItemFilter.Rule.NONE && filter.whitelist()) {
            graphics.nextStratum();
            for (ItemFilter.Entry entry : filter.entries()) {
                String count = Integer.toString(entry.count());
                int sx = x + GRID_X + entry.slot() % 3 * CELL + 17 - font.width(count);
                int sy = y + GRID_Y + entry.slot() / 3 * CELL + 9;
                graphics.text(font, count, sx, sy, 0xFFFFFFFF, true);
            }
        }
        // What it does now.
        HexPlate.paragraph(graphics, font, summary(filter), x + GRID_X - 4, y + SUMMARY_Y, imageWidth - (GRID_X - 4) * 2,
                SUMMARY_LINES, HexPlate.MUTED);
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        HexPlate.line(graphics, font, title, titleLabelX, titleLabelY, imageWidth - titleLabelX * 2, HexPlate.TITLE, true);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, HexPlate.TEXT, false);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.render(graphics.vanilla(), mouseX, mouseY, partialTick);
        int slot = ghostAt(mouseX, mouseY);
        if (slot >= 0) {
            ItemFilter filter = menu.filter();
            ItemFilter.Entry entry = filter.at(slot);
            List<Component> lines = new ArrayList<>();
            if (entry == null) {
                lines.add(Component.translatable("gui.hextale.pylon_filter.slot.empty"));
            } else {
                lines.add(entry.item().getHoverName());
                if (filter.rule() != ItemFilter.Rule.NONE && filter.whitelist()) {
                    lines.add(Component.translatable("gui.hextale.pylon_filter.slot.count", entry.count()));
                }
                lines.add(Component.translatable("gui.hextale.pylon_filter.slot.hint"));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
        for (int i = 0; i < 3; i++) {
            if (over(mouseX, mouseY, leftPos + BUTTON_X, topPos + BUTTON_Y[i], BUTTON_W, BUTTON_H)) {
                // A label cut to fit its button heads its tooltip whole.
                Component label = label(menu.filter(), i);
                List<FormattedCharSequence> tip = new ArrayList<>();
                if (HexPlate.cut(font, label, BUTTON_W - 6)) {
                    tip.addAll(font.split(label, 200));
                }
                tip.addAll(font.split(Component.translatable(tipKey(menu.filter(), i)), 200));
                graphics.setTooltipForNextFrame(font, tip, mouseX, mouseY);
            }
        }
        // The summary may be cut: hovering it shows it whole.
        Component summary = summary(menu.filter());
        int width = imageWidth - (GRID_X - 4) * 2;
        if (font.split(summary, width).size() > SUMMARY_LINES
                && over(mouseX, mouseY, leftPos + GRID_X - 4, topPos + SUMMARY_Y, width, SUMMARY_LINES * 10)) {
            graphics.setTooltipForNextFrame(font, font.split(summary, 200), mouseX, mouseY);
        }
    }

    private static Component label(ItemFilter filter, int button) {
        return switch (button) {
            case 0 -> Component.translatable(filter.whitelist() ? "gui.hextale.pylon_filter.whitelist" : "gui.hextale.pylon_filter.blacklist");
            case 1 -> Component.translatable(filter.exact() ? "gui.hextale.pylon_filter.exact" : "gui.hextale.pylon_filter.kind");
            default -> Component.translatable("gui.hextale.pylon_filter.rule." + filter.rule().getSerializedName());
        };
    }

    private static String tipKey(ItemFilter filter, int button) {
        return switch (button) {
            case 0 -> "gui.hextale.pylon_filter.tip.list";
            case 1 -> "gui.hextale.pylon_filter.tip.exact";
            default -> "gui.hextale.pylon_filter.tip.rule";
        };
    }

    /** One sentence on what the filter does with these settings. */
    private static Component summary(ItemFilter filter) {
        if (filter.entries().isEmpty()) {
            return Component.translatable("gui.hextale.pylon_filter.summary.empty");
        }
        if (!filter.whitelist()) {
            return Component.translatable("gui.hextale.pylon_filter.summary.blacklist");
        }
        return Component.translatable("gui.hextale.pylon_filter.summary." + filter.rule().getSerializedName());
    }

    private int ghostAt(double mouseX, double mouseY) {
        for (int slot = 0; slot < ItemFilter.SLOTS; slot++) {
            if (over(mouseX, mouseY, leftPos + GRID_X + slot % 3 * CELL, topPos + GRID_Y + slot / 3 * CELL, CELL, CELL)) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double portX, double portY, int portButton) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        boolean doubleClick = false;
        int slot = ghostAt(event.x(), event.y());
        if (slot >= 0) {
            press(ItemFilterMenu.SET + slot);
            return true;
        }
        int[] ids = {ItemFilterMenu.TOGGLE_LIST, ItemFilterMenu.TOGGLE_EXACT, ItemFilterMenu.CYCLE_RULE};
        for (int i = 0; i < 3; i++) {
            if (over(event.x(), event.y(), leftPos + BUTTON_X, topPos + BUTTON_Y[i], BUTTON_W, BUTTON_H)) {
                press(ids[i]);
                return true;
            }
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int slot = ghostAt(mouseX, mouseY);
        if (slot >= 0 && scrollY != 0 && menu.filter().at(slot) != null) {
            boolean ten = minecraft != null && (InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT));
            int id = scrollY > 0 ? (ten ? ItemFilterMenu.PLUS_TEN : ItemFilterMenu.PLUS) : (ten ? ItemFilterMenu.MINUS_TEN : ItemFilterMenu.MINUS);
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id + slot);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private static boolean over(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
