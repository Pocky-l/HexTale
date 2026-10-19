package com.pockyl.hextale.client.screen.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.infusion.InfusionRecipe;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.multiblock.Multiblock;
import com.pockyl.hextale.multiblock.MultiblockPattern;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.QuestDisplay;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechPickaxe;
import com.pockyl.hextale.runework.PickaxeEffects;
import com.pockyl.hextale.runework.PickaxeRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.PylonRune;
import com.pockyl.hextale.runework.RuneHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;

/**
 * The research book's quest page: the researcher's guide as branching trees in the dark sky, each theme growing its own
 * way. Only what is done and what is open now can be seen; every path that goes on beyond them fades into the fog. Item
 * quests sit in round brass medallions (an ink badge on the ones still to be researched), devices still being worked
 * out (WIP) in turning dashed rings. Important steps wear an aura: gold, storm arcs, hextech sparks, cursed smoke,
 * embers or green motes. A click opens the step's plate in the middle: the recipe or scheme for an item and what it
 * opens (on a table, a button that lays its research on the parchment), the idea behind a WIP device.
 */
final class GuidePage {
    private static final int ITEM_NODE = 24;
    /** The landmark's medallion: the altar, the heart of the guide. */
    private static final int LANDMARK_NODE = 44;
    private static final int BIG = 6;
    private static final int PLATE_WIDTH = 252;
    /** The back arrow in the plate's top left corner. */
    private static final int BACK = 14;
    /** The header leaves room for the back arrow and the step's sign before the name. */
    private static final int TITLE_INDENT = BACK + 4 + 20;
    private static final int PAD = 6;
    private static final int SLOT = 18;
    private static final int TEXT = 0xFFD8D0E8;
    private static final int MUTED = 0xFF9A90B8;
    private static final int DONE = 0x8FE07A;
    private static final int OPEN = 0x5CC8F2;
    private static final int WIP = 0xB08AE0;
    private static final int MARGIN = 90;
    private static final int BRASS_LIGHT = 0xFFF0B8;
    private static final int BRASS_DARK = 0x6A4A18;

    private final StarBook book;
    private final Font font;
    @Nullable
    private Quest selected;
    /** The step under the mouse while the map is drawn. */
    @Nullable
    private Quest focus;
    private int scroll;
    /** The rune chosen in a runes picture (pickaxe or pylon); where its grid was drawn last (screen coordinates). */
    @Nullable
    private ComplexRune optionRune;
    private int runeGridX = Integer.MIN_VALUE;
    private int runeGridY;

    GuidePage(StarBook book) {
        this.book = book;
        this.font = Minecraft.getInstance().font;
    }

    static int minX() {
        return Guide.left() - MARGIN;
    }

    static int maxX() {
        return Guide.right() + MARGIN;
    }

    static int minY() {
        return Guide.top() - MARGIN;
    }

    static int maxY() {
        return Guide.bottom() + MARGIN;
    }

    /**
     * Where a reader should look first, in map coordinates: the middle of the steps open now (not done, not WIP) when
     * they fit into a view of this size, else the earliest of them with as many of its open neighbours as fit; with
     * nothing open, the middle of the done steps around the last one.
     */
    static float[] focus(RuneKnowledge knowledge, float viewWidth, float viewHeight) {
        List<Quest> open = new ArrayList<>();
        Quest last = Guide.BRASS;
        for (Quest quest : Guide.all()) {
            boolean done = Guide.done(quest, knowledge);
            if (!done && quest.kind() != Quest.Kind.WIP && Guide.open(quest, knowledge)) {
                open.add(quest);
            }
            if (done) {
                last = quest;
            }
        }
        if (open.isEmpty()) {
            open.add(last);
        }
        // Grow a box from the earliest open step, taking every other open step that still keeps it within the view.
        Quest first = open.getFirst();
        float minX = first.x();
        float maxX = first.x();
        float minY = first.y();
        float maxY = first.y();
        float roomX = viewWidth - MARGIN;
        float roomY = viewHeight - MARGIN;
        for (Quest quest : open) {
            float x0 = Math.min(minX, quest.x());
            float x1 = Math.max(maxX, quest.x());
            float y0 = Math.min(minY, quest.y());
            float y1 = Math.max(maxY, quest.y());
            if (x1 - x0 <= roomX && y1 - y0 <= roomY) {
                minX = x0;
                maxX = x1;
                minY = y0;
                maxY = y1;
            }
        }
        return new float[]{(minX + maxX) / 2, (minY + maxY) / 2};
    }

    /** Whether any step the reader can see lies inside the view around this point. */
    static boolean anyVisibleNear(RuneKnowledge knowledge, float cx, float cy, float viewWidth, float viewHeight) {
        for (Quest quest : Guide.all()) {
            if (Guide.visible(quest, knowledge) && Math.abs(quest.x() - cx) < viewWidth / 2 - 16 && Math.abs(quest.y() - cy) < viewHeight / 2 - 16) {
                return true;
            }
        }
        return false;
    }

    private static int size(Quest quest) {
        return quest.landmark() ? LANDMARK_NODE : ITEM_NODE + (quest.important() ? BIG : 0);
    }

    // ---- The map --------------------------------------------------------------------------------------------------

    /** Draws the path in map coordinates (the caller has set up the camera's pose). */
    void renderMap(GuiGraphicsExtractor graphics, RuneKnowledge knowledge, long now, @Nullable Quest hovered) {
        List<Quest> shown = Guide.all().stream().filter(quest -> Guide.visible(quest, knowledge)).toList();
        focus = hovered;
        renderHaze(graphics, shown);
        for (Quest quest : shown) {
            renderLinks(graphics, quest, knowledge, now);
        }
        for (Quest quest : shown) {
            renderTrails(graphics, quest, knowledge, now);
        }
        for (Quest quest : shown) {
            Auras.draw(graphics, quest, size(quest), now);
        }
        for (Quest quest : shown) {
            renderNode(graphics, quest, knowledge, now, quest == hovered);
        }
        PixelArt.flush(graphics);
        for (Quest quest : shown) {
            if (quest.landmark()) {
                // The landmark's item is drawn twice as large, filling its great medallion.
                graphics.pose().pushMatrix();
                graphics.pose().translate(quest.x(), quest.y());
                graphics.pose().scale(2, 2);
                graphics.item(quest.icon(), -8, -8);
                graphics.pose().popMatrix();
            } else {
                graphics.item(quest.icon(), quest.x() - 8, quest.y() - 8);
            }
        }
        // Small badges only where they can be read: zoomed out they would only speckle the sky.
        boolean badges = book.zoom() >= 1;
        for (Quest quest : shown) {
            int badge = Math.round(size(quest) * 0.36F);
            if (quest.kind() == Quest.Kind.WIP) {
                // Mist over a device still being worked out, and a badge with three dots waiting in turn.
                PixelArt.disc(graphics, quest.x(), quest.y(), size(quest) - 4, PixelArt.argb(0x120E1E, 0.45F), 0);
                inProgress(graphics, quest.x() + badge, quest.y() + badge, now, quest.index());
            } else if (Guide.done(quest, knowledge)) {
                if (badges) {
                    check(graphics, quest.x() + badge - 4, quest.y() + badge - 4);
                }
            } else if (quest.topic() != null && !knowledge.researched(quest.topic())) {
                // Still to be worked out at the table: an ink drop on the medallion's shoulder.
                inkBadge(graphics, quest.x() - badge, quest.y() - badge, now, quest.index());
            }
        }
        PixelArt.flush(graphics);
    }

    /**
     * A faint haze of each region's colour behind its visible steps: wide soft discs that melt into one cloud, so a theme
     * reads as a nebula of its own rather than a box.
     */
    private static void renderHaze(GuiGraphicsExtractor graphics, List<Quest> shown) {
        // Only the branch's own steps breathe the haze (devices to come would blot it), in many faint layers so the
        // edges melt away instead of showing rings.
        for (Quest quest : shown) {
            if (quest.kind() == Quest.Kind.WIP) {
                continue;
            }
            int color = quest.region().color();
            int widest = quest.landmark() ? 220 : 130;
            for (int ring = 0; ring < 5; ring++) {
                PixelArt.disc(graphics, quest.x(), quest.y(), widest - ring * widest / 6, PixelArt.argb(color, 0.009F), 0);
            }
        }
    }

