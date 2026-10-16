package com.pockyl.hextale.client.screen.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.essence.Reagents;
import com.pockyl.hextale.menu.SeparatorMenu;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.List;

/**
 * The Separator, left to right: the mixture's tank, the reagent (the element it binds above it, its remaining charge
 * under it) and the two products it splits the mixture into - the concentrate above, leaving through the top, and the
 * rest below, leaving through the front. When it cannot work, the part in the way glows red and says why.
 */
public class SeparatorScreen extends MachineScreen<SeparatorMenu> {
    public static final int MIX_X = 36;
    public static final int MIX_Y = 16;
    public static final int OUT_TANK_X = 106;
    public static final int EXTRACT_Y = 16;
    public static final int REST_Y = 46;
    public static final int OUT_TANK_H = 24;

    public SeparatorScreen(SeparatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, Hextale.id("textures/gui/separator.png"));
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SeparatorBlockEntity separator = blockEntity(SeparatorBlockEntity.class);
        int x = leftPos;
        int y = topPos;
        String blocked = separator == null ? "empty" : separator.blocked();
        if (separator != null) {
            tank(graphics, x + MIX_X, y + MIX_Y, TANK_H, separator.mix(), mouseX, mouseY, Component.translatable("gui.hextale.separator.mix"));
            tank(graphics, x + OUT_TANK_X, y + EXTRACT_Y, OUT_TANK_H, separator.extract(), mouseX, mouseY,
                    Component.translatable("gui.hextale.separator.extract"));
            tank(graphics, x + OUT_TANK_X, y + REST_Y, OUT_TANK_H, separator.rest(), mouseX, mouseY,
                    Component.translatable("gui.hextale.separator.rest"));
            if ("extract_full".equals(blocked)) {
                warn(graphics, x + OUT_TANK_X, y + EXTRACT_Y, 16, OUT_TANK_H);
            } else if ("rest_full".equals(blocked)) {
                warn(graphics, x + OUT_TANK_X, y + REST_Y, 16, OUT_TANK_H);
            }
        }
        SimpleRune element = menu.charge() > 0 && menu.chargeElement() >= 0 ? SimpleRune.values()[menu.chargeElement()]
                : separator == null ? null : Reagents.element(separator.getItem(SeparatorBlockEntity.REAGENT));
        int rx = x + SeparatorMenu.REAGENT_X;
        int ry = y + SeparatorMenu.REAGENT_Y;
        // The element the reagent binds, above it; its charge under it.
        if (element != null) {
            PixelArt.radical(graphics, element, rx + 3, ry - 13, 9, PixelArt.argb(element.color(), 1));
            if (isOver(mouseX, mouseY, rx, ry - 14, 16, 12)) {
                tooltip(List.of(Component.translatable("gui.hextale.separator.binds", ResearchText.element(element))));
            }
        }
        if ("no_reagent".equals(blocked)) {
            warn(graphics, rx, ry, 16, 16);
            if (isOver(mouseX, mouseY, rx, ry, 16, 16) && separator.getItem(SeparatorBlockEntity.REAGENT).isEmpty()) {
                tooltip(List.of(Component.translatable("gui.hextale.separator.no_reagent"),
                        Component.translatable("gui.hextale.separator.no_reagent.hint").withStyle(ChatFormatting.GRAY)));
            }
        }
        int charge = Math.round(16 * menu.charge() / (float) Reagents.BINDS);
        graphics.fill(rx, ry + 18, rx + 16, ry + 20, 0xFF2A1B0C);
        graphics.fill(rx, ry + 18, rx + charge, ry + 20, element == null ? 0xFF8A7A60 : 0xFF000000 | element.color());
        float progress = menu.step() / (float) SeparatorBlockEntity.STEP_TICKS;
        arrow(graphics, x + 55, y + 39, 6, blocked == null ? 1 : 0);
        arrow(graphics, x + 88, y + 23, 8, progress);
        arrow(graphics, x + 88, y + 53, 8, progress);
        PixelArt.flush(graphics);
    }

    @Override
    protected String slotHint(int slot) {
        return switch (slot) {
            case SeparatorBlockEntity.POUR -> "separator.pour";
            case SeparatorBlockEntity.REAGENT -> "separator.reagent";
            case SeparatorBlockEntity.SUPPLY -> "bottles";
            case SeparatorBlockEntity.EXTRACT_OUT -> "separator.extract";
            default -> "separator.rest";
        };
    }

    /** A pulsing red frame around the part that stops the separator. */
    private static void warn(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int alpha = Math.round(120 + 100 * Mth.sin(Util.getMillis() / 200.0F));
        int colour = alpha << 24 | 0xE0503C;
        graphics.fill(x - 1, y - 1, x + w + 1, y, colour);
        graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, colour);
        graphics.fill(x - 1, y, x, y + h, colour);
        graphics.fill(x + w, y, x + w + 1, y + h, colour);
    }
}
