package com.pockyl.hextale.client.screen.book;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import com.pockyl.hextale.port.client.CharacterEvent;
import com.pockyl.hextale.port.client.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.Util;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The diary's search (GitHub issue #8): a field in the sky's top right corner. Typing finds the steps the reader can
 * see whose name holds the words, rings them on the map and flies to the first; Enter flies to the next, Escape clears.
 * Steps still in the fog are never found.
 */
final class GuideSearch {
    static final int WIDTH = 104;
    static final int HEIGHT = 18;
    private static final int MAX_LENGTH = 32;

    /** The words stay between openings, like the camera. */
    private static String query = "";
    private boolean focused;
    private List<Quest> matches = List.of();
    private int current = -1;
    private int seenVersion = -1;

    /** The box's left and top edges (screen coordinates). */
    int x;
    int y;

    boolean over(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    boolean focused() {
        return focused;
    }

    void focus(boolean on) {
        focused = on;
    }

    /** The steps found, refreshed when the words or the knowledge change. */
    List<Quest> matches(RuneKnowledge knowledge, int knowledgeVersion) {
        if (seenVersion != knowledgeVersion) {
            seenVersion = knowledgeVersion;
            find(knowledge);
        }
        return matches;
    }

    @Nullable
    Quest current() {
        return current >= 0 && current < matches.size() ? matches.get(current) : null;
    }

    private void find(RuneKnowledge knowledge) {
        String words = query.trim().toLowerCase(Locale.ROOT);
        if (words.isEmpty()) {
            matches = List.of();
            current = -1;
            return;
        }
        List<Quest> found = new ArrayList<>();
        for (Quest quest : Guide.client().all()) {
            if (Guide.visible(quest, knowledge)
                    && Component.translatable(quest.translationKey()).getString().toLowerCase(Locale.ROOT).contains(words)) {
                found.add(quest);
            }
        }
        matches = found;
        current = found.isEmpty() ? -1 : 0;
    }

    /** Typed letters go into the field; returns the step to fly to when the words changed, or null. */
    @Nullable
    Quest charTyped(CharacterEvent event, RuneKnowledge knowledge) {
        if (!event.isAllowedChatCharacter() || query.length() >= MAX_LENGTH) {
            return null;
        }
        focused = true;
        query += event.codepointAsString();
        find(knowledge);
        return current();
    }

    /**
     * Keys while the field is focused: Backspace, Enter (the next step found), Escape (clear and leave). Returns
     * whether the key was taken; {@link #flyTo} then holds the step to fly to, if any.
     */
    boolean keyPressed(KeyEvent event, RuneKnowledge knowledge) {
        flyTo = null;
        if (!focused) {
            // A letter typed over the map focuses the field and stays here (the inventory key would close the table);
            // its character arrives right after as charTyped.
            if (printable(event.key()) && !event.hasControlDown() && !event.hasAltDown()) {
                focused = true;
                return true;
            }
            return false;
        }
        if (event.isEscape()) {
            query = "";
            find(knowledge);
            focused = false;
            return true;
        }
        if (event.key() == InputConstants.KEY_BACKSPACE) {
            if (!query.isEmpty()) {
                query = query.substring(0, query.offsetByCodePoints(query.length(), -1));
                find(knowledge);
                flyTo = current();
            }
            return true;
        }
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            if (!matches.isEmpty()) {
                current = (current + 1) % matches.size();
                flyTo = current();
            }
            return true;
        }
        // Every other key (the inventory key too) stays in the field while it is focused.
        return true;
    }

    /**
     * Whether a key writes a character: letters, digits, space and the punctuation keys (physical keys, so the same on
     * a Cyrillic layout); not Enter, Escape, Backspace, Tab, arrows or function keys.
     */
    private static boolean printable(int key) {
        return key >= InputConstants.KEY_A && key <= InputConstants.KEY_0 || key >= InputConstants.KEY_SPACE && key <= InputConstants.KEY_SLASH;
    }

    @Nullable
    Quest flyTo;

