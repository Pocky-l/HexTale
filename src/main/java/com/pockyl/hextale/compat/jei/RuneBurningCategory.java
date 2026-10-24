package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.runework.PickaxeEffects;
import com.pockyl.hextale.runework.PickaxeRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;

import java.util.List;
import java.util.Map;

/**
 * Burning a rune into a hextech pickaxe or a hex pylon at the altar: it lies alone on the altar, the rune on a stele, no pedestal
 * items - only the rune's essence. Shows what the rune does, how much at one, two and three stars, what each star costs
 * and the runes it is incompatible with.
 */
public class RuneBurningCategory extends AbstractRecipeCategory<RuneBurningCategory.Recipe> {
    public static final RecipeType<Recipe> TYPE = RecipeType.create(Hextale.MOD_ID, "rune_burning", Recipe.class);

    /** One rune burned into a pickaxe, a Hex Smelter or a hex pylon. */
    public record Recipe(RuneHolder holder, ComplexRune rune) {
        String descriptionKey() {
            return holder.descriptionKey(rune);
        }

        String valueKey() {
            return holder.valueKey(rune);
        }

        String amount(int stars) {
            return holder.amount(rune, stars);
        }

        static List<Recipe> all() {
            List<Recipe> out = new java.util.ArrayList<>();
            for (RuneHolder holder : RuneHolder.values()) {
                for (ComplexRune rune : holder.takes()) {
                    out.add(new Recipe(holder, rune));
                }
            }
            return out;
        }
    }

    private static final int WIDTH = 168;
    private static final int HEIGHT = 124;
    private static final int GOLD = 0xF0C24A;
    private static final int INK = 0xFF3C3850;
    private static final int MUTED = 0xFF6A6480;

