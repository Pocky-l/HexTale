package com.pockyl.hextale.client;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.TankFuel;
import com.pockyl.hextale.client.render.PixelArt;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The fuel of the Chem Tank one wears, shown beside the hotbar as a small glass vial: the liquid rises and falls with the
 * tank, in the fuel's colour; an empty tank shows a dark, empty vial. Drawn only while a tank is worn.
 */
public final class TankGauge {
    private static final int WIDTH = 7;
    private static final int HEIGHT = 22;
    private static final int IRON = 0xFF2E2A26;
    private static final int IRON_LIGHT = 0xFF5B544C;
    private static final int GLASS = 0x5086A07A;
    private static final int GLINT = 0x70FFFFFF;

    private TankGauge() {
    }

    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Hextale.id("chem_tank_gauge"), (portGraphics, portDelta) -> TankGauge.render(com.pockyl.hextale.port.client.GuiGraphicsExtractor.of(portGraphics), portDelta));
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }
        ItemStack tank = ChemTank.worn(minecraft.player);
        if (tank.isEmpty()) {
            return;
        }
        TankFuel fuel = ChemTank.contents(tank);
        int x = graphics.guiWidth() / 2 + 91 + 4;
        int y = graphics.guiHeight() - HEIGHT - 1;
        // Iron frame with a lit top-left edge, glass inside.
        PixelArt.fill(graphics, x, y, x + WIDTH, y + HEIGHT, IRON);
        PixelArt.fill(graphics, x, y, x + WIDTH, y + 1, IRON_LIGHT);
        PixelArt.fill(graphics, x, y, x + 1, y + HEIGHT, IRON_LIGHT);
        int inX = x + 2;
        int inY = y + 2;
        int inW = WIDTH - 4;
        int inH = HEIGHT - 4;
        PixelArt.fill(graphics, inX, inY, inX + inW, inY + inH, GLASS);
        if (!fuel.isEmpty()) {
            int level = Math.max(1, Math.round(inH * fuel.level()));
            int colour = 0xFF000000 | fuel.color();
            PixelArt.fill(graphics, inX, inY + inH - level, inX + inW, inY + inH, colour);
            // The surface catches the light.
            PixelArt.fill(graphics, inX, inY + inH - level, inX + inW, inY + inH - level + 1,
                    0xFF000000 | PixelArt.mix(fuel.color(), 0xFFFFFF, 0.4F));
        }
        PixelArt.fill(graphics, inX, inY + 1, inX + 1, inY + inH - 2, GLINT);
        PixelArt.flush(graphics);
    }
}
