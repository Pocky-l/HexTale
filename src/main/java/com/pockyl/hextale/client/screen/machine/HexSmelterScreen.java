package com.pockyl.hextale.client.screen.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

import com.pockyl.hextale.block.HexSmelterBlockEntity;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.client.screen.HexPlate;
import com.pockyl.hextale.menu.HexSmelterMenu;
import com.pockyl.hextale.menu.MachineMenu;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.SmelterRune;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hex Smelter: bottles of fiery essence pour into its tank (left), the heat gauge under the input glows while the
 * drawn heat lasts, the arrow fills as the item smelts. Three rune sockets on the right show the runes burned in.
 */
public class HexSmelterScreen extends MachineScreen<HexSmelterMenu> {
    public static final int TANK_X = 30;
    public static final int TANK_Y = 24;
    /** The rune sockets: a column of hexagons centred on this x, spread evenly over the machine part of the plate. */
    public static final int SOCKET_CX = 158;
    public static final int SOCKET_TOP = 18;
    public static final int SOCKET_BOTTOM = MachineMenu.INVENTORY_Y - 16;
    public static final int SOCKET = 18;

    public HexSmelterScreen(HexSmelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        HexSmelterBlockEntity smelter = blockEntity(HexSmelterBlockEntity.class);
        int x = leftPos;
        int y = topPos;
        if (smelter == null) {
            return;
        }
        tank(graphics, x + TANK_X, y + TANK_Y, TANK_H, smelter.tank(), mouseX, mouseY,
                Component.translatable("gui.hextale.hex_smelter.tank"));
        if ("no_heat".equals(smelter.blocked())) {
            warn(graphics, x + TANK_X, y + TANK_Y, TANK_W, TANK_H);
        }
        float share = Mth.clamp(menu.heat() / (float) Math.max(1, smelter.heatPerItem()), 0, 1);
        heat(graphics, x + HexSmelterMenu.INPUT_X, y + HexSmelterMenu.INPUT_Y + 19, share, menu.progress() > 0);
        if (isOver(mouseX, mouseY, x + HexSmelterMenu.INPUT_X, y + HexSmelterMenu.INPUT_Y + 19, 16, 16)) {
            tooltip(List.of(Component.translatable("gui.hextale.hex_smelter.heat"),
                    Component.translatable("gui.hextale.hex_smelter.heat.hint", HexSmelterBlockEntity.HEAT_PER_ITEM)
                            .withStyle(ChatFormatting.GRAY)));
        }
        arrow(graphics, x + 80, y + 43, 20, menu.progress() / (float) menu.cookTicks());
        if ("output_full".equals(smelter.blocked())) {
            warn(graphics, x + HexSmelterMenu.OUTPUT_X - 4, y + HexSmelterMenu.OUTPUT_Y - 4, 24, 24);
        }
        sockets(graphics, smelter.runes(), x + SOCKET_CX, y + SOCKET_TOP, mouseX, mouseY);
    }

