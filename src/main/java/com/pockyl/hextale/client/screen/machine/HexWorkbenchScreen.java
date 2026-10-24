package com.pockyl.hextale.client.screen.machine;

import net.minecraft.ChatFormatting;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

import com.pockyl.hextale.block.HexWorkbenchBlockEntity;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.client.screen.HexPlate;
import com.pockyl.hextale.crafting.HexCraftingRecipe;
import com.pockyl.hextale.menu.HexWorkbenchMenu;
import com.pockyl.hextale.menu.MachineMenu;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Hex Workbench on the mod's brass plate: the crystal socket under a glass tube of charge (left), the grid inside a
 * faint hexagon of channels, and the result in its brass ring. Under the arrow the craft's price in charge; a locked
 * device names the research it waits for.
 */
public class HexWorkbenchScreen extends AbstractContainerScreen<HexWorkbenchMenu> {
    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics portGraphics, float portPartialTick, int portMouseX, int portMouseY) {
    }

    private static final int W = 176;
    private static final int GAUGE_X = 17;
    private static final int GAUGE_Y = 19;
    private static final int GAUGE_W = 10;
    private static final int GAUGE_H = 44;
    /** Charge that fills the tube; more than this shows a full tube. */
    private static final int GAUGE_FULL = HexWorkbenchBlockEntity.MAX_CHARGE;
    public static final int ARROW_X = 106;
    public static final int ARROW_Y = 40;

    public HexWorkbenchScreen(HexWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, MachineMenu.INVENTORY_Y + 82);
        titleLabelX = 8;
        titleLabelY = 7;
        inventoryLabelY = MachineMenu.INVENTORY_Y - 11;
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.renderBackground(graphics.vanilla(), mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        HexPlate.plate(graphics, x, y, W, imageHeight);
        HexPlate.divider(graphics, x, y + MachineMenu.INVENTORY_Y - 15, W);
        for (Slot slot : menu.slots) {
            if (slot.index == HexWorkbenchBlockEntity.SIZE) {
                HexPlate.outputSlot(graphics, x + slot.x, y + slot.y);
            } else {
                HexPlate.slot(graphics, x + slot.x, y + slot.y);
            }
        }
        gauge(graphics, x + GAUGE_X, y + GAUGE_Y, menu.charge(), menu.why() == HexWorkbenchMenu.Why.NO_CHARGE);
        PixelArt.flush(graphics);
        MachineScreen.arrow(graphics, x + ARROW_X, y + ARROW_Y, 16, menu.slots.get(HexWorkbenchBlockEntity.SIZE).hasItem() ? 1 : 0);
        PixelArt.flush(graphics);
    }

    /** A glass tube of charge over the crystal socket: cyan light rising with the charge, a red pulse when short. */
    private static void gauge(GuiGraphicsExtractor graphics, int x, int y, int charge, boolean short_) {
        HexPlate.well(graphics, x - 1, y - 1, GAUGE_W + 2, GAUGE_H + 2);
        PixelArt.fill(graphics, x, y, x + GAUGE_W, y + GAUGE_H, PixelArt.argb(0x120E1A, 1));
        int filled = Math.round(GAUGE_H * Mth.clamp(charge / (float) GAUGE_FULL, 0, 1));
        long now = Util.getMillis();
        for (int row = 0; row < filled; row++) {
            float wave = 0.5F + 0.5F * Mth.sin(now / 240.0F - row * 0.5F);
            int colour = PixelArt.mix(0x2A7FB8, 0xA8ECFF, 0.25F + 0.45F * wave);
            PixelArt.fill(graphics, x, y + GAUGE_H - 1 - row, x + GAUGE_W, y + GAUGE_H - row, PixelArt.argb(colour, 1));
        }
        PixelArt.fill(graphics, x + 1, y + 1, x + 2, y + GAUGE_H - 1, PixelArt.argb(0xFFFFFF, 0.25F));
        for (int mark = 1; mark < 5; mark++) {
            int my = y + GAUGE_H * mark / 5;
            PixelArt.fill(graphics, x + GAUGE_W - 3, my, x + GAUGE_W, my + 1, PixelArt.argb(0x140C06, 0.6F));
        }
        PixelArt.flush(graphics);
        if (short_) {
            MachineScreen.warn(graphics, x, y, GAUGE_W, GAUGE_H);
        }
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        HexPlate.line(graphics, font, title, titleLabelX, titleLabelY, W - titleLabelX * 2, HexPlate.TITLE, true);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, HexPlate.TEXT, false);
        if (menu.cost() > 0) {
            Component price = Component.translatable("gui.hextale.hex_workbench.cost", menu.cost());
            HexPlate.centered(graphics, font, price, ARROW_X + 10, ARROW_Y + 14, 44,
                    menu.why() == HexWorkbenchMenu.Why.NO_CHARGE ? 0xFFE0503C : 0xFF7FD8FF);
        }
        if (menu.why() == HexWorkbenchMenu.Why.LOCKED) {
            HexPlate.centered(graphics, font, Component.translatable("gui.hextale.hex_workbench.locked"), ARROW_X + 10, ARROW_Y - 12, 60,
                    0xFFE0503C);
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.render(graphics.vanilla(), mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        if (mouseX >= x + GAUGE_X - 1 && mouseX < x + GAUGE_X + GAUGE_W + 1 && mouseY >= y + GAUGE_Y - 1 && mouseY < y + GAUGE_Y + GAUGE_H + 1) {
            graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.hextale.hex_workbench.charge", menu.charge()),
                    Component.translatable("gui.hextale.hex_workbench.charge.hint", HexWorkbenchBlockEntity.UNSTABLE_CHARGE,
                            HexWorkbenchBlockEntity.CRYSTAL_CHARGE, HexWorkbenchBlockEntity.GEMSTONE_CHARGE,
                            HexWorkbenchBlockEntity.MAX_CHARGE).withStyle(ChatFormatting.GRAY)),
                    mouseX, mouseY);
        } else if (menu.why() == HexWorkbenchMenu.Why.LOCKED && mouseX >= x + ARROW_X - 20 && mouseX < x + ARROW_X + 40
                && mouseY >= y + ARROW_Y - 14 && mouseY < y + ARROW_Y + 10) {
            List<Component> lines = new ArrayList<>();
            ItemStack wanted = lockedResult();
            var topic = wanted.isEmpty() ? null : ResearchTopics.of(wanted.getItem());
            Component why = HexWorkbenchMenu.whyText(menu.why(), topic);
            if (why != null) {
                lines.add(why);
                graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            }
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index == HexWorkbenchBlockEntity.CRYSTAL) {
            graphics.setTooltipForNextFrame(font, font.split(Component.translatable("gui.hextale.slot.hex_workbench.crystal"), 180),
                    mouseX, mouseY);
        }
    }

    /** The result the grid would make (known from the synced recipes), shown nowhere while locked. */
    private ItemStack lockedResult() {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < HexWorkbenchBlockEntity.GRID; i++) {
            grid.add(menu.slots.get(i).getItem());
        }
        Map.Entry<ResourceLocation, HexCraftingRecipe> found = HexCraftingRecipe.find(CraftingInput.of(3, 3, grid), true);
        return found == null ? ItemStack.EMPTY : found.getValue().result().create();
    }
}
