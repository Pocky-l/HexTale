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
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.PickaxeRune;
import com.pockyl.hextale.runework.PickaxeRunes;

import java.util.List;
import java.util.Map;

/**
 * Burning a rune into a hextech pickaxe at the altar: the pickaxe alone on the altar, the rune on a stele, no pedestal
 * items - only the rune's essence, more for more stars. Shows the cost of each star and the runes it cannot share a
 * pickaxe with. Never what the rune does.
 */
public class RuneBurningCategory extends AbstractRecipeCategory<PickaxeRune> {
    public static final IRecipeType<PickaxeRune> TYPE = IRecipeType.create(Hextale.MOD_ID, "rune_burning", PickaxeRune.class);
    private static final int WIDTH = 160;
    private static final int HEIGHT = 74;

    public RuneBurningCategory(IGuiHelper guiHelper) {
        super(TYPE, Component.translatable("jei.hextale.rune_burning"), guiHelper.createDrawableItemLike(ModItems.RUNE_STELE.get()), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PickaxeRune rune, IFocusGroup focuses) {
        List<ItemStack> before = ModItems.PICKAXES.values().stream().map(item -> new ItemStack(item.get())).toList();
        List<ItemStack> after = before.stream().map(stack -> {
            ItemStack out = stack.copy();
            PickaxeRunes.set(out, PickaxeRunes.EMPTY.with(rune, 1));
            return out;
        }).toList();
        builder.addInputSlot(4, 6).setStandardSlotBackground().addItemStacks(before);
        builder.addOutputSlot(84, 6).setOutputSlotBackground().addItemStacks(after);
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 140, 6).add(new ItemStack(ModItems.HEXTECH_ALTAR.get()));
    }

    @Override
    public void draw(PickaxeRune rune, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        ComplexRune complex = rune.rune();
        // The rune on its stele between the pickaxe and the result.
        PixelArt.glyph(graphics, complex, 34, 4, 14, PixelArt.argb(complex.color(), 1), 0xFF000000);
        PixelArt.fill(graphics, 54, 13, 72, 15, PixelArt.argb(0xF0C24A, 1));
        for (int i = 0; i < 5; i++) {
            PixelArt.fill(graphics, 72 + i, 10 + i, 73 + i, 18 - i, PixelArt.argb(0xF0C24A, 1));
        }
        PixelArt.flush(graphics);
        graphics.text(font, Component.translatable(complex.translationKey()), 4, 28, 0xFF000000 | complex.color(), true);
        // The essence per star.
        int x = 4;
        for (int stars = 1; stars <= 3; stars++) {
            Map.Entry<Essence, Integer> cost = rune.cost(stars).asMap().entrySet().iterator().next();
            String label = "★".repeat(stars);
            graphics.text(font, label, x, 41, 0xFFF0C24A, true);
            int ex = x + font.width(label) + 2;
            PixelArt.essence(graphics, cost.getKey(), ex, 40, PixelArt.ICON, PixelArt.argb(cost.getKey().color(), 1));
            PixelArt.flush(graphics);
            graphics.text(font, cost.getValue() + " mB", ex + 11, 41, 0xFF000000 | cost.getKey().color(), true);
            x += 52;
        }
        StringBuilder clashes = new StringBuilder();
        for (PickaxeRune other : PickaxeRune.values()) {
            if (rune.clashes(other)) {
                clashes.append(clashes.isEmpty() ? "" : ", ").append(Component.translatable(other.rune().translationKey()).getString());
            }
        }
        if (!clashes.isEmpty()) {
            graphics.text(font, Component.translatable("jei.hextale.rune_burning.clash", clashes.toString()), 4, 54, 0xFFE07A6A, false);
        }
        graphics.text(font, Component.translatable("jei.hextale.rune_burning.note", PickaxeRunes.SLOTS), 4, 64, 0xFF7A7498, false);
    }
}