    /**
     * Three hexagonal rune sockets in a column centred on {@code cx}; a burned-in rune glows in its colour, its stars as
     * pips left of its socket.
     */
    private void sockets(GuiGraphicsExtractor graphics, PickaxeRunes runes, int cx, int top, int mouseX, int mouseY) {
        List<PickaxeRunes.Entry> entries = runes.entries();
        float pulse = 0.5F + 0.5F * Mth.sin(Util.getMillis() / 500.0F);
        int height = PixelArt.Hex.of(SOCKET).height;
        int gap = Math.max(0, (SOCKET_BOTTOM - SOCKET_TOP - SmelterRune.SLOTS * height) / (SmelterRune.SLOTS - 1));
        int spread = SmelterRune.SLOTS * height + (SmelterRune.SLOTS - 1) * gap;
        int first = top + (SOCKET_BOTTOM - SOCKET_TOP - spread) / 2;
        for (int i = 0; i < SmelterRune.SLOTS; i++) {
            int sy = first + i * (height + gap);
            int cy = sy + height / 2;
            boolean over = isOver(mouseX, mouseY, cx - SOCKET / 2, sy, SOCKET + 1, height);
            if (i >= entries.size()) {
                PixelArt.hex(graphics, cx, cy, SOCKET, PixelArt.argb(0x0A0810, 1), PixelArt.argb(over ? HexPlate.BRASS : 0x4A4060, 1));
                if (over) {
                    tooltip(List.of(Component.translatable("gui.hextale.hex_smelter.socket").withStyle(ChatFormatting.GRAY)));
                }
                continue;
            }
            ComplexRune rune = entries.get(i).rune();
            int stars = entries.get(i).stars();
            PixelArt.hex(graphics, cx, cy, SOCKET + 2, PixelArt.argb(rune.color(), 0.18F + 0.12F * pulse), 0);
            PixelArt.hex(graphics, cx, cy, SOCKET, PixelArt.argb(PixelArt.mix(rune.color(), 0x0A0710, 0.75F), 1),
                    PixelArt.argb(over ? HexPlate.BRASS_LIGHT : 0xF0C24A, 1));
            PixelArt.glyph(graphics, rune, cx - 6, cy - 6, 12, PixelArt.argb(PixelArt.mix(rune.color(), 0xFFFFFF, 0.2F), 1), 0xFF000000);
            for (int s = 0; s < 3; s++) {
                int py = cy - 4 + s * 3;
                PixelArt.fill(graphics, cx - SOCKET / 2 - 5, py, cx - SOCKET / 2 - 3, py + 2,
                        PixelArt.argb(s < stars ? 0xF0C24A : 0x3A3460, 1));
            }
            if (over) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable(rune.translationKey()).withColor(rune.color())
                        .append(Component.literal(" " + "★".repeat(stars)).withColor(0xF0C24A)));
                SmelterRune smelterRune = SmelterRune.of(rune);
                if (smelterRune != null) {
                    lines.add(Component.translatable(smelterRune.descriptionKey()).withStyle(ChatFormatting.GRAY));
                    int strength = runes.strength(rune);
                    if (smelterRune != SmelterRune.LENS) {
                        lines.add(Component.translatable(smelterRune.valueKey(), smelterRune.amount(strength)).withStyle(ChatFormatting.GOLD));
                    }
                }
                tooltip(lines);
            }
        }
        PixelArt.flush(graphics);
    }

    /** A pixel heat gauge: coals that glow with the heat stored; flames lick up while it smelts. */
    private static void heat(GuiGraphicsExtractor graphics, int x, int y, float share, boolean working) {
        long now = Util.getMillis();
        for (int col = 0; col < 16; col++) {
            float flicker = 0.5F + 0.5F * Mth.sin(now / 110.0F + col * 1.7F);
            int coal = share > 0 ? PixelArt.mix(0x3A1A0C, 0xFF7A1C, share * (0.6F + 0.4F * flicker)) : 0x2A2420;
            graphics.fill(x + col, y + 12, x + col + 1, y + 16, 0xFF000000 | coal);
            if (working) {
                int height = 2 + Math.round(8 * share * flicker);
                for (int row = 0; row < height; row++) {
                    int colour = row < height / 3 ? 0xFFFF7A1C : row < height * 2 / 3 ? 0xFFFFB02E : 0xFFFFE680;
                    if ((col + row) % 3 != 0 || row < 2) {
                        graphics.fill(x + col, y + 11 - row, x + col + 1, y + 12 - row, colour);
                    }
                }
            }
        }
    }

    @Override
    protected boolean isOutput(int slot) {
        return slot == HexSmelterBlockEntity.OUTPUT;
    }

    @Override
    protected String slotHint(int slot) {
        return switch (slot) {
            case HexSmelterBlockEntity.INPUT -> "hex_smelter.input";
            case HexSmelterBlockEntity.BOTTLE_IN -> "hex_smelter.bottle";
            case HexSmelterBlockEntity.BOTTLE_OUT -> "hex_smelter.empty";
            default -> "hex_smelter.output";
        };
    }
}
