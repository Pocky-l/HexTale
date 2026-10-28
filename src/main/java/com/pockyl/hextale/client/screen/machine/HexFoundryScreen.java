package com.pockyl.hextale.client.screen.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

import com.pockyl.hextale.block.HexFoundryBlock;
import com.pockyl.hextale.block.HexFoundryBlockEntity;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.client.screen.HexPlate;
import com.pockyl.hextale.menu.HexFoundryMenu;

import java.util.List;

/**
 * The Hex Foundry: the charge in a square at the left, every piece glowing from below as it melts, the ingots at the
 * right; between them the kiln's heat as a bar of cooling iron (dark, then red, orange and white-hot, the line where
 * melting begins marked), the arrow, and the fire over the fuel slot burning down.
 */
public class HexFoundryScreen extends MachineScreen<HexFoundryMenu> {
    private static final int GAUGE_X = 66;
    private static final int GAUGE_Y = 23;
    private static final int GAUGE_W = 46;
    private static final int GAUGE_H = 5;
    /** The arrow to the ingots (a click opens the smelting recipes in JEI). */
    public static final int ARROW_X = 75;
    public static final int ARROW_Y = 34;
    private static final int FLAME_X = HexFoundryMenu.FUEL_X + 1;
    private static final int FLAME_Y = 46;

    public HexFoundryScreen(HexFoundryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        float heat = menu.heat() / (float) HexFoundryBlockEntity.MAX_HEAT;
        float most = 0;
        for (int slot = 0; slot < HexFoundryBlockEntity.INPUTS; slot++) {
            float melt = menu.melt(slot);
            most = Math.max(most, melt);
            if (melt > 0) {
                glow(graphics, x + HexFoundryMenu.INPUT_X + slot % 3 * 18, y + HexFoundryMenu.GRID_Y + slot / 3 * 18, melt, heat);
            }
        }
        gauge(graphics, x + GAUGE_X, y + GAUGE_Y, heat);
        if (isOver(mouseX, mouseY, x + GAUGE_X - 1, y + GAUGE_Y - 1, GAUGE_W + 2, GAUGE_H + 2)) {
            int percent = Math.round(heat * 100);
            tooltip(List.of(Component.translatable("gui.hextale.hex_foundry.heat", percent),
                    Component.translatable(menu.heat() < HexFoundryBlockEntity.MELT_HEAT ? "gui.hextale.hex_foundry.heat.cold"
                            : "gui.hextale.hex_foundry.heat.hint").withStyle(ChatFormatting.GRAY)));
        }
        arrow(graphics, x + ARROW_X, y + ARROW_Y, 22, most);
        flames(graphics, x + FLAME_X, y + FLAME_Y, menu.fire());
        if (isOver(mouseX, mouseY, x + FLAME_X, y + FLAME_Y, 14, 14)) {
            tooltip(List.of(Component.translatable("gui.hextale.hex_foundry.fire"),
                    Component.translatable("gui.hextale.hex_foundry.fire.hint").withStyle(ChatFormatting.GRAY)));
        }
        HexFoundryBlockEntity foundry = blockEntity(HexFoundryBlockEntity.class);
        if (foundry == null) {
            return;
        }
        switch (foundry.blocked()) {
            case "no_fire" -> warn(graphics, x + HexFoundryMenu.FUEL_X, y + HexFoundryMenu.FUEL_Y, 16, 16);
            case "output_full" -> warn(graphics, x + HexFoundryMenu.OUTPUT_X, y + HexFoundryMenu.GRID_Y, 52, 52);
            default -> {
            }
        }
        if (isOver(mouseX, mouseY, x + ARROW_X, y + ARROW_Y, 28, 10)) {
            tooltip(List.of(HexFoundryBlock.status(foundry)));
        }
    }

