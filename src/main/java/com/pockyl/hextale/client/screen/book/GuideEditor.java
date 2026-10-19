package com.pockyl.hextale.client.screen.book;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.Nullable;

/**
 * The guide map's editor, only in a development client: drag a step with the right mouse button to place it by hand,
 * H shows every step (fog or not). On release the map is laid out again around the moved step (devices still being worked
 * out find new places, links bow anew) and every hand-moved step is written to the layout file
 * ({@link Guide#LAYOUT_FILE}) in the mod's sources, so the next build ships the new map. Crossing links are reported.
 */
final class GuideEditor {
    private static final int HINT = 0xFFB8B0D0;

    private final StarBook book;
    private final GuidePage guide;
    @Nullable
    private Quest dragged;
    /** Where on the step it was grabbed, in map pixels. */
    private float grabX;
    private float grabY;

    GuideEditor(StarBook book, GuidePage guide) {
        this.book = book;
        this.guide = guide;
    }

    static boolean enabled() {
        return !FMLEnvironment.isProduction();
    }

    boolean press(double mouseX, double mouseY) {
        if (!enabled()) {
            return false;
        }
        dragged = guide.questAt(mouseX, mouseY, ClientKnowledge.get());
        if (dragged != null) {
            grabX = book.mapX(mouseX) - dragged.x();
            grabY = book.mapY(mouseY) - dragged.y();
        }
        return dragged != null;
    }

    boolean drag(double mouseX, double mouseY) {
        if (dragged == null) {
            return false;
        }
        Guide.drag(dragged, Math.round(book.mapX(mouseX) - grabX), Math.round(book.mapY(mouseY) - grabY));
        return true;
    }

    boolean release(double mouseX, double mouseY) {
        if (dragged == null) {
            return false;
        }
        Quest quest = dragged;
        dragged = null;
        Guide.move(quest, Math.round(book.mapX(mouseX) - grabX), Math.round(book.mapY(mouseY) - grabY));
        Path file = save();
        int crossings = Guide.crossings();
        String text = quest.id() + " -> " + quest.x() + ", " + quest.y() + (file != null ? "  saved" : "  NOT saved")
                + (crossings > 0 ? "  -  " + crossings + " link crossing(s)!" : "");
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendOverlayMessage(Component.literal(text).withColor(crossings > 0 ? 0xFF7060 : 0x8FE07A));
        }
        return true;
    }

    boolean keyPressed(KeyEvent event) {
        if (!enabled() || event.key() != InputConstants.KEY_H) {
            return false;
        }
        guide.revealAll = !guide.revealAll;
        return true;
    }

    /** A line in the sky's bottom right corner (screen coordinates) naming the editor's controls. */
    void renderHint(GuiGraphicsExtractor graphics, int right, int bottom) {
        if (!enabled()) {
            return;
        }
        var font = Minecraft.getInstance().font;
        Component text = Component.literal("DEV: right-drag moves a step, H " + (guide.revealAll ? "brings the fog back" : "shows every step"));
        int w = font.width(text);
        PixelArt.fill(graphics, right - w - 6, bottom - 13, right, bottom, PixelArt.argb(0x120C08, 0.82F));
        PixelArt.flush(graphics);
        graphics.text(font, text, right - w - 3, bottom - 11, HINT, false);
    }

    /**
     * Writes the hand-moved steps to the layout file in the mod's sources (found by walking up from the game folder: the
     * mod's own run folder or the workspace's playtest folder), else into the game folder. Returns the file, or null.
     */
    @Nullable
    private static Path save() {
        JsonObject json = new JsonObject();
        Guide.moved().forEach((id, at) -> {
            JsonArray pair = new JsonArray();
            pair.add(at[0]);
            pair.add(at[1]);
            json.add(id, pair);
        });
        Path file = sources().resolve(Guide.LAYOUT_FILE);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json) + "\n", StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            Hextale.LOGGER.warn("Could not save the guide layout to {}", file, e);
            return null;
        }
    }

    private static Path sources() {
        for (Path dir = FMLPaths.GAMEDIR.get().toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path own = dir.resolve("src/main/java/com/pockyl/hextale");
            if (Files.isDirectory(own)) {
                return dir.resolve("src/main/resources");
            }
            Path workspace = dir.resolve("mods/hextale/src/main/resources");
            if (Files.isDirectory(workspace)) {
                return workspace;
            }
        }
        return FMLPaths.GAMEDIR.get();
    }
}
