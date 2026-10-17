package com.pockyl.hextale.client.screen.machine;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.PortMode;
import com.pockyl.hextale.block.SeparatorBlock;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.menu.SeparatorMenu;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.Essence;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The Separator, left to right: the mixture's tank, the essence to draw out (a plate above the blaze powder slot: click
 * it to pick any of the essences from a grid, right click clears it, the wheel steps through them; the powder's charge
 * under the slot) and the two products it splits the mixture into - the concentrate above, leaving through the top, and
 * the rest below, leaving through the front. When it cannot work, the part in the way glows red and says why.
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
    /** The essence picker: nine columns, rows of primals, pairs and deeper compounds with a gap between the groups. */
    private static final int PICK_CELL = 11;
    private static final int PICK_COLUMNS = 9;
    private static final int PICK_GAP = 4;
    private static final int PICK_PAD = 5;
    private boolean picking;

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
                    // Closed: a small cross.
                    for (int k = 2; k < CELL - 2; k++) {
                        graphics.fill(at[0] + k, at[1] + k, at[0] + k + 1, at[1] + k + 1, 0xFF8A7A60);
                        graphics.fill(at[0] + CELL - 1 - k, at[1] + k, at[0] + CELL - k, at[1] + k + 1, 0xFF8A7A60);
                    }
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
            case INLET -> 0xFF3E8A4E;
            case PRODUCT -> 0xFF1A557F;
            case WASTE -> 0xFF9A4A22;
            default -> 0xFF2A1B0C;
        };
    }

    /** The plate above the powder slot that shows (and opens the choice of) the essence to draw out. */
    private boolean overChooser(double mouseX, double mouseY) {
        return isOver(mouseX, mouseY, leftPos + SeparatorMenu.REAGENT_X, topPos + SeparatorMenu.REAGENT_Y - 15, 16, 13);
    }

    private static int pickRows() {
        int pairs = 0;
        int deep = 0;
        for (Essence essence : Essence.values()) {
            if (essence.tier() == 1) {
                pairs++;
            } else if (essence.tier() > 1) {
                deep++;
            }
        }
        return 1 + ceil(pairs) + ceil(deep);
    }

    private static int ceil(int count) {
        return (count + PICK_COLUMNS - 1) / PICK_COLUMNS;
    }

    private int pickWidth() {
        return PICK_COLUMNS * PICK_CELL + PICK_PAD * 2;
    }

    private int pickHeight() {
        return pickRows() * PICK_CELL + PICK_GAP * 2 + PICK_PAD * 2 + 10;
    }

    private int pickLeft() {
        return leftPos + 88 - pickWidth() / 2;
    }

    private int pickTop() {
        return topPos + 4;
    }

    /** Top-left corner of an essence's cell in the picker (screen coordinates). */
    private int[] pickCell(Essence essence) {
        int group = essence.isPrimal() ? 0 : essence.tier() == 1 ? 1 : 2;
        int index = 0;
        for (Essence other : Essence.values()) {
            if (other == essence) {
                break;
            }
            int otherGroup = other.isPrimal() ? 0 : other.tier() == 1 ? 1 : 2;
            if (otherGroup == group) {
                index++;
            }
        }
        int pairs = (int) java.util.Arrays.stream(Essence.values()).filter(e -> !e.isPrimal() && e.tier() == 1).count();
        int row = (group == 0 ? 0 : group == 1 ? 1 : 1 + ceil(pairs)) + index / PICK_COLUMNS;
        int x = pickLeft() + PICK_PAD + index % PICK_COLUMNS * PICK_CELL;
        int y = pickTop() + PICK_PAD + 10 + row * PICK_CELL + group * PICK_GAP;
        return new int[]{x, y};
    }

    @Nullable
    private Essence pickedAt(double mouseX, double mouseY) {
        for (Essence essence : Essence.values()) {
            int[] at = pickCell(essence);
            if (isOver(mouseX, mouseY, at[0], at[1], PICK_CELL, PICK_CELL)) {
                return essence;
            }
        }
        return null;
    }

    private void choose(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    /** The picker, over everything else: every essence in its colour, the chosen one framed in gold. */
    private void renderPicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.nextStratum();
        int x = pickLeft();
        int y = pickTop();
        int w = pickWidth();
        int h = pickHeight();
        graphics.fill(x, y, x + w, y + h, 0xFF4A3312);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xF0120D16);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFC29445);
        graphics.centeredText(font, Component.translatable("gui.hextale.separator.choose"), x + w / 2, y + PICK_PAD, 0xFFE9C66D);
        Essence chosen = menu.chosen();
        Essence over = pickedAt(mouseX, mouseY);
        for (Essence essence : Essence.values()) {
            int[] at = pickCell(essence);
            if (essence == chosen) {
                graphics.fill(at[0], at[1], at[0] + PICK_CELL, at[1] + PICK_CELL, 0xFFE9C66D);
                graphics.fill(at[0] + 1, at[1] + 1, at[0] + PICK_CELL - 1, at[1] + PICK_CELL - 1, 0xFF120D16);
            } else if (essence == over) {
                graphics.fill(at[0], at[1], at[0] + PICK_CELL, at[1] + PICK_CELL, 0xFF6A5A40);
                graphics.fill(at[0] + 1, at[1] + 1, at[0] + PICK_CELL - 1, at[1] + PICK_CELL - 1, 0xFF120D16);
            }
            PixelArt.essence(graphics, essence, at[0] + 1, at[1] + 1, 9, PixelArt.argb(essence.color(), 1));
        }
        PixelArt.flush(graphics);
        if (over != null) {
            graphics.setComponentTooltipForNextFrame(font, List.of(ResearchText.essence(over)), mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (picking) {
            renderPicker(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!picking && overChooser(mouseX, mouseY) && scrollY != 0) {
            Essence chosen = menu.chosen();
            int next = chosen == null ? (scrollY < 0 ? 0 : Essence.COUNT - 1)
                    : Math.floorMod(chosen.ordinal() + (scrollY < 0 ? 1 : -1), Essence.COUNT);
            choose(SeparatorMenu.CHOOSE + next);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (picking) {
            // While the picker is open every click belongs to it: an essence chooses, anything else closes it.
            Essence essence = pickedAt(event.x(), event.y());
            if (essence != null) {
                choose(SeparatorMenu.CHOOSE + essence.ordinal());
                if (minecraft != null) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
            }
            picking = false;
            return true;
        }
        if (overChooser(event.x(), event.y())) {
            if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
                choose(SeparatorMenu.CHOOSE - 1);
            } else {
                picking = true;
            }
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
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
        Essence element = menu.chosen();
        int rx = x + SeparatorMenu.REAGENT_X;
        int ry = y + SeparatorMenu.REAGENT_Y;
        // The essence to draw out on a small plate above the powder; the powder's charge under it.
        boolean overPlate = overChooser(mouseX, mouseY) && !picking;
        graphics.fill(rx, ry - 15, rx + 16, ry - 2, overPlate ? 0xFFF8E3A0 : 0xFF9C7230);
        graphics.fill(rx + 1, ry - 14, rx + 15, ry - 3, 0xFF1C140C);
        if (element != null) {
            PixelArt.essence(graphics, element, rx + 3, ry - 13, 9, PixelArt.argb(element.color(), 1));
        } else {
            PixelArt.flush(graphics);
            graphics.centeredText(font, "?", rx + 8, ry - 13, 0xFF8A7A60);
        }
        if ("no_essence".equals(blocked)) {
            warn(graphics, rx, ry - 15, 16, 13);
        }
        if (overPlate) {
            tooltip(List.of(element == null ? Component.translatable("gui.hextale.separator.no_essence")
                            : Component.translatable("gui.hextale.separator.binds", ResearchText.essence(element)),
                    Component.translatable("gui.hextale.separator.choose.hint").withStyle(ChatFormatting.GRAY)));
        }
        if ("no_fuel".equals(blocked)) {
            warn(graphics, rx, ry, 16, 16);
            if (isOver(mouseX, mouseY, rx, ry, 16, 16) && separator.getItem(SeparatorBlockEntity.FUEL).isEmpty()) {
                tooltip(List.of(Component.translatable("gui.hextale.separator.no_fuel"),
                        Component.translatable("gui.hextale.separator.no_fuel.hint").withStyle(ChatFormatting.GRAY)));
            }
        }
        int charge = Math.round(16 * Math.min(menu.charge(), SeparatorBlockEntity.FUEL_BINDS) / (float) SeparatorBlockEntity.FUEL_BINDS);
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
            case SeparatorBlockEntity.FUEL -> "separator.fuel";
            case SeparatorBlockEntity.SUPPLY -> "bottles";
            case SeparatorBlockEntity.EXTRACT_OUT -> "separator.extract";
            default -> "separator.rest";
        };
    }

}
