package com.pockyl.hextale.client.screen.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;

import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.menu.AlembicMenu;

import java.util.List;

/** The Alembic: item -> boiling arrow -> the flask's essence -> bottles; a flame lights while there is heat below. */
public class AlembicScreen extends MachineScreen<AlembicMenu> {
    public static final int TANK_X = 98;
    public static final int TANK_Y = 24;
    public static final int WATER_X = 20;

    public AlembicScreen(AlembicMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderMachine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        AlembicBlockEntity alembic = blockEntity(AlembicBlockEntity.class);
        int x = leftPos;
        int y = topPos;
        flame(graphics, x + AlembicMenu.INPUT_X + 3, y + AlembicMenu.INPUT_Y + 20, menu.heated());
        if (isOver(mouseX, mouseY, x + AlembicMenu.INPUT_X, y + AlembicMenu.INPUT_Y + 19, 16, 12)) {
            tooltip(List.of(Component.translatable(menu.heated() ? "gui.hextale.alembic.heat" : "gui.hextale.alembic.cold")));
        }
        arrow(graphics, x + 66, y + 47, 22, menu.progress() / (float) AlembicBlockEntity.BOIL_TICKS);
        if (alembic != null) {
            waterTank(graphics, x + WATER_X, y + TANK_Y, TANK_H, alembic.water(), AlembicBlockEntity.WATER_CAPACITY, mouseX, mouseY,
                    Component.translatable("gui.hextale.alembic.water", alembic.water(), AlembicBlockEntity.WATER_CAPACITY),
                    Component.translatable("gui.hextale.alembic.water.hint", AlembicBlockEntity.WATER_PER_ITEM).withStyle(ChatFormatting.GRAY));
            if (alembic.water() < AlembicBlockEntity.WATER_PER_ITEM && !alembic.input().isEmpty()) {
                warn(graphics, x + WATER_X, y + TANK_Y, TANK_W, TANK_H);
            }
            tank(graphics, x + TANK_X, y + TANK_Y, TANK_H, alembic.tank(), mouseX, mouseY,
                    Component.translatable("gui.hextale.alembic.out"));
        }
        arrow(graphics, x + 118, y + 47, 10, 0);
    }

    @Override
    protected boolean isOutput(int slot) {
        return slot == AlembicBlockEntity.FLASK_OUT;
    }

    @Override
    protected String slotHint(int slot) {
        return switch (slot) {
            case AlembicBlockEntity.INPUT -> "alembic.input";
            case AlembicBlockEntity.FLASK_IN -> "bottles";
            default -> "alembic.output";
        };
    }

    /** A small pixel flame: lively when heated, a grey ember otherwise. */
    private static void flame(GuiGraphicsExtractor graphics, int x, int y, boolean lit) {
        float flicker = lit ? 0.5F + 0.5F * Mth.sin(Util.getMillis() / 90.0F) : 0;
        int height = lit ? 8 + Math.round(flicker * 2) : 3;
        for (int row = 0; row < height; row++) {
            int half = Math.max(1, (height - row) * 4 / height);
            int colour = !lit ? 0xFF5A4E44 : row < height / 3 ? 0xFFFF7A1C : row < height * 2 / 3 ? 0xFFFFB02E : 0xFFFFE680;
            graphics.fill(x + 5 - half, y + 10 - row, x + 5 + half, y + 11 - row, colour);
        }
    }
}
