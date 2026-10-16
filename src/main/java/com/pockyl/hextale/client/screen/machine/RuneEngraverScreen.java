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
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.RuneEngraverBlockEntity;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.menu.RuneEngraverMenu;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.runework.HextechTools;
import com.pockyl.hextale.runework.RuneSlots;
import com.pockyl.hextale.runework.ToolRunes;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The Rune Engraver: scroll and bottle on the left, the tool in the vise, its essence cell, and one plaque per rune slot
 * on the right. A free plaque that can take the scroll glows - click it to engrave; shift-click an engraved plaque to
 * grind its rune off.
 */
public class RuneEngraverScreen extends MachineScreen<RuneEngraverMenu> {
    public static final int CELL_X = 68;
    public static final int CELL_Y = 16;
    public static final int PLAQUE_X = 92;
    public static final int PLAQUE_Y = 16;
    public static final int PLAQUE_W = 76;
    public static final int PLAQUE_H = 18;
    public static final int PLAQUE_STEP = 20;

    public RuneEngraverScreen(RuneEngraverMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, Hextale.id("textures/gui/rune_engraver.png"));
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack tool = menu.tool();
        boolean hasTool = HextechTools.isTool(tool);
        EssenceTank cell = new EssenceTank(HextechTools.CELL);
        if (hasTool) {
            cell.set(HextechTools.cell(tool));
        }
        tank(graphics, leftPos + CELL_X, topPos + CELL_Y, TANK_H, cell, mouseX, mouseY,
                Component.translatable(hasTool ? "gui.hextale.engraver.cell" : "gui.hextale.engraver.cell.no_tool"));
        RuneSlots runes = HextechTools.runes(tool);
        for (int slot = 0; slot < HextechTools.SLOTS; slot++) {
            plaque(graphics, slot, hasTool, runes.get(slot), mouseX, mouseY);
        }
    }

    private void plaque(GuiGraphicsExtractor graphics, int slot, boolean hasTool, RuneSlots.@Nullable Engraving engraving, int mouseX, int mouseY) {
        int x = leftPos + PLAQUE_X;
        int y = topPos + PLAQUE_Y + slot * PLAQUE_STEP;
        boolean hover = isOver(mouseX, mouseY, x, y, PLAQUE_W, PLAQUE_H);
        graphics.fill(x, y, x + PLAQUE_W, y + PLAQUE_H, hover ? 0xFF2A2030 : 0xFF1C1622);
        graphics.fill(x, y, x + PLAQUE_W, y + 1, 0xFF0E0A12);
        graphics.fill(x, y + PLAQUE_H - 1, x + PLAQUE_W, y + PLAQUE_H, 0xFF5A4630);
        List<Component> lines = new ArrayList<>();
        if (engraving != null) {
            ComplexRune rune = engraving.rune();
            PixelArt.glyph(graphics, rune, x + 2, y + 2, 14, PixelArt.argb(rune.color(), 1), 0xFF000000);
            PixelArt.flush(graphics);
            Component name = Component.translatable(rune.translationKey());
            graphics.text(font, font.plainSubstrByWidth(name.getString(), PLAQUE_W - 21), x + 19, y + 1, 0xFF000000 | rune.color(), true);
            int stars = engraving.stars();
            graphics.text(font, "★".repeat(stars), x + 19, y + 9, 0xFFF0C24A, false);
            graphics.text(font, "★".repeat(3 - stars), x + 19 + font.width("★".repeat(stars)), y + 9, 0xFF4A3A28, false);
            if (hover) {
                lines.add(name.copy().withColor(rune.color()));
                lines.add(Component.translatable(ToolRunes.translationKey(rune), ToolRunes.cost(rune),
                        Component.translatable(rune.action().translationKey())).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("gui.hextale.engraver.grind").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            String refusal = refusal(slot);
            boolean ready = refusal == null;
            if (ready) {
                int alpha = Math.round(140 + 100 * Mth.sin(Util.getMillis() / 220.0F));
                int gold = alpha << 24 | 0xF0C24A;
                graphics.fill(x, y, x + PLAQUE_W, y + 1, gold);
                graphics.fill(x, y + PLAQUE_H - 1, x + PLAQUE_W, y + PLAQUE_H, gold);
                graphics.fill(x, y, x + 1, y + PLAQUE_H, gold);
                graphics.fill(x + PLAQUE_W - 1, y, x + PLAQUE_W, y + PLAQUE_H, gold);
            }
            Component label = Component.translatable(ready ? "gui.hextale.engraver.engrave" : hasTool ? "gui.hextale.engraver.empty"
                    : "gui.hextale.engraver.locked");
            graphics.text(font, label, x + (PLAQUE_W - font.width(label)) / 2, y + 5, ready ? 0xFFF0C24A : 0xFF6A5A4A, false);
            if (hover) {
                lines.add(Component.translatable("gui.hextale.engraver.slot", slot + 1));
                lines.add(Component.translatable(ready ? "gui.hextale.engraver.engrave.hint" : "gui.hextale.engraver.refused." + refusal)
                        .withStyle(ready ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            }
        }
        if (!lines.isEmpty()) {
            tooltip(lines);
        }
    }

    @Nullable
    private String refusal(int slot) {
        boolean creative = minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
        return RuneEngraverBlockEntity.refusal(creative, ClientKnowledge.get(), menu.tool(), menu.scroll(), slot);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (minecraft != null && minecraft.gameMode != null) {
            RuneSlots runes = HextechTools.runes(menu.tool());
            for (int slot = 0; slot < HextechTools.SLOTS; slot++) {
                int y = topPos + PLAQUE_Y + slot * PLAQUE_STEP;
                if (!isOver(event.x(), event.y(), leftPos + PLAQUE_X, y, PLAQUE_W, PLAQUE_H)) {
                    continue;
                }
                int id = -1;
                if (runes.get(slot) != null) {
                    if (event.hasShiftDown()) {
                        id = RuneEngraverMenu.GRIND + slot;
                    }
                } else {
                    id = slot;
                }
                if (id >= 0) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected String slotHint(int slot) {
        return switch (slot) {
            case RuneEngraverBlockEntity.TOOL -> "engraver.tool";
            case RuneEngraverBlockEntity.SCROLL -> "engraver.scroll";
            default -> "engraver.bottle";
        };
    }
}
