package com.pockyl.hextale.client.screen.book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.infusion.InfusionRecipe;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.runework.PickaxeRune;
import com.pockyl.hextale.runework.PickaxeEffects;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.client.ClientKnowledge;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import com.pockyl.hextale.runework.PickaxeRunes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.QuestDisplay;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The research book's first page: the researcher's path. Quests hang as item signs joined in the order they are meant
 * to be done; a click opens the quest's plate with its goal, its recipe or scheme and a short explanation.
 */
final class QuestPath {
    static final int MIN_X = -40;
    static final int MAX_X = 430;
    static final int MIN_Y = -40;
    static final int MAX_Y = 250;
    /** The middle of the path: the whole path fits the book at zoom 1. */
    static final int CENTRE_X = 196;
    static final int CENTRE_Y = 108;
    private static final int NODE = 24;
    private static final int PLATE_WIDTH = 180;
    private static final int PAD = 6;
    private static final int SLOT = 18;
    private static final int TEXT = 0xFFD8D0E8;
    private static final int DONE = 0x8FE07A;
    private static final int OPEN = 0x5CC8F2;
    private static final int LOCKED = 0x4A4468;

    enum State {
        DONE,
        OPEN,
        LOCKED
    }

    private final StarBook book;
    private final Font font;
    @Nullable
    private Quest selected;
    private int scroll;
    /** The rune chosen in a pickaxe-runes picture; where its grid was drawn last (screen coordinates). */
    private PickaxeRune pickaxeRune = PickaxeRune.SMELTING;
    private int runeGridX = Integer.MIN_VALUE;
    private int runeGridY;

    QuestPath(StarBook book) {
        this.book = book;
        this.font = Minecraft.getInstance().font;
    }

    static State state(Quest quest, RuneKnowledge knowledge) {
        if (knowledge.questDone(quest)) {
            return State.DONE;
        }
        return quest.parent() == null || knowledge.questDone(quest.parent()) ? State.OPEN : State.LOCKED;
    }

    // ---- The map --------------------------------------------------------------------------------------------------

    /** Draws links and signs in map coordinates (the caller has set up the camera's pose). */
    void renderMap(GuiGraphicsExtractor graphics, RuneKnowledge knowledge, long now, @Nullable Quest hovered) {
        for (Quest quest : Quest.values()) {
            Quest parent = quest.parent();
            if (parent == null) {
                continue;
            }
            State to = state(quest, knowledge);
            int color = to == State.DONE ? PixelArt.argb(StarBook.GOLD, 0.75F) : to == State.OPEN
                    ? PixelArt.argb(OPEN, 0.45F + 0.2F * pulse(now, 0)) : PixelArt.argb(0x5A5080, 0.3F);
            if (to == State.LOCKED) {
                PixelArt.dotted(graphics, parent.x(), parent.y(), quest.x(), quest.y(), 2, 2, 0, color);
            } else {
                PixelArt.line(graphics, parent.x(), parent.y(), quest.x(), quest.y(), color);
            }
        }
        for (Quest quest : Quest.values()) {
            renderFrame(graphics, quest, state(quest, knowledge), now, quest == hovered);
        }
        PixelArt.flush(graphics);
        for (Quest quest : Quest.values()) {
            graphics.item(quest.icon(), quest.x() - 8, quest.y() - 8);
        }
        for (Quest quest : Quest.values()) {
            State state = state(quest, knowledge);
            if (state == State.LOCKED) {
                PixelArt.hex(graphics, quest.x(), quest.y(), NODE - 4, PixelArt.argb(0x06050D, 0.6F), 0);
            } else if (state == State.DONE) {
                check(graphics, quest.x() + 6, quest.y() + 6);
            }
        }
    }

