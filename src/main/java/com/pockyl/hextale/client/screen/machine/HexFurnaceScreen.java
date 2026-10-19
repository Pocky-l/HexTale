package com.pockyl.hextale.client.screen.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

import com.pockyl.hextale.block.HexFurnaceBlockEntity;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.menu.HexFurnaceMenu;

import java.util.List;

/**
 * The Hex Furnace: three metals side by side over a firebox, the flames under them burning down with the fuel, a pool
 * of molten metal filling as the alloy melts, and the arrow to the ingot.
 */
public class HexFurnaceScreen extends MachineScreen<HexFurnaceMenu> {
    private static final int POOL_X = HexFurnaceMenu.INPUT_X;
    private static final int POOL_Y = HexFurnaceMenu.INPUT_Y + 19;
    private static final int POOL_W = 52;
    private static final int FLAME_X = HexFurnaceMenu.FUEL_X + 22;

    public HexFurnaceScreen(HexFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        HexFurnaceBlockEntity furnace = blockEntity(HexFurnaceBlockEntity.class);
        int x = leftPos;
        int y = topPos;
        float melt = menu.progress() / (float) menu.total();
        pool(graphics, x + POOL_X, y + POOL_Y, melt);
        flames(graphics, x + FLAME_X, y + HexFurnaceMenu.FUEL_Y, menu.fuel());
        if (isOver(mouseX, mouseY, x + FLAME_X, y + HexFurnaceMenu.FUEL_Y, 14, 16)) {
            tooltip(List.of(Component.translatable("gui.hextale.hex_furnace.fuel"),
                    Component.translatable("gui.hextale.hex_furnace.fuel.hint").withStyle(ChatFormatting.GRAY)));
        }
        arrow(graphics, x + 92, y + 44, 24, melt);
        if (furnace == null) {
            return;
        }
        switch (furnace.blocked()) {
            case "no_fuel" -> warn(graphics, x + HexFurnaceMenu.FUEL_X, y + HexFurnaceMenu.FUEL_Y, 16, 16);
            case "output_full" -> warn(graphics, x + HexFurnaceMenu.OUTPUT_X - 4, y + HexFurnaceMenu.OUTPUT_Y - 4, 24, 24);
            case "locked" -> {
                warn(graphics, x + POOL_X - 1, y + HexFurnaceMenu.INPUT_Y - 1, POOL_W + 2, 18);
                if (isOver(mouseX, mouseY, x + POOL_X, y + POOL_Y, POOL_W, 6)) {
                    tooltip(List.of(Component.translatable("gui.hextale.hex_furnace.locked").withStyle(ChatFormatting.RED)));
                }
            }
            default -> {
            }
        }
    }

    /** A shallow pool of molten metal under the inputs, filling and glowing as the alloy melts. */
    private static void pool(GuiGraphicsExtractor graphics, int x, int y, float melt) {
        PixelArt.fill(graphics, x - 1, y, x + POOL_W + 1, y + 6, PixelArt.argb(0x0A0810, 1));
        PixelArt.fill(graphics, x, y + 1, x + POOL_W, y + 5, PixelArt.argb(0x231C2C, 1));
        int filled = Math.round(POOL_W * Mth.clamp(melt, 0, 1));
        long now = Util.getMillis();
        for (int col = 0; col < filled; col++) {
            float flicker = 0.5F + 0.5F * Mth.sin(now / 160.0F + col * 0.9F);
            int hot = PixelArt.mix(0xD8571C, 0xFFD27A, 0.35F + 0.4F * flicker);
            PixelArt.fill(graphics, x + col, y + 2, x + col + 1, y + 5, PixelArt.argb(hot, 1));
            if ((col + now / 120) % 7 == 0) {
                PixelArt.fill(graphics, x + col, y + 1, x + col + 1, y + 2, PixelArt.argb(0xFFF0B0, 1));
            }
        }
        PixelArt.flush(graphics);
    }

    /** Pixel flames that burn lower as the fuel runs out, with a few blue tongues of hextech among them. */
    private static void flames(GuiGraphicsExtractor graphics, int x, int y, float fuel) {
        long now = Util.getMillis();
        for (int col = 0; col < 14; col++) {
            graphics.fill(x + col, y + 14, x + col + 1, y + 16, fuel > 0 ? 0xFF5A2410 : 0xFF2A2420);
            if (fuel <= 0) {
                continue;
            }
            float flicker = 0.5F + 0.5F * Mth.sin(now / 110.0F + col * 1.7F);
            int height = 2 + Math.round(11 * fuel * flicker);
            boolean blue = col % 5 == 2;
            for (int row = 0; row < height; row++) {
                int colour = blue ? (row < height / 2 ? 0xFF3FA9D8 : 0xFFA8ECFF)
                        : row < height / 3 ? 0xFFFF7A1C : row < height * 2 / 3 ? 0xFFFFB02E : 0xFFFFE680;
                if ((col + row) % 3 != 0 || row < 2) {
                    graphics.fill(x + col, y + 13 - row, x + col + 1, y + 14 - row, colour);
                }
            }
        }
    }

    @Override
    protected boolean isOutput(int slot) {
        return slot == HexFurnaceBlockEntity.OUTPUT;
    }

    @Override
    protected String slotHint(int slot) {
        return switch (slot) {
            case HexFurnaceBlockEntity.FUEL -> "hex_furnace.fuel";
            case HexFurnaceBlockEntity.OUTPUT -> "hex_furnace.output";
            default -> "hex_furnace.input";
        };
    }
}
