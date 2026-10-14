package com.pockyl.hextale.client.screen.diary;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.research.ResearchTool;
import com.pockyl.hextale.research.RuneBoard;
import com.pockyl.hextale.research.RuneBoards;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * The research book, quest-book style: one spread per chapter. The left page maps the chapter's runes (stars under
 * each), the right page describes the selected rune - what it does, its star thresholds and resources - and, on a
 * Research Table, sends it to the table.
 */
final class ResearchSection implements DiarySection {
    private static final String KEY = "diary.hextale.research.";
    private static final int NODE = 13;
    private static final int GLYPH = 9;
    private static final int PARCHMENT = 0xE8D9B5;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final WidgetSprites BUTTON_SPRITES = new WidgetSprites(ResourceLocation.withDefaultNamespace("widget/button"),
            ResourceLocation.withDefaultNamespace("widget/button_disabled"),
            ResourceLocation.withDefaultNamespace("widget/button_highlighted"));
    private static final String[] TIER_KEYS = {"", "simple", "medium", "hard"};
    private static final int[] TIER_COLORS = {0, 0x4E8A2E, 0x9A6B12, 0x9E3426};

    @Nullable
    private ComplexRune selected;
    /** Node positions of the spread last drawn, relative to the left page: rune -> {x, y} of the centre. */
    private final Map<ComplexRune, int[]> nodes = new LinkedHashMap<>();
    private int nodesChapter;

    /** Selects a rune; returns the spread (chapter - 1) to show it on. */
    int select(ComplexRune rune) {
        selected = rune;
        return ResearchBook.chapter(rune) - 1;
    }

    @Override
    public Component title() {
        return Component.translatable(KEY + "title");
    }

    @Override
    public void icon(InkRenderer ink, float x, float y, int color) {
        int c = RuneDrawing.argb(color, 1);
        ink.stroke(new float[]{x - 4, y + 4, x, y - 4, x + 4, y + 4, x - 4, y + 4}, 1.1F, c);
        ink.disc(x, y + 1.5F, 1.2F, c, c, false);
    }

    @Override
    public int spreads(DiaryContext context) {
        return 3;
    }

    @Override
    public boolean numberedPages() {
        return false;
    }

    @Override
    public void opened(DiaryContext context) {
        if (selected != null) {
            return;
        }
        DiaryBook.Host host = context.host();
        ComplexRune onTable = host == null ? null : host.studying();
        if (onTable != null) {
            selected = onTable;
            context.turnTo(ResearchBook.chapter(onTable) - 1);
            return;
        }
        for (int chapter = 3; chapter >= 1; chapter--) {
            if (!ResearchBook.chapterOpen(context.knowledge, chapter)) {
                continue;
            }
            for (ComplexRune rune : ResearchBook.runes(chapter)) {
                if (ResearchBook.open(context.knowledge, rune) && !context.knowledge.knows(rune)) {
                    selected = rune;
                    context.turnTo(chapter - 1);
                    return;
                }
            }
        }
        selected = ResearchBook.FIRST;
    }

    @Override
    public void render(DiaryContext context, int spread, int mouseX, int mouseY) {
        int chapter = spread + 1;
        layout(chapter);
        Page left = context.left();
        renderMap(context, left, chapter, mouseX, mouseY);
        if (selected == null || ResearchBook.chapter(selected) != chapter) {
            selected = ResearchBook.runes(chapter).get(0);
        }
        renderCard(context, context.right(), selected, mouseX, mouseY);
    }

    // ---- The chapter map ------------------------------------------------------------------------------------------

    private void layout(int chapter) {
        if (nodesChapter == chapter && !nodes.isEmpty()) {
            return;
        }
        nodes.clear();
        nodesChapter = chapter;
        List<ComplexRune> runes = ResearchBook.runes(chapter);
        if (chapter == 1) {
            nodes.put(ResearchBook.FIRST, new int[]{14, 92});
            ResearchTool[] tools = ResearchTool.values();
            for (int i = 0; i < tools.length; i++) {
                nodes.put(tools[i].rune(), new int[]{60, 36 + i * 28});
            }
            return;
        }
        for (int row = 0; row < rows(chapter).size(); row++) {
            List<ComplexRune> list = rows(chapter).get(row);
            int spacing = chapter == 2 ? 17 : 15;
            int y = chapter == 2 ? 36 + row * 20 : 50 + row * 40;
            for (int i = 0; i < list.size(); i++) {
                nodes.put(list.get(i), new int[]{chapter == 2 ? 23 + i * spacing : 22 + i * spacing, y});
            }
        }
        if (nodes.size() != runes.size()) {
            throw new IllegalStateException("Research book layout misses runes of chapter " + chapter);
        }
    }