    /**
     * Links to the parents: a brass conduit with rounded bends (along, down, along) for the parent a step hangs from, a thin
     * dotted line for the others. Gold where both ends are done, a pulsing cyan into an open step.
     */
    private void renderLinks(GuiGraphicsExtractor graphics, Quest quest, RuneKnowledge knowledge, long now) {
        boolean done = Guide.done(quest, knowledge);
        for (Quest parent : quest.parents()) {
            if (!Guide.visible(parent, knowledge)) {
                continue;
            }
            boolean lit = done && Guide.done(parent, knowledge);
            float pulse = pulse(now, quest.index());
            // Travelled paths settle into a quiet line of their branch's colour; the open ones keep their cyan pulse.
            int color = lit ? PixelArt.argb(PixelArt.mix(quest.region().color(), StarBook.GOLD, 0.35F), 0.32F)
                    : quest.kind() == Quest.Kind.WIP ? PixelArt.argb(WIP, 0.4F) : PixelArt.argb(OPEN, 0.45F + 0.3F * pulse);
            int[][] path = curve(parent, quest);
            if (parent != quest.parent()) {
                // Second parents: a dotted thread for the step in focus only, so cross links never criss-cross the sky.
                if (quest == selected || quest == focus) {
                    color = PixelArt.argb(OPEN, 0.7F);
                    int phase = (int) (now / 120 % 4);
                    for (int i = 0; i < path.length; i++) {
                        if ((i + phase) % 4 == 0) {
                            PixelArt.fill(graphics, path[i][0], path[i][1], path[i][0] + 1, path[i][1] + 1, color);
                        }
                    }
                }
                continue;
            }
            for (int[] p : path) {
                PixelArt.fill(graphics, p[0], p[1], p[0] + 1, p[1] + 1, color);
            }
            if (!lit) {
                // A bead of light running along an open link, with a soft glow around it.
                float t = (now % 1600) / 1600.0F;
                int[] p = path[Math.min(path.length - 1, (int) (t * path.length))];
                float fade = 1 - t;
                PixelArt.fill(graphics, p[0] - 1, p[1], p[0] + 2, p[1] + 1, PixelArt.argb(OPEN, 0.45F * fade));
                PixelArt.fill(graphics, p[0], p[1] - 1, p[0] + 1, p[1] + 2, PixelArt.argb(OPEN, 0.45F * fade));
                PixelArt.fill(graphics, p[0], p[1], p[0] + 1, p[1] + 1, PixelArt.argb(0xE8FCFF, 0.9F * fade));
            }
        }
    }

    /** Paths into the fog: a few fading dots from a visible step towards each hidden one, no further. */
    private void renderTrails(GuiGraphicsExtractor graphics, Quest quest, RuneKnowledge knowledge, long now) {
        for (Quest child : quest.children()) {
            if (Guide.visible(child, knowledge) || child.parent() != quest) {
                continue;
            }
            float dx = child.x() - quest.x();
            float dy = child.y() - quest.y();
            float length = Math.max(1, Mth.sqrt(dx * dx + dy * dy));
            dx /= length;
            dy /= length;
            float start = size(quest) / 2.0F + 3;
            for (int i = 0; i < 6; i++) {
                float fade = 1 - i / 6.0F;
                float drift = 0.5F + 0.5F * Mth.sin(now / 600.0F + i + child.index());
                int px = Math.round(quest.x() + dx * (start + i * 4));
                int py = Math.round(quest.y() + dy * (start + i * 4));
                PixelArt.fill(graphics, px, py, px + 1, py + 1, PixelArt.argb(0x8C84B0, 0.6F * fade * (0.5F + 0.5F * drift)));
            }
        }
    }

