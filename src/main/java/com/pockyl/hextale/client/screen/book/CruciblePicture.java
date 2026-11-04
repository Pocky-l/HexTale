package com.pockyl.hextale.client.screen.book;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.crafting.BrewRecipe;
import com.pockyl.hextale.crafting.CrucibleRecipe;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.quest.QuestDisplay;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A Crucible recipe on a diary plate, drawn as what happens at the pot instead of "this plus that": the cast-iron pot
 * over its fire with the essences its water must hold (sign, name, mB) as rows in the water; over the pot the catalyst,
 * falling in; right of it the result (for a brew the vials, with the bottle that scoops them). Where the essences come
 * from is the player's own study - the picture names only the essences.
 */
final class CruciblePicture {
    private static final int SLOT = 18;
    private static final int ROW = 18;
    private static final int POT_WIDTH = 92;
    private static final int WALL = 3;
    /** From the picture's top: the catalyst (18), its fall into the water (10). */
    private static final int POT_TOP = 28;
    /** Room under the pot: legs and fire (10), the heat line (18). */
    private static final int BELOW = 28;
    private static final int RESULT_GAP = 26;
    private static final int MUTED = 0xFF9A90B8;
    private static final int IRON_DARK = 0x1F1C24;
    private static final int IRON = 0x3E3946;
    private static final int IRON_LIGHT = 0x67606F;

    private CruciblePicture() {
    }

    /** What a crucible picture shows, taken from the recipe the server sent. */
    private record Pot(Ingredient catalyst, EssenceMix essences, ItemStack result, int heat, boolean brew) {
        @Nullable
        static Pot of(QuestDisplay display) {
            Identifier id = Identifier.tryParse(display.result());
            if (display.kind() == QuestDisplay.Kind.BREW) {
                BrewRecipe recipe = BrewRecipe.ALL.named(id, true);
                return recipe == null ? null
                        : new Pot(recipe.catalyst(), recipe.essences(), ChemVialItem.of(recipe.brew(), recipe.doses()), recipe.heat(), true);
            }
            CrucibleRecipe recipe = CrucibleRecipe.ALL.named(id, true);
            return recipe == null ? null : new Pot(recipe.catalyst(), recipe.essences(), recipe.result().create(), 1, false);
        }

        List<Map.Entry<Essence, Integer>> rows() {
            return new ArrayList<>(essences.asMap().entrySet());
        }
    }

    static int height(QuestDisplay display) {
        Pot pot = Pot.of(display);
        int rows = pot == null ? 1 : Math.max(1, pot.essences().asMap().size());
        return POT_TOP + potHeight(rows) + BELOW;
    }

    private static int potHeight(int rows) {
        return WALL + rows * ROW + 2 + WALL;
    }

    @Nullable
    static List<FormattedCharSequence> render(GuidePage page, Font font, GuiGraphicsExtractor graphics, QuestDisplay display, int x, int y,
                                              int width, int mouseX, int mouseY, long now) {
        Pot pot = Pot.of(display);
        if (pot == null) {
            PixelArt.flush(graphics);
            graphics.centeredText(font, Component.translatable("book.hextale.quest.infusion.missing"), x + width / 2, y + 30, MUTED);
            return null;
        }
        List<FormattedCharSequence> tooltip = null;
        List<Map.Entry<Essence, Integer>> rows = pot.rows();
        int count = Math.max(1, rows.size());
        int total = POT_WIDTH + RESULT_GAP + SLOT;
        int potX = x + (width - total) / 2;
        int potY = y + POT_TOP;
        int waterTop = potY + WALL;
        int waterBottom = waterTop + count * ROW + 2;
        int potBottom = waterBottom + WALL;
        int potMid = potX + POT_WIDTH / 2;

        // The pot: water first, the rows of essence on it, then the iron round it.
        water(graphics, pot, potX + WALL, waterTop, potX + POT_WIDTH - WALL, waterBottom, now);
        fire(graphics, potX, potBottom, pot.heat(), now);
        ironPot(graphics, potX, potY, potBottom);

        // The essences the water must hold, one row each.
        for (int i = 0; i < rows.size(); i++) {
            int rowY = waterTop + 1 + i * ROW;
            tooltip = orTooltip(tooltip, essenceRow(font, graphics, rows.get(i).getKey(), rows.get(i).getValue(), potX + WALL + 3, rowY,
                    POT_WIDTH - WALL * 2 - 4, mouseX, mouseY));
        }

        // The catalyst, falling into the boiling water last.
        int catalystX = potMid - SLOT / 2;
        tooltip = orTooltip(tooltip, page.slot(graphics, cycle(pot.catalyst(), now), catalystX, y, mouseX, mouseY,
                Component.translatable("book.hextale.quest.crucible.catalyst").withColor(0xE8C36A)));
        fall(graphics, potMid, y + SLOT + 1, waterTop - 1, now);

        // What rises: the arrow out of the water, the result; a brew is scooped with bottles.
        int midY = waterTop + (count * ROW + 2) / 2;
        int arrowX = potX + POT_WIDTH + 4;
        GuidePage.arrow(graphics, arrowX, midY);
        int resultX = arrowX + RESULT_GAP - 4;
        Component resultNote = pot.brew() ? Component.translatable("book.hextale.quest.crucible.bottles", pot.result().getCount()).withColor(0x9FE6FF) : null;
        tooltip = orTooltip(tooltip, page.slot(graphics, pot.result(), resultX, midY - SLOT / 2, mouseX, mouseY, resultNote));
        if (pot.brew()) {
            int bottleY = midY - SLOT / 2 - SLOT - 4;
            tooltip = orTooltip(tooltip, page.slot(graphics, new ItemStack(Items.GLASS_BOTTLE), arrowX, bottleY, mouseX, mouseY, resultNote));
        }

        // The heat it boils over: the icon of the plainest heater says it; words only for "or hotter" (their full
        // sentence in the tooltip).
        int heatY = potBottom + 10;
        ItemStack heater = heater(pot.heat());
        boolean orHotter = pot.heat() > 1 && pot.heat() < BrewRecipe.BURNER_HEAT;
        Component heat = Component.translatable(orHotter ? "book.hextale.quest.crucible.heat_or_hotter" : "book.hextale.quest.crucible.heat",
                Component.translatable("block.hextale.crucible.heat." + pot.heat()));
        Component words = Component.translatable("book.hextale.quest.crucible.or_hotter");
        int lineWidth = orHotter ? 18 + font.width(words) : 16;
        int lineX = potMid - lineWidth / 2;
        PixelArt.flush(graphics);
        graphics.item(heater, lineX, heatY);
        if (orHotter) {
            graphics.text(font, words, lineX + 18, heatY + 4, heatColor(pot.heat()), true);
        }
        if (GuidePage.over(mouseX, mouseY, lineX, heatY, lineWidth, 16)) {
            tooltip = page.pointAt(heater, heat);
        }
        return tooltip;
    }

