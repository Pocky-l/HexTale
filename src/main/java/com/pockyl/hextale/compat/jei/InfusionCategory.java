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
import com.pockyl.hextale.runework.HextechTools;
import com.pockyl.hextale.runework.RuneSlots;
import com.pockyl.hextale.runework.ToolRunes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Hextech Altar rituals: the centre item inside the ring of pedestal items, the result, and under them the essences
 * the altar draws from the tanks and the runes it needs on rune steles. Engravings show a hextech pickaxe taking the
 * rune; erasing shows the runes ground off.
 */
public class InfusionCategory extends AbstractRecipeCategory<InfusionCategory.Recipe> {
    public static final IRecipeType<Recipe> TYPE = IRecipeType.create(Hextale.MOD_ID, "infusion", Recipe.class);
    private static final int WIDTH = 150;
    private static final int HEIGHT = 96;
    private static final int RING = 28;
    private static final int CX = 44;
    private static final int CY = 38;

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
        ItemStack tool = new ItemStack(ModItems.HEXTECH_PICKAXE.get());
        if (recipe.kind() == InfusionRecipe.Kind.INFUSE) {
            Ingredient center = recipe.center().orElseThrow();
            builder.addInputSlot(CX - 8, CY - 8).setStandardSlotBackground().add(center);
            ItemStack shown = center.items().findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
            builder.addOutputSlot(124, CY - 8).setOutputSlotBackground().addItemStack(recipe.resultFor(shown));
        } else {
            ItemStack before = tool.copy();
            ItemStack after = tool.copy();
            if (recipe.kind() == InfusionRecipe.Kind.ENGRAVE) {
                HextechTools.setRunes(after, RuneSlots.EMPTY.with(new RuneSlots.Engraving(0, recipe.rune(), 1)));
            } else {
                HextechTools.setRunes(before, RuneSlots.EMPTY.with(new RuneSlots.Engraving(0, ToolRunes.SMELTING, 1)));
            }
            builder.addInputSlot(CX - 8, CY - 8).setStandardSlotBackground().addItemStack(before);
            builder.addOutputSlot(124, CY - 8).setOutputSlotBackground().addItemStack(after);
        }
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 124, 4).addItemStack(new ItemStack(ModItems.HEXTECH_ALTAR.get()));
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
        int ax = 92;
        PixelArt.fill(graphics, ax, CY - 1, ax + 18, CY + 1, PixelArt.argb(0xF0C24A, 1));
        for (int i = 0; i < 5; i++) {
            PixelArt.fill(graphics, ax + 18 + i, CY - 4 + i, ax + 19 + i, CY + 4 - i, PixelArt.argb(0xF0C24A, 1));
        }
        // Essences and runes along the bottom.
        int x = 2;
        int y = HEIGHT - 12;
        for (Map.Entry<Essence, Integer> essence : recipe.essences().asMap().entrySet()) {
            PixelArt.essence(graphics, essence.getKey(), x, y, PixelArt.ICON, PixelArt.argb(essence.getKey().color(), 1));
            PixelArt.flush(graphics);
            String amount = essence.getValue() + " mB";
            graphics.text(font, amount, x + 11, y + 1, 0xFF000000 | essence.getKey().color(), true);
            x += 11 + font.width(amount) + 6;
        }
        for (ComplexRune rune : recipe.kind() == InfusionRecipe.Kind.INFUSE ? recipe.runes() : List.<ComplexRune>of()) {
            PixelArt.glyph(graphics, rune, x, y - 2, 12, PixelArt.argb(rune.color(), 1), 0xFF000000);
            x += 14;
        }
        if (recipe.kind() == InfusionRecipe.Kind.ENGRAVE) {
            ComplexRune rune = recipe.rune();
            PixelArt.glyph(graphics, rune, 100, CY + 12, 14, PixelArt.argb(rune.color(), 1), 0xFF000000);
            PixelArt.flush(graphics);
            graphics.text(font, Component.translatable("jei.hextale.infusion.engrave"), 92, CY + 28, 0xFF7A7498, false);
        }
        PixelArt.flush(graphics);
        int pedestals = recipe.pedestals().size();
        int ticks = new Ritual.Plan(Ritual.Kind.INFUSE, null, Collections.nCopies(pedestals, null), recipe.essences(), List.of(),
                ItemStack.EMPTY).duration();
        graphics.text(font, Component.translatable("jei.hextale.infusion.time", String.format("%.1f", ticks / 20.0F)), 92, 4, 0xFF7A7498, false);
    }
}