    /**
     * The pixels of a link from a parent's rim to a child's rim, in order: a gentle arc (a quadratic curve bowed to one
     * side, the side set by the pair so neighbouring links bow differently). Every pixel appears once, so translucent
     * links stay even.
     */
    private static int[][] curve(Quest from, Quest to) {
        float ax = from.x();
        float ay = from.y();
        float bx = to.x();
        float by = to.y();
        float dx = bx - ax;
        float dy = by - ay;
        float length = Math.max(1, Mth.sqrt(dx * dx + dy * dy));
        float bow = ((from.index() + to.index()) % 2 == 0 ? 1 : -1) * Math.min(26, length * 0.16F);
        float cx = (ax + bx) / 2 - dy / length * bow;
        float cy = (ay + by) / 2 + dx / length * bow;
        float t0 = Math.min(0.45F, (size(from) / 2.0F + 2) / length);
        float t1 = Math.max(0.55F, 1 - (size(to) / 2.0F + 2) / length);
        int steps = Math.max(2, Math.round(length * 1.6F));
        List<int[]> points = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int i = 0; i <= steps; i++) {
            float t = t0 + (t1 - t0) * i / steps;
            float u = 1 - t;
            point(points, seen, Math.round(u * u * ax + 2 * u * t * cx + t * t * bx), Math.round(u * u * ay + 2 * u * t * cy + t * t * by));
        }
        return points.toArray(int[][]::new);
    }

    private static void point(List<int[]> points, java.util.Set<Long> seen, int x, int y) {
        if (seen.add((long) x << 32 | (y & 0xFFFFFFFFL))) {
            points.add(new int[]{x, y});
        }
    }

    private void renderNode(GuiGraphicsExtractor graphics, Quest quest, RuneKnowledge knowledge, long now, boolean hovered) {
        int x = quest.x();
        int y = quest.y();
        int size = size(quest);
        boolean done = Guide.done(quest, knowledge);
        boolean wip = quest.kind() == Quest.Kind.WIP;
        float pulse = pulse(now, quest.index());
        if (!done && !wip) {
            // An open step breathes in cyan.
            for (int ring = 0; ring < 3; ring++) {
                int halo = PixelArt.argb(OPEN, (0.14F - ring * 0.04F) * (0.6F + 0.4F * pulse));
                PixelArt.disc(graphics, x, y, size + 4 + ring * 4, halo, 0);
            }
        }
        // Done steps keep a quieter brass, tinted by their branch; key steps stay bright gold.
        int rim = done ? (quest.important() || quest.landmark() ? StarBook.GOLD : PixelArt.mix(0xB08A40, quest.region().color(), 0.25F))
                : wip ? WIP : PixelArt.mix(0x2B8FB8, 0x7EF0FF, pulse);
        if (quest == selected || hovered) {
            rim = PixelArt.mix(StarBook.GOLD, 0xFFF6D6, pulse);
        }
        if (quest.landmark()) {
            landmarkFrame(graphics, x, y, size, done, now);
        }
        switch (quest.kind()) {
            case ITEM -> {
                int fill = done ? 0x2A2210 : 0x0C1A2A;
                medallion(graphics, x, y, size, rim, fill, done ? PixelArt.mix(fill, StarBook.GOLD, 0.18F) : PixelArt.mix(fill, OPEN, 0.12F));
            }
            case WIP -> {
                // A dashed ring turning slowly around a dim disc: a device still being drawn up.
                PixelArt.disc(graphics, x, y, size, PixelArt.argb(0x120E1E, 0.9F), 0);
                PixelArt.Circle circle = PixelArt.Circle.of(size);
                float turn = now / 2600.0F + quest.index();
                for (int i = 0; i < circle.pixels.length; i++) {
                    float a = circle.angles[i] - turn;
                    float dash = Mth.sin(a * 8);
                    if (dash > -0.15F) {
                        int[] p = circle.pixels[i];
                        PixelArt.fill(graphics, x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, PixelArt.argb(rim, 0.55F + 0.45F * dash));
                    }
                }
            }
        }
        if (quest == selected) {
            PixelArt.discRing(graphics, x, y, size + 6, PixelArt.argb(StarBook.GOLD, 0.5F + 0.5F * pulse));
        }
    }

    /**
     * A round brass medallion: a dark outline, a bevelled rim lit from the top left, a thin inner groove and a face
     * that glows a little towards its centre.
     */
    private static void medallion(GuiGraphicsExtractor graphics, int cx, int cy, int size, int rim, int fill, int glow) {
        PixelArt.disc(graphics, cx, cy, size + 2, 0, PixelArt.argb(0x05040A, 0.85F));
        PixelArt.Circle circle = PixelArt.Circle.of(size);
        for (int i = 0; i < circle.pixels.length; i++) {
            // Light from the top left (-3/4 pi on screen), shade towards the bottom right.
            float light = Mth.cos(circle.angles[i] + 3 * Mth.PI / 4);
            int color = light > 0 ? PixelArt.mix(rim, BRASS_LIGHT, 0.5F * light) : PixelArt.mix(rim, BRASS_DARK, -0.55F * light);
            int[] p = circle.pixels[i];
            PixelArt.fill(graphics, cx + p[0], cy + p[1], cx + p[0] + 1, cy + p[1] + 1, PixelArt.argb(color, 1));
        }
        PixelArt.disc(graphics, cx, cy, size - 2, PixelArt.argb(fill, 1), PixelArt.argb(PixelArt.mix(rim, BRASS_DARK, 0.7F), 1));
        PixelArt.disc(graphics, cx, cy, size - 8, PixelArt.argb(glow, 0.8F), 0);
        PixelArt.discRing(graphics, cx, cy, size - 4, PixelArt.argb(rim, 0.22F));
    }

    /** The plate's back arrow: a round brass button with a left-pointing arrow, brighter under the mouse. */
    private static void backArrow(GuiGraphicsExtractor graphics, int x, int y, boolean hovered, long now) {
        int cx = x + BACK / 2;
        int cy = y + BACK / 2;
        float pulse = 0.5F + 0.5F * Mth.sin(now / 300.0F);
        int rim = hovered ? PixelArt.mix(StarBook.GOLD, 0xFFF6D6, pulse) : StarBook.GOLD;
        PixelArt.disc(graphics, cx, cy, BACK, PixelArt.argb(hovered ? 0x2A2210 : 0x14101E, 1), PixelArt.argb(rim, 1));
        int color = PixelArt.argb(hovered ? 0xFFF6D6 : StarBook.GOLD, 1);
        // The shaft and the head of the arrow.
        PixelArt.fill(graphics, cx - 3, cy - 1, cx + 4, cy + 1, color);
        for (int i = 0; i < 4; i++) {
            PixelArt.fill(graphics, cx - 4 + i, cy - 1 - i, cx - 3 + i, cy + 1 + i, color);
        }
        PixelArt.flush(graphics);
    }

    /** A violet badge with three dots lighting up in turn: being worked out. */
    private static void inProgress(GuiGraphicsExtractor graphics, int cx, int cy, long now, int phase) {
        PixelArt.disc(graphics, cx, cy, 10, PixelArt.argb(0x2A1840, 1), PixelArt.argb(WIP, 1));
        int lit = (int) ((now / 300 + phase) % 3);
        for (int i = 0; i < 3; i++) {
            // Dots at -3, -1..0 and 2: symmetric around the badge's centre corner.
            int x0 = i == 0 ? cx - 3 : i == 1 ? cx - 1 : cx + 2;
            int x1 = i == 1 ? cx + 1 : x0 + 1;
            PixelArt.fill(graphics, x0, cy - 1, x1, cy + 1, PixelArt.argb(i == lit ? 0xFFF0FF : 0xB89ADF, 1));
        }
    }

    /** A small violet "WIP" ribbon with rounded ends. */
    private void wipTag(GuiGraphicsExtractor graphics, int cx, int top) {
        String label = Component.translatable("book.hextale.guide.wip").getString();
        int w = font.width(label) + 6;
        int x0 = cx - w / 2;
        int x1 = cx + w / 2;
        int body = PixelArt.argb(0x2A1840, 0.95F);
        int edge = PixelArt.argb(WIP, 1);
        PixelArt.fill(graphics, x0 + 2, top, x1 - 2, top + 1, edge);
        PixelArt.fill(graphics, x0 + 2, top + 9, x1 - 2, top + 10, edge);
        PixelArt.fill(graphics, x0 + 1, top + 1, x0 + 2, top + 2, edge);
        PixelArt.fill(graphics, x1 - 2, top + 1, x1 - 1, top + 2, edge);
        PixelArt.fill(graphics, x0 + 1, top + 8, x0 + 2, top + 9, edge);
        PixelArt.fill(graphics, x1 - 2, top + 8, x1 - 1, top + 9, edge);
        PixelArt.fill(graphics, x0, top + 2, x0 + 1, top + 8, edge);
        PixelArt.fill(graphics, x1 - 1, top + 2, x1, top + 8, edge);
        PixelArt.fill(graphics, x0 + 2, top + 1, x1 - 2, top + 9, body);
        PixelArt.fill(graphics, x0 + 1, top + 2, x0 + 2, top + 8, body);
        PixelArt.fill(graphics, x1 - 2, top + 2, x1 - 1, top + 8, body);
        PixelArt.flush(graphics);
        graphics.text(font, label, x0 + 3, top + 1, 0xFFD8C0FF, false);
    }

    /**
     * The landmark's setting: a wide hexagonal plate of dark glass with a brass rim behind the medallion, a ring of sixteen
     * glowing ticks turning slowly around it and a second, finer ring turning the other way - the altar's gimbal rings
     * drawn in the sky.
     */
    private static void landmarkFrame(GuiGraphicsExtractor graphics, int x, int y, int size, boolean done, long now) {
        int glow = done ? StarBook.GOLD : OPEN;
        for (int ring = 0; ring < 4; ring++) {
            PixelArt.disc(graphics, x, y, size + 26 + ring * 10, PixelArt.argb(0x5CC8F2, 0.05F - ring * 0.01F), 0);
        }
        PixelArt.hex(graphics, x, y, size + 18, PixelArt.argb(0x0A0818, 0.92F), PixelArt.argb(PixelArt.mix(StarBook.GOLD, 0x6A4A18, 0.3F), 1));
        PixelArt.hexRing(graphics, x, y, size + 13, PixelArt.argb(glow, 0.35F));
        float turn = now / 4200.0F;
        float outer = size / 2.0F + 15;
        for (int i = 0; i < 16; i++) {
            double angle = turn + i * Math.PI / 8;
            int px = x + (int) Math.round(Math.cos(angle) * outer);
            int py = y + (int) Math.round(Math.sin(angle) * outer);
            boolean big = i % 4 == 0;
            int color = PixelArt.argb(big ? 0xE8FCFF : glow, big ? 0.95F : 0.6F);
            PixelArt.fill(graphics, px - (big ? 1 : 0), py - (big ? 1 : 0), px + (big ? 2 : 1), py + (big ? 2 : 1), color);
        }
        float inner = size / 2.0F + 9;
        for (int i = 0; i < 24; i++) {
            double angle = -turn * 1.6 + i * Math.PI / 12;
            if (i % 3 == 0) {
                continue;
            }
            int px = x + (int) Math.round(Math.cos(angle) * inner);
            int py = y + (int) Math.round(Math.sin(angle) * inner);
            PixelArt.fill(graphics, px, py, px + 1, py + 1, PixelArt.argb(0x9FE6FF, 0.5F));
        }
    }

    /** A drop of ink in a dark disc, glinting now and then: this step is to be researched at the table first. */
    private static void inkBadge(GuiGraphicsExtractor graphics, int cx, int cy, long now, int phase) {
        float glint = Math.max(0, Mth.sin(now / 500.0F + phase));
        PixelArt.disc(graphics, cx, cy, 10, PixelArt.argb(0x0C1426, 1), PixelArt.argb(PixelArt.mix(0x2B8FB8, 0x9FE6FF, glint), 1));
        int ink = PixelArt.argb(0x7EF0FF, 1);
        PixelArt.fill(graphics, cx - 1, cy - 3, cx, cy - 2, ink);
        PixelArt.fill(graphics, cx - 2, cy - 2, cx + 1, cy, ink);
        PixelArt.fill(graphics, cx - 3, cy, cx + 2, cy + 2, ink);
        PixelArt.fill(graphics, cx - 2, cy + 2, cx + 1, cy + 3, ink);
        PixelArt.fill(graphics, cx - 2, cy, cx - 1, cy + 1, PixelArt.argb(0xFFFFFF, 0.5F + 0.5F * glint));
    }

    /** A small tick in a green disc: the quest is done. */
    private static void check(GuiGraphicsExtractor graphics, int x, int y) {
        PixelArt.disc(graphics, x + 4, y + 4, 10, 0xFF1C3A16, PixelArt.argb(PixelArt.mix(DONE, 0x1C3A16, 0.45F), 1));
        int tick = PixelArt.argb(DONE, 1);
        PixelArt.fill(graphics, x + 1, y + 3, x + 2, y + 5, tick);
        PixelArt.fill(graphics, x + 2, y + 4, x + 3, y + 6, tick);
        PixelArt.fill(graphics, x + 3, y + 3, x + 4, y + 5, tick);
        PixelArt.fill(graphics, x + 4, y + 2, x + 5, y + 4, tick);
        PixelArt.fill(graphics, x + 5, y + 1, x + 6, y + 3, tick);
    }

    private static float pulse(long now, int phase) {
        return 0.5F + 0.5F * Mth.sin(now / 420.0F + phase);
    }

    @Nullable
    Quest questAt(double mouseX, double mouseY, RuneKnowledge knowledge) {
        float mx = book.mapX(mouseX);
        float my = book.mapY(mouseY);
        Quest best = null;
        float bestDistance = Float.MAX_VALUE;
        for (Quest quest : Guide.all()) {
            if (!Guide.visible(quest, knowledge)) {
                continue;
            }
            float reach = Math.max(size(quest) / 2.0F + 1, 8 / book.zoom());
            float dx = quest.x() + 0.5F - mx;
            float dy = quest.y() + 0.5F - my;
            float d = dx * dx + dy * dy;
            if (d < reach * reach && d < bestDistance) {
                bestDistance = d;
                best = quest;
            }
        }
        return best;
    }

    List<FormattedCharSequence> tooltip(Quest quest, RuneKnowledge knowledge) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(title(quest).getVisualOrderText());
        lines.add(status(quest, knowledge).getVisualOrderText());
        return lines;
    }

    private static Component title(Quest quest) {
        return Component.translatable(quest.translationKey()).withColor(StarBook.GOLD);
    }

    private static Component status(Quest quest, RuneKnowledge knowledge) {
        boolean done = Guide.done(quest, knowledge);
        return switch (quest.kind()) {
            case ITEM -> done ? Component.translatable("book.hextale.quest.done").withColor(DONE)
                    : quest.topic() != null && !knowledge.researched(quest.topic())
                    ? Component.translatable("book.hextale.quest.research_first", Component.translatable(quest.topic().translationKey()))
                    .withColor(0x9FE6FF)
                    : Component.translatable("book.hextale.quest.goal", Component.translatable(quest.translationKey() + ".goal")).withColor(OPEN);
            case WIP -> Component.translatable("book.hextale.guide.wip_status").withColor(WIP);
        };
    }

    @Nullable
    Quest selected() {
        return selected;
    }

    /** @return whether the selection changed */
    boolean select(@Nullable Quest quest) {
        if (quest == selected) {
            return false;
        }
        selected = quest;
        scroll = 0;
        optionRune = null;
        runeGridX = Integer.MIN_VALUE;
        return true;
    }

    // ---- The plate ------------------------------------------------------------------------------------------------

    // The plate is a window in the middle of the sky over the dimmed map, with a back arrow in its corner.

    private int plateWidth() {
        return Math.min(PLATE_WIDTH, book.skyWidth() - 24);
    }

    private int plateLeft() {
        return book.skyLeft() + (book.skyWidth() - plateWidth()) / 2;
    }

    private int plateTop(Quest quest, RuneKnowledge knowledge) {
        return book.skyTop() + (book.skyHeight() - plateHeight(quest, knowledge)) / 2;
    }

    private int innerWidth() {
        return plateWidth() - PAD * 2;
    }

    private List<FormattedCharSequence> titleLines(Quest quest) {
        return font.split(title(quest), innerWidth() - TITLE_INDENT - (quest.kind() == Quest.Kind.WIP ? 26 : 0));
    }

    private List<FormattedCharSequence> statusLines(Quest quest, RuneKnowledge knowledge) {
        return font.split(status(quest, knowledge), innerWidth());
    }

    /** The plate's text under the picture: what an item quest is about, the idea behind a device. */
    private List<FormattedCharSequence> bodyLines(Quest quest) {
        return new ArrayList<>(font.split(Component.translatable(quest.translationKey() + ".desc"), innerWidth()));
    }

    /** "Opens:" and the names of the visible steps after this one; the hidden ones are question marks. */
    private List<FormattedCharSequence> opens(Quest quest, RuneKnowledge knowledge) {
        if (quest.children().isEmpty()) {
            return List.of();
        }
        MutableComponent list = Component.empty();
        int hidden = 0;
        boolean first = true;
        for (Quest child : quest.children()) {
            if (!Guide.visible(child, knowledge)) {
                hidden++;
                continue;
            }
            list.append(Component.literal(first ? "" : ", ")).append(title(child));
            first = false;
        }
        if (hidden > 0) {
            list.append(Component.literal((first ? "" : ", ") + "?".repeat(Math.min(3, hidden))).withColor(0x6A6480));
        }
        return font.split(Component.translatable("book.hextale.guide.opens").withColor(StarBook.GOLD).append(" ").append(list), innerWidth());
    }

    /** "Needs:" and the steps a WIP device waits for. */
    private List<FormattedCharSequence> needs(Quest quest) {
        MutableComponent list = Component.empty();
        for (int i = 0; i < quest.parents().size(); i++) {
            list.append(Component.literal(i == 0 ? "" : ", ")).append(title(quest.parents().get(i)));
        }
        return font.split(Component.translatable("book.hextale.guide.needs").withColor(StarBook.GOLD).append(" ").append(list), innerWidth());
    }

    private int headerHeight(Quest quest) {
        return Math.max(18, titleLines(quest).size() * 10 + 4);
    }

    /** On a table, a step that asks for research shows a button laying its topic on the parchment. */
    private boolean studyButton(Quest quest) {
        return quest.topic() != null && book.host() != null;
    }

    private int contentHeight(Quest quest, RuneKnowledge knowledge) {
        int h = PAD + headerHeight(quest) + statusLines(quest, knowledge).size() * 10 + 6;
        if (quest.scene() != null) {
            h += SCENE_HEIGHT + 8;
        }
        for (QuestDisplay display = quest.display(); display != null; display = display.then()) {
            h += displayHeight(display) + 8;
        }
        h += bodyLines(quest).size() * 10;
        if (quest.kind() == Quest.Kind.WIP) {
            h += 4 + needs(quest).size() * 10;
        } else {
            List<FormattedCharSequence> opens = opens(quest, knowledge);
            h += opens.isEmpty() ? 0 : 4 + opens.size() * 10;
        }
        if (studyButton(quest)) {
            h += 6 + StarBook.BUTTON_HEIGHT;
        }
        return h + PAD;
    }

    private int maxPlateHeight() {
        return book.skyHeight() - 24;
    }

    private int plateHeight(Quest quest, RuneKnowledge knowledge) {
        return Math.min(contentHeight(quest, knowledge), maxPlateHeight());
    }

    boolean overPlate(double x, double y, RuneKnowledge knowledge) {
        return selected != null && x >= plateLeft() && x < plateLeft() + plateWidth() && y >= plateTop(selected, knowledge)
                && y < plateTop(selected, knowledge) + plateHeight(selected, knowledge);
    }

    private boolean overBack(double x, double y, RuneKnowledge knowledge) {
        if (selected == null) {
            return false;
        }
        int bx = plateLeft() + 4;
        int by = plateTop(selected, knowledge) + 4;
        return x >= bx && x < bx + BACK && y >= by && y < by + BACK;
    }

    private int buttonTop(Quest quest, RuneKnowledge knowledge) {
        return plateTop(quest, knowledge) + contentHeight(quest, knowledge) - PAD - StarBook.BUTTON_HEIGHT - scroll;
    }

    /** Draws the selected step's plate; returns the tooltip of what is under the mouse, or null. */
    @Nullable
    List<FormattedCharSequence> renderPlate(GuiGraphicsExtractor graphics, RuneKnowledge knowledge, int mouseX, int mouseY, long now) {
        Quest quest = selected;
        if (quest == null) {
            return null;
        }
        int x = plateLeft();
        int top = plateTop(quest, knowledge);
        int w = plateWidth();
        int h = plateHeight(quest, knowledge);
        int content = contentHeight(quest, knowledge);
        scroll = Mth.clamp(scroll, 0, Math.max(0, content - h));
        // The map sinks into the dark behind the window.
        PixelArt.fill(graphics, book.skyLeft(), book.skyTop(), book.skyLeft() + book.skyWidth(), book.skyTop() + book.skyHeight(), 0xB0040310);
        StarBook.plateBackground(graphics, x, top, w, h);
        PixelArt.flush(graphics);
        graphics.enableScissor(x + 1, top + 1, x + w - 1, top + h - 1);
        boolean mouseIn = mouseX >= x && mouseX < x + w && mouseY >= top && mouseY < top + h;
        List<FormattedCharSequence> tooltip = null;

        int ix = x + PAD;
        int y = top + PAD - scroll;
        // Header: the step's sign and name.
        int sx = ix + BACK + 4;
        graphics.item(quest.icon(), sx, y);
        int ty = y + Math.max(0, (headerHeight(quest) - titleLines(quest).size() * 10) / 2 - 1);
        for (FormattedCharSequence line : titleLines(quest)) {
            graphics.text(font, line, ix + TITLE_INDENT, ty, 0xFFFFFFFF, true);
            ty += 10;
        }
        if (quest.kind() == Quest.Kind.WIP) {
            wipTag(graphics, x + w - PAD - 12, y);
        }
        y += headerHeight(quest);
        for (FormattedCharSequence line : statusLines(quest, knowledge)) {
            graphics.text(font, line, ix, y, 0xFFFFFFFF, true);
            y += 10;
        }
        y += 2;
        PixelArt.fill(graphics, ix, y, ix + innerWidth(), y + 1, PixelArt.argb(StarBook.GOLD, 0.3F));
        y += 4;

        if (quest.scene() != null) {
            // The circle at work, in 3D: what this step is about stands in a ring of light.
            int sw = innerWidth();
            int frame = PixelArt.argb(StarBook.GOLD, 0.35F);
            PixelArt.fill(graphics, ix, y, ix + sw, y + SCENE_HEIGHT, PixelArt.argb(0x0B0918, 1));
            PixelArt.fill(graphics, ix, y, ix + sw, y + 1, frame);
            PixelArt.fill(graphics, ix, y + SCENE_HEIGHT - 1, ix + sw, y + SCENE_HEIGHT, frame);
            PixelArt.fill(graphics, ix, y + 1, ix + 1, y + SCENE_HEIGHT - 1, frame);
            PixelArt.fill(graphics, ix + sw - 1, y + 1, ix + sw, y + SCENE_HEIGHT - 1, frame);
            PixelArt.flush(graphics);
            CircleDiorama.draw(graphics, quest.scene(), now / 50F, ix + 1, y + 1, ix + sw - 1, y + SCENE_HEIGHT - 1, SCENE_SCALE);
            y += SCENE_HEIGHT + 8;
        }
        for (QuestDisplay display = quest.display(); display != null; display = display.then()) {
            y += 2;
            List<FormattedCharSequence> hover = renderDisplay(graphics, quest, display, ix, y, mouseIn ? mouseX : -1, mouseY, now);
            if (hover != null) {
                tooltip = hover;
            }
            y += displayHeight(display) + 6;
        }
        PixelArt.flush(graphics);
        for (FormattedCharSequence line : bodyLines(quest)) {
            graphics.text(font, line, ix, y, TEXT, false);
            y += 10;
        }
        List<FormattedCharSequence> extra = switch (quest.kind()) {
            case WIP -> needs(quest);
            case ITEM -> opens(quest, knowledge);
        };
        if (!extra.isEmpty()) {
            y += 4;
            for (FormattedCharSequence line : extra) {
                graphics.text(font, line, ix, y, TEXT, false);
                y += 10;
            }
        }
        if (studyButton(quest)) {
            StarBook.Host host = book.host();
            ResearchTopic topic = quest.topic();
            boolean open = Guide.canResearch(topic, knowledge);
            boolean onTable = host.researching() == topic;
            boolean active = open && !onTable;
            Component label = Component.translatable(!open ? "book.hextale.closed" : onTable ? "book.hextale.on_table"
                    : knowledge.researched(topic) ? "book.hextale.research_again" : "book.hextale.research");
            int by = buttonTop(quest, knowledge);
            boolean hovered = active && mouseIn && mouseX >= ix && mouseX < ix + innerWidth() && mouseY >= by && mouseY < by + StarBook.BUTTON_HEIGHT;
            book.magicButton(graphics, ix, by, innerWidth(), label, active, hovered, now);
        }
        PixelArt.flush(graphics);
        graphics.disableScissor();
        boolean back = overBack(mouseX, mouseY, knowledge);
        backArrow(graphics, x + 4, top + 4, back, now);
        if (back) {
            tooltip = List.of(Component.translatable("book.hextale.guide.back").getVisualOrderText());
        }
        if (content > h) {
            // A thin scroll bar on the right rim.
            int bar = Math.max(12, h * h / content);
            int by = top + (h - bar) * scroll / Math.max(1, content - h);
            PixelArt.fill(graphics, x + w - 3, by, x + w - 1, by + bar, PixelArt.argb(StarBook.GOLD, 0.6F));
        }
        return tooltip;
    }

    // ---- Pictures on item plates -------------------------------------------------------------------------------------

    private static final int INFUSION_RING = 26;
    private static final int ESSENCES_PER_ROW = 3;
    private static final int ESSENCE_ROW = 11;
    private static final int RECIPE_RUNE_ROW = 13;
    private static final int RUNES_PER_ROW = 7;
    private static final int RUNE_CELL = 24;
    private static final int RUNE_ROW = 22;
    private static final int DIORAMA_HEIGHT = 84;
    /** The ritual circle's 3D scene on the altar's, pedestal's and stele's plates. */
    private static final int SCENE_HEIGHT = 128;
    private static final float SCENE_SCALE = 19;

    private int displayHeight(QuestDisplay display) {
        return switch (display.kind()) {
            case SHAPED -> SLOT * 3;
            case SHAPELESS -> SLOT * 3 + 10;
            case SCHEME -> SLOT + (Language.getInstance().has(captionKey(display)) ? 10 : 0);
            case ESSENCES -> SLOT;
            case REAGENTS -> 30;
            case INFUSION -> infusionHeight(display);
            case PICKAXE_RUNES -> optionsHeight(RuneHolder.PICKAXE);
            case PYLON_RUNES -> optionsHeight(RuneHolder.PYLON);
            case MULTIBLOCK -> multiblockHeight(display);
            case HEX_CRAFTING -> SLOT * 3 + 10;
            case ALLOY -> SLOT + 10;
        };
    }

    private static String captionKey(QuestDisplay display) {
        return "book.hextale.quest.scheme." + display.result().substring(display.result().indexOf(':') + 1);
    }

    @Nullable
    private List<FormattedCharSequence> renderDisplay(GuiGraphicsExtractor graphics, Quest quest, QuestDisplay display, int x, int y,
                                                      int mouseX, int mouseY, long now) {
        List<FormattedCharSequence> tooltip = null;
        int centre = x + innerWidth() / 2;
        switch (display.kind()) {
            case SHAPED, SHAPELESS -> {
                int gridWidth = SLOT * 3;
                int left = centre - (gridWidth + 26 + SLOT) / 2;
                for (int i = 0; i < 9; i++) {
                    String id = i < display.ingredients().size() ? display.ingredients().get(i) : "";
                    tooltip = orTooltip(tooltip, slot(graphics, stack(id, now, i), left + i % 3 * SLOT, y + i / 3 * SLOT, mouseX, mouseY, null));
                }
                int ax = left + gridWidth + 4;
                arrow(graphics, ax, y + SLOT + SLOT / 2);
                tooltip = orTooltip(tooltip, slot(graphics, result(quest, display), ax + 22, y + SLOT, mouseX, mouseY, null));
                if (display.kind() == QuestDisplay.Kind.SHAPELESS) {
                    PixelArt.flush(graphics);
                    graphics.centeredText(font, Component.translatable("book.hextale.quest.shapeless"), centre, y + SLOT * 3 + 2, MUTED);
                }
            }
            case SCHEME -> {
                int count = display.ingredients().size();
                int width = count * SLOT + (count - 1) * 10 + 26 + SLOT;
                int sx = centre - width / 2;
                for (int i = 0; i < count; i++) {
                    tooltip = orTooltip(tooltip, slot(graphics, stack(display.ingredients().get(i), now, i), sx, y, mouseX, mouseY, null));
                    sx += SLOT;
                    if (i < count - 1) {
                        plus(graphics, sx + 5, y + SLOT / 2);
                        sx += 10;
                    }
                }
                arrow(graphics, sx + 4, y + SLOT / 2);
                tooltip = orTooltip(tooltip, slot(graphics, result(quest, display), sx + 26, y, mouseX, mouseY, null));
                String caption = captionKey(display);
                if (Language.getInstance().has(caption)) {
                    PixelArt.flush(graphics);
                    graphics.centeredText(font, Component.translatable(caption), centre, y + SLOT + 2, MUTED);
                }
            }
            case ESSENCES -> {
                int sx = centre - Essence.PRIMALS.size() * SLOT / 2;
                for (Essence essence : Essence.PRIMALS) {
                    PixelArt.hex(graphics, sx + SLOT / 2, y + SLOT / 2, SLOT - 1, PixelArt.argb(PixelArt.mix(essence.color(), 0x06050D, 0.8F), 1),
                            PixelArt.argb(essence.color(), 0.7F));
                    PixelArt.essence(graphics, essence, sx + 4, y + 4, 10, PixelArt.argb(essence.color(), 1));
                    if (over(mouseX, mouseY, sx, y, SLOT, SLOT)) {
                        tooltip = List.of(Component.translatable(essence.translationKey()).withColor(essence.color()).getVisualOrderText());
                    }
                    sx += SLOT;
                }
            }
            case REAGENTS -> {
                int sx = centre - Essence.PRIMALS.size() * SLOT / 2;
                for (Essence essence : Essence.PRIMALS) {
                    PixelArt.essence(graphics, essence, sx + 4, y, 10, PixelArt.argb(essence.color(), 1));
                    Component extra = Component.translatable("book.hextale.quest.reagent_for", Component.translatable(essence.translationKey()))
                            .withColor(essence.color());
                    tooltip = orTooltip(tooltip, slot(graphics, new ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER), sx, y + 12,
                            mouseX, mouseY, extra));
                    sx += SLOT;
                }
            }
            case INFUSION -> tooltip = renderInfusion(graphics, display, x, y, mouseX, mouseY, now);
            case PICKAXE_RUNES -> tooltip = renderOptions(graphics, RuneHolder.PICKAXE, x, y, mouseX, mouseY, now);
            case PYLON_RUNES -> tooltip = renderOptions(graphics, RuneHolder.PYLON, x, y, mouseX, mouseY, now);
            case MULTIBLOCK -> tooltip = renderMultiblock(graphics, display, x, y, mouseX, mouseY, now);
            case HEX_CRAFTING -> tooltip = renderHexCrafting(graphics, display, x, y, mouseX, mouseY, now);
            case ALLOY -> tooltip = renderAlloy(graphics, display, x, y, mouseX, mouseY, now);
        }
        return tooltip;
    }

    // ---- The Hex Workbench and the Hex Furnace --------------------------------------------------------------------------

    /** A Hex Workbench recipe: the 3 x 3 grid, the arrow, the result, and under it the bench and the charge it spends. */
    @Nullable
    private List<FormattedCharSequence> renderHexCrafting(GuiGraphicsExtractor graphics, QuestDisplay display, int x, int y, int mouseX,
                                                          int mouseY, long now) {
        com.pockyl.hextale.crafting.HexCraftingRecipe recipe = com.pockyl.hextale.crafting.HexCraftingRecipe.ALL.get(true)
                .get(Identifier.parse(display.result()));
        int centre = x + innerWidth() / 2;
        if (recipe == null) {
            PixelArt.flush(graphics);
            graphics.centeredText(font, Component.translatable("book.hextale.quest.infusion.missing"), centre, y + 20, MUTED);
            return null;
        }
        List<FormattedCharSequence> tooltip = null;
        int gridWidth = SLOT * 3;
        int left = centre - (gridWidth + 26 + SLOT) / 2;
        List<java.util.Optional<Ingredient>> cells = recipe.grid();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = cells.get(i).map(ingredient -> cycle(ingredient, now, 0)).orElse(ItemStack.EMPTY);
            tooltip = orTooltip(tooltip, slot(graphics, stack, left + i % 3 * SLOT, y + i / 3 * SLOT, mouseX, mouseY, null));
        }
        int ax = left + gridWidth + 4;
        arrow(graphics, ax, y + SLOT + SLOT / 2);
        tooltip = orTooltip(tooltip, slot(graphics, recipe.result().create(), ax + 22, y + SLOT, mouseX, mouseY, null));
        PixelArt.flush(graphics);
        Component caption = Component.translatable(recipe.shapeless() ? "book.hextale.quest.hex_crafting.shapeless" : "book.hextale.quest.hex_crafting",
                recipe.charge());
        graphics.centeredText(font, caption, centre, y + SLOT * 3 + 2, 0xFF7FD8FF);
        return tooltip;
    }

    /** A Hex Furnace alloy: the metals with their counts, plus signs, the arrow and the ingot, the furnace named under them. */
    @Nullable
    private List<FormattedCharSequence> renderAlloy(GuiGraphicsExtractor graphics, QuestDisplay display, int x, int y, int mouseX,
                                                    int mouseY, long now) {
        com.pockyl.hextale.crafting.AlloyRecipe recipe = com.pockyl.hextale.crafting.AlloyRecipe.ALL.get(true)
                .get(Identifier.parse(display.result()));
        int centre = x + innerWidth() / 2;
        if (recipe == null) {
            PixelArt.flush(graphics);
            graphics.centeredText(font, Component.translatable("book.hextale.quest.infusion.missing"), centre, y + 8, MUTED);
            return null;
        }
        List<FormattedCharSequence> tooltip = null;
        int count = recipe.inputs().size();
        int width = count * SLOT + (count - 1) * 10 + 26 + SLOT;
        int sx = centre - width / 2;
        for (int i = 0; i < count; i++) {
            var input = recipe.inputs().get(i);
            ItemStack stack = cycle(input.ingredient(), now, i).copyWithCount(input.count());
            tooltip = orTooltip(tooltip, slot(graphics, stack, sx, y, mouseX, mouseY, null));
            sx += SLOT;
            if (i < count - 1) {
                plus(graphics, sx + 5, y + SLOT / 2);
                sx += 10;
            }
        }
        arrow(graphics, sx + 4, y + SLOT / 2);
        tooltip = orTooltip(tooltip, slot(graphics, recipe.result().create(), sx + 26, y, mouseX, mouseY, null));
        PixelArt.flush(graphics);
        graphics.centeredText(font, Component.translatable("book.hextale.quest.alloy"), centre, y + SLOT + 2, 0xFFFFB060);
        return tooltip;
    }

    // ---- Multiblocks: the structure layer by layer --------------------------------------------------------------------

    private static final int LAYER_GAP = 8;
    private static final int LAYER_LABEL = 10;

    private int layersPerRow(MultiblockPattern pattern) {
        return Math.max(1, (innerWidth() + LAYER_GAP) / (pattern.width() * SLOT + LAYER_GAP));
    }

    private int multiblockHeight(QuestDisplay display) {
        Multiblock multiblock = Multiblocks.byId(display.result());
        if (multiblock == null) {
            return SLOT;
        }
        MultiblockPattern pattern = multiblock.pattern();
        int rows = (pattern.height() + layersPerRow(pattern) - 1) / layersPerRow(pattern);
        return rows * (LAYER_LABEL + pattern.depth() * SLOT + 6) + 10 + SLOT;
    }

    /**
     * A multiblock from the bottom layer up, each layer a small grid seen from above (north at the top), the hollow cells
     * dark, and the items that wake it under them.
     */
    @Nullable
    private List<FormattedCharSequence> renderMultiblock(GuiGraphicsExtractor graphics, QuestDisplay display, int x, int y, int mouseX,
                                                         int mouseY, long now) {
        Multiblock multiblock = Multiblocks.byId(display.result());
        if (multiblock == null) {
            return null;
        }
        MultiblockPattern pattern = multiblock.pattern();
        java.util.Map<Long, MultiblockPattern.Cell> cells = new java.util.HashMap<>();
        for (MultiblockPattern.Cell cell : pattern.cells()) {
            cells.put(BlockPos.asLong(cell.x(), cell.y(), cell.z()), cell);
        }
        List<FormattedCharSequence> tooltip = null;
        int perRow = layersPerRow(pattern);
        int gridWidth = pattern.width() * SLOT;
        int rowWidth = Math.min(pattern.height(), perRow) * (gridWidth + LAYER_GAP) - LAYER_GAP;
        int left = x + (innerWidth() - rowWidth) / 2;
        int rowHeight = LAYER_LABEL + pattern.depth() * SLOT + 6;
        for (int layer = 0; layer < pattern.height(); layer++) {
            int gx = left + layer % perRow * (gridWidth + LAYER_GAP);
            int gy = y + layer / perRow * rowHeight;
            PixelArt.flush(graphics);
            graphics.centeredText(font, Component.translatable("book.hextale.quest.multiblock.layer", layer + 1), gx + gridWidth / 2, gy, MUTED);
            for (int z = 0; z < pattern.depth(); z++) {
                for (int cx = 0; cx < pattern.width(); cx++) {
                    MultiblockPattern.Cell cell = cells.get(BlockPos.asLong(cx, layer, z));
                    int sx = gx + cx * SLOT;
                    int sy = gy + LAYER_LABEL + z * SLOT;
                    if (cell == null) {
                        continue;
                    }
                    if (cell.key() == MultiblockPattern.Key.AIR) {
                        PixelArt.fill(graphics, sx, sy, sx + SLOT, sy + SLOT, PixelArt.argb(0x05040A, 1));
                        if (over(mouseX, mouseY, sx, sy, SLOT, SLOT)) {
                            tooltip = List.of(Component.translatable("book.hextale.quest.multiblock.hollow").withColor(MUTED).getVisualOrderText());
                        }
                        continue;
                    }
                    List<ItemStack> shown = cell.key().shown().get();
                    ItemStack stack = shown.isEmpty() ? ItemStack.EMPTY : shown.get((int) ((now / 1000 + cx + z) % shown.size()));
                    tooltip = orTooltip(tooltip, slot(graphics, stack, sx, sy, mouseX, mouseY, null));
                }
            }
        }
        int rows = (pattern.height() + perRow - 1) / perRow;
        int wy = y + rows * rowHeight;
        PixelArt.flush(graphics);
        Component wake = Component.translatable("book.hextale.quest.multiblock.wake");
        List<ItemStack> activators = multiblock.activators();
        int width = font.width(wake) + 6 + activators.size() * (SLOT + 2);
        int wx = x + (innerWidth() - width) / 2;
        graphics.text(font, wake, wx, wy + 5, MUTED, false);
        int ax = wx + font.width(wake) + 6;
        for (ItemStack activator : activators) {
            tooltip = orTooltip(tooltip, slot(graphics, activator, ax, wy, mouseX, mouseY, null));
            ax += SLOT + 2;
        }
        return tooltip;
    }

    private static int infusionHeight(QuestDisplay display) {
        InfusionRecipe recipe = InfusionRecipes.get(true).get(Identifier.parse(display.result()));
        if (recipe == null) {
            return INFUSION_RING * 2 + SLOT + 16;
        }
        int essences = recipe.totalEssences().asMap().size();
        int runes = recipe.runes().size();
        return INFUSION_RING * 2 + SLOT + 6 + (essences + ESSENCES_PER_ROW - 1) / ESSENCES_PER_ROW * ESSENCE_ROW
                + runes * RECIPE_RUNE_ROW + (runes > 0 ? 12 : 0);
    }

    /**
     * An altar ritual: the centre item inside a ring of pedestal items, an arrow to the result, and under them every
     * essence the altar draws (sign and mB, the runes' own essences included) and the runes it needs on steles.
     */
    @Nullable
    private List<FormattedCharSequence> renderInfusion(GuiGraphicsExtractor graphics, QuestDisplay display, int x, int y, int mouseX, int mouseY,
                                                       long now) {
        InfusionRecipe recipe = InfusionRecipes.get(true).get(Identifier.parse(display.result()));
        if (recipe == null) {
            PixelArt.flush(graphics);
            graphics.centeredText(font, Component.translatable("book.hextale.quest.infusion.missing"), x + innerWidth() / 2, y + 30, MUTED);
            return null;
        }
        List<FormattedCharSequence> tooltip = null;
        int ringCentreX = x + innerWidth() / 2 - 22;
        int ringCentreY = y + INFUSION_RING + SLOT / 2;
        for (int a = 0; a < 48; a++) {
            double angle = a * Math.PI * 2 / 48;
            int px = ringCentreX + (int) Math.round(Math.sin(angle) * INFUSION_RING);
            int py = ringCentreY - (int) Math.round(Math.cos(angle) * INFUSION_RING);
            PixelArt.fill(graphics, px, py, px + 1, py + 1, PixelArt.argb(0x5CC8F2, 0.35F));
        }
        List<Ingredient> pedestals = recipe.pedestals();
        for (int i = 0; i < pedestals.size(); i++) {
            double angle = i * Math.PI * 2 / pedestals.size();
            int sx = ringCentreX + (int) Math.round(Math.sin(angle) * INFUSION_RING) - SLOT / 2;
            int sy = ringCentreY - (int) Math.round(Math.cos(angle) * INFUSION_RING) - SLOT / 2;
            tooltip = orTooltip(tooltip, slot(graphics, cycle(pedestals.get(i), now, i), sx, sy, mouseX, mouseY,
                    Component.translatable("book.hextale.quest.infusion.pedestal").withColor(0x9FE6FF)));
        }
        ItemStack center = cycle(recipe.center(), now, 99);
        tooltip = orTooltip(tooltip, slot(graphics, center, ringCentreX - SLOT / 2, ringCentreY - SLOT / 2, mouseX, mouseY,
                Component.translatable("book.hextale.quest.infusion.center").withColor(StarBook.GOLD)));
        int ax = ringCentreX + INFUSION_RING + SLOT / 2 + 6;
        arrow(graphics, ax, ringCentreY);
        tooltip = orTooltip(tooltip, slot(graphics, recipe.resultFor(center), ax + 22, ringCentreY - SLOT / 2, mouseX, mouseY, null));
        int rowY = y + INFUSION_RING * 2 + SLOT + 4;
        var essences = recipe.totalEssences().asMap();
        int column = innerWidth() / ESSENCES_PER_ROW;
        int i = 0;
        for (var entry : essences.entrySet()) {
            Essence essence = entry.getKey();
            int amount = entry.getValue();
            int cx = x + i % ESSENCES_PER_ROW * column;
            int cy = rowY + i / ESSENCES_PER_ROW * ESSENCE_ROW;
            PixelArt.essence(graphics, essence, cx, cy, PixelArt.ICON, PixelArt.argb(essence.color(), 1));
            PixelArt.flush(graphics);
            String text = amount + " mB";
            graphics.text(font, text, cx + 11, cy + 1, 0xFF000000 | essence.color(), true);
            if (over(mouseX, mouseY, cx, cy, 11 + font.width(text), 10)) {
                tooltip = List.of(Component.translatable("book.hextale.quest.infusion.essence", Component.translatable(essence.translationKey()), amount)
                        .withColor(essence.color()).getVisualOrderText());
            }
            i++;
        }
        rowY += (essences.size() + ESSENCES_PER_ROW - 1) / ESSENCES_PER_ROW * ESSENCE_ROW;
        for (ComplexRune rune : recipe.runes()) {
            PixelArt.glyph(graphics, rune, x, rowY, 11, PixelArt.argb(rune.color(), 1), 0xFF000000);
            PixelArt.flush(graphics);
            Component name = Component.translatable("book.hextale.quest.infusion.rune", Component.translatable(rune.translationKey()));
            graphics.text(font, name, x + 14, rowY + 2, 0xFF000000 | rune.color(), true);
            rowY += RECIPE_RUNE_ROW;
        }
        if (!recipe.runes().isEmpty()) {
            graphics.text(font, Component.translatable("book.hextale.quest.infusion.runes"), x, rowY + 1, MUTED, false);
        }
        return tooltip;
    }

    // ---- Runes a pickaxe or a pylon takes ---------------------------------------------------------------------------

    /** One rune a holder takes: what it does, how much per strength, its cost per star and what it is incompatible with. */
    private record Option(ComplexRune rune, String descriptionKey, String valueKey, IntFunction<String> amount, EssenceMix costPerStar,
                          String clashes) {
    }

    private static List<Option> options(RuneHolder holder) {
        List<Option> out = new ArrayList<>();
        if (holder == RuneHolder.PICKAXE) {
            for (PickaxeRune rune : PickaxeRune.values()) {
                StringBuilder clashes = new StringBuilder();
                for (PickaxeRune other : PickaxeRune.values()) {
                    if (rune.clashes(other)) {
                        clashes.append(clashes.isEmpty() ? "" : ", ").append(Component.translatable(other.rune().translationKey()).getString());
                    }
                }
                out.add(new Option(rune.rune(), rune.descriptionKey(), rune.valueKey(), strength -> PickaxeEffects.amount(rune, strength),
                        rune.cost(1), clashes.toString()));
            }
        } else {
            for (ComplexRune rune : holder.takes()) {
                out.add(new Option(rune, holder.descriptionKey(rune), holder.valueKey(rune), strength -> holder.amount(rune, strength),
                        holder.cost(rune, 1), ""));
            }
        }
        return out;
    }

    private Option chosen(List<Option> options) {
        for (Option option : options) {
            if (option.rune() == optionRune) {
                return option;
            }
        }
        return options.getFirst();
    }

    private List<FormattedCharSequence> optionDescription(Option option) {
        return font.split(Component.translatable(option.descriptionKey()), innerWidth());
    }

    private List<FormattedCharSequence> optionNote(RuneHolder holder) {
        return font.split(Component.translatable(holder.bookNoteKey(), holder.slots()), innerWidth());
    }

    private static boolean knows(ComplexRune rune) {
        RuneKnowledge knowledge = ClientKnowledge.get();
        return knowledge != null && knowledge.knows(rune);
    }

    private int optionsHeight(RuneHolder holder) {
        List<Option> options = options(holder);
        Option option = chosen(options);
        int rows = (options.size() + RUNES_PER_ROW - 1) / RUNES_PER_ROW;
        return DIORAMA_HEIGHT + 4 + 10 + rows * RUNE_ROW + 4 + 12 + (knows(option.rune()) ? 0 : 10) + optionDescription(option).size() * 10 + 4
                + 22 + 12 + (option.clashes().isEmpty() ? 0 : 10) + 2 + optionNote(holder).size() * 10;
    }

    /**
     * The runes a holder takes: a turning 3D altar with the chosen rune flowing from its stele into the pickaxe or the
     * smelter, a grid of rune signs to choose from (greyed while not studied) and the chosen rune's card - what it does,
     * how much at one, two and three stars, its essence cost and the runes it is incompatible with.
     */
    @Nullable
    private List<FormattedCharSequence> renderOptions(GuiGraphicsExtractor graphics, RuneHolder holder, int x, int y, int mouseX, int mouseY,
                                                      long now) {
        List<FormattedCharSequence> tooltip = null;
        int w = innerWidth();
        List<Option> options = options(holder);
        Option option = chosen(options);
        ComplexRune chosen = option.rune();

        int frame = PixelArt.argb(StarBook.GOLD, 0.35F);
        PixelArt.fill(graphics, x, y, x + w, y + DIORAMA_HEIGHT, PixelArt.argb(0x0B0918, 1));
        PixelArt.fill(graphics, x, y, x + w, y + 1, frame);
        PixelArt.fill(graphics, x, y + DIORAMA_HEIGHT - 1, x + w, y + DIORAMA_HEIGHT, frame);
        PixelArt.fill(graphics, x, y + 1, x + 1, y + DIORAMA_HEIGHT - 1, frame);
        PixelArt.fill(graphics, x + w - 1, y + 1, x + w, y + DIORAMA_HEIGHT - 1, frame);
        PixelArt.flush(graphics);
        ItemStack item = holder.items().getFirst();
        holder.set(item, PickaxeRunes.EMPTY.with(chosen, 3));
        AltarDiorama.draw(graphics, item, chosen, now / 50F, x + 1, y + 1, x + w - 1, y + DIORAMA_HEIGHT - 1, 26);
        y += DIORAMA_HEIGHT + 4;

        graphics.text(font, Component.translatable("book.hextale.quest.pickaxe_rune.pick"), x, y, MUTED, false);
        y += 10;
        runeGridX = x;
        runeGridY = y;
        for (int r = 0; r < options.size(); r++) {
            ComplexRune rune = options.get(r).rune();
            int cx = x + r % RUNES_PER_ROW * RUNE_CELL;
            int cy = y + r / RUNES_PER_ROW * RUNE_ROW;
            boolean hovered = over(mouseX, mouseY, cx, cy, RUNE_CELL, RUNE_ROW);
            boolean picked = rune == chosen;
            if (picked || hovered) {
                PixelArt.hex(graphics, cx + RUNE_CELL / 2, cy + RUNE_ROW / 2, RUNE_ROW - 1,
                        PixelArt.argb(PixelArt.mix(rune.color(), 0x06050D, picked ? 0.65F : 0.82F), 1),
                        PixelArt.argb(picked ? rune.color() : 0x5A5080, picked ? 0.9F : 0.6F));
            }
            int color = knows(rune) ? rune.color() : 0x6A6480;
            PixelArt.glyph(graphics, rune, cx + (RUNE_CELL - 14) / 2, cy + (RUNE_ROW - 14) / 2, 14, PixelArt.argb(color, 1), 0xFF000000);
            if (hovered) {
                tooltip = List.of(Component.translatable(rune.translationKey()).withColor(rune.color()).getVisualOrderText());
            }
        }
        PixelArt.flush(graphics);
        y += (options.size() + RUNES_PER_ROW - 1) / RUNES_PER_ROW * RUNE_ROW + 4;
        PixelArt.fill(graphics, x, y - 2, x + w, y - 1, PixelArt.argb(StarBook.GOLD, 0.2F));
        PixelArt.flush(graphics);

        graphics.text(font, Component.translatable(chosen.translationKey()), x, y + 2, 0xFF000000 | chosen.color(), true);
        Component chapter = Component.translatable("book.hextale.quest.pickaxe_rune.chapter", "I".repeat(ResearchBook.chapter(chosen)));
        graphics.text(font, chapter, x + w - font.width(chapter), y + 2, MUTED, false);
        y += 12;
        if (!knows(chosen)) {
            graphics.text(font, Component.translatable("book.hextale.quest.pickaxe_rune.unknown"), x, y, 0xFFE0A86A, false);
            y += 10;
        }
        for (FormattedCharSequence line : optionDescription(option)) {
            graphics.text(font, line, x, y, TEXT, false);
            y += 10;
        }
        y += 4;
        int column = w / 3;
        for (int stars = 1; stars <= 3; stars++) {
            int cx = x + (stars - 1) * column + column / 2;
            graphics.centeredText(font, Component.literal("★".repeat(stars)), cx, y, 0xFF000000 | StarBook.GOLD);
            graphics.centeredText(font, Component.translatable(option.valueKey(), option.amount().apply(stars)), cx, y + 10, 0xFFFFFFFF);
        }
        y += 22;
        var cost = option.costPerStar().asMap().entrySet().iterator().next();
        Essence essence = cost.getKey();
        PixelArt.essence(graphics, essence, x, y, PixelArt.ICON, PixelArt.argb(essence.color(), 1));
        PixelArt.flush(graphics);
        Component costLine = Component.translatable(essence.translationKey()).withColor(essence.color())
                .append(Component.literal(" · ").withColor(0x9A90B8))
                .append(Component.translatable("book.hextale.quest.pickaxe_rune.cost", cost.getValue()).withColor(0xD8D0E8));
        graphics.text(font, costLine, x + PixelArt.ICON + 2, y + 1, 0xFFFFFFFF, false);
        y += 12;
        if (!option.clashes().isEmpty()) {
            graphics.text(font, Component.translatable("book.hextale.quest.pickaxe_rune.clash", option.clashes()), x, y, 0xFFE07A6A, false);
            y += 10;
        }
        y += 2;
        for (FormattedCharSequence line : optionNote(holder)) {
            graphics.text(font, line, x, y, MUTED, false);
            y += 10;
        }
        return tooltip;
    }

    /** Picks the rune under the mouse in the last drawn runes grid; true when one was hit. */
    private boolean pickRune(double mouseX, double mouseY) {
        QuestDisplay display = selected == null ? null : selected.display();
        if (display == null || runeGridX == Integer.MIN_VALUE) {
            return false;
        }
        RuneHolder holder = switch (display.kind()) {
            case PICKAXE_RUNES -> RuneHolder.PICKAXE;
            case PYLON_RUNES -> RuneHolder.PYLON;
            default -> null;
        };
        if (holder == null) {
            return false;
        }
        List<Option> options = options(holder);
        for (int r = 0; r < options.size(); r++) {
            int cx = runeGridX + r % RUNES_PER_ROW * RUNE_CELL;
            int cy = runeGridY + r / RUNES_PER_ROW * RUNE_ROW;
            if (mouseX >= cx && mouseX < cx + RUNE_CELL && mouseY >= cy && mouseY < cy + RUNE_ROW) {
                ComplexRune rune = options.get(r).rune();
                if (rune != chosen(options).rune()) {
                    optionRune = rune;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.3F, 0.6F));
                }
                return true;
            }
        }
        return false;
    }

    // ---- Small drawing helpers ---------------------------------------------------------------------------------------

    /** One of an ingredient's items, changing every second. */
    private static ItemStack cycle(Ingredient ingredient, long now, int salt) {
        var items = ingredient.items().toList();
        if (items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(items.get((int) ((now / 1000 + salt) % items.size())));
    }

    @Nullable
    private static List<FormattedCharSequence> orTooltip(@Nullable List<FormattedCharSequence> current, @Nullable List<FormattedCharSequence> next) {
        return next != null ? next : current;
    }

    /** A recessed slot with an item; returns the item's tooltip when the mouse is over it. */
    @Nullable
    private List<FormattedCharSequence> slot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY,
                                             @Nullable Component extra) {
        boolean hovered = over(mouseX, mouseY, x, y, SLOT, SLOT);
        PixelArt.fill(graphics, x, y, x + SLOT, y + SLOT, PixelArt.argb(hovered ? 0x3A3460 : 0x1A1630, 1));
        PixelArt.fill(graphics, x, y, x + SLOT, y + 1, PixelArt.argb(0x05040A, 1));
        PixelArt.fill(graphics, x, y, x + 1, y + SLOT, PixelArt.argb(0x05040A, 1));
        PixelArt.fill(graphics, x + 1, y + SLOT - 1, x + SLOT, y + SLOT, PixelArt.argb(0x4A4468, 1));
        PixelArt.fill(graphics, x + SLOT - 1, y + 1, x + SLOT, y + SLOT, PixelArt.argb(0x4A4468, 1));
        if (stack.isEmpty()) {
            return null;
        }
        PixelArt.flush(graphics);
        graphics.item(stack, x + 1, y + 1);
        graphics.itemDecorations(font, stack, x + 1, y + 1);
        if (!hovered) {
            return null;
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(stack.getHoverName().getVisualOrderText());
        if (extra != null) {
            lines.add(extra.getVisualOrderText());
        }
        return lines;
    }

    private static boolean over(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static void arrow(GuiGraphicsExtractor graphics, int x, int midY) {
        int color = PixelArt.argb(StarBook.GOLD, 0.9F);
        PixelArt.fill(graphics, x, midY - 1, x + 12, midY + 1, color);
        for (int i = 0; i < 5; i++) {
            PixelArt.fill(graphics, x + 12 + i, midY - 4 + i, x + 13 + i, midY + 4 - i, color);
        }
    }

    private static void plus(GuiGraphicsExtractor graphics, int cx, int cy) {
        int color = PixelArt.argb(StarBook.GOLD, 0.9F);
        PixelArt.fill(graphics, cx - 3, cy - 1, cx + 3, cy + 1, color);
        PixelArt.fill(graphics, cx - 1, cy - 3, cx + 1, cy + 3, color);
    }

    /** The result shows the quest's own icon when it is the same item (a filled bottle, not an empty one). */
    private static ItemStack result(Quest quest, QuestDisplay display) {
        ItemStack icon = quest.icon();
        ItemStack result = stack(display.result(), 0, 0);
        if (!result.isEmpty() && icon.is(result.getItem())) {
            result = icon;
        }
        result = result.copy();
        result.setCount(Math.max(1, display.count()));
        return result;
    }

    /** An item id, or a tag cycling through its items once a second. */
    private static ItemStack stack(String id, long now, int phase) {
        if (id.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (id.startsWith("#")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1)));
            List<Holder<Item>> items = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(items::add);
            return items.isEmpty() ? ItemStack.EMPTY : new ItemStack(items.get((int) ((now / 1000 + phase) % items.size())));
        }
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
    }

    // ---- Input ----------------------------------------------------------------------------------------------------

    /** A click on the plate: picks a rune in a runes picture or presses the research button (laying the topic on the table). */
    void plateClicked(double mouseX, double mouseY, RuneKnowledge knowledge) {
        Quest quest = selected;
        if (quest == null) {
            return;
        }
        if (overBack(mouseX, mouseY, knowledge)) {
            close();
            return;
        }
        if (pickRune(mouseX, mouseY) || !studyButton(quest)) {
            return;
        }
        int ix = plateLeft() + PAD;
        int by = buttonTop(quest, knowledge);
        ResearchTopic topic = quest.topic();
        StarBook.Host host = book.host();
        if (mouseX >= ix && mouseX < ix + innerWidth() && mouseY >= by && mouseY < by + StarBook.BUTTON_HEIGHT
                && Guide.canResearch(topic, knowledge) && host.researching() != topic) {
            host.research(topic);
        }
    }

    /** Closes the plate and goes back to the map. */
    void close() {
        if (select(null)) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.9F, 0.5F));
        }
    }

    void scrollPlate(double amount) {
        scroll -= (int) Math.round(amount * 12);
        if (scroll < 0) {
            scroll = 0;
        }
    }
}