    // ---- The pot -----------------------------------------------------------------------------------------------------

    /** One essence in the water: its sign, its name when it fits, and the mB the water must hold. */
    @Nullable
    private static List<FormattedCharSequence> essenceRow(Font font, GuiGraphicsExtractor graphics, Essence essence, int amount, int x, int y,
                                                          int width, int mouseX, int mouseY) {
        int sign = x + 1;
        PixelArt.fill(graphics, x, y + 2, x + width, y + SLOT - 2, PixelArt.argb(0x08070E, 0.45F));
        PixelArt.essence(graphics, essence, sign, y + 4, 10, PixelArt.argb(essence.color(), 1));
        Component name = Component.translatable(essence.translationKey());
        Component full = Component.translatable("book.hextale.quest.crucible.amount", name, amount);
        Component text = font.width(full) <= width - 14 ? full : Component.translatable("book.hextale.quest.crucible.amount_short", amount);
        PixelArt.flush(graphics);
        graphics.text(font, text, x + 14, y + 5, 0xFF000000 | essence.color(), true);
        if (!GuidePage.over(mouseX, mouseY, x, y, width, SLOT)) {
            return null;
        }
        return List.of(Component.translatable("book.hextale.quest.crucible.essence", name, amount).withColor(essence.color()).getVisualOrderText());
    }

    /** The water tinted by what it holds, its bright surface and bubbles rising through it. */
    private static void water(GuiGraphicsExtractor graphics, Pot pot, int x0, int y0, int x1, int y1, long now) {
        int r = 0;
        int g = 0;
        int b = 0;
        int weight = 0;
        for (Map.Entry<Essence, Integer> entry : pot.essences().asMap().entrySet()) {
            int color = entry.getKey().color();
            r += (color >> 16 & 0xFF) * entry.getValue();
            g += (color >> 8 & 0xFF) * entry.getValue();
            b += (color & 0xFF) * entry.getValue();
            weight += entry.getValue();
        }
        int tint = weight == 0 ? 0x3A78B0 : (r / weight) << 16 | (g / weight) << 8 | b / weight;
        int deep = PixelArt.mix(tint, 0x10142A, 0.72F);
        PixelArt.fill(graphics, x0, y0, x1, y1, PixelArt.argb(deep, 1));
        PixelArt.fill(graphics, x0, y0, x1, y0 + 1, PixelArt.argb(PixelArt.mix(tint, 0xFFFFFF, 0.25F), 0.9F));
        PixelArt.fill(graphics, x0, y0 + 1, x1, y0 + 2, PixelArt.argb(tint, 0.45F));
        int height = y1 - y0 - 3;
        int bubbleColor = PixelArt.argb(PixelArt.mix(tint, 0xFFFFFF, 0.5F), 0.55F);
        for (int i = 0; i < 7; i++) {
            int period = 1300 + i * 170;
            float t = (float) ((now + i * 431L) % period) / period;
            int bx = x0 + 4 + (i * 37) % Math.max(1, x1 - x0 - 8);
            int by = y1 - 2 - (int) (t * height);
            int size = i % 3 == 0 ? 2 : 1;
            PixelArt.fill(graphics, bx, by, bx + size, by + size, bubbleColor);
        }
    }

