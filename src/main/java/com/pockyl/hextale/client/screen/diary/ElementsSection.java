package com.pockyl.hextale.client.screen.diary;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.RuneCategory;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The nine elements (simple runes), one to a page: the radical drawn large, the element's corner of the research
 * parchment, what it means as an action and as a target, and the tried items that carry it. A last page shows how two
 * elements make a complex rune.
 */
final class ElementsSection implements DiarySection {
    private static final String KEY = "diary.hextale.simple.";
    private static final int BOX = 54;

    @Override
    public Component title() {
        return Component.translatable(KEY + "title");
    }

    @Override
    public void icon(InkRenderer ink, float x, float y, int color) {
        RuneDrawing.strokes(ink, SimpleRune.FIRE.radical(), x, y, 12, color, RuneDrawing.Style.FAINT, 3.3F);
    }

    @Override
    public int spreads(DiaryContext context) {
        return (SimpleRune.values().length + 2) / 2;
    }

    @Override
    public void render(DiaryContext context, int spread, int mouseX, int mouseY) {
        renderPage(context, context.left(), spread * 2);
        renderPage(context, context.right(), spread * 2 + 1);
    }

    private static void renderPage(DiaryContext context, Page page, int index) {
        SimpleRune[] runes = SimpleRune.values();
        if (index < runes.length) {
            entry(context, page, runes[index]);
        } else if (index == runes.length) {
            combining(context, page);
        }
    }