    public RuneBurningCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.hextale.rune_burning"), guiHelper.createDrawableItemLike(ModItems.RUNE_STELE.get()), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        List<ItemStack> before = recipe.holder().items();
        List<ItemStack> after = before.stream().map(stack -> {
            ItemStack out = stack.copy();
            recipe.holder().set(out, PickaxeRunes.EMPTY.with(recipe.rune(), 1));
            return out;
        }).toList();
        builder.addInputSlot(4, 4).setStandardSlotBackground().addItemStacks(before);
        builder.addSlot(RecipeIngredientRole.CATALYST, 40, 4).addIngredientsUnsafe(com.pockyl.hextale.port.PortJei.stacks(new ItemStack(ModItems.RUNE_STELE.get())));
        builder.addOutputSlot(98, 4).setOutputSlotBackground().addItemStacks(after);
        builder.addSlot(RecipeIngredientRole.CATALYST, WIDTH - 20, 4).addIngredientsUnsafe(com.pockyl.hextale.port.PortJei.stacks(new ItemStack(ModItems.HEXTECH_ALTAR.get())));
    }

    @Override
    public void draw(Recipe rune, IRecipeSlotsView slots, net.minecraft.client.gui.GuiGraphics portGraphics, double mouseX, double mouseY) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        Font font = Minecraft.getInstance().font;
        ComplexRune complex = rune.rune();
        // pickaxe + (rune over its stele) -> pickaxe with the rune
        plus(graphics, 29, 13);
        PixelArt.glyph(graphics, complex, 60, 5, 14, PixelArt.argb(complex.color(), 1), 0xFF000000);
        PixelArt.fill(graphics, 78, 12, 90, 14, PixelArt.argb(GOLD, 1));
        for (int i = 0; i < 4; i++) {
            PixelArt.fill(graphics, 90 + i, 9 + i, 91 + i, 17 - i, PixelArt.argb(GOLD, 1));
        }
        PixelArt.flush(graphics);

        int y = 28;
        graphics.text(font, Component.translatable(complex.translationKey()), 4, y, 0xFF000000 | complex.color(), true);
        Component chapter = Component.translatable("book.hextale.quest.pickaxe_rune.chapter", "I".repeat(ResearchBook.chapter(complex)));
        graphics.text(font, chapter, WIDTH - 4 - font.width(chapter), y, MUTED, false);
        y += 12;
        for (FormattedCharSequence line : font.split(Component.translatable(rune.descriptionKey()), WIDTH - 8)) {
            graphics.text(font, line, 4, y, INK, false);
            y += 10;
        }
        y = Math.max(y, 60) + 2;

        // One, two and three stars: how strong, and what it costs.
        // A rune of a pair without a compound costs both its primals.
        Map<Essence, Integer> cost = rune.holder().cost(complex, 1).asMap();
        int column = (WIDTH - 8) / 3;
        PixelArt.fill(graphics, 4, y - 2, WIDTH - 4, y + 31, PixelArt.argb(0x8B8B8B, 0.35F));
        PixelArt.flush(graphics);
        for (int stars = 1; stars <= 3; stars++) {
            int cx = 4 + (stars - 1) * column + column / 2;
            graphics.centeredText(font, Component.literal("★".repeat(stars)), cx, y, 0xFF000000 | GOLD);
            Component value = Component.translatable(rune.valueKey(), rune.amount(stars));
            graphics.text(font, value, cx - font.width(value) / 2, y + 10, 0xFF000000, false);
            int width = 0;
            for (int amount : cost.values()) {
                width += PixelArt.ICON + 2 + font.width(amountText(amount * stars, cost.size())) + 4;
            }
            int ex = cx - (width - 4) / 2;
            for (Map.Entry<Essence, Integer> entry : cost.entrySet()) {
                String mb = amountText(entry.getValue() * stars, cost.size());
                PixelArt.essence(graphics, entry.getKey(), ex, y + 20, PixelArt.ICON, PixelArt.argb(entry.getKey().color(), 1));
                PixelArt.flush(graphics);
                graphics.text(font, mb, ex + PixelArt.ICON + 2, y + 21, INK, false);
                ex += PixelArt.ICON + 2 + font.width(mb) + 4;
            }
        }
        y += 35;

        StringBuilder clashes = new StringBuilder();
        for (PickaxeRune other : PickaxeRune.values()) {
            if (rune.holder().clashes(complex, other.rune()) && other.rune() != complex) {
                clashes.append(clashes.isEmpty() ? "" : ", ").append(Component.translatable(other.rune().translationKey()).getString());
            }
        }
        if (!clashes.isEmpty()) {
            for (FormattedCharSequence line : font.split(Component.translatable("jei.hextale.rune_burning.clash", clashes.toString()), WIDTH - 8)) {
                graphics.text(font, line, 4, y, 0xFFB03A2E, false);
                y += 10;
            }
        }
        String note = rune.holder().jeiNoteKey();
        for (FormattedCharSequence line : font.split(Component.translatable(note, rune.holder().slots()), WIDTH - 8)) {
            graphics.text(font, line, 4, y, MUTED, false);
            y += 10;
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, Recipe rune, IRecipeSlotsView slots, double mouseX, double mouseY) {
        // The essence icons name their essence.
        if (mouseY >= 0 && mouseX >= 0) {
            int top = costTop(rune);
            if (mouseY >= top && mouseY < top + 10 && mouseX >= 4 && mouseX < WIDTH - 4) {
                for (Essence essence : rune.holder().cost(rune.rune(), 1).asMap().keySet()) {
                    tooltip.add(Component.translatable(essence.translationKey()).withColor(essence.color()));
                }
            }
        }
    }

    /** One essence's amount: "16 mB" alone, a bare number beside another essence (the column is narrow). */
    private static String amountText(int mb, int essences) {
        return essences > 1 ? Integer.toString(mb) : mb + " mB";
    }

    private static int costTop(Recipe rune) {
        int lines = Minecraft.getInstance().font.split(Component.translatable(rune.descriptionKey()), WIDTH - 8).size();
        return Math.max(40 + lines * 10, 60) + 2 + 20;
    }

    private static void plus(GuiGraphicsExtractor graphics, int cx, int cy) {
        int color = PixelArt.argb(GOLD, 1);
        PixelArt.fill(graphics, cx - 4, cy - 1, cx + 4, cy + 1, color);
        PixelArt.fill(graphics, cx - 1, cy - 4, cx + 1, cy + 4, color);
    }
}