    /** Chapter II: a row per acting element; chapter III: shadow acts, lightning acts, others act on shadow. */
    private static List<List<ComplexRune>> rows(int chapter) {
        List<List<ComplexRune>> rows = new ArrayList<>();
        List<ComplexRune> runes = ResearchBook.runes(chapter);
        if (chapter == 2) {
            for (SimpleRune action : SimpleRune.values()) {
                List<ComplexRune> row = runes.stream().filter(r -> r.action() == action).toList();
                if (!row.isEmpty()) {
                    rows.add(row);
                }
            }
        } else {
            rows.add(runes.stream().filter(r -> r.action() == SimpleRune.SHADOW).toList());
            rows.add(runes.stream().filter(r -> r.action() == SimpleRune.SPARK).toList());
            rows.add(runes.stream().filter(r -> r.action() != SimpleRune.SHADOW && r.action() != SimpleRune.SPARK).toList());
        }
        return rows;
    }

    private void renderMap(DiaryContext context, Page page, int chapter, int mouseX, int mouseY) {
        RuneKnowledge knowledge = context.knowledge;
        List<ComplexRune> runes = ResearchBook.runes(chapter);
        int learned = ResearchBook.learnedIn(knowledge, chapter);
        int stars = runes.stream().mapToInt(knowledge::stars).sum();
        boolean open = ResearchBook.chapterOpen(knowledge, chapter);
        page.centered(Component.translatable(KEY + "chapter." + chapter).withStyle(ChatFormatting.BOLD), DiaryContext.HEADING);
        page.centered(open ? Component.translatable(KEY + "progress", learned, runes.size(), stars, runes.size() * 3)
                : Component.translatable(KEY + "locked." + chapter, chapter == 2 ? ResearchBook.CHAPTER_II_AFTER
                : ResearchBook.CHAPTER_III_AFTER), open ? DiaryContext.MUTED : DiaryContext.RED);

        int px = page.x;
        int py = page.top;
        if (chapter == 1) {
            int[] first = nodes.get(ResearchBook.FIRST);
            int lineColor = knowledge.knows(ResearchBook.FIRST) ? 0xFFB8860B : 0x669A7A55;
            for (ResearchTool tool : ResearchTool.values()) {
                int[] to = nodes.get(tool.rune());
                PixelArt.line(context.graphics, px + first[0] + 7, py + first[1], px + to[0] - 7, py + to[1], lineColor);
            }
        } else {
            List<List<ComplexRune>> rows = rows(chapter);
            for (int row = 0; row < rows.size(); row++) {
                int[] firstNode = nodes.get(rows.get(row).get(0));
                int ly = py + firstNode[1] - 4;
                int lx = px + (chapter == 2 ? 2 : 1);
                if (chapter == 3 && row == 2) {
                    PixelArt.line(context.graphics, lx, ly + 4, lx + 5, ly + 4, 0xFF7A6648);
                    PixelArt.radical(context.graphics, SimpleRune.SHADOW, lx + 6, ly, 9, argb(PixelArt.onParchment(SimpleRune.SHADOW.color())));
                } else {
                    SimpleRune action = rows.get(row).get(0).action();
                    PixelArt.radical(context.graphics, action, lx, ly, 9, argb(PixelArt.onParchment(action.color())));
                    PixelArt.line(context.graphics, lx + 10, ly + 4, lx + 12, ly + 4, 0xFF7A6648);
                }
            }
        }
        DiaryBook.Host host = context.host();
        ComplexRune onTable = host == null ? null : host.studying();
        long now = Util.getMillis();
        for (Map.Entry<ComplexRune, int[]> entry : nodes.entrySet()) {
            ComplexRune rune = entry.getKey();
            int x = px + entry.getValue()[0];
            int y = py + entry.getValue()[1];
            boolean runeOpen = ResearchBook.open(knowledge, rune);
            int st = knowledge.stars(rune);
            int fill = st > 0 ? PixelArt.mix(rune.color(), PARCHMENT, 0.62F) : runeOpen ? 0xF4EAD0 : 0xD8C8A2;
            int outline = 0x8A6A44;
            if (st == 3) {
                outline = 0xB8860B;
            }
            if (rune == onTable) {
                outline = 0x2B8FB8;
            }
            if (rune == selected) {
                outline = PixelArt.mix(0x6B3A12, 0xE0A21C, 0.5F + 0.5F * Mth.sin(now / 250.0F));
            }
            PixelArt.hex(context.graphics, x, y, NODE, argb(fill), argb(outline));
            int glyphColor = st > 0 ? PixelArt.onParchment(rune.color()) : runeOpen ? 0x6B5A40 : 0xB09C78;
            PixelArt.glyph(context.graphics, rune, x - GLYPH / 2, y - GLYPH / 2, GLYPH, argb(glyphColor));
            for (int s = 0; s < 3; s++) {
                int color = s < st ? 0xFFD49A1E : 0x55705838;
                context.graphics.fill(x - 4 + s * 3, y + 9, x - 2 + s * 3, y + 10, color);
            }
            if (chapter == 1) {
                Component name = Component.translatable(rune.translationKey());
                int textX = rune == ResearchBook.FIRST ? x - 12 : x + 10;
                int textY = rune == ResearchBook.FIRST ? y + 13 : y - 4;
                context.graphics.drawString(context.font, name, textX, textY,
                        0xFF000000 | (runeOpen ? (st > 0 ? PixelArt.onParchment(rune.color()) : DiaryContext.INK) : DiaryContext.MUTED), false);
            }
            if (context.hovered(x - 7, y - 8, 14, 18)) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable(rune.translationKey()).withColor(rune.color()));
                lines.add(stars(st));
                if (!runeOpen) {
                    lines.add(Component.translatable(KEY + "node_locked").withStyle(ChatFormatting.GRAY));
                }
                context.tooltip(lines.stream().map(Component::getVisualOrderText).toList());
            }
        }
    }

    // ---- The rune card --------------------------------------------------------------------------------------------

    private void renderCard(DiaryContext context, Page page, ComplexRune rune, int mouseX, int mouseY) {
        RuneKnowledge knowledge = context.knowledge;
        int tier = ResearchBook.chapter(rune);
        int st = knowledge.stars(rune);
        boolean open = ResearchBook.open(knowledge, rune);
        int top = page.y;

        context.graphics.fill(page.x, top, page.x + 34, top + 34, argb(PixelArt.mix(rune.color(), PARCHMENT, 0.78F)));
        PixelArt.glyph(context.graphics, rune, page.x + 3, top + 3, 28, argb(PixelArt.onParchment(rune.color())), 0x40000000);
        int tx = page.x + 39;
        Page head = new Page(context, tx, top, page.width - 39, page.bottom);
        head.text(Component.translatable(rune.translationKey()).withStyle(ChatFormatting.BOLD), PixelArt.onParchment(rune.color()));
        head.y -= 3;
        context.graphics.drawString(context.font, Component.translatable(KEY + "tier." + TIER_KEYS[tier]), tx, head.y,
                0xFF000000 | TIER_COLORS[tier], false);
        head.y += 10;
        context.graphics.drawString(context.font, stars(st), tx, head.y, 0xFFFFFFFF, false);
        page.y = Math.max(top + 37, head.y + 12);
        page.text(formula(rune), DiaryContext.MUTED);
        page.y -= 2;
        page.text(Component.translatable(rune.translationKey() + ".effect"));
        Component use = Component.translatable(rune.translationKey() + ".use").withStyle(ChatFormatting.ITALIC);
        int reserved = BUTTON_HEIGHT + 34;
        if (page.y + page.measure(use, 0) <= page.bottom - reserved) {
            page.text(use, DiaryContext.MUTED);
        }

        RuneBoard board = RuneBoards.request(rune).getNow(null);
        int bottom = page.bottom - (context.host() != null ? BUTTON_HEIGHT + 2 : 0);
        int y = bottom - 30;
        if (!open) {
            new Page(context, page.x, y, page.width, bottom).text(Component.translatable(
                    tier == 1 ? KEY + "locked_first" : KEY + "locked_chapter"), DiaryContext.RED);
        } else if (board == null) {
            context.graphics.drawString(context.font, Component.translatable(KEY + "preparing"), page.x, y + 4,
                    0xFF000000 | DiaryContext.MUTED, false);
        } else {
            RuneBoard.Par par = board.par();
            MutableComponent goals = Component.empty();
            int[] limits = {par.sheet(), par.two(), par.three()};
            for (int s = 1; s <= 3; s++) {
                goals.append(Component.literal("★".repeat(s)).withStyle(st >= s ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY))
                        .append(Component.literal("≤" + limits[s - 1] + (s < 3 ? "  " : "")).withColor(DiaryContext.INK));
            }
            context.graphics.drawString(context.font, goals, page.x, y, 0xFFFFFFFF, false);
            if (context.hovered(page.x, y - 1, page.width, 10)) {
                context.tooltip(Component.translatable(KEY + "goals_tooltip", par.sheet(), par.two(), par.three()));
            }
            Map<Item, Integer> resources = new LinkedHashMap<>();
            for (RuneBoard.Point point : board.points()) {
                resources.merge(point.resource(), 1, Integer::sum);
            }
            int ix = page.x;
            for (Map.Entry<Item, Integer> entry : resources.entrySet()) {
                context.item(new ItemStack(entry.getKey(), entry.getValue()), ix, y + 11);
                ix += 18;
            }
            int best = knowledge.best(rune);
            Component bestText = best > 0 ? Component.translatable(KEY + "best", best) : Component.translatable(KEY + "points",
                    board.points().size());
            context.graphics.drawString(context.font, bestText, page.x + page.width - context.font.width(bestText), y + 15,
                    0xFF000000 | DiaryContext.MUTED, false);
        }
        for (ResearchTool tool : ResearchTool.values()) {
            if (tool.rune() == rune && context.hovered(page.x, top, 34, 34)) {
                context.tooltip(Component.translatable(KEY + "tool", tool.item().getDescription(),
                        Component.translatable(tool.translationKey() + ".level." + Math.max(1, st))));
            }
        }
        DiaryBook.Host host = context.host();
        if (host != null) {
            boolean onTable = host.studying() == rune;
            boolean active = open && !onTable;
            int bx = page.x + (page.width - BUTTON_WIDTH) / 2;
            int by = page.bottom - BUTTON_HEIGHT;
            boolean hovered = active && context.hovered(bx, by, BUTTON_WIDTH, BUTTON_HEIGHT);
            context.graphics.blitSprite(BUTTON_SPRITES.get(active, hovered), bx, by, BUTTON_WIDTH, BUTTON_HEIGHT);
            Component label = Component.translatable(onTable ? KEY + "on_table" : st > 0 ? KEY + "restudy" : KEY + "study");
            context.graphics.drawCenteredString(context.font, label, bx + BUTTON_WIDTH / 2, by + 6, active ? 0xFFFFFFFF : 0xFFA0A0A0);
        }
    }

    private static Component formula(ComplexRune rune) {
        return Component.translatable("rune.hextale.formula", Component.translatable(rune.action().translationKey()),
                Component.translatable(rune.target().translationKey())).append(" · ")
                .append(Component.translatable(rune.category().translationKey()));
    }

    static Component stars(int count) {
        return Component.literal("★".repeat(count)).withStyle(ChatFormatting.GOLD)
                .append(Component.literal("★".repeat(3 - count)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static int argb(int rgb) {
        return 0xFF000000 | rgb;
    }

    @Override
    public boolean mouseClicked(DiaryContext context, int spread, double mouseX, double mouseY) {
        int px = context.bookLeft + DiaryContext.LEFT_PAGE_X;
        int py = context.bookTop + DiaryContext.PAGE_TOP;
        for (Map.Entry<ComplexRune, int[]> entry : nodes.entrySet()) {
            int x = px + entry.getValue()[0];
            int y = py + entry.getValue()[1];
            if (mouseX >= x - 7 && mouseX < x + 7 && mouseY >= y - 8 && mouseY < y + 10) {
                if (selected != entry.getKey()) {
                    selected = entry.getKey();
                    DiaryBook.pageSound();
                }
                return true;
            }
        }
        DiaryBook.Host host = context.host();
        if (host != null && selected != null) {
            int bx = context.bookLeft + DiaryContext.RIGHT_PAGE_X + (DiaryContext.PAGE_WIDTH - BUTTON_WIDTH) / 2;
            int by = context.bookTop + DiaryContext.PAGE_BOTTOM - BUTTON_HEIGHT;
            if (mouseX >= bx && mouseX < bx + BUTTON_WIDTH && mouseY >= by && mouseY < by + BUTTON_HEIGHT
                    && ResearchBook.open(context.knowledge, selected) && host.studying() != selected) {
                host.study(selected);
                return true;
            }
        }
        return false;
    }
}