    /** The field: dark glass with a brass rim, the words with a caret, the count of steps found at the right. */
    void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        boolean hovered = over(mouseX, mouseY);
        PixelArt.fill(graphics, x, y, x + WIDTH, y + HEIGHT, PixelArt.argb(0x120C08, 0.96F));
        int rim = PixelArt.argb(focused ? StarBook.GOLD : hovered ? 0xC9B48A : 0x5A5080, 1);
        PixelArt.fill(graphics, x, y, x + WIDTH, y + 1, rim);
        PixelArt.fill(graphics, x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, rim);
        PixelArt.fill(graphics, x, y, x + 1, y + HEIGHT, rim);
        PixelArt.fill(graphics, x + WIDTH - 1, y, x + WIDTH, y + HEIGHT, rim);
        // A small lens on the left: a ring and its handle.
        int lx = x + 7;
        int ly = y + 8;
        int lens = PixelArt.argb(focused ? StarBook.GOLD : 0x8A82B0, 1);
        PixelArt.fill(graphics, lx - 2, ly - 3, lx + 2, ly - 2, lens);
        PixelArt.fill(graphics, lx - 2, ly + 2, lx + 2, ly + 3, lens);
        PixelArt.fill(graphics, lx - 3, ly - 2, lx - 2, ly + 2, lens);
        PixelArt.fill(graphics, lx + 2, ly - 2, lx + 3, ly + 2, lens);
        PixelArt.fill(graphics, lx + 2, ly + 2, lx + 4, ly + 4, lens);
        PixelArt.fill(graphics, lx + 3, ly + 3, lx + 5, ly + 5, lens);
        PixelArt.flush(graphics);
        int textX = x + 15;
        String count = query.isBlank() ? "" : matches.isEmpty() ? "0" : (current + 1) + "/" + matches.size();
        int countWidth = count.isEmpty() ? 0 : font.width(count) + 4;
        int room = WIDTH - 15 - 4 - countWidth;
        if (query.isEmpty() && !focused) {
            graphics.text(font, font.plainSubstrByWidth(Component.translatable("book.hextale.search").getString(), room), textX, y + 5,
                    0xFF6E6890, false);
        } else {
            String shown = query;
            while (font.width(shown + "_") > room && !shown.isEmpty()) {
                shown = shown.substring(1);
            }
            boolean caret = focused && (Util.getMillis() / 500) % 2 == 0;
            graphics.text(font, shown + (caret ? "_" : ""), textX, y + 5, 0xFFE8E0C8, false);
        }
        if (!count.isEmpty()) {
            graphics.text(font, count, x + WIDTH - 3 - font.width(count), y + 5, matches.isEmpty() ? 0xFFB06060 : 0xFFE0B85A, false);
        }
    }

    /** A pulsing ring round every step found (map coordinates, drawn inside the map's pose); the current one brighter. */
    void renderRings(GuiGraphicsExtractor graphics, long now) {
        if (matches.isEmpty()) {
            return;
        }
        float pulse = 0.5F + 0.5F * Mth.sin(now / 220.0F);
        Quest lit = current();
        for (Quest quest : matches) {
            int radius = GuidePage.nodeSize(quest) / 2 + 5 + (quest == lit ? Math.round(pulse * 2) : 0);
            int colour = PixelArt.argb(quest == lit ? 0xFFE8A0 : StarBook.GOLD, quest == lit ? 0.95F : 0.55F);
            ring(graphics, Math.round(quest.x()), Math.round(quest.y()), radius, colour);
        }
        PixelArt.flush(graphics);
    }

    private static void ring(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int colour) {
        int steps = Math.max(24, radius * 6);
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        for (int i = 0; i < steps; i++) {
            double a = Math.PI * 2 * i / steps;
            int px = cx + (int) Math.round(Math.cos(a) * radius);
            int py = cy + (int) Math.round(Math.sin(a) * radius);
            if (px != lastX || py != lastY) {
                PixelArt.fill(graphics, px, py, px + 1, py + 1, colour);
                lastX = px;
                lastY = py;
            }
        }
    }

    static void click() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.4F, 0.3F));
    }
}
