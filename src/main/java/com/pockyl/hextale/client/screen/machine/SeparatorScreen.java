package com.pockyl.hextale.client.screen.machine;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
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
 * The Separator: the mixture's tank on the left, the reagent with the element it binds in the middle (its remaining
 * charge as a bar, an arrow while drops move), the drawn-out concentrate on the right; each tank pours into the flask
 * slot beside it.
 */
public class SeparatorScreen extends MachineScreen<SeparatorMenu> {
    public static final int MIX_X = 36;
    public static final int EXTRACT_X = 106;
    public static final int TANK_Y = 16;

    public SeparatorScreen(SeparatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, Hextale.id("textures/gui/separator.png"));
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SeparatorBlockEntity separator = blockEntity(SeparatorBlockEntity.class);
        int x = leftPos;
        int y = topPos;
        if (separator != null) {
            tank(graphics, x + MIX_X, y + TANK_Y, separator.mix(), mouseX, mouseY, Component.translatable("gui.hextale.separator.mix"));
            tank(graphics, x + EXTRACT_X, y + TANK_Y, separator.extract(), mouseX, mouseY,
                    Component.translatable("gui.hextale.separator.extract"));
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
        int charge = Math.round(16 * menu.charge() / (float) Reagents.BINDS);
        graphics.fill(rx, ry + 18, rx + 16, ry + 20, 0xFF2A1B0C);
        graphics.fill(rx, ry + 18, rx + charge, ry + 20, element == null ? 0xFF8A7A60 : 0xFF000000 | element.color());
        arrow(graphics, x + 58, y + 36, 16, menu.step() / (float) SeparatorBlockEntity.STEP_TICKS);
        arrow(graphics, x + 82, y + 50, 16, menu.step() / (float) SeparatorBlockEntity.STEP_TICKS);
        PixelArt.flush(graphics);
    }
}