    /** The cast-iron pot round the water: a rolled rim, thick walls, rounded bottom corners and two short legs. */
    private static void ironPot(GuiGraphicsExtractor graphics, int x, int top, int bottom) {
        int right = x + POT_WIDTH;
        int dark = PixelArt.argb(IRON_DARK, 1);
        int iron = PixelArt.argb(IRON, 1);
        int light = PixelArt.argb(IRON_LIGHT, 1);
        // Walls.
        PixelArt.fill(graphics, x, top + 2, x + WALL, bottom - 2, iron);
        PixelArt.fill(graphics, right - WALL, top + 2, right, bottom - 2, iron);
        PixelArt.fill(graphics, x, top + 2, x + 1, bottom - 2, light);
        PixelArt.fill(graphics, right - 1, top + 2, right, bottom - 2, dark);
        // Bottom with rounded corners.
        PixelArt.fill(graphics, x + 2, bottom - WALL, right - 2, bottom, iron);
        PixelArt.fill(graphics, x + 1, bottom - 2, x + 2, bottom - 1, iron);
        PixelArt.fill(graphics, right - 2, bottom - 2, right - 1, bottom - 1, iron);
        PixelArt.fill(graphics, x + 2, bottom - 1, right - 2, bottom, dark);
        // Rolled rim, wider than the walls.
        PixelArt.fill(graphics, x - 2, top, right + 2, top + 3, iron);
        PixelArt.fill(graphics, x - 2, top, right + 2, top + 1, light);
        PixelArt.fill(graphics, x - 2, top + 2, right + 2, top + 3, dark);
        // Legs.
        PixelArt.fill(graphics, x + 4, bottom, x + 8, bottom + 3, iron);
        PixelArt.fill(graphics, right - 8, bottom, right - 4, bottom + 3, iron);
        PixelArt.fill(graphics, x + 4, bottom + 2, x + 8, bottom + 3, dark);
        PixelArt.fill(graphics, right - 8, bottom + 2, right - 4, bottom + 3, dark);
    }

    /** Flickering tongues of the heat's colour between the legs. */
    private static void fire(GuiGraphicsExtractor graphics, int x, int potBottom, int heat, long now) {
        int outer = heatColor(heat) & 0xFFFFFF;
        int inner = PixelArt.mix(outer, 0xFFFFE0, 0.55F);
        int base = potBottom + 9;
        for (int fx = x + 11; fx < x + POT_WIDTH - 11; fx += 4) {
            int flicker = (int) ((now / 110 + fx * 7L) % 4);
            int tall = 4 + (fx * 13 % 3) + (flicker == 0 ? 1 : 0) - (flicker == 3 ? 1 : 0);
            PixelArt.fill(graphics, fx, base - tall, fx + 3, base, PixelArt.argb(outer, 0.85F));
            PixelArt.fill(graphics, fx + 1, base - tall + 2, fx + 2, base, PixelArt.argb(inner, 0.9F));
        }
    }

    private static int heatColor(int heat) {
        return switch (heat) {
            case 3 -> 0xFF5CE0F0;
            case 4 -> 0xFFFF7A2A;
            case 5 -> 0xFF9CF06A;
            default -> 0xFFFFA040;
        };
    }

    /** The plainest thing that heats the pot enough. */
    private static ItemStack heater(int heat) {
        return switch (heat) {
            case 3 -> new ItemStack(Items.SOUL_CAMPFIRE);
            case 4 -> new ItemStack(Items.LAVA_BUCKET);
            case 5 -> new ItemStack(ModItems.CHEM_BURNER.get());
            default -> new ItemStack(Items.CAMPFIRE);
        };
    }

    // ---- Small drawing helpers ---------------------------------------------------------------------------------------

    /** The catalyst's fall: a dashed line running down into the water, with a head. */
    private static void fall(GuiGraphicsExtractor graphics, int cx, int y0, int y1, long now) {
        int color = PixelArt.argb(0xE8C36A, 0.9F);
        int phase = (int) (6 - now / 90 % 6);
        PixelArt.dotted(graphics, cx - 1, y0, cx - 1, y1 - 4, 3, 3, phase, color);
        PixelArt.dotted(graphics, cx, y0, cx, y1 - 4, 3, 3, phase, color);
        for (int i = 0; i < 3; i++) {
            PixelArt.fill(graphics, cx - 3 + i, y1 - 3 + i, cx + 3 - i, y1 - 2 + i, color);
        }
    }

    private static ItemStack cycle(Ingredient ingredient, long now) {
        List<Holder<Item>> items = ingredient.items().toList();
        return items.isEmpty() ? ItemStack.EMPTY : new ItemStack(items.get((int) (now / 1000 % items.size())));
    }

    @Nullable
    private static List<FormattedCharSequence> orTooltip(@Nullable List<FormattedCharSequence> current, @Nullable List<FormattedCharSequence> next) {
        return next != null ? next : current;
    }
}
