package com.pockyl.hextale.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
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
    private static final int GRID_X = 12;
    private static final int GRID_Y = 22;
    private static final int CELL = 18;
    private static final int BUTTON_X = 72;
    private static final int BUTTON_W = 96;
    private static final int BUTTON_H = 14;
    private static final int[] BUTTON_Y = {20, 37, 54};
    private static final int BRASS = 0xC29445;
    private static final int BRASS_LIGHT = 0xF8E3A0;
    private static final int BRASS_DARK = 0x4A3312;
    private static final int PANEL = 0x16121F;
    private static final int TEXT = 0xFFE8E0F0;
    private static final int MUTED = 0xFF9A90B8;

    public ItemFilterScreen(ItemFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        inventoryLabelY = ItemFilterMenu.INVENTORY_Y - 11;
        titleLabelX = 8;
        titleLabelY = 7;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        plate(graphics, x, y, imageWidth, imageHeight);
        ItemFilter filter = menu.filter();
        // Ghost slots: dark wells, the items drawn faded by being under a veil.
        for (int slot = 0; slot < ItemFilter.SLOTS; slot++) {
            int sx = x + GRID_X + slot % 3 * CELL;
            int sy = y + GRID_Y + slot / 3 * CELL;
            well(graphics, sx, sy, CELL, CELL);
        }
        for (int i = 0; i < 3; i++) {
            button(graphics, x + BUTTON_X, y + BUTTON_Y[i], label(filter, i), over(mouseX, mouseY, x + BUTTON_X, y + BUTTON_Y[i], BUTTON_W, BUTTON_H));
        }
        // Inventory wells.
        for (Slot slot : menu.slots) {
            well(graphics, x + slot.x - 1, y + slot.y - 1, 18, 18);
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
        int ty = y + 73;
        for (FormattedCharSequence line : font.split(summary(filter), BUTTON_W + 4)) {
            graphics.text(font, line, x + BUTTON_X - 2, ty, MUTED, false);
            ty += 9;
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFF000000 | BRASS_LIGHT, true);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
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
                graphics.setTooltipForNextFrame(font, font.split(Component.translatable(tipKey(menu.filter(), i)), 200), mouseX, mouseY);
            }
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
        return super.mouseClicked(event, doubleClick);
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

    // ---- Drawing ----------------------------------------------------------------------------------------------------

    /** The plate: a dark panel in a brass frame lit at the top left. */
    private static void plate(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        PixelArt.fill(graphics, x, y, x + w, y + h, PixelArt.argb(BRASS_DARK, 1));
        PixelArt.fill(graphics, x + 1, y + 1, x + w - 1, y + h - 1, PixelArt.argb(BRASS, 1));
        PixelArt.fill(graphics, x + 1, y + 1, x + w - 1, y + 2, PixelArt.argb(BRASS_LIGHT, 1));
        PixelArt.fill(graphics, x + 1, y + 1, x + 2, y + h - 1, PixelArt.argb(BRASS_LIGHT, 1));
        PixelArt.fill(graphics, x + 3, y + 3, x + w - 3, y + h - 3, PixelArt.argb(BRASS_DARK, 1));
        PixelArt.fill(graphics, x + 4, y + 4, x + w - 4, y + h - 4, PixelArt.argb(PANEL, 1));
        PixelArt.fill(graphics, x + 4, y + ItemFilterMenu.INVENTORY_Y - 15, x + w - 4, y + ItemFilterMenu.INVENTORY_Y - 14,
                PixelArt.argb(BRASS, 0.5F));
    }

    /** A slot well: shaded at the top left, lit at the bottom right. */
    private static void well(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        PixelArt.fill(graphics, x, y, x + w, y + h, PixelArt.argb(0x0A0810, 1));
        PixelArt.fill(graphics, x + w - 1, y + 1, x + w, y + h, PixelArt.argb(0x4A4060, 1));
        PixelArt.fill(graphics, x + 1, y + h - 1, x + w, y + h, PixelArt.argb(0x4A4060, 1));
        PixelArt.fill(graphics, x + 1, y + 1, x + w - 1, y + h - 1, PixelArt.argb(0x1E1A2A, 1));
    }

    private void button(GuiGraphicsExtractor graphics, int x, int y, Component text, boolean hovered) {
        int rim = hovered ? BRASS_LIGHT : BRASS;
        PixelArt.fill(graphics, x + 1, y, x + BUTTON_W - 1, y + BUTTON_H, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x, y + 1, x + BUTTON_W, y + BUTTON_H - 1, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x + 1, y + 1, x + BUTTON_W - 1, y + BUTTON_H - 1, PixelArt.argb(hovered ? 0x2E2440 : 0x221B30, 1));
        PixelArt.flush(graphics);
        graphics.centeredText(font, text, x + BUTTON_W / 2, y + 3, hovered ? 0xFFFFF6D6 : TEXT);
    }
}
