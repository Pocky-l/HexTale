package com.pockyl.hextale.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.item.HandLoupeItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.Loupe;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.rune.Essence;

import java.util.List;

/**
 * The Hand Loupe held to the eye: the world shrinks into a round lens in a brass ring, the rest goes dark. As the look
 * deepens three inner rings close in, and the essences of what is looked at rise as beads around the lens - glimmers
 * at first, their signs once the item is known.
 */
public final class HandLoupeView {
    private static final int BRASS = 0xC8A050;
    private static final int BRASS_LIGHT = 0xFFF0B8;
    private static final int BRASS_DARK = 0x5A3E14;

    private HandLoupeView() {
    }

    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CROSSHAIR, Hextale.id("hand_loupe"), HandLoupeView::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !player.isUsingItem() || !player.getUseItem().is(ModItems.HAND_LOUPE.get())) {
            return;
        }
        ItemStack loupe = player.getUseItem();
        float used = player.getTicksUsingItem() + delta.getGameTimeDeltaPartialTick(false);
        float progress = Mth.clamp(used / HandLoupeItem.lookTicks(loupe), 0, 1);
        float open = Mth.clamp(used / 6, 0, 1);
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int cx = w / 2;
        int cy = h / 2;
        float radius = Math.min(w, h) * (0.5F - 0.16F * open);
        int dark = PixelArt.argb(0x05040A, 0.92F * open);
        // Everything outside the lens goes dark, row by row.
        for (int y = 0; y < h; y++) {
            float dy = y + 0.5F - cy;
            if (Math.abs(dy) >= radius) {
                PixelArt.fill(graphics, 0, y, w, y + 1, dark);
                continue;
            }
            int half = Math.round(Mth.sqrt(radius * radius - dy * dy));
            PixelArt.fill(graphics, 0, y, cx - half, y + 1, dark);
            PixelArt.fill(graphics, cx + half, y, w, y + 1, dark);
        }
        int diameter = Math.round(radius * 2);
        // The brass ring: dark outside, lit from the top left, a thin groove inside.
        PixelArt.discRing(graphics, cx, cy, diameter + 6, PixelArt.argb(BRASS_DARK, open));
        PixelArt.discRing(graphics, cx, cy, diameter + 4, PixelArt.argb(BRASS, open));
        PixelArt.discRing(graphics, cx, cy, diameter + 2, PixelArt.argb(BRASS_LIGHT, 0.6F * open));
        PixelArt.discRing(graphics, cx, cy, diameter, PixelArt.argb(BRASS_DARK, 0.7F * open));
        // Three lenses close in as the look deepens.
        for (int lens = 0; lens < Loupe.LENSES; lens++) {
            float in = Mth.clamp(progress * Loupe.LENSES - lens, 0, 1);
            if (in > 0) {
                int d = Math.round(diameter * (1.0F - 0.08F * (lens + 1) * in));
                PixelArt.discRing(graphics, cx, cy, d, PixelArt.argb(0xBFE8FF, 0.18F * in));
            }
        }
        ItemStack target = HandLoupeItem.target(player, player.getUsedItemHand());
        if (Loupe.studiable(target)) {
            beads(graphics, target, cx, cy, radius, progress);
        }
        PixelArt.flush(graphics);
    }

    /** The target's essences as beads on an arc over the lens, rising one after another. */
    private static void beads(GuiGraphicsExtractor graphics, ItemStack target, int cx, int cy, float radius, float progress) {
        ItemEssences essences = ResearchItems.of(target);
        List<Essence> list = essences.essences().keySet().stream().toList();
        boolean known = ClientKnowledge.get().hasStudied(BuiltInRegistries.ITEM.getKey(target.getItem()));
        int count = list.size();
        for (int i = 0; i < count; i++) {
            float show = Mth.clamp(progress * (count + 1) - i, 0, 1);
            if (show <= 0) {
                continue;
            }
            double angle = -Math.PI / 2 + (i - (count - 1) / 2.0) * 0.22;
            int x = cx + (int) Math.round(Math.cos(angle) * (radius + 12));
            int y = cy + (int) Math.round(Math.sin(angle) * (radius + 12)) + Math.round((1 - show) * 6);
            Essence essence = list.get(i);
            PixelArt.disc(graphics, x, y, 14, PixelArt.argb(0x120E1E, 0.9F * show), PixelArt.argb(BRASS, show));
            if (known || progress >= 1) {
                PixelArt.essence(graphics, essence, x - PixelArt.ICON / 2, y - PixelArt.ICON / 2, PixelArt.ICON, PixelArt.argb(essence.color(), show));
            } else {
                PixelArt.disc(graphics, x, y, 4, PixelArt.argb(essence.color(), 0.5F * show), 0);
            }
        }
    }
}