    /** The well of the heat bar: iron that glows up from dark red to white as the kiln heats, a notch where melting starts. */
    private static void gauge(GuiGraphicsExtractor graphics, int x, int y, float heat) {
        HexPlate.well(graphics, x - 1, y - 1, GAUGE_W + 2, GAUGE_H + 2);
        PixelArt.fill(graphics, x, y, x + GAUGE_W, y + GAUGE_H, PixelArt.argb(0x1A1210, 1));
        int filled = Math.round(GAUGE_W * Mth.clamp(heat, 0, 1));
        long now = Util.getMillis();
        for (int col = 0; col < filled; col++) {
            float t = col / (float) (GAUGE_W - 1);
            int colour = t < 0.35F ? PixelArt.mix(0x5A1408, 0xC2361A, t / 0.35F)
                    : t < 0.7F ? PixelArt.mix(0xC2361A, 0xF29A2E, (t - 0.35F) / 0.35F)
                    : PixelArt.mix(0xF29A2E, 0xFFF1C8, (t - 0.7F) / 0.3F);
            float flicker = 0.85F + 0.15F * Mth.sin(now / 140.0F + col * 0.8F);
            PixelArt.fill(graphics, x + col, y, x + col + 1, y + GAUGE_H, PixelArt.argb(PixelArt.mix(0x1A1210, colour, flicker), 1));
            PixelArt.fill(graphics, x + col, y, x + col + 1, y + 1, PixelArt.argb(PixelArt.mix(colour, 0xFFFFFF, 0.35F), 1));
        }
        int notch = x + Math.round(GAUGE_W * HexFoundryBlockEntity.MELT_HEAT / (float) HexFoundryBlockEntity.MAX_HEAT);
        PixelArt.fill(graphics, notch, y - 1, notch + 1, y + GAUGE_H + 1, PixelArt.argb(HexPlate.BRASS_LIGHT, 1));
        PixelArt.flush(graphics);
    }

    /** A piece melting in its slot: an ember glow rising from the slot's floor, brighter the hotter the kiln. */
    private static void glow(GuiGraphicsExtractor graphics, int x, int y, float melt, float heat) {
        int height = Math.max(1, Math.round(16 * Mth.clamp(melt, 0, 1)));
        int colour = PixelArt.mix(0xC2361A, 0xFFC35A, Mth.clamp(heat, 0, 1));
        for (int row = 0; row < height; row++) {
            float alpha = 0.55F * (1 - row / 16F);
            PixelArt.fill(graphics, x, y + 15 - row, x + 16, y + 16 - row, PixelArt.argb(colour, alpha));
        }
        PixelArt.flush(graphics);
    }

    /** Pixel flames over the fuel slot that burn lower as the fire runs out. */
    private static void flames(GuiGraphicsExtractor graphics, int x, int y, float fire) {
        long now = Util.getMillis();
        for (int col = 0; col < 14; col++) {
            graphics.fill(x + col, y + 12, x + col + 1, y + 14, fire > 0 ? 0xFF5A2410 : 0xFF2A1C12);
            if (fire <= 0) {
                continue;
            }
            float flicker = 0.5F + 0.5F * Mth.sin(now / 110.0F + col * 1.7F);
            int height = 2 + Math.round(10 * Mth.clamp(fire, 0, 1) * flicker);
            for (int row = 0; row < height; row++) {
                int colour = row < height / 3 ? 0xFFE0471C : row < height * 2 / 3 ? 0xFFFF9A2E : 0xFFFFE08A;
                if ((col + row) % 3 != 0 || row < 2) {
                    graphics.fill(x + col, y + 11 - row, x + col + 1, y + 12 - row, colour);
                }
            }
        }
    }

    @Override
    protected boolean isOutput(int slot) {
        return slot >= HexFoundryBlockEntity.OUTPUT_START && slot < HexFoundryBlockEntity.FUEL;
    }

    @Override
    protected String slotHint(int slot) {
        if (slot == HexFoundryBlockEntity.FUEL) {
            return "hex_foundry.fuel";
        }
        return slot >= HexFoundryBlockEntity.OUTPUT_START ? "hex_foundry.output" : "hex_foundry.input";
    }
}
