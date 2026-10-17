package com.pockyl.hextale.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.infusion.InfusionRecipe;
import com.pockyl.hextale.infusion.Ritual;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechPickaxeItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Hextech Altar rituals: the centre item inside the ring of pedestal items, the result, and under them every essence
 * the altar draws (the runes' own essences included), the runes it needs on rune steles - sign and name - and what
 * decides a pickaxe's stars.
 */
public class InfusionCategory extends AbstractRecipeCategory<InfusionCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "infusion", Recipe.class);
    private static final int WIDTH = 160;
    private static final int HEIGHT = 140;
    private static final int RING = 28;
    private static final int CX = 44;
    private static final int CY = 36;
    private static final int TOP = 72;
    private static final int ESSENCES_PER_ROW = 3;
    private static final int RUNES_PER_ROW = 2;

    public record Recipe(Identifier id, InfusionRecipe recipe) {
    }

    public InfusionCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.hextale.infusion"), guiHelper.createDrawableItemLike(ModItems.HEXTECH_ALTAR.get()), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe entry, IFocusGroup focuses) {
        InfusionRecipe recipe = entry.recipe();
        List<Ingredient> pedestals = recipe.pedestals();
        for (int i = 0; i < pedestals.size(); i++) {
            double angle = i * Math.PI * 2 / pedestals.size();
            int x = CX + (int) Math.round(Math.sin(angle) * RING) - 8;
            int y = CY - (int) Math.round(Math.cos(angle) * RING) - 8;
            builder.addInputSlot(x, y).setStandardSlotBackground().add(pedestals.get(i));
        }
        Ingredient center = recipe.center();
        builder.addInputSlot(CX - 8, CY - 8).setStandardSlotBackground().add(center);
        ItemStack shown = center.items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
        builder.addOutputSlot(130, CY - 8).setOutputSlotBackground().add(recipe.resultFor(shown));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 130, 4).add(new ItemStack(ModItems.HEXTECH_ALTAR.get()));
    }

    @Override
    public void draw(Recipe entry, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        InfusionRecipe recipe = entry.recipe();
        Font font = Minecraft.getInstance().font;
        // The circle joining the pedestals and an arrow to the result.
        for (int a = 0; a < 56; a++) {
            double angle = a * Math.PI * 2 / 56;
            int x = CX + (int) Math.round(Math.sin(angle) * RING);
            int y = CY - (int) Math.round(Math.cos(angle) * RING);
            PixelArt.fill(graphics, x, y, x + 1, y + 1, PixelArt.argb(0x5CC8F2, 0.5F));
        }
        int ax = 96;
        PixelArt.fill(graphics, ax, CY - 1, ax + 18, CY + 1, PixelArt.argb(0xF0C24A, 1));
        for (int i = 0; i < 5; i++) {
            PixelArt.fill(graphics, ax + 18 + i, CY - 4 + i, ax + 19 + i, CY + 4 - i, PixelArt.argb(0xF0C24A, 1));
        }
        // Every essence in rows, then the runes, then the stars rule.
        int y = TOP;
        int column = WIDTH / ESSENCES_PER_ROW;
        int i = 0;
        Map<Essence, Integer> essences = recipe.totalEssences().asMap();
        for (Map.Entry<Essence, Integer> essence : essences.entrySet()) {
            int x = i % ESSENCES_PER_ROW * column;
            int ey = y + i / ESSENCES_PER_ROW * 11;
            PixelArt.essence(graphics, essence.getKey(), x, ey, PixelArt.ICON, PixelArt.argb(essence.getKey().color(), 1));
            PixelArt.flush(graphics);
            graphics.text(font, essence.getValue() + " mB", x + 11, ey + 1, 0xFF000000 | essence.getKey().color(), true);
            i++;
        }
        y += (essences.size() + ESSENCES_PER_ROW - 1) / ESSENCES_PER_ROW * 11 + 2;
        List<ComplexRune> runes = recipe.runes();
        int runeColumn = WIDTH / RUNES_PER_ROW;
        for (int r = 0; r < runes.size(); r++) {
            ComplexRune rune = runes.get(r);
            int x = r % RUNES_PER_ROW * runeColumn;
            int ry = y + r / RUNES_PER_ROW * 13;
            PixelArt.glyph(graphics, rune, x, ry, 11, PixelArt.argb(rune.color(), 1), 0xFF000000);
            PixelArt.flush(graphics);
            graphics.text(font, Component.translatable(rune.translationKey()), x + 14, ry + 2, 0xFF000000 | rune.color(), true);
        }
        if (!runes.isEmpty()) {
            y += (runes.size() + RUNES_PER_ROW - 1) / RUNES_PER_ROW * 13;
            boolean pickaxe = HextechPickaxeItem.kind(recipe.result().create()) != null;
            graphics.text(font, Component.translatable(pickaxe ? "jei.hextale.infusion.stars" : "jei.hextale.infusion.runes"), 0, y + 1,
                    0xFF7A7498, false);
        }
        PixelArt.flush(graphics);
        int ticks = new Ritual.Plan(Ritual.Kind.INFUSE, null, Collections.nCopies(recipe.pedestals().size(), null), recipe.totalEssences(),
                ItemStack.EMPTY).duration();
        graphics.text(font, Component.translatable("jei.hextale.infusion.time", String.format("%.1f", ticks / 20.0F)), 92, 8, 0xFF7A7498, false);
    }
}
