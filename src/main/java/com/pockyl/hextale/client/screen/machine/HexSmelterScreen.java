package com.pockyl.hextale.client.screen.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.HexSmelterBlockEntity;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.menu.HexSmelterMenu;
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
    public static final int TANK_Y = 16;
    public static final int SOCKET_X = 146;
    public static final int SOCKET_Y = 14;
    public static final int SOCKET = 18;

    public HexSmelterScreen(HexSmelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, Hextale.id("textures/gui/hex_smelter.png"));
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
        arrow(graphics, x + 80, y + 35, 20, menu.progress() / (float) menu.cookTicks());
        if ("output_full".equals(smelter.blocked())) {
            warn(graphics, x + HexSmelterMenu.OUTPUT_X - 4, y + HexSmelterMenu.OUTPUT_Y - 4, 24, 24);
        }
        sockets(graphics, smelter.runes(), x + SOCKET_X, y + SOCKET_Y, mouseX, mouseY);
    }

    /** Three hexagonal rune sockets; a burned-in rune glows in its colour with its stars under it. */
    private void sockets(GuiGraphicsExtractor graphics, PickaxeRunes runes, int x, int y, int mouseX, int mouseY) {
        List<PickaxeRunes.Entry> entries = runes.entries();
        float pulse = 0.5F + 0.5F * Mth.sin(Util.getMillis() / 500.0F);
        for (int i = 0; i < SmelterRune.SLOTS; i++) {
            int sy = y + i * (SOCKET + 6);
            int cx = x + SOCKET / 2;
            int cy = sy + SOCKET / 2;
            if (i >= entries.size()) {
                PixelArt.hex(graphics, cx, cy, SOCKET, PixelArt.argb(0x1A1424, 1), PixelArt.argb(0x4A3A28, 1));
                if (isOver(mouseX, mouseY, x, sy, SOCKET, SOCKET)) {
                    tooltip(List.of(Component.translatable("gui.hextale.hex_smelter.socket").withStyle(ChatFormatting.GRAY)));
                }
                continue;
            }
            ComplexRune rune = entries.get(i).rune();
            int stars = entries.get(i).stars();
            PixelArt.hex(graphics, cx, cy, SOCKET + 4, PixelArt.argb(rune.color(), 0.18F + 0.12F * pulse), 0);
            PixelArt.hex(graphics, cx, cy, SOCKET, PixelArt.argb(PixelArt.mix(rune.color(), 0x0A0710, 0.75F), 1), PixelArt.argb(0xF0C24A, 1));
            PixelArt.glyph(graphics, rune, cx - 6, cy - 6, 12, PixelArt.argb(PixelArt.mix(rune.color(), 0xFFFFFF, 0.2F), 1), 0xFF000000);
            for (int s = 0; s < 3; s++) {
                PixelArt.fill(graphics, cx - 4 + s * 3, sy + SOCKET + 1, cx - 2 + s * 3, sy + SOCKET + 3,
                        PixelArt.argb(s < stars ? 0xF0C24A : 0x3A3460, 1));
            }
            if (isOver(mouseX, mouseY, x, sy, SOCKET, SOCKET)) {
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
    protected String slotHint(int slot) {
        return switch (slot) {
            case HexSmelterBlockEntity.INPUT -> "hex_smelter.input";
            case HexSmelterBlockEntity.BOTTLE_IN -> "hex_smelter.bottle";
            case HexSmelterBlockEntity.BOTTLE_OUT -> "hex_smelter.empty";
            default -> "hex_smelter.output";
        };
    }
}