    private static void entry(DiaryContext context, Page page, SimpleRune rune) {
        Component name = Component.translatable(rune.translationKey()).withStyle(ChatFormatting.BOLD);
        context.graphics.drawString(context.font, name, page.x + (page.width - context.font.width(name)) / 2, page.y,
                0xFF000000 | PixelArt.onParchment(rune.color()), false);
        page.y += 12;
        int boxX = page.x + 14;
        int boxY = page.y;
        InkRenderer ink = InkRenderer.begin(context.graphics);
        practiceGrid(ink, boxX, boxY, BOX);
        ink.end();
        PixelArt.radical(context.graphics, rune, boxX + 7, boxY + 7, BOX - 14, 0xFF000000 | PixelArt.onParchment(rune.color()), 0x30000000);
        corner(context, rune, page.x + page.width - 30, boxY + BOX / 2);
        page.y += BOX + 4;
        page.text(Component.translatable(KEY + "as_action").withStyle(ChatFormatting.BOLD)
                .append(" ").append(Component.translatable(rune.translationKey() + ".action").withStyle(style -> style.withBold(false))));
        page.y -= 2;
        page.text(Component.translatable(KEY + "as_target").withStyle(ChatFormatting.BOLD)
                .append(" ").append(Component.translatable(rune.translationKey() + ".target").withStyle(style -> style.withBold(false))));
        List<Item> carriers = new ArrayList<>();
        for (String id : context.knowledge.items()) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            Item item = key == null ? null : BuiltInRegistries.ITEM.get(key);
            ItemElements elements = item == null ? null : ResearchItems.of(item);
            if (elements != null && elements.elements().containsKey(rune)) {
                carriers.add(item);
            }
        }
        page.text(Component.translatable(carriers.isEmpty() ? KEY + "carriers.none" : KEY + "carriers"), DiaryContext.MUTED);
        int x = page.x;
        for (Item item : carriers) {
            if (x + 16 > page.x + page.width || page.y + 16 > page.bottom) {
                break;
            }
            context.item(new ItemStack(item), x, page.y - 2);
            x += 17;
        }
    }

    /** The parchment's nine corners as dots, this element's one lit: items carrying it pull symbols that way. */
    private static void corner(DiaryContext context, SimpleRune rune, int cx, int cy) {
        for (int i = 0; i < HexGrid.RING.length; i++) {
            double[] u = HexGrid.unit(HexGrid.RING[i]);
            int x = cx + (int) Math.round(u[0] * 18);
            int y = cy + (int) Math.round(u[1] * 18);
            double[] v = HexGrid.unit(HexGrid.RING[(i + 1) % HexGrid.RING.length]);
            PixelArt.line(context.graphics, x, y, cx + (int) Math.round(v[0] * 18), cy + (int) Math.round(v[1] * 18), 0x667A6648);
        }
        for (SimpleRune element : HexGrid.RING) {
            double[] u = HexGrid.unit(element);
            int x = cx + (int) Math.round(u[0] * 18);
            int y = cy + (int) Math.round(u[1] * 18);
            boolean self = element == rune;
            context.graphics.fill(x - (self ? 2 : 1), y - (self ? 2 : 1), x + (self ? 3 : 2), y + (self ? 3 : 2),
                    0xFF000000 | (self ? PixelArt.onParchment(rune.color()) : 0xB09C78));
        }
        double[] u = HexGrid.unit(rune);
        PixelArt.line(context.graphics, cx, cy, cx + (int) Math.round(u[0] * 13), cy + (int) Math.round(u[1] * 13),
                0xFF000000 | PixelArt.onParchment(rune.color()));
        context.graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF2B8FB8);
        if (context.hovered(cx - 20, cy - 20, 40, 40)) {
            context.tooltip(Component.translatable(KEY + "corner", Component.translatable(rune.translationKey())));
        }
    }

    /** A square with its centre lines and diagonals, as on calligraphy practice paper. */
    private static void practiceGrid(InkRenderer ink, float x, float y, float size) {
        int frame = RuneDrawing.argb(0x8E2A1E, 0.45F);
        int guide = RuneDrawing.argb(0x8E2A1E, 0.16F);
        ink.rect(x, y, x + size, y + size, RuneDrawing.argb(0xFFF6E0, 0.35F));
        ink.stroke(new float[]{x, y, x + size, y, x + size, y + size, x, y + size, x, y}, 0.9F, frame);
        dashed(ink, x + size / 2, y, x + size / 2, y + size, guide);
        dashed(ink, x, y + size / 2, x + size, y + size / 2, guide);
        dashed(ink, x, y, x + size, y + size, guide);
        dashed(ink, x + size, y, x, y + size, guide);
    }

    private static void dashed(InkRenderer ink, float x0, float y0, float x1, float y1, int color) {
        int dashes = 9;
        for (int i = 0; i < dashes; i++) {
            float a = i / (float) dashes;
            float b = (i + 0.55F) / dashes;
            ink.stroke(new float[]{x0 + (x1 - x0) * a, y0 + (y1 - y0) * a, x0 + (x1 - x0) * b, y0 + (y1 - y0) * b}, 0.6F, color);
        }
    }

    /** How simple runes become complex ones, with one example hieroglyph per structure. */
    private static void combining(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "combining.title"));
        page.text(Component.translatable(KEY + "combining.text"));
        RuneCategory.Layout[] layouts = RuneCategory.Layout.values();
        SimpleRune[][] examples = {{SimpleRune.FIRE, SimpleRune.EARTH}, {SimpleRune.LIFE, SimpleRune.METAL},
                {SimpleRune.WATER, SimpleRune.FIRE}, {SimpleRune.LIGHT, SimpleRune.EARTH}};
        int cell = page.width / layouts.length;
        int y = page.y + 11;
        for (int i = 0; i < layouts.length; i++) {
            ComplexRune rune = ComplexRunes.of(examples[i][0], examples[i][1]);
            PixelArt.glyph(context.graphics, rune, page.x + cell * i + cell / 2 - 10, y - 10, 20,
                    0xFF000000 | PixelArt.onParchment(rune.color()));
        }
        for (int i = 0; i < layouts.length; i++) {
            Component label = Component.translatable(KEY + "layout." + layouts[i].name().toLowerCase(Locale.ROOT));
            context.graphics.pose().pushPose();
            context.graphics.pose().translate(page.x + cell * i + cell / 2.0F, y + 14, 0);
            context.graphics.pose().scale(0.75F, 0.75F, 1);
            context.graphics.drawString(context.font, label, -context.font.width(label) / 2, 0, 0xFF000000 | DiaryContext.MUTED, false);
            context.graphics.pose().popPose();
        }
        page.y = y + 25;
        page.text(Component.translatable(KEY + "combining.categories"), DiaryContext.MUTED);
    }
}