    private void renderFrame(GuiGraphicsExtractor graphics, Quest quest, State state, long now, boolean hovered) {
        int x = quest.x();
        int y = quest.y();
        float pulse = pulse(now, quest.ordinal());
        if (state == State.OPEN) {
            for (int ring = 0; ring < 3; ring++) {
                PixelArt.hex(graphics, x, y, NODE + 4 + ring * 4, PixelArt.argb(OPEN, (0.16F - ring * 0.05F) * (0.6F + 0.4F * pulse)), 0);
            }
        }
        int rim = switch (state) {
            case DONE -> StarBook.GOLD;
            case OPEN -> PixelArt.mix(0x2B8FB8, 0x7EF0FF, pulse);
            case LOCKED -> LOCKED;
        };
        if (quest == selected || hovered) {
            rim = PixelArt.mix(StarBook.GOLD, 0xFFF6D6, pulse);
        }
        int fill = state == State.DONE ? 0x2A2210 : state == State.OPEN ? 0x0C1A2A : 0x0E0C1E;
        PixelArt.hex(graphics, x, y, NODE, PixelArt.argb(fill, 1), PixelArt.argb(rim, 1));
        if (quest == selected) {
            PixelArt.hexRing(graphics, x, y, NODE + 4, PixelArt.argb(StarBook.GOLD, 0.5F + 0.5F * pulse));
        }
    }

