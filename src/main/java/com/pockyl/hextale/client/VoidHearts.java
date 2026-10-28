package com.pockyl.hextale.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.thevoid.VoidMites;

/**
 * The hearts the Void Mites drink are not simply gone: their places in the health row show dead, infected hearts - dark,
 * cracked with magenta - that fill with nothing until a potion of healing shakes the mites off. Drawn right after the
 * vanilla hearts, in the same layout (the row the health layer just used).
 */
public final class VoidHearts {
    private static final Identifier SPRITE = Hextale.id("hud/void_heart");

    private VoidHearts() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterGuiLayersEvent event) ->
                event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Hextale.id("void_hearts"), VoidHearts::render));
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer()) {
            return;
        }
        int mites = player.getData(ModAttachments.VOID_MITES);
        if (mites <= 0) {
            return;
        }
        // The same numbers the health layer laid its hearts out with; it has already raised leftHeight past its rows.
        float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), Mth.ceil(player.getHealth()));
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        int rows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (rows - 2), 3);
        int yLineBase = graphics.guiHeight() - (minecraft.gui.leftHeight - ((rows - 1) * rowHeight + 10));
        int xLeft = graphics.guiWidth() / 2 - 91;
        int first = Mth.ceil(maxHealth / 2.0) + Mth.ceil(absorption / 2.0);
        for (int i = 0; i < Math.min(mites, VoidMites.MAX); i++) {
            int index = first + i;
            int x = xLeft + index % 10 * 8;
            int y = yLineBase - index / 10 * rowHeight;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, x, y, 9, 9);
        }
    }
}
