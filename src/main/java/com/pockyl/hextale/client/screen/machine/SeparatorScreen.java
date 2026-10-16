package com.pockyl.hextale.client.screen.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.PortMode;
import com.pockyl.hextale.block.SeparatorBlock;
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
    /** The port panel right of the screen: the block seen from above, its front towards the viewer (down). */
    private static final int PANEL_X = 178;
    private static final int PANEL_Y = 4;
    private static final int PANEL_W = 38;
    private static final int PANEL_H = 52;
    private static final int CELL = 10;

    public SeparatorScreen(SeparatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, Hextale.id("textures/gui/separator.png"), PANEL_X + PANEL_W);
    }

    /** Top-left corner of a face's cell on the panel (screen coordinates). */
    private int[] cell(SeparatorBlock.Side side) {
        int cx = leftPos + PANEL_X + (PANEL_W - CELL) / 2;
        int cy = topPos + PANEL_Y + 18;
        return switch (side) {
            case BACK -> new int[]{cx, cy - CELL - 1};
            case LEFT -> new int[]{cx - CELL - 1, cy};
            case RIGHT -> new int[]{cx + CELL + 1, cy};
            case FRONT -> new int[]{cx, cy + CELL + 1};
        };
    }

    /** The port panel: one cell per face, coloured by its mode, with a tooltip saying what the face does. */
    private void renderPorts(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos + PANEL_X;
        int y = topPos + PANEL_Y;
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFF4A3312);
        graphics.fill(x + 1, y + 1, x + PANEL_W - 1, y + PANEL_H - 1, 0xFF1C140C);
        graphics.fill(x + 1, y + 1, x + PANEL_W - 1, y + 2, 0xFFC29445);
        // Where the viewer stands: a small arrow under the front cell.
        int[] front = cell(SeparatorBlock.Side.FRONT);
        for (int k = 0; k < 3; k++) {
            graphics.fill(front[0] + CELL / 2 - 1 - k, front[1] + CELL + 2 + k, front[0] + CELL / 2 + 1 + k, front[1] + CELL + 3 + k, 0xFF8A7A60);
        }
        for (SeparatorBlock.Side side : SeparatorBlock.Side.values()) {
            int[] at = cell(side);
            PortMode mode = menu.mode(side);
            boolean over = isOver(mouseX, mouseY, at[0], at[1], CELL, CELL);
            int rim = over ? 0xFFF8E3A0 : 0xFF9C7230;
            graphics.fill(at[0], at[1], at[0] + CELL, at[1] + CELL, rim);
            graphics.fill(at[0] + 1, at[1] + 1, at[0] + CELL - 1, at[1] + CELL - 1, colour(mode));
            switch (mode) {
                case INLET -> graphics.fill(at[0] + 3, at[1] + 3, at[0] + CELL - 3, at[1] + CELL - 3, 0xFF15121A);
                case PRODUCT -> {
                    graphics.fill(at[0] + 3, at[1] + 3, at[0] + CELL - 3, at[1] + CELL - 3, 0xFF98E3FB);
                    graphics.fill(at[0] + 4, at[1] + 4, at[0] + CELL - 4, at[1] + CELL - 4, 0xFF15121A);
                }
                case WASTE -> graphics.fill(at[0] + 2, at[1] + 4, at[0] + CELL - 2, at[1] + 6, 0xFFE9C66D);
                default -> {
                }
            }
            if (over) {
                tooltip(List.of(Component.translatable("gui.hextale.separator.side." + side.name().toLowerCase(java.util.Locale.ROOT))
                                .append(": ").append(Component.translatable("gui.hextale.port." + mode.getSerializedName())),
                        Component.translatable("gui.hextale.port." + mode.getSerializedName() + ".hint").withStyle(ChatFormatting.GRAY),
                        Component.translatable("gui.hextale.port.click").withStyle(ChatFormatting.DARK_GRAY)));
            }
        }
        // The top: the gas vent, no pipe.
        int vx = leftPos + PANEL_X + (PANEL_W - CELL) / 2;
        int vy = topPos + PANEL_Y + 18;
        graphics.fill(vx, vy, vx + CELL, vy + CELL, 0xFF4A3312);
        graphics.fill(vx + 2, vy + 2, vx + CELL - 2, vy + CELL - 2, 0xFF8A9A7A);
        graphics.fill(vx + 4, vy + 4, vx + CELL - 4, vy + CELL - 4, 0xFF15121A);
        if (isOver(mouseX, mouseY, vx, vy, CELL, CELL)) {
            tooltip(List.of(Component.translatable("gui.hextale.separator.vent"),
                    Component.translatable("gui.hextale.separator.vent.hint").withStyle(ChatFormatting.GRAY)));
        }
        if (isOver(mouseX, mouseY, x, y, PANEL_W, 8)) {
            tooltip(List.of(Component.translatable("gui.hextale.separator.ports"),
                    Component.translatable("gui.hextale.separator.ports.hint").withStyle(ChatFormatting.GRAY)));
        }
    }

    private static int colour(PortMode mode) {
        return switch (mode) {
            case INLET -> 0xFF6D7F8A;
            case PRODUCT -> 0xFF1A557F;
            case WASTE -> 0xFF7A4A2A;
            default -> 0xFF2A1B0C;
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (SeparatorBlock.Side side : SeparatorBlock.Side.values()) {
            int[] at = cell(side);
            if (isOver(event.x(), event.y(), at[0], at[1], CELL, CELL) && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, side.ordinal());
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
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
        renderPorts(graphics, mouseX, mouseY);
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