    /** A small tick in a green disc: the quest is done. */
    private static void check(GuiGraphicsExtractor graphics, int x, int y) {
        PixelArt.fill(graphics, x - 1, y, x + 8, y + 7, 0xFF1C3A16);
        PixelArt.fill(graphics, x, y - 1, x + 7, y + 8, 0xFF1C3A16);
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
    Quest questAt(double mouseX, double mouseY) {
        float mx = book.mapX(mouseX);
        float my = book.mapY(mouseY);
        float reach = Math.max(NODE / 2.0F, 8 / book.zoom());
        Quest best = null;
        float bestDistance = reach * reach;
        for (Quest quest : Quest.values()) {
            float dx = quest.x() + 0.5F - mx;
            float dy = quest.y() + 0.5F - my;
            float d = dx * dx + dy * dy;
            if (d < bestDistance) {
                bestDistance = d;
                best = quest;
            }
        }
        return best;
    }

    List<FormattedCharSequence> tooltip(Quest quest, RuneKnowledge knowledge) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable(quest.translationKey()).withColor(StarBook.GOLD).getVisualOrderText());
        lines.add(status(quest, knowledge).getVisualOrderText());
        return lines;
    }

    private static Component status(Quest quest, RuneKnowledge knowledge) {
        return switch (state(quest, knowledge)) {
            case DONE -> Component.translatable("book.hextale.quest.done").withColor(DONE);
            case OPEN -> Component.translatable("book.hextale.quest.goal", Component.translatable(quest.translationKey() + ".goal"))
                    .withColor(OPEN);
            case LOCKED -> Component.translatable("book.hextale.quest.after",
                    Component.translatable(quest.parent().translationKey())).withColor(0x9A90B8);
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
        return true;
    }

    // ---- The plate ------------------------------------------------------------------------------------------------

    private int plateLeft() {
        return book.skyLeft() + book.skyWidth() - PLATE_WIDTH - 6;
    }

    private int plateTop() {
        return book.skyTop() + 6;
    }

    private int innerWidth() {
        return PLATE_WIDTH - PAD * 2;
    }

    private List<FormattedCharSequence> titleLines(Quest quest) {
        return font.split(Component.translatable(quest.translationKey()), innerWidth() - 22);
    }

    private List<FormattedCharSequence> statusLines(Quest quest, RuneKnowledge knowledge) {
        return font.split(status(quest, knowledge), innerWidth());
    }

    private List<FormattedCharSequence> descriptionLines(Quest quest) {
        return font.split(Component.translatable(quest.translationKey() + ".desc"), innerWidth());
    }

    private int headerHeight(Quest quest) {
        return Math.max(18, titleLines(quest).size() * 10 + 4);
    }

    private int displayHeight(QuestDisplay display) {
        return switch (display.kind()) {
            case SHAPED -> SLOT * 3;
            case SHAPELESS -> SLOT * 3 + 10;
            case SCHEME -> SLOT + (Language.getInstance().has(captionKey(display)) ? 10 : 0);
            case ESSENCES -> SLOT;
            case REAGENTS -> 30;
            case INFUSION -> infusionHeight(display);
            case PICKAXE_RUNES -> pickaxeRunesHeight();
        };
    }

    private static String captionKey(QuestDisplay display) {
        return "book.hextale.quest.scheme." + display.result().substring(display.result().indexOf(':') + 1);
    }

    private int contentHeight(Quest quest, RuneKnowledge knowledge) {
        int h = PAD + headerHeight(quest) + statusLines(quest, knowledge).size() * 10 + 6;
        if (quest.display() != null) {
            h += displayHeight(quest.display()) + 8;
        }
        h += descriptionLines(quest).size() * 10;
        if (quest.leadsToStars()) {
            h += 6 + StarBook.BUTTON_HEIGHT;
        }
        return h + PAD;
    }

    private int maxPlateHeight() {
        return book.skyHeight() - 12;
    }

    private int plateHeight(Quest quest, RuneKnowledge knowledge) {
        return Math.min(contentHeight(quest, knowledge), maxPlateHeight());
    }

    boolean overPlate(double x, double y, RuneKnowledge knowledge) {
        return selected != null && x >= plateLeft() && x < plateLeft() + PLATE_WIDTH && y >= plateTop()
                && y < plateTop() + plateHeight(selected, knowledge);
    }

    private int buttonTop(Quest quest, RuneKnowledge knowledge) {
        return plateTop() + contentHeight(quest, knowledge) - PAD - StarBook.BUTTON_HEIGHT - scroll;
    }

    /** Draws the selected quest's plate; returns the tooltip of the item under the mouse, or null. */
    @Nullable
    List<FormattedCharSequence> renderPlate(GuiGraphicsExtractor graphics, RuneKnowledge knowledge, int mouseX, int mouseY, long now) {
        Quest quest = selected;
        if (quest == null) {
            return null;
        }
        int x = plateLeft();
        int top = plateTop();
        int w = PLATE_WIDTH;
        int h = plateHeight(quest, knowledge);
        int content = contentHeight(quest, knowledge);
        scroll = Mth.clamp(scroll, 0, Math.max(0, content - h));
        StarBook.plateBackground(graphics, x, top, w, h);
        PixelArt.flush(graphics);
        graphics.enableScissor(x + 1, top + 1, x + w - 1, top + h - 1);
        boolean mouseIn = mouseX >= x && mouseX < x + w && mouseY >= top && mouseY < top + h;
        List<FormattedCharSequence> tooltip = null;

        int ix = x + PAD;
        int y = top + PAD - scroll;
        // Header: the quest's sign and name.
        graphics.item(quest.icon(), ix, y);
        int ty = y + Math.max(0, (headerHeight(quest) - titleLines(quest).size() * 10) / 2 - 1);
        for (FormattedCharSequence line : titleLines(quest)) {
            graphics.text(font, line, ix + 22, ty, 0xFF000000 | PixelArt.mix(StarBook.GOLD, 0xFFFFFF, 0.35F), true);
            ty += 10;
        }
        y += headerHeight(quest);
        for (FormattedCharSequence line : statusLines(quest, knowledge)) {
            graphics.text(font, line, ix, y, 0xFFFFFFFF, true);
            y += 10;
        }
        y += 2;
        PixelArt.fill(graphics, ix, y, ix + innerWidth(), y + 1, PixelArt.argb(StarBook.GOLD, 0.3F));
        y += 4;

        QuestDisplay display = quest.display();
        if (display != null) {
            y += 2;
            List<FormattedCharSequence> hover = renderDisplay(graphics, quest, display, ix, y, mouseIn ? mouseX : -1, mouseY, now);
            if (hover != null) {
                tooltip = hover;
            }
            y += displayHeight(display) + 6;
        }
        PixelArt.flush(graphics);
        for (FormattedCharSequence line : descriptionLines(quest)) {
            graphics.text(font, line, ix, y, TEXT, false);
            y += 10;
        }
        if (quest.leadsToStars()) {
            int by = buttonTop(quest, knowledge);
            boolean hovered = mouseIn && mouseX >= ix && mouseX < ix + innerWidth() && mouseY >= by && mouseY < by + StarBook.BUTTON_HEIGHT;
            book.magicButton(graphics, ix, by, innerWidth(), Component.translatable("book.hextale.quest.to_stars"), true, hovered, now);
        }
        PixelArt.flush(graphics);
        graphics.disableScissor();
        if (content > h) {
            // A thin scroll bar on the right rim.
            int bar = Math.max(12, h * h / content);
            int by = top + (h - bar) * scroll / Math.max(1, content - h);
            PixelArt.fill(graphics, x + w - 3, by, x + w - 1, by + bar, PixelArt.argb(StarBook.GOLD, 0.6F));
        }
        return tooltip;
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
                ItemStack result = result(quest, display);
                tooltip = orTooltip(tooltip, slot(graphics, result, ax + 22, y + SLOT, mouseX, mouseY, null));
                if (display.kind() == QuestDisplay.Kind.SHAPELESS) {
                    PixelArt.flush(graphics);
                    graphics.centeredText(font, Component.translatable("book.hextale.quest.shapeless"), centre, y + SLOT * 3 + 2, 0xFF9A90B8);
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
                    graphics.centeredText(font, Component.translatable(caption), centre, y + SLOT + 2, 0xFF9A90B8);
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
                    ItemStack reagent = reagent(essence, now);
                    Component extra = Component.translatable("book.hextale.quest.reagent_for", Component.translatable(essence.translationKey()))
                            .withColor(essence.color());
                    tooltip = orTooltip(tooltip, slot(graphics, reagent, sx, y + 12, mouseX, mouseY, extra));
                    sx += SLOT;
                }
            }
            case INFUSION -> tooltip = renderInfusion(graphics, display, x, y, mouseX, mouseY, now);
            case PICKAXE_RUNES -> tooltip = renderPickaxeRunes(graphics, x, y, mouseX, mouseY, now);
        }
        return tooltip;
    }

    /** Radius of the pedestal ring in an altar picture. */
    private static final int INFUSION_RING = 26;

    /** Essences per row and runes per row under an altar picture. */
    private static final int ESSENCES_PER_ROW = 3;
    private static final int RUNES_PER_ROW = 7;
    private static final int RUNE_CELL = 24;
    private static final int DIORAMA_HEIGHT = 84;
    private static final int ESSENCE_ROW = 11;
    private static final int RUNE_ROW = 22;

    private static int infusionHeight(QuestDisplay display) {
        InfusionRecipe recipe = InfusionRecipes.get(true).get(Identifier.parse(display.result()));
        if (recipe == null) {
            return INFUSION_RING * 2 + SLOT + 16;
        }
        int essences = recipe.totalEssences().asMap().size();
        int runes = recipe.runes().size();
        return INFUSION_RING * 2 + SLOT + 6 + (essences + ESSENCES_PER_ROW - 1) / ESSENCES_PER_ROW * ESSENCE_ROW
                + (runes + RUNES_PER_ROW - 1) / RUNES_PER_ROW * RUNE_ROW + (runes > 0 ? 12 : 0);
    }

    /**
     * An altar ritual: the centre item inside a ring of pedestal items, an arrow to the result, and under them every
     * essence the altar draws (sign and mB, the runes' own essences included), the runes it needs on steles (sign and
     * name) and what decides a pickaxe's stars. Drawn from the recipes the server sent.
     */
    @Nullable
    private List<FormattedCharSequence> renderInfusion(GuiGraphicsExtractor graphics, QuestDisplay display, int x, int y, int mouseX, int mouseY,
                                                       long now) {
        InfusionRecipe recipe = InfusionRecipes.get(true).get(Identifier.parse(display.result()));
        if (recipe == null) {
            PixelArt.flush(graphics);
            graphics.centeredText(font, Component.translatable("book.hextale.quest.infusion.missing"), x + innerWidth() / 2, y + 30, 0xFF9A90B8);
            return null;
        }
        List<FormattedCharSequence> tooltip = null;
        int ringCentreX = x + innerWidth() / 2 - 22;
        int ringCentreY = y + INFUSION_RING + SLOT / 2;
        // Faint circle joining the pedestals.
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
        // Every essence, then the runes, in rows under the ring.
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
        List<ComplexRune> runes = recipe.runes();
        int runeColumn = innerWidth() / RUNES_PER_ROW;
        for (int r = 0; r < runes.size(); r++) {
            ComplexRune rune = runes.get(r);
            int cx = x + r % RUNES_PER_ROW * runeColumn;
            int cy = rowY + r / RUNES_PER_ROW * RUNE_ROW;
            PixelArt.glyph(graphics, rune, cx, cy, 11, PixelArt.argb(rune.color(), 1), 0xFF000000);
            PixelArt.flush(graphics);
            Component name = Component.translatable(rune.translationKey());
            graphics.text(font, name, cx + 14, cy + 2, 0xFF000000 | rune.color(), true);
            if (over(mouseX, mouseY, cx, cy, 14 + font.width(name), 12)) {
                tooltip = List.of(Component.translatable("book.hextale.quest.infusion.rune", name).withColor(rune.color()).getVisualOrderText());
            }
        }
        if (!runes.isEmpty()) {
            rowY += (runes.size() + RUNES_PER_ROW - 1) / RUNES_PER_ROW * RUNE_ROW;
            graphics.text(font, Component.translatable("book.hextale.quest.infusion.runes"),
                    x, rowY + 1, 0xFF9A90B8, false);
        }
        return tooltip;
    }

    private int runeRows() {
        return (PickaxeRune.values().length + RUNES_PER_ROW - 1) / RUNES_PER_ROW;
    }

    private List<FormattedCharSequence> runeDescription() {
        return font.split(Component.translatable(pickaxeRune.descriptionKey()), innerWidth());
    }

    private List<FormattedCharSequence> runeNote() {
        return font.split(Component.translatable("book.hextale.quest.pickaxe_rune.note", PickaxeRunes.SLOTS), innerWidth());
    }

    private static boolean knows(PickaxeRune rune) {
        RuneKnowledge knowledge = ClientKnowledge.get();
        return knowledge != null && knowledge.knows(rune.rune());
    }

    private static String clashes(PickaxeRune rune) {
        StringBuilder out = new StringBuilder();
        for (PickaxeRune other : PickaxeRune.values()) {
            if (rune.clashes(other)) {
                out.append(out.isEmpty() ? "" : ", ").append(Component.translatable(other.rune().translationKey()).getString());
            }
        }
        return out.toString();
    }

    private int pickaxeRunesHeight() {
        return DIORAMA_HEIGHT + 4 + 10 + runeRows() * RUNE_ROW + 4 + 12 + (knows(pickaxeRune) ? 0 : 10) + runeDescription().size() * 10 + 4
                + 22 + 12 + (clashes(pickaxeRune).isEmpty() ? 0 : 10) + 2 + runeNote().size() * 10;
    }

    /**
     * The runes a hextech pickaxe takes: a turning 3D altar with the chosen rune flowing from its stele into the
     * pickaxe, a grid of rune signs to choose from (greyed while not studied) and the chosen rune's card - what it does,
     * how much at one, two and three stars, its essence cost and the runes it is incompatible with.
     */
    @Nullable
    private List<FormattedCharSequence> renderPickaxeRunes(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, long now) {
        List<FormattedCharSequence> tooltip = null;
        int w = innerWidth();
        ComplexRune chosen = pickaxeRune.rune();

        // The scene in a dark recess with a thin gold frame.
        int frame = PixelArt.argb(StarBook.GOLD, 0.35F);
        PixelArt.fill(graphics, x, y, x + w, y + DIORAMA_HEIGHT, PixelArt.argb(0x0B0918, 1));
        PixelArt.fill(graphics, x, y, x + w, y + 1, frame);
        PixelArt.fill(graphics, x, y + DIORAMA_HEIGHT - 1, x + w, y + DIORAMA_HEIGHT, frame);
        PixelArt.fill(graphics, x, y + 1, x + 1, y + DIORAMA_HEIGHT - 1, frame);
        PixelArt.fill(graphics, x + w - 1, y + 1, x + w, y + DIORAMA_HEIGHT - 1, frame);
        PixelArt.flush(graphics);
        ItemStack pickaxe = new ItemStack(ModItems.pickaxe(com.pockyl.hextale.runework.HextechPickaxe.HEXTECH).get());
        PickaxeRunes.set(pickaxe, PickaxeRunes.EMPTY.with(pickaxeRune, 3));
        AltarDiorama.draw(graphics, pickaxe, chosen, now / 50F, x + 1, y + 1, x + w - 1, y + DIORAMA_HEIGHT - 1, 26);
        y += DIORAMA_HEIGHT + 4;

        // The signs to choose from.
        graphics.text(font, Component.translatable("book.hextale.quest.pickaxe_rune.pick"), x, y, 0xFF9A90B8, false);
        y += 10;
        runeGridX = x;
        runeGridY = y;
        PickaxeRune[] runes = PickaxeRune.values();
        for (int r = 0; r < runes.length; r++) {
            PickaxeRune option = runes[r];
            ComplexRune rune = option.rune();
            int cx = x + r % RUNES_PER_ROW * RUNE_CELL;
            int cy = y + r / RUNES_PER_ROW * RUNE_ROW;
            boolean hovered = over(mouseX, mouseY, cx, cy, RUNE_CELL, RUNE_ROW);
            boolean picked = option == pickaxeRune;
            if (picked || hovered) {
                PixelArt.hex(graphics, cx + RUNE_CELL / 2, cy + RUNE_ROW / 2, RUNE_ROW - 1,
                        PixelArt.argb(PixelArt.mix(rune.color(), 0x06050D, picked ? 0.65F : 0.82F), 1),
                        PixelArt.argb(picked ? rune.color() : 0x5A5080, picked ? 0.9F : 0.6F));
            }
            int color = knows(option) ? rune.color() : 0x6A6480;
            PixelArt.glyph(graphics, rune, cx + (RUNE_CELL - 14) / 2, cy + (RUNE_ROW - 14) / 2, 14, PixelArt.argb(color, 1), 0xFF000000);
            if (hovered) {
                tooltip = List.of(Component.translatable(rune.translationKey()).withColor(rune.color()).getVisualOrderText());
            }
        }
        PixelArt.flush(graphics);
        y += runeRows() * RUNE_ROW + 4;
        PixelArt.fill(graphics, x, y - 2, x + w, y - 1, PixelArt.argb(StarBook.GOLD, 0.2F));
        PixelArt.flush(graphics);

        // The chosen rune's card: name and chapter, what it does, how much per star, cost, incompatible runes.
        graphics.text(font, Component.translatable(chosen.translationKey()), x, y + 2, 0xFF000000 | chosen.color(), true);
        Component chapter = Component.translatable("book.hextale.quest.pickaxe_rune.chapter",
                "I".repeat(ResearchBook.chapter(chosen)));
        graphics.text(font, chapter, x + w - font.width(chapter), y + 2, 0xFF9A90B8, false);
        y += 12;
        if (!knows(pickaxeRune)) {
            graphics.text(font, Component.translatable("book.hextale.quest.pickaxe_rune.unknown"), x, y, 0xFFE0A86A, false);
            y += 10;
        }
        for (FormattedCharSequence line : runeDescription()) {
            graphics.text(font, line, x, y, TEXT, false);
            y += 10;
        }
        y += 4;
        int column = w / 3;
        for (int stars = 1; stars <= 3; stars++) {
            int cx = x + (stars - 1) * column + column / 2;
            graphics.centeredText(font, Component.literal("★".repeat(stars)), cx, y, 0xFF000000 | StarBook.GOLD);
            graphics.centeredText(font, Component.translatable(pickaxeRune.valueKey(), PickaxeEffects.amount(pickaxeRune, stars)), cx, y + 10,
                    0xFFFFFFFF);
        }
        y += 22;
        var cost = pickaxeRune.cost(1).asMap().entrySet().iterator().next();
        Essence essence = cost.getKey();
        PixelArt.essence(graphics, essence, x, y, PixelArt.ICON, PixelArt.argb(essence.color(), 1));
        PixelArt.flush(graphics);
        Component costLine = Component.translatable(essence.translationKey()).withColor(essence.color())
                .append(Component.literal(" · ").withColor(0x9A90B8))
                .append(Component.translatable("book.hextale.quest.pickaxe_rune.cost", cost.getValue()).withColor(0xD8D0E8));
        graphics.text(font, costLine, x + PixelArt.ICON + 2, y + 1, 0xFFFFFFFF, false);
        y += 12;
        String clashes = clashes(pickaxeRune);
        if (!clashes.isEmpty()) {
            graphics.text(font, Component.translatable("book.hextale.quest.pickaxe_rune.clash", clashes), x, y, 0xFFE07A6A, false);
            y += 10;
        }
        y += 2;
        for (FormattedCharSequence line : runeNote()) {
            graphics.text(font, line, x, y, 0xFF9A90B8, false);
            y += 10;
        }
        return tooltip;
    }

    /** Picks the rune under the mouse in the last drawn pickaxe-runes grid; true when one was hit. */
    private boolean pickRune(double mouseX, double mouseY) {
        QuestDisplay display = selected == null ? null : selected.display();
        if (display == null || display.kind() != QuestDisplay.Kind.PICKAXE_RUNES || runeGridX == Integer.MIN_VALUE) {
            return false;
        }
        PickaxeRune[] runes = PickaxeRune.values();
        for (int r = 0; r < runes.length; r++) {
            int cx = runeGridX + r % RUNES_PER_ROW * RUNE_CELL;
            int cy = runeGridY + r / RUNES_PER_ROW * RUNE_ROW;
            if (mouseX >= cx && mouseX < cx + RUNE_CELL && mouseY >= cy && mouseY < cy + RUNE_ROW) {
                if (runes[r] != pickaxeRune) {
                    pickaxeRune = runes[r];
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.3F, 0.6F));
                }
                return true;
            }
        }
        return false;
    }

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
            return cycle(BuiltInRegistries.ITEM.getTagOrEmpty(tag), now, phase);
        }
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
    }

    private static ItemStack reagent(Essence essence, long now) {
        // The separator always burns blaze powder, whatever essence is chosen on its screen.
        return new ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER);
    }

    private static ItemStack cycle(Iterable<Holder<Item>> holders, long now, int phase) {
        List<Holder<Item>> items = new ArrayList<>();
        holders.forEach(items::add);
        if (items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(items.get((int) ((now / 1000 + phase) % items.size())));
    }

    // ---- Input ----------------------------------------------------------------------------------------------------

    /** A click on the plate; returns true when it asks for the star map. */
    boolean plateClicked(double mouseX, double mouseY, RuneKnowledge knowledge) {
        Quest quest = selected;
        if (quest == null || pickRune(mouseX, mouseY) || !quest.leadsToStars()) {
            return false;
        }
        int ix = plateLeft() + PAD;
        int by = buttonTop(quest, knowledge);
        return mouseX >= ix && mouseX < ix + innerWidth() && mouseY >= by && mouseY < by + StarBook.BUTTON_HEIGHT;
    }

    void scrollPlate(double amount) {
        scroll -= (int) Math.round(amount * 12);
        if (scroll < 0) {
            scroll = 0;
        }
    }
}
