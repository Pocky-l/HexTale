package com.pockyl.hextale.client.screen.diary;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

/** Getting started: where the orbs come from, the table, how research works and the recipes of the tools. */
final class StartSection implements DiarySection {
    private static final String KEY = "diary.hextale.start.";

    @Override
    public Component title() {
        return Component.translatable(KEY + "title");
    }

    @Override
    public void icon(InkRenderer ink, float x, float y, int color) {
        ink.stroke(new float[]{x + 2, y - 6, x - 2, y, x + 2, y, x - 2, y + 6}, 1.4F, RuneDrawing.argb(color, 1));
    }

    @Override
    public int spreads(DiaryContext context) {
        return 5;
    }

    @Override
    public void render(DiaryContext context, int spread, int mouseX, int mouseY) {
        switch (spread) {
            case 0 -> {
                welcome(context, context.left());
                hextech(context, context.right());
            }
            case 1 -> {
                table(context, context.left());
                slots(context, context.right());
            }
            case 2 -> {
                research(context, context.left());
                stars(context, context.right());
            }
            case 3 -> {
                order(context, context.left());
                toolRecipes(context, context.right());
            }
            default -> {
                tools(context, context.left());
                materials(context, context.right());
            }
        }
    }

    private static void welcome(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "diary_title"));
        page.centered(Component.translatable(KEY + "owner", context.owner), DiaryContext.MUTED).space(4);
        page.text(Component.translatable(KEY + "intro"));
        page.text(Component.translatable(KEY + "intro2"), DiaryContext.MUTED);
    }

    private static void hextech(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "hextech.title"));
        int x = page.x + 18;
        int y = page.y + 2;
        long now = Util.getMillis();
        // Lightning strikes the rod standing on amethyst...
        InkRenderer ink = InkRenderer.begin(context.graphics);
        float flash = (now % 2600) < 160 ? 1 : 0.35F;
        float[] bolt = {x + 14, y - 1, x + 8, y + 6, x + 12, y + 7, x + 7, y + 13};
        ink.glow(bolt, 5, RuneDrawing.argb(0x7EF0FF, 0.6F * flash));
        ink.stroke(bolt, 1.2F, RuneDrawing.argb(0xFFFFFF, Math.min(1, flash + 0.3F)));
        ink.end();
        context.item(new ItemStack(Items.LIGHTNING_ROD), x, y + 10);
        context.item(new ItemStack(Items.AMETHYST_BLOCK), x, y + 26);
        // ...and the amethyst shatters into orbs.
        ink = InkRenderer.begin(context.graphics);
        ink.stroke(new float[]{x + 24, y + 26, x + 44, y + 26}, 1.2F, RuneDrawing.argb(DiaryContext.HEADING, 0.8F));
        ink.stroke(new float[]{x + 40, y + 22, x + 44, y + 26, x + 40, y + 30}, 1.2F, RuneDrawing.argb(DiaryContext.HEADING, 0.8F));
        ink.end();
        for (int i = 0; i < 3; i++) {
            float bob = Mth.sin(now / 400.0F + i * 2.1F) * 1.5F;
            context.item(new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), x + 50 + i * 17, (int) (y + 18 + bob));
        }
        context.graphics.drawString(context.font, Component.literal("1-3"), x + 70, y + 36, 0xFF000000 | DiaryContext.MUTED, false);
        page.space(48);
        page.text(Component.translatable(KEY + "hextech.text"));
        page.text(Component.translatable(KEY + "hextech.text2"), DiaryContext.MUTED);
    }

    private static void table(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "table.title"));
        ItemLike brass = ModItems.BRASS_INGOT.get();
        ItemLike planks = Items.OAK_PLANKS;
        grid(context, page.x + 14, page.y + 2, new ItemLike[]{brass, brass, brass, planks, Items.BOOKSHELF, planks, planks, null, planks},
                new ItemStack(ModItems.RESEARCH_TABLE.get()));
        page.space(60);
        page.text(Component.translatable(KEY + "table.text"));
    }

    private static void slots(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "slots.title"));
        ItemLike[] items = {ModItems.RESEARCHERS_DIARY.get(), Items.PAPER, ModItems.UNSTABLE_HEXTECH.get(), ModItems.HEXTECH_INK.get(),
                ModItems.RUNE_BRUSH.get()};
        String[] keys = {"diary", "paper", "hextech", "ink", "brush"};
        for (int i = 0; i < items.length; i++) {
            context.slot(new ItemStack(items[i]), page.x + 1, page.y);
            Page line = new Page(context, page.x + 22, page.y - 1, page.width - 22, page.bottom);
            line.text(Component.translatable(KEY + "slots." + keys[i]));
            page.y = Math.max(page.y + 20, line.y);
        }
        page.text(Component.translatable(KEY + "slots.output"), DiaryContext.MUTED);
    }

    private static void research(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "research.title"));
        page.text(Component.translatable(KEY + "research.text"));
        page.text(Component.translatable(KEY + "research.points"));
        page.text(Component.translatable(KEY + "research.walls"), DiaryContext.MUTED);
    }

    private static void stars(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "stars.title"));
        page.text(Component.translatable(KEY + "stars.text"));
        page.text(Component.translatable(KEY + "stars.tools"));
        page.text(Component.translatable(KEY + "stars.sheet"), DiaryContext.MUTED);
    }

    private static void order(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "order.title"));
        page.text(Component.translatable(KEY + "order.text"));
        example(context, page, ComplexRunes.of(SimpleRune.FIRE, SimpleRune.EARTH));
        example(context, page, ComplexRunes.of(SimpleRune.EARTH, SimpleRune.FIRE));
    }

    private static void toolRecipes(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "research_tools.title"));
        page.text(Component.translatable(KEY + "research_tools.text"), DiaryContext.MUTED);
        ItemStack scroll = RuneScrollItem.of(ComplexRunes.byKey("water_light"), 1, ModItems.RUNE_SCROLL.get());
        int y = page.y;
        context.slot(scroll, page.x + 2, y);
        context.slot(new ItemStack(Items.GLASS_PANE), page.x + 20, y);
        context.slot(new ItemStack(ModItems.BRASS_INGOT.get()), page.x + 38, y);
        resultArrow(context, page.x + 58, y);
        context.slot(new ItemStack(ModItems.RESEARCHERS_LENS.get()), page.x + 82, y);
        page.space(22);
        page.text(Component.translatable(KEY + "research_tools.others"), DiaryContext.MUTED);
    }

    /** One line "radical -> radical = hieroglyph Name". */
    private static void example(DiaryContext context, Page page, ComplexRune rune) {
        int y = page.y + 9;
        float x = page.x + 10;
        InkRenderer ink = InkRenderer.begin(context.graphics);
        RuneDrawing.radical(ink, rune.action(), x, y, 15, RuneDrawing.Style.INK, 1);
        ink.stroke(new float[]{x + 11, y, x + 19, y, x + 16, y - 2.5F, x + 19, y, x + 16, y + 2.5F}, 0.9F,
                RuneDrawing.argb(DiaryContext.MUTED, 1));
        RuneDrawing.radical(ink, rune.target(), x + 30, y, 15, RuneDrawing.Style.INK, 1);
        ink.stroke(new float[]{x + 40, y - 1.5F, x + 46, y - 1.5F}, 0.9F, RuneDrawing.argb(DiaryContext.MUTED, 1));
        ink.stroke(new float[]{x + 40, y + 1.5F, x + 46, y + 1.5F}, 0.9F, RuneDrawing.argb(DiaryContext.MUTED, 1));
        RuneDrawing.glyph(ink, rune, x + 58, y, 18, RuneDrawing.Style.INK, 1);
        ink.end();
        context.graphics.drawString(context.font, Component.translatable(rune.translationKey()), (int) x + 72, y - 4,
                0xFF000000 | RuneDrawing.darken(rune.color(), 0.5F), false);
        page.y += 22;
    }

    private static void tools(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "tools.title"));
        row(context, page, Component.translatable(KEY + "tools.ink"),
                new ItemLike[]{Items.INK_SAC, Items.GLOWSTONE_DUST, Items.GLASS_BOTTLE}, new ItemStack(ModItems.HEXTECH_INK.get()));
        page.text(Component.translatable(KEY + "tools.brush"), DiaryContext.MUTED);
        int y = page.y;
        context.slot(new ItemStack(Items.FEATHER), page.x + 30, y);
        context.slot(new ItemStack(ModItems.BRASS_NUGGET.get()), page.x + 30, y + 18);
        context.slot(new ItemStack(Items.STICK), page.x + 30, y + 36);
        resultArrow(context, page.x + 54, y + 18);
        context.slot(new ItemStack(ModItems.RUNE_BRUSH.get()), page.x + 80, y + 18);
        page.space(58);
    }

    private static void materials(DiaryContext context, Page page) {
        page.heading(Component.translatable(KEY + "materials.title"));
        row(context, page, Component.translatable(KEY + "materials.brass"),
                new ItemLike[]{Items.COPPER_INGOT, Items.COPPER_INGOT, Items.COPPER_INGOT, Items.GOLD_INGOT},
                new ItemStack(ModItems.BRASS_INGOT.get(), 2));
        row(context, page, Component.translatable(KEY + "materials.diary"),
                new ItemLike[]{Items.BOOK, ModItems.BRASS_INGOT.get(), ModItems.UNSTABLE_HEXTECH.get()},
                new ItemStack(ModItems.RESEARCHERS_DIARY.get()));
        page.text(Component.translatable(KEY + "materials.nuggets"), DiaryContext.MUTED);
    }

    /** A shapeless recipe as a row of slots, an arrow and the result. */
    private static void row(DiaryContext context, Page page, Component caption, ItemLike[] inputs, ItemStack result) {
        page.text(caption, DiaryContext.MUTED);
        int x = page.x + 2;
        for (ItemLike input : inputs) {
            context.slot(new ItemStack(input), x, page.y);
            x += 18;
        }
        resultArrow(context, x + 2, page.y);
        context.slot(result, x + 26, page.y);
        page.space(24);
    }

    private static void grid(DiaryContext context, int x, int y, ItemLike[] items, ItemStack result) {
        for (int i = 0; i < 9; i++) {
            ItemLike item = items[i];
            context.slot(item == null ? null : new ItemStack(item), x + i % 3 * 18, y + i / 3 * 18);
        }
        resultArrow(context, x + 58, y + 18);
        context.slot(result, x + 84, y + 18);
    }

    private static void resultArrow(DiaryContext context, float x, float y) {
        InkRenderer ink = InkRenderer.begin(context.graphics);
        int color = RuneDrawing.argb(DiaryContext.HEADING, 0.85F);
        ink.stroke(new float[]{x, y + 8, x + 16, y + 8}, 1.3F, color);
        ink.stroke(new float[]{x + 12, y + 4, x + 16, y + 8, x + 12, y + 12}, 1.3F, color);
        ink.end();
    }
}
